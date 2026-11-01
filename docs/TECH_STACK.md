# Technology Stack and Rationale

- Status: Proposed baseline for owner review
- Principle: choose boring, well-supported technology that matches the product's consistency and retrieval needs. Pin exact dependency versions when implementation begins, after checking current supported releases.

## Recommended stack

| Layer | Choice | Why |
|---|---|---|
| Primary language | Java | Strong fit for domain-heavy backend, concurrency, mature tooling, and the owner's existing experience |
| Backend framework | Spring Boot | Mature HTTP, validation, dependency injection, security integration, metrics, and test ecosystem |
| API style | REST/JSON, versioned under /api/v1 | Stable contract for web UI and integrations |
| Agent interface | MCP server adapter | Standard tool interface for compatible AI clients; shares application services with REST |
| Frontend | React + TypeScript | Component ecosystem, type-safe API integration, and familiar web stack |
| Primary database | PostgreSQL | Transactions, constraints, JSONB, full-text search, and operational maturity |
| Vector search | pgvector | Keeps embeddings and authorization metadata in the same database initially |
| Background work | PostgreSQL-backed job table | Idempotency, leases, retries, and status without a separate broker |
| Database migrations | Versioned SQL migrations (Flyway or equivalent) | Explicit, reviewable schema evolution |
| API documentation | OpenAPI | Contract visibility and client generation/testing |
| Testing | JUnit 5, Spring test support, Testcontainers; frontend unit/component tests | Unit, integration, persistence, and contract coverage |
| Local environment | Docker Compose for PostgreSQL; app processes run locally | Low-friction development and reproducibility |
| Observability | Structured logs, Micrometer metrics, health/readiness endpoints | Fits Spring ecosystem and avoids premature telemetry platform |
| Build | Maven or Gradle; choose one at implementation start | Both are viable; prefer team familiarity and reproducible dependency locking |

## Decisions and boundaries

### Java and Spring Boot
Use a currently supported LTS JDK and a Spring Boot release compatible with it. Pin versions in the build and document upgrade policy. Do not choose a framework version based on an unverified blog post or copy a stale tutorial's dependency set.

Backend modules should be organized by domain capability, not technical layer alone. Keep controllers thin; application services own use cases; domain types express invariants; infrastructure adapters own SQL, external providers, and MCP transport.

### React and TypeScript
Use TypeScript in strict mode. Keep API response types generated from or checked against OpenAPI where practical. Avoid duplicating business rules in the browser; the server is authoritative for permissions, validation, lifecycle, and ranking.

### PostgreSQL and pgvector
Use PostgreSQL for canonical records, relational edges, revisions, scopes, job state, and audit. pgvector is a derived retrieval index. Benchmark exact vector search before enabling approximate indexes. Use full-text search for lexical retrieval and RRF for initial hybrid merging.

### LLM provider abstraction
Do not couple domain logic to one model vendor. Define a narrow extraction interface that accepts bounded input and returns a versioned structured proposal. Provider selection remains open until a representative synthetic benchmark compares output quality, schema reliability, latency, cost, and data handling.

### MCP implementation
Use an MCP SDK compatible with the selected Java runtime and the current protocol specification at implementation time. MCP remains a transport adapter; no domain logic or direct database access belongs in tool handlers.

### Deferred infrastructure
Do not add Kafka, Redis, Neo4j, a separate vector database, Kubernetes, service mesh, or cloud-specific managed services to the MVP without measured need. The architecture should permit replacement behind interfaces but should not build speculative abstractions.

## Implementation-time version policy

- Select currently supported stable/LTS releases.
- Pin all direct dependencies and commit lock/build metadata.
- Enable automated dependency update review.
- Check license compatibility and known vulnerabilities.
- Document JDK, Node.js, PostgreSQL, pgvector, and container versions.
- Avoid preview language/runtime features in the first release.
