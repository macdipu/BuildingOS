-- F4-T5b: transfer documents (UO-13). Bytes live in object storage; removal is recorded, never a row delete.
CREATE TABLE ownership_document (
    id uuid PRIMARY KEY,
    building_id uuid NOT NULL,
    transfer_id uuid NOT NULL,
    object_key varchar(200) NOT NULL UNIQUE,
    file_name varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes > 0),
    uploaded_by uuid NOT NULL,
    uploaded_at timestamptz NOT NULL,
    removed_by uuid,
    removed_at timestamptz,
    removal_reason varchar(1000),
    FOREIGN KEY (building_id, transfer_id) REFERENCES ownership_transfer (building_id, id),
    CHECK ((removed_at IS NULL AND removed_by IS NULL AND removal_reason IS NULL)
        OR (removed_at IS NOT NULL AND removed_by IS NOT NULL AND length(trim(removal_reason)) > 0))
);
CREATE INDEX ownership_document_transfer_idx ON ownership_document (transfer_id, uploaded_at) WHERE removed_at IS NULL;
