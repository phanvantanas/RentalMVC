package service;

import jakarta.servlet.ServletContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public record ChatbotConfig(String apiKey, String model) {
    public static ChatbotConfig load(ServletContext context) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = context.getResourceAsStream("/WEB-INF/config/chatbot.properties")) {
            if (in != null) properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        String key = System.getenv("GEMINI_API_KEY"), model = System.getenv("GEMINI_MODEL");
        if (key == null || key.isBlank()) key = properties.getProperty("gemini.api.key", "");
        if (model == null || model.isBlank())
            model = properties.getProperty("gemini.model", "gemini-3.5-flash-lite");
        if (!model.matches("[A-Za-z0-9.-]{1,100}"))
            throw new IOException("Invalid chatbot model configuration");
        return new ChatbotConfig(key.strip(), model);
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "ChatbotConfig[model=" + model + ",configured=" + configured() + "]";
    }
}
