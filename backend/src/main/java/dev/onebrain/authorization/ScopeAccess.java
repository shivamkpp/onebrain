package dev.onebrain.authorization;

import dev.onebrain.api.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class ScopeAccess {
    private final JdbcClient jdbc;

    public ScopeAccess(JdbcClient jdbc) { this.jdbc = jdbc; }

    public record Scope(UUID id, UUID organizationId, UUID ownerTeamId, String name, boolean canContribute) {}

    public List<Scope> readableScopes(String principal) {
        return jdbc.sql("""
                SELECT s.id, s.organization_id, s.owner_team_id, s.name,
                    EXISTS (SELECT 1 FROM resource_grant w
                            WHERE w.organization_id = s.organization_id AND w.scope_id = s.id
                              AND w.principal_name = :principal AND w.action = 'CONTRIBUTE') AS can_contribute
                FROM visibility_scope s
                JOIN principal_account p ON p.organization_id = s.organization_id AND p.name = :principal
                JOIN resource_grant g ON g.organization_id = s.organization_id AND g.scope_id = s.id
                    AND g.principal_name = p.name AND g.action = 'READ'
                ORDER BY s.name, s.id
                """).param("principal", principal).query(Scope.class).list();
    }

    public Scope requireContribution(String principal, UUID scopeId) {
        return readableScopes(principal).stream()
                .filter(scope -> scope.id().equals(scopeId) && scope.canContribute())
                .findFirst().orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "This action is not permitted."));
    }
}
