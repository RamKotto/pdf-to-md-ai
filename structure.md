# 📄 PDF-to-MD — структура проекта

> Сервис конвертации PDF-документов в Markdown с опциональным улучшением через LLM.

---

## 🌳 Дерево проекта

```text
pdf-to-md/
├── docker-compose.yml
├── pom.xml
├── .gitignore
├── README.md
├── models/
│   └── .gitkeep
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── PdfToMdApplication.java
    │   │   ├── config/
    │   │   │   ├── AppProperties.java
    │   │   │   └── LlmProperties.java
    │   │   ├── controller/
    │   │   │   └── ConversionController.java
    │   │   ├── service/
    │   │   │   ├── ConversionService.java
    │   │   │   ├── LlmMarkdownEnhancer.java
    │   │   │   ├── SelfDeletingResource.java
    │   │   │   └── StorageService.java
    │   │   ├── dto/
    │   │   │   └── ConvertResponse.java
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-local.yml
    └── test/java/com/example/pdftomd/
        └── PdfToMdIntegrationTest.java
```