-- F6-T5b (D-37): one row per effective platform-role grant/revoke, written with the role change.
CREATE TABLE platform_role_audit (
    id uuid PRIMARY KEY,
    actor_user_id uuid NOT NULL,
    target_user_id uuid NOT NULL,
    role varchar(32) NOT NULL,
    action varchar(16) NOT NULL CHECK (action IN ('GRANT', 'REVOKE')),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX platform_role_audit_occurred_at_idx ON platform_role_audit (occurred_at DESC);
