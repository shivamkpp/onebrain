# ADR 0003: Evidence and provenance are mandatory

- Status: Proposed
- Date: 2026-11-01

## Context
An organizational knowledge product can cause harm if plausible but unsupported relationships are presented as facts. LLM extraction is probabilistic and source content may be incomplete or conflicting.

## Decision
Every extracted fact and relationship must reference source evidence, extraction run/version, confidence, and review state. Evidence locators must be validated against the source revision. Unsupported relationships are rejected or retained only as failed proposals. Conflicts and supersession are represented explicitly.

## Alternatives considered
- Store only generated summaries: rejected because claims cannot be verified.
- Trust model confidence alone: rejected because confidence is not evidence.
- Require human review of every deterministic metadata field: rejected as unnecessary overhead; deterministic mappings may be accepted under explicit rules.

## Consequences
- More provenance data and review logic.
- Better traceability, correction, and benchmarkability.
- Evidence quality becomes a first-class evaluation dimension.
