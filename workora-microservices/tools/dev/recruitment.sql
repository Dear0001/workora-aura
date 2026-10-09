-- DEV ONLY: synthetic recruitment post and application workflow data.
BEGIN;

INSERT INTO recruitment_posts (
    id, company_id, department_id, project_id, title, description,
    fee, currency, intended_role, author_id, status, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000008',
    '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000012',
    '00000000-0000-4000-8000-000000000003',
    'Sample frontend contributor', 'Synthetic recruitment post for development.',
    100.00, 'USD', 'DEVELOPER',
    :'dev_user_id',
    'OPEN', TIMESTAMPTZ '2024-01-01 00:00:00+00',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, department_id = EXCLUDED.department_id,
    project_id = EXCLUDED.project_id, title = EXCLUDED.title,
    description = EXCLUDED.description, fee = EXCLUDED.fee,
    currency = EXCLUDED.currency, intended_role = EXCLUDED.intended_role,
    author_id = EXCLUDED.author_id, status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO applications (
    id, post_id, applicant_id, applicant_email, cover_note, status,
    manager_note, owner_note, submitted_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000009',
    '00000000-0000-4000-8000-000000000008',
    :'dev_user_id',
    'dev.owner@example.test', 'Synthetic application for development.',
    'SUBMITTED', NULL, NULL,
    TIMESTAMPTZ '2024-01-02 00:00:00+00',
    TIMESTAMPTZ '2024-01-02 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    post_id = EXCLUDED.post_id, applicant_id = EXCLUDED.applicant_id,
    applicant_email = EXCLUDED.applicant_email, cover_note = EXCLUDED.cover_note,
    status = EXCLUDED.status, manager_note = EXCLUDED.manager_note,
    owner_note = EXCLUDED.owner_note, submitted_at = EXCLUDED.submitted_at,
    updated_at = EXCLUDED.updated_at;

INSERT INTO application_structured_data (application_id, entry, entry_index)
VALUES
    ('00000000-0000-4000-8000-000000000009', 'experience=synthetic', 0),
    ('00000000-0000-4000-8000-000000000009', 'portfolio=https://example.test', 1)
ON CONFLICT DO NOTHING;

INSERT INTO application_files (id, application_id, file_key, mime_type, uploaded_at)
VALUES (
    '00000000-0000-4000-8000-000000000030',
    '00000000-0000-4000-8000-000000000009',
    'dev-seed/recruitment/sample-resume.pdf', 'application/pdf',
    TIMESTAMPTZ '2024-01-02 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    application_id = EXCLUDED.application_id, file_key = EXCLUDED.file_key,
    mime_type = EXCLUDED.mime_type, uploaded_at = EXCLUDED.uploaded_at;

INSERT INTO application_history (
    id, application_id, from_status, to_status, actor_id, reason, changed_at
) VALUES (
    '00000000-0000-4000-8000-000000000031',
    '00000000-0000-4000-8000-000000000009',
    NULL, 'SUBMITTED', :'dev_user_id',
    'Synthetic application history.',
    TIMESTAMPTZ '2024-01-02 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    application_id = EXCLUDED.application_id, from_status = EXCLUDED.from_status,
    to_status = EXCLUDED.to_status, actor_id = EXCLUDED.actor_id,
    reason = EXCLUDED.reason, changed_at = EXCLUDED.changed_at;

COMMIT;
