# ADR 0007: Use Java/Spring Boot, React/TypeScript, and PostgreSQL

- Status: Proposed
- Date: 2026-11-01

## Context
The MVP needs a domain-rich backend, human-facing UI, relational integrity, full-text search, vector similarity, asynchronous ingestion, and an MCP interface. The project is built by one developer; stack familiarity and a manageable local environment matter.

## Decision
Recommend:
- Java on a supported LTS JDK with Spring Boot for the backend.
- React with strict TypeScript for the web UI.
- PostgreSQL with pgvector as the initial persistence and vector-search layer.
- REST/JSON as the primary application API.
- MCP as a thin adapter over application services.
- PostgreSQL-backed job processing for initial asynchronous ingestion.
- Versioned SQL migrations and OpenAPI contracts.

Exact dependency versions and the MCP Java SDK are selected at implementation start after checking current compatibility and maintenance status.

## Alternatives considered
- Node.js backend: viable, but Java/Spring is a better fit for the owner's existing backend experience and desired project learning.
- Python backend: viable for AI-heavy prototypes, but this product's core is data modeling, authorization, ingestion, and retrieval rather than model training.
- Separate graph/vector databases: deferred until benchmark and workload evidence justify them.
- Microservices: deferred under ADR 0001.

## Consequences
- A coherent stack with strong relational and backend tooling.
- Java and Spring Boot may make rapid experimentation with some AI SDKs less convenient; isolate provider calls behind adapters.
- Exact versions and deployment target remain implementation-time decisions.
