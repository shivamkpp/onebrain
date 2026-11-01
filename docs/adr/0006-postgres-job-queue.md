# ADR 0006: Use a PostgreSQL job table initially

- Status: Proposed
- Date: 2026-11-01

## Context
Ingestion and extraction are asynchronous and need retries, leases, idempotency, and visibility. Initial workload is small and the application already depends on PostgreSQL.

## Decision
Use a PostgreSQL-backed job table with atomic claims, leases, bounded retries, and terminal failure states. Keep job processing interfaces independent so a broker can replace the implementation later.

## Alternatives considered
- Kafka: rejected for MVP because event streaming and replay scale are not yet requirements.
- RabbitMQ/SQS: viable future options but add operational/configuration cost without current evidence.
- Synchronous ingestion: rejected because external parsing, embedding, and model calls have variable latency and failure modes.

## Consequences
- Fewer infrastructure dependencies.
- Database polling and job contention must be measured.
- A dedicated queue is reconsidered if throughput or operational needs justify it.
