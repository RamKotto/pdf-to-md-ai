package application.service;

import application.config.AppProperties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class LinkResolverService {

    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)");
    private static final Pattern BARE_URL = Pattern.compile("(?<![(\\[])(https?://[^\\s)\\]<>\"']+)");
    private static final Pattern HTML_ANCHOR = Pattern.compile("<a\\s+[^>]*href=[\"'](https?://[^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);

    private final AppProperties props;

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

    Set<String> extractUrls(String markdown, int maxLinks) {
        Set<String> urls = new LinkedHashSet<>();
        collect(MARKDOWN_LINK.matcher(markdown), urls, maxLinks, 2);
        collect(HTML_ANCHOR.matcher(markdown), urls, maxLinks, 1);
        collect(BARE_URL.matcher(markdown), urls, maxLinks, 1);
        return urls;
    }

    private void collect(Matcher matcher, Set<String> urls, int maxLinks, int group) {
        while (matcher.find() && urls.size() < maxLinks) {
            String url = matcher.group(group);
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                urls.add(url);
            }
        }
    }

    private String fetchContent(String url) {
        var links = props.getLinks();
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(links.getFetchTimeoutMs())
                    .userAgent(links.getUserAgent())
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
