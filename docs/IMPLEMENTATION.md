# Foundation milestone

This document describes implemented scope, not completion of the full PRD.

## Design alignment

The owner approved the PRD in the implementation brief. Its older proposal label is retained. The HLD, LLD, data model, and technology ADRs remain proposals for the complete application. Inspection found no first-milestone architectural conflict requiring a PRD change.

The implementation follows their modular-monolith, versioned REST, PostgreSQL, provenance, and authorization direction. It deliberately implements only manually authored `NOTE` items. Supporting other item types now would require their real validation rules, particularly structured decision fields.

## Selected decisions

- **Runtime/build:** Java 21 LTS, Spring Boot 4.1.2, Maven wrapper 3.9.16. Dependency versions are managed together by Spring Boot. This release manages JUnit Jupiter 6; the earlier stack proposal mentioned JUnit 5. The tests retain the Jupiter API and use the framework's compatible release rather than overriding its BOM.
- **Frontend:** React 19.3.1, TypeScript 7.0.2, Vite 8.3.5, Node 24.19.0. Direct versions and npm lockfile are committed. Compatibility was checked against package metadata and actual builds.
- **Database:** PostgreSQL 18.6, pinned by container digest in Compose and integration tests; Flyway SQL migrations. No vector extension is needed before semantic retrieval.
- **Persistence:** Spring JDBC with parameterized SQL. A repository contains the knowledge SQL; use-case transactions sit in the knowledge service. No generic repository framework or speculative adapter hierarchy.
- **Identity:** explicit local profile, synthetic Basic-auth accounts, encoded in-memory password records, no usable default account outside the profile. Authentication is stateless; the CSRF token uses a session cookie. This adapter is for loopback development only; production identity remains an open decision.
- **Authorization:** database-backed grants, checked on each operation. Read queries include principal, organization, and scope predicates. Scope listings contain only granted scopes. Contribution requires read and contribute grants. The owning team is derived from the selected scope, preventing contradictory team/scope submissions.
- **Provenance/lifecycle:** source title and URL are an optional supporting pair on the initial revision. Author and organization come from the authenticated identity and authorized scope. A note starts at revision 1 and remains `DRAFT`; it is readable by permitted users by ID, not published or indexed.
- **Atomicity:** note, initial revision, fingerprint, and audit event are created in one transaction. A deferred composite foreign key ensures the current revision belongs to its item. Database constraints prevent cross-organization scopes and grants.

The PRD's eventual indexing jobs/outbox, source records/revisions, extracted evidence, lifecycle transitions, and revision editing are not simulated. A manually supplied supporting URL is not an ingested source or verified evidence span.

## Verification map

| Behavior | Evidence |
|---|---|
| Real persistence, source metadata, revision, timestamps, author, audit correlation | `KnowledgeApiTest.createsAndReadsPersistedNoteWithProvenanceRevisionAndAudit` |
| Missing source allowed for manual notes | `KnowledgeApiTest.manualNoteMayOmitSupportingSource` |
| Cross-team and cross-organization item access denied without existence disclosure | `KnowledgeApiTest.missingAndInaccessibleNotesHaveTheSameSafeResponse` |
| Scope write checks, current grants, and immediate revocation | `KnowledgeApiTest` permission and revocation cases |
| Input bounds, URL validation, malformed JSON, author/organization spoof attempts | Parameterized invalid-input API test |
| CSRF and authentication | `KnowledgeApiTest.csrfAndAuthenticationAreRequired` |
| Failed audit rolls back note and revision | `KnowledgeApiTest.failedAuditRollsBackBothItemAndRevision` |
| Local identity absent by default | `LocalIdentityTest` |
| Form submission, source pairing, plain-text rendering, safe errors, read-only UI | `frontend/src/App.test.tsx` |
| Browser → API → database, reload, reader access, outsider denial | `frontend/e2e/knowledge.spec.ts` |

Run the commands in the README for fresh evidence. No search-quality, scalability, or complete MVP claims are supported by this milestone. The CI workflow executes these checks, but its hosted result is known only after a run.

## Next milestone

Add note editing with an expected revision precondition, preserved prior revisions, and audit events. Then add archival and a permission-filtered list. These extend the existing tested persistence boundary before introducing ingestion or retrieval infrastructure.

Production identity provider, source ACL mappings, model providers, retention, and deployment remain owner decisions for later milestones; none blocks this synthetic local workflow.
