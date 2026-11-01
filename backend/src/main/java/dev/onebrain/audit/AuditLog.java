package dev.onebrain.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AuditLog {
    private final JdbcClient jdbc;

    public AuditLog(JdbcClient jdbc) { this.jdbc = jdbc; }

    public void knowledgeCreated(UUID organizationId, String actor, UUID itemId, UUID requestId, Instant now) {
        jdbc.sql("""
                INSERT INTO audit_event (id, organization_id, actor_name, action, target_id, request_id, created_at)
                VALUES (:id, :org, :actor, 'KNOWLEDGE_CREATED', :item, :request, :now)
                """).param("id", UUID.randomUUID()).param("org", organizationId).param("actor", actor)
                .param("item", itemId).param("request", requestId).param("now", Timestamp.from(now)).update();
    }
}
