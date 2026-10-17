package application.service;

import application.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AppProperties props;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(props.getStorage().getDirectory());
            log.info("Директория для MD: {}", props.getStorage().getDirectory().toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось создать директорию", e);
        }
    }

    public SavedFile save(String markdown, String originalName) {
        String id = UUID.randomUUID().toString();
        String safeBase = sanitize(stripExtension(originalName));
        String filename = id + "_" + safeBase + ".md";
        Path target = props.getStorage().getDirectory().resolve(filename);

        try {
            Files.writeString(target, markdown);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить " + target, e);
        }
        log.info("Сохранён MD: {} ({} символов)", target.getFileName(), markdown.length());
        return new SavedFile(id, filename, target);
    }

    public record SavedFile(String id, String filename, Path path) {}

    private String stripExtension(String name) {
        if (name == null) return "document";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String sanitize(String name) {
        String cleaned = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return cleaned.isBlank() ? "document" : cleaned;
    }
}
