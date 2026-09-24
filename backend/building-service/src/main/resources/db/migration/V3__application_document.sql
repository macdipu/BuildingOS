CREATE TABLE application_document (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES building_application,
    object_key varchar(200) NOT NULL UNIQUE,
    file_name varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes > 0),
    uploaded_by uuid NOT NULL,
    uploaded_at timestamptz NOT NULL
);
CREATE INDEX application_document_application_idx ON application_document (application_id, uploaded_at);
