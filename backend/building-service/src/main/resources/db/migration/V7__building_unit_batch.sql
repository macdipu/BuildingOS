-- F4-T3a: all-or-nothing unit batches (UO-04); a unit keeps the batch that created it.
CREATE TABLE building_unit_batch (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL REFERENCES building,
    created_by uuid NOT NULL,
    row_count integer NOT NULL CHECK (row_count > 0),
    created_at timestamptz NOT NULL,
    UNIQUE (building_id, id)
);

ALTER TABLE building_unit
    ADD COLUMN batch_id uuid,
    ADD CONSTRAINT building_unit_batch_fk FOREIGN KEY (building_id, batch_id)
        REFERENCES building_unit_batch (building_id, id);
CREATE INDEX building_unit_batch_idx ON building_unit (building_id, batch_id) WHERE batch_id IS NOT NULL;
