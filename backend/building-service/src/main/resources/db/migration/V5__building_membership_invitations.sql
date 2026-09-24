-- F4: keep the existing building-service membership authority (ADR-F4-001).
ALTER TABLE building_membership
    ADD COLUMN version bigint NOT NULL DEFAULT 0,
    ADD COLUMN updated_at timestamptz,
    ADD COLUMN revoked_at timestamptz,
    ADD COLUMN revoked_by uuid,
    ADD COLUMN revocation_reason varchar(1000);

UPDATE building_membership SET updated_at = created_at;
ALTER TABLE building_membership
    ALTER COLUMN updated_at SET DEFAULT now(),
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT membership_role_check CHECK (role IN ('BUILDING_ADMIN', 'OWNER')),
    ADD CONSTRAINT membership_version_check CHECK (version >= 0),
    ADD CONSTRAINT membership_state_check CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL AND revoked_by IS NULL AND revocation_reason IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL AND revoked_by IS NOT NULL
            AND revocation_reason IS NOT NULL AND length(trim(revocation_reason)) > 0)
    );
CREATE INDEX building_membership_active_user_idx
    ON building_membership (user_id, building_id) WHERE status = 'ACTIVE';

CREATE TABLE building_invitation (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES building,
    phone varchar(11) NOT NULL CHECK (phone ~ '^01[3-9][0-9]{8}$'),
    role varchar(32) NOT NULL CHECK (role = 'OWNER'),
    status varchar(16) NOT NULL CHECK (status IN ('PENDING', 'CLAIMED', 'REVOKED', 'EXPIRED')),
    created_by uuid NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at > created_at),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    claimed_user_id uuid,
    claimed_at timestamptz,
    revoked_by uuid,
    revoked_at timestamptz,
    revocation_reason varchar(1000),
    FOREIGN KEY (building_id, claimed_user_id, role)
        REFERENCES building_membership (building_id, user_id, role),
    CHECK (
        (status = 'CLAIMED' AND claimed_user_id IS NOT NULL AND claimed_at IS NOT NULL
            AND claimed_at >= created_at AND claimed_at < expires_at)
        OR (status <> 'CLAIMED' AND claimed_user_id IS NULL AND claimed_at IS NULL)
    ),
    CHECK (
        (status = 'REVOKED' AND revoked_by IS NOT NULL AND revoked_at IS NOT NULL
            AND revocation_reason IS NOT NULL AND length(trim(revocation_reason)) > 0)
        OR (status <> 'REVOKED' AND revoked_by IS NULL AND revoked_at IS NULL AND revocation_reason IS NULL)
    )
);
-- Expiry is transitioned under the building lock before a replacement invite is inserted.
-- A clock-dependent partial-index predicate would not be stable.
CREATE UNIQUE INDEX building_invitation_pending_unique
    ON building_invitation (building_id, phone, role) WHERE status = 'PENDING';
CREATE INDEX building_invitation_phone_idx ON building_invitation (phone, status, expires_at);
CREATE INDEX building_invitation_building_idx ON building_invitation (building_id, created_at, id);

CREATE TABLE building_audit (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES building,
    actor_user_id uuid NOT NULL,
    action varchar(80) NOT NULL CHECK (length(trim(action)) > 0),
    entity_type varchar(40) NOT NULL,
    entity_id uuid NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    before_data jsonb NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(before_data) = 'object'),
    after_data jsonb NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(after_data) = 'object'),
    trace_id varchar(128),
    occurred_at timestamptz NOT NULL
);
CREATE INDEX building_audit_entity_idx ON building_audit (building_id, entity_type, entity_id, occurred_at);

CREATE TABLE building_operation (
    actor_user_id uuid NOT NULL,
    action varchar(80) NOT NULL,
    operation_id uuid NOT NULL,
    building_id uuid NOT NULL REFERENCES building,
    request_fingerprint varchar(64) NOT NULL CHECK (request_fingerprint ~ '^[a-f0-9]{64}$'),
    result_entity_id uuid NOT NULL,
    result_version bigint NOT NULL CHECK (result_version >= 0),
    created_at timestamptz NOT NULL,
    PRIMARY KEY (actor_user_id, action, operation_id)
);
