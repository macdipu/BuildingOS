-- F4-T2: floors and individual units (UO-02/03, UO-D03). Numbers are unique per building after trim + upper-case.
CREATE TABLE building_floor (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES building,
    label varchar(40) NOT NULL CHECK (length(trim(label)) > 0),
    normalized_label varchar(40) NOT NULL,
    kind varchar(16) NOT NULL CHECK (kind IN ('BASEMENT', 'GROUND', 'REGULAR', 'ROOF', 'COMMON')),
    display_order integer NOT NULL,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (building_id, normalized_label),
    UNIQUE (building_id, id)
);
CREATE INDEX building_floor_order_idx ON building_floor (building_id, display_order, id);

CREATE TABLE building_unit (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES building,
    floor_id uuid NOT NULL,
    number varchar(32) NOT NULL CHECK (length(trim(number)) > 0),
    normalized_number varchar(32) NOT NULL,
    unit_type varchar(16) NOT NULL
        CHECK (unit_type IN ('FLAT', 'PARKING', 'STORAGE', 'COMMERCIAL', 'COMMON', 'OTHER')),
    area_sqft numeric(12, 2) NOT NULL CHECK (area_sqft > 0),
    bedrooms smallint CHECK (bedrooms >= 0),
    default_maintenance_rate numeric(12, 2) CHECK (default_maintenance_rate >= 0),
    notes varchar(1000),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (building_id, normalized_number),
    UNIQUE (building_id, id),
    FOREIGN KEY (building_id, floor_id) REFERENCES building_floor (building_id, id)
);
CREATE INDEX building_unit_floor_idx ON building_unit (building_id, floor_id);
