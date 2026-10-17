package application.service;

import application.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private static final Pattern MULTIPLE_BLANK_LINES = Pattern.compile("\\n{3,}");
    private final OpenRouterClient openRouter;
    private final LinkResolverService linkResolver;
    private final AppProperties props;

    public String pdfToMarkdown(byte[] pdfBytes, String filename) {
        if (pdfBytes == null || pdfBytes.length == 0) throw new IllegalArgumentException("Пустой PDF");
        return processResult(openRouter.convertPdfFile(pdfBytes, filename));
    }

    public String urlToMarkdown(String url, String filename) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("URL не указан");
        String lower = url.toLowerCase();
        boolean looksLikePdf = lower.endsWith(".pdf") || lower.contains("format=pdf");
        String markdown = looksLikePdf
                ? openRouter.convertPdfUrl(url, filename != null ? filename : "document.pdf")
                : openRouter.convertTextUrl(url, filename != null ? filename : "document.md");
        return processResult(markdown);
    }

    private String processResult(String markdown) {
        String cleaned = MULTIPLE_BLANK_LINES.matcher(markdown).replaceAll("\n\n").strip() + "\n";
        if (props.getLinks().isEnabled()) {
            cleaned = linkResolver.enrichWithLinkedContent(
                    cleaned,
                    props.getLinks().getMaxLinks(),
                    props.getLinks().getMaxCharsPerLink()
            );
        }
        return cleaned;
    }
}
