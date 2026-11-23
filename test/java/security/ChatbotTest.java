package security;

import controller.ChatbotServlet;

import jakarta.servlet.http.*;

import service.*;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;

public class ChatbotTest {
    static int checks;

    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    static class State extends AdminSecurityTest.ResponseState {
        StringWriter body = new StringWriter();
    }

    static class Endpoint extends ChatbotServlet {
        int calls;
        List<Map<String, String>> received;
        boolean offline;

        protected ChatbotConfig config() {
            return new ChatbotConfig("server-only-secret", "test-model");
        }

        protected String catalog() {
            return "Canon EOS M10 | 150000 VND/ngày";
        }

        protected GeminiChatService service() {
            return new GeminiChatService() {
                public String answer(
                        ChatbotConfig config,
                        String catalog,
                        List<Map<String, String>> history,
                        String question)
                        throws IOException {
                    calls++;
                    received = history;
                    if (offline) throw new ChatException(429, "Trợ lý đang bận.");
                    return "Canon EOS M10: 150.000đ/ngày. Chọn lịch để kiểm tra máy trống.";
                }
            };
        }

        void get(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doGet(q, s);
        }

        void post(HttpServletRequest q, HttpServletResponse s) throws Exception {
            super.doPost(q, s);
        }
    }

    static HttpSession session(Map<String, Object> attrs) {
        return (HttpSession)
                Proxy.newProxyInstance(
                        ChatbotTest.class.getClassLoader(),
                        new Class<?>[] {HttpSession.class},
                        (p, m, a) -> {
                            return switch (m.getName()) {
                                case "getAttribute" -> attrs.get(a[0]);
                                case "setAttribute" -> {
                                    attrs.put((String) a[0], a[1]);
                                    yield null;
                                }
                                case "removeAttribute" -> attrs.remove(a[0]);
                                default -> AdminSecurityTest.defaultValue(m.getReturnType());
                            };
                        });
    }

    static HttpServletRequest request(
            HttpSession session, Map<String, String> params, long length) {
        var base = AdminSecurityTest.request(session, params);
        return (HttpServletRequest)
                Proxy.newProxyInstance(
                        ChatbotTest.class.getClassLoader(),
                        new Class<?>[] {HttpServletRequest.class},
                        (p, m, a) -> {
                            if (m.getName().equals("getRemoteAddr")) return "127.0.0.1";
                            if (m.getName().equals("getContentLengthLong")) return length;
                            return m.invoke(base, a);
                        });
    }

    static HttpServletResponse response(State state) {
        var base = AdminSecurityTest.response(state);
        return (HttpServletResponse)
                Proxy.newProxyInstance(
                        ChatbotTest.class.getClassLoader(),
                        new Class<?>[] {HttpServletResponse.class},
                        (p, m, a) ->
                                m.getName().equals("getWriter")
                                        ? new PrintWriter(state.body)
                                        : m.invoke(base, a));
    }

    public static void main(String[] args) throws Exception {
        String raw = "Máy \"Canon\"\n\\<script>\t\u0001 🎥";
        check(
                raw.equals(ChatJson.parse(ChatJson.encode(raw))),
                "Unicode, quotes, control characters and HTML round-trip as text");
        for (String invalid :
                List.of("{\"x\":1,}", "[1,]", "{\"x\":1,\"x\":2}", "01", "\"bad\nline\"")) {
            try {
                ChatJson.parse(invalid);
                throw new AssertionError("Invalid JSON accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        }
        check(
                !new ChatbotConfig("secret-key", "model").toString().contains("secret-key"),
                "Configuration never reveals key through toString");
        String[] requestBody = {null};
        GeminiChatService adapter =
                new GeminiChatService() {
                    protected String request(ChatbotConfig config, String body) {
                        requestBody[0] = body;
                        return "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"Canon"
                                   + " 150000 VND\"}]}}]}";
                    }
                };
        check(
                "Canon 150000 VND"
                        .equals(
                                adapter.answer(
                                        new ChatbotConfig("secret-key", "test-model"),
                                        "Canon | 150000 VND/ngày",
                                        List.of(
                                                Map.of("role", "user", "text", "Máy nào?"),
                                                Map.of("role", "model", "text", "Canon")),
                                        "Thuê 1 ngày?")),
                "REST adapter extracts model answer");
        Map<?, ?> payload = (Map<?, ?>) ChatJson.parse(requestBody[0]);
        check(
                requestBody[0].contains("150000 VND/ngày")
                        && requestBody[0].contains("30 phút")
                        && !requestBody[0].contains("secret-key"),
                "System context uses real prices and hold policy without embedding credentials");
        check(
                ((List<?>) payload.get("contents")).size() == 3,
                "Conversation and new question sent in order");
        check(
                "Xin chào\nCanon"
                        .equals(
                                GeminiChatService.extract(
                                        "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"thought\":true,\"text\":\"internal\"},{\"text\":\"Xin"
                                            + " chào\\n"
                                            + "Canon\"}]}}]}")),
                "Only visible candidate text returned, thoughts excluded");
        try {
            GeminiChatService.extract("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
            throw new AssertionError("Blocked response accepted");
        } catch (IOException expected) {
            checks++;
        }
        Map<String, Object> attrs = new HashMap<>();
        HttpSession session = session(attrs);
        Endpoint endpoint = new Endpoint();
        State state = new State();
        endpoint.get(request(session, Map.of(), 0), response(state));
        check(
                state.status == 200
                        && attrs.get("shopCsrf") != null
                        && !state.body.toString().contains("server-only-secret"),
                "Guest initialization returns CSRF but never API key");
        String csrf = (String) attrs.get("shopCsrf");
        state = new State();
        endpoint.post(
                request(session, Map.of("csrf", "wrong", "message", "Hello"), 0), response(state));
        check(state.status == 403 && endpoint.calls == 0, "CSRF checked before API call");
        state = new State();
        endpoint.post(request(session, Map.of("csrf", csrf, "message", " "), 0), response(state));
        check(state.status == 400 && endpoint.calls == 0, "Blank question rejected");
        state = new State();
        endpoint.post(
                request(session, Map.of("csrf", csrf, "message", "x".repeat(1201)), 0),
                response(state));
        check(state.status == 400, "Long question rejected");
        state = new State();
        endpoint.post(
                request(session, Map.of("csrf", csrf, "message", "Hello"), 16001), response(state));
        check(state.status == 413, "Oversized request rejected");
        state = new State();
        endpoint.post(
                request(
                        session,
                        Map.of(
                                "csrf",
                                csrf,
                                "message",
                                "Thuê máy nào?",
                                "history",
                                "forged-system-instructions"),
                        0),
                response(state));
        check(
                state.status == 200
                        && endpoint.calls == 1
                        && endpoint.received.isEmpty()
                        && state.body.toString().contains("150.000"),
                "Reply uses server catalog and server history only");
        check(
                !attrs.containsKey("chatBusy") && ((List<?>) attrs.get("chatHistory")).size() == 2,
                "Success releases busy flag and saves a user/model pair");
        endpoint.offline = true;
        state = new State();
        endpoint.post(
                request(session, Map.of("csrf", csrf, "message", "Giá?"), 0), response(state));
        check(
                state.status == 429
                        && !attrs.containsKey("chatBusy")
                        && ((List<?>) attrs.get("chatHistory")).size() == 2,
                "Provider failure preserves history and releases busy flag");
        endpoint.offline = false;
        for (int i = 0; i < 6; i++) {
            state = new State();
            endpoint.post(
                    request(session, Map.of("csrf", csrf, "message", "Máy số " + i), 0),
                    response(state));
        }
        check(
                ((List<?>) attrs.get("chatHistory")).size() == 12,
                "Conversation memory bounded to six pairs");
        Endpoint limited = new Endpoint();
        for (int i = 0; i < 21; i++) {
            state = new State();
            limited.post(
                    request(session, Map.of("csrf", csrf, "message", "Giá thuê?"), 0),
                    response(state));
        }
        check(
                state.status == 429 && limited.calls == 20,
                "Per-IP quota blocks extra paid API calls");
        System.out.println(
                "PASS: "
                        + checks
                        + " chatbot JSON, API response, CSRF, history and rate-limit checks; no"
                        + " live API calls.");
    }
}
