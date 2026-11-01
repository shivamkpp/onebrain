# MCP Integration Design

- Status: Proposed baseline
- Role: agent-facing adapter to OneBrain retrieval capabilities.

## 1. Purpose

MCP gives compatible AI clients a standard way to request organizational context. It is an interface to OneBrain's retrieval services, not the knowledge store, authorization system, or agent runtime.

The MCP server must return bounded, structured, evidence-backed context. It does not execute code, autonomously change repositories, or write canonical knowledge by default.

## 2. Initial tools

### search_context
Search across authorized knowledge for a task or question.

Input:
- query (required, max 4,000 characters);
- optional service/repository/entity hints;
- optional source and lifecycle filters;
- max_results (server capped; default 8).

Output:
- result list with title, type, excerpt, source citation, evidence, freshness, confidence/review state, and relationship rationale;
- truncation indicator;
- explicit no-context/conflict state.

### get_knowledge_item
Retrieve a specific knowledge item by opaque ID, subject to authorization.

Input:
- knowledge_item_id.

Output:
- authorized content, revision, lifecycle, provenance, and related authorized entities.

### get_entity_context
Retrieve context around a known service, repository, decision, or incident.

Input:
- entity identifier or exact name + type;
- max_depth (server capped at 2);
- max_results (server capped).

Output:
- entity summary;
- authorized accepted relationships;
- evidence-backed related knowledge;
- freshness and lifecycle metadata.

### report_context_feedback
Submit a retrieval-quality report.

Input:
- result/knowledge ID;
- category: incorrect, stale, irrelevant, duplicate, missing_context;
- optional short comment.

Output:
- feedback ID and accepted status.

This tool creates feedback only; it does not directly alter canonical knowledge.

## 3. Authorization and identity

- MCP host authenticates the user and establishes a delegated identity context.
- OneBrain validates the identity at the server boundary.
- Tool arguments cannot override the principal or organization.
- Service-to-service tokens must be scoped and rotated.
- If delegated identity cannot be established, return an authorization error; do not fall back to a broad service account.
- Every tool delegates to the same application authorization and retrieval services as REST.

The exact OAuth/OIDC transport profile is deployment-dependent and remains an implementation decision. Local development may use an explicitly marked local identity adapter that is disabled outside development.

## 4. Tool behavior constraints

- Read-only by default, except feedback submission.
- Bounded inputs, result count, relationship depth, and response size.
- Stable error codes for unauthorized, not found, invalid input, and unavailable backend.
- Do not expose internal database schema, SQL, unrestricted search syntax, raw prompts, or connector credentials.
- No tool for arbitrary SQL, arbitrary HTTP fetch, code execution, repository write, or admin configuration.
- Content returned from sources is untrusted; clients should treat it as context, not executable instructions.

## 5. Response and citation requirements

Each result must provide:
- OneBrain knowledge ID.
- Human-readable title and type.
- Source system and canonical URL or stable locator.
- Evidence excerpt or source locator.
- Current lifecycle and freshness label.
- Relationship path when relationship expansion contributed to relevance.
- Confidence/review state for extracted content.

If the MCP protocol/client supports structured content, return both structured JSON and concise text rendering. Never omit evidence just to fit a response budget; instead reduce result count and indicate truncation.

## 6. Versioning and compatibility

- Tool names and schemas are versioned through compatibility policy.
- Breaking changes require a new schema/tool version or documented migration window.
- The server advertises only implemented tools.
- Contract tests validate input schema, output schema, authorization behavior, and size limits.
- REST and MCP must share use-case behavior; they may have different transport serialization.

## 7. Security test cases

- User A cannot retrieve Team B-only content.
- Agent delegated by User A cannot retrieve more than User A.
- Agent cannot pass a different user ID in tool arguments.
- An authorized item linked to an unauthorized entity does not leak that entity's name or existence.
- Search, detail, and relationship expansion enforce the same policy.
- Revoked access invalidates relevant cached results.
- Feedback cannot be used to mutate knowledge or elevate permissions.
