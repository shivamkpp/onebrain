# Data Model

- Status: Proposed baseline
- Primary store: PostgreSQL; vector extension: pgvector
- This is a logical model. Physical schema and indexes are finalized during implementation with migration review.

## 1. Modeling principles

1. Preserve upstream source identity separately from OneBrain knowledge identity.
2. Keep entities and relationships typed and versionable.
3. Make provenance mandatory for extracted facts and relationships.
4. Model access scope explicitly and enforce it in every read path.
5. Preserve history for decisions, revisions, and review actions.
6. Treat embeddings as derived indexes, never canonical content.
7. Keep source snapshots immutable by revision where retention policy permits.

## 2. Core entities

### Organization
Represents the initial synthetic organization and future customer boundary. Fields: id, name, status, created_at.

### Team
Fields: id, organization_id, name, description, lifecycle_status.

### SourceInstallation
A configured connector or fixture source. Fields: id, organization_id, connector_type, display_name, configuration_reference, scope_mapping_version, sync_cursor, status, created_at, updated_at.

### SourceRecord
One upstream record identity. Fields: id, source_installation_id, external_id, source_type, canonical_url, current_revision_id, source_acl_snapshot, last_seen_at, deleted_at, created_at, updated_at. Unique key: source_installation_id + external_id.

### SourceRevision
Immutable or append-only snapshot metadata. Fields: id, source_record_id, revision_key, content_fingerprint, title, normalized_content_location/content, upstream_created_at, upstream_updated_at, fetched_at, parser_version, status. Unique key: source_record_id + revision_key.

For MVP, source content may be stored in PostgreSQL. A future deployment may store large raw payloads in object storage with database metadata and retention controls.

### KnowledgeItem
Retrievable normalized knowledge. Fields: id, organization_id, item_type, title, body, lifecycle_status, owner_team_id, visibility_scope_id, current_revision_id, created_by, created_at, updated_at, last_verified_at, archived_at.

### KnowledgeRevision
Fields: id, knowledge_item_id, revision_number, title, body, structured_fields JSONB, content_fingerprint, created_by, created_at, change_reason. Unique key: knowledge_item_id + revision_number.

### Entity
Fields: id, organization_id, entity_type, canonical_name, normalized_name, description, lifecycle_status, created_at, updated_at. Uniqueness is scoped by organization and entity type only where deterministic identity rules permit; aliases and candidate matches handle ambiguity.

### EntityAlias
Fields: id, entity_id, alias, normalized_alias, source, confidence, status. Alias collisions must not auto-merge distinct entities.

### Relationship
Fields: id, organization_id, from_entity_id, relationship_type, to_entity_id, status, confidence, created_at, updated_at, valid_from, valid_to, supersedes_relationship_id. A uniqueness rule may prevent duplicate accepted edges with the same endpoints/type, but evidence and time validity must be considered.

### Evidence
Fields: id, organization_id, source_revision_id (nullable for manual source), knowledge_revision_id (nullable), locator_type, locator_value, excerpt, content_hash, start_offset, end_offset, created_at. Evidence locators may be a URL, JSON pointer, line range, or text span. At least one origin reference is required.

### RelationshipEvidence
Join table from relationship/proposal to one or more evidence records. Extracted relationships require at least one supporting evidence record.

### ExtractionRun
Fields: id, source_revision_id, pipeline_version, model_provider, model_name, prompt_version, schema_version, started_at, completed_at, status, safe_error_code, token_usage, cost_estimate.

### ExtractionProposal
Fields: id, extraction_run_id, proposal_type, proposed_payload JSONB, confidence, status, reviewer_id, reviewed_at, review_reason, created_at. Proposals can represent candidate entities, aliases, or relationships.

### VisibilityScope / ResourceGrant
VisibilityScope defines a named access boundary. ResourceGrant maps principal/team/group to actions within a scope. Source ACL mapping must be explicit and versioned. Unknown mapping means deny.

### IngestionJob
Fields: id, source_installation_id, source_record_id, source_revision_id, idempotency_key, stage, status, attempt_count, next_attempt_at, lease_owner, lease_expires_at, last_error_code, created_at, updated_at, completed_at. Unique idempotency_key.

### Feedback
Fields: id, organization_id, actor_id, target_type, target_id, category, comment, status, created_at, resolved_by, resolved_at, resolution.

### AuditEvent
Fields: id, organization_id, actor_id, action, target_type, target_id, correlation_id, safe_metadata JSONB, created_at. Append-only for application-level audit.

### Embedding
Fields: id, target_type, target_id, revision_id, model_provider, model_name, dimensions, vector, content_fingerprint, created_at. Unique by target/revision/model version.

## 3. Relationship model

Initial relationship types:
- OWNS: Team -> Service or Repository.
- DEPENDS_ON: Service -> Service.
- IMPLEMENTED_IN: Service or Feature -> Repository/CodeReference.
- DISCUSSES: PR/Incident/Decision -> Service/Repository/CodeReference.
- CAUSED: Incident -> Service/Change/FailureMode (only where evidence supports causality).
- MITIGATED_BY: Incident -> Change/Runbook/Decision.
- SUPERSEDES: Decision/KnowledgeItem -> prior Decision/KnowledgeItem.
- RELATES_TO: broad association; should be used sparingly and carry an explanation.
- AUTHORED_BY: KnowledgeItem/Decision -> Person (synthetic-only in initial data).

The ontology is versioned. Adding a relationship type requires definition, valid endpoint types, directionality, evidence rules, and retrieval semantics.

## 4. Lifecycle states

Knowledge item:
- DRAFT: not available in ordinary search.
- PROCESSING: saved, indexing/extraction pending.
- ACTIVE: retrievable if authorized.
- STALE: still inspectable, freshness warning applied.
- DISPUTED: known conflict or correctness concern.
- SUPERSEDED: replaced by a successor; historical retrieval only when relevant.
- ARCHIVED: excluded from default search, retrievable by explicit authorized filter.

Relationship:
- PROPOSED: machine-generated or user-suggested, not canonical.
- ACCEPTED: reviewed or deterministically accepted under policy.
- REJECTED: reviewed and rejected.
- SUPERSEDED: replaced by a newer edge or fact.
- EXPIRED: no longer valid after valid_to.

Source record:
- ACTIVE, SYNC_PENDING, SYNC_FAILED, DELETED, ACCESS_MAPPING_FAILED.

Job:
- PENDING, RUNNING, RETRY_WAIT, SUCCEEDED, FAILED, CANCELLED.

## 5. Provenance requirements

- Manual knowledge: author, revision, and optional supporting source references.
- Imported source: source installation, external ID, revision key, fetch time, canonical URL.
- Extracted entity: extraction run and evidence reference.
- Extracted relationship: extraction run, confidence, evidence, review status.
- Human correction: reviewer, timestamp, reason, prior value.
- Derived embedding: content fingerprint and embedding model/version.

An LLM-generated relationship without a valid evidence locator must be rejected or stored as an ungrounded extraction error, not as a relationship.

## 6. Authorization model

Initial model uses organization + visibility scope + grants:
- organization boundary is mandatory;
- scope may represent team, project, or explicitly shared knowledge;
- principals receive actions through grants;
- source-derived content inherits the mapped source ACL;
- a derived entity/relationship must not broaden access beyond its evidence.

When a result aggregates evidence from multiple sources, it is visible only if the requesting principal can read the evidence necessary to support the returned claim. Do not expose entity names, counts, or relationship existence if doing so leaks restricted source information.

## 7. Deletion and retention

- Source deletion/tombstone marks source record deleted and triggers policy-defined removal or suppression of derived content.
- A connector outage or missing page is not a tombstone.
- Deleting a source revision must invalidate derived embeddings and evidence locators.
- Manual knowledge archival is distinct from hard deletion.
- Audit events are retained according to a future deployment policy; MVP records them for traceability.
- Exact retention, backup, and erasure guarantees are open until production data policy is defined.

## 8. Indexing and database constraints

Expected indexes:
- B-tree on organization_id, lifecycle_status, updated_at.
- Unique on source_installation_id + external_id.
- Unique on source_record_id + revision_key.
- Unique on knowledge_item_id + revision_number.
- B-tree on entity_type + normalized_name.
- B-tree on relationship from/type/to/status.
- GIN for JSONB structured fields where justified.
- Full-text GIN index for normalized search text.
- pgvector index selected after benchmark (exact search first for small dataset; approximate index only when needed).
- Index on ingestion_job status + next_attempt_at + lease_expires_at.
- Index on visibility scope/grants used by authorization predicates.

Use migrations and verify indexes against actual query plans; do not create every proposed index blindly.
