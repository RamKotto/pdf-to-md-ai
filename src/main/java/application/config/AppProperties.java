package application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Storage storage = new Storage();
    private Links links = new Links();

    @Data
    public static class Storage {
        private Path directory = Path.of("../data/inbox");
        private String baseUrl = "http://localhost:8080";
    }

    @Data
    public static class Links {
        private boolean enabled = true;
        private int maxLinks = 5;
        private int maxCharsPerLink = 5000;
    }
}
