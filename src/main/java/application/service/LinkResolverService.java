package application.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LinkResolverService {

    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^\\)]+)\\)");

    public String enrichWithLinkedContent(String markdown, int maxLinks, int maxCharsPerLink) {
        Set<String> urls = extractUrls(markdown, maxLinks);
        if (urls.isEmpty()) {
            return markdown;
        }
        StringBuilder enriched = new StringBuilder(markdown);
        enriched.append("\n\n## Связанные документы (по ссылкам)\n\n");
        for (String url : urls) {
            log.info("Загрузка содержимого по ссылке: {}", url);
            enriched.append("### Источник: ").append(url).append("\n\n");
            String content = fetchContent(url);
            if (content.isBlank()) {
                enriched.append("_Не удалось извлечь текст._\n\n");
            } else {
                if (content.length() > maxCharsPerLink) {
                    content = content.substring(0, maxCharsPerLink) + "\n...(текст обрезан)...";
                }
                enriched.append(content).append("\n\n---\n\n");
            }
        }
        return enriched.toString();
    }

    private Set<String> extractUrls(String markdown, int maxLinks) {
        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = MARKDOWN_LINK.matcher(markdown);
        while (matcher.find() && urls.size() < maxLinks) {
            String url = matcher.group(2);
            if (url.startsWith("http")) {
                urls.add(url);
            }
        }
        return urls;
    }

    private String fetchContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .ignoreContentType(true)
                    .get();
            String contentType = doc.connection().response().contentType();
            if (contentType != null && (contentType.contains("yaml")
                    || contentType.contains("json")
                    || contentType.contains("text/plain"))) {
                return doc.body().text();
            }
            doc.select("script, style, nav, footer, header, iframe, noscript, .ad").remove();
            return doc.body().text().replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            log.warn("Не удалось загрузить {}: {}", url, e.getMessage());
            return "";
        }
    }
}
