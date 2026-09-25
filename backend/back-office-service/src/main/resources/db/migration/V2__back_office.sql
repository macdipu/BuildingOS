CREATE TABLE assisted_onboarding_session (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL,
    assigned_agent_user_id uuid NOT NULL,
    requested_by_user_id uuid,
    status varchar(32) NOT NULL
        CHECK (status IN ('REQUESTED', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER',
                           'COMPLETED', 'CANCELLED', 'EXPIRED')),
    access_scope text[] NOT NULL,
    reason text NOT NULL,
    started_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    completed_at timestamptz,
    notes text,
    CHECK (cardinality(access_scope) > 0),
    CHECK (expires_at > started_at)
);
CREATE INDEX idx_assisted_onboarding_session_building ON assisted_onboarding_session (building_id);
CREATE INDEX idx_assisted_onboarding_session_agent ON assisted_onboarding_session (assigned_agent_user_id);

CREATE TABLE support_session (
    id uuid PRIMARY KEY,
    platform_user_id uuid NOT NULL,
    target_user_id uuid,
    building_id uuid,
    reason text NOT NULL,
    permission_scope text[] NOT NULL,
    started_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    ended_at timestamptz,
    CHECK (cardinality(permission_scope) > 0),
    CHECK (expires_at > started_at),
    CHECK (target_user_id IS NOT NULL OR building_id IS NOT NULL)
);
CREATE INDEX idx_support_session_platform_user ON support_session (platform_user_id);
CREATE INDEX idx_support_session_target_user ON support_session (target_user_id);
CREATE INDEX idx_support_session_building ON support_session (building_id);

CREATE TABLE elevated_approval_request (
    id uuid PRIMARY KEY,
    support_session_id uuid NOT NULL REFERENCES support_session,
    requested_scope varchar(64) NOT NULL
        CHECK (requested_scope IN ('SUPPORT_REVERSE_PAYMENT', 'SUPPORT_TRANSFER_OWNERSHIP',
                                    'SUPPORT_REMOVE_BUILDING_ADMIN', 'SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED')),
    status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'DENIED')),
    approved_by uuid,
    requested_at timestamptz NOT NULL,
    decided_at timestamptz,
    decision_reason text,
    CHECK ((status = 'PENDING' AND approved_by IS NULL AND decided_at IS NULL)
        OR (status IN ('APPROVED', 'DENIED') AND decided_at IS NOT NULL))
);
CREATE INDEX idx_elevated_approval_request_session ON elevated_approval_request (support_session_id);
CREATE UNIQUE INDEX uq_elevated_approval_request_pending
    ON elevated_approval_request (support_session_id, requested_scope) WHERE status = 'PENDING';
