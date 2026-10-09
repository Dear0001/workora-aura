-- DEV ONLY: synthetic project, phase, asset, and bug-fix-job data.
BEGIN;

INSERT INTO projects (
    id, company_id, department_id, name, description, project_manager_id,
    status, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000003',
    '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000012',
    'Dev Sample Project', 'Synthetic project fixture.',
    :'dev_user_id', 'ACTIVE',
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'
)
ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id, department_id = EXCLUDED.department_id,
    name = EXCLUDED.name, description = EXCLUDED.description,
    project_manager_id = EXCLUDED.project_manager_id, status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO phases (
    id, project_id, name, description, visibility, status, created_at, updated_at, published_at
) VALUES (
    '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000003',
    'Foundation', 'Synthetic phase fixture.', 'PRIVATE', 'PUBLISHED',
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days',
    CURRENT_TIMESTAMP - INTERVAL '10 days'
)
ON CONFLICT (id) DO UPDATE SET
    project_id = EXCLUDED.project_id, name = EXCLUDED.name,
    description = EXCLUDED.description, visibility = EXCLUDED.visibility,
    status = EXCLUDED.status, updated_at = EXCLUDED.updated_at,
    published_at = EXCLUDED.published_at;

INSERT INTO phase_members (id, phase_id, user_id, email, added_by, created_at)
VALUES (
    '00000000-0000-4000-8000-000000000018',
    '00000000-0000-4000-8000-000000000004',
    :'dev_user_id',
    'dev.owner@example.test', :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    phase_id = EXCLUDED.phase_id, user_id = EXCLUDED.user_id,
    email = EXCLUDED.email, added_by = EXCLUDED.added_by,
    created_at = EXCLUDED.created_at;

INSERT INTO phase_members (id, phase_id, user_id, email, added_by, created_at)
VALUES (
    '00000000-0000-4000-8000-000000000039',
    '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000099',
    'dev.member@example.test', :'dev_user_id',
    CURRENT_TIMESTAMP - INTERVAL '5 days'
)
ON CONFLICT (id) DO UPDATE SET
    phase_id = EXCLUDED.phase_id, user_id = EXCLUDED.user_id,
    email = EXCLUDED.email, added_by = EXCLUDED.added_by,
    created_at = EXCLUDED.created_at;

INSERT INTO project_assets (
    id, project_id, phase_id, name, storage_key, content_type, kind, uploaded_by, created_at
) VALUES (
    '00000000-0000-4000-8000-000000000019',
    '00000000-0000-4000-8000-000000000003',
    '00000000-0000-4000-8000-000000000004',
    'sample-design.png', 'dev-seed/project/sample-design.png',
    'image/png', 'DOCUMENT', :'dev_user_id',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    project_id = EXCLUDED.project_id, phase_id = EXCLUDED.phase_id,
    name = EXCLUDED.name, storage_key = EXCLUDED.storage_key,
    content_type = EXCLUDED.content_type, kind = EXCLUDED.kind,
    uploaded_by = EXCLUDED.uploaded_by, created_at = EXCLUDED.created_at;

INSERT INTO bug_fix_jobs (
    id, phase_id, project_id, department_id, company_id, project_manager_id,
    title, summary, category, fix_expectation, requires_nda,
    fee, currency, split_fee_allowed, closes_after_one_resolution,
    status, job_version, created_at, updated_at, published_at
) VALUES (
    '00000000-0000-4000-8000-000000000005',
    '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000003',
    '00000000-0000-4000-8000-000000000012',
    '00000000-0000-4000-8000-000000000002',
    :'dev_user_id',
    'Review sample validation flow', 'Synthetic bug-fix job fixture.',
    'FUNCTIONAL', 'PATCH', false, 25.00, 'USD', false, true,
    'OPEN', 1,
    TIMESTAMPTZ '2024-01-01 00:00:00+00', TIMESTAMPTZ '2024-01-01 00:00:00+00',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    phase_id = EXCLUDED.phase_id, project_id = EXCLUDED.project_id,
    department_id = EXCLUDED.department_id, company_id = EXCLUDED.company_id,
    project_manager_id = EXCLUDED.project_manager_id, title = EXCLUDED.title,
    summary = EXCLUDED.summary, category = EXCLUDED.category,
    fix_expectation = EXCLUDED.fix_expectation, requires_nda = EXCLUDED.requires_nda,
    fee = EXCLUDED.fee, currency = EXCLUDED.currency,
    split_fee_allowed = EXCLUDED.split_fee_allowed,
    closes_after_one_resolution = EXCLUDED.closes_after_one_resolution,
    status = EXCLUDED.status, job_version = EXCLUDED.job_version,
    updated_at = EXCLUDED.updated_at, published_at = EXCLUDED.published_at;

INSERT INTO bug_fix_job_skills (job_id, skill)
VALUES ('00000000-0000-4000-8000-000000000005', 'API testing')
ON CONFLICT DO NOTHING;

INSERT INTO bug_fix_job_applications (
    id, job_id, applicant_id, applicant_email, proposal, decision, created_at
) VALUES (
    '00000000-0000-4000-8000-000000000020',
    '00000000-0000-4000-8000-000000000005',
    :'dev_user_id',
    'dev.owner@example.test', 'Synthetic proposal for UI development.',
    'PENDING', TIMESTAMPTZ '2024-01-02 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    job_id = EXCLUDED.job_id, applicant_id = EXCLUDED.applicant_id,
    applicant_email = EXCLUDED.applicant_email, proposal = EXCLUDED.proposal,
    decision = EXCLUDED.decision, created_at = EXCLUDED.created_at;

INSERT INTO bug_fix_job_submissions (
    id, job_id, freelancer_id, report_summary, reproduction_details,
    fix_details, version, submitted_at
) VALUES (
    '00000000-0000-4000-8000-000000000021',
    '00000000-0000-4000-8000-000000000005',
    :'dev_user_id',
    'Synthetic submission summary.', 'Synthetic reproduction steps.',
    'Synthetic fix details.', 1, TIMESTAMPTZ '2024-01-03 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    job_id = EXCLUDED.job_id, freelancer_id = EXCLUDED.freelancer_id,
    report_summary = EXCLUDED.report_summary,
    reproduction_details = EXCLUDED.reproduction_details,
    fix_details = EXCLUDED.fix_details, version = EXCLUDED.version,
    submitted_at = EXCLUDED.submitted_at;

INSERT INTO bug_fix_job_submission_evidence (submission_id, evidence_key)
VALUES ('00000000-0000-4000-8000-000000000021', 'dev-seed/evidence/fix-proof.png')
ON CONFLICT DO NOTHING;

INSERT INTO bug_fix_job_reviews (id, job_id, reviewer_id, outcome, reason, created_at)
VALUES (
    '00000000-0000-4000-8000-000000000022',
    '00000000-0000-4000-8000-000000000005',
    :'dev_user_id',
    'TEST_PASSED', 'Synthetic review fixture.',
    TIMESTAMPTZ '2024-01-04 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    job_id = EXCLUDED.job_id, reviewer_id = EXCLUDED.reviewer_id,
    outcome = EXCLUDED.outcome, reason = EXCLUDED.reason,
    created_at = EXCLUDED.created_at;

INSERT INTO bug_fix_job_review_evidence (review_id, evidence_key)
VALUES ('00000000-0000-4000-8000-000000000022', 'dev-seed/evidence/review.png')
ON CONFLICT DO NOTHING;

INSERT INTO bug_fix_job_status_history (
    id, job_id, from_status, to_status, actor_id, reason, changed_at
) VALUES (
    '00000000-0000-4000-8000-000000000023',
    '00000000-0000-4000-8000-000000000005',
    'DRAFT', 'OPEN', :'dev_user_id',
    'Synthetic publication history.',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    job_id = EXCLUDED.job_id, from_status = EXCLUDED.from_status,
    to_status = EXCLUDED.to_status, actor_id = EXCLUDED.actor_id,
    reason = EXCLUDED.reason, changed_at = EXCLUDED.changed_at;

COMMIT;
