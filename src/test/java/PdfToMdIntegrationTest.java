import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.llm.enabled=false",
        "spring.docker.compose.enabled=false"
})
@AutoConfigureMockMvc
class PdfToMdIntegrationTest {

    @Autowired
    MockMvc mockMvc;

//    @Test
    void convertsAndDeletesAfterDownload() throws Exception {
        byte[] pdf = createPdf("Hello PDF to MD application.service");

        // 1. Конвертация
        String response = mockMvc.perform(multipart("/api/convert")
                        .file(new MockMultipartFile(
                                "file", "test.pdf", "application/pdf", pdf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andReturn().getResponse().getContentAsString();

        String url = response.split("\"downloadUrl\":\"")[1].split("\"")[0];
        String id = url.substring(url.lastIndexOf('/') + 1);

        // 2. Первое скачивание — успех
        mockMvc.perform(get("/api/download/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello")));

        // 3. Повторное скачивание — 404 (файл удалён)
        mockMvc.perform(get("/api/download/" + id))
                .andExpect(status().isNotFound());
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }

            doc.save(out);
            return out.toByteArray();
        }
    }
}
