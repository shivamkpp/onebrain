# ADR 0001: Start with a modular monolith

- Status: Proposed
- Date: 2026-11-01

## Context
OneBrain is an early product whose principal uncertainty is whether structured, evidence-backed knowledge improves retrieval. The expected MVP is a single-organization prototype with bounded source types. There is no measured need for independently scaled services or independent team ownership.

## Decision
Use a modular monolith with explicit feature boundaries. Run API and worker as separate runtime modes from the same codebase where isolation is useful. Keep transport adapters separate from domain/application logic.

## Alternatives considered
- Microservices from day one: rejected because it adds deployment, network, tracing, and consistency overhead before scale or team boundaries exist.
- Single unstructured application package: rejected because it allows domain boundaries to erode and makes later extraction harder.

## Consequences
- Simpler local development and transactional workflows.
- Modules need enforced dependency direction and clear ownership.
- Independent scaling is limited; revisit only with measurements.
