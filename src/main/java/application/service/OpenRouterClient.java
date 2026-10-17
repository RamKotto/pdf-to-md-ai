package application.service;

import application.config.AppProperties;
import application.config.OpenRouterProperties;
import application.exception.OpenRouterException;
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
    private final AppProperties appProps;
    private final RestClient http;

    public OpenRouterClient(OpenRouterProperties props,
                            AppProperties appProps,
                            RestClient openRouterRestClient) {
        this.props = props;
        this.appProps = appProps;
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
        String systemPrompt = isSwagger(content)
                ? appProps.getPrompts().getSwagger()
                : appProps.getPrompts().getText();
        int maxChars = appProps.getLimits().getMaxTextChars();
        if (content.length() > maxChars) {
            content = content.substring(0, maxChars);
        }
        return callWithText(content, filename, systemPrompt);
    }

    private String callWithFile(String fileData, String filename) {
        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", appProps.getPrompts().getPdf()),
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
            if (response == null) {
                throw new OpenRouterException("Пустой ответ от OpenRouter");
            }
            var choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new OpenRouterException("OpenRouter не вернул choices");
            }
            var message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message == null ? null : message.get("content");
            if (content == null) {
                throw new OpenRouterException("Пустой content в ответе OpenRouter");
            }
            return content.toString();
        } catch (OpenRouterException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка OpenRouter для '{}'", label, e);
            throw new OpenRouterException(e.getMessage(), e);
        }
    }

    private String downloadTextOrHtml(String url) {
        try {
            var entity = RestClient.create().get().uri(URI.create(url)).retrieve().toEntity(String.class);
            return entity.getBody() == null ? "" : entity.getBody();
        } catch (Exception e) {
            throw new OpenRouterException("Не удалось скачать: " + url, e);
        }
    }

    private boolean isSwagger(String content) {
        String lower = content.toLowerCase();
        return lower.contains("swagger") || lower.contains("openapi") || lower.contains("paths:");
    }
}
