# OneBrain development instructions

- Read `README.md` for actual implemented capabilities and setup; read `docs/PRD.md` for approved product scope. The PRD's proposal label is stale; the owner approved its requirements. Do not rewrite it as part of routine implementation.
- `docs/IMPLEMENTATION.md` and ADR 0008 define this milestone's boundary. Other architecture documents are proposals for the larger product, not evidence that features exist.
- Use the existing checkout. Cloud tasks are already isolated; do not create a Git worktree unless the user asks for one.
- Prerequisites: Java 21 JDK, Node 24.19.0, Docker with Compose v2, curl. Use the Maven wrapper and `npm ci`.
- Setup: `./scripts/setup.sh`. Required verification: `./scripts/check.sh` (real PostgreSQL integration tests, component tests, TypeScript, production build).
- Start: `./scripts/dev.sh`. Only enable the `local` profile against the synthetic local database. API/frontend bind to loopback, ports 8080/5173.
- Full flow verification: install Playwright Chromium, then `./scripts/e2e.sh` with app ports free. An existing Chromium can be selected through `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH`.
- Preserve server-side organization/scope checks in every query. Never accept author or organization from a request body. Missing grants deny access; inaccessible IDs look absent.
- Preserve the transaction covering item, initial revision, and audit creation. Manual notes are drafts until publication/indexing is implemented.
- Render source content as plain text; accept only absolute HTTP(S) supporting URLs without credentials. Do not fetch submitted URLs.
- Use only synthetic data and authorized personal infrastructure. Keep credentials out of Git, logs, and browser storage.
- Keep the modular monolith small. Do not add empty modules, Kafka, Redis, graph infrastructure, AI providers, or MCP as incidental groundwork.
- Report what was verified, what failed, and what remains unimplemented. Do not claim a completion percentage or hosted CI success from local checks.
