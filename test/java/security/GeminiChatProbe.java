package security;

import service.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Optional live check. Reads a local server config; never prints the API key. */
public class GeminiChatProbe {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        try (var in = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8)) {
            properties.load(in);
        }
        ChatbotConfig config =
                new ChatbotConfig(
                        properties.getProperty("gemini.api.key"),
                        properties.getProperty("gemini.model"));
        String answer =
                new GeminiChatService()
                        .answer(
                                config,
                                "Canon EOS M10 | 150000 VND/ngày",
                                List.of(),
                                "Tôi thuê 1 Canon EOS M10 trong 1 ngày thì tiền thuê bao nhiêu? Trả"
                                    + " lời một câu.");
        if (answer.isBlank()) throw new AssertionError("Empty live response");
        System.out.println("LIVE GEMINI RESPONSE: " + answer);
    }
}
