#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
SEED_DIR="$ROOT/tools/dev"
COMPOSE_FILE="$ROOT/docker-compose.yml"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
DEV_SEED_USER_ID="${DEV_SEED_USER_ID:-00000000-0000-4000-8000-000000000001}"

usage() {
  printf '%s\n' \
    'Usage:' \
    '  sh tools/dev/seed-dev.sh --list' \
    '  sh tools/dev/seed-dev.sh --confirm-dev <identity|organization|project|work|bug|recruitment|notification|payment|all>' \
    '  DEV_SEED_IDENTITY_USER_ID=<profile-uuid> sh tools/dev/seed-dev.sh --confirm-dev --set-account-role <USER|ADMIN|MANAGER|COMPANY_OWNER>' \
    '' \
    'Seeds are local-development only; the script never starts services.'
}

if [ "$#" -eq 1 ] && [ "$1" = "--list" ]; then
  printf '%s\n' identity organization project work bug recruitment notification payment
  exit 0
fi

if [ "$#" -eq 3 ] && [ "$1" = "--confirm-dev" ] && [ "$2" = "--set-account-role" ]; then
  account_role=$3
  case "$account_role" in
    USER|ADMIN|MANAGER|COMPANY_OWNER) ;;
    *)
      printf 'Unsupported identity account role: %s\n' "$account_role" >&2
      printf '%s\n' 'Allowed roles: USER, ADMIN, MANAGER, COMPANY_OWNER' >&2
      exit 2
      ;;
  esac

  if [ -z "${DEV_SEED_IDENTITY_USER_ID:-}" ] ||
    ! printf '%s\n' "$DEV_SEED_IDENTITY_USER_ID" |
      LC_ALL=C grep -Eq '^[[:xdigit:]]{8}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{12}$'; then
    printf '%s\n' 'Set DEV_SEED_IDENTITY_USER_ID to an existing Workora identity profile UUID.' >&2
    exit 2
  fi
  if ! command -v docker >/dev/null 2>&1; then
    printf '%s\n' 'docker is required to update a local identity role.' >&2
    exit 127
  fi

  sql_file="$SEED_DIR/identity-role.sql"
  if [ ! -f "$sql_file" ]; then
    printf 'Missing role update SQL file: %s\n' "$sql_file" >&2
    exit 1
  fi
  printf 'Setting local identity profile %s to role %s...\n' "$DEV_SEED_IDENTITY_USER_ID" "$account_role"
  docker compose --project-name "$COMPOSE_PROJECT" \
    -f "$COMPOSE_FILE" --project-directory "$ROOT" \
    exec -T -e "DEV_SEED_IDENTITY_USER_ID=$DEV_SEED_IDENTITY_USER_ID" \
    -e "DEV_SEED_ACCOUNT_ROLE=$account_role" identity-db sh -c \
    'exec psql --set ON_ERROR_STOP=1 --set="identity_user_id=$DEV_SEED_IDENTITY_USER_ID" --set="account_role=$DEV_SEED_ACCOUNT_ROLE" --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"' \
    < "$sql_file"
  exit 0
fi

if [ "$#" -ne 2 ] || [ "$1" != "--confirm-dev" ]; then
  usage >&2
  exit 2
fi

target=$2
case "$target" in
  identity|organization|project|work|bug|recruitment|notification|payment|all) ;;
  *)
    printf 'Unknown seed target: %s\n' "$target" >&2
    usage >&2
    exit 2
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  printf '%s\n' 'docker is required to run database seeds.' >&2
  exit 127
fi

if ! printf '%s\n' "$DEV_SEED_USER_ID" |
  LC_ALL=C grep -Eq '^[[:xdigit:]]{8}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{12}$'; then
  printf '%s\n' 'DEV_SEED_USER_ID must be a UUID.' >&2
  exit 2
fi

seed_one() {
  service=$1
  sql_file="$SEED_DIR/$service.sql"
  if [ ! -f "$sql_file" ]; then
    printf 'Missing seed SQL file: %s\n' "$sql_file" >&2
    return 1
  fi
  printf 'Applying synthetic %s fixtures to local Compose service %s...\n' "$service" "$service-db"
  docker compose --project-name "$COMPOSE_PROJECT" \
    -f "$COMPOSE_FILE" --project-directory "$ROOT" \
    exec -T -e "DEV_SEED_USER_ID=$DEV_SEED_USER_ID" "$service-db" sh -c \
    'exec psql --set ON_ERROR_STOP=1 --set="dev_user_id=$DEV_SEED_USER_ID" --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"' \
    < "$sql_file"
}

if [ "$target" = all ]; then
  for service in identity organization project work bug recruitment notification payment; do
    if ! seed_one "$service"; then
      printf 'Stopping after failed %s seed; remaining databases were not attempted.\n' "$service" >&2
      exit 1
    fi
  done
else
  seed_one "$target"
fi
