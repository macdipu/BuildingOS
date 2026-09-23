-- BOS-010 F5a revenue foundation (D-22..D-27). Forward-only.
CREATE TABLE free_tier (
    id smallint PRIMARY KEY CHECK (id = 1),
    entitlements jsonb NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);
-- Initial free tier per D-23: maintenance for every user. Editable afterwards (RV-03).
INSERT INTO free_tier (id, entitlements) VALUES (1, '{"maintenance.enabled": true}');

CREATE TABLE subscription_plan (
    id uuid PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    status varchar(16) NOT NULL,
    billing_cycles jsonb NOT NULL,
    self_service boolean NOT NULL,
    entitlements jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE subscription (
    id uuid PRIMARY KEY,
    subject_type varchar(16) NOT NULL,
    subject_id uuid NOT NULL,
    plan_id uuid NOT NULL REFERENCES subscription_plan (id),
    status varchar(16) NOT NULL,
    billing_cycle varchar(16) NOT NULL,
    granted_by varchar(16) NOT NULL,
    started_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX subscription_one_active_per_subject
    ON subscription (subject_type, subject_id) WHERE status = 'ACTIVE';
CREATE INDEX subscription_plan_idx ON subscription (plan_id);

-- No fee row is seeded: amounts are operator data (D-24).
CREATE TABLE fee_schedule (
    code varchar(32) PRIMARY KEY,
    amount numeric(12, 2) NOT NULL CHECK (amount >= 0),
    currency char(3) NOT NULL,
    required boolean NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE payment_record (
    id uuid PRIMARY KEY,
    fee_code varchar(32) NOT NULL REFERENCES fee_schedule (code),
    reference_type varchar(32) NOT NULL,
    reference_id uuid NOT NULL,
    amount numeric(12, 2) NOT NULL CHECK (amount > 0),
    currency char(3) NOT NULL,
    method varchar(16) NOT NULL,
    external_reference varchar(128),
    paid_on date NOT NULL,
    recorded_by uuid NOT NULL,
    recorded_at timestamptz NOT NULL
);
CREATE INDEX payment_record_reference_idx ON payment_record (fee_code, reference_type, reference_id);

CREATE TABLE audit_event (
    id uuid PRIMARY KEY,
    actor_user_id uuid NOT NULL,
    action varchar(64) NOT NULL,
    entity_type varchar(32) NOT NULL,
    entity_id varchar(64) NOT NULL,
    before jsonb,
    after jsonb,
    occurred_at timestamptz NOT NULL
);
CREATE INDEX audit_event_entity_idx ON audit_event (entity_type, entity_id, occurred_at);
