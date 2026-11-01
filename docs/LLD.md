# Low-Level Design (LLD)

- Status: Proposed baseline
- Scope: module responsibilities, contracts, state transitions, and failure semantics
- This document describes intended design; it is not implemented code.

## 1. Design conventions

- IDs are opaque UUIDs at API boundaries.
- Timestamps are UTC instants; store source-local timestamps separately when relevant.
- API payloads use explicit versioned schemas.
- Database transactions protect canonical state changes and their outbox/job records.
- External source IDs are namespaced by connector and installation.
- All user-controlled text and source content are untrusted input.
- Every use case receives an explicit principal and authorization context.

## 2. Application modules

### Identity
Responsibilities:
- Resolve authenticated subject from local development identity or future identity provider.
- Represent user, service principal, and delegated agent identity.
- Produce immutable request principal context.

Does not:
- Decide resource visibility alone.
- Trust identity claims supplied directly by an MCP tool argument.

### Authorization
Responsibilities:
- Evaluate actions against scopes and resource ACLs.
- Map source ACLs into OneBrain access scopes.
- Provide reusable predicates for retrieval queries.
- Fail closed when mapping is missing or ambiguous.

Policy operations:
- `canRead(principal, resource)`
- `canContribute(principal, scope)`
- `canReview(principal, proposal)`
- `canAdminister(principal, organization)`

### Sources
Responsibilities:
- Store connector installation and source identity metadata.
- Define source adapter contract.
- Track source revision, upstream URL, sync cursor, and source status.
- Never store long-lived plaintext credentials in ordinary source rows.

Adapter contract (conceptual):
- `validateConfiguration(config)`
- `fetchChanges(cursor, scope)`
- `fetchRecord(externalId)`
- `normalize(rawRecord)`
- `mapAccess(rawAcl)`

Adapters must return stable external identifiers and explicit deletion/tombstone semantics. A transient missing record is not a deletion signal.

### Ingestion
Responsibilities:
- Accept source snapshots and create idempotent processing jobs.
- Coordinate retries, leases, and terminal states.
- Persist normalized source envelopes.
- Publish follow-up work through the job table.

Job states:
`PENDING -> RUNNING -> SUCCEEDED`
`RUNNING -> RETRY_WAIT -> RUNNING`
`RUNNING -> FAILED`
`PENDING -> CANCELLED`
A job may be reclaimed after its lease expires. State transitions use optimistic version checks or row locks.

### Knowledge
Responsibilities:
- CRUD for manually authored knowledge items.
- Revision history.
- Lifecycle status and supersession.
- Scope assignment and provenance links.

Manual item validation:
- title required, bounded length;
- body required for note/procedure/incident summary;
- decision items require context, decision, consequences, and status;
- owner/team and scope must be explicit;
- external URLs validated against allowed schemes;
- no arbitrary HTML execution.

### Ontology
Responsibilities:
- Define entity and relationship types with version.
- Validate allowed source/target combinations where applicable.
- Store entity aliases and normalized names.
- Resolve candidate entity matches with deterministic rules first.
- Keep ambiguous entity resolution as a proposal.

Initial entity types:
Organization, Team, Service, Repository, CodeReference, Decision, Incident, PullRequest, Person, KnowledgeItem.

Initial relationship types:
OWNS, DEPENDS_ON, IMPLEMENTED_IN, DISCUSSES, CAUSED, MITIGATED_BY, SUPERSEDES, RELATES_TO, AUTHORED_BY.

Relationship record fields:
- source entity ID;
- relationship type;
- target entity ID;
- status (proposed, accepted, rejected, superseded);
- confidence;
- evidence reference(s);
- extraction run/version;
- reviewer and review timestamp when reviewed.

### Extraction
Responsibilities:
- Parse known structured formats deterministically.
- Invoke optional LLM only for ambiguous classification/entity/relationship extraction.
- Validate output against a versioned JSON schema.
- Verify every claimed evidence span exists in the source snapshot.
- Enforce allowed entity/relationship types.
- Persist proposals, not unreviewed facts, unless a documented deterministic rule applies.

LLM prompt must instruct the model to return only source-supported statements and explicit unknowns. Model output is untrusted and cannot set ACLs, reviewer identity, or canonical lifecycle state.

### Indexing
Responsibilities:
- Generate normalized searchable text.
- Generate embeddings using a configured model/version.
- Maintain lexical and vector index status.
- Reindex on content, embedding model, or permission-scope changes.
- Avoid storing embeddings as the only representation of knowledge.

Embedding metadata must include provider, model identifier, dimensions, and version. A model change requires re-embedding or an explicit compatibility strategy.

### Retrieval
Responsibilities:
- Parse query and optional entity hints.
- Execute lexical and vector candidate retrieval.
- Apply ACL predicates in the database query.
- Expand a bounded neighborhood through accepted edges.
- Rank and package evidence.
- Enforce result and token budgets.

Retrieval must not use a post-filter-only security model. Unauthorized candidates must not be sent to an LLM reranker or included in response metadata.

### Review and feedback
Responsibilities:
- Record feedback without changing canonical knowledge.
- Queue review tasks.
- Accept/reject/correct extraction proposals.
- Record actor, action, timestamp, and reason.
- Trigger reindexing when approved state changes affect search.

### MCP adapter
Responsibilities:
- Expose narrow, documented read-only tools.
- Resolve delegated identity from the MCP host's authenticated session.
- Validate inputs and response size.
- Call application retrieval services.
- Return structured content with citations and freshness.

MCP adapter must not duplicate business logic or bypass application authorization.

## 3. Core service contracts

Conceptual application interfaces:

| Use case | Input | Output |
|---|---|---|
| CreateKnowledgeItem | principal, item payload | item ID, revision, indexing state |
| UpdateKnowledgeItem | principal, item ID, expected revision, patch | updated revision |
| RegisterSource | principal, connector config, scope mapping | source installation ID |
| SubmitSourceSnapshot | source identity, revision, normalized envelope | ingestion job ID |
| ReviewRelationship | principal, proposal ID, decision, reason | review result |
| SearchKnowledge | principal, query, filters, limits | ranked result page |
| GetEntityContext | principal, entity ID, depth, limits | entity and authorized neighborhood |
| SubmitFeedback | principal, target, category, comment | feedback ID |
| GetIngestionStatus | principal, source/job ID | status and safe diagnostics |

Expected revision/version is required for mutating updates to prevent silent lost writes.

## 4. REST API outline

All routes are under `/api/v1`.

| Method | Route | Purpose |
|---|---|---|
| POST | `/knowledge` | Create item |
| GET | `/knowledge/{id}` | Read authorized item |
| PATCH | `/knowledge/{id}` | Update item with revision precondition |
| POST | `/knowledge/{id}/archive` | Archive item |
| GET | `/search` | Hybrid search |
| GET | `/entities/{id}` | Read entity and permitted summary |
| GET | `/entities/{id}/related` | Bounded related knowledge |
| POST | `/sources` | Register source |
| GET | `/sources` | List authorized sources |
| GET | `/ingestion/jobs/{id}` | Job status |
| GET | `/review/proposals` | List reviewable proposals |
| POST | `/review/proposals/{id}/decision` | Accept/reject/correct proposal |
| POST | `/feedback` | Submit feedback |
| GET | `/admin/health` | Admin health details |

API errors use stable machine-readable codes, safe messages, and correlation IDs. Do not expose SQL errors, provider credentials, raw stack traces, or inaccessible resource existence.

## 5. Search request/response contract

Search request fields:
- query (required, bounded);
- optional entity hints;
- optional source/entity type filters;
- optional team/scope filters;
- lifecycle filter;
- limit (server-capped);
- response token budget (server-capped).

Each result contains:
- knowledge item ID and title;
- excerpt;
- item/source type;
- source reference;
- evidence reference or span;
- matched entities and relationship rationale;
- freshness and lifecycle state;
- extraction/review confidence metadata;
- safe citation URL where available.

No result should claim a relationship rationale without an accepted edge and evidence.

## 6. Job processing contract

- Job identity is unique by source installation + external record ID + source revision/fingerprint + processing pipeline version.
- Worker claims jobs using a lease and atomic update.
- Worker heartbeat extends lease during long work.
- Retryable errors use bounded exponential backoff with jitter.
- Non-retryable schema/policy errors transition to FAILED with a safe error code.
- A failed extraction stage must not discard the normalized source snapshot.
- Re-running a completed job with the same idempotency key returns the existing outcome unless an explicit reprocess version is requested.
- Jobs record stage, attempts, start/end time, and pipeline version.

## 7. Transaction and consistency boundaries

- Knowledge item update + revision + outbox event: one transaction.
- Relationship review + audit event + reindex event: one transaction.
- Source sync cursor advancement occurs only after the corresponding fetched page/records are durably persisted.
- Embedding generation is outside the primary write transaction; status tracks pending/failed/succeeded.
- Retrieval is read-only and operates on a consistent database snapshot where practical.

## 8. Observability

Structured logs should include request/job correlation ID, module, operation, duration, outcome, and safe resource IDs. Metrics:
- ingestion jobs by state and age;
- retries and terminal failures;
- extraction schema rejection rate;
- embedding failures;
- search latency by retrieval stage;
- candidate count before/after authorization;
- result count and token budget;
- feedback category counts;
- authorization denials.

Never log raw credentials, full sensitive source bodies, or full prompts containing restricted content.
