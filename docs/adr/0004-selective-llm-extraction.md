# ADR 0004: LLM extraction is constrained and reviewable

- Status: Proposed
- Date: 2026-11-01

## Context
Some source metadata and relationships can be parsed deterministically; other knowledge requires semantic interpretation. Calling an LLM for every field increases cost and variability, while trusting free-form output creates hallucination risk.

## Decision
Prefer deterministic parsers and explicit source mappings. Use an LLM behind a provider abstraction only for tasks where semantic extraction is needed. Require versioned structured output, schema validation, evidence locators, bounded retries, and review status. The model cannot set authorization or canonical review state.

## Alternatives considered
- LLM-only ingestion: rejected due to cost, nondeterminism, and weak provenance.
- No LLM: viable baseline but limits semantic extraction; retain as a benchmark baseline.
- Fine-tuned model: out of scope and unsupported by current evidence.

## Consequences
- Provider/model can be swapped and evaluated.
- Extraction behavior must be benchmarked by model and prompt version.
- External data handling must be explicitly configured before real source content is sent.
