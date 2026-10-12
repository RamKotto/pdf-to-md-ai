package service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private static final Pattern MULTIPLE_BLANK_LINES = Pattern.compile("\\n{3,}");

    private final LlmMarkdownEnhancer llmEnhancer;

    /**
     * Двухступенчатая конвертация:
     * 1. PDFBox извлекает чистый текст (гарантия отсутствия выдумок)
     * 2. LLM расставляет Markdown-разметку (если включён)
     * 3. При выключенной/упавшей LLM — эвристика
     */
    public String pdfToMarkdown(byte[] pdfBytes) {
        String rawText = extractText(pdfBytes);

        String markdown = llmEnhancer.enhance(rawText);

        // LLM вернул исходный текст — значит, выключен или упал.
        // Применяем эвристику как fallback.
        if (markdown.equals(rawText)) {
            log.debug("LLM не применилась, используем эвристику");
            markdown = normalizeToMarkdown(rawText);
        } else {
            log.debug("LLM-разметка применена");
        }

        return markdown;
    }

    private String extractText(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            stripper.setParagraphEnd("\n");

            String text = stripper.getText(doc);
            log.debug("Извлечено {} символов из PDF ({} страниц)",
                    text.length(), doc.getNumberOfPages());
            return text;
        } catch (IOException e) {
            throw new UncheckedIOException("Ошибка извлечения текста из PDF", e);
        }
    }

    /**
     * Простая эвристика на случай недоступности LLM.
     */
    private String normalizeToMarkdown(String text) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder md = new StringBuilder();

        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                md.append('\n');
                continue;
            }
            if (looksLikeHeading(line)) {
                md.append("## ").append(line).append("\n\n");
            } else {
                md.append(line).append("\n\n");
            }
        }

        return MULTIPLE_BLANK_LINES.matcher(md.toString())
                .replaceAll("\n\n")
                .strip() + "\n";
    }

    private boolean looksLikeHeading(String line) {
        if (line.length() >= 100) {
            return false;
        }
        boolean hasLetter = line.chars().anyMatch(Character::isLetter);
        if (!hasLetter) {
            return false;
        }
        boolean allUpper = line.equals(line.toUpperCase());
        boolean noSentenceEnd = !line.endsWith(".")
                && !line.endsWith("!")
                && !line.endsWith("?");
        return allUpper
                || (line.length() < 60 && noSentenceEnd && line.split("\\s+").length <= 8);
    }
}
