package application.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import application.config.LlmProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmMarkdownEnhancer {

    private final LlmProperties props;
    private OpenAIClient client;

    private static final String SYSTEM_PROMPT = """
            Ты — помощник по форматированию Markdown. Твоя единственная задача — 
            расставить разметку Markdown в предоставленном тексте.
                        
            Строгие правила:
            1. Нельзя изменять, удалять или добавлять любой текст из оригинала
            2. Нельзя выдумывать информацию, которой нет в тексте
            3. Только структурная разметка:
               - определяй заголовки и ставь префиксы # ## ### (по уровню)
               - определяй списки и ставь - или 1.
               - определяй блоки кода и оборачивай в ```
               - таблицы оформляй синтаксисом Markdown-таблиц
            4. Если уровень заголовка неясен — лучше оставь как обычный абзац, не угадывай
            5. Выводи только Markdown, без пояснений и без обрамляющих блоков ```
                        
            На входе — чистый текст, извлечённый из PDF, возможны разрывы строк 
            и смещения. Твоя работа — сделать его читаемым, а не переписать.
            """;

    @PostConstruct
    void init() {
        if (!props.isEnabled()) {
            log.info("LLM-улучшение выключено (app.llm.enabled=false)");
            return;
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.warn("LLM-улучшение включено, но API-ключ пустой — работаем без LLM");
            return;
        }

        this.client = OpenAIOkHttpClient.builder()
                .baseUrl(props.getBaseUrl())
                .apiKey(props.getApiKey())
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .build();

        log.info("LLM-улучшение включено: {} / {}", props.getBaseUrl(), props.getModel());
    }

    /**
     * Обогащает текст Markdown-разметкой.
     * При любой ошибке или отключённой LLM возвращает исходный текст без изменений —
     * вызывающий код поймёт это по равенству строк и применит fallback.
     */
    public String enhance(String rawText) {
        if (client == null) {
            return rawText;
        }
        if (rawText.isBlank()) {
            return rawText;
        }
        if (rawText.length() > props.getMaxInputChars()) {
            log.warn("Текст слишком длинный ({} символов > {}), пропускаем LLM",
                    rawText.length(), props.getMaxInputChars());
            return rawText;
        }

        long start = System.currentTimeMillis();
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(props.getModel())
                    .addSystemMessage(SYSTEM_PROMPT)
                    .addUserMessage(rawText)
                    .temperature(0.1)
                    .maxTokens(props.getMaxOutputTokens())
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);
            String result = completion.choices().get(0).message().content().orElse(rawText);

            // Защита: если модель вернула явно мало текста (< 30% от исходного),
            // вероятно, она что-то потеряла — возвращаем оригинал.
            if (result.length() < rawText.length() * 0.3) {
                log.warn("LLM вернула подозрительно короткий ответ ({} vs {}), откат",
                        result.length(), rawText.length());
                return rawText;
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("LLM обработала {} символов за {} мс", rawText.length(), elapsed);
            return result;
        } catch (Exception e) {
            log.error("Ошибка LLM, откатываемся на эвристику", e);
            return rawText;
        }
    }
}
