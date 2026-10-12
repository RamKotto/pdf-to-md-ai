package controller;

import config.AppProperties;
import dto.ConvertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import service.ConversionService;
import service.SelfDeletingResource;
import service.StorageService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversionController {

    private static final MediaType MARKDOWN_UTF8 =
            new MediaType("text", "markdown", StandardCharsets.UTF_8);

    private final ConversionService conversionService;
    private final StorageService storageService;
    private final AppProperties props;

    /**
     * Шаг 1. Загрузка PDF → конвертация → возврат ссылки на скачивание.
     */
    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConvertResponse> convert(@RequestParam("file") MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл пустой");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Требуется PDF файл");
        }

        long start = System.currentTimeMillis();
        String markdown = conversionService.pdfToMarkdown(file.getBytes());
        String id = storageService.save(markdown.getBytes(StandardCharsets.UTF_8), name);
        long elapsed = System.currentTimeMillis() - start;

        log.info("PDF '{}' ({} bytes) сконвертирован за {} мс", name, file.getSize(), elapsed);

        String url = props.getBaseUrl() + "/api/download/" + id;
        return ResponseEntity.ok(new ConvertResponse(url, props.getTtl().toMinutes() + " мин"));
    }

    /**
     * Шаг 2. Скачивание. Файл удаляется автоматически после отдачи клиенту.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        var stored = storageService.take(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Файл не найден или срок хранения истёк"));

        Resource resource = new SelfDeletingResource(stored.path());

        String filename = stored.path().getFileName().toString();
        int underscore = filename.indexOf('_');
        if (underscore > 0) {
            filename = filename.substring(underscore + 1);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MARKDOWN_UTF8)
                .body(resource);
    }
}
