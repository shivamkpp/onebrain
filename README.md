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

The initial product is a modular monolith with PostgreSQL and pgvector, a Java/Spring Boot API, a React/TypeScript web interface, and an MCP interface for AI coding clients. The detailed rationale and boundaries are in the design documents.

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

**Design phase.** This repository contains product and architecture specifications, not an implemented application. All performance targets and acceptance thresholds in the documents are proposed engineering targets to validate, not measured results.

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

See [CONTRIBUTING.md](CONTRIBUTING.md) for documentation conventions and the design review process.