package application.controller;

import application.dto.ConvertResponse;
import application.dto.ConvertUrlRequest;
import application.service.ConversionService;
import application.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final StorageService storageService;

    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConvertResponse> convertFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Требуется PDF-файл");
        }
        String markdown = conversionService.pdfToMarkdown(file.getBytes(), name);
        var saved = storageService.save(markdown, name);
        return ResponseEntity.ok(toResponse(saved, markdown));
    }

    @PostMapping(value = "/convert", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConvertResponse> convertUrl(@Valid @RequestBody ConvertUrlRequest request) {
        String filename = (request.filename() == null || request.filename().isBlank())
                ? "document"
                : request.filename();
        String markdown = conversionService.urlToMarkdown(request.url(), filename);
        var saved = storageService.save(markdown, filename);
        return ResponseEntity.ok(toResponse(saved, markdown));
    }

    private ConvertResponse toResponse(StorageService.SavedFile saved, String markdown) {
        return new ConvertResponse(saved.id(), saved.filename(), saved.downloadUrl(), markdown);
    }
}
