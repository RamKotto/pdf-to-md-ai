package application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterProperties {

    private String baseUrl = "https://openrouter.ai/api/v1";
    private String apiKey;
    private String model = "openrouter/free";
    private int maxOutputTokens = 8192;
    private int timeoutSeconds = 600;
    private String pdfEngine = "mistral-ocr";
    private String referer = "http://localhost:8080";
    private String title = "pdf-to-md";
}
