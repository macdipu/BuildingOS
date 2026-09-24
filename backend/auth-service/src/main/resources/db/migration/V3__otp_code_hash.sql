ALTER TABLE otp_challenge ADD COLUMN code_hash varchar(64);
CREATE INDEX otp_challenge_phone_created_at_idx ON otp_challenge (phone, created_at);
