# ADR 0005: MCP is an adapter over application services

- Status: Proposed
- Date: 2026-11-01

## Context
AI coding clients need a standard way to request organizational context. MCP is a transport/tool protocol, not an internal knowledge architecture or authorization system.

## Decision
Implement MCP as a thin adapter that invokes the same application use cases as REST. Expose narrow, bounded retrieval tools. Use delegated identity and preserve OneBrain authorization. Keep tools read-only except for explicit feedback submission.

## Alternatives considered
- MCP as the only API: rejected because the web UI and non-MCP clients need stable REST contracts.
- Give agents database/SQL access: rejected due to authorization and data-integrity risk.
- Build an autonomous agent: out of scope; OneBrain supplies context, not autonomous action.

## Consequences
- REST and MCP share behavior and tests.
- Authentication/delegation details depend on deployment and must be resolved before production.
