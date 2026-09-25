-- Audited status changes of back-office entities (BOC-06, AU-01). from_status is null on creation;
-- actor_user_id is null for a system transition (automatic expiry of an assisted onboarding session).
CREATE TABLE lifecycle_transition (
    id uuid PRIMARY KEY,
    entity_type varchar(40) NOT NULL,
    entity_id uuid NOT NULL,
    from_status varchar(32),
    to_status varchar(32) NOT NULL,
    actor_user_id uuid,
    reason varchar(1000),
    occurred_at timestamptz NOT NULL
);
CREATE INDEX idx_lifecycle_transition_entity ON lifecycle_transition (entity_type, entity_id, occurred_at);

-- Lazy expiry looks up active sessions at or past expires_at.
CREATE INDEX idx_assisted_onboarding_session_due ON assisted_onboarding_session (expires_at)
    WHERE status IN ('REQUESTED', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER');
