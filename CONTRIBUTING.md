# Contributing to OneBrain

OneBrain has a working create/read knowledge-note foundation. The broader product remains under development; the architecture documents describe its intended direction. See `README.md` for setup and `docs/IMPLEMENTATION.md` for the implemented boundary.

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

## Code changes

- Keep changes focused on the current milestone and organize backend code by feature.
- Run `./scripts/check.sh`; for changes to the user flow, also run `./scripts/e2e.sh` after installing Chromium.
- Backend integration tests require Docker and use real PostgreSQL, not an in-memory substitute.
- Commit the npm lockfile. Change versioned migrations by adding a new migration once they have been shared.
- Test access denial as well as successful behavior when adding a data access path.
- Record actual verification results; distinguish local checks from hosted CI and future-task restoration.

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
