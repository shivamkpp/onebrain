# Retrieval Design

- Status: Proposed baseline
- Objective: return relevant, authorized, evidence-backed knowledge with transparent freshness and relationship context.

## 1. Retrieval is the product's core hypothesis

OneBrain must prove that organizing knowledge into typed entities and evidence-backed relationships improves task context beyond ordinary keyword and vector search. The system must be benchmarked against simple baselines on the same data and query set. Graph expansion is not assumed to improve results.

## 2. Pipeline

1. Validate query, filters, identity, and response budget.
2. Resolve authorization scopes and allowed source constraints.
3. Generate lexical candidates using PostgreSQL full-text search.
4. Generate semantic candidates using pgvector.
5. Merge candidate sets using rank fusion.
6. Resolve query entity hints and candidate entity references.
7. Expand through accepted relationships only, with strict depth/fan-out bounds and authorization checks.
8. Compute ranking features.
9. Apply lifecycle/freshness handling and deduplication.
10. Produce evidence-backed result package and safe citations.

## 3. Candidate generation

### Lexical
Use PostgreSQL full-text search with language-aware tokenization and weighted fields (title > entity names/tags > body). Exact identifiers such as service names, repository names, ticket IDs, and code symbols should receive a deterministic boost.

### Semantic
Embed normalized query and indexed knowledge text using a configured embedding model. Persist model identity and dimensions. Queries must not compare incompatible embedding versions.

### Hybrid merge
Initial approach: Reciprocal Rank Fusion (RRF), because it avoids assuming lexical and vector scores are calibrated to the same scale.

`RRF(d) = Σ_i 1 / (k + rank_i(d))`

Use a small, documented constant k (initially 60) and tune only through benchmark evaluation. Candidate generation must apply authorization filters in the database query.

## 4. Entity resolution and relationship expansion

- Prefer exact identifier and alias matches.
- Use normalized-name matching for candidate resolution.
- Use embedding similarity only to propose ambiguous entity matches, not to silently merge identities.
- Expand only from accepted relationships.
- Initial maximum relationship depth: 2.
- Initial maximum neighbors per hop: 20.
- Initial maximum expanded candidates: 100.
- Filter authorization at every hop; inaccessible nodes must not be included in intermediate LLM prompts or response metadata.
- Relationship rationale must name the accepted edge and its evidence.
- Do not infer transitive ownership or dependency unless the ontology explicitly defines it.

## 5. Ranking features

Candidate ranking may use:
- hybrid relevance score;
- exact identifier match;
- evidence quality and locator validity;
- accepted relationship strength and distance;
- source type/trust configuration;
- freshness and last verified time;
- lifecycle status;
- duplicate penalty;
- user-specified filters.

Initial ranking is deterministic and inspectable. Optional LLM reranking is P1 and must be evaluated separately. It must receive only already-authorized candidates and must not generate new facts.

## 6. Freshness and conflicts

Freshness is not simply document age. Use:
- source updated timestamp;
- last successful sync;
- last human verification;
- lifecycle status;
- known successor/supersession links;
- source-specific freshness policy.

Rules:
- SUPERSEDED items are demoted or excluded from default current-context retrieval, but remain accessible for historical queries.
- DISPUTED items are labeled and should not be presented as settled guidance.
- STALE items remain retrievable with a warning when relevant.
- If two current sources conflict, return the conflict and both citations rather than silently selecting one.
- Missing source updates do not prove the content is current.

## 7. Response contract

Each result should include:
- item ID, title, item type;
- concise excerpt;
- source type and canonical source reference;
- evidence locator and supporting excerpt;
- matched entity IDs/names that are safe to disclose;
- relationship path and explanation, if used;
- lifecycle state and freshness label;
- confidence/review status;
- score breakdown for diagnostics (not necessarily exposed to end users).

The top-level response includes query interpretation, result count, truncation/token-budget indicator, and an explicit no-context/conflict indicator when appropriate.

## 8. Security properties

- Apply organization and scope predicates to lexical and vector candidate queries.
- Do not rely on filtering after an LLM has seen candidate text.
- Relationship expansion rechecks ACL at each node and edge.
- Aggregated summaries may only use evidence accessible to the caller.
- Error responses must not reveal whether an inaccessible item exists.
- Cache keys include principal/authorization scope and relevant permission version; invalidation occurs on ACL changes.
- Retrieval telemetry records counts and timings, not sensitive content.

## 9. Evaluation methodology

Compare at minimum:
1. Keyword-only.
2. Vector-only.
3. Hybrid RRF.
4. Hybrid + bounded relationship expansion.

Use a fixed versioned dataset and fixed query/expected-evidence set. Evaluate:
- precision@5 and recall@10;
- MRR or nDCG@10;
- citation validity;
- relationship path correctness;
- stale/superseded result rate;
- authorization leakage (must be zero);
- p50/p95 latency;
- embedding and LLM cost per 1,000 queries where applicable.

Report confidence intervals or query-level results when sample size permits. Do not claim statistical significance from a tiny synthetic dataset.
