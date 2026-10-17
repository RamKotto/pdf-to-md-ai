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
            Ты — агент по оформлению Markdown. Твоя единственная задача — расставить разметку в предоставленном тексте,
            не меняя его содержание.
                
            Строгие правила:
            1. Не изменяй, не удаляй и не добавляй текст. Разрешено только склеивать разорванные строки,
               убирать случайные переносы и схлопывать лишние пустые строки.
            2. Не выдумывай информацию. Если данных нет — не добавляй. Не воображай колонтитулы, номера страниц
               и артефакты вёрстки, если их нет во входном тексте.
            3. Оформляй только структуру:
               - Заголовки: используй все уровни `#`, `##`, `###`, `####`, `#####`. `#` — только название документа
                 (один раз). Не ставь всё на `##`, не пропускай уровни.
               - Таблицы: любые пары «Термин → Описание», «Поле → Значение» оформляй Markdown-таблицей
                 с разделителем `|---|---|`.
               - Списки: `-` для маркированных, `1.` для нумерованных. Вложенность — отступом в 2–4 пробела.
                 Без `;` и `.` в конце пунктов.
               - Ссылки: только `[текст](url)`.
               - Выделение: `**жирный**` для терминов, `*курсив*` для названий, `` `код` `` для файлов/команд/значений.
                 Блоки кода — ``` с указанием языка.
               - Оглавление: кликабельные ссылки `- [Раздел](#anchor)`, без номеров страниц.
            4. Пустая строка обязательна до и после заголовков, таблиц, списков, блоков кода и между абзацами.
            5. В заголовках не ставь точку, не используй `**жирный**`, не дублируй `#` в тексте.
            6. Если уровень заголовка неясен — оставь обычным абзацем, не угадывай.
            7. Убирай висячие символы (`;`, `.`, `:`) в конце пунктов, если они не несут смысла.
            8. Выводи только Markdown. Без пояснений, без обрамляющих блоков ```.
                
            На входе — плоский текст, уже извлечённый из PDF через PDFBox. Возможны разрывы строк,
            склеенные абзацы и потерянные пробелы. PDF-специфичных артефактов (колонтитулы, номера страниц)
            в тексте уже нет. Твоя работа — расставить разметку, а не переписать текст.
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
