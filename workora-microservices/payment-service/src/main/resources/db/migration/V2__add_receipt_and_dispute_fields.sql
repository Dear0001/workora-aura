alter table payments
    add column receipt_confirmed_at timestamp with time zone,
    add column dispute_reason varchar(2000);
