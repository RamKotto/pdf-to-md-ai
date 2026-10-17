package application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConvertUrlRequest(
        @NotBlank
        @Pattern(regexp = "^https?://.*", message = "URL должен начинаться с http:// или https://")
        String url,
        String filename
) {
}
