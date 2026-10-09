-- DEV ONLY: linked synthetic company, membership, and audit fixtures.
BEGIN;

INSERT INTO companies (
    id, name, description, contact_email, owner_user_id, average_rating,
    rating_count, status, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000002',
    'Dev Sample Company', 'Synthetic organization fixture.',
    'dev.owner@example.test', :'dev_user_id',
    5.0, 1, 'ACTIVE',
    TIMESTAMPTZ '2024-01-01 00:00:00+00', TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, description = EXCLUDED.description,
    contact_email = EXCLUDED.contact_email, owner_user_id = EXCLUDED.owner_user_id,
    average_rating = EXCLUDED.average_rating, rating_count = EXCLUDED.rating_count,
    status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

INSERT INTO departments (id, company_id, name, description, status, created_at, updated_at)
VALUES (
    '00000000-0000-4000-8000-000000000012',
    '00000000-0000-4000-8000-000000000002',
    'Engineering', 'Synthetic engineering department.', 'ACTIVE',
    TIMESTAMPTZ '2024-01-01 00:00:00+00', TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, name = EXCLUDED.name,
    description = EXCLUDED.description, status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO memberships (
    id, company_id, user_id, email, role, status, department_id, invited_by,
    created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000013',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    'dev.owner@example.test', 'OWNER', 'ACTIVE',
    '00000000-0000-4000-8000-000000000012',
    :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00', TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, user_id = EXCLUDED.user_id,
    email = EXCLUDED.email, role = EXCLUDED.role, status = EXCLUDED.status,
    department_id = EXCLUDED.department_id, invited_by = EXCLUDED.invited_by,
    updated_at = EXCLUDED.updated_at;

INSERT INTO memberships (
    id, company_id, user_id, email, role, status, department_id, invited_by,
    created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000038',
    '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000099',
    'dev.member@example.test', 'DEVELOPER', 'ACTIVE',
    '00000000-0000-4000-8000-000000000012',
    :'dev_user_id',
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, user_id = EXCLUDED.user_id,
    email = EXCLUDED.email, role = EXCLUDED.role, status = EXCLUDED.status,
    department_id = EXCLUDED.department_id, invited_by = EXCLUDED.invited_by,
    updated_at = EXCLUDED.updated_at;

INSERT INTO membership_permissions (membership_id, permission)
VALUES
    ('00000000-0000-4000-8000-000000000013', 'MANAGE_COMPANY'),
    ('00000000-0000-4000-8000-000000000013', 'MANAGE_MEMBERS'),
    ('00000000-0000-4000-8000-000000000013', 'CREATE_PROJECTS'),
    ('00000000-0000-4000-8000-000000000038', 'VIEW_REPORTS')
ON CONFLICT DO NOTHING;

INSERT INTO invitations (
    id, company_id, email, role, department_id, token, status, invited_by, created_at, expires_at
) VALUES (
    '00000000-0000-4000-8000-000000000014',
    '00000000-0000-4000-8000-000000000002',
    'dev.invitee@example.test', 'DEVELOPER',
    '00000000-0000-4000-8000-000000000012',
    'dev-seed-invitation-token-not-for-production', 'REVOKED',
    :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00', TIMESTAMPTZ '2024-01-08 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, email = EXCLUDED.email, role = EXCLUDED.role,
    department_id = EXCLUDED.department_id, token = EXCLUDED.token,
    status = EXCLUDED.status, invited_by = EXCLUDED.invited_by,
    created_at = EXCLUDED.created_at, expires_at = EXCLUDED.expires_at;

INSERT INTO membership_audit (
    id, company_id, user_id, actor_user_id, action, role_before, role_after, details, created_at
) VALUES (
    '00000000-0000-4000-8000-000000000015',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    :'dev_user_id',
    'MEMBERSHIP_CREATED', NULL, 'OWNER', 'Synthetic seed fixture.',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, user_id = EXCLUDED.user_id,
    actor_user_id = EXCLUDED.actor_user_id, action = EXCLUDED.action,
    role_before = EXCLUDED.role_before, role_after = EXCLUDED.role_after,
    details = EXCLUDED.details, created_at = EXCLUDED.created_at;

INSERT INTO company_ratings (id, company_id, user_id, score, review, created_at)
VALUES (
    '00000000-0000-4000-8000-000000000016',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    5, 'Synthetic development rating.',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, user_id = EXCLUDED.user_id,
    score = EXCLUDED.score, review = EXCLUDED.review, created_at = EXCLUDED.created_at;

INSERT INTO subscriptions (
    id, user_id, plan, max_companies, entitled, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000017',
    :'dev_user_id',
    'FREE', 1, false,
    TIMESTAMPTZ '2024-01-01 00:00:00+00', TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id, plan = EXCLUDED.plan,
    max_companies = EXCLUDED.max_companies, entitled = EXCLUDED.entitled,
    updated_at = EXCLUDED.updated_at;

COMMIT;
