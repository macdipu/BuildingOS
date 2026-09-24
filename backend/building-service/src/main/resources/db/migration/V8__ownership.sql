-- F4-T4a: ownership periods, transfers and the transactional outbox (UO-06/07/10, UO-D02 immediate changes).
ALTER TABLE building_unit
    ADD COLUMN ownership_revision bigint NOT NULL DEFAULT 0 CHECK (ownership_revision >= 0);

CREATE TABLE ownership_period (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    share numeric(7, 4) NOT NULL CHECK (share > 0 AND share <= 100),
    start_at timestamptz NOT NULL,
    start_revision bigint NOT NULL CHECK (start_revision > 0),
    end_at timestamptz,
    end_revision bigint,
    notes varchar(1000),
    created_by uuid NOT NULL,
    FOREIGN KEY (building_id, unit_id) REFERENCES building_unit (building_id, id),
    CHECK ((end_at IS NULL AND end_revision IS NULL)
        OR (end_at IS NOT NULL AND end_revision > start_revision AND end_at >= start_at))
);
CREATE UNIQUE INDEX ownership_period_open_owner ON ownership_period (unit_id, owner_user_id) WHERE end_revision IS NULL;
CREATE INDEX ownership_period_owner_idx ON ownership_period (owner_user_id, building_id) WHERE end_revision IS NULL;
CREATE INDEX ownership_period_unit_idx ON ownership_period (unit_id, start_revision, id);

CREATE TABLE ownership_transfer (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    source_owner_user_id uuid NOT NULL,
    recipient_user_id uuid NOT NULL CHECK (recipient_user_id <> source_owner_user_id),
    share numeric(7, 4) NOT NULL CHECK (share > 0 AND share <= 100),
    effective_date date NOT NULL,
    effective_at timestamptz NOT NULL,
    revision bigint NOT NULL CHECK (revision > 0),
    actor_user_id uuid NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    reference varchar(200),
    UNIQUE (unit_id, revision),
    UNIQUE (building_id, id),
    FOREIGN KEY (building_id, unit_id) REFERENCES building_unit (building_id, id)
);
CREATE INDEX ownership_transfer_party_idx ON ownership_transfer (unit_id, source_owner_user_id, recipient_user_id);

CREATE TABLE building_outbox (
    event_id uuid PRIMARY KEY,
    event_type varchar(80) NOT NULL CHECK (event_type ~ '^[a-z0-9-]+\.[a-z0-9-]+$'),
    event_version integer NOT NULL CHECK (event_version >= 1),
    building_id uuid NOT NULL REFERENCES building,
    aggregate_type varchar(40) NOT NULL,
    aggregate_id uuid NOT NULL,
    aggregate_revision bigint NOT NULL,
    correlation_id varchar(128),
    payload jsonb NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    occurred_at timestamptz NOT NULL,
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at timestamptz NOT NULL,
    delivered_at timestamptz,
    last_error varchar(500)
);
CREATE INDEX building_outbox_pending_idx ON building_outbox (next_attempt_at) WHERE delivered_at IS NULL;
