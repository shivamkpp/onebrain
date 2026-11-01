# Architecture Decision Records

ADRs document decisions that materially affect architecture, data, security, dependencies, or delivery. Accepted decisions should not be silently changed; supersede them with a new ADR.

## Status definitions
- Proposed: recommendation awaiting owner review.
- Accepted: selected for the initial implementation.
- Rejected: considered and not selected.
- Superseded: replaced by a later decision.

## Index

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-modular-monolith.md) | Start with a modular monolith | Proposed |
| [0002](0002-postgresql-pgvector.md) | PostgreSQL + pgvector as initial datastore | Proposed |
| [0003](0003-evidence-backed-knowledge.md) | Evidence and provenance are mandatory | Proposed |
| [0004](0004-selective-llm-extraction.md) | LLM extraction is constrained and reviewable | Proposed |
| [0005](0005-mcp-as-adapter.md) | MCP is an adapter over application services | Proposed |
| [0006](0006-postgres-job-queue.md) | Use a PostgreSQL job table initially | Proposed |
| [0007](0007-technology-stack.md) | Java/Spring Boot, React/TypeScript, PostgreSQL | Proposed |
| [0008](0008-foundation-milestone.md) | Scoped manual-note foundation | Accepted |

The owner approved the foundation milestone in ADR 0008. Earlier proposals describe the broader intended product and retain their review status.
