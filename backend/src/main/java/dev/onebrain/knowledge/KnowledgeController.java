package dev.onebrain.knowledge;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) { this.service = service; }

    @PostMapping
    ResponseEntity<KnowledgeItem> create(Principal principal, @Valid @RequestBody CreateNote input,
                                         @RequestAttribute UUID requestId) {
        KnowledgeItem item = service.create(principal.getName(), input, requestId);
        return ResponseEntity.created(URI.create("/api/v1/knowledge/" + item.id())).body(item);
    }

    @GetMapping("/{id}")
    KnowledgeItem get(Principal principal, @PathVariable UUID id) {
        return service.get(principal.getName(), id);
    }
}
