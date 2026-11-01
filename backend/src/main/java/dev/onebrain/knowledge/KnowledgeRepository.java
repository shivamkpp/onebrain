package dev.onebrain.knowledge;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class KnowledgeRepository {
    private final JdbcClient jdbc;

    KnowledgeRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    void insert(KnowledgeItem item, UUID revisionId, String fingerprint) {
        jdbc.sql("""
                INSERT INTO knowledge_item
                    (id, organization_id, visibility_scope_id, item_type, lifecycle_status,
                     current_revision_id, created_by, created_at, updated_at)
                VALUES (:id, :org, :scope, 'NOTE', 'DRAFT', :revisionId, :author, :now, :now)
                """).param("id", item.id()).param("org", item.organizationId())
                .param("scope", item.visibilityScopeId()).param("revisionId", revisionId)
                .param("author", item.createdBy()).param("now", Timestamp.from(item.createdAt())).update();
        jdbc.sql("""
                INSERT INTO knowledge_revision
                    (id, knowledge_item_id, revision_number, title, body, source_title, source_url,
                     content_sha256, created_by, created_at)
                VALUES (:revisionId, :id, 1, :title, :body, :sourceTitle, :sourceUrl, :hash, :author, :now)
                """).param("revisionId", revisionId).param("id", item.id()).param("title", item.title())
                .param("body", item.body()).param("sourceTitle", item.source() == null ? null : item.source().title())
                .param("sourceUrl", item.source() == null ? null : item.source().url())
                .param("hash", fingerprint).param("author", item.createdBy())
                .param("now", Timestamp.from(item.createdAt())).update();
    }

    Optional<KnowledgeItem> findReadable(UUID id, String principal) {
        // Authorization is part of the query: inaccessible content never reaches application code.
        return jdbc.sql("""
                SELECT k.*, s.owner_team_id, r.title, r.body, r.source_title, r.source_url, r.revision_number
                FROM knowledge_item k
                JOIN knowledge_revision r ON r.id = k.current_revision_id AND r.knowledge_item_id = k.id
                JOIN visibility_scope s ON s.id = k.visibility_scope_id AND s.organization_id = k.organization_id
                JOIN principal_account p ON p.organization_id = k.organization_id AND p.name = :principal
                JOIN resource_grant g ON g.organization_id = k.organization_id AND g.scope_id = s.id
                    AND g.principal_name = p.name AND g.action = 'READ'
                WHERE k.id = :id
                """).param("id", id).param("principal", principal).query((rs, row) ->
                new KnowledgeItem(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
                        rs.getObject("visibility_scope_id", UUID.class), rs.getObject("owner_team_id", UUID.class),
                        rs.getString("item_type"), rs.getString("lifecycle_status"),
                        rs.getString("title"), rs.getString("body"),
                        rs.getString("source_url") == null ? null :
                                new SourceReference(rs.getString("source_title"), rs.getString("source_url")),
                        rs.getInt("revision_number"), rs.getString("created_by"),
                        rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()))
                .optional();
    }
}
