package application.service;

import application.config.OpenRouterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenRouterClient {

    private final OpenRouterProperties props;
    private final RestClient http;

    public OpenRouterClient(OpenRouterProperties props, RestClient openRouterRestClient) {
        this.props = props;
        this.http = openRouterRestClient;
    }

    public String convertPdfFile(byte[] pdfBytes, String filename) {
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        return callWithFile("data:application/pdf;base64," + base64, filename);
    }

    public String convertPdfUrl(String url, String filename) {
        return callWithFile(url, filename);
    }

    public String convertTextUrl(String url, String filename) {
        String content = downloadTextOrHtml(url);
        String systemPrompt = isSwagger(content) ? SWAGGER_PROMPT : TEXT_PROMPT;
        if (content.length() > 30000) {
            content = content.substring(0, 30000);
        }
        return callWithText(content, filename, systemPrompt);
    }

    private String callWithFile(String fileData, String filename) {
        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", PDF_PROMPT),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", "Преобразуй этот документ в Markdown."),
                                Map.of("type", "file", "file",
                                        Map.of("filename", filename, "file_data", fileData))
                        ))
                ),
                "temperature", 0.0,
                "max_tokens", props.getMaxOutputTokens(),
                "plugins", List.of(Map.of("id", "file-parser", "pdf",
                        Map.of("engine", props.getPdfEngine())))
        );
        return execute(body, filename);
    }

    private String callWithText(String text, String filename, String systemPrompt) {
        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Файл: " + filename + "\n\n" + text)
                ),
                "temperature", 0.0,
                "max_tokens", props.getMaxOutputTokens()
        );
        return execute(body, filename);
    }

    @SuppressWarnings("unchecked")
    private String execute(Map<String, Object> body, String label) {
        try {
            Map<String, Object> response = http.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) throw new IllegalStateException("Пустой ответ");
            var choices = (List<Map<String, Object>>) response.get("choices");
            var message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Ошибка OpenRouter для '{}'", label, e);
            throw new RuntimeException("Ошибка OpenRouter: " + e.getMessage(), e);
        }
    }

    private String downloadTextOrHtml(String url) {
        try {
            var entity = RestClient.create().get().uri(URI.create(url)).retrieve().toEntity(String.class);
            return entity.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось скачать: " + url, e);
        }
    }

    private boolean isSwagger(String content) {
        String lower = content.toLowerCase();
        return lower.contains("swagger") || lower.contains("openapi") || lower.contains("paths:");
    }

    private static final String PDF_PROMPT = "Ты — агент по преобразованию PDF в Markdown. " +
            "Сохраняй весь текст. Восстанавливай таблицы. Только Markdown.";
    private static final String TEXT_PROMPT = "Приведи текст к чистому Markdown. Не меняй содержание.";
    private static final String SWAGGER_PROMPT = "Оформи Swagger/OpenAPI в Markdown. " +
            "Сохрани эндпоинты, таблицы параметров. Только Markdown.";
}
