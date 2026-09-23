CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    phone varchar(32) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE platform_user_role (
    user_id uuid NOT NULL REFERENCES app_user(id),
    role varchar(32) NOT NULL,
    granted_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE otp_challenge (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    phone varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    attempt_count int NOT NULL DEFAULT 0,
    consumed_at timestamptz
);

CREATE INDEX otp_challenge_phone_idx ON otp_challenge (phone);
