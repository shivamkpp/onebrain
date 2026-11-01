package dev.onebrain.knowledge;

import dev.onebrain.api.ApiException;
import dev.onebrain.audit.AuditLog;
import dev.onebrain.authorization.ScopeAccess;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {
    private final KnowledgeRepository repository;
    private final ScopeAccess access;
    private final AuditLog audit;

    public KnowledgeService(KnowledgeRepository repository, ScopeAccess access, AuditLog audit) {
        this.repository = repository;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public KnowledgeItem create(String principal, CreateNote input, UUID requestId) {
        var scope = access.requireContribution(principal, input.visibilityScopeId());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var item = new KnowledgeItem(UUID.randomUUID(), scope.organizationId(), scope.id(), scope.ownerTeamId(),
                "NOTE", "DRAFT", input.title().strip(), input.body(), input.source(), 1, principal, now, now);
        repository.insert(item, UUID.randomUUID(), fingerprint(item));
        audit.knowledgeCreated(scope.organizationId(), principal, item.id(), requestId, now);
        return item;
    }

    @Transactional(readOnly = true)
    public KnowledgeItem get(String principal, UUID id) {
        return repository.findReadable(id, principal).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Knowledge item not found."));
    }

    private String fingerprint(KnowledgeItem item) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Length-prefix each component to keep the fingerprint unambiguous.
            for (String value : new String[]{item.title(), item.body(),
                    item.source() == null ? "" : item.source().title(),
                    item.source() == null ? "" : item.source().url()}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
