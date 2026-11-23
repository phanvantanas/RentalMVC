package controller;

import data.dao.ChatCatalogDao;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import service.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

@WebServlet(
        name = "ChatbotServlet",
        urlPatterns = {"/chatbot"})
public class ChatbotServlet extends HttpServlet {
    private final Semaphore slots = new Semaphore(4);
    private final ConcurrentHashMap<String, Window> limits = new ConcurrentHashMap<>();

    private record Window(long start, int count) {}

    protected ChatbotConfig config() throws IOException {
        return ChatbotConfig.load(getServletContext());
    }

    protected GeminiChatService service() {
        return new GeminiChatService();
    }

    protected String catalog() throws SQLException {
        return new ChatCatalogDao().context();
    }

    protected long now() {
        return System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> history(HttpSession session) {
        Object value = session.getAttribute("chatHistory");
        return value instanceof List<?> ? (List<Map<String, String>>) value : List.of();
    }

    private void reply(HttpServletResponse s, int status, Object body) throws IOException {
        s.setStatus(status);
        s.setHeader("Cache-Control", "no-store");
        s.setHeader("X-Content-Type-Options", "nosniff");
        s.setContentType("application/json");
        s.setCharacterEncoding("UTF-8");
        s.getWriter().write(ChatJson.encode(body));
    }

    private boolean allow(String address) {
        long time = now();
        if (limits.size() >= 4096) {
            limits.entrySet().removeIf(entry -> time - entry.getValue().start() >= 60000);
            if (limits.size() >= 4096 && !limits.containsKey(address)) return false;
        }
        Window window =
                limits.compute(
                        address,
                        (key, old) ->
                                old == null || time - old.start() >= 60000
                                        ? new Window(time, 1)
                                        : new Window(old.start(), Math.min(21, old.count() + 1)));
        return window.count() <= 20;
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse s) throws IOException {
        HttpSession session = q.getSession();
        try {
            synchronized (session) {
                reply(
                        s,
                        200,
                        Map.of(
                                "ready",
                                config().configured(),
                                "csrf",
                                CartSupport.token(session),
                                "messages",
                                history(session)));
            }
        } catch (IOException ex) {
            reply(s, 503, Map.of("error", "Trợ lý đang được cấu hình. Bạn vui lòng thử lại sau."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse s) throws IOException {
        q.setCharacterEncoding("UTF-8");
        if (q.getContentLengthLong() > 16000) {
            reply(s, 413, Map.of("error", "Tin nhắn quá dài."));
            return;
        }
        if (!CartSupport.validToken(q)) {
            reply(
                    s,
                    403,
                    Map.of("error", "Phiên trò chuyện đã thay đổi. Hãy đóng rồi mở lại trợ lý."));
            return;
        }
        String message = Objects.toString(q.getParameter("message"), "").strip();
        if (message.isEmpty() || message.length() > 1200) {
            reply(s, 400, Map.of("error", "Bạn nhập câu hỏi từ 1 đến 1.200 ký tự nhé."));
            return;
        }
        if (!allow(Objects.toString(q.getRemoteAddr(), "unknown"))) {
            s.setHeader("Retry-After", "60");
            reply(s, 429, Map.of("error", "Bạn gửi hơi nhanh. Hãy thử lại sau một phút nhé."));
            return;
        }
        HttpSession session = q.getSession();
        List<Map<String, String>> previous;
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute("chatBusy"))) {
                reply(
                        s,
                        429,
                        Map.of("error", "Mình đang trả lời câu trước, bạn đợi một chút nhé."));
                return;
            }
            session.setAttribute("chatBusy", true);
            previous = List.copyOf(history(session));
        }
        boolean acquired = slots.tryAcquire();
        try {
            if (!acquired) {
                reply(s, 429, Map.of("error", "Trợ lý đang bận. Bạn thử lại sau ít giây nhé."));
                return;
            }
            ChatbotConfig settings = config();
            if (!settings.configured())
                throw new GeminiChatService.ChatException(
                        503, "Trợ lý đang được cấu hình. Bạn vui lòng thử lại sau.");
            String answer = service().answer(settings, catalog(), previous, message);
            synchronized (session) {
                List<Map<String, String>> updated = new ArrayList<>(previous);
                updated.add(Map.of("role", "user", "text", message));
                updated.add(Map.of("role", "model", "text", answer));
                if (updated.size() > 12)
                    updated = new ArrayList<>(updated.subList(updated.size() - 12, updated.size()));
                session.setAttribute("chatHistory", List.copyOf(updated));
            }
            reply(s, 200, Map.of("reply", answer));
        } catch (GeminiChatService.ChatException ex) {
            reply(s, ex.status(), Map.of("error", ex.getMessage()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            reply(s, 503, Map.of("error", "Kết nối bị gián đoạn. Bạn thử lại nhé."));
        } catch (IOException | SQLException ex) {
            reply(
                    s,
                    503,
                    Map.of(
                            "error",
                            "Trợ lý tạm gián đoạn. Bạn vui lòng thử lại hoặc liên hệ LensCam."));
        } finally {
            if (acquired) slots.release();
            try {
                synchronized (session) {
                    session.removeAttribute("chatBusy");
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }
}
