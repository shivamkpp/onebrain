CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE team (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    name VARCHAR(200) NOT NULL,
    UNIQUE (organization_id, id)
);

CREATE TABLE principal_account (
    name VARCHAR(100) PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    UNIQUE (organization_id, name)
);

CREATE TABLE visibility_scope (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    owner_team_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    UNIQUE (organization_id, id),
    FOREIGN KEY (organization_id, owner_team_id) REFERENCES team(organization_id, id)
);

CREATE TABLE resource_grant (
    organization_id UUID NOT NULL,
    principal_name VARCHAR(100) NOT NULL,
    scope_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('READ', 'CONTRIBUTE')),
    PRIMARY KEY (principal_name, scope_id, action),
    FOREIGN KEY (organization_id, principal_name) REFERENCES principal_account(organization_id, name),
    FOREIGN KEY (organization_id, scope_id) REFERENCES visibility_scope(organization_id, id)
);

CREATE TABLE knowledge_item (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    visibility_scope_id UUID NOT NULL,
    item_type VARCHAR(30) NOT NULL CHECK (item_type = 'NOTE'),
    lifecycle_status VARCHAR(30) NOT NULL CHECK (lifecycle_status = 'DRAFT'),
    current_revision_id UUID NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (organization_id, id),
    FOREIGN KEY (organization_id, visibility_scope_id) REFERENCES visibility_scope(organization_id, id),
    FOREIGN KEY (organization_id, created_by) REFERENCES principal_account(organization_id, name)
);

CREATE TABLE knowledge_revision (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL REFERENCES knowledge_item(id),
    revision_number INTEGER NOT NULL CHECK (revision_number > 0),
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    body TEXT NOT NULL CHECK (length(trim(body)) BETWEEN 1 AND 20000),
    source_title VARCHAR(200),
    source_url VARCHAR(2048),
    content_sha256 CHAR(64) NOT NULL,
    created_by VARCHAR(100) NOT NULL REFERENCES principal_account(name),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (knowledge_item_id, revision_number),
    UNIQUE (knowledge_item_id, id),
    CHECK ((source_title IS NULL AND source_url IS NULL)
        OR (source_title IS NOT NULL AND source_url IS NOT NULL))
);

-- A current revision must belong to this item; deferred to allow the initial pair in one transaction.
ALTER TABLE knowledge_item ADD CONSTRAINT knowledge_current_revision_fk
    FOREIGN KEY (id, current_revision_id) REFERENCES knowledge_revision(knowledge_item_id, id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    actor_name VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL CHECK (action = 'KNOWLEDGE_CREATED'),
    target_id UUID NOT NULL,
    request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (organization_id, actor_name) REFERENCES principal_account(organization_id, name),
    FOREIGN KEY (organization_id, target_id) REFERENCES knowledge_item(organization_id, id)
);

CREATE INDEX knowledge_scope_idx ON knowledge_item(organization_id, visibility_scope_id);
