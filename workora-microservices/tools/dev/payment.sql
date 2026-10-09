-- DEV ONLY: CREATED payment and synthetic wallet profile; no provider calls occur.
BEGIN;

INSERT INTO payments (
    id, payer_id, reference, amount, currency, purpose, status, provider,
    provider_reference, qr_payload, deep_link, expires_at, paid_at,
    receipt_confirmed_at, dispute_reason, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000034',
    :'dev_user_id',
    'DEV-SEED-PAYMENT-0001', 25.0000, 'USD', 'Synthetic development payment',
    'CREATED', 'DEV_SEED', NULL, NULL, NULL,
    TIMESTAMPTZ '2030-01-01 00:00:00+00', NULL, NULL, NULL,
    TIMESTAMPTZ '2024-01-01 00:00:00+00',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    payer_id = EXCLUDED.payer_id, reference = EXCLUDED.reference,
    amount = EXCLUDED.amount, currency = EXCLUDED.currency,
    purpose = EXCLUDED.purpose, status = EXCLUDED.status,
    provider = EXCLUDED.provider, provider_reference = EXCLUDED.provider_reference,
    qr_payload = EXCLUDED.qr_payload, deep_link = EXCLUDED.deep_link,
    expires_at = EXCLUDED.expires_at, paid_at = EXCLUDED.paid_at,
    receipt_confirmed_at = EXCLUDED.receipt_confirmed_at,
    dispute_reason = EXCLUDED.dispute_reason, updated_at = EXCLUDED.updated_at;

INSERT INTO payment_profiles (
    id, owner_id, provider, wallet_name, phone_number, primary_profile,
    qr_payload, created_at, updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000035',
    :'dev_user_id',
    'DEV_SEED', 'Synthetic Dev Wallet', '+0000000000', true, NULL,
    TIMESTAMPTZ '2024-01-01 00:00:00+00',
    TIMESTAMPTZ '2024-01-01 00:00:00+00'
)
ON CONFLICT (id) DO UPDATE SET
    owner_id = EXCLUDED.owner_id, provider = EXCLUDED.provider,
    wallet_name = EXCLUDED.wallet_name, phone_number = EXCLUDED.phone_number,
    primary_profile = EXCLUDED.primary_profile, qr_payload = EXCLUDED.qr_payload,
    updated_at = EXCLUDED.updated_at;

COMMIT;
