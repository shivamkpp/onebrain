# ADR 0002: PostgreSQL + pgvector as the initial datastore

- Status: Proposed
- Date: 2026-11-01

## Context
The product needs relational entities, typed relationships, revisions, ACLs, provenance, job state, full-text search, and vector similarity. The MVP does not yet demonstrate graph traversal or vector throughput that requires dedicated infrastructure.

## Decision
Use PostgreSQL as the system of record and pgvector for embeddings. Model entities and relationships relationally. Begin with exact vector search for small datasets; select approximate indexes only after benchmark evidence.

## Alternatives considered
- Dedicated graph database: deferred until relational traversal is measured to be inadequate.
- Separate vector database: deferred because it fragments authorization and consistency before scale justifies it.
- Document database: not selected because relational constraints, revisions, ACL joins, and provenance are central.

## Consequences
- One database simplifies transactions and access predicates.
- Query plans and vector index performance must be benchmarked.
- Keep retrieval interfaces independent of physical index choice.
