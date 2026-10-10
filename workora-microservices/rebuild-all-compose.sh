#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
NETWORK_NAME="${NETWORK_NAME:-workora-microservices_default}"

CORE_INFRA_SERVICES=(
  redis
  rabbitmq
  zipkin
  minio
)

DATABASE_SERVICES=(
  postgres
)

KEYCLOAK_SERVICE=(keycloak)

WORKORA_CONTAINERS=(
  workora-rabbitmq
  workora-redis
  workora-zipkin
  workora-minio
  workora-minio-init
  workora-keycloak
  workora-postgres
  workora-identity-db
  workora-organization-db
  workora-project-db
  workora-work-db
  workora-bug-db
  workora-recruitment-db
  workora-notification-db
  workora-payment-db
  identity-service
  organization-service
  project-service
  work-service
  bug-service
  recruitment-service
  notification-service
  payment-service
  api-gateway
)

APP_SERVICES=(
  identity-service
  organization-service
  project-service
  work-service
  bug-service
  recruitment-service
  notification-service
  payment-service
  api-gateway
)

log() {
  echo
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

wait_for_http_health() {
  local name="$1"
  local port="$2"
  local tries="${3:-60}"

  for ((i=1; i<=tries; i++)); do
    if curl --silent --fail --max-time 3 "http://localhost:${port}/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  echo "Service ${name} did not become healthy on port ${port}." >&2
  docker compose --project-name "$COMPOSE_PROJECT_NAME" ps "$name" >&2 || true
  docker compose --project-name "$COMPOSE_PROJECT_NAME" logs --tail=80 "$name" >&2 || true
  return 1
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local tries="${3:-60}"

  for ((i=1; i<=tries; i++)); do
    if curl --silent --fail --max-time 3 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  echo "Service ${name} did not become ready at ${url}." >&2
  docker compose --project-name "$COMPOSE_PROJECT_NAME" ps "$name" >&2 || true
  docker compose --project-name "$COMPOSE_PROJECT_NAME" logs --tail=80 "$name" >&2 || true
  return 1
}

wait_for_exec_health() {
  local name="$1"
  local command="$2"
  local tries="${3:-60}"

  for ((i=1; i<=tries; i++)); do
    if docker compose --project-name "$COMPOSE_PROJECT_NAME" exec -T "$name" sh -c "$command" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  echo "Service ${name} did not become ready." >&2
  docker compose --project-name "$COMPOSE_PROJECT_NAME" ps "$name" >&2 || true
  docker compose --project-name "$COMPOSE_PROJECT_NAME" logs --tail=80 "$name" >&2 || true
  return 1
}

wait_for_service() {
  local service="$1"

  case "$service" in
    redis)
      wait_for_exec_health "$service" "redis-cli ping"
      ;;
    rabbitmq)
      if ! wait_for_exec_health "$service" "rabbitmq-diagnostics -q ping"; then
        log "RabbitMQ diagnostics:"
        docker inspect "$service" --format 'status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}not-configured{{end}}' >&2 || true
        docker logs --tail=120 "$service" >&2 || true
        return 1
      fi
      ;;
    zipkin)
      wait_for_url "$service" "http://localhost:9411/health"
      ;;
    minio)
      wait_for_url "$service" "http://localhost:9000/minio/health/live"
      ;;
    keycloak)
      wait_for_url "$service" "http://localhost:8180/realms/master"
      ;;
    *)
      wait_for_exec_health "$service" "pg_isready -U workora"
      ;;
  esac
}

check_docker() {
  if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running or not available in PATH." >&2
    exit 1
  fi
}

check_docker

log "Stopping all containers while preserving database and infrastructure volumes..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" down --remove-orphans || true

log "Removing fixed-name Workora containers left by previous Compose runs..."
for container in "${WORKORA_CONTAINERS[@]}"; do
  if docker container inspect "$container" >/dev/null 2>&1; then
    docker container rm --force "$container" >/dev/null
    log "Removed container $container."
  fi
done

log "Removing unused Docker images..."
docker image prune -f

log "Removing all unused Docker images..."
docker image prune -a -f

log "Removing unused Docker networks..."
docker network prune -f

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  log "Creating Docker network ${NETWORK_NAME}..."
  docker network create "$NETWORK_NAME" >/dev/null
fi

log "Rebuilding all images without cache..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" build --no-cache

log "Starting core infrastructure images once: Redis, RabbitMQ, and Zipkin..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" up -d "${CORE_INFRA_SERVICES[@]}"

log "Waiting for core infrastructure..."
for service in "${CORE_INFRA_SERVICES[@]}"; do
  wait_for_service "$service"
done

log "Initializing the MinIO workora-files bucket..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" up --no-deps minio-init

log "Starting Keycloak once..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" up -d "${KEYCLOAK_SERVICE[@]}"
wait_for_service keycloak

log "Starting the shared PostgreSQL instance..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" up -d "${DATABASE_SERVICES[@]}"

log "Waiting for the shared PostgreSQL instance..."
for service in "${DATABASE_SERVICES[@]}"; do
  wait_for_service "$service"
done

log "Starting application services from the images built above..."
docker compose --project-name "$COMPOSE_PROJECT_NAME" up -d "${APP_SERVICES[@]}"

log "Waiting for app health checks..."
for service in identity-service organization-service project-service work-service bug-service recruitment-service notification-service payment-service api-gateway; do
  case "$service" in
    identity-service) port="${IDENTITY_SERVICE_PORT:-8101}" ;;
    organization-service) port="${ORGANIZATION_SERVICE_PORT:-8102}" ;;
    project-service) port="${PROJECT_SERVICE_PORT:-8103}" ;;
    work-service) port="${WORK_SERVICE_PORT:-8104}" ;;
    bug-service) port="${BUG_SERVICE_PORT:-8105}" ;;
    recruitment-service) port="${RECRUITMENT_SERVICE_PORT:-8106}" ;;
    notification-service) port="${NOTIFICATION_SERVICE_PORT:-8107}" ;;
    payment-service) port="${PAYMENT_SERVICE_PORT:-8108}" ;;
    api-gateway) port="${GATEWAY_PORT:-8080}" ;;
  esac

  if ! wait_for_http_health "$service" "$port" 60; then
    echo "App failed to start: $service" >&2
    exit 1
  fi

done

echo
log "Rebuild complete."
log "Gateway: http://localhost:8080/gateway/swagger-ui.html"
log "Redis: localhost:6379"
log "RabbitMQ: localhost:5672 (UI: http://localhost:15672)"
log "Keycloak: http://localhost:8180"
log "Zipkin: http://localhost:9411"
