# common-service 说明

`common-service` 是 `cloud-family-demo` 的公共 Jar 模块，不单独启动。它为各可运行服务提供共享配置、数据库连接、Redis 客户端、安全工具、统一响应结构和业务异常模型。

## 模块定位

本模块只放跨服务复用能力，不承载独立业务流程，也不依赖任何业务服务模块。

主要能力：

- 统一响应结构：`ApiResponse`
- 统一业务异常和错误码：`BusinessException`、`ErrorCode`
- 网关向下游透传身份信息的 Header 常量：`GatewayHeaders`
- JWT 基础配置和 HMAC 密钥工具：`CommonJwtProperties`、`JwtSecretKeys`
- MySQL 双数据源自动配置：`fa-cloud`、`fa-model`
- 数据源用户名、密码的 `ENC(...)` 加解密工具
- Redis Cluster 的 Redisson / Redisson Reactive 自动配置
- Spring Authorization Server JDBC 表结构脚本

依赖方向必须保持为：

```text
gateway-service/auth-service/partner-service/manager-service/job-service -> common-service
```

`common-service` 不能反向依赖这些业务模块。

## 接入方式

业务服务在 `pom.xml` 中依赖本模块：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common-service</artifactId>
    <version>${project.version}</version>
</dependency>
```

公共自动配置入口：

```text
common-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

当前注册的自动配置类：

```text
com.kfpd.cloud.common.config.datasource.MultiDataSourceAutoConfiguration
com.kfpd.cloud.common.config.redis.RedissonAutoConfiguration
```

只要业务服务依赖 `common-service`，并具备相关 classpath 条件，就会自动注册双数据源和 Redisson Bean。

## 包结构

```text
common-service/src/main/java/com/kfpd/cloud/common
├── config
│   ├── datasource
│   │   ├── DataSourceCryptoProperties.java
│   │   ├── EncryptedDataSourceValues.java
│   │   ├── MultiDataSourceAutoConfiguration.java
│   │   ├── MultiDataSourceNames.java
│   │   └── MultiDataSourceProperties.java
│   ├── redis
│   │   └── RedissonAutoConfiguration.java
│   └── security
│       ├── CommonJwtProperties.java
│       └── JwtSecretKeys.java
├── exception
│   ├── BusinessException.java
│   └── ErrorCode.java
├── utils
│   ├── CacheKeyUtil.java
│   └── RedisKeyUtil.java
└── web
    ├── ApiResponse.java
    └── GatewayHeaders.java
```

## 统一响应

类：`com.kfpd.cloud.common.web.ApiResponse`

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `int` | 业务返回码，成功固定为 `0`。 |
| `message` | `String` | 返回消息，成功固定为 `success`。 |
| `data` | 泛型 | 业务数据，无数据时为 `null`。 |
| `timestamp` | `LocalDateTime` | 响应生成时间。 |

常用方法：

| 方法 | 说明 |
| --- | --- |
| `ApiResponse.success(data)` | 成功响应，携带数据。 |
| `ApiResponse.success()` | 成功响应，不携带数据。 |
| `ApiResponse.error(errorCode)` | 按 `ErrorCode` 生成错误响应。 |
| `ApiResponse.error(errorCode, message)` | 按 `ErrorCode` 生成错误响应，并覆盖错误消息。 |

示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "username": "alice"
  },
  "timestamp": "2026-09-14T10:00:00"
}
```

## 异常和错误码

异常类：`com.kfpd.cloud.common.exception.BusinessException`

错误码枚举：`com.kfpd.cloud.common.exception.ErrorCode`

错误码分段：

| 范围 | 模块 | 示例 |
| --- | --- | --- |
| `40000`、`40100`、`40300`、`40400`、`50000` | 公共错误 | 参数错误、未认证、无权限、资源不存在、服务异常。 |
| `100001` - `100999` | `auth-service` | 登录失败、refresh token 无效、OAuth2 client 缺失。 |
| `200001` - `200999` | `partner-service` | VIP 用户不存在。 |
| `300001` - `300999` | `manager-service` | 管理资源不存在。 |
| `400001` - `400999` | `gateway-service` | 缺少认证 JWT、缺少管理角色、限流。 |

业务代码示例：

```java
throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "username is required");
```

各 Servlet 服务通过自己的异常处理器把 `BusinessException` 转成 `ApiResponse`：

| 服务 | 处理器 |
| --- | --- |
| `auth-service` | `AuthExceptionHandler` |
| `partner-service` | `PartnerExceptionHandler` |
| `manager-service` | `ManagerExceptionHandler` |

## 网关透传 Header

类：`com.kfpd.cloud.common.web.GatewayHeaders`

| 常量 | Header 名 | 说明 |
| --- | --- | --- |
| `USER_NAME` | `X-User-Name` | 当前认证用户名。 |
| `USER_TYPE` | `X-User-Type` | 用户类型，例如 `API`、`MANAGER`。 |
| `USER_ROLES` | `X-User-Roles` | 逗号分隔的角色列表。 |
| `USER_PERMISSIONS` | `X-User-Permissions` | 逗号分隔的权限列表。 |

这些 Header 由 `gateway-service` 在 JWT 验签成功后注入。生产环境应限制业务服务只能被网关访问，避免外部调用方伪造 `X-User-*`。

## JWT 公共配置

类：`com.kfpd.cloud.common.config.security.CommonJwtProperties`

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
| `issuer` | `String` | `cloud-family-demo` | JWT 签发方。 |
| `secret` | `String` | 无 | HS256 对称签名密钥，auth 和 gateway 必须一致。 |
| `ttlSeconds` | `long` | `7200` | Access token TTL 默认值，用于首次初始化 OAuth2 client。 |
| `refreshTtlSeconds` | `long` | `604800` | Refresh token TTL 默认值，用于首次初始化 OAuth2 client。 |

密钥工具：`com.kfpd.cloud.common.config.security.JwtSecretKeys`

```java
SecretKey key = JwtSecretKeys.hmacSha256Key(secret);
```

## MySQL 双数据源

相关类：

- `com.kfpd.cloud.common.config.datasource.MultiDataSourceProperties`
- `com.kfpd.cloud.common.config.datasource.DataSourceCryptoProperties`
- `com.kfpd.cloud.common.config.datasource.MultiDataSourceAutoConfiguration`
- `com.kfpd.cloud.common.config.datasource.MultiDataSourceNames`
- `com.kfpd.cloud.common.config.datasource.EncryptedDataSourceValues`

配置前缀：

| 类 | 前缀 | 说明 |
| --- | --- | --- |
| `MultiDataSourceProperties` | `demo.datasource` | 维护 `cloud`、`model` 两组连接池配置。 |
| `DataSourceCryptoProperties` | `demo.datasource.crypto` | 维护 `ENC(...)` 解密密钥。 |

配置示例：

```yaml
demo:
  datasource:
    crypto:
      secret: ${DATASOURCE_CRYPTO_SECRET:cloud-family-demo-datasource-crypto-secret}
    cloud:
      url: ${FA_CLOUD_DB_URL:jdbc:mysql://10.0.1.10:3306/fa-cloud?useSSL=false&allowPublicKeyRetrieval=true&preserveInstants=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8}
      username: ${FA_CLOUD_DB_USERNAME:ENC(...)}
      password: ${FA_CLOUD_DB_PASSWORD:ENC(...)}
      maximum-pool-size: ${FA_CLOUD_DB_MAX_POOL_SIZE:10}
      minimum-idle: ${FA_CLOUD_DB_MIN_IDLE:2}
      connection-timeout: ${FA_CLOUD_DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${FA_CLOUD_DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${FA_CLOUD_DB_MAX_LIFETIME:1800000}
      validation-timeout: ${FA_CLOUD_DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${FA_CLOUD_DB_LEAK_DETECTION_THRESHOLD:0}
      pool-name: fa-cloud
    model:
      url: ${FA_MODEL_DB_URL:jdbc:mysql://10.0.2.20:3306/fa-model?useSSL=false&allowPublicKeyRetrieval=true&preserveInstants=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8}
      username: ${FA_MODEL_DB_USERNAME:ENC(...)}
      password: ${FA_MODEL_DB_PASSWORD:ENC(...)}
      maximum-pool-size: ${FA_MODEL_DB_MAX_POOL_SIZE:10}
      minimum-idle: ${FA_MODEL_DB_MIN_IDLE:2}
      connection-timeout: ${FA_MODEL_DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${FA_MODEL_DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${FA_MODEL_DB_MAX_LIFETIME:1800000}
      validation-timeout: ${FA_MODEL_DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${FA_MODEL_DB_LEAK_DETECTION_THRESHOLD:0}
      pool-name: fa-model
```

`fa-cloud` 和 `fa-model` 可以是不同 IP、不同端口、不同账号密码。只要分别配置 `FA_CLOUD_DB_URL` 和 `FA_MODEL_DB_URL` 即可。

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
| `pool-name` | `fa-cloud` / `fa-model` | 连接池名称。 |

自动创建的 Bean：

| Bean 名 | 类型 | Primary | 说明 |
| --- | --- | --- | --- |
| `faCloudDataSource` | `DataSource` | 是 | `fa-cloud` 数据源。 |
| `faModelDataSource` | `DataSource` | 否 | `fa-model` 数据源。 |
| `faCloudJdbcTemplate` | `JdbcTemplate` | 是 | `fa-cloud` JDBC 操作模板。 |
| `faModelJdbcTemplate` | `JdbcTemplate` | 否 | `fa-model` JDBC 操作模板。 |
| `faCloudTransactionManager` | `PlatformTransactionManager` | 是 | `fa-cloud` 事务管理器。 |
| `faModelTransactionManager` | `PlatformTransactionManager` | 否 | `fa-model` 事务管理器。 |

使用示例：

```java
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
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
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
public void updateModelData() {
    // 使用 fa-model 数据源的 DAO/JdbcTemplate。
}
```

## 数据源密文

用户名和密码支持明文，也支持 `ENC(base64Payload)`。

`EncryptedDataSourceValues` 会在创建 `DataSource` 前自动解密 `ENC(...)`。加密格式：

| 项 | 值 |
| --- | --- |
| Cipher | `AES/GCM/NoPadding` |
| IV 长度 | `12` bytes |
| Tag 长度 | `128` bits |
| Key 派生 | 对 `demo.datasource.crypto.secret` 做 `SHA-256` 后作为 AES key。 |
| 密文格式 | `ENC(base64(iv + ciphertext))` |

生成密文示例：

```java
import com.kfpd.cloud.common.config.datasource.EncryptedDataSourceValues;

String encrypted = EncryptedDataSourceValues.encrypt(
        "root",
        "cloud-family-demo-datasource-crypto-secret"
);
```

注意事项：

- `ENC(...)` 每次生成都会使用随机 IV，同一个明文多次加密的密文不同是正常现象。
- 只加密用户名和密码，不要加密完整 JDBC URL。
- 修改 `DATASOURCE_CRYPTO_SECRET` 后，已有密文必须用新密钥重新生成。
- 生产环境必须覆盖默认密钥，不要使用示例密钥。

## Redis Cluster 自动配置

类：`com.kfpd.cloud.common.config.redis.RedissonAutoConfiguration`

启用条件：

- classpath 中存在 `org.redisson.api.RedissonClient`
- 配置了 `spring.data.redis.cluster.nodes`
- 当前容器中没有自定义 `RedissonClient`

配置示例：

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:123456}
      cluster:
        nodes: ${REDIS_CLUSTER_NODES:127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003,127.0.0.1:7004,127.0.0.1:7005}
```

自动创建的 Bean：

| Bean | 说明 |
| --- | --- |
| `RedissonClient` | 阻塞式 Redisson 客户端。 |
| `RedissonReactiveClient` | Reactive Redisson 客户端。 |
| `RedissonService` | 基于 Redisson 的缓存、集合、计数器和分布式锁封装。 |

`nodes` 可以写 `host:port`，自动补成 `redis://host:port`；也可以显式写 `redis://` 或 `rediss://`。

## RedissonService

类：`com.kfpd.cloud.common.config.redis.RedissonService`

当前封装的操作：

| 方法 | 说明 |
| --- | --- |
| `get` / `set` | 读写普通 key/value。 |
| `set(key, value, time)` | 写入普通 key/value 并设置秒级 TTL。 |
| `hasKey` / `del` / `expire` | 判断、删除、设置过期时间。 |
| `hGet` / `hSet` / `hGetAll` / `hDel` | Hash 类型常用操作。 |
| `sAdd` / `sMembers` / `sRemove` | Set 类型常用操作。 |
| `lAdd` / `lReadAll` / `lRemove` | List 类型常用操作。 |
| `zsetAdd` / `zsetGet` / `zsetRemoveMember` | Sorted Set 类型常用操作。 |
| `incrAndGet` / `decrAndGet` / `incr` / `decr` | 原子计数器操作。 |
| `tryLock` / `lock` / `unlock` / `execute` | 分布式锁操作。 |

使用示例：

```java
import com.kfpd.cloud.common.config.redis.RedissonService;
import org.springframework.stereotype.Service;

@Service
public class DemoCacheService {

    private final RedissonService redissonService;

    public DemoCacheService(RedissonService redissonService) {
        this.redissonService = redissonService;
    }

    public void cacheProfile(String username, String payload) {
        redissonService.set("profile:" + username, payload, 600);
    }
}
```

## Redisson 分布式锁

类：

- `com.kfpd.cloud.common.config.redis.RedissonProperties`
- `com.kfpd.cloud.common.config.redis.RedissonService`

配置前缀：

```yaml
demo:
  redisson:
    lock:
      key-prefix: ${REDIS_LOCK_KEY_PREFIX:cloud-family-demo:lock}
      wait-time-seconds: ${REDIS_LOCK_WAIT_TIME_SECONDS:5}
      lease-time-seconds: ${REDIS_LOCK_LEASE_TIME_SECONDS:30}
```

字段说明：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `key-prefix` | `cloud-family-demo:lock` | 锁 key 前缀，最终 key 形如 `cloud-family-demo:lock:<name>`。 |
| `wait-time-seconds` | `5` | 默认尝试获取锁的最长等待时间。 |
| `lease-time-seconds` | `30` | 默认锁租约时间，到期后 Redisson 自动释放锁。 |

使用示例：

```java
import com.kfpd.cloud.common.config.redis.RedissonService;
import org.springframework.stereotype.Service;

@Service
public class DemoLockedService {

    private final RedissonService redissonService;

    public DemoLockedService(RedissonService redissonService) {
        this.redissonService = redissonService;
    }

    public void rebuildStats(Long userId) {
        redissonService.execute("rebuild-stats:" + userId, () -> {
            // 需要互斥执行的业务逻辑。
        });
    }
}
```

如果不希望获取锁失败时抛异常，可以使用 `tryExecute`：

```java
redissonService.tryExecute("daily-job", () -> {
    runDailyJob();
    return true;
});
```

注意事项：

- 锁名称要包含业务维度，例如 `vip-user:update:<id>`，避免无关业务互相阻塞。
- `lease-time-seconds` 应大于正常业务执行时间；长任务应拆分或显式设置更长租约。
- `execute` 获取锁失败会抛 `IllegalStateException`；`tryExecute` 获取失败会返回 `Optional.empty()`。
- 使用手动 `tryLock` 时，必须在 `finally` 中调用 `unlock`。

## OAuth2 授权表

初始化脚本：

```text
common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

执行示例：

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

脚本创建：

| 表 | 说明 |
| --- | --- |
| `oauth2_registered_client` | OAuth2 client 注册信息，`token_settings` 中保存 access/refresh token TTL。 |
| `oauth2_authorization` | access token、refresh token、授权属性等运行期授权数据。 |
| `oauth2_authorization_consent` | 授权同意记录。 |

`auth-service` 启动时会初始化两个 demo client：

| client_id | registered_client_id | grant type | scopes |
| --- | --- | --- | --- |
| `cloud-api-client` | `cloud-api-client-registration` | `demo_api_login` | `user:profile:read` |
| `cloud-manager-client` | `cloud-manager-client-registration` | `demo_manager_login` | `manager:login`, `manager:dashboard:read`, `user:profile:read` |

## 环境变量

常用数据源变量：

| 变量 | 说明 |
| --- | --- |
| `DATASOURCE_CRYPTO_SECRET` | dev 环境数据源密文解密密钥。 |
| `DATASOURCE_TEST_CRYPTO_SECRET` | test 环境数据源密文解密密钥。 |
| `FA_CLOUD_DB_URL` | dev `fa-cloud` JDBC URL。 |
| `FA_CLOUD_DB_USERNAME` / `FA_CLOUD_DB_PASSWORD` | dev `fa-cloud` 账号密码。 |
| `FA_MODEL_DB_URL` | dev `fa-model` JDBC URL。 |
| `FA_MODEL_DB_USERNAME` / `FA_MODEL_DB_PASSWORD` | dev `fa-model` 账号密码。 |
| `FA_CLOUD_TEST_DB_URL` | test `fa-cloud-test` JDBC URL。 |
| `FA_CLOUD_TEST_DB_USERNAME` / `FA_CLOUD_TEST_DB_PASSWORD` | test `fa-cloud-test` 账号密码。 |
| `FA_MODEL_TEST_DB_URL` | test `fa-model-test` JDBC URL。 |
| `FA_MODEL_TEST_DB_USERNAME` / `FA_MODEL_TEST_DB_PASSWORD` | test `fa-model-test` 账号密码。 |

Redis 变量：

| 变量 | 说明 |
| --- | --- |
| `REDIS_CLUSTER_NODES` | dev Redis Cluster 节点列表。 |
| `REDIS_PASSWORD` | dev Redis 密码。 |
| `REDIS_TEST_CLUSTER_NODES` | test Redis Cluster 节点列表。 |
| `REDIS_TEST_PASSWORD` | test Redis 密码。 |
| `REDIS_LOCK_KEY_PREFIX` | dev Redisson 锁 key 前缀。 |
| `REDIS_LOCK_WAIT_TIME_SECONDS` | dev Redisson 锁默认等待时间。 |
| `REDIS_LOCK_LEASE_TIME_SECONDS` | dev Redisson 锁默认租约时间。 |
| `REDIS_TEST_LOCK_KEY_PREFIX` | test Redisson 锁 key 前缀。 |
| `REDIS_TEST_LOCK_WAIT_TIME_SECONDS` | test Redisson 锁默认等待时间。 |
| `REDIS_TEST_LOCK_LEASE_TIME_SECONDS` | test Redisson 锁默认租约时间。 |

## 当前服务使用方式

| 服务 | common-service 使用点 |
| --- | --- |
| `gateway-service` | JWT secret、统一错误响应、网关错误码、Header 常量。 |
| `auth-service` | JWT 配置、双数据源、OAuth2 JDBC、Redis 会话、统一异常。 |
| `partner-service` | `fa-cloud` 数据源、Redis、统一响应和异常。 |
| `manager-service` | `fa-cloud` 数据源、Redis、统一响应和异常。 |
| `job-service` | `fa-cloud` 读取、`fa-model` 写入、双事务管理器。 |

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

## 验证命令

只验证 `common-service`：

```bash
mvn -pl common-service test
```

验证依赖公共模块的主要服务：

```bash
mvn -pl common-service,gateway-service,auth-service,partner-service,manager-service,job-service test
```

完整测试：

```bash
mvn test
```

## 维护约定

- 新增公共错误码时，先在 `ErrorCode` 中分配清晰模块区间，再由各服务异常处理器统一转换为 `ApiResponse`。
- 新增跨服务 Header 时，统一放入 `GatewayHeaders`，避免各服务手写字符串。
- 新增共享配置类时，优先使用 `@ConfigurationProperties`，并在 README 中补充绑定前缀和字段说明。
- 新增自动配置类时，必须写入 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- `common-service` 不依赖业务服务模块；业务逻辑不要下沉到本模块。
- 数据库脚本按逻辑库放置，例如 `db/fa-cloud/`、`db/fa-model/`。

## 生产化注意点

- 替换默认 JWT secret 和数据源密文 secret。
- 生产密码不要直接提交到配置文件，优先使用环境变量、配置中心或密钥管理系统。
- 限制下游服务只允许网关访问，防止伪造 `X-User-*` Header。
- 根据实际连接数调整 Hikari 连接池参数，避免多服务同时启动压垮数据库。
- 将 OAuth2 client、token TTL、scope 等安全参数纳入数据库变更流程。
- 如果需要密钥轮换或多调用方验签，建议从 HS256 切换到 RSA/ECDSA 并暴露 JWK Set。
