#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
COMPOSE=(docker compose --project-name "$PROJECT" --file "$ROOT/docker-compose.yml")

cleanup() {
  rm -f /tmp/workora-e2e-register.json
  if [[ "${KEEP_STACK:-false}" != "true" ]]; then
    "${COMPOSE[@]}" down --remove-orphans >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

cd "$ROOT"
"${COMPOSE[@]}" up -d --build

wait_http() {
  local name="$1" url="$2"
  for attempt in {1..90}; do
    if curl --fail --silent --max-time 3 "$url" >/dev/null; then
      printf 'PASS %-24s %s\n' "$name" "$url"
      return
    fi
    sleep 2
  done
  printf 'FAIL %-24s %s\n' "$name" "$url" >&2
  "${COMPOSE[@]}" logs --tail=120 "$name" >&2 || true
  exit 1
}

wait_exec() {
  local name="$1" command="$2"
  for attempt in {1..60}; do
    if "${COMPOSE[@]}" exec -T "$name" sh -c "$command" >/dev/null 2>&1; then
      printf 'PASS %-24s %s\n' "$name" "$command"
      return
    fi
    sleep 2
  done
  printf 'FAIL %-24s %s\n' "$name" "$command" >&2
  exit 1
}

wait_http keycloak http://localhost:8180/realms/master
wait_http minio http://localhost:9000/minio/health/live
wait_exec rabbitmq 'rabbitmq-diagnostics -q ping'
wait_exec redis 'redis-cli ping'

for database in identity_db organization_db project_db task_db bug_db recruitment_db notification_db payment_db; do
  wait_exec postgres "pg_isready -U workora -d $database"
done

wait_http api-gateway http://localhost:8080/actuator/health
wait_http identity-service http://localhost:8101/actuator/health
wait_http organization-service http://localhost:8102/actuator/health
wait_http project-service http://localhost:8103/actuator/health
wait_http work-service http://localhost:8104/actuator/health
wait_http bug-service http://localhost:8105/actuator/health
wait_http recruitment-service http://localhost:8106/actuator/health
wait_http notification-service http://localhost:8107/actuator/health
wait_http payment-service http://localhost:8108/actuator/health

if ! "${COMPOSE[@]}" ps --status exited minio-init | grep -q 'minio-init'; then
  echo "FAIL MinIO bucket initialization did not complete" >&2
  exit 1
fi
echo "PASS MinIO workora-files bucket"

email="e2e-$(date +%s)@workora.test"
password='WorkoraE2E!123'
register_status="$(curl --silent --output /tmp/workora-e2e-register.json --write-out '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$email\",\"password\":\"$password\",\"firstName\":\"E2E\",\"lastName\":\"User\"}" \
  http://localhost:8080/api/identity/api/v1/auth/register)"
if [[ "$register_status" != "200" && "$register_status" != "201" ]]; then
  echo "FAIL identity registration returned HTTP $register_status" >&2
  cat /tmp/workora-e2e-register.json >&2
  exit 1
fi
echo "PASS identity registration"

token_response="$(curl --silent --fail \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode grant_type=password \
  --data-urlencode client_id=workora-api \
  --data-urlencode username="$email" \
  --data-urlencode password="$password" \
  http://localhost:8180/realms/workora/protocol/openid-connect/token)"
token="$(python -c "import json,sys; print(json.load(sys.stdin)['access_token'])" <<< "$token_response")"
if [[ -z "$token" ]]; then
  echo "FAIL Keycloak token acquisition" >&2
  exit 1
fi
echo "PASS Keycloak token acquisition"

curl --silent --fail -H "Authorization: Bearer $token" \
  http://localhost:8080/api/identity/api/v1/users/me >/dev/null
echo "PASS authenticated identity profile"

storage_response="$(curl --silent --fail -H "Authorization: Bearer $token" \
  -H 'Content-Type: application/json' \
  -d '{"namespace":"projects","fileName":"e2e.txt","contentType":"text/plain"}' \
  http://localhost:8080/api/project/api/v1/storage/uploads)"
python -c "import json,sys; body=json.load(sys.stdin); assert body['success'] and body['data']['key'].startswith('projects/')" <<< "$storage_response"
echo "PASS presigned storage workflow"

rm -f /tmp/workora-e2e-register.json
echo "Docker Compose infrastructure and authenticated workflow checks passed."
