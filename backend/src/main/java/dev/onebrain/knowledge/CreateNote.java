package dev.onebrain.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateNote(@NotBlank @Size(max = 200) String title,
                         @NotBlank @Size(max = 20000) String body,
                         @NotNull UUID visibilityScopeId,
                         @Valid SourceReference source) {}
