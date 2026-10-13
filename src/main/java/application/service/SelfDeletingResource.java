package application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resource, который удаляет файл после того, как Spring закроет поток.
 * Это гарантирует удаление после фактической отдачи клиенту.
 */
@Slf4j
public class SelfDeletingResource extends FileSystemResource {

    private final Path path;

    public SelfDeletingResource(Path path) {
        super(path);
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream delegate = super.getInputStream();
        return new FilterInputStream(delegate) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    try {
                        Files.deleteIfExists(path);
                        log.info("Удалён после скачивания: {}", path.getFileName());
                    } catch (IOException e) {
                        log.warn("Не удалось удалить файл после скачивания: {}", path, e);
                    }
                }
            }
        };
    }
}
