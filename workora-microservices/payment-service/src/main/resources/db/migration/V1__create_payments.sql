create table payments (
    id uuid primary key,
    payer_id uuid not null,
    reference varchar(120) not null,
    amount numeric(19,4) not null,
    currency varchar(3) not null,
    purpose varchar(255) not null,
    status varchar(32) not null,
    provider varchar(32) not null,
    provider_reference varchar(160),
    qr_payload varchar(4000),
    expires_at timestamp with time zone not null,
    paid_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_payments_reference unique (reference)
);
create index idx_payments_payer_created on payments (payer_id, created_at);
create index idx_payments_status on payments (status);
