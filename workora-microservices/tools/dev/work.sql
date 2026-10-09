-- DEV ONLY: synthetic task and submission fixtures.
BEGIN;

INSERT INTO tasks (
    id, phase_id, project_id, department_id, company_id, project_manager_id,
    title, description, due_date, status, completion_rule, created_by, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000006',
    '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000003',
    '00000000-0000-4000-8000-000000000012',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    'Validate sample task flow', 'Synthetic task fixture for development UI.',
    CURRENT_DATE, 'ASSIGNED', 'ALL_ASSIGNEES',
    :'dev_user_id',
    CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
)
ON CONFLICT (id) DO UPDATE SET
    phase_id = EXCLUDED.phase_id, project_id = EXCLUDED.project_id,
    department_id = EXCLUDED.department_id, company_id = EXCLUDED.company_id,
    project_manager_id = EXCLUDED.project_manager_id, title = EXCLUDED.title,
    description = EXCLUDED.description, due_date = EXCLUDED.due_date,
    status = EXCLUDED.status, completion_rule = EXCLUDED.completion_rule,
    created_by = EXCLUDED.created_by, updated_at = EXCLUDED.updated_at;

INSERT INTO tasks (
    id, phase_id, project_id, department_id, company_id, project_manager_id,
    title, description, due_date, status, completion_rule, created_by, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000036',
    '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000003',
    '00000000-0000-4000-8000-000000000012',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    'Completed sample task', 'Synthetic completed task for dashboard activity.',
    NULL, 'COMPLETED', 'ALL_ASSIGNEES',
    :'dev_user_id',
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
)
ON CONFLICT (id) DO UPDATE SET
    phase_id = EXCLUDED.phase_id, project_id = EXCLUDED.project_id,
    department_id = EXCLUDED.department_id, company_id = EXCLUDED.company_id,
    project_manager_id = EXCLUDED.project_manager_id, title = EXCLUDED.title,
    description = EXCLUDED.description, due_date = EXCLUDED.due_date,
    status = EXCLUDED.status, completion_rule = EXCLUDED.completion_rule,
    created_by = EXCLUDED.created_by, updated_at = EXCLUDED.updated_at;

INSERT INTO task_assignees (
    id, task_id, user_id, department_id, status, assigned_by, assigned_at
) VALUES (
    '00000000-0000-4000-8000-000000000024',
    '00000000-0000-4000-8000-000000000006',
    :'dev_user_id',
    '00000000-0000-4000-8000-000000000012',
    'ASSIGNED', :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    task_id = EXCLUDED.task_id, user_id = EXCLUDED.user_id,
    department_id = EXCLUDED.department_id, status = EXCLUDED.status,
    assigned_by = EXCLUDED.assigned_by, assigned_at = EXCLUDED.assigned_at;

INSERT INTO task_attachments (
    id, task_id, file_key, file_name, mime_type, description, uploaded_by, uploaded_at
) VALUES (
    '00000000-0000-4000-8000-000000000025',
    '00000000-0000-4000-8000-000000000006',
    'dev-seed/task/requirements.pdf', 'requirements.pdf',
    'application/pdf', 'Synthetic storage metadata; no file is uploaded.',
    :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    task_id = EXCLUDED.task_id, file_key = EXCLUDED.file_key,
    file_name = EXCLUDED.file_name, mime_type = EXCLUDED.mime_type,
    description = EXCLUDED.description, uploaded_by = EXCLUDED.uploaded_by,
    uploaded_at = EXCLUDED.uploaded_at;

INSERT INTO submissions (id, task_id, author_id, version, note, submitted_at)
VALUES (
    '00000000-0000-4000-8000-000000000026',
    '00000000-0000-4000-8000-000000000006',
    :'dev_user_id',
    1, 'Synthetic task submission.',
    TIMESTAMPTZ '2024-01-02 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    task_id = EXCLUDED.task_id, author_id = EXCLUDED.author_id,
    version = EXCLUDED.version, note = EXCLUDED.note,
    submitted_at = EXCLUDED.submitted_at;

INSERT INTO submission_attachments (submission_id, attachment_key)
VALUES ('00000000-0000-4000-8000-000000000026', 'dev-seed/submission/work-proof.png')
ON CONFLICT DO NOTHING;

INSERT INTO task_status_history (
    id, task_id, from_status, to_status, actor_id, reason, changed_at
) VALUES (
    '00000000-0000-4000-8000-000000000027',
    '00000000-0000-4000-8000-000000000006',
    'DRAFT', 'ASSIGNED', :'dev_user_id',
    'Synthetic assignment history.',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    task_id = EXCLUDED.task_id, from_status = EXCLUDED.from_status,
    to_status = EXCLUDED.to_status, actor_id = EXCLUDED.actor_id,
    reason = EXCLUDED.reason, changed_at = EXCLUDED.changed_at;

INSERT INTO task_status_history (
    id, task_id, from_status, to_status, actor_id, reason, changed_at
) VALUES (
    '00000000-0000-4000-8000-000000000037',
    '00000000-0000-4000-8000-000000000036',
    'ACCEPTED', 'COMPLETED', :'dev_user_id',
    'Synthetic task completion history.',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
)
ON CONFLICT (id) DO UPDATE SET
    task_id = EXCLUDED.task_id, from_status = EXCLUDED.from_status,
    to_status = EXCLUDED.to_status, actor_id = EXCLUDED.actor_id,
    reason = EXCLUDED.reason, changed_at = EXCLUDED.changed_at;

COMMIT;
