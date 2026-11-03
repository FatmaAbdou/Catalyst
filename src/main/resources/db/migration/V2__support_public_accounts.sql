-- Add public account roles without exposing staff roles during sign-up.
INSERT INTO roles (role_name) VALUES ('DONOR'), ('VOLUNTEER')
ON CONFLICT (role_name) DO NOTHING;

ALTER TABLE donors
    ADD COLUMN organization VARCHAR(200);

-- Email addresses are stored in lowercase by the application and are unique
-- regardless of letter case.
CREATE UNIQUE INDEX uq_users_email_case_insensitive ON users (LOWER(email));
