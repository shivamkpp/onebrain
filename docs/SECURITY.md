# Security, Privacy, and Trust Boundaries

- Status: Proposed baseline
- Security is a product requirement, not a later hardening phase.

## 1. Assets to protect

- Source documents and snapshots.
- Knowledge items, entities, and relationships derived from sources.
- Access-control mappings and organization boundaries.
- Connector credentials and provider API keys.
- User identity and audit history.
- Search queries and retrieval telemetry.

## 2. Threat model

| Threat | Example | Primary control |
|---|---|---|
| Cross-scope data leakage | Search returns another team's incident | Server-side ACL predicates in every retrieval path |
| Graph side-channel | Visible item reveals restricted linked service | ACL checks on nodes, edges, counts, and explanations |
| Prompt injection in source | Imported markdown instructs model to reveal secrets | Treat source content as untrusted data; constrained extraction; no tool execution |
| LLM hallucination | Model invents dependency edge | Evidence-required schema, validation, review status |
| Credential compromise | Connector token committed to repo/logs | Secret references, environment injection, redaction |
| Connector permission drift | Source ACL changes but index remains open | Versioned ACL sync; fail closed on unknown mapping |
| Stale authorization cache | Revoked user retains access | Permission-versioned cache and invalidation |
| Malicious content | HTML/script embedded in knowledge | Output encoding, safe markdown rendering, URL allowlist |
| Denial of service | Huge query or ingestion payload | Input bounds, quotas, timeouts, job limits |
| Supply-chain compromise | Vulnerable dependency | Dependency updates, lockfiles, vulnerability scanning |

## 3. Authorization rules

- Every record belongs to an organization.
- Every record has an explicit visibility scope or inherited source ACL.
- Missing/unknown scope mapping denies access.
- Authorization is checked before data enters ranking, reranking, graph expansion, or response assembly.
- Derived claims cannot be more visible than the evidence supporting them.
- For a claim supported by multiple evidence records, only evidence readable by the caller may be used to justify the returned claim.
- Admin bypasses, if introduced, must be explicit, audited, and restricted to administrative use cases.

## 4. Source and LLM trust

- Source text is untrusted input, including markdown, comments, code, issue bodies, and PR descriptions.
- Extraction prompts separate instructions from quoted source content.
- LLM output is parsed against a strict schema; unknown fields are rejected.
- The model cannot assign user identities, ACLs, reviewer state, or source authenticity.
- Relationship evidence must resolve to a source revision and valid locator.
- No source content is sent to an external model unless the deployment has explicitly configured that provider and accepted the data-handling implications.
- Provider calls use timeouts, token limits, and cost budgets.

## 5. Data minimization

- MVP fixtures use synthetic people and engineering artifacts.
- Do not ingest private chat or personal communications in MVP.
- Store only content required for retrieval and evidence.
- Keep operational logs free of full source content and secrets.
- Define retention/deletion behavior before onboarding real customer data.
- Do not use OneBrain as employee productivity monitoring.

## 6. Application security controls

- Validate and bound all API/MCP inputs.
- Use parameterized database queries.
- Apply CSRF/CORS/session protections appropriate to selected auth transport.
- Rate-limit expensive search and extraction operations.
- Encode rendered content; sanitize markdown and links.
- Keep secrets out of Git and client bundles.
- Use least-privilege database and connector credentials.
- Record audit events for permission, connector, review, and lifecycle operations.
- Return generic not-found/unauthorized behavior where resource existence itself is sensitive.

## 7. Required security tests

- Cross-organization and cross-team search denial.
- Unauthorized item ID lookup.
- Relationship traversal through inaccessible intermediate node.
- Mixed-permission evidence aggregation.
- Revoked access after index/cache creation.
- Unknown source ACL mapping.
- MCP delegated identity spoof attempt.
- Prompt injection fixture in source content.
- Malformed extraction JSON and invented evidence locator.
- Secret redaction in logs and error responses.

A release cannot pass if any unauthorized-content leakage test fails.
