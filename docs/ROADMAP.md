# Product and Engineering Roadmap

- Status: Proposed sequence
- This roadmap is outcome-based. Dates are intentionally omitted until the owner confirms available time and project constraints.

## Phase 0 — Product and architecture review
**Outcome:** one coherent, reviewed specification.
- Review PRD problem statement, personas, scope, and acceptance criteria.
- Review HLD/LLD and data model.
- Resolve open product/security decisions.
- Accept/revise ADRs.
- Freeze MVP boundary.

**Exit:** owner approves PRD v1 and architecture baseline.

## Phase 1 — Foundation
**Outcome:** reproducible local system with secure core boundaries.
- Repository/build conventions and configuration.
- PostgreSQL schema and migrations.
- Identity and authorization abstractions.
- Knowledge item CRUD and revisioning.
- Audit model and API error conventions.
- Basic web navigation and health checks.

**Exit:** manually authored knowledge can be created, scoped, read, revised, and audited.

## Phase 2 — Ingestion and organization
**Outcome:** synthetic sources become normalized knowledge and reviewable graph proposals.
- Canonical source envelope and source identity.
- PostgreSQL job queue, leases, retries, idempotency.
- Synthetic fixture importer.
- Deterministic parsers and schema validation.
- Entity and relationship proposal persistence.
- Review workflow and provenance.
- Indexing and embeddings.

**Exit:** source fixtures are ingested reproducibly; every extracted edge has evidence and a review state.

## Phase 3 — Retrieval
**Outcome:** useful evidence-backed search.
- Keyword baseline.
- Vector retrieval.
- Hybrid RRF.
- ACL-aware candidate generation.
- Bounded relationship expansion.
- Freshness/supersession handling.
- Result explanations and citation UI.
- Benchmark runner and fixed query set.

**Exit:** benchmark comparison shows whether relationship-aware retrieval adds value; permission tests pass.

## Phase 4 — MCP and human workflows
**Outcome:** humans and compatible agents use the same retrieval contract.
- MCP read-only tools.
- Delegated identity adapter.
- Context response schema and token budgets.
- Search, entity, and knowledge detail screens.
- Feedback and review queue.
- MCP contract and security tests.

**Exit:** MCP retrieval returns the same permission-filtered, cited context as REST.

## Phase 5 — Read-only GitHub integration
**Outcome:** first real connector, only after synthetic pipeline is stable.
- Read-only repository/PR/ADR ingestion.
- Scope/permission mapping.
- Incremental sync and tombstones.
- Rate limits and retry behavior.
- Connector health and reconciliation.

**Exit:** no permission widening; source updates and deletions reconcile predictably.

## Phase 6 — Evidence-led expansion
Only add capabilities supported by user need and measurements:
- additional documentation/issue/incident connectors;
- organization-wide freshness analytics;
- advanced entity resolution;
- feedback-informed ranking;
- production SSO and deployment hardening;
- dedicated queue/vector/graph infrastructure only if measured triggers justify it.

## Explicit sequencing rule

Do not begin with GitHub integration, agent autonomy, or complex infrastructure. First prove the core lifecycle using synthetic data: publish -> organize -> retrieve -> cite -> correct.
