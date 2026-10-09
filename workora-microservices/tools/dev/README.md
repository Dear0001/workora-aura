# Development database seeds

These fixtures add one small, synthetic example set to each Workora PostgreSQL
database. They are for local development only; they do not create Keycloak
accounts, upload file contents, or run external service workflows.

From the microservices repository root:

```sh
sh tools/dev/seed-dev.sh --list
sh tools/dev/seed-dev.sh --confirm-dev all
sh tools/dev/seed-dev.sh --confirm-dev organization
```

By default, the domain fixtures use the synthetic subject
`00000000-0000-4000-8000-000000000001`. To associate domain fixture ownership
with an existing Keycloak account, set `DEV_SEED_USER_ID` to that account's
UUID subject before running the seeds:

```sh
DEV_SEED_USER_ID=your-keycloak-user-uuid sh tools/dev/seed-dev.sh --confirm-dev all
```

This only links development rows in domain databases to that user ID; it does
not create or modify the Keycloak account. The identity fixture always remains
a separate, unverified synthetic user. Do not set this to a production account
ID or use these fixtures against non-development databases.

The global identity account role is separate from company membership roles.
`DEV_SEED_USER_ID` is the Keycloak JWT subject used by domain fixtures; it is
not the identity database profile ID. To explicitly change an existing local
identity profile to the global `ADMIN` role, get its `id` from the signed-in
account's `GET /api/identity/api/v1/users/me` response and run:

```sh
DEV_SEED_IDENTITY_USER_ID=your-workora-profile-uuid \
  sh tools/dev/seed-dev.sh --confirm-dev --set-account-role ADMIN
```

This command only updates exactly one existing row in the local `identity-db`;
it fails if the profile ID does not match exactly one row. It does not alter
the Keycloak realm role, and it does not change organization membership roles.
For admin dashboard access, also assign the `ADMIN` realm role to that account
in the Keycloak Admin Console (Users → account → Role mapping → Assign role →
Realm roles), then sign out and back in so the access token contains the
updated role. Never run this against a production database or use it to grant
production access.

The confirmation flag is required. The runner targets only the PostgreSQL
services in the repository's `docker-compose.yml` and passes SQL on standard
input. The databases must already be running. Each SQL file is transactional
and uses stable IDs so re-running it updates/reuses the same fixture rows.
No database is modified by listing the available targets.

The runner uses the same Compose project name as `start-all-db.sh`
(`workora-microservices-2` by default). If `COMPOSE_PROJECT_NAME` is set when
starting the databases, set it to the same value when running the seed script.
To inspect the matching project's database state, use:

```sh
docker compose --project-name workora-microservices-2 ps
```

To run the dashboard locally without pulling MinIO images, start it with the
dashboard-only Compose override from the repository root:

```sh
docker compose --project-name workora-microservices-2 \
  -f docker-compose.yml -f docker-compose.dashboard.yml \
  up -d --build api-gateway dashboard-service
```

The override keeps database and Keycloak startup dependencies but removes the
MinIO bucket-init dependency from the services needed by the gateway. File
upload/download features will not work in this mode. It requires Docker Compose
2.24 or newer. The default Compose configuration is unchanged.

The company/project fixtures use stable `...0002` / `...0003` IDs. These IDs
are logical references between independently owned databases, not
cross-database foreign keys.

Identity rows are deliberately unverified and carry a non-credential marker;
they are not login accounts and are not synchronized to Keycloak. Payment
fixtures remain in `CREATED` state and do not represent a real or settled
transaction. Storage keys are examples only; these files are not uploaded.
Project and bug activity timestamps are relative to seed time. The sample task
is due today, and a completed task provides recent activity for the dashboard
chart. The organization and project fixtures also include a separate active
synthetic developer membership so the dashboard's team-member metric is not
zero for an owner account.

For backend schema changes, review and update the related SQL fixture before
using this runner. Raw SQL bypasses application validation, auditing, lifecycle
callbacks, event publishing, and provider integrations.
