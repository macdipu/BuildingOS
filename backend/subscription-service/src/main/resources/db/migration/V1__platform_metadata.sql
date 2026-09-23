CREATE TABLE platform_metadata (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    service_name varchar(64) NOT NULL,
    installed_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO platform_metadata (singleton, service_name) VALUES (true, 'subscription-service');
REVOKE ALL ON SCHEMA public FROM PUBLIC;
