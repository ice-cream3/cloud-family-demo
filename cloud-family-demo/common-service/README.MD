# common-service 说明

`common-service` 是 `cloud-family-demo` 的公共 Jar 模块，不单独启动。它向 `gateway-service`、`auth-service`、`partner-service`、`manager-service` 提供共享配置模型、数据库自动配置、安全工具、统一响应模型和业务异常模型。

## 模块定位

`common-service` 只负责横向公共能力，不承载独立业务流程：

- 公共 Web 返回结构：`ApiResponse`
- 公共异常和错误码：`BusinessException`、`ErrorCode`
- 网关和下游服务约定的身份 Header 常量：`GatewayHeaders`
- JWT 基础配置和 HMAC 密钥工具：`CommonJwtProperties`、`JwtSecretKeys`
- MySQL 双数据源自动配置：`fa-cloud`、`fa-model`
- 数据源用户名、密码的 `ENC(...)` 加解密工具
- OAuth2 授权服务表结构初始化脚本

依赖方向必须保持为：

```text
gateway-service/auth-service/partner-service/manager-service -> common-service
```

`common-service` 不应反向依赖任何业务服务模块。

## 接入方式

业务服务在 `pom.xml` 中依赖本模块：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common-service</artifactId>
    <version>${project.version}</version>
</dependency>
```

模块通过 Spring Boot 4 自动配置机制注册公共数据源配置：

```text
common-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

当前自动配置类：

```text
com.kfpd.cloud.common.datasource.MultiDataSourceAutoConfiguration
```

只要业务服务依赖了 `common-service`，且 classpath 中存在 `DataSource`、`JdbcTemplate`、`HikariDataSource`，就会自动创建 `fa-cloud` 和 `fa-model` 两组 JDBC Bean。

## 工程上下文

| 模块 | 默认端口 | 职责 |
| --- | --- | --- |
| `gateway-service` | `8080` | 统一入口、路由转发、JWT 本地验签、用户身份 header 注入、管理接口角色校验。 |
| `auth-service` | `8081` | 登录、刷新 token、JWT 签发、OAuth2 授权数据持久化。 |
| `partner-service` | `8082` | 伙伴侧用户接口和 VIP 用户维护。 |
| `manager-service` | `8083` | 管理侧用户、角色、权限、菜单接口。 |
| `common-service` | 无 | 公共代码和自动配置。 |

默认 profile 是 `dev`：

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

切换测试环境：

```bash
SPRING_PROFILES_ACTIVE=test mvn -pl auth-service spring-boot:run
```

## 包结构

```text
common-service/src/main/java/com/kfpd/cloud/common
├── datasource
│   ├── DataSourceCryptoProperties.java
│   ├── EncryptedDataSourceValues.java
│   ├── MultiDataSourceAutoConfiguration.java
│   ├── MultiDataSourceNames.java
│   └── MultiDataSourceProperties.java
├── exception
│   ├── BusinessException.java
│   └── ErrorCode.java
├── security
│   ├── CommonJwtProperties.java
│   └── JwtSecretKeys.java
└── web
    ├── ApiResponse.java
    └── GatewayHeaders.java
```

自动配置入口：

```text
common-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 统一返回结果

类：`com.kfpd.cloud.common.web.ApiResponse`

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `int` | 业务返回码。成功固定为 `0`。 |
| `message` | `String` | 返回消息。成功固定为 `success`。 |
| `data` | 泛型 | 业务数据。无数据时为 `null`。 |
| `timestamp` | `LocalDateTime` | 响应生成时间。 |

成功响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "username": "alice"
  },
  "timestamp": "2026-09-11T14:11:35"
}
```

错误响应示例：

```json
{
  "code": 100001,
  "message": "Invalid api username or password",
  "data": null,
  "timestamp": "2026-09-11T14:11:35"
}
```

静态方法：

| 方法 | 说明 |
| --- | --- |
| `ApiResponse.success(data)` | 返回成功响应并携带数据。 |
| `ApiResponse.success()` | 返回成功响应，不携带数据。 |
| `ApiResponse.error(errorCode)` | 按 `ErrorCode` 返回错误响应。 |
| `ApiResponse.error(errorCode, message)` | 按 `ErrorCode` 返回错误响应，并覆盖消息。 |

当前已接入：

- `auth-service` 登录、刷新、token 校验接口。
- `partner-service` 用户资料、VIP 用户 CRUD、health 接口。
- `manager-service` dashboard、用户、角色、权限、菜单 CRUD 和关系维护接口。
- `gateway-service` 鉴权失败响应。
- 各服务全局异常处理器。

## 自定义异常编码

枚举：`com.kfpd.cloud.common.exception.ErrorCode`

异常类：`com.kfpd.cloud.common.exception.BusinessException`

错误码分段：

| 范围 | 模块 | 示例 |
| --- | --- | --- |
| `40000`、`40100`、`40300`、`40400`、`50000` | 公共错误 | 参数错误、未认证、无权限、资源不存在、服务异常。 |
| `100001` - `100999` | `auth-service` | 登录失败、refresh token 无效、OAuth2 client 缺失。 |
| `200001` - `200999` | `partner-service` | VIP 用户不存在。 |
| `300001` - `300999` | `manager-service` | 管理资源不存在。 |
| `400001` - `400999` | `gateway-service` | 缺少认证 JWT、缺少管理角色。 |

业务代码抛出示例：

```java
throw new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
```

覆盖错误消息：

```java
throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "username is required");
```

三个 Servlet 服务分别有全局异常处理器：

| 服务 | 处理器 |
| --- | --- |
| `auth-service` | `AuthExceptionHandler` |
| `partner-service` | `PartnerExceptionHandler` |
| `manager-service` | `ManagerExceptionHandler` |

异常日志策略：

| 异常类型 | 日志级别 | 说明 |
| --- | --- | --- |
| `BusinessException` | `warn` | 记录 `code/status/method/path/message`。 |
| 参数绑定/校验异常 | `warn` | 记录公共错误码和请求信息。 |
| `ResponseStatusException` | `warn` | 兼容 Spring 抛出的状态异常。 |
| 未知异常 | `error` | 记录堆栈。 |
| gateway 鉴权失败 | `warn` | 记录网关错误码、HTTP 状态、method、path。 |

## JWT 公共配置

类：`com.kfpd.cloud.common.security.CommonJwtProperties`

绑定前缀：

```yaml
demo:
  auth:
    jwt:
      issuer: cloud-family-demo
      secret: cloud-family-demo-jwt-signing-secret-please-change-in-production
```

字段：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `issuer` | `String` | `cloud-family-demo` | JWT 签发方。`auth-service` 写入，`gateway-service` 验签。 |
| `secret` | `String` | 无 | HS256 对称签名密钥。auth 和 gateway 必须一致。 |
| `ttlSeconds` | `long` | `7200` | Access token TTL 默认值，仅用于首次初始化 OAuth2 client 入库。 |
| `refreshTtlSeconds` | `long` | `604800` | Refresh token TTL 默认值，仅用于首次初始化 OAuth2 client 入库。 |

运行期 token 有效期来源：

- `auth-service` 登录和刷新 token 时，从数据库 `oauth2_registered_client.token_settings` 读取 TTL。
- `application-dev.yml` / `application-test.yml` 不再直接配置 `ttl-seconds` 和 `refresh-ttl-seconds`。
- `CommonJwtProperties` 中的 TTL 默认值只用于 `AuthOAuth2JdbcConfig` 首次创建 demo client 时写入数据库。

## JWT Secret 工具

类：`com.kfpd.cloud.common.security.JwtSecretKeys`

方法：

```java
SecretKey hmacSha256Key(String secret)
```

用途：

- 将字符串密钥转换为 `HmacSHA256` 的 `SecretKey`。
- `auth-service` 的 `JwtEncoder` / `JwtDecoder` 和 `gateway-service` 的 `ReactiveJwtDecoder` 复用同一套密钥转换逻辑。

生产建议：

- 使用环境变量、配置中心或密钥管理系统覆盖默认 secret。
- 多服务部署时，auth 和 gateway 必须使用同一个 secret。
- 如果需要多调用方验签或密钥轮换，建议切换到 RSA/ECDSA 并暴露 JWK Set。

## 网关透传 Header

类：`com.kfpd.cloud.common.web.GatewayHeaders`

| 常量 | Header 名 | 说明 |
| --- | --- | --- |
| `USER_NAME` | `X-User-Name` | 当前认证用户名。 |
| `USER_TYPE` | `X-User-Type` | 用户类型，例如 `API`、`MANAGER`。 |
| `USER_ROLES` | `X-User-Roles` | 逗号分隔的角色列表。 |
| `USER_PERMISSIONS` | `X-User-Permissions` | 逗号分隔的权限列表。 |

这些 header 由 `gateway-service` 在 JWT 验签成功后注入，下游服务直接读取。

安全边界：

- 下游服务默认信任 `X-User-*` header。
- 生产部署时应限制下游服务只能被网关访问，避免外部调用方伪造 header。

## MySQL 多数据源自动配置

相关类：

- `MultiDataSourceProperties`
- `DataSourceCryptoProperties`
- `MultiDataSourceAutoConfiguration`
- `MultiDataSourceNames`
- `EncryptedDataSourceValues`

配置类：

| 类 | 绑定前缀 | 说明 |
| --- | --- | --- |
| `MultiDataSourceProperties` | `demo.datasource` | 维护 `cloud`、`model` 两组连接池配置。 |
| `DataSourceCryptoProperties` | `demo.datasource.crypto` | 维护数据源密文解密密钥。 |

绑定前缀：

```yaml
demo:
  datasource:
    crypto:
      secret: cloud-family-demo-datasource-crypto-secret
    cloud:
      url: jdbc:mysql://localhost:3306/fa-cloud?useSSL=false&allowPublicKeyRetrieval=true&preserveInstants=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8
      username: ENC(...)
      password: ENC(...)
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      leak-detection-threshold: 0
      pool-name: fa-cloud
    model:
      url: jdbc:mysql://localhost:3306/fa-model?useSSL=false&allowPublicKeyRetrieval=true&preserveInstants=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8
      username: ENC(...)
      password: ENC(...)
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      leak-detection-threshold: 0
      pool-name: fa-model
```

数据源字段：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `url` | 无 | JDBC URL，必须按环境配置。 |
| `username` | 无 | 数据库用户名，支持明文或 `ENC(...)`。 |
| `password` | 无 | 数据库密码，支持明文或 `ENC(...)`。 |
| `driver-class-name` | `com.mysql.cj.jdbc.Driver` | JDBC Driver。 |
| `maximum-pool-size` | `10` | Hikari 最大连接数。 |
| `minimum-idle` | `2` | Hikari 最小空闲连接数。 |
| `connection-timeout` | `30000` | 获取连接超时时间，单位毫秒。 |
| `idle-timeout` | `600000` | 空闲连接保留时间，单位毫秒。 |
| `max-lifetime` | `1800000` | 连接最长生命周期，单位毫秒。 |
| `validation-timeout` | `5000` | 连接校验超时时间，单位毫秒。 |
| `leak-detection-threshold` | `0` | 连接泄漏检测阈值，`0` 表示关闭。 |
| `pool-name` | `fa-cloud` / `fa-model` | 连接池名称。未配置时使用默认池名。 |

自动创建的 Bean：

| Bean 名 | 类型 | Primary | 说明 |
| --- | --- | --- | --- |
| `faCloudDataSource` | `DataSource` | 是 | `fa-cloud` 主数据源。 |
| `faModelDataSource` | `DataSource` | 否 | `fa-model` 数据源。 |
| `faCloudJdbcTemplate` | `JdbcTemplate` | 是 | 主库 JDBC 操作模板。 |
| `faModelJdbcTemplate` | `JdbcTemplate` | 否 | 模型库 JDBC 操作模板。 |
| `faCloudTransactionManager` | `PlatformTransactionManager` | 是 | 主库事务管理器。 |
| `faModelTransactionManager` | `PlatformTransactionManager` | 否 | 模型库事务管理器。 |

使用示例：

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

事务示例：

```java
import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
public void updateModelData() {
    // Use faModelJdbcTemplate in this method.
}
```

## 数据源密文

用户名和密码支持：

- 明文：`root`
- 密文：`ENC(base64Payload)`

`EncryptedDataSourceValues` 会在创建 `DataSource` 前自动解密 `ENC(...)`。

加密格式：

| 项 | 值 |
| --- | --- |
| Cipher | `AES/GCM/NoPadding` |
| IV 长度 | `12` bytes |
| Tag 长度 | `128` bits |
| Key 派生 | 对 `demo.datasource.crypto.secret` 做 `SHA-256` 后作为 AES key。 |
| 密文格式 | `ENC(base64(iv + ciphertext))` |

默认解密密钥：

```yaml
demo:
  datasource:
    crypto:
      secret: ${DATASOURCE_CRYPTO_SECRET:cloud-family-demo-datasource-crypto-secret}
```

环境变量覆盖也支持明文或 `ENC(...)`：

| 环境变量 | 说明 |
| --- | --- |
| `DATASOURCE_CRYPTO_SECRET` | dev 环境数据源密文解密密钥。 |
| `DATASOURCE_TEST_CRYPTO_SECRET` | test 环境数据源密文解密密钥。 |
| `FA_CLOUD_DB_USERNAME` / `FA_CLOUD_DB_PASSWORD` | dev 主库账号密码。 |
| `FA_MODEL_DB_USERNAME` / `FA_MODEL_DB_PASSWORD` | dev 模型库账号密码。 |
| `FA_CLOUD_TEST_DB_USERNAME` / `FA_CLOUD_TEST_DB_PASSWORD` | test 主库账号密码。 |
| `FA_MODEL_TEST_DB_USERNAME` / `FA_MODEL_TEST_DB_PASSWORD` | test 模型库账号密码。 |

生产环境应覆盖默认解密密钥，并重新生成数据库用户名和密码密文。

生成密文的最小示例：

```java
import com.kfpd.cloud.common.datasource.EncryptedDataSourceValues;

class EncryptDatasourceValue {

    public static void main(String[] args) {
        String secret = args[0];
        String value = args[1];
        System.out.println(EncryptedDataSourceValues.encrypt(value, secret));
    }
}
```

也可以临时在测试或本地小工具中调用：

```java
String encrypted = EncryptedDataSourceValues.encrypt("root", "cloud-family-demo-datasource-crypto-secret");
```

注意事项：

- `ENC(...)` 每次生成都会使用随机 IV，同一个明文多次加密的密文不同是正常现象。
- 只加密配置值，不要加密完整 JDBC URL。
- 修改 `DATASOURCE_CRYPTO_SECRET` 后，已有密文必须用新密钥重新生成。
- 生产环境不要继续使用 README 或配置文件中的默认密钥。

## 环境配置

各可运行服务都使用三类配置文件：

| 文件 | 说明 |
| --- | --- |
| `application.yml` | 环境无关配置，主要包含服务名、默认 profile、actuator 暴露项。 |
| `application-dev.yml` | dev 端口、路由、JWT secret、登录限制、数据源。 |
| `application-test.yml` | test 端口、路由、JWT secret、登录限制、数据源。 |

dev 数据库默认值：

| 数据源 | 默认库 |
| --- | --- |
| `demo.datasource.cloud` | `fa-cloud` |
| `demo.datasource.model` | `fa-model` |

test 数据库默认值：

| 数据源 | 默认库 |
| --- | --- |
| `demo.datasource.cloud` | `fa-cloud-test` |
| `demo.datasource.model` | `fa-model-test` |

常用启动参数：

```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_PROFILES_ACTIVE=test
```

服务端口可按环境覆盖：

| 服务 | dev 变量 | test 变量 |
| --- | --- | --- |
| gateway | `GATEWAY_SERVER_PORT` | `GATEWAY_TEST_SERVER_PORT` |
| auth | `AUTH_SERVER_PORT` | `AUTH_TEST_SERVER_PORT` |
| partner | `PARTNER_SERVER_PORT` | `PARTNER_TEST_SERVER_PORT` |
| manager | `MANAGER_SERVER_PORT` | `MANAGER_TEST_SERVER_PORT` |

## OAuth2 授权表

初始化脚本：

```text
common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

执行：

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

脚本创建：

| 表 | 说明 |
| --- | --- |
| `oauth2_registered_client` | 保存 OAuth2 client 注册信息，`token_settings` 中保存 access/refresh token TTL。 |
| `oauth2_authorization` | 保存 access token、refresh token、授权属性等运行期授权数据。 |
| `oauth2_authorization_consent` | 保存授权同意记录。 |

`auth-service` 启动时会初始化两个 demo client：

| client_id | registered_client_id | grant type | scopes |
| --- | --- | --- | --- |
| `cloud-api-client` | `cloud-api-client-registration` | `demo_api_login` | `user:profile:read` |
| `cloud-manager-client` | `cloud-manager-client-registration` | `demo_manager_login` | `manager:login`, `manager:dashboard:read`, `user:profile:read` |

Token TTL 保存在 `oauth2_registered_client.token_settings`。如果需要调整有效期，修改对应 client 的 `token_settings` 后重启或确保重新读取到更新后的 `RegisteredClient`。

## 认证流程

1. 客户端请求网关登录接口：`POST /auth/api/login` 或 `POST /auth/manager/login`。
2. `gateway-service` 将 `/auth/**` 转发到 `auth-service`。
3. `auth-service` 校验账号、密码、登录限制和管理端 IP 白名单。
4. `auth-service` 从 `oauth2_registered_client.token_settings` 读取 token TTL。
5. `auth-service` 签发 JWT access token，生成 opaque refresh token。
6. `auth-service` 通过 `JdbcOAuth2AuthorizationService` 将授权数据写入 `oauth2_authorization`。
7. 客户端访问业务接口时携带 `Authorization: Bearer <accessToken>`。
8. `gateway-service` 使用 HS256 secret 本地验签 JWT。
9. 验签成功后，网关注入 `X-User-*` header 并转发到下游。
10. 访问 `/api/manager/**` 时，网关要求 JWT roles 包含 `MANAGER` 或 `SUPER_ADMIN`。

## JWT Claims

| Claim | 类型 | 说明 |
| --- | --- | --- |
| `iss` | `String` | 签发方。 |
| `sub` | `String` | JWT subject，当前为用户名。 |
| `iat` | `Instant` | 签发时间。 |
| `exp` | `Instant` | 过期时间。 |
| `username` | `String` | 用户名。 |
| `user_type` | `String` | 用户类型。 |
| `roles` | `List<String>` | 角色列表。 |
| `permissions` | `List<String>` | 权限列表。 |
| `client_id` | `String` | OAuth2 client id。 |

## Gateway 路由

| 网关路径 | 默认转发目标 |
| --- | --- |
| `/auth/**` | `http://localhost:8081` |
| `/api/users/**` | `http://localhost:8082` |
| `/api/manager/**` | `http://localhost:8083` |

白名单：

| 路径 | 说明 |
| --- | --- |
| `/auth/api/login` | API 用户登录。 |
| `/auth/manager/login` | 管理员登录。 |
| `/auth/refresh` | 刷新 access token。 |
| `/actuator/health` | 网关健康检查。 |

其他路径都需要合法 Bearer JWT。

## Log4j2 日志

四个可运行服务都使用 `log4j2-spring.xml`：

```text
gateway-service/src/main/resources/log4j2-spring.xml
auth-service/src/main/resources/log4j2-spring.xml
partner-service/src/main/resources/log4j2-spring.xml
manager-service/src/main/resources/log4j2-spring.xml
```

Log4j2 配置内部通过 Spring profile 区分环境：

```xml
<SpringProfile name="dev">
    ...
</SpringProfile>

<SpringProfile name="test">
    ...
</SpringProfile>
```

环境差异：

| 环境 | `com.kfpd.cloud` 级别 | Root 级别 | 滚动大小 | 保留数量 |
| --- | --- | --- | --- | --- |
| `dev` | `DEBUG` | `INFO` | `50 MB` | `14` |
| `test` | `INFO` | `WARN` | `100 MB` | `30` |

日志文件：

```text
logs/<spring.application.name>/<spring.profiles.active>/application.log
logs/<spring.application.name>/<spring.profiles.active>/error.log
```

可通过环境变量 `LOG_DIR` 修改日志根目录。

工程根 `.gitignore` 已忽略：

```text
target/
logs/
```

## 本地初始化

初始化 OAuth2 授权表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

初始化 partner VIP 用户表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < partner-service/src/main/resources/db/fa-cloud/vip-user-schema.sql
```

初始化 manager 系统管理表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < manager-service/src/main/resources/db/fa-cloud/system-management-schema.sql
```

## 本地启动

编译：

```bash
mvn clean package
```

分别启动服务：

```bash
mvn -pl auth-service spring-boot:run
mvn -pl partner-service spring-boot:run
mvn -pl manager-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

test 环境启动示例：

```bash
SPRING_PROFILES_ACTIVE=test mvn -pl auth-service spring-boot:run
```

IntelliJ IDEA 可使用项目中的 `All Services` compound run configuration 同时启动。

## 调用示例

API 用户登录：

```bash
curl -s -X POST http://localhost:8080/auth/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"demo"}'
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "<jwt>",
    "refreshToken": "<refresh-token>",
    "username": "alice",
    "userType": "API",
    "roles": [],
    "permissions": [],
    "expiresAt": "2026-09-11T16:00:00",
    "refreshTokenExpiresAt": "2026-09-18T14:00:00"
  },
  "timestamp": "2026-09-11T14:00:00"
}
```

访问当前用户：

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

访问管理端：

```bash
curl -s http://localhost:8080/api/manager/dashboard \
  -H 'Authorization: Bearer <managerAccessToken>'
```

无权限访问管理端的错误响应：

```json
{
  "code": 400002,
  "message": "Manager role required",
  "data": null,
  "timestamp": "2026-09-11T14:00:00"
}
```

## Demo 账号

| 类型 | 用户名 | 密码 | 角色 | 权限 |
| --- | --- | --- | --- | --- |
| API 用户 | `alice` | `demo` | 无 | `user:profile:read` |
| 管理员 | `manager` | `demo` | `MANAGER` | `manager:login`, `manager:dashboard:read`, `user:profile:read` |
| 超级管理员 | `admin` | `demo` | `SUPER_ADMIN` | `manager:login`, `manager:dashboard:read`, `user:profile:read` |

## 验证命令

只验证 `common-service`：

```bash
mvn -pl common-service test
```

验证依赖 `common-service` 的完整构建：

```bash
mvn -pl common-service,gateway-service,auth-service,partner-service,manager-service -am test
```

完整测试：

```bash
mvn test
```

检查 Log4j2 依赖：

```bash
mvn -pl gateway-service,auth-service,partner-service,manager-service dependency:tree \
  -Dincludes=ch.qos.logback:logback-classic,org.springframework.boot:spring-boot-starter-log4j2,org.springframework.boot:spring-boot-starter-logging
```

预期四个可运行服务只出现 `spring-boot-starter-log4j2`，不应出现 `logback-classic`。

## 生产化注意点

- 替换默认 JWT secret 和数据源密文 secret。
- 使用 BCrypt/Argon2 存储密码哈希。
- 限制下游服务只允许网关访问，防止伪造 `X-User-*` header。
- 将 token TTL、client scopes 等安全参数纳入数据库变更流程。
- 根据业务需要增加 logout、token revoke、审计日志和细粒度权限校验。

## 维护约定

- 公共错误码新增时，先在 `ErrorCode` 中分配清晰的模块区间，再由各服务异常处理器统一转换为 `ApiResponse`。
- 新增跨服务 Header 时，统一放入 `GatewayHeaders`，避免各服务手写字符串。
- 新增共享配置类时，优先使用 `@ConfigurationProperties`，并补充 README 中的绑定前缀和字段说明。
- `common-service` 不能依赖业务服务模块；如果公共代码需要调用业务逻辑，应下沉接口约定或放回具体业务服务。
- 数据库脚本放在对应逻辑库目录下，例如 `db/fa-cloud/`，脚本文件名需要能表达所属功能。
