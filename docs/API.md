# Foundation REST contract — v1

Base path: `/api/v1`. JSON request/response bodies. Only manually authored notes are supported. Requests require explicit local-profile HTTP Basic credentials; use this adapter only on loopback with synthetic data.

## Session and CSRF

`GET /session` returns the authenticated username, readable scopes, and a CSRF token:

```json
{
  "username": "contributor",
  "scopes": [{
    "id": "30000000-0000-0000-0000-000000000001",
    "organizationId": "10000000-0000-0000-0000-000000000001",
    "ownerTeamId": "20000000-0000-0000-0000-000000000001",
    "name": "Platform",
    "canContribute": true
  }],
  "csrfToken": "<session token>",
  "csrfHeader": "X-CSRF-TOKEN"
}
```

Retain the session cookie and send the returned header/token on POST requests. Every request still needs Basic credentials. Unknown usernames/passwords return 401. Browser credentials live in memory only; reloading requires signing in again.

## Create a note

`POST /knowledge` → **201 Created**, with `Location: /api/v1/knowledge/{id}`.

```json
{
  "title": "Keep the initial service simple",
  "body": "Synthetic note: use a modular monolith for the first milestone.",
  "visibilityScopeId": "30000000-0000-0000-0000-000000000001",
  "source": {
    "title": "Synthetic architecture reference",
    "url": "https://example.com/synthetic/architecture"
  }
}
```

Title: nonblank, at most 200 characters; trimmed on save. Body: nonblank, at most 20,000 characters; preserved as text. Scope: required UUID for which the caller has READ and CONTRIBUTE grants. Source: optional or null; if present, both title (nonblank, at most 200) and URL (at most 2048) are required. URLs must be absolute HTTP(S), include a valid host, and contain no username/password. The server never fetches them. Unknown request properties are rejected; clients cannot set author, organization, status, or timestamps.

The response is the saved note:

```json
{
  "id": "40000000-0000-0000-0000-000000000001",
  "organizationId": "10000000-0000-0000-0000-000000000001",
  "visibilityScopeId": "30000000-0000-0000-0000-000000000001",
  "ownerTeamId": "20000000-0000-0000-0000-000000000001",
  "itemType": "NOTE",
  "lifecycleStatus": "DRAFT",
  "title": "Keep the initial service simple",
  "body": "Synthetic note: use a modular monolith for the first milestone.",
  "source": {
    "title": "Synthetic architecture reference",
    "url": "https://example.com/synthetic/architecture"
  },
  "revision": 1,
  "createdBy": "contributor",
  "createdAt": "2026-11-01T12:00:00Z",
  "updatedAt": "2026-11-01T12:00:00Z"
}
```

IDs and timestamps above are illustrative. Without a supporting source, `source` is null. A successful response follows transaction commit of the item, revision, and audit event.

## Read a note

`GET /knowledge/{id}` → **200 OK**, same representation as creation. Access requires the caller's current READ grant and matching organization. Missing and inaccessible IDs both return 404 with the same code/message. An invalid UUID is a 400.

## Errors

Responses include `X-Request-Id`. Errors contain a safe message and the same server-generated request ID:

```json
{
  "code": "NOT_FOUND",
  "message": "Knowledge item not found.",
  "requestId": "50000000-0000-0000-0000-000000000001"
}
```

| Status | Code | Meaning |
|---|---|---|
| 400 | `INVALID_REQUEST` | Invalid fields, unsupported/unknown input, or malformed JSON/ID |
| 401 | `UNAUTHENTICATED` | Missing or invalid credentials |
| 403 | `FORBIDDEN` | Missing/invalid CSRF token or contribution grant |
| 404 | `NOT_FOUND` | Absent or inaccessible note |
| 500 | `INTERNAL_ERROR` | Unexpected failure; no SQL or content details returned |

Create retries are not idempotent yet; each successful POST creates a new note. There is no list, update, archive, source-ingestion, or search endpoint in this milestone.
