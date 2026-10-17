package application.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @Valid
    private Storage storage = new Storage();

    @Valid
    private Links links = new Links();

    @Valid
    private Limits limits = new Limits();

    @Valid
    private Prompts prompts = new Prompts();

    @Data
    public static class Storage {
        private Path directory = Path.of("../data/inbox");
        @NotBlank
        private String baseUrl = "http://localhost:8080";
    }

    @Data
    public static class Links {
        private boolean enabled = true;
        @Min(1)
        private int maxLinks = 5;
        @Min(1)
        private int maxCharsPerLink = 5000;
        @Min(100)
        private int fetchTimeoutMs = 10_000;
        private String userAgent = "Mozilla/5.0";
    }

    @Data
    public static class Limits {
        @Min(1000)
        private int maxTextChars = 30_000;
    }

    @Data
    public static class Prompts {
        @NotBlank
        private String pdf;
        @NotBlank
        private String text;
        @NotBlank
        private String swagger;
    }
}
