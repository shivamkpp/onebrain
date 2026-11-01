# OneBrain

**A shared organizational knowledge network that turns scattered team knowledge into structured, traceable, permission-aware context for people and AI tools.**

OneBrain addresses a recurring engineering problem: important decisions, operational lessons, system relationships, and implementation context are spread across repositories, pull requests, architecture documents, incident reports, and team discussions. The information exists, but it is difficult to find, connect, validate, and reuse when someone is doing related work.

OneBrain is a knowledge layer over those sources. It helps teams **publish knowledge, organize it into a connected and evidence-backed model, and retrieve the right context** for a task. It is not intended to replace source systems, become another chat application, or let an LLM freely invent an organization's architecture.

## Product promise

Given a question about a system or a piece of engineering work, OneBrain should return relevant knowledge with:
- **Evidence:** the source and exact passage or reference supporting each result.
- **Relationships:** why the result is relevant to the current system, service, decision, incident, or code area.
- **Freshness:** whether the knowledge is current, superseded, disputed, or unverified.
- **Access control:** only information the requesting user or agent is allowed to see.

## Initial product scope

The first release focuses on engineering organizations and a deliberately small set of source types: curated knowledge entries, architecture decision records (ADRs), repository metadata, pull requests, and incident reports. A synthetic engineering organization and synthetic documents will be used during development and evaluation. No employer-confidential data is required or permitted.

The planned product is a modular monolith with PostgreSQL and pgvector, a Java/Spring Boot API, a React/TypeScript web interface, and an MCP interface for AI coding clients. The first implemented milestone is the smaller create/read workflow below.

## Working foundation

The application currently supports **creating and retrieving manually authored knowledge notes**:

- React form and saved-note detail, including optional supporting source title and HTTP(S) URL.
- Versioned Spring Boot API backed by PostgreSQL and Flyway migrations.
- Server-assigned author, organization, timestamps, and revision 1; explicit visibility scope with its owning team.
- Synthetic local development accounts, server-side scope grants, and CSRF protection.
- Transactional creation of the note, initial revision, content fingerprint, and audit event.
- PostgreSQL integration tests, frontend component tests, a Chromium end-to-end test, and a GitHub Actions workflow.

Notes are saved as **DRAFT**. They can be opened by ID by permitted users; publication, indexing, listing, editing, and revision updates are not implemented. A supporting URL is user-supplied metadata, not a fetched or independently verified citation.

### Run locally

Prerequisites: **JDK 21**, **Node.js 24.19.0** (see `.node-version`), Docker Engine with Compose v2, and curl. Maven 3.9.16 is downloaded and checksum-verified by the committed wrapper. No cloud account, LLM key, or external source credentials are needed.

```bash
./scripts/setup.sh
./scripts/check.sh
./scripts/dev.sh
```

The development script starts PostgreSQL, the API on loopback port **8080**, and Vite on loopback port **5173**. Open the frontend on port 5173 in your local browser. Stop app processes with Ctrl-C; stop PostgreSQL separately with `docker compose stop`. Its named volume retains data. Integration tests use their own temporary PostgreSQL container.

Local accounts all use the synthetic password **`onebrain-local`** by default:

| Account | Organization / scope | Access |
|---|---|---|
| `contributor` | Synthetic Engineering / Platform | Create and read |
| `reader` | Synthetic Engineering / Platform | Read |
| `outsider` | Synthetic Engineering / Payments | Read only in its own scope |
| `external` | Synthetic External Organization / External | Read only in its own organization |

Sign in as `contributor`, save a note, and copy its displayed ID. After signing out, `reader` can open it; `outsider` and `external` receive the same not-found response as for an absent note. Reloading the page clears the in-memory login and displayed note; the database retains the saved record.

The script explicitly enables Spring's `local` profile. Without that profile there are no usable development accounts. This is a local synthetic-data environment, not production authentication or a shared deployment.

### Tests and configuration

```bash
# Backend integration and identity tests, component tests, TypeScript and production build:
./scripts/check.sh

# One-time browser installation (standard local machine / CI):
cd frontend
npx playwright install --with-deps chromium
cd ..

# Starts app processes, tests the real UI/API/database, then stops those processes:
./scripts/e2e.sh
```

The browser test creates a synthetic note, reloads the page, reads it as `reader`, and verifies that other scopes and organizations cannot read it. It leaves the synthetic note in the local development database. If Chromium is already installed, set `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` instead of downloading a browser. The app ports must be free before running the development or browser-test scripts.

| Variable | Purpose / local default |
|---|---|
| `DB_URL` | JDBC URL; `jdbc:postgresql://127.0.0.1:5432/onebrain` |
| `DB_USERNAME` | Database username; `onebrain` |
| `DB_PASSWORD` | Database password; `onebrain-local` only under the local profile and Compose |
| `ONEBRAIN_LOCAL_PASSWORD` | Synthetic account password under the local profile; `onebrain-local` |
| `DB_PORT` | Compose host port; `5432`. If changed, also set `DB_URL` to match. |
| `SERVER_ADDRESS`, `SERVER_PORT` | API bind overrides; `127.0.0.1`, `8080` |

Export overrides in the shell before running scripts. The combined development workflow and Vite proxy use API port 8080; custom API ports require starting the backend separately and adjusting the proxy. For an existing PostgreSQL volume, changing `DB_PASSWORD` does not change the database role password. Use a dedicated local database; do not point the synthetic profile at an existing shared database. Credentials belong in environment settings, never tracked files or client bundles.

GitHub Actions runs setup, backend/frontend checks, and the browser test. A workflow file alone is not evidence of a successful hosted CI run; inspect its results after pushing.

### Repository layout

```text
backend/    Spring Boot; knowledge, identity, authorization, audit, and API packages
frontend/   React + strict TypeScript; component and browser tests
scripts/    Reproducible setup, validation, development, and browser-test commands
docs/       Approved product scope, proposed architecture, API contract, milestone decisions
```

See [API contract](docs/API.md), [milestone boundaries](docs/IMPLEMENTATION.md), and [agent instructions](AGENTS.md). Search, vectors, ingestion, relationships, extraction, and MCP remain future work.

## Start here

| Document | Purpose |
|---|---|
| [Product Requirements (PRD)](docs/PRD.md) | The authoritative problem statement, users, requirements, scope, workflows, and acceptance criteria |
| [High-Level Design](docs/HLD.md) | System boundaries, components, data flow, and deployment view |
| [Low-Level Design](docs/LLD.md) | Module responsibilities, interfaces, processing contracts, and failure handling |
| [Technology Stack](docs/TECH_STACK.md) | Recommended languages, frameworks, infrastructure, and rationale |
| [Data Model](docs/DATA_MODEL.md) | Canonical entities, relationships, provenance, permissions, and lifecycle |
| [Retrieval Design](docs/RETRIEVAL_DESIGN.md) | Hybrid retrieval, graph expansion, ranking, citations, and freshness |
| [MCP Design](docs/MCP_DESIGN.md) | Agent-facing tools and protocol boundary |
| [Security & Privacy](docs/SECURITY.md) | Tenant isolation, authorization, data handling, and threat model |
| [Evaluation Plan](docs/EVALUATION.md) | Baselines, datasets, metrics, and release gates |
| [Roadmap](docs/ROADMAP.md) | Sequenced delivery plan and explicit non-goals |
| [Open Questions](docs/OPEN_QUESTIONS.md) | Decisions that require product-owner or deployment input |
| [Architecture Decisions](docs/adr/README.md) | Decision records and alternatives considered |

## Non-goals

OneBrain will not initially:
- Replace GitHub, issue trackers, documentation platforms, chat systems, or incident-management tools.
- Become a general-purpose conversational assistant or autonomous coding agent.
- Automatically ingest every connected source without explicit configuration and permission mapping.
- Treat LLM-generated summaries or inferred relationships as facts without provenance and confidence.
- Introduce a dedicated graph database, event-streaming platform, microservices, or multi-region deployment before measured requirements justify them.
- Train or fine-tune a foundation model.

## Current status

**Foundation milestone.** The create/read workflow above is implemented. The broader architecture documents describe intended capabilities, not completed features. The owner has approved the PRD; its older proposal label has not been rewritten. All retrieval performance targets and acceptance thresholds remain unmeasured targets.

## Design principles

1. Sources remain the system of record; OneBrain stores normalized knowledge and references back to evidence.
2. Every extracted fact and relationship must retain provenance.
3. Authorization is enforced before content is returned, including during retrieval expansion.
4. Currentness and supersession are first-class properties.
5. Deterministic processing is preferred; LLM extraction is selective, schema-constrained, and reviewable.
6. Retrieval quality is measured against a simple baseline before complexity is added.
7. Start with the smallest architecture that can prove the product's core value.

## Repository status and contribution

The project is being designed as an independently owned software project. Use synthetic or appropriately licensed public data only. Do not add employer, client, university, or other confidential information.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution conventions and the design review process.
