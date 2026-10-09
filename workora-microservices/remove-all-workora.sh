#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
NETWORK_NAME="${NETWORK_NAME:-workora-microservices_default}"
STATE_FILE="$PROJECT_ROOT/.workora-service-state"

WORKORA_CONTAINERS=(
  workora-rabbitmq
  workora-redis
  workora-zipkin
  workora-minio
  workora-minio-init
  workora-keycloak
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

cd "$PROJECT_ROOT"

echo "Removing all Workora Compose containers, images, volumes, and networks..."
docker compose \
  --project-name "$COMPOSE_PROJECT" \
  down \
  --volumes \
  --remove-orphans \
  --rmi all

echo "Removing fixed-name Workora containers..."
for container in "${WORKORA_CONTAINERS[@]}"; do
  if docker container inspect "$container" >/dev/null 2>&1; then
    docker container rm --force --volumes "$container" >/dev/null
    echo "Removed container $container."
  fi
done

echo "Removing unused Docker images..."
docker image prune -f

echo "Removing all unused Docker images..."
docker image prune -a -f

echo "Removing unused Docker volumes..."
docker volume prune -f

echo "Removing unused Docker networks..."
docker network prune -f

if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network rm "$NETWORK_NAME" >/dev/null
  echo "Removed Docker network $NETWORK_NAME."
else
  echo "Docker network $NETWORK_NAME was not found."
fi

if [[ -f "$STATE_FILE" ]]; then
  rm -f "$STATE_FILE"
fi

echo "All Workora Compose resources have been removed."
