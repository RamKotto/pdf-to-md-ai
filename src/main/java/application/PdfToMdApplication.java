package application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PdfToMdApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdfToMdApplication.class, args);
    }
}
