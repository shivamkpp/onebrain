# Product Requirements Document (PRD)

- Product: OneBrain
- Document status: Proposed baseline for owner review
- Version: 0.1
- Last updated: 2026-11-01
- Authority: This document defines the initial product contract. Architecture documents must implement these requirements or record an explicit exception.

## 1. Executive summary

OneBrain is a shared organizational knowledge network. It allows an organization to publish knowledge from people and approved information sources, automatically organize that knowledge into typed entities and evidence-backed relationships, and retrieve relevant context for authorized people and AI tools.

The problem is not simply that organizations have too many documents. The problem is that useful knowledge is fragmented across systems and expressed in incompatible forms. A developer investigating a service may need to connect an ADR, a pull request, a past incident, a repository, and a discussion. Traditional search can find textually similar documents but often fails to explain how those sources relate, whether the information is still valid, and whether the requester is allowed to access it.

OneBrain adds a structured, traceable knowledge layer over existing systems. It does not replace those systems and does not claim that generated knowledge is automatically true.

## 2. Problem statement

Engineering organizations accumulate valuable knowledge in repositories, pull requests, design decisions, operational incidents, runbooks, and human-authored notes. That knowledge is fragmented, inconsistently labeled, duplicated, and frequently detached from the systems or decisions it describes.

As a result:
- Engineers spend time rediscovering decisions and debugging context that already exists.
- Knowledge held by one team is difficult for another team to find and apply.
- Search results lack reliable relationships between services, code, incidents, and decisions.
- Old or superseded guidance can be mistaken for current guidance.
- AI coding tools lack authorized, organization-specific context and may produce generic or incorrect suggestions.

**OneBrain's product problem:** provide a dependable way to publish, organize, and retrieve collective organizational knowledge across approved sources, with evidence, freshness, and access control attached to every result.

## 3. Product vision and positioning

**Vision:** an organization's useful knowledge should be reusable across people, teams, projects, and authorized AI tools without losing its source, context, or access boundaries.

**Positioning:** a knowledge organization and retrieval layer, not a chatbot, document repository, vector database wrapper, or autonomous agent.

**Core loop:**
1. Publish: a person or configured connector contributes a knowledge item or source reference.
2. Organize: OneBrain normalizes, classifies, indexes, resolves entities, and proposes evidence-backed relationships.
3. Retrieve: an authorized user or agent asks a question and receives ranked context with citations, relationship explanations, and freshness signals.
4. Correct: users can report incorrect, stale, duplicate, or missing context; maintainers review and update the knowledge record.

## 4. Goals

### 4.1 Product goals

- G1. Provide one searchable knowledge surface across a bounded set of engineering knowledge sources.
- G2. Represent important engineering concepts as typed entities (for example services, repositories, decisions, incidents, and teams).
- G3. Connect entities and knowledge items using explicit, evidence-backed relationships.
- G4. Return useful context with source citations, freshness, confidence, and permission filtering.
- G5. Make the knowledge layer usable by both a human-facing interface and AI coding clients through MCP.
- G6. Make retrieval quality measurable against a conventional keyword-plus-vector baseline.
- G7. Preserve human correction and review paths for uncertain or conflicting information.

### 4.2 Initial success criteria

The following are proposed acceptance targets for a controlled synthetic benchmark, not claims about achieved performance:
- At least 90% of benchmark queries return only authorized evidence.
- Zero unauthorized-content leakage in the permission test suite.
- At least 0.80 precision@5 on the curated initial benchmark, with every result traceable to source evidence.
- At least 0.80 recall@10 for explicitly linked entities in the curated relationship benchmark.
- At least 95% of returned source references resolve to a valid stored source record or configured external URL.
- A superseded decision is not ranked as current when a valid successor is known.
- Search API p95 latency target below 1.5 seconds on the documented local benchmark dataset, excluding external connector latency and optional LLM reranking.
- Every benchmark result is reported with dataset version, query set, configuration, and baseline comparison.

These targets must be revisited after a baseline is measured. If a target is not meaningful for the dataset size, document why rather than optimizing a vanity number.

## 5. Users and roles

### 5.1 Primary users

**Engineer / knowledge consumer**
- Searches for relevant architecture, service, code, incident, or decision context.
- Opens evidence and follows relationships to related knowledge.
- Reports stale or incorrect results.

**Engineer / knowledge contributor**
- Publishes a curated knowledge item.
- Links it to relevant services, repositories, decisions, or incidents.
- Updates or supersedes prior knowledge.

**Knowledge administrator**
- Configures source connections and scopes.
- Reviews extraction proposals and conflicts.
- Manages access mappings, retention, and connector health.

**AI coding client**
- Uses the MCP interface on behalf of an authenticated user.
- Requests context for a task and receives only authorized, cited results.
- Does not receive unrestricted database access or permission to mutate canonical knowledge by default.

### 5.2 Roles and permissions

| Capability | Consumer | Contributor | Admin | Agent |
|---|---:|---:|---:|---:|
| Search authorized knowledge | Yes | Yes | Yes | Yes, delegated |
| Read evidence user can access | Yes | Yes | Yes | Yes, delegated |
| Publish a knowledge item | No | Yes | Yes | No by default |
| Suggest correction / feedback | Yes | Yes | Yes | Limited, auditable |
| Approve extracted relationship | No | Optional | Yes | No |
| Configure connectors and ACL mappings | No | No | Yes | No |
| Delete or restrict source-derived content | No | No | Yes | No |

Authorization is not granted merely because an item is indexed. Agent access is delegated from a human or service identity and must be scoped.

## 6. Scope

### 6.1 Initial release (MVP)

The MVP is deliberately limited to one synthetic organization and a bounded source set.

**Knowledge publishing**
- Create, edit, archive, and view manually authored knowledge items.
- Support knowledge item types: note, architecture decision, service description, incident summary, operational procedure, and engineering guideline.
- Attach tags and typed entity references.
- Store author, created/updated timestamps, visibility scope, and lifecycle status.
- Preserve revision history for manually authored items.

**Source registration and ingestion**
- Register a source record and its type.
- Import synthetic fixtures for ADRs, repository metadata, pull requests, and incident reports.
- Normalize source metadata and content into a canonical ingestion envelope.
- Deduplicate using stable source identity and content fingerprints.
- Process ingestion asynchronously with retryable job states.
- Track connector/source sync status and ingestion errors.
- Preserve original source references; do not silently replace upstream sources.

**Knowledge organization**
- Extract typed entities from ingested content using a constrained schema.
- Support initial entity types: Organization, Team, Service, Repository, CodeReference, Decision, Incident, PullRequest, Person (synthetic only), and KnowledgeItem.
- Support initial relationship types: OWNS, DEPENDS_ON, IMPLEMENTED_IN, DISCUSSES, CAUSED, MITIGATED_BY, SUPERSEDES, RELATES_TO, and AUTHORED_BY.
- Store evidence spans or source references for each extracted fact and relationship.
- Store extraction method, confidence, and review status.
- Mark uncertain extraction as proposed/unverified; do not present it as confirmed fact.
- Allow authorized maintainers to accept, reject, or correct proposed relationships.
- Detect likely duplicates and surface them for review rather than destructive automatic merges.

**Search and retrieval**
- Keyword search and vector similarity search.
- Hybrid ranking with configurable weights.
- Optional bounded relationship expansion from high-confidence, authorized entities.
- Filters for entity type, source type, team/scope, lifecycle status, and freshness.
- Search results include title, short excerpt, source, relevant entities/relationships, confidence/review state, freshness, and access-safe citation.
- Detail view shows the original content or safe external reference and its provenance.
- Search results must exclude unauthorized records before ranking and relationship expansion.

**Knowledge lifecycle**
- Mark knowledge current, stale, disputed, archived, or superseded.
- Link successor decisions or replacement guidance.
- Show last verified time and source update time where available.
- Allow maintainers to trigger reprocessing after source updates.
- Preserve audit events for sensitive administrative and knowledge lifecycle actions.

**Human interface**
- Search page.
- Knowledge item detail page.
- Create/edit knowledge item flow.
- Entity detail page with related knowledge.
- Admin pages for source registration, ingestion status, and review queue.
- Feedback action for incorrect, stale, irrelevant, or missing context.

**Agent interface**
- MCP server exposing read-only retrieval tools.
- Tools operate under delegated identity/authorization context.
- Structured response schema with evidence and freshness metadata.
- No general-purpose SQL tool and no unrestricted raw database access.

**Evaluation**
- Versioned synthetic dataset and query set.
- Keyword-only and vector-only baselines.
- Hybrid retrieval evaluation.
- Relationship extraction and permission leakage tests.
- Reproducible latency and quality reports.

### 6.2 Explicitly out of scope for MVP

- Arbitrary autonomous agents or automated code changes.
- Writing back to GitHub, chat, ticketing, or incident systems.
- Broad production connector catalog.
- Real-time event streaming from every source.
- Automatic organization-wide crawling.
- Dedicated graph database.
- Microservices and Kubernetes.
- Multi-region active-active deployment.
- Enterprise billing, usage metering, or complex organization provisioning.
- Training or fine-tuning foundation models.
- Automatic approval of low-confidence facts.
- Ingesting personal communications or personal data.
- Public internet search as a knowledge source.
- Mobile application.

## 7. Key user journeys

### Journey A: Publish a durable decision
1. Contributor creates an architecture decision item.
2. Contributor provides title, status, context, decision, consequences, owner/team, and relevant services/repositories.
3. OneBrain validates required fields and saves a versioned item.
4. Indexing job creates search representations and entity links.
5. Contributor can inspect the resulting entity links and correct them.
6. Search can retrieve the decision with its current status and evidence.

**Acceptance:** the item is searchable, versioned, permission-scoped, and all displayed links resolve.

### Journey B: Ingest and organize a source
1. Admin registers a supported source or imports a fixture.
2. System validates source identity and scope.
3. Ingestion job normalizes metadata/content and deduplicates.
4. Extraction proposes entities and relationships with evidence.
5. High-confidence deterministic mappings may be accepted according to policy; uncertain LLM-derived relationships remain proposed.
6. Admin reviews proposals and errors.
7. Indexed knowledge becomes retrievable under the source's ACL.

**Acceptance:** retries do not create duplicate source records; every proposed relationship points to evidence; failed jobs are observable and retryable.

### Journey C: Find cross-system context
1. Engineer searches for a service, incident, decision, or task.
2. System identifies relevant lexical and semantic matches.
3. System expands through permitted, evidence-backed relationships within a bounded depth.
4. System ranks candidates and removes stale or superseded items according to lifecycle policy.
5. Engineer receives cited results and can inspect why each result is related.

**Acceptance:** results include evidence, currentness, and relationship rationale; no inaccessible content is exposed.

### Journey D: Agent requests task context
1. An MCP client connects with a delegated user identity.
2. Client calls a narrow retrieval tool with a task/question and optional service/repository hints.
3. OneBrain authorizes the caller and filters candidate records.
4. Retrieval returns a bounded, structured context package.
5. Each item includes source reference, evidence, freshness, and confidence.
6. The agent uses the returned context; OneBrain does not execute code or make autonomous changes.

**Acceptance:** the agent receives the same authorization boundary as the delegated user and can cite the returned sources.

### Journey E: Correct stale or incorrect knowledge
1. User flags an item or relationship as stale, incorrect, duplicate, or irrelevant.
2. Feedback is stored with actor, target, reason, and timestamp.
3. Authorized maintainer reviews the report.
4. Maintainer updates, rejects, supersedes, or reprocesses the item.
5. Audit history records the resolution.

**Acceptance:** feedback is traceable and does not silently mutate canonical knowledge.

## 8. Functional requirements

Priority definitions: P0 = required for MVP; P1 = valuable after core proof; P2 = later.

| ID | Priority | Requirement |
|---|---|---|
| FR-001 | P0 | Create, edit, view, and archive manually authored knowledge items. |
| FR-002 | P0 | Validate required fields and supported item types. |
| FR-003 | P0 | Preserve author, timestamps, revision history, and visibility scope. |
| FR-004 | P0 | Register and inspect supported source records. |
| FR-005 | P0 | Import synthetic ADR, repository, PR, and incident fixtures. |
| FR-006 | P0 | Normalize source content and metadata into a canonical envelope. |
| FR-007 | P0 | Deduplicate source records using source identity and content fingerprint. |
| FR-008 | P0 | Execute ingestion asynchronously with observable status and retry behavior. |
| FR-009 | P0 | Extract supported entity types into a validated schema. |
| FR-010 | P0 | Propose typed relationships with evidence, extraction method, and confidence. |
| FR-011 | P0 | Keep uncertain extracted relationships unverified until accepted by policy or reviewer. |
| FR-012 | P0 | Review, accept, reject, and correct proposed relationships. |
| FR-013 | P0 | Search using lexical and vector retrieval. |
| FR-014 | P0 | Combine candidate sets using hybrid ranking. |
| FR-015 | P0 | Enforce authorization before result exposure and graph expansion. |
| FR-016 | P0 | Return source citations, excerpts, freshness, and review status. |
| FR-017 | P0 | Represent stale, disputed, archived, and superseded lifecycle states. |
| FR-018 | P0 | Expose read-only MCP retrieval tools under delegated authorization. |
| FR-019 | P0 | Record audit events for admin, review, permission, and lifecycle actions. |
| FR-020 | P0 | Provide a versioned benchmark dataset and reproducible baseline evaluation. |
| FR-021 | P0 | Capture user feedback on retrieval quality and knowledge correctness. |
| FR-022 | P1 | Add a GitHub read-only connector for repositories, PRs, and ADR files. |
| FR-023 | P1 | Add scheduled incremental synchronization and source deletion handling. |
| FR-024 | P1 | Add configurable per-source extraction and relationship policies. |
| FR-025 | P1 | Add feedback-informed ranking experiments with offline evaluation. |
| FR-026 | P2 | Add additional documentation, issue tracker, and incident platform connectors. |
| FR-027 | P2 | Add organization-level analytics for knowledge coverage and freshness. |
| FR-028 | P2 | Add more advanced entity resolution and duplicate clustering. |

## 9. Non-functional requirements

### Security and privacy
- NFR-SEC-001: Every request must have an authenticated principal or explicit local-development identity.
- NFR-SEC-002: Authorization must be enforced server-side, not by hiding UI elements.
- NFR-SEC-003: Source ACLs must be mapped to OneBrain scopes; unknown or unmappable permissions fail closed.
- NFR-SEC-004: Relationship traversal must not reveal the existence or metadata of inaccessible nodes.
- NFR-SEC-005: Secrets must be supplied through environment/configuration secret mechanisms and never committed.
- NFR-SEC-006: Logs must not contain full sensitive source payloads or credentials.
- NFR-SEC-007: Administrative and knowledge lifecycle actions must be auditable.

### Reliability
- NFR-REL-001: Ingestion jobs must be idempotent.
- NFR-REL-002: Transient failures must be retryable with bounded exponential backoff.
- NFR-REL-003: Poison jobs must reach a terminal failed state with diagnostic metadata.
- NFR-REL-004: Search must remain available when background ingestion is delayed.
- NFR-REL-005: Source sync must not delete valid knowledge merely because a connector temporarily fails.

### Performance and scale
- NFR-PERF-001: Initial target dataset: 10,000 knowledge/source records and 100,000 entity/relationship records on a developer machine.
- NFR-PERF-002: Search p95 target below 1.5 seconds on the documented benchmark environment, excluding external calls and optional LLM reranking.
- NFR-PERF-003: Retrieval response must have configurable result and token budgets.
- NFR-PERF-004: Relationship expansion must have explicit depth and fan-out limits.
- NFR-PERF-005: Performance claims must include environment, dataset size, query set, and percentile.

### Maintainability and operability
- NFR-MNT-001: Modular monolith with explicit module boundaries.
- NFR-MNT-002: API contracts and persisted schemas must be versioned.
- NFR-MNT-003: Background jobs must expose status, attempt count, and failure reason.
- NFR-MNT-004: Structured logs and basic metrics must cover ingestion, extraction, and retrieval.
- NFR-MNT-005: Local development must be reproducible with documented setup.

### Explainability and data quality
- NFR-DQ-001: Every extracted relationship must have provenance.
- NFR-DQ-002: Confidence is not a substitute for evidence.
- NFR-DQ-003: Conflicting sources must be represented as conflicts, not silently collapsed.
- NFR-DQ-004: Superseded knowledge must remain inspectable but clearly labeled.
- NFR-DQ-005: LLM output must pass schema and policy validation before persistence.

## 10. Information architecture and canonical concepts

The product distinguishes:
- **Source:** external or imported origin (for example a repository, PR, ADR, incident record).
- **Knowledge item:** normalized, retrievable unit of knowledge.
- **Entity:** named concept such as a service, team, repository, decision, or incident.
- **Relationship:** typed link between entities or knowledge items, supported by evidence.
- **Evidence:** source location or text span that supports a fact or relationship.
- **Scope:** authorization boundary describing who can access an item.
- **Extraction proposal:** machine-generated candidate requiring validation or review.
- **Feedback:** user report about quality or correctness.
- **Ingestion job:** asynchronous processing record for a source revision.

Canonical source systems remain authoritative for their original records. OneBrain is authoritative only for its own normalized knowledge, relationships, review decisions, and retrieval metadata.

## 11. Retrieval behavior contract

- Interpret the user query and optional entity hints without assuming a single correct entity.
- Generate lexical and semantic candidate sets.
- Apply authorization filters before returning or expanding candidates.
- Resolve entity references and expand only approved, evidence-backed relationships.
- Rank by relevance, evidence quality, relationship distance, freshness, lifecycle status, and source trust.
- Deduplicate near-identical results while preserving distinct evidence.
- Return a bounded response with citations and reason codes.
- If evidence is weak or conflicting, say so; do not fabricate a definitive answer.
- If no relevant authorized context exists, return an explicit no-context result.

See [Retrieval Design](RETRIEVAL_DESIGN.md) for algorithmic detail.

## 12. Product analytics and feedback

MVP analytics are operational and quality-oriented, not employee surveillance:
- ingestion success/failure and processing duration;
- search latency and result counts;
- benchmark precision/recall and citation validity;
- feedback categories and resolution time;
- stale/superseded result incidence;
- authorization-denial and permission-test outcomes.

Do not build individual productivity scoring, engineer ranking, or employee monitoring.

## 13. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| LLM invents entities or links | Incorrect knowledge graph | Evidence-required extraction, schema validation, review state, benchmark |
| Stale decisions outrank current decisions | Bad engineering decisions | Supersession model, freshness features, explicit conflict display |
| Permission mismatch leaks content | Severe security failure | Fail-closed ACL mapping, authorization before expansion, adversarial tests |
| Vector search adds little value | Complexity without benefit | Compare against keyword baseline; keep retrieval components replaceable |
| Scope expands into generic enterprise search | MVP never completes | Bounded source types and explicit non-goals |
| Connectors are unreliable | Missing or duplicated knowledge | Idempotent sync, retry state, source identity, reconciliation |
| Graph database is premature | Operational burden | PostgreSQL relational edges first; measure traversal needs |
| AI dependency makes behavior unpredictable | Cost and quality variance | Deterministic parsing first, selective LLM use, cached/versioned extraction |

## 14. Open product decisions

These are intentionally not hidden behind assumptions:
1. Which identity provider and organization membership model should a production deployment support? MVP uses a simple local identity adapter; production SSO is deferred.
2. What is the exact source ACL mapping for each future connector? Connector-specific mapping must be designed before enabling that connector.
3. Which LLM provider/model is used for extraction? Keep provider behind an interface; select after comparing quality, cost, privacy, and structured-output reliability.
4. Should manual knowledge items support approval workflows beyond admin review? MVP uses contributor permissions plus review status for extracted relationships.
5. What retention and deletion guarantees are required for source content and derived embeddings? Define connector-specific behavior before real customer data is ingested.

## 15. MVP exit criteria

MVP is ready for implementation completion review only when:
- all P0 functional requirements have implementation and automated test evidence;
- the five user journeys work end to end against the synthetic organization;
- authorization tests show no cross-scope leakage, including relationship traversal;
- source citations resolve and point to supporting evidence;
- stale and superseded cases behave as specified;
- ingestion retries are idempotent and observable;
- MCP tools return bounded, structured, permission-filtered context;
- the benchmark report compares lexical, vector, and hybrid approaches;
- setup, architecture, limitations, and known risks are documented.

A feature is not complete merely because an endpoint or UI screen exists.
