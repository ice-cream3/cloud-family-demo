# cloud-family-demo

JDK 21 + Spring Boot 4.0.8 + Spring Cloud 2025.1.3 microservice demo.

## Modules

- `common-service`: shared configuration properties, constants, utility classes, and MySQL multi-data-source auto-configuration.
- `gateway-service`: Spring Cloud Gateway WebFlux gateway on port `8080`.
- `auth-service`: JWT issuing service on port `8081`.
- `user-api-service`: protected user API service on port `8082`.
- `manager-service`: protected manager API service on port `8083`.

## Build

```bash
mvn clean package
```

## Run

Start each service in a separate terminal:

```bash
mvn -pl auth-service spring-boot:run
mvn -pl user-api-service spring-boot:run
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
