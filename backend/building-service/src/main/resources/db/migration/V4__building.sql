CREATE TABLE building (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL UNIQUE REFERENCES building_application,
    name varchar(200) NOT NULL,
    building_type varchar(16) NOT NULL,
    address varchar(500) NOT NULL,
    area varchar(120) NOT NULL,
    district varchar(120) NOT NULL,
    postal_code varchar(16),
    latitude numeric(9, 6),
    longitude numeric(9, 6),
    contact_phone varchar(11) NOT NULL,
    status varchar(16) NOT NULL,
    version int NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CHECK ((latitude IS NULL) = (longitude IS NULL))
);
CREATE INDEX building_district_idx ON building (lower(district));
CREATE INDEX building_contact_phone_idx ON building (contact_phone);

CREATE TABLE building_membership (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES building,
    user_id uuid NOT NULL,
    role varchar(32) NOT NULL,
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (building_id, user_id, role)
);
CREATE INDEX building_membership_user_idx ON building_membership (user_id);

CREATE INDEX building_application_district_idx ON building_application (lower(district));
CREATE INDEX building_application_contact_phone_idx ON building_application (contact_phone);
