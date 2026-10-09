-- DEV ONLY: synthetic inbox notification, delivery attempt, and preference.
BEGIN;

INSERT INTO notifications (
    id, user_id, type, title, body, related_entity_type, related_entity_id,
    recipient_email, status, created_at
) VALUES (
    '00000000-0000-4000-8000-000000000010',
    :'dev_user_id',
    'SYSTEM', 'Development sample notification',
    'Synthetic inbox item for development UI.',
    'PROJECT', '00000000-0000-4000-8000-000000000003',
    'dev.owner@example.test', 'CREATED',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id, type = EXCLUDED.type, title = EXCLUDED.title,
    body = EXCLUDED.body, related_entity_type = EXCLUDED.related_entity_type,
    related_entity_id = EXCLUDED.related_entity_id,
    recipient_email = EXCLUDED.recipient_email, status = EXCLUDED.status,
    created_at = EXCLUDED.created_at;

INSERT INTO delivery_attempts (
    id, notification_id, channel, status, attempted_at, error
) VALUES (
    '00000000-0000-4000-8000-000000000032',
    '00000000-0000-4000-8000-000000000010',
    'IN_APP', 'SENT', TIMESTAMPTZ '2024-01-01 00:00:00+00', NULL
)
ON CONFLICT (id) DO UPDATE SET
    notification_id = EXCLUDED.notification_id, channel = EXCLUDED.channel,
    status = EXCLUDED.status, attempted_at = EXCLUDED.attempted_at,
    error = EXCLUDED.error;

INSERT INTO notification_preferences (
    id, user_id, channel, type, enabled, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000033',
    :'dev_user_id',
    'IN_APP', 'SYSTEM', true, TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id, channel = EXCLUDED.channel,
    type = EXCLUDED.type, enabled = EXCLUDED.enabled,
    updated_at = EXCLUDED.updated_at;

COMMIT;
