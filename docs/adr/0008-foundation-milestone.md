# ADR 0008: Implement a scoped manual-note vertical slice

- Status: Accepted for the owner-approved foundation milestone
- Date: 2026-11-01

## Context

The repository contained design documents only. The owner requested a small working foundation with a React form, Spring Boot endpoints, PostgreSQL, local identity, server-side authorization, automated tests, and reproducible setup and CI. The approved PRD remains the product source of truth; full MVP implementation is outside this milestone.

## Decision

Create `backend/`, `frontend/`, and root Compose/scripts. Use Java 21, Spring Boot 4.1.2, Maven 3.9.16, React/TypeScript/Vite, and PostgreSQL 18.6 with Flyway. Use Spring JDBC for explicit authorization-aware SQL. Keep feature packages without introducing a module framework or empty future modules.

Implement POST and GET-by-ID for `NOTE` items only. Persist the initial revision and optional supporting reference with server-assigned provenance, selected scope, owning team, and draft status. Write an audit event atomically. Enable synthetic development identities only under the explicit local profile, with Basic authentication, CSRF protection, and database-backed READ/CONTRIBUTE grants.

## Consequences

A small, executable workflow establishes real persistence, provenance, access boundaries, and validation. The local identity adapter and synthetic fixtures do not constitute production authentication. Supporting references are not independently verified evidence. Initial revision persistence does not implement editing or full history workflows.

Publication/indexing, jobs/outbox, additional item types, source ingestion, retrieval, relationships, extraction, and MCP remain deferred. Add these incrementally with meaningful tests; do not simulate their completion. See [implementation boundaries](../IMPLEMENTATION.md).
