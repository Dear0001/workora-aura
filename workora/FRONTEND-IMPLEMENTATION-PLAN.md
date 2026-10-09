# Workora Frontend Integration and Architecture Guide

This document explains the technical stack, the service-by-service responsibilities, and the full business flow of the Workora microservice project so a frontend team can start building the website with the correct API contracts and authentication flow.

## 1. Technology stack used and where it is used

### 1.1 Java 17 + Spring Boot 3.3.4

- Technology: Java 17 and Spring Boot 3.3.4
- Where it is used:
  - Each backend service is built as a Spring Boot application.
  - Example modules: `identity-service`, `organization-service`, `project-service`, `work-service`, `bug-service`, `recruitment-service`, `notification-service`, `payment-service`, and `api-gateway`.
  - Spring Boot handles REST API development, dependency injection, configuration, validation, and application bootstrapping.

### 1.2 Spring Cloud Gateway

- Technology: Spring Cloud Gateway
- Where it is used:
  - `api-gateway` is the single public entry point for frontend requests.
  - It routes requests to internal services.
  - It performs Keycloak JWT validation, CORS, rate limiting, request correlation ID propagation, and Swagger aggregation.
  - Example router rules:
    - `/api/identity/**` -> `identity-service`
    - `/api/organization/**` -> `organization-service`
    - `/api/project/**` -> `project-service`
    - `/api/work/**` -> `work-service`
    - `/api/bug/**` -> `bug-service`
    - `/api/recruitment/**` -> `recruitment-service`
    - `/api/notification/**` -> `notification-service`
    - `/api/payment/**` -> `payment-service`

### 1.3 Spring Security + OAuth2 Resource Server

- Technology: Spring Security with OAuth2 Resource Server
- Where it is used:
  - API Gateway validates JWT tokens against Keycloak.
  - Protected routes require a valid bearer token.
  - The gateway converts Keycloak `roles` claim into Spring `ROLE_*` authorities.
  - Each service keeps business-level permission checks in the domain service logic.

### 1.4 Keycloak

- Technology: Keycloak (identity provider / IAM)
- Where it is used:
  - User registration, login, email verification, password reset, and token issuing.
  - The runtime config points to `http://localhost:8180/realms/workora`.
  - The frontend should authenticate against Keycloak or use the backend identity endpoints that wrap Keycloak behaviors.
  - Keycloak is the source of truth for user credentials and token management.

### 1.5 PostgreSQL

- Technology: PostgreSQL 16
- Where it is used:
  - Each service owns its own PostgreSQL database/schema.
  - Example DBs:
    - `identity_db`
    - `organization_db`
    - `project_db`
    - `task_db`
    - `bug_db`
    - `recruitment_db`
    - `notification_db`
    - `payment_db`
  - These databases are isolated by domain, which matches microservice rules.

### 1.6 Redis

- Technology: Redis
- Where it is used:
  - In-memory request rate limiting at the API gateway.
  - Caching and temporary token or request management support.
  - Local dev infrastructure includes Redis in Docker Compose.

### 1.7 RabbitMQ

- Technology: RabbitMQ
- Where it is used:
  - Domain event communication between services.
  - Event-driven workflow for notifications, payment flows, and future service integrations.
  - Used because the project is organized as Spring MVC services and RabbitMQ is simpler to operate locally than a heavy streaming platform.

### 1.8 MinIO

- Technology: MinIO (S3-compatible object storage)
- Where it is used:
  - Storing uploaded files such as project assets, task submissions, bug evidence, and recruitment file metadata.
  - The system stores object keys in the DB and uses presigned upload/download flows.

### 1.9 Zipkin

- Technology: Zipkin
- Where it is used:
  - Distributed tracing and request tracing across microservices.
  - Helps visualize a request from the frontend through the gateway into the relevant backend service.

### 1.10 Docker + Docker Compose

- Technology: Docker Compose
- Where it is used:
  - Runs PostgreSQL databases, Redis, RabbitMQ, MinIO, Keycloak, Zipkin, and all backend services together locally.
  - Makes local environment setup consistent and reproducible.

### 1.11 Maven

- Technology: Maven multi-module build
- Where it is used:
  - Builds and packages the monorepo-like structure of the microservices project.
  - Each module has its own Spring Boot app and is built from the root `pom.xml`.

### 1.12 Swagger / OpenAPI

- Technology: SpringDoc OpenAPI
- Where it is used:
  - API documentation for each service and the combined gateway.
  - Frontend team can inspect schema and endpoint contracts from Swagger UI.
  - Example docs:
    - Gateway UI: `http://localhost:8080/gateway/swagger-ui.html`
    - Identity UI: `http://localhost:8080/identify/swagger-ui.html`

## 2. Service technical chart (TC)

### 2.1 Service map

| Service | Main role | Database | Port | Key responsibility |
| --- | --- | --- | --- | --- |
| `api-gateway` | Public API entry | Optional gateway DB | 8080 | Routing, auth validation, gateway security, rate limiting, CORS |
| `identity-service` | Identity + auth profile | `identity_db` | 8101 | Login, register, refresh, logout, user info |
| `organization-service` | Company and org domain | `organization_db` | 8102 | Companies, departments, invitations, membership |
| `project-service` | Project planning | `project_db` | 8103 | Projects, phases, bug-fix jobs |
| `work-service` | Work execution | `task_db` | 8104 | Tasks, assignees, submissions |
| `bug-service` | Bug management | `bug_db` | 8105 | Bug reports, status, evidence |
| `recruitment-service` | Candidate hiring | `recruitment_db` | 8106 | Recruitment posts, applications |
| `notification-service` | Messaging | `notification_db` | 8107 | Inbox notifications and preferences |
| `payment-service` | Payment and QR | `payment_db` | 8108 | Payment creation, verify, callback |
| `Keycloak` | IAM | `-` | 8180 | User identity and token issuance |
| `PostgreSQL` cluster | Data stores | per service DB | 5433-5440 | Domain data storage |
| `RabbitMQ` | Event bus | `-` | 5672 | Async inter-service messaging |
| `Redis` | Cache/rate limit | `-` | 6379 | Gateway limiter and fast data |
| `MinIO` | File storage | `-` | 9000 | Object/file storage |
| `Zipkin` | Tracing | `-` | 9411 | Distributed trace collection |

### 2.2 Gateway access conventions for frontend

The frontend should always call the API Gateway and never call each microservice directly. The actual gateway routing is defined in the `api-gateway` application config and uses the prefix structure below.

| Gateway prefix | Service target | Example frontend URL |
| --- | --- | --- |
| `/api/identity` | `identity-service` | `http://localhost:8080/api/identity/api/v1/auth/login` |
| `/api/organization` | `organization-service` | `http://localhost:8080/api/organization/api/v1/companies` |
| `/api/project` | `project-service` | `http://localhost:8080/api/project/api/v1/projects/{projectId}` |
| `/api/work` | `work-service` | `http://localhost:8080/api/work/api/v1/phases/{phaseId}/tasks` |
| `/api/bug` | `bug-service` | `http://localhost:8080/api/bug/api/v1/bug-reports` |
| `/api/recruitment` | `recruitment-service` | `http://localhost:8080/api/recruitment/api/v1/recruitment-posts` |
| `/api/notification` | `notification-service` | `http://localhost:8080/api/notification/api/v1/notifications` |
| `/api/payment` | `payment-service` | `http://localhost:8080/api/payment/api/v1/fee-payments/{paymentId}` |

The gateway strips the first two path segments (`/api/<service>`) before forwarding to the internal service, so the frontend can keep an easy, service-specific route pattern while the backend still uses its internal `/api/v1/...` controllers.

### 2.3 Identity service endpoint chart

Reference from `identity-service/API-ENDPOINTS.md` and the current Spring controllers.

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/identity/api/health` | `/api/health` | Public | Service health |
| POST | `/api/identity/api/v1/auth/register` | `/api/v1/auth/register` | Public | Create account and local profile |
| GET | `/api/identity/api/v1/auth/registration-verification-sessions/{token}` | `/api/v1/auth/registration-verification-sessions/{token}` | Public | Check email verification status |
| POST | `/api/identity/api/v1/auth/password-reset-requests` | `/api/v1/auth/password-reset-requests` | Public | Trigger password reset email |
| POST | `/api/identity/api/v1/auth/verification-email-requests` | `/api/v1/auth/verification-email-requests` | Public | Trigger verification email |
| POST | `/api/identity/api/v1/auth/password-resets` | `/api/v1/auth/password-resets` | Public | Reset password action |
| POST | `/api/identity/api/v1/auth/login` | `/api/v1/auth/login` | Public | Login and issue tokens |
| POST | `/api/identity/api/v1/auth/logout` | `/api/v1/auth/logout` | Public | Revoke refresh token |
| POST | `/api/identity/api/v1/auth/refresh` | `/api/v1/auth/refresh` | Public | Refresh access token |
| GET | `/api/identity/api/v1/users/me` | `/api/v1/users/me` | Protected | Get current user |
| PATCH | `/api/identity/api/v1/users/me` | `/api/v1/users/me` | Protected | Update current user profile |
| GET | `/api/identity/api/v1/users/{userId}` | `/api/v1/users/{userId}` | Protected | Get public user info |
| PATCH | `/api/identity/api/v1/users/{userId}` | `/api/v1/users/{userId}` | Admin | Admin update user |
| DELETE | `/api/identity/api/v1/users/{userId}` | `/api/v1/users/{userId}` | Admin | Disable/delete user |

Frontend note:
- The actual implementation currently exposes the auth routes under the gateway prefix `api/identity`.
- `refresh` and `logout` are token-based endpoints; secure them in the frontend with a centralized interceptor.

### 2.4 Organization service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/organization/api/health` | `/api/health` | Public | Health |
| POST | `/api/organization/api/v1/companies` | `/api/v1/companies` | Protected | Create company |
| GET | `/api/organization/api/v1/subscription/me` | `/api/v1/subscription/me` | Protected | Get current plan and company entitlement |
| GET | `/api/organization/api/v1/companies` | `/api/v1/companies` | Protected | List companies |
| GET | `/api/organization/api/v1/companies/{companyId}` | `/api/v1/companies/{companyId}` | Protected | Get company detail |
| PATCH | `/api/organization/api/v1/companies/{companyId}` | `/api/v1/companies/{companyId}` | Protected | Update company |
| DELETE | `/api/organization/api/v1/companies/{companyId}` | `/api/v1/companies/{companyId}` | Protected | Archive/delete company |
| GET | `/api/organization/api/v1/companies/{companyId}/profile` | `/api/v1/companies/{companyId}/profile` | Protected | Company profile |
| GET | `/api/organization/api/v1/companies/{companyId}/posts` | `/api/v1/companies/{companyId}/posts` | Protected | Company posts |
| GET | `/api/organization/api/v1/companies/{companyId}/feedback` | `/api/v1/companies/{companyId}/feedback` | Protected | Company feedback |
| GET | `/api/organization/api/v1/companies/{companyId}/ratings` | `/api/v1/companies/{companyId}/ratings` | Protected | Company ratings |
| POST | `/api/organization/api/v1/companies/{companyId}/departments` | `/api/v1/companies/{companyId}/departments` | Protected | Create department |
| GET | `/api/organization/api/v1/companies/{companyId}/departments` | `/api/v1/companies/{companyId}/departments` | Protected | List departments |
| GET | `/api/organization/api/v1/departments/{departmentId}` | `/api/v1/departments/{departmentId}` | Protected | Get department |
| PATCH | `/api/organization/api/v1/departments/{departmentId}` | `/api/v1/departments/{departmentId}` | Protected | Update department |
| DELETE | `/api/organization/api/v1/departments/{departmentId}` | `/api/v1/departments/{departmentId}` | Protected | Remove department |
| GET | `/api/organization/api/v1/departments/{departmentId}/members` | `/api/v1/departments/{departmentId}/members` | Protected | List department members |
| POST | `/api/organization/api/v1/departments/{departmentId}/members/{userId}` | `/api/v1/departments/{departmentId}/members/{userId}` | Protected | Add member |
| DELETE | `/api/organization/api/v1/departments/{departmentId}/members/{userId}` | `/api/v1/departments/{departmentId}/members/{userId}` | Protected | Remove member |
| POST | `/api/organization/api/v1/companies/{companyId}/members/invitations` | `/api/v1/companies/{companyId}/members/invitations` | Protected | Invite member |
| GET | `/api/organization/api/v1/companies/{companyId}/invitations` | `/api/v1/companies/{companyId}/invitations` | Protected | List invitations |
| POST | `/api/organization/api/v1/companies/{companyId}/invitations/{invitationId}/accept` | `/api/v1/companies/{companyId}/invitations/{invitationId}/accept` | Protected | Accept invitation |
| POST | `/api/organization/api/v1/companies/{companyId}/members/{userId}/roles` | `/api/v1/companies/{companyId}/members/{userId}/roles` | Protected | Assign role |
| PATCH | `/api/organization/api/v1/companies/{companyId}/members/{userId}/roles` | `/api/v1/companies/{companyId}/members/{userId}/roles` | Protected | Update role |
| DELETE | `/api/organization/api/v1/companies/{companyId}/members/{userId}` | `/api/v1/companies/{companyId}/members/{userId}` | Protected | Remove member |
| GET | `/api/organization/api/v1/companies/{companyId}/members` | `/api/v1/companies/{companyId}/members` | Protected | List company members |
| POST | `/api/organization/api/v1/companies/{companyId}/ratings` | `/api/v1/companies/{companyId}/ratings` | Protected | Create rating |
| GET | `/api/organization/api/v1/companies/{companyId}/ratings` | `/api/v1/companies/{companyId}/ratings` | Protected | List ratings |
| GET | `/api/organization/api/v1/users/{userId}/ratings` | `/api/v1/users/{userId}/ratings` | Protected | User ratings |
| PATCH | `/api/organization/api/v1/ratings/{ratingId}` | `/api/v1/ratings/{ratingId}` | Protected | Update rating |
| POST | `/api/organization/api/v1/ratings/{ratingId}/response` | `/api/v1/ratings/{ratingId}/response` | Protected | Reply to rating |

### 2.5 Project service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/project/api/health` | `/api/health` | Public | Health |
| POST | `/api/project/api/v1/companies/{companyId}/projects` | `/api/v1/companies/{companyId}/projects` | Protected | Create project |
| GET | `/api/project/api/v1/companies/{companyId}/projects` | `/api/v1/companies/{companyId}/projects` | Protected | List company projects |
| GET | `/api/project/api/v1/departments/{departmentId}/projects` | `/api/v1/departments/{departmentId}/projects` | Protected | List department projects |
| GET | `/api/project/api/v1/projects/{projectId}` | `/api/v1/projects/{projectId}` | Protected | Project detail |
| PATCH | `/api/project/api/v1/projects/{projectId}` | `/api/v1/projects/{projectId}` | Protected | Update project |
| DELETE | `/api/project/api/v1/projects/{projectId}` | `/api/v1/projects/{projectId}` | Protected | Delete/archive project |
| POST | `/api/project/api/v1/projects/{projectId}/archive` | `/api/v1/projects/{projectId}/archive` | Protected | Archive project |
| GET | `/api/project/api/v1/projects/{projectId}/members` | `/api/v1/projects/{projectId}/members` | Protected | List project members |
| POST | `/api/project/api/v1/projects/{projectId}/managers/{userId}` | `/api/v1/projects/{projectId}/managers/{userId}` | Protected | Assign manager |
| POST | `/api/project/api/v1/projects/{projectId}/phases` | `/api/v1/projects/{projectId}/phases` | Protected | Create phase |
| GET | `/api/project/api/v1/projects/{projectId}/phases` | `/api/v1/projects/{projectId}/phases` | Protected | List phases |
| GET | `/api/project/api/v1/phases/{phaseId}` | `/api/v1/phases/{phaseId}` | Protected | Get phase |
| PATCH | `/api/project/api/v1/phases/{phaseId}` | `/api/v1/phases/{phaseId}` | Protected | Update phase |
| DELETE | `/api/project/api/v1/phases/{phaseId}` | `/api/v1/phases/{phaseId}` | Protected | Delete phase |
| POST | `/api/project/api/v1/phases/{phaseId}/publish` | `/api/v1/phases/{phaseId}/publish` | Protected | Publish phase |
| POST | `/api/project/api/v1/phases/{phaseId}/hide` | `/api/v1/phases/{phaseId}/hide` | Protected | Hide phase |
| POST | `/api/project/api/v1/phases/{phaseId}/close` | `/api/v1/phases/{phaseId}/close` | Protected | Close phase |
| GET | `/api/project/api/v1/public/phases` | `/api/v1/public/phases` | Public | Public phase feed |
| GET | `/api/project/api/v1/public/phases/{phaseId}` | `/api/v1/public/phases/{phaseId}` | Public | Public phase detail |
| GET | `/api/project/api/v1/phases/{phaseId}/members` | `/api/v1/phases/{phaseId}/members` | Protected | Phase members |
| POST | `/api/project/api/v1/phases/{phaseId}/members/{userId}` | `/api/v1/phases/{phaseId}/members/{userId}` | Protected | Add member |
| DELETE | `/api/project/api/v1/phases/{phaseId}/members/{userId}` | `/api/v1/phases/{phaseId}/members/{userId}` | Protected | Remove member |
| POST | `/api/project/api/v1/phases/{phaseId}/bug-fix-jobs` | `/api/v1/phases/{phaseId}/bug-fix-jobs` | Protected | Create bug-fix job |
| GET | `/api/project/api/v1/phases/{phaseId}/bug-fix-jobs` | `/api/v1/phases/{phaseId}/bug-fix-jobs` | Protected | List bug-fix jobs |
| GET | `/api/project/api/v1/bug-fix-jobs/{jobId}` | `/api/v1/bug-fix-jobs/{jobId}` | Protected | Bug-fix job detail |
| PATCH | `/api/project/api/v1/bug-fix-jobs/{jobId}` | `/api/v1/bug-fix-jobs/{jobId}` | Protected | Update bug-fix job |
| POST | `/api/project/api/v1/bug-fix-jobs/{jobId}/publish` | `/api/v1/bug-fix-jobs/{jobId}/publish` | Protected | Publish job |
| POST | `/api/project/api/v1/bug-fix-jobs/{jobId}/close` | `/api/v1/bug-fix-jobs/{jobId}/close` | Protected | Close job |
| POST | `/api/project/api/v1/bug-fix-jobs/{jobId}/fund` | `/api/v1/bug-fix-jobs/{jobId}/fund` | Protected | Fund job |

### 2.6 Work service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/work/api/health` | `/api/health` | Public | Health |
| POST | `/api/work/api/v1/phases/{phaseId}/tasks` | `/api/v1/phases/{phaseId}/tasks` | Protected | Create task |
| GET | `/api/work/api/v1/phases/{phaseId}/tasks` | `/api/v1/phases/{phaseId}/tasks` | Protected | List tasks |
| GET | `/api/work/api/v1/tasks/{taskId}` | `/api/v1/tasks/{taskId}` | Protected | Task detail |
| GET | `/api/work/api/v1/tasks/{taskId}/history` | `/api/v1/tasks/{taskId}/history` | Protected | Task status history |
| GET | `/api/work/api/v1/tasks/{taskId}/summary` | `/api/v1/tasks/{taskId}/summary` | Protected | Task summary |
| PATCH | `/api/work/api/v1/tasks/{taskId}` | `/api/v1/tasks/{taskId}` | Protected | Update task |
| DELETE | `/api/work/api/v1/tasks/{taskId}` | `/api/v1/tasks/{taskId}` | Protected | Delete task |
| GET | `/api/work/api/v1/tasks/{taskId}/assignees` | `/api/v1/tasks/{taskId}/assignees` | Protected | List assignees |
| POST | `/api/work/api/v1/tasks/{taskId}/assignees` | `/api/v1/tasks/{taskId}/assignees` | Protected | Assign user |
| DELETE | `/api/work/api/v1/tasks/{taskId}/assignees/{userId}` | `/api/v1/tasks/{taskId}/assignees/{userId}` | Protected | Remove assignee |
| PATCH | `/api/work/api/v1/tasks/{taskId}/status` | `/api/v1/tasks/{taskId}/status` | Protected | Change status |
| POST | `/api/work/api/v1/tasks/{taskId}/submissions` | `/api/v1/tasks/{taskId}/submissions` | Protected | Submit work |
| GET | `/api/work/api/v1/tasks/{taskId}/submissions` | `/api/v1/tasks/{taskId}/submissions` | Protected | Submission history |
| GET | `/api/work/api/v1/tasks/{taskId}/submissions/{submissionId}` | `/api/v1/tasks/{taskId}/submissions/{submissionId}` | Protected | Submission detail |
| GET | `/api/work/api/v1/tasks/{taskId}/attachments` | `/api/v1/tasks/{taskId}/attachments` | Protected | List attachments |
| POST | `/api/work/api/v1/tasks/{taskId}/attachments` | `/api/v1/tasks/{taskId}/attachments` | Protected | Upload attachment |
| DELETE | `/api/work/api/v1/tasks/{taskId}/attachments/{attachmentId}` | `/api/v1/tasks/{taskId}/attachments/{attachmentId}` | Protected | Remove attachment |
| GET | `/api/work/api/v1/workspace` | `/api/v1/workspace` | Protected | Workspace summary |
| GET | `/api/work/api/v1/dashboard` | `/api/v1/dashboard` | Protected | Dashboard |
| GET | `/api/work/api/v1/history` | `/api/v1/history` | Protected | History feed |

### 2.7 Bug service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/bug/api/health` | `/api/health` | Public | Health |
| POST | `/api/bug/api/v1/phases/{phaseId}/bug-reports` | `/api/v1/phases/{phaseId}/bug-reports` | Protected | Create bug report from phase |
| POST | `/api/bug/api/v1/tasks/{taskId}/bug-reports` | `/api/v1/tasks/{taskId}/bug-reports` | Protected | Create bug report from task |
| GET | `/api/bug/api/v1/bug-reports/{reportId}` | `/api/v1/bug-reports/{reportId}` | Protected | Bug report detail |
| GET | `/api/bug/api/v1/bug-reports` | `/api/v1/bug-reports` | Protected | List bug reports |
| PATCH | `/api/bug/api/v1/bug-reports/{reportId}` | `/api/v1/bug-reports/{reportId}` | Protected | Update bug report |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/triage` | `/api/v1/bug-reports/{reportId}/triage` | Protected | Triage bug |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/accept` | `/api/v1/bug-reports/{reportId}/accept` | Protected | Accept bug |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/reject` | `/api/v1/bug-reports/{reportId}/reject` | Protected | Reject bug |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/resolve` | `/api/v1/bug-reports/{reportId}/resolve` | Protected | Resolve bug |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/duplicate` | `/api/v1/bug-reports/{reportId}/duplicate` | Protected | Mark duplicate |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/close` | `/api/v1/bug-reports/{reportId}/close` | Protected | Close bug |
| POST | `/api/bug/api/v1/bug-reports/{reportId}/evidence` | `/api/v1/bug-reports/{reportId}/evidence` | Protected | Add evidence |
| GET | `/api/bug/api/v1/bug-reports/{reportId}/evidence` | `/api/v1/bug-reports/{reportId}/evidence` | Protected | View evidence |
| DELETE | `/api/bug/api/v1/bug-reports/{reportId}/evidence/{evidenceId}` | `/api/v1/bug-reports/{reportId}/evidence/{evidenceId}` | Protected | Remove evidence |

### 2.8 Recruitment service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/recruitment/api/health` | `/api/health` | Public | Health |
| POST | `/api/recruitment/api/v1/recruitment-posts` | `/api/v1/recruitment-posts` | Protected | Create recruitment post |
| GET | `/api/recruitment/api/v1/recruitment-posts` | `/api/v1/recruitment-posts` | Protected | List competition posts |
| GET | `/api/recruitment/api/v1/recruitment-posts/{postId}` | `/api/v1/recruitment-posts/{postId}` | Protected | Post detail |
| PATCH | `/api/recruitment/api/v1/recruitment-posts/{postId}` | `/api/v1/recruitment-posts/{postId}` | Protected | Update post |
| DELETE | `/api/recruitment/api/v1/recruitment-posts/{postId}` | `/api/v1/recruitment-posts/{postId}` | Protected | Delete/close post |
| POST | `/api/recruitment/api/v1/recruitment-posts/{postId}/close` | `/api/v1/recruitment-posts/{postId}/close` | Protected | Close post |
| POST | `/api/recruitment/api/v1/recruitment-posts/{postId}/applications` | `/api/v1/recruitment-posts/{postId}/applications` | Protected | Apply |
| GET | `/api/recruitment/api/v1/recruitment-posts/{postId}/applications` | `/api/v1/recruitment-posts/{postId}/applications` | Protected | View applications |
| GET | `/api/recruitment/api/v1/applications/{applicationId}` | `/api/v1/applications/{applicationId}` | Protected | Application detail |
| PATCH | `/api/recruitment/api/v1/applications/{applicationId}` | `/api/v1/applications/{applicationId}` | Protected | Update application |
| POST | `/api/recruitment/api/v1/applications/{applicationId}/manager-review` | `/api/v1/applications/{applicationId}/manager-review` | Protected | Manager review |
| POST | `/api/recruitment/api/v1/applications/{applicationId}/owner-approval` | `/api/v1/applications/{applicationId}/owner-approval` | Protected | Owner approval |
| POST | `/api/recruitment/api/v1/applications/{applicationId}/withdraw` | `/api/v1/applications/{applicationId}/withdraw` | Protected | Withdraw application |

### 2.9 Notification service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/notification/api/health` | `/api/health` | Public | Health |
| GET | `/api/notification/api/v1/notifications` | `/api/v1/notifications` | Protected | List notifications |
| GET | `/api/notification/api/v1/notifications/{notificationId}` | `/api/v1/notifications/{notificationId}` | Protected | Notification detail |
| PATCH | `/api/notification/api/v1/notifications/{notificationId}/read` | `/api/v1/notifications/{notificationId}/read` | Protected | Mark as read |
| PATCH | `/api/notification/api/v1/notifications/{notificationId}/archive` | `/api/v1/notifications/{notificationId}/archive` | Protected | Archive notification |
| DELETE | `/api/notification/api/v1/notifications/{notificationId}` | `/api/v1/notifications/{notificationId}` | Protected | Delete notification |
| GET | `/api/notification/api/v1/notifications/preferences` | `/api/v1/notifications/preferences` | Protected | Get preferences |
| PATCH | `/api/notification/api/v1/notifications/preferences` | `/api/v1/notifications/preferences` | Protected | Update preferences |
| POST | `/api/notification/api/v1/notifications/test-email` | `/api/v1/notifications/test-email` | Protected | Send test email |

### 2.10 Payment service endpoint chart

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/payment/api/health` | `/api/health` | Public | Health |
| POST | `/api/payment/api/v1/bug-fix-jobs/{jobId}/fee-approval` | `/api/v1/bug-fix-jobs/{jobId}/fee-approval` | Protected | Approve fee |
| GET | `/api/payment/api/v1/fee-payments/{paymentId}` | `/api/v1/fee-payments/{paymentId}` | Protected | Payment detail |
| GET | `/api/payment/api/v1/fee-payments/{paymentId}/qr` | `/api/v1/fee-payments/{paymentId}/qr` | Protected | Get QR code |
| POST | `/api/payment/api/v1/fee-payments/{paymentId}/verify` | `/api/v1/fee-payments/{paymentId}/verify` | Protected | Verify payment |
| POST | `/api/payment/api/v1/fee-payments/{paymentId}/confirm-receipt` | `/api/v1/fee-payments/{paymentId}/confirm-receipt` | Protected | Confirm receipt |
| POST | `/api/payment/api/v1/fee-payments/{paymentId}/dispute` | `/api/v1/fee-payments/{paymentId}/dispute` | Protected | Raise dispute |
| GET | `/api/payment/api/v1/disputes/{disputeId}` | `/api/v1/disputes/{disputeId}` | Protected | Dispute detail |
| GET | `/api/payment/api/v1/users/me/payment-profiles/bakong` | `/api/v1/users/me/payment-profiles/bakong` | Protected | Get Bakong profile |
| POST | `/api/payment/api/v1/users/me/payment-profiles/bakong` | `/api/v1/users/me/payment-profiles/bakong` | Protected | Create Bakong profile |
| PATCH | `/api/payment/api/v1/users/me/payment-profiles/bakong` | `/api/v1/users/me/payment-profiles/bakong` | Protected | Update Bakong profile |
| DELETE | `/api/payment/api/v1/users/me/payment-profiles/bakong` | `/api/v1/users/me/payment-profiles/bakong` | Protected | Delete Bakong profile |
| POST | `/api/payment/api/v1/users/me/payment-profiles/bakong/qr-upload` | `/api/v1/users/me/payment-profiles/bakong/qr-upload` | Protected | Upload QR |

### 2.11 Feed and query services

| Method | Gateway path | Internal service path | Auth | Description |
| --- | --- | --- | --- | --- |
| GET | `/api/feed` or `/api/project/api/v1/public/phases` | read-model endpoints | Protected/Public | Feed and query APIs |
| GET | `/api/notification/api/v1/notifications` | `/api/v1/notifications` | Protected | Notifications feed |
| GET | `/api/work/api/v1/workspace` | `/api/v1/workspace` | Protected | Workspace overview |
| GET | `/api/work/api/v1/dashboard` | `/api/v1/dashboard` | Protected | Dashboard metrics |
| GET | `/api/project/api/v1/public/phases` | `/api/v1/public/phases` | Public | Public phase listing |

## 3. Whole project flow (frontend-ready system flow)

### 3.1 High-level user flow

1. Browser loads frontend website.
2. Frontend checks whether user already has a valid JWT token in local storage or session storage.
3. If not logged in, the frontend redirects to the login page.
4. Frontend calls Keycloak or an Identity endpoint to authenticate.
5. Identity service validates credentials with Keycloak and returns tokens plus user profile info.
6. Frontend stores the JWT and uses it for all protected API calls.
7. Frontend sends requests to the API Gateway, not directly to each microservice.
8. API Gateway validates the JWT and routes the request to the correct backend service.
9. The target service reads the required data from its own database.
10. If a workflow crosses services, an event may be published to RabbitMQ.
11. Consumers like notification-service may react to the event and create notifications.
12. The response returns to the frontend and the UI updates the screen.

### 3.2 Example end-to-end flow: login

```text
Frontend
  -> API Gateway: POST /api/identity/api/v1/auth/login
  -> Identity Service
  -> Keycloak: validate user credentials / issue token
  -> Identity Service: returns access token + refresh token + user profile
  -> Frontend: stores JWT and user data
```

### 3.3 Example end-to-end flow: create company and project

```text
Frontend
  -> API Gateway: POST /api/organization/api/v1/companies
  -> Organization Service
  -> organization_db: insert company data
  -> Frontend receives company object

Frontend
  -> API Gateway: POST /api/project/api/v1/projects
  -> Project Service
  -> project_db: insert project data
  -> Frontend receives project object
```

### 3.4 Example end-to-end flow: assign task and notify user

```text
Frontend
  -> API Gateway: POST /api/work/api/v1/phases/{phaseId}/tasks
  -> Work Service
  -> task_db: save task

Work Service -> RabbitMQ: task-created / task-assigned event
Notification Service -> RabbitMQ consumer
Notification Service -> notification_db: create notification item
Frontend -> API Gateway: GET /api/notification/api/v1/notifications
```

### 3.5 Example end-to-end flow: payment

```text
Frontend
  -> API Gateway: POST /api/payment/api/v1/payments
  -> Payment Service
  -> payment_db: save payment request and QR data
  -> Payment Service: connect to Bakong provider or simulator
  -> Frontend: returns QR payload and payment status

Bank / provider -> Payment Service callback: POST /api/v1/payments/callbacks/bakong
Payment Service -> payment_db: update transaction status
```

## 4. Frontend implementation guidance

### 4.1 Recommended frontend architecture

For the website implementation, the frontend should be organized like this:

```text
src/
  app/
    auth/
      login/
      register/
      forgot-password/
      reset-password/
    dashboard/
    company/
      list/
      detail/
      members/
      invitations/
    project/
      list/
      detail/
      phase/
      board/
    work/
      tasks/
      task-detail/
      submissions/
    bug/
      list/
      detail/
      triage/
    recruitment/
      posts/
      applications/
    notifications/
    payment/
      list/
      detail/
      qr/
  features/
    auth/
    company/
    project/
    work/
    bug/
    recruitment/
    notification/
    payment/
  lib/
    api/
      client.ts
      auth.ts
      endpoints.ts
      types.ts
    auth/
      token-storage.ts
      refresh-token.ts
    hooks/
    utils/
  store/
    auth-store.ts
    company-store.ts
    project-store.ts
    notification-store.ts
```

This structure keeps feature modules aligned to the backend domains and allows a single API layer to wrap all gateway calls.

### 4.2 Authentication flow for frontend

- Prefer a centralized auth module.
- Store access token and refresh token securely.
- Add an HTTP interceptor to attach the bearer token to every request.
- On `401 Unauthorized`, trigger refresh-token logic.
- Use the API Gateway as the single base URL for all API calls.

Suggested base config:

```text
API_BASE_URL = http://localhost:8080
```

And then route calls through service-specific gateways:

```text
POST {API_BASE_URL}/api/identity/api/v1/auth/login
POST {API_BASE_URL}/api/identity/api/v1/auth/refresh
GET  {API_BASE_URL}/api/organization/api/v1/companies
GET  {API_BASE_URL}/api/project/api/v1/projects/{projectId}
POST {API_BASE_URL}/api/work/api/v1/phases/{phaseId}/tasks
GET  {API_BASE_URL}/api/notification/api/v1/notifications
POST {API_BASE_URL}/api/payment/api/v1/fee-payments/{paymentId}/verify
```

Example API client pattern:

```ts
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // refresh token flow
      await refreshToken();
      return api.request(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 4.3 Suggested frontend state model

- `auth`: current user, token, refresh state, loading status
- `company`: active company, members, invitations, roles
- `project`: project list and selected project
- `phase`: selected phase and phase member list
- `task`: tasks, assignee status, submission status
- `notification`: unread count and inbox items
- `payment`: payment status and QR payload

### 4.4 Design guidance for UI screens

- Landing page: general landing + CTA to login/register
- Login/Register screen: use Identity service
- Dashboard: summary cards for company, project, task, notification, payment
- Company management screen: create company, invite members
- Project board: list projects and phases, show tasks per phase
- Bug board: list bugs, assign status, evidence viewer
- Recruitment board: recruitment posts and candidate review
- Payment screen: show QR, transaction status, payment history

### 4.5 Implementation priority order for frontend

1. Authentication + user profile
2. Company membership and dashboard
3. Project and phase list
4. Task management and assignment
5. Notification inbox
6. Bug report workflow
7. Recruitment workflow
8. Payment workflow

This order matches the most important core business flows for a working website.

## 5. Key decision summary for frontend integration

- Frontend must call the API Gateway as the single entry point.
- Use Keycloak-issued JWT as the authentication mechanism.
- Keep each service isolated with its own database.
- Use REST API calls for almost all pages and workflows.
- Use RabbitMQ for event-based cross-service communication, especially notifications.
- Storage of files is handled by MinIO; the DB stores keys, not raw binary content.

## 7. Endpoint reconciliation and implementation blockers

Verified against `E:\Bakong\workora\Workora-Microservices` on 2026-09-25:

- 128 non-health backend controller routes were found.
- 122 frontend HTTP call sites were found in `src/lib/api`.
- The frontend client was corrected for notification preferences, recruitment
  listing and application status flows, task cancellation, and bug status
  transitions.
- New client coverage was added for bug-fix job management/applications and
  notification unread counts/creation.

The following UI work now has backend contracts: task attachments, task
history/summary, bug evidence deletion, notification test email, Bakong
profiles, fee-payment aliases, department-member mutations, and project
member/manager operations. Task-originated bug creation still needs to map to
the existing phase-scoped bug-report creation route.

Feature acceptance should therefore use this order:

1. Enable the corrected identity, organization, subscription, project phase,
   work task, bug status, recruitment, notification, and payment routes.
2. Wire the frontend task attachment/history/summary and department/project
   membership controls to their implemented endpoints.
3. Display the backend `COMPANY_QUOTA_EXCEEDED` response and keep users in the
   subscription flow when the free-plan company limit is reached.

   ## 8. Company creation and multi-company membership

   The organization domain is multi-tenant by design: one user can belong to
   multiple companies, and each company has its own departments, members, and
   invitations. The frontend should therefore treat company management as a
   company-switching workspace rather than a single-company screen.

   Required frontend behavior:

   1. `GET /api/organization/api/v1/companies?page=0&size=100` loads the full
      list of companies the current user can access.
   2. The company management page should render a company selector or switcher
      when multiple companies exist, so each company keeps its own departments,
      member list, and invitation history.
   3. The base/free plan allows only one company. If the user already has one
      company and tries to create another, **Create Company** opens a
      subscription prompt instead of the form.
   4. When the user is below the limit, **Create Company** opens the normal
      creation form and after success the new company is added to the company
      list and selected immediately.
   5. Company-specific actions such as department creation, member invitations,
      and role management operate against the selected company only.

   Backend entitlement checks enforce the allowance: the server exposes
   `GET /api/organization/api/v1/subscription/me` and rejects additional
   company creation beyond the subscribed allowance with `403 Forbidden` and
   code `COMPANY_QUOTA_EXCEEDED`. The frontend should display that backend
   message and keep the user in the subscription flow when the limit is
   reached.

## 6. Final recommendation

The website frontend should be built around a gateway-first API integration pattern, with strong auth handling at the app shell and feature-specific API modules for each domain. The backend is already structured in a clear and realistic microservice pattern, so the frontend can begin with the Identity, Organization, Project, and Work flows first before expanding into Bug, Recruitment, Notification, and Payment.
