# PDF to Markdown Converter (pdf-to-md)

Сервис для конвертации PDF-файлов, веб-страниц и Swagger/OpenAPI-документов в чистый Markdown.
Использует мультимодальные LLM через OpenRouter для распознавания таблиц и сложной вёрстки,
а также умеет переходить по ссылкам внутри документа для обогащения контекста.

## Возможности

- **Приём PDF-файлов**: `POST /api/convert` (`multipart/form-data`).
- **Приём ссылок**: `POST /api/convert` (`application/json`) — PDF, YAML/JSON (Swagger/OpenAPI), HTML.
- **Link Resolution**: находит markdown-ссылки, HTML `<a href>` и голые URL (до `app.links.max-links`),
  скачивает содержимое и дописывает в конец MD-файла.
- **Скачивание результата**: `GET /api/files/{filename}`.
- **Сохранение**: MD-файлы складываются в `app.storage.directory` (по умолчанию `${user.dir}/data/inbox`).
- **Healthcheck**: `GET /actuator/health`.

## Требования

- Java 21
- Maven 3.8+
- API-ключ OpenRouter

## Конфигурация

Все настройки — в `src/main/resources/application.yml`. Ключевые:

| Переменная окружения | Свойство | Назначение |
|---|---|---|
| `OPENROUTER_API_KEY` | `openrouter.api-key` | API-ключ (обязателен) |
| — | `openrouter.model` | Модель (по умолчанию `openrouter/free`) |
| — | `openrouter.pdf-engine` | Движок PDF-парсера (`mistral-ocr`) |
| — | `app.storage.directory` | Куда писать MD |
| — | `app.storage.base-url` | Базовый URL для `downloadUrl` |
| — | `app.links.*` | Настройки обхода ссылок |
| — | `app.limits.max-text-chars` | Лимит символов для текстовых источников |
| — | `app.prompts.*` | Промпты для LLM |

## Запуск

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
mvn clean spring-boot:run
```

Сервис поднимется на `http://localhost:8080`.

## API

### `POST /api/convert` (PDF)

```bash
curl -F "file=@document.pdf" http://localhost:8080/api/convert
```

### `POST /api/convert` (URL)

```bash
curl -X POST http://localhost:8080/api/convert \
  -H "Content-Type: application/json" \
  -d '{"url":"https://raw.githubusercontent.com/example/api/main/openapi.yaml