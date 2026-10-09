#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
NETWORK_NAME="${NETWORK_NAME:-workora-microservices_default}"
STATE_FILE="$PROJECT_ROOT/.workora-service-state"

APPLICATION_SERVICES=(
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

declare -A SERVICE_IMAGES=()

echo "Collecting application images..."
for service in "${APPLICATION_SERVICES[@]}"; do
  while IFS= read -r image; do
    [[ -n "$image" ]] && SERVICE_IMAGES["$image"]=1
  done < <(docker compose --project-name "$COMPOSE_PROJECT" images -q "$service")

  image="$(
    docker image inspect "${COMPOSE_PROJECT}-${service}" \
      --format '{{.Id}}' 2>/dev/null || true
  )"
  [[ -n "$image" ]] && SERVICE_IMAGES["$image"]=1
done

echo "Removing application containers..."
docker compose --project-name "$COMPOSE_PROJECT" rm --force --stop "${APPLICATION_SERVICES[@]}"

if [[ "${#SERVICE_IMAGES[@]}" -gt 0 ]]; then
  echo "Removing application images..."
  for image in "${!SERVICE_IMAGES[@]}"; do
    docker image rm --force "$image"
  done
else
  echo "No application images were found."
fi

echo "Removing unused Docker images..."
docker image prune -f

echo "Removing all unused Docker images..."
docker image prune -a -f

echo "Removing unused Docker volumes..."
docker volume prune -f

echo "Removing unused Docker networks..."
docker network prune -f

if [[ -f "$STATE_FILE" ]]; then
  rm -f "$STATE_FILE"
fi

if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  if docker network rm "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "Removed Docker network $NETWORK_NAME."
  else
    echo "Docker network $NETWORK_NAME is still in use; leaving it in place."
  fi
else
  echo "Docker network $NETWORK_NAME was not found."
fi

echo "Application service containers and images have been removed."
