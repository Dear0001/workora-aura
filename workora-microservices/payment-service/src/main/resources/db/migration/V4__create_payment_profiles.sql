create table payment_profiles (
    id uuid primary key,
    owner_id uuid not null,
    provider varchar(30) not null,
    wallet_name varchar(255) not null,
    phone_number varchar(40) not null,
    primary_profile boolean not null,
    qr_payload varchar(4000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_payment_profile_owner_provider unique (owner_id, provider)
);
