# PDF → Markdown сервис

Сервис на Spring Boot для конвертации PDF в Markdown.
Опционально использует локальную LLM (Qwen2.5 1.5B через llama.cpp)
для качественной расстановки Markdown-разметки.

## Возможности

- POST `/api/convert` — загрузка PDF, получение ссылки на скачивание
- GET `/api/download/{id}` — скачивание MD, файл удаляется автоматически после отдачи
- Автоматическая уборка по TTL (по умолчанию 30 минут)
- Опциональная LLM-постобработка через локальный llama.cpp
- Полностью локальный запуск — без облачных API

## Требования

- Java 17+
- Maven 3.8+
- Docker + Docker Compose

## Запуск

```bash
# Полный стек с локальной LLM
mvn spring-boot:run

# Без LLM (только эвристика)
mvn spring-boot:run -Dspring-boot.run.profiles=local