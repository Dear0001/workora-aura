-- DEV ONLY: explicitly change the role of one existing local identity profile.
BEGIN;

SELECT set_config('workora.dev_seed_identity_user_id', :'identity_user_id', true);
SELECT set_config('workora.dev_seed_account_role', :'account_role', true);

DO $role_update$
DECLARE
    target_id UUID := current_setting('workora.dev_seed_identity_user_id', true)::UUID;
    target_role TEXT := current_setting('workora.dev_seed_account_role', true);
    updated_rows INTEGER;
BEGIN
    IF target_role NOT IN ('USER', 'ADMIN', 'MANAGER', 'COMPANY_OWNER') THEN
        RAISE EXCEPTION 'Unsupported identity account role: %', target_role;
    END IF;

    UPDATE users
    SET role = target_role
    WHERE id = target_id;

    GET DIAGNOSTICS updated_rows = ROW_COUNT;
    IF updated_rows <> 1 THEN
        RAISE EXCEPTION 'Expected one existing identity profile for %, updated % rows',
            target_id, updated_rows;
    END IF;
END;
$role_update$;

SELECT id, role FROM users
WHERE id = current_setting('workora.dev_seed_identity_user_id', true)::UUID;

COMMIT;
