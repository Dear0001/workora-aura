set -eu

create_role() {
  psql \
    --set=ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set="role_name=$1" \
    --set="role_password=$2" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'role_name', :'role_password') \gexec
SQL
}

create_service_database() {
  database=$1
  role_name=$2

  if [ "$database" = "$POSTGRES_DB" ]; then
    psql \
      --set=ON_ERROR_STOP=1 \
      --username "$POSTGRES_USER" \
      --dbname "$POSTGRES_DB" \
      --set="database=$database" \
      --set="role_name=$role_name" <<'SQL'
SELECT format('ALTER DATABASE %I OWNER TO %I', :'database', :'role_name') \gexec
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'database') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database', :'role_name') \gexec
SQL
  else
    psql \
      --set=ON_ERROR_STOP=1 \
      --username "$POSTGRES_USER" \
      --dbname "$POSTGRES_DB" \
      --set="database=$database" \
      --set="role_name=$role_name" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER TO %I', :'database', :'role_name') \gexec
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'database') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database', :'role_name') \gexec
SQL
  fi
}

create_role "$DB_IDENTITY_USERNAME" "$DB_IDENTITY_PASSWORD"
create_service_database identity_db "$DB_IDENTITY_USERNAME"
create_role "$DB_ORGANIZATION_USERNAME" "$DB_ORGANIZATION_PASSWORD"
create_service_database organization_db "$DB_ORGANIZATION_USERNAME"
create_role "$DB_PROJECT_USERNAME" "$DB_PROJECT_PASSWORD"
create_service_database project_db "$DB_PROJECT_USERNAME"
create_role "$DB_WORK_USERNAME" "$DB_WORK_PASSWORD"
create_service_database task_db "$DB_WORK_USERNAME"
create_role "$DB_BUG_USERNAME" "$DB_BUG_PASSWORD"
create_service_database bug_db "$DB_BUG_USERNAME"
create_role "$DB_RECRUITMENT_USERNAME" "$DB_RECRUITMENT_PASSWORD"
create_service_database recruitment_db "$DB_RECRUITMENT_USERNAME"
create_role "$DB_NOTIFICATION_USERNAME" "$DB_NOTIFICATION_PASSWORD"
create_service_database notification_db "$DB_NOTIFICATION_USERNAME"
create_role "$DB_PAYMENT_USERNAME" "$DB_PAYMENT_PASSWORD"
create_service_database payment_db "$DB_PAYMENT_USERNAME"
