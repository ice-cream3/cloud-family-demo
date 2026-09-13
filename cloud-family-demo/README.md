# cloud-family-demo

JDK 21 + Spring Boot 4.0.8 + Spring Cloud 2025.1.3 microservice demo.

## Modules

- `common-service`: shared configuration properties, constants, and MySQL multi-data-source auto-configuration.
- `gateway-service`: Spring Cloud Gateway WebFlux gateway on port `8080`.
- `auth-service`: JWT issuing service on port `8081`.
- `partner-service`: protected partner API service on port `8082`.
- `manager-service`: protected manager API service on port `8083`.
- `xxl-job-admin`: lightweight XXL-JOB admin-compatible service on port `8088`.
- `job-service`: XXL-JOB executor service on HTTP port `8084` and executor port `9999`.

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
- Optional Redis-backed auth sessions with `AUTH_SESSION_STORE=redis`.
- XXL-JOB admin-compatible APIs and executor integration for scheduled jobs.
- Spring Boot Actuator for health and info endpoints.
- Log4j2 for application logging.

## Build

```bash
mvn clean package
```

## Run

Start each service in a separate terminal:

```bash
mvn -pl auth-service spring-boot:run
mvn -pl partner-service spring-boot:run
mvn -pl manager-service spring-boot:run
mvn -pl xxl-job-admin spring-boot:run
mvn -pl job-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

In IntelliJ IDEA, use the shared `All Services` compound run configuration
to start all services together.

Auth sessions are stored in MySQL by default through Spring Authorization Server JDBC tables.
API login events are stored in `fa-cloud.partner_login_log`, and manager login events are stored in
`fa-cloud.manager_login_log`. Create both tables with `auth-service/src/main/resources/db/fa-cloud/auth-login-log-schema.sql`.
To store login sessions in Redis instead, start `auth-service` with:

```bash
AUTH_SESSION_STORE=redis mvn -pl auth-service spring-boot:run
```

`job-service` registers as the XXL-JOB executor app `cloud-family-job-service`
against `http://localhost:8088/xxl-job-admin` by default. Override it with:

```bash
XXL_JOB_ADMIN_ADDRESSES=http://localhost:8088/xxl-job-admin \
XXL_JOB_ACCESS_TOKEN=default_token \
mvn -pl job-service spring-boot:run
```

`xxl-job-admin` is a lightweight local admin-compatible service for this demo. It accepts
executor registry callbacks at `/xxl-job-admin/api/**` and provides manual trigger APIs:

```bash
curl -s http://localhost:8088/xxl-job-admin/executors

curl -s -X POST http://localhost:8088/xxl-job-admin/jobs/trigger \
  -H 'Content-Type: application/json' \
  -H 'XXL-JOB-ACCESS-TOKEN: default_token' \
  -d '{"appName":"cloud-family-job-service","handler":"demoJobHandler","param":"hello"}'
```

The sample handler names for XXL-JOB admin are:

- `demoJobHandler`: normal demo job.
- `paramDemoJobHandler`: job parameter demo, reads comma-separated values from XXL-JOB Admin job params.
- `shardingDemoJobHandler`: sharding demo job, intended for the `SHARDING_BROADCAST` route strategy.
- `managerLoginStats10mJobHandler`: aggregates manager login counts into `fa-model.manager_login_10m_stats` by 10-minute windows.

For `managerLoginStats10mJobHandler`, use a 10-minute cron such as `0 0/10 * * * ?`.
Without a job param it calculates the previous complete 10-minute window. To recalculate
a specific window, pass the window end as an ISO timestamp, for example `2026-09-13T10:20:00Z`.
Create the target table manually with `job-service/src/main/resources/db/fa-model/manager-login-stats-schema.sql`.

## Try

Login as an API user through the gateway:

```bash
curl -s -X POST http://localhost:8080/auth/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"demo"}'
```

Use the returned `accessToken`:

```bash
curl -s http://localhost:8080/api/users/me \
  -H 'Authorization: Bearer <accessToken>'
```

The gateway validates this JWT locally with Spring Security Resource Server before routing protected requests.

Refresh an access token with the returned `refreshToken`:

```bash
curl -s -X POST http://localhost:8080/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

Logout revokes the current access token and its refresh token:

```bash
curl -s -X POST http://localhost:8080/auth/logout \
  -H 'Authorization: Bearer <accessToken>'
```

Login as a manager user:

```bash
curl -s -X POST http://localhost:8080/auth/manager/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"demo"}'
```

Use the manager token to access manager APIs:

```bash
curl -s http://localhost:8080/api/manager/dashboard \
  -H 'Authorization: Bearer <accessToken>'
```

Kick a user offline by revoking all of their active sessions:

```bash
curl -s -X POST http://localhost:8080/auth/manager/kick-out \
  -H 'Authorization: Bearer <managerAccessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice"}'
```

System management endpoints are available through the manager service prefix:

```bash
curl -s http://localhost:8080/api/manager/system/users \
  -H 'Authorization: Bearer <accessToken>'
```

Relationship endpoints use `{"ids":[...]}` request bodies:

```bash
curl -s -X PUT http://localhost:8080/api/manager/system/users/1/roles \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2]}'

curl -s -X PUT http://localhost:8080/api/manager/system/roles/1/permissions \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2,3]}'

curl -s -X PUT http://localhost:8080/api/manager/system/roles/1/menus \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1,2]}'
```
