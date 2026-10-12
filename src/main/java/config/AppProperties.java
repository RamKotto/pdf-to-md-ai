package config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.storage")
public class AppProperties {

    /**
     * Директория для временного хранения MD-файлов.
     */
    private Path directory = Path.of(System.getProperty("java.io.tmpdir"), "pdf-to-md");

    /**
     * Время жизни файла до принудительной уборки.
     */
    private Duration ttl = Duration.ofMinutes(30);

    /**
     * Базовый URL для формирования ссылок на скачивание.
     */
    private String baseUrl = "http://localhost:8080";
}