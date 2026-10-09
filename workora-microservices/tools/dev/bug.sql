-- DEV ONLY: synthetic bug report, evidence metadata, and history.
BEGIN;

INSERT INTO bug_reports (
    id, phase_id, task_id, project_id, department_id, company_id, reporter_id,
    project_manager_id, title, description, problem, location, category, severity,
    status, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000007',
    '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000006',
    '00000000-0000-4000-8000-000000000003',
    '00000000-0000-4000-8000-000000000012',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    :'dev_user_id',
    'Sample validation issue', 'Synthetic bug report for development.',
    'Example validation edge case.', 'Sample form',
    'FUNCTIONAL', 'MEDIUM', 'SUBMITTED',
    CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'
)
ON CONFLICT (id) DO UPDATE SET
    phase_id = EXCLUDED.phase_id, task_id = EXCLUDED.task_id,
    project_id = EXCLUDED.project_id, department_id = EXCLUDED.department_id,
    company_id = EXCLUDED.company_id, reporter_id = EXCLUDED.reporter_id,
    project_manager_id = EXCLUDED.project_manager_id, title = EXCLUDED.title,
    description = EXCLUDED.description, problem = EXCLUDED.problem,
    location = EXCLUDED.location, category = EXCLUDED.category,
    severity = EXCLUDED.severity, status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO bug_evidence (
    id, report_id, file_key, mime_type, description, uploaded_by, uploaded_at
) VALUES (
    '00000000-0000-4000-8000-000000000028',
    '00000000-0000-4000-8000-000000000007',
    'dev-seed/bug/evidence.png', 'image/png',
    'Synthetic file metadata; no file is uploaded.',
    :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    report_id = EXCLUDED.report_id, file_key = EXCLUDED.file_key,
    mime_type = EXCLUDED.mime_type, description = EXCLUDED.description,
    uploaded_by = EXCLUDED.uploaded_by, uploaded_at = EXCLUDED.uploaded_at;

INSERT INTO bug_status_history (
    id, report_id, from_status, to_status, actor_id, reason, changed_at
) VALUES (
    '00000000-0000-4000-8000-000000000029',
    '00000000-0000-4000-8000-000000000007',
    NULL, 'SUBMITTED', :'dev_user_id',
    'Synthetic report submission.',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    report_id = EXCLUDED.report_id, from_status = EXCLUDED.from_status,
    to_status = EXCLUDED.to_status, actor_id = EXCLUDED.actor_id,
    reason = EXCLUDED.reason, changed_at = EXCLUDED.changed_at;

COMMIT;
