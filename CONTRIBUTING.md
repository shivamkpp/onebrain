# Contributing to OneBrain

OneBrain is currently in the product and architecture design phase. The goal of this repository is to make the product sufficiently unambiguous that implementation can proceed without repeatedly re-deciding its foundations.

## Working agreements

- Treat `docs/PRD.md` as the source of truth for product scope and externally observable behavior.
- Treat accepted architecture decision records (ADRs) as the source of truth for technology and structural decisions.
- If a design document conflicts with the PRD, raise the conflict and update the documents together; do not silently reinterpret requirements.
- Distinguish **decided**, **proposed**, and **open** items. Do not present an assumption as a user-approved fact.
- Do not claim that a feature, benchmark, security control, or integration exists until it is implemented and verified.
- Use synthetic data or public data whose license permits the intended use. Never commit employer, customer, or other confidential material, credentials, access tokens, or personal data.

## Documentation change process

1. Identify the affected requirement, component, or decision.
2. Update the authoritative document and cross-references.
3. Add or amend an ADR for decisions that materially change architecture, dependencies, data ownership, or security posture.
4. Update acceptance criteria and evaluation cases when observable behavior changes.
5. Record unresolved trade-offs explicitly.

## Status vocabulary

- **Accepted:** selected for the initial implementation.
- **Proposed:** a concrete recommendation awaiting review.
- **Open:** requires evidence, clarification, or a decision.
- **Deferred:** intentionally outside the current release.
- **Superseded:** replaced by a newer decision; retain the historical record.

## Design review checklist

- Is the user problem explicit?
- Is the behavior testable?
- Are permissions and provenance addressed?
- Are failure and retry behaviors specified?
- Are freshness and deletion semantics clear?
- Is the complexity justified by a requirement or measured result?
- Are all performance numbers labeled as targets rather than measured facts?
