CREATE SEQUENCE building_application_number_seq;

CREATE TABLE building_application (
    id uuid PRIMARY KEY,
    application_number varchar(16) NOT NULL UNIQUE,
    applicant_user_id uuid NOT NULL,
    building_name varchar(200),
    building_type varchar(16),
    address varchar(500),
    area varchar(120),
    district varchar(120),
    postal_code varchar(16),
    total_floors int CHECK (total_floors > 0),
    estimated_units int CHECK (estimated_units > 0),
    applicant_relationship varchar(24),
    relationship_note varchar(200),
    contact_name varchar(200),
    contact_phone varchar(11),
    contact_email varchar(254),
    management_type varchar(24),
    latitude numeric(9, 6),
    longitude numeric(9, 6),
    source varchar(16) NOT NULL,
    status varchar(32) NOT NULL,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    reviewed_by uuid,
    rejection_reason varchar(1000),
    info_request_message varchar(1000),
    version int NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CHECK ((latitude IS NULL) = (longitude IS NULL))
);
CREATE INDEX building_application_applicant_idx ON building_application (applicant_user_id, created_at);
CREATE INDEX building_application_status_idx ON building_application (status, submitted_at);

CREATE TABLE internal_note (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES building_application,
    author_user_id uuid NOT NULL,
    body varchar(2000) NOT NULL,
    created_at timestamptz NOT NULL
);
CREATE INDEX internal_note_application_idx ON internal_note (application_id, created_at);

CREATE TABLE lifecycle_transition (
    id uuid PRIMARY KEY,
    entity_type varchar(32) NOT NULL,
    entity_id uuid NOT NULL,
    from_status varchar(32),
    to_status varchar(32) NOT NULL,
    actor_user_id uuid NOT NULL,
    reason varchar(1000),
    occurred_at timestamptz NOT NULL
);
CREATE INDEX lifecycle_transition_entity_idx ON lifecycle_transition (entity_type, entity_id, occurred_at);
