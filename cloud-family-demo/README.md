# cloud-family-demo

JDK 21 + Spring Boot 4.0.8 + Spring Cloud 2025.1.3 microservice demo.

## Modules

- `common-service`: shared configuration properties, constants, and MySQL multi-data-source auto-configuration.
- `gateway-service`: Spring Cloud Gateway WebFlux gateway on port `8080`.
- `auth-service`: JWT issuing service on port `8081`.
- `partner-service`: protected partner API service on port `8082`.
- `manager-service`: protected manager API service on port `8083`.

System users, roles, permissions, menus, and their relationships are manager-service features backed by the `fa-cloud` MySQL database. API user profile data is stored in `fa-cloud.vip_user`.

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
mvn -pl gateway-service spring-boot:run
```

In IntelliJ IDEA, use the shared `All Services` compound run configuration
to start all services together.

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
