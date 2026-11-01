package dev.onebrain.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;

public record SourceReference(@NotBlank @Size(max = 200) String title,
                              @NotBlank @Size(max = 2048) String url) {
    @AssertTrue(message = "Source must be an absolute HTTP(S) URL without credentials")
    @JsonIgnore
    public boolean isValidUrl() {
        if (url == null) return false;
        try {
            URI uri = URI.create(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
