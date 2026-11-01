# Evaluation and Benchmark Plan

- Status: Proposed baseline
- Purpose: determine whether OneBrain's structure and relationship-aware retrieval improve useful context over simpler search.

## 1. Hypotheses

- H1: Hybrid lexical + vector retrieval improves coverage over either method alone.
- H2: Evidence-backed entity relationships improve retrieval for cross-service and cross-artifact questions.
- H3: Freshness and supersession handling reduce obsolete guidance in top results.
- H4: Permission filtering can be enforced with zero leakage in the benchmark suite.
- H5: Structured context improves agent grounding, measured by evidence use and answer correctness in a fixed task set.

These are hypotheses, not product claims.

## 2. Synthetic benchmark organization

Create a small fictional engineering company with:
- teams: Payments, Orders, Notifications, Platform;
- services: Payment API, Order API, Notification Dispatcher, Identity Gateway;
- repositories and code references;
- ADRs with accepted and superseded states;
- PRs implementing decisions;
- incidents with cause and mitigation evidence;
- runbooks and engineering notes;
- intentionally duplicated, stale, conflicting, and irrelevant content;
- multiple visibility scopes with overlapping and restricted content.

All names, people, events, code, and incidents must be synthetic.

## 3. Query categories

1. Exact lookup: find a named service, ADR, PR, or incident.
2. Semantic lookup: ask about a concept using different wording.
3. Cross-artifact: connect incident, mitigation PR, and decision.
4. Dependency: identify upstream/downstream service context.
5. Historical: retrieve the decision that was later superseded.
6. Freshness: identify current guidance and flag old guidance.
7. Conflict: surface two conflicting records and their evidence.
8. Permission: ask a query whose most relevant answer is inaccessible.
9. Ambiguous entity: similar service/repository names.
10. No answer: query for a fact absent from the dataset.

Each query has expected relevant evidence, acceptable alternatives, expected lifecycle behavior, and an authorization principal.

## 4. Baselines and variants

- B0: PostgreSQL keyword/full-text search.
- B1: vector similarity only.
- B2: hybrid RRF.
- B3: hybrid RRF + bounded accepted-relationship expansion.
- Optional B4: B3 + LLM reranking (only after deterministic baseline is stable).

Use identical corpus, query set, filters, and authorization policy across variants.

## 5. Metrics

- Precision@5.
- Recall@10.
- MRR@10 or nDCG@10.
- Citation validity rate.
- Evidence support accuracy (does cited passage support the returned claim?).
- Relationship path correctness.
- Stale/superseded top-result rate.
- Zero-answer calibration for absent facts.
- Unauthorized-content leakage count (must be zero).
- p50/p95 end-to-end and per-stage latency.
- Embedding/LLM cost per 1,000 queries.
- Extraction entity and relationship precision/recall on labeled fixtures.

## 6. Test set design

Start with at least 50 manually authored queries across the ten categories, then expand as failure cases are found. Label expected evidence at the source-revision/evidence level, not only document title. Include negative and adversarial cases. Keep a held-out set that is not used to tune ranking weights.

For extraction, label entity spans and relationship triples, including examples where the correct output is “unknown” or “no relationship.”

## 7. Reproducibility

Every benchmark run records:
- dataset version/hash;
- query set version/hash;
- application commit;
- retrieval configuration and weights;
- embedding model/provider/version;
- LLM model and prompt/schema versions if used;
- hardware/runtime environment;
- metric outputs and per-query results.

Never compare runs with different corpora or query sets without clearly identifying the change.

## 8. Acceptance gates

Initial proposed gates:
- zero authorization leaks across all test cases;
- 100% citation references resolve to stored evidence or valid source locators;
- hybrid retrieval does not materially regress both precision@5 and recall@10 versus the strongest simple baseline;
- relationship expansion must improve a predeclared cross-artifact subset without unacceptable overall regression;
- superseded records are correctly labeled and do not masquerade as current;
- performance meets PRD targets on documented hardware or the target is revised with evidence.

A tiny synthetic benchmark demonstrates engineering behavior, not general enterprise effectiveness. Report limitations explicitly.
