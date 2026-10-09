# Docker Compose E2E smoke tests

Run from `Workora-Microservices`:

```bash
bash e2e/compose-smoke.sh
```

The harness starts the complete Compose stack and verifies:

- Keycloak realm availability
- MinIO health and the `workora-files` bucket
- RabbitMQ diagnostics
- Redis connectivity
- PostgreSQL readiness for every service database
- Actuator health for the gateway and all eight application services
- Identity registration and Keycloak password-token acquisition
- Authenticated profile access through the gateway
- Authenticated MinIO presigned-upload URL creation

Set `KEEP_STACK=true` to keep the containers running after the checks. These
checks create a disposable test identity and never use production credentials.
Extend the workflow section with isolated fixtures when organization, project,
task, bug, recruitment, notification, and payment business tests are added.
