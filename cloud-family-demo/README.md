# cloud-family-demo

JDK 21 + Spring Boot 4.0.8 + Spring Cloud 2025.1.3 microservice demo.

## Modules

- `common-service`: shared configuration properties, constants, and MySQL multi-data-source auto-configuration.
- `gateway-service`: Spring Cloud Gateway WebFlux gateway on port `38080`.
- `auth-service`: JWT issuing service on port `38081`.
- `partner-service`: protected partner API service on port `38082`.
- `manager-service`: protected manager API service on port `38083`.
- `job-service`: XXL-JOB executor service on HTTP port `38084` and executor port `39999`.
- `frontend`: standalone React frontend on port `35173`, separated from backend services.

System users, roles, permissions, menus, and their relationships are manager-service features backed by the `fa-cloud` MySQL database. API user profile data is stored in `fa-cloud.vip_user`.

## Features

- Gateway-first API access with protected API and manager routes.
- API user and manager user login flows with JWT access and refresh tokens.
- Local JWT validation before protected requests are routed to backend services.
- Manager dashboard and system management APIs for users, roles, permissions, and menus.
- Lightweight XXL-JOB admin, executor registration, manual trigger APIs, and demo job handlers.
- Shared common module for cross-service configuration, constants, and infrastructure setup.

## Integrations

- Spring Cloud Gateway WebFlux for API routing.
- Spring Security OAuth2, Authorization Server, Resource Server, and JOSE/JWT support.
- MyBatis-Plus and MyBatis Spring for database access.
- MySQL with shared multi-data-source configuration.
- Redisson for Redis Cluster client integration.
- RocketMQ producer integration with configurable NameServer address.
- Optional Redis-backed auth sessions with `AUTH_SESSION_STORE=redis`.
- XXL-JOB admin-compatible APIs and executor integration for scheduled jobs.
- Spring Boot Actuator for health and info endpoints.
- Log4j2 for application logging.

## Build

```bash
mvn clean package
```

Build the standalone frontend separately:

```bash
cd frontend
npm install
npm run build
```

## Run

Install the shared module once before starting individual services from the Maven reactor:

```bash
mvn -pl common-service install
```

Start each service in a separate terminal:

```bash
mvn -pl auth-service spring-boot:run
mvn -pl partner-service spring-boot:run
mvn -pl manager-service spring-boot:run
mvn -pl job-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

Start the separated React frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:35173` and proxies `/auth/**` and `/api/**`
requests to the gateway at `http://localhost:38080` by default. Override the gateway
target with `VITE_GATEWAY_URL`.

In IntelliJ IDEA, use the shared `All Services` compound run configuration
to start all services together.

Auth sessions are stored in MySQL by default through Spring Authorization Server JDBC tables.
API login events are stored in `fa-model.partner_login_log`, and manager login events are stored in
`fa-model.manager_login_log`. Create both tables with `auth-service/src/main/resources/db/fa-model/auth-login-log-schema.sql`.
To store login sessions in Redis instead, start `auth-service` with:

```bash
AUTH_SESSION_STORE=redis mvn -pl auth-service spring-boot:run
```

`job-service` registers as the XXL-JOB executor app `cloud-family-job-service`
against `http://localhost:38088/xxl-job-admin` by default. Override it with:

```bash
XXL_JOB_ADMIN_ADDRESSES=http://localhost:38088/xxl-job-admin \
XXL_JOB_ACCESS_TOKEN=default_token \
mvn -pl job-service spring-boot:run
```

RocketMQ producer auto-configuration is available in every service through `common-service`.
It is disabled by default so services can start without a local broker. Enable it with:

```bash
ROCKETMQ_ENABLED=true \
ROCKETMQ_NAMESRV_ADDR=localhost:9876 \
mvn -pl manager-service spring-boot:run
```

Equivalent YAML:

```yaml
demo:
  rocketmq:
    enabled: true
    namesrv-addr: localhost:9876
```

`xxl-job-admin` is a lightweight local admin-compatible service for this demo. It accepts
executor registry callbacks at `/xxl-job-admin/api/**` and provides manual trigger APIs:

```bash
curl -s http://localhost:38088/xxl-job-admin/executors

curl -s -X POST http://localhost:38088/xxl-job-admin/jobs/trigger \
  -H 'Content-Type: application/json' \
  -H 'XXL-JOB-ACCESS-TOKEN: default_token' \
  -d '{"appName":"cloud-family-job-service","handler":"demoJobHandler","param":"hello"}'
```

The sample handler names for XXL-JOB admin are:

- `demoJobHandler`: normal demo job.
- `paramDemoJobHandler`: job parameter demo, reads comma-separated values from XXL-JOB Admin job params.
- `shardingDemoJobHandler`: sharding demo job, intended for the `SHARDING_BROADCAST` route strategy.
- `managerLoginStats10mJobHandler`: aggregates successful manager login log records into `fa-model.manager_login_10m_stats` by 10-minute windows.
- `partnerLoginStats5mJobHandler`: aggregates successful partner login log records into `fa-model.partner_login_5m_stats` by 5-minute windows.

For `managerLoginStats10mJobHandler`, use a 10-minute cron such as `0 0/10 * * * ?`.
Without a job param it calculates the previous complete 10-minute window. To recalculate
a specific window, pass the window end as an ISO timestamp, for example `2026-09-13T10:20:00Z`.
For `partnerLoginStats5mJobHandler`, use a 5-minute cron such as `0 0/5 * * * ?`.
Create the source login log tables with `auth-service/src/main/resources/db/fa-model/auth-login-log-schema.sql`
and the target stats tables with `job-service/src/main/resources/db/fa-model/manager-login-stats-schema.sql`
and `job-service/src/main/resources/db/fa-model/partner-login-stats-schema.sql`.

## Try

Login as an API user through the gateway:

```bash
curl -s -X POST http://localhost:38080/auth/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"demo"}'
```

Use the returned `accessToken`:

```bash
curl -s -X POST http://localhost:38080/api/users/me \
  -H 'Authorization: Bearer <accessToken>'
```

The gateway validates this JWT locally with Spring Security Resource Server before routing protected requests.

Refresh an access token with the returned `refreshToken`:

```bash
curl -s -X POST http://localhost:38080/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

Logout revokes the current access token and its refresh token:

```bash
curl -s -X POST http://localhost:38080/auth/logout \
  -H 'Authorization: Bearer <accessToken>'
```

Login as a manager user:

```bash
curl -s -X POST http://localhost:38080/auth/manager/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"demo"}'
```

Use the manager token to access manager APIs:

```bash
curl -s -X POST http://localhost:38080/api/manager/dashboard \
  -H 'Authorization: Bearer <accessToken>'
```

Kick a user offline by revoking all of their active sessions:

```bash
curl -s -X POST http://localhost:38080/auth/manager/kick-out \
  -H 'Authorization: Bearer <managerAccessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice"}'
```

System management endpoints are available through the manager service prefix:

```bash
curl -s -X POST http://localhost:38080/api/manager/system/users/page \
  -H 'Authorization: Bearer <accessToken>'
```

Relationship endpoints use `{"ids":[...]}` request bodies:

```bash
curl -s -X POST http://localhost:38080/api/manager/system/users/roles/replace/1 \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2]}'

curl -s -X POST http://localhost:38080/api/manager/system/roles/permissions/replace/1 \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2,3]}'

curl -s -X POST http://localhost:38080/api/manager/system/roles/menus/replace/1 \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2]}'
```

## API endpoints

Gateway base URL: `http://localhost:38080`.

Auth service endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/auth/api/login` | API user login |
| POST | `http://localhost:38080/auth/manager/login` | Manager user login |
| POST | `http://localhost:38080/auth/refresh` | Refresh access token |
| POST | `http://localhost:38080/auth/logout` | Logout and revoke token |
| POST | `http://localhost:38080/auth/manager/kick-out` | Kick a user offline |
| POST | `http://localhost:38080/auth/validate` | Validate access token |

Partner service endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/api/users/me` | Current API user profile |
| POST | `http://localhost:38080/api/users/my` | Current API user profile alias |
| POST | `http://localhost:38080/api/users/vip-users/page?pageNum=1&pageSize=10` | List VIP users |
| POST | `http://localhost:38080/api/users/vip-users/detail/{id}` | Get VIP user detail |
| POST | `http://localhost:38080/api/users/vip-users` | Create VIP user |
| POST | `http://localhost:38080/api/users/vip-users/update/{id}` | Update VIP user |
| POST | `http://localhost:38080/api/users/vip-users/delete/{id}` | Delete VIP user |
| POST | `http://localhost:38080/api/users/health` | Partner service health |

Manager service endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/api/manager/dashboard` | Manager dashboard |
| POST | `http://localhost:38080/api/manager/health` | Manager service health |
| POST | `http://localhost:38080/api/manager/vip-users/page?pageNum=1&pageSize=10` | List VIP users for manager |
| POST | `http://localhost:38080/api/manager/vip-users/detail/{id}` | Get VIP user detail for manager |
| POST | `http://localhost:38080/api/manager/vip-users` | Create VIP user for manager |
| POST | `http://localhost:38080/api/manager/vip-users/update/{id}` | Update VIP user for manager |
| POST | `http://localhost:38080/api/manager/vip-users/delete/{id}` | Delete VIP user for manager |

System user endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/api/manager/system/users/page?pageNum=1&pageSize=10` | List system users |
| POST | `http://localhost:38080/api/manager/system/users/detail/{id}` | Get system user detail |
| POST | `http://localhost:38080/api/manager/system/users/access/{id}` | Get system user roles, permissions, and menus |
| POST | `http://localhost:38080/api/manager/system/users` | Create system user |
| POST | `http://localhost:38080/api/manager/system/users/update/{id}` | Update system user |
| POST | `http://localhost:38080/api/manager/system/users/delete/{id}` | Delete system user |
| POST | `http://localhost:38080/api/manager/system/users/roles/{id}` | Get system user roles |
| POST | `http://localhost:38080/api/manager/system/users/roles/replace/{id}` | Replace system user roles |

System role endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/api/manager/system/roles/page?pageNum=1&pageSize=10` | List roles |
| POST | `http://localhost:38080/api/manager/system/roles/detail/{id}` | Get role detail |
| POST | `http://localhost:38080/api/manager/system/roles` | Create role |
| POST | `http://localhost:38080/api/manager/system/roles/update/{id}` | Update role |
| POST | `http://localhost:38080/api/manager/system/roles/delete/{id}` | Delete role |
| POST | `http://localhost:38080/api/manager/system/roles/permissions/{id}` | Get role permissions |
| POST | `http://localhost:38080/api/manager/system/roles/permissions/replace/{id}` | Replace role permissions |
| POST | `http://localhost:38080/api/manager/system/roles/menus/{id}` | Get role menus |
| POST | `http://localhost:38080/api/manager/system/roles/menus/replace/{id}` | Replace role menus |

System permission endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/api/manager/system/permissions/page?pageNum=1&pageSize=10` | List permissions |
| POST | `http://localhost:38080/api/manager/system/permissions/detail/{id}` | Get permission detail |
| POST | `http://localhost:38080/api/manager/system/permissions` | Create permission |
| POST | `http://localhost:38080/api/manager/system/permissions/update/{id}` | Update permission |
| POST | `http://localhost:38080/api/manager/system/permissions/delete/{id}` | Delete permission |

System menu endpoints:

| Method | Request URL | Description |
| --- | --- | --- |
| POST | `http://localhost:38080/api/manager/system/menus/page?pageNum=1&pageSize=10` | List menus |
| POST | `http://localhost:38080/api/manager/system/menus/detail/{id}` | Get menu detail |
| POST | `http://localhost:38080/api/manager/system/menus` | Create menu |
| POST | `http://localhost:38080/api/manager/system/menus/update/{id}` | Update menu |
| POST | `http://localhost:38080/api/manager/system/menus/delete/{id}` | Delete menu |
