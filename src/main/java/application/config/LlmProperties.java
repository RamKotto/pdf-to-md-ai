package application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /**
     * Включает LLM-постобработку. Если false — используется эвристика.
     */
    private boolean enabled = false;

    /**
     * OpenAI-совместимый эндпоинт.
     */
    private String baseUrl = "http://localhost:8081/v1";

    /**
     * API-ключ. Для локальных моделей не нужен, но SDK требует непустое значение.
     */
    private String apiKey = "local-no-auth";

    /**
     * Имя модели. Для llama.cpp любое — сервер игнорирует.
     */
    private String model = "local-model";

    /**
     * Максимум символов в одном запросе. Превышение — пропуск LLM.
     */
    private int maxInputChars = 8000;

    /**
     * Максимум токенов в ответе.
     */
    private int maxOutputTokens = 4096;

    /**
     * Таймаут запроса в секундах.
     */
    private int timeoutSeconds = 120;
}
