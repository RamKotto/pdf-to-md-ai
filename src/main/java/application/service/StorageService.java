package application.service;

import application.config.AppProperties;
import application.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9]+$");

    private final AppProperties props;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(props.getStorage().getDirectory());
            log.info("Директория для MD: {}", props.getStorage().getDirectory().toAbsolutePath());
        } catch (IOException e) {
            throw new StorageException("Не удалось создать директорию", e);
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
            throw new StorageException("Не удалось сохранить " + target, e);
        }
        log.info("Сохранён MD: {} ({} символов)", target.getFileName(), markdown.length());
        String downloadUrl = props.getStorage().getBaseUrl() + "/api/files/" + filename;
        return new SavedFile(id, filename, target, downloadUrl);
    }

    public Resource load(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("Некорректное имя файла");
        }
        Path target = props.getStorage().getDirectory().resolve(filename).normalize();
        if (!target.startsWith(props.getStorage().getDirectory())) {
            throw new IllegalArgumentException("Некорректный путь");
        }
        if (!Files.exists(target)) {
            throw new IllegalArgumentException("Файл не найден: " + filename);
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            throw new StorageException("Некорректный путь к файлу", e);
        }
    }

    public record SavedFile(String id, String filename, Path path, String downloadUrl) {
    }

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
