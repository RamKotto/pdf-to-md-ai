package service;

import config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AppProperties props;
    private final Map<String, StoredFile> store = new ConcurrentHashMap<>();

    public record StoredFile(String id, Path path, Instant createdAt, Instant expiresAt) {
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(props.getDirectory());
        log.info("Хранилище инициализировано: {}", props.getDirectory());
    }

    public String save(byte[] content, String originalName) {
        String id = UUID.randomUUID().toString();
        String baseName = originalName.replaceAll("(?i)\\.pdf$", "");
        String safeName = sanitize(baseName);
        Path target = props.getDirectory().resolve(id + "_" + safeName + ".md");

        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить MD файл", e);
        }

        Instant now = Instant.now();
        store.put(id, new StoredFile(id, target, now, now.plus(props.getTtl())));
        log.info("Сохранён файл: {} (id={}, {} bytes)",
                target.getFileName(), id, content.length);
        return id;
    }

    public Optional<StoredFile> take(String id) {
        StoredFile file = store.remove(id);
        if (file == null) {
            return Optional.empty();
        }
        if (file.expiresAt().isBefore(Instant.now())) {
            deleteQuietly(file.path());
            return Optional.empty();
        }
        return Optional.of(file);
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
            log.info("Удалён файл: {}", path.getFileName());
        } catch (IOException e) {
            log.warn("Не удалось удалить файл {}", path, e);
        }
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void cleanupExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> {
            StoredFile f = entry.getValue();
            if (f.expiresAt().isBefore(now)) {
                deleteQuietly(f.path());
                return true;
            }
            return false;
        });
    }

    public int size() {
        return store.size();
    }

    private String sanitize(String name) {
        String cleaned = name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return cleaned.isBlank() ? "document" : cleaned;
    }
}
