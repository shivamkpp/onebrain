package dev.onebrain.knowledge;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeItem(UUID id, UUID organizationId, UUID visibilityScopeId, UUID ownerTeamId,
                            String itemType, String lifecycleStatus, String title, String body,
                            SourceReference source, int revision, String createdBy,
                            Instant createdAt, Instant updatedAt) {}
