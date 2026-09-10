# cloud-family-demo 工程说明

`cloud-family-demo` 是一个基于 JDK 21、Spring Boot 4.0.8、Spring Cloud 2025.1.3 的微服务示例工程。工程使用 Maven 多模块组织，通过 `gateway-service` 统一入口转发请求，通过 `auth-service` 签发 JWT，通过 Spring Security Resource Server 在网关侧本地验签，并把认证后的用户信息透传给下游业务服务。

## 模块结构

| 模块 | 端口 | 类型 | 职责 |
| --- | --- | --- | --- |
| `common-service` | 无 | Jar 公共模块 | 放置全局公用配置、常量、工具类。 |
| `gateway-service` | `8080` | Spring Cloud Gateway WebFlux | 统一 API 入口、路由转发、JWT 本地验签、用户身份 header 注入、管理接口角色校验。 |
| `auth-service` | `8081` | Spring MVC REST 服务 | 用户登录、管理员登录、JWT 签发、OAuth2 授权数据持久化、兼容性 token 校验接口。 |
| `user-api-service` | `8082` | Spring MVC REST 服务 | 用户侧受保护接口示例。 |
| `manager-service` | `8083` | Spring MVC REST 服务 | 管理侧受保护接口示例。 |

## common-service 功能

`common-service` 当前提供四类公共能力。

### JWT 配置模型

类：`com.kfpd.cloud.common.security.CommonJwtProperties`

绑定配置前缀：

```yaml
demo:
  auth:
    jwt:
      issuer: cloud-family-demo
      secret: cloud-family-demo-jwt-signing-secret-please-change-in-production
      ttl-seconds: 7200
```

字段说明：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `issuer` | `String` | `cloud-family-demo` | JWT 签发方。`auth-service` 写入 JWT，`gateway-service` 验签时校验。 |
| `secret` | `String` | 无 | HS256 对称签名密钥。auth 和 gateway 必须一致。生产环境应改为强随机密钥，并从环境变量或密钥管理系统加载。 |
| `ttlSeconds` | `long` | `7200` | JWT 有效期，单位秒。 |
| `refreshTtlSeconds` | `long` | `604800` | Refresh token 有效期，单位秒，默认 7 天。 |

### JWT Secret 工具

类：`com.kfpd.cloud.common.security.JwtSecretKeys`

方法：

```java
SecretKey hmacSha256Key(String secret)
```

用途：

- 把字符串密钥转换为 `HmacSHA256` 的 `SecretKey`。
- `auth-service` 的 `JwtDecoder` 和 `gateway-service` 的 `ReactiveJwtDecoder` 复用同一套转换逻辑。

### 网关透传 Header 常量

类：`com.kfpd.cloud.common.web.GatewayHeaders`

常量：

| 常量 | Header 名 | 说明 |
| --- | --- | --- |
| `USER_NAME` | `X-User-Name` | 当前认证用户名。 |
| `USER_TYPE` | `X-User-Type` | 用户类型，例如 `API`、`MANAGER`。 |
| `USER_ROLES` | `X-User-Roles` | 逗号分隔的角色列表。 |
| `USER_PERMISSIONS` | `X-User-Permissions` | 逗号分隔的权限列表。 |

这些 header 由 `gateway-service` 在 JWT 验签成功后注入，下游服务直接读取。

### MySQL 多数据源自动配置

类：

- `com.kfpd.cloud.common.datasource.MultiDataSourceProperties`
- `com.kfpd.cloud.common.datasource.MultiDataSourceAutoConfiguration`
- `com.kfpd.cloud.common.datasource.MultiDataSourceNames`

自动配置入口：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

绑定配置前缀：

```yaml
demo:
  datasource:
    crypto:
      secret: cloud-family-demo-datasource-crypto-secret
    cloud:
      url: jdbc:mysql://localhost:3306/fa-cloud?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
      username: ENC(U0rj/0TFqN/ikdyYo1QcsZa9APbjTkdLYLth5ycDEk4=)
      password: ENC(O/mIkbzTm1uHasWUrJSMKaIDlfcpqXW6E8FFh3e0+Y0=)
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      leak-detection-threshold: 0
      pool-name: fa-cloud
    model:
      url: jdbc:mysql://localhost:3306/fa-model?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
      username: ENC(U0rj/0TFqN/ikdyYo1QcsZa9APbjTkdLYLth5ycDEk4=)
      password: ENC(O/mIkbzTm1uHasWUrJSMKaIDlfcpqXW6E8FFh3e0+Y0=)
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      leak-detection-threshold: 0
      pool-name: fa-model
```

当前会自动创建以下 Bean：

| Bean 名称 | 类型 | 是否 Primary | 连接库 | 说明 |
| --- | --- | --- | --- | --- |
| `faCloudDataSource` | `DataSource` | 是 | `fa-cloud` | 默认主数据源。 |
| `faModelDataSource` | `DataSource` | 否 | `fa-model` | 模型库数据源。 |
| `faCloudJdbcTemplate` | `JdbcTemplate` | 是 | `fa-cloud` | 默认主库 JDBC 操作模板。 |
| `faModelJdbcTemplate` | `JdbcTemplate` | 否 | `fa-model` | 模型库 JDBC 操作模板。 |
| `faCloudTransactionManager` | `PlatformTransactionManager` | 是 | `fa-cloud` | 主库事务管理器。 |
| `faModelTransactionManager` | `PlatformTransactionManager` | 否 | `fa-model` | 模型库事务管理器。 |

用户名和密码支持两种形式：

- 明文：`root`
- 密文：`ENC(base64Payload)`

`common-service` 会在创建 `DataSource` 前自动解密 `ENC(...)` 值。环境变量覆盖值也支持这两种形式。

当前密文使用 JDK AES-GCM 生成，默认解密密钥来自：

```yaml
demo:
  datasource:
    crypto:
      secret: ${DATASOURCE_CRYPTO_SECRET:cloud-family-demo-datasource-crypto-secret}
```

生产环境必须通过 `DATASOURCE_CRYPTO_SECRET` 覆盖默认密钥，并重新生成数据库用户名和密码密文。

### Spring Security OAuth2 授权表

`fa-cloud` 库中使用 Spring Authorization Server JDBC 标准表结构保存 OAuth2 客户端、授权和授权同意数据。初始化脚本位于：

```text
common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

脚本会创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS `fa-cloud`;
```

并创建三张表：

| 表名 | Spring JDBC 服务 | 说明 |
| --- | --- | --- |
| `oauth2_registered_client` | `JdbcRegisteredClientRepository` | 保存 OAuth2 client 注册信息。 |
| `oauth2_authorization` | `JdbcOAuth2AuthorizationService` | 保存 authorization code、access token、refresh token、OIDC id token、device code 等授权数据。 |
| `oauth2_authorization_consent` | `JdbcOAuth2AuthorizationConsentService` | 保存用户对 client 的授权同意记录。 |

`auth-service` 当前已经接入：

| Bean | 数据源 | 说明 |
| --- | --- | --- |
| `JdbcRegisteredClientRepository` | `faCloudJdbcTemplate` | 持久化和读取 `oauth2_registered_client`。 |
| `JdbcOAuth2AuthorizationService` | `faCloudJdbcTemplate` | 持久化和读取 `oauth2_authorization`。 |

服务启动时会自动初始化两个 demo client：

| client_id | registered_client_id | grant type | scopes |
| --- | --- | --- | --- |
| `cloud-api-client` | `cloud-api-client-registration` | `demo_api_login` | `user:profile:read` |
| `cloud-manager-client` | `cloud-manager-client-registration` | `demo_manager_login` | `manager:login`, `manager:dashboard:read`, `user:profile:read` |

登录成功后，`auth-service` 会：

1. 从 `JdbcRegisteredClientRepository` 读取对应 `RegisteredClient`。
2. 签发 JWT access token。
3. 构造 `OAuth2Authorization`，写入 principal、grant type、authorized scopes、access token、token claims 等信息。
4. 通过 `JdbcOAuth2AuthorizationService.save(...)` 保存到 `fa-cloud.oauth2_authorization`。

兼容性接口 `/auth/validate` 会先校验 JWT 签名和过期时间，再通过 `JdbcOAuth2AuthorizationService.findByToken(...)` 确认 access token 存在且仍处于 active 状态。

执行脚本：

```bash
mysql -uroot -proot < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

该表结构以 Spring Authorization Server 官方 JDBC schema 为基准，并针对 MySQL 增加了 `IF NOT EXISTS`、字符集、存储引擎和常用索引。

### 系统管理表

用户、角色、权限、菜单功能属于 `manager-service`。它们使用 `fa-cloud` 库中的系统管理表，初始化脚本位于：

```text
manager-service/src/main/resources/db/fa-cloud/system-management-schema.sql
```

脚本会创建以下主表：

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 用户表，保存用户名、密码哈希、显示名、邮箱、状态。 |
| `sys_role` | 角色表，保存角色编码、名称、描述、状态。 |
| `sys_permission` | 权限表，保存权限编码、名称、描述、状态。 |
| `sys_menu` | 菜单表，保存菜单编码、名称、路径、组件、图标、排序、可见性、状态。 |

脚本也预留以下关联表：

| 表名 | 说明 |
| --- | --- |
| `sys_user_role` | 用户和角色关联。 |
| `sys_role_permission` | 角色和权限关联。 |
| `sys_role_menu` | 角色和菜单关联。 |

执行脚本：

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < manager-service/src/main/resources/db/fa-cloud/system-management-schema.sql
```

`manager-service` 提供 CRUD 和关联关系维护能力：

| 类 | 说明 |
| --- | --- |
| `com.kfpd.cloud.manager.dao.*Dao` | MyBatis DAO 接口，对接 `mapper/system/*.xml`。 |
| `com.kfpd.cloud.manager.service.SystemManagementService` | 用户、角色、权限、菜单 CRUD 及关系维护业务服务。 |
| `com.kfpd.cloud.manager.pojo.entity.SysUser` / `SysRole` / `SysPermission` / `SysMenu` | 数据库表实体。 |
| `com.kfpd.cloud.manager.pojo.dto.*Request` | 管理接口请求模型。 |

写操作使用 `faCloudTransactionManager`。

业务服务使用示例：

```java
import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DemoQueryService {

    private final JdbcTemplate faCloudJdbcTemplate;
    private final JdbcTemplate faModelJdbcTemplate;

    public DemoQueryService(
            @Qualifier(MultiDataSourceNames.FA_CLOUD_JDBC_TEMPLATE) JdbcTemplate faCloudJdbcTemplate,
            @Qualifier(MultiDataSourceNames.FA_MODEL_JDBC_TEMPLATE) JdbcTemplate faModelJdbcTemplate) {
        this.faCloudJdbcTemplate = faCloudJdbcTemplate;
        this.faModelJdbcTemplate = faModelJdbcTemplate;
    }
}
```

事务使用示例：

```java
import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
public void updateModelData() {
    // Use faModelJdbcTemplate in this method.
}
```

## 认证流程

1. 客户端访问 `gateway-service` 的登录接口：`POST /auth/api/login` 或 `POST /auth/manager/login`。
2. `gateway-service` 将 `/auth/**` 路由到 `auth-service`。
3. `auth-service` 校验账号、密码以及登录限制。
4. 登录成功后，`auth-service` 使用 HS256 签发 JWT，通过 `JdbcOAuth2AuthorizationService` 写入 `fa-cloud.oauth2_authorization`，并返回 `accessToken`。
5. 客户端访问受保护接口时携带：

```http
Authorization: Bearer <accessToken>
```

6. `gateway-service` 使用 Spring Security Resource Server 本地验签 JWT，同时校验 `issuer`、`exp` 等标准声明。
7. 验签成功后，`gateway-service` 从 JWT claims 中读取用户信息，并注入 `X-User-*` header。
8. 请求被转发到 `user-api-service` 或 `manager-service`。
9. 访问 `/api/manager/**` 时，`gateway-service` 额外要求 JWT 的 `roles` 包含 `MANAGER`，否则返回 `403`。

## JWT 内容

`auth-service` 签发的 JWT 当前包含以下 claims：

| Claim | 类型 | 示例 | 说明 |
| --- | --- | --- | --- |
| `iss` | `String` | `cloud-family-demo` | 签发方。 |
| `sub` | `String` | `alice` | JWT subject，当前为用户名。 |
| `iat` | `Instant` | 自动生成 | 签发时间。 |
| `exp` | `Instant` | 自动生成 | 过期时间。 |
| `username` | `String` | `alice` | 用户名。 |
| `user_type` | `String` | `API` | 用户类型。 |
| `roles` | `List<String>` | `["USER"]` | 角色列表。 |
| `permissions` | `List<String>` | `["user:profile:read"]` | 权限列表。 |

当前使用 HS256 对称签名。生产环境如果存在多个调用方或需要密钥轮换，建议切换到 RSA/ECDSA 非对称签名，并通过 JWK Set 暴露公钥。

## 路由规则

`gateway-service` 路由配置：

| 网关路径 | 转发目标 | 说明 |
| --- | --- | --- |
| `/auth/**` | `http://localhost:8081` | 认证服务。 |
| `/api/users/**` | `http://localhost:8082` | 用户 API 服务。 |
| `/api/manager/**` | `http://localhost:8083` | 管理 API 服务。 |

网关白名单路径：

| 路径前缀 | 说明 |
| --- | --- |
| `/auth/api/login` | API 用户登录，无需 token。 |
| `/auth/manager/login` | 管理员登录，无需 token。 |
| `/auth/refresh` | Access token 刷新，无需 access token，但需要合法 refresh token。 |
| `/actuator/health` | 网关健康检查，无需 token。 |

其他请求都需要合法 Bearer JWT。

## 接口清单

所有业务调用建议通过 `gateway-service` 的 `8080` 端口访问。下游服务自身端口只用于本地调试。

### API 用户登录

```http
POST /auth/api/login
Content-Type: application/json
```

请求体：

```json
{
  "username": "alice",
  "password": "demo"
}
```

成功响应：

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "username": "alice",
  "userType": "API",
  "roles": ["USER"],
  "permissions": ["user:profile:read"],
  "expiresAt": "2026-09-09T12:00:00",
  "refreshTokenExpiresAt": "2026-09-16T10:00:00"
}
```

校验规则：

- 用户名和密码不能为空。
- 当前 demo 账号为 `alice / demo`。
- 按 `username@clientIp` 记录失败次数。
- 失败次数达到 `demo.auth.login.api.max-failed-attempts` 后锁定 `demo.auth.login.api.lock-seconds` 秒。

可能状态码：

| 状态码 | 场景 |
| --- | --- |
| `200` | 登录成功。 |
| `400` | 请求体字段为空或格式错误。 |
| `401` | 用户名或密码错误。 |
| `429` | API 登录失败次数过多，临时锁定。 |

### 管理员登录

```http
POST /auth/manager/login
Content-Type: application/json
```

请求体：

```json
{
  "username": "manager",
  "password": "demo"
}
```

成功响应：

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "username": "manager",
  "userType": "MANAGER",
  "roles": ["MANAGER"],
  "permissions": ["manager:login", "manager:dashboard:read", "user:profile:read"],
  "expiresAt": "2026-09-09T12:00:00",
  "refreshTokenExpiresAt": "2026-09-16T10:00:00"
}
```

校验规则：

- 用户名和密码不能为空。
- 当前 demo 账号为 `manager / demo`。
- 来源 IP 必须在 `demo.auth.login.manager.allowed-ips` 中，配置 `*` 可放开 IP 限制。
- 账号权限必须包含 `manager:login`。

可能状态码：

| 状态码 | 场景 |
| --- | --- |
| `200` | 登录成功。 |
| `400` | 请求体字段为空或格式错误。 |
| `401` | 用户名或密码错误。 |
| `403` | 来源 IP 不允许，或账号缺少 `manager:login` 权限。 |

### 刷新 Access Token

```http
POST /auth/refresh
Content-Type: application/json
```

请求体：

```json
{
  "refreshToken": "<refresh-token>"
}
```

成功响应：

```json
{
  "tokenType": "Bearer",
  "accessToken": "<new-jwt>",
  "refreshToken": "<same-refresh-token>",
  "username": "alice",
  "userType": "API",
  "roles": ["USER"],
  "permissions": ["user:profile:read"],
  "expiresAt": "2026-09-09T12:00:00",
  "refreshTokenExpiresAt": "2026-09-16T10:00:00"
}
```

说明：

- refresh token 是随机 opaque token，不是 JWT。
- refresh token 保存在 `fa-cloud.oauth2_authorization.refresh_token_value`。
- 刷新时通过 `JdbcOAuth2AuthorizationService.findByToken(..., OAuth2TokenType.REFRESH_TOKEN)` 查库。
- refresh token 必须存在、未过期、未失效。
- 当前配置复用 refresh token，刷新 access token 后会更新同一条 `oauth2_authorization` 记录里的 `access_token_*` 字段。

可能状态码：

| 状态码 | 场景 |
| --- | --- |
| `200` | 刷新成功。 |
| `400` | 请求体字段为空或格式错误。 |
| `401` | refresh token 不存在、过期、失效，或授权记录不完整。 |

### Token 校验

```http
POST /auth/validate
Authorization: Bearer <accessToken>
```

说明：

- 该接口由 `auth-service` 提供，当前保留为兼容性/调试接口。
- 该接口会读取 `fa-cloud.oauth2_authorization`，确认 access token 已持久化且未失效。
- `gateway-service` 的主鉴权流程已经改为 Resource Server 本地验签，不依赖该接口。

成功响应示例：

```json
{
  "valid": true,
  "username": "alice",
  "userType": "API",
  "roles": ["USER"],
  "permissions": ["user:profile:read"],
  "claims": {
    "expiresAt": "2026-09-09T08:00:00Z"
  }
}
```

无效 token 响应示例：

```json
{
  "valid": false,
  "username": null,
  "userType": null,
  "roles": [],
  "permissions": [],
  "claims": {}
}
```

### 获取当前用户

```http
GET /api/users/me
Authorization: Bearer <accessToken>
```

等价路径：

```http
GET /api/users/my
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "username": "alice",
  "displayName": "demo-user",
  "roles": ["USER"],
  "requestTime": "2026-09-09T12:00:00"
}
```

说明：

- `user-api-service` 从 `X-User-Name` 读取用户名。
- `X-User-Name` 由 `gateway-service` 从 JWT 中解析后注入。

可能状态码：

| 状态码 | 场景 |
| --- | --- |
| `200` | token 合法，返回用户资料。 |
| `401` | 缺少 token、token 格式错误、签名错误、过期或 issuer 不匹配。 |

### 用户服务健康检查

```http
GET /api/users/health
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "status": "UP",
  "service": "user-api-service"
}
```

说明：

- 通过网关访问该接口时需要 JWT。
- 如果直接访问 `user-api-service:8082/api/users/health`，当前服务自身没有本地鉴权过滤。

### 获取管理员面板

```http
GET /api/manager/dashboard
Authorization: Bearer <managerAccessToken>
```

成功响应：

```json
{
  "username": "manager",
  "displayName": "manager-user",
  "roles": ["MANAGER"],
  "permissions": ["manager:login", "manager:dashboard:read", "user:profile:read"],
  "requestTime": "2026-09-09T12:00:00"
}
```

说明：

- `gateway-service` 会先验证 JWT。
- 路径匹配 `/api/manager/**` 时，JWT 的 `roles` 必须包含 `MANAGER`。
- `manager-service` 从 `X-User-Name`、`X-User-Roles`、`X-User-Permissions` 读取用户身份、角色和权限。

可能状态码：

| 状态码 | 场景 |
| --- | --- |
| `200` | 管理员 token 合法。 |
| `401` | 缺少 token、token 格式错误、签名错误、过期或 issuer 不匹配。 |
| `403` | token 合法但缺少 `MANAGER` 角色。 |

### 管理服务健康检查

```http
GET /api/manager/health
Authorization: Bearer <managerAccessToken>
```

成功响应：

```json
{
  "status": "UP",
  "service": "manager-service"
}
```

说明：

- 因为路径属于 `/api/manager/**`，通过网关访问时也需要 `MANAGER` 角色。

### 管理服务系统管理 CRUD

`manager-service` 暴露系统管理 CRUD，基础路径：

```http
/api/manager/system
```

| 资源 | 列表 | 单条 | 新增 | 修改 | 删除 |
| --- | --- | --- | --- | --- | --- |
| 用户 | `GET /api/manager/system/users` | `GET /api/manager/system/users/{id}` | `POST /api/manager/system/users` | `PUT /api/manager/system/users/{id}` | `DELETE /api/manager/system/users/{id}` |
| 角色 | `GET /api/manager/system/roles` | `GET /api/manager/system/roles/{id}` | `POST /api/manager/system/roles` | `PUT /api/manager/system/roles/{id}` | `DELETE /api/manager/system/roles/{id}` |
| 权限 | `GET /api/manager/system/permissions` | `GET /api/manager/system/permissions/{id}` | `POST /api/manager/system/permissions` | `PUT /api/manager/system/permissions/{id}` | `DELETE /api/manager/system/permissions/{id}` |
| 菜单 | `GET /api/manager/system/menus` | `GET /api/manager/system/menus/{id}` | `POST /api/manager/system/menus` | `PUT /api/manager/system/menus/{id}` | `DELETE /api/manager/system/menus/{id}` |

通过网关访问 `/api/manager/system/**` 时需要合法 JWT，并且 JWT 的 `roles` 必须包含 `MANAGER`。

### Actuator 健康检查

各服务都暴露 actuator health/info。通过网关访问：

```http
GET /actuator/health
```

说明：

- `gateway-service` 对 `/actuator/health` 放行。
- 其他服务的 actuator endpoint 是否直接访问，取决于对应服务端口和本地网络暴露方式。

## 配置项

### auth-service

端口：

```yaml
server:
  port: 8081
```

JWT：

```yaml
demo:
  auth:
    jwt:
      issuer: cloud-family-demo
      secret: cloud-family-demo-jwt-signing-secret-please-change-in-production
      ttl-seconds: 7200
      refresh-ttl-seconds: 604800
```

登录限制：

```yaml
demo:
  auth:
    login:
      api:
        max-failed-attempts: 5
        lock-seconds: 300
      manager:
        allowed-ips:
          - 127.0.0.1
          - 0:0:0:0:0:0:0:1
          - "::1"
```

MySQL 多数据源：

```yaml
demo:
  datasource:
    crypto:
      secret: ${DATASOURCE_CRYPTO_SECRET:cloud-family-demo-datasource-crypto-secret}
    cloud:
      url: ${FA_CLOUD_DB_URL:jdbc:mysql://localhost:3306/fa-cloud?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8}
      username: ${FA_CLOUD_DB_USERNAME:ENC(U0rj/0TFqN/ikdyYo1QcsZa9APbjTkdLYLth5ycDEk4=)}
      password: ${FA_CLOUD_DB_PASSWORD:ENC(O/mIkbzTm1uHasWUrJSMKaIDlfcpqXW6E8FFh3e0+Y0=)}
      maximum-pool-size: ${FA_CLOUD_DB_MAX_POOL_SIZE:10}
      minimum-idle: ${FA_CLOUD_DB_MIN_IDLE:2}
      connection-timeout: ${FA_CLOUD_DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${FA_CLOUD_DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${FA_CLOUD_DB_MAX_LIFETIME:1800000}
      validation-timeout: ${FA_CLOUD_DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${FA_CLOUD_DB_LEAK_DETECTION_THRESHOLD:0}
      pool-name: fa-cloud
    model:
      url: ${FA_MODEL_DB_URL:jdbc:mysql://localhost:3306/fa-model?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8}
      username: ${FA_MODEL_DB_USERNAME:ENC(U0rj/0TFqN/ikdyYo1QcsZa9APbjTkdLYLth5ycDEk4=)}
      password: ${FA_MODEL_DB_PASSWORD:ENC(O/mIkbzTm1uHasWUrJSMKaIDlfcpqXW6E8FFh3e0+Y0=)}
      maximum-pool-size: ${FA_MODEL_DB_MAX_POOL_SIZE:10}
      minimum-idle: ${FA_MODEL_DB_MIN_IDLE:2}
      connection-timeout: ${FA_MODEL_DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${FA_MODEL_DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${FA_MODEL_DB_MAX_LIFETIME:1800000}
      validation-timeout: ${FA_MODEL_DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${FA_MODEL_DB_LEAK_DETECTION_THRESHOLD:0}
      pool-name: fa-model
```

以上配置已添加到 `auth-service`、`gateway-service`、`user-api-service`、`manager-service`。可通过下面环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DATASOURCE_CRYPTO_SECRET` | `cloud-family-demo-datasource-crypto-secret` | 数据源用户名和密码的 AES-GCM 解密密钥。 |
| `FA_CLOUD_DB_URL` | `jdbc:mysql://localhost:3306/fa-cloud?...` | `fa-cloud` JDBC URL。 |
| `FA_CLOUD_DB_USERNAME` | `ENC(...)`，解密后为 `root` | `fa-cloud` 用户名。 |
| `FA_CLOUD_DB_PASSWORD` | `ENC(...)`，解密后为 `root` | `fa-cloud` 密码。 |
| `FA_CLOUD_DB_MAX_POOL_SIZE` | `10` | `fa-cloud` 最大连接数。 |
| `FA_CLOUD_DB_MIN_IDLE` | `2` | `fa-cloud` 最小空闲连接数。 |
| `FA_MODEL_DB_URL` | `jdbc:mysql://localhost:3306/fa-model?...` | `fa-model` JDBC URL。 |
| `FA_MODEL_DB_USERNAME` | `ENC(...)`，解密后为 `root` | `fa-model` 用户名。 |
| `FA_MODEL_DB_PASSWORD` | `ENC(...)`，解密后为 `root` | `fa-model` 密码。 |
| `FA_MODEL_DB_MAX_POOL_SIZE` | `10` | `fa-model` 最大连接数。 |
| `FA_MODEL_DB_MIN_IDLE` | `2` | `fa-model` 最小空闲连接数。 |

### gateway-service

端口和路由：

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: auth-service
              uri: http://localhost:8081
              predicates:
                - Path=/auth/**
            - id: user-api-service
              uri: http://localhost:8082
              predicates:
                - Path=/api/users/**
            - id: manager-service
              uri: http://localhost:8083
              predicates:
                - Path=/api/manager/**
```

JWT 验签：

```yaml
demo:
  auth:
    jwt:
      issuer: cloud-family-demo
      secret: cloud-family-demo-jwt-signing-secret-please-change-in-production
```

### user-api-service

```yaml
server:
  port: 8082
```

### manager-service

```yaml
server:
  port: 8083
```

## 本地启动

编译：

```bash
mvn clean package
```

分别启动服务：

```bash
mvn -pl auth-service spring-boot:run
mvn -pl user-api-service spring-boot:run
mvn -pl manager-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

也可以在 IntelliJ IDEA 使用项目中的 `All Services` compound run configuration 同时启动。

## 调用示例

API 用户登录：

```bash
curl -s -X POST http://localhost:8080/auth/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"demo"}'
```

使用返回的 `accessToken` 访问用户接口：

```bash
curl -s http://localhost:8080/api/users/me \
  -H 'Authorization: Bearer <accessToken>'
```

管理员登录：

```bash
curl -s -X POST http://localhost:8080/auth/manager/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"demo"}'
```

使用管理员 token 访问管理接口：

```bash
curl -s http://localhost:8080/api/manager/dashboard \
  -H 'Authorization: Bearer <accessToken>'
```

用普通 API 用户 token 访问管理接口会返回 `403`：

```json
{
  "code": 403,
  "message": "Manager role required"
}
```

## 当前 Demo 账号

| 类型 | 用户名 | 密码 | 角色 | 权限 |
| --- | --- | --- | --- | --- |
| API 用户 | `alice` | `demo` | `USER` | `user:profile:read` |
| 管理员 | `manager` | `demo` | `MANAGER` | `manager:login`, `manager:dashboard:read`, `user:profile:read` |

## 安全边界

- `gateway-service` 是外部访问入口，负责 JWT 本地验签和管理接口角色判断。
- 下游服务当前依赖网关注入的 `X-User-*` header，不直接解析 JWT。
- 生产部署时应避免下游服务被公网或不可信网络直接访问，否则调用方可以伪造 `X-User-*` header。
- 当前 HS256 secret 写在配置文件中，仅适合 demo。生产环境应使用环境变量、配置中心或密钥管理系统。
- 当前 demo 没有实现 refresh token、登出、token 黑名单、账号数据库、密码哈希和 RBAC 持久化。

## 扩展建议

- 把 demo 内存账号替换为数据库用户表。
- 使用 BCrypt/Argon2 存储密码哈希。
- 使用 RSA/ECDSA JWT 签名，并暴露 JWK Set 给网关和其他资源服务。
- 增加 refresh token、logout 和 token revoke 能力。
- 将权限判断从简单角色判断扩展为基于 `permissions` 的细粒度授权。
- 对下游服务增加内部网络隔离或服务间认证。
