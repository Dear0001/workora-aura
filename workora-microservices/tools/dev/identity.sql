-- DEV ONLY: synthetic profile and verification-session rows; not Keycloak accounts.
BEGIN;

INSERT INTO users (
    id, email, password, first_name, last_name, account_status, role,
    email_verified, email_verification_attempts, password_reset_attempts
) VALUES (
    '00000000-0000-4000-8000-000000000001',
    'dev.owner@example.test',
    '!DEV_SEED_NO_LOGIN!',
    'Dev', 'Owner', 'ACTIVE', 'USER', false, 0, 0
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    account_status = EXCLUDED.account_status,
    role = EXCLUDED.role,
    email_verified = EXCLUDED.email_verified,
    email_verification_attempts = EXCLUDED.email_verification_attempts,
    password_reset_attempts = EXCLUDED.password_reset_attempts;

INSERT INTO registration_verification_sessions (
    id, token_hash, user_id, email, expires_at, created_at, verified_at
) VALUES (
    '00000000-0000-4000-8000-000000000011',
    '0000000000000000000000000000000000000000000000000000000000000001',
    '00000000-0000-4000-8000-000000000001',
    'dev.owner@example.test',
    TIMESTAMPTZ '2020-01-01 00:00:00+00',
    TIMESTAMPTZ '2020-01-01 00:00:00+00',
    NULL
)
ON CONFLICT (id) DO UPDATE SET
    token_hash = EXCLUDED.token_hash,
    user_id = EXCLUDED.user_id,
    email = EXCLUDED.email,
    expires_at = EXCLUDED.expires_at,
    created_at = EXCLUDED.created_at,
    verified_at = EXCLUDED.verified_at;

COMMIT;
