# common-service

`common-service` 是 `cloud-family-demo` 的公共 Jar 模块，不单独启动。它为各业务服务提供共享基础设施配置、公共模型和工具类。

依赖方向必须保持为：

```text
gateway-service/auth-service/partner-service/manager-service/job-service -> common-service
```

`common-service` 不应依赖任何业务服务模块。它可以承载两端共用的纯校验和类型策略，但账号校验、商家申请审核、权限控制及事务编排仍由业务服务负责。

本文中的 Maven、MySQL 命令均在仓库根目录执行。当前构建目标为 JDK 21，依赖版本由[根 pom.xml](../pom.xml)管理；本模块没有启动类、独立端口或 `application.yml`，运行配置由引用它的服务提供。

## 快速验证

```bash
# 编译公共模块并运行现有单元测试，无需启动 MySQL、Redis 或 RocketMQ
mvn -pl common-service -am test

# 安装到本地 Maven 仓库，供单独构建的业务模块使用
mvn -pl common-service -am install
```

接入业务服务时，先配置 `demo.datasource.cloud` 和 `demo.datasource.model` 两组数据源。需要缓存、锁时配置 Redis Cluster；需要消息收发时显式启用 RocketMQ。完整服务启动顺序和地址见[项目说明](../README.md)。

## 核心能力

- 统一响应结构：`ApiResponse`
- 统一业务异常和错误码：`BusinessException`、`ErrorCode`
- 网关向下游透传身份信息的 Header 常量：`GatewayHeaders`
- JWT 公共配置和 HMAC 密钥工具：`CommonJwtProperties`、`JwtSecretKeys`
- MySQL 双数据源自动配置：`fa-cloud`、`fa-model`
- 数据源用户名和密码的 `ENC(...)` 加解密工具
- Jackson `ObjectMapper` 默认 Bean 自动配置
- Redis Cluster 的 Redisson / Redisson Reactive / RedisTemplate 自动配置
- `RedissonService` 缓存、集合、计数器和分布式锁封装
- RocketMQ producer 和通用收发封装
- Redis key 常量与拼装工具
- Spring Authorization Server JDBC 表结构脚本
- 商家、商品字段白名单校验与共用类型策略：`CommerceRules`、`CommerceTypePolicy`、`CommerceTypeStore`

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
com.kfpd.cloud.common.config.json.JacksonAutoConfiguration
com.kfpd.cloud.common.config.rocketmq.RocketMqAutoConfiguration
com.kfpd.cloud.common.config.redis.RedissonAutoConfiguration
```

自动装配条件与覆盖方式：

| 自动配置 | 启用条件 | 自定义方式 |
| --- | --- | --- |
| `MultiDataSourceAutoConfiguration` | classpath 存在 JDBC、DataSource 和 HikariCP。 | 按 `MultiDataSourceNames` 中的 Bean 名分别替换数据源、JdbcTemplate 或事务管理器。 |
| `JacksonAutoConfiguration` | 容器缺少 `com.fasterxml.jackson.databind.ObjectMapper`。 | 声明自己的同类型 Bean。 |
| `RedissonAutoConfiguration` | 存在 Redisson 类、配置了集群节点，且 `demo.redisson.enabled` 为 `true` 或未设置。 | 按类型替换客户端或 `RedissonService`；模板按 `redisTemplate` 名称替换。 |
| `RocketMqAutoConfiguration` | 存在 RocketMQ producer 类；producer 和 service Bean 还要求 `demo.rocketmq.enabled=true`。 | 分别声明自己的 `DefaultMQProducer` 或 `RocketMqService`。 |

`CommonJwtProperties` 由使用 JWT 的业务配置通过 `@EnableConfigurationProperties(CommonJwtProperties.class)` 注册，当前接入点为 auth 和 gateway；它不在上述公共自动装配清单中。MyBatis 的 Mapper 扫描、SqlSessionFactory 和业务异常处理器也由各业务服务声明。

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
│   ├── json
│   │   └── JacksonAutoConfiguration.java
│   ├── redis
│   │   ├── RedissonAutoConfiguration.java
│   │   ├── RedissonProperties.java
│   │   └── RedissonService.java
│   ├── rocketmq
│   │   ├── RocketMqAutoConfiguration.java
│   │   ├── RocketMqProperties.java
│   │   └── RocketMqService.java
│   └── security
│       ├── CommonJwtProperties.java
│       └── JwtSecretKeys.java
├── constant
│   └── RedisKeyConsts.java
├── exception
│   ├── BusinessException.java
│   └── ErrorCode.java
├── showcase
│   ├── CommerceRules.java
│   ├── CommerceTypePolicy.java
│   └── CommerceTypeStore.java
├── utils
│   └── RedisKeyUtil.java
└── web
    ├── ApiResponse.java
    └── GatewayHeaders.java
```

## 商家与商品共享规则

源码位于 `com.kfpd.cloud.common.showcase`。这些类不参与自动装配，无独立 HTTP 接口；partner 和 manager 显式调用同一套规则，通过各自的 MyBatis DAO 实现持久化接口。

| 类 / 方法 | 职责 |
| --- | --- |
| `CommerceRules.shop` | 校验店铺名称、介绍、联系方式与封面，只返回允许保存的资料字段。 |
| `CommerceRules.product` | 校验商品资料、价格、库存、排序和用户上下架状态，不接受所属账号及后台停用状态等管理字段。 |
| `CommerceRules.typeCode` | 去除首尾空白并转大写；编码以字母开头，仅含字母、数字、下划线，最多 64 位。 |
| `CommerceTypePolicy.codes` | 要求提交 1–20 个类型编码，再规范化、去重并排序。 |
| `CommerceTypePolicy.assign` | 锁定商家和类型，确认所有类型已启用，替换经营类型关联，并下架已移出经营范围的商品。 |
| `CommerceTypePolicy.product` | 确认商品类型属于商家且启用，由字典生成 `category` 展示名称，返回类型 ID 与编码。 |
| `CommerceTypePolicy.attach` | 批量补充商家的 `types`、`typeCodes`、`typeNames`，避免逐条查询。 |
| `CommerceTypeStore` | 类型查询、商家及类型行锁、关联写入和商品下架的持久化契约。 |

价格范围为 `0` 至 `9999999999.99`，最多两位小数；库存和排序为非负整数。封面支持 HTTP(S) 地址或以单个 `/` 开头的站内路径；商品详情作为文本保存。

`CommerceTypeStore` 由 [partner DAO](../partner-service/src/main/java/com/kfpd/cloud/partner/dao/cloud/CommerceTypeDao.java) 和 [manager DAO](../manager-service/src/main/java/com/kfpd/cloud/manager/dao/cloud/CommerceTypeDao.java) 实现。调用 `assign` 或 `product` 的服务方法需使用 `fa-cloud` 事务管理器，保证校验、行锁和写入在同一事务中：

```java
@Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
public void changeTypes(Long shopId, List<String> codes) {
    CommerceTypePolicy.assign(commerceTypeDao, shopId, codes);
}
```

公共策略不判断登录账号归属，也不决定商家是否允许编辑或公开展示。业务服务须先校验账号与归属，并执行申请状态限制：申请必选启用类型，提交后只读，驳回后可重提；仅审核通过且正常启用的商家可新增商品。字典由后台维护，审核时可调整申请类型。完整流程及迁移顺序见[项目 README](../README.md#商家申请与精品展示)。

## 统一响应

类：`com.kfpd.cloud.common.web.ApiResponse`

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

各 Servlet 服务通过自己的异常处理器把 `BusinessException` 转成 `ApiResponse`。

`BusinessException` 提供 `getErrorCode()`、`getCode()` 和 `getHttpStatus()`。`ApiResponse.error(...)` 只构造响应体，HTTP 状态码需要由异常处理器或调用方设置。

## 网关透传 Header

类：`com.kfpd.cloud.common.web.GatewayHeaders`

| 常量 | Header 名 | 说明 |
| --- | --- | --- |
| `USER_NAME` | `X-User-Name` | 当前认证用户名。 |
| `USER_TYPE` | `X-User-Type` | 用户类型，例如 `API`、`MANAGER`。 |
| `USER_ROLES` | `X-User-Roles` | 逗号分隔的角色列表。 |
| `USER_PERMISSIONS` | `X-User-Permissions` | 逗号分隔的权限列表。 |
| `INTERNAL_TOKEN` | `X-Gateway-Internal-Token` | 网关调用下游服务时携带的内部校验令牌。 |

这些 Header 由 `gateway-service` 在 JWT 验签及授权有效性校验后注入。partner 和 manager 的网关访问过滤器会校验内部令牌；各服务的 `demo.gateway.internal-token` 应保持一致。公共模块提供常量，具体校验逻辑由各服务负责。

## JWT 公共配置

类：`com.kfpd.cloud.common.config.security.CommonJwtProperties`

绑定前缀：

```yaml
demo:
  auth:
    jwt:
      issuer: cloud-family-demo
      secret: cloud-family-demo-jwt-signing-secret-please-change-in-production
      ttl-seconds: 7200
      refresh-ttl-seconds: 604800
```

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `issuer` | `String` | `cloud-family-demo` | JWT 签发方。 |
| `secret` | `String` | 无 | HS256 对称签名密钥，auth 和 gateway 必须一致。 |
| `ttl-seconds` | `long` | `7200` | Access token TTL 默认值。 |
| `refresh-ttl-seconds` | `long` | `604800` | Refresh token TTL 默认值。 |

密钥工具：

```java
SecretKey key = JwtSecretKeys.hmacSha256Key(secret);
```

该方法直接把 secret 的 UTF-8 字节构造成 `HmacSHA256` 密钥，不做 Base64 解码或哈希派生。auth 签发与 gateway 验签须使用相同的原始 secret。实际 token 签发、校验及有效期使用逻辑位于业务模块。

## Jackson 自动配置

类：`com.kfpd.cloud.common.config.json.JacksonAutoConfiguration`

自动配置行为：

- 当 Spring 容器中没有自定义 `ObjectMapper` 时，提供一个默认 `ObjectMapper` Bean。
- 当业务服务已经声明自己的 `ObjectMapper` 时，本配置不会覆盖业务服务配置。
- 默认 Mapper 注册 `JavaTimeModule`、发现其他可用模块，并关闭 `WRITE_DATES_AS_TIMESTAMPS`，支持 `Instant`、`LocalDateTime` 等时间类型的字符串序列化。

这个配置主要用于 `RedisTemplate` JSON 序列化等公共基础设施，避免服务没有显式声明 `ObjectMapper` 时启动失败。

## MySQL 双数据源

相关类：

- `MultiDataSourceAutoConfiguration`
- `MultiDataSourceProperties`
- `DataSourceCryptoProperties`
- `MultiDataSourceNames`
- `EncryptedDataSourceValues`

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
      url: ${FA_CLOUD_DB_URL:jdbc:mysql://localhost:3306/fa-cloud?useSSL=false&allowPublicKeyRetrieval=true&preserveInstants=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8}
      username: ${FA_CLOUD_DB_USERNAME:root}
      password: ${FA_CLOUD_DB_PASSWORD:}
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: ${FA_CLOUD_DB_MAX_POOL_SIZE:10}
      minimum-idle: ${FA_CLOUD_DB_MIN_IDLE:2}
      connection-timeout: ${FA_CLOUD_DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${FA_CLOUD_DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${FA_CLOUD_DB_MAX_LIFETIME:1800000}
      validation-timeout: ${FA_CLOUD_DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${FA_CLOUD_DB_LEAK_DETECTION_THRESHOLD:0}
      pool-name: fa-cloud
    model:
      url: ${FA_MODEL_DB_URL:jdbc:mysql://localhost:3306/fa-model?useSSL=false&allowPublicKeyRetrieval=true&preserveInstants=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8}
      username: ${FA_MODEL_DB_USERNAME:root}
      password: ${FA_MODEL_DB_PASSWORD:}
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: ${FA_MODEL_DB_MAX_POOL_SIZE:10}
      minimum-idle: ${FA_MODEL_DB_MIN_IDLE:2}
      connection-timeout: ${FA_MODEL_DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${FA_MODEL_DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${FA_MODEL_DB_MAX_LIFETIME:1800000}
      validation-timeout: ${FA_MODEL_DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${FA_MODEL_DB_LEAK_DETECTION_THRESHOLD:0}
      pool-name: fa-model
```

示例中的账号密码可以通过环境变量传入明文或完整的 `ENC(...)` 密文。`ENC(...)` 中的省略号不是有效密文，不可直接作为配置使用。

`cloud`、`model` 各自支持的完整连接池参数：

| 字段 | Java 默认值 | 说明 |
| --- | --- | --- |
| `url` | 无 | JDBC URL，需指定实际数据库。 |
| `username` / `password` | 无 | 连接账号、密码，可使用 `ENC(...)`。 |
| `driver-class-name` | `com.mysql.cj.jdbc.Driver` | JDBC 驱动类。 |
| `maximum-pool-size` | `10` | 最大连接数。 |
| `minimum-idle` | `2` | 最小空闲连接数。 |
| `connection-timeout` | `30000` | 等待获取连接的超时，毫秒。 |
| `idle-timeout` | `600000` | 空闲连接超时，毫秒。 |
| `max-lifetime` | `1800000` | 连接最大存活时间，毫秒。 |
| `validation-timeout` | `5000` | 连接验证超时，毫秒。 |
| `leak-detection-threshold` | `0` | 连接泄漏检测阈值，毫秒，`0` 表示关闭。 |
| `pool-name` | 按库回退 | 未填写时分别使用 `fa-cloud`、`fa-model`。 |

公共配置没有 `demo.datasource.enabled` 开关；即使服务不直接执行 SQL，引入后仍会按自动装配条件创建两组数据源。使用自定义数据源时，需保持 Bean 名和事务管理器的绑定一致。

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
@Service
public class DemoQueryService {

    private final JdbcTemplate faModelJdbcTemplate;

    public DemoQueryService(
            @Qualifier(MultiDataSourceNames.FA_MODEL_JDBC_TEMPLATE) JdbcTemplate faModelJdbcTemplate) {
        this.faModelJdbcTemplate = faModelJdbcTemplate;
    }
}
```

事务示例：

```java
@Transactional(transactionManager = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
public void updateModelData() {
    // 使用 fa-model 数据源的 DAO/JdbcTemplate。
}
```

两个事务管理器各自管理一个数据源。选择 `FA_MODEL_TRANSACTION_MANAGER` 不会使 `fa-cloud` 的操作自动加入同一个事务；跨库操作需要由业务代码明确设计事务边界。

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
String encrypted = EncryptedDataSourceValues.encrypt(
        "root",
        "cloud-family-demo-datasource-crypto-secret"
);
```

注意事项：

- `ENC(...)` 每次生成都会使用随机 IV，同一明文多次加密得到不同密文是正常现象。
- 只加密用户名和密码，不要加密完整 JDBC URL。
- 修改 `DATASOURCE_CRYPTO_SECRET` 后，已有密文必须用新密钥重新生成。
- 生产环境必须覆盖默认密钥。

## Redis Cluster 自动配置

类：`com.kfpd.cloud.common.config.redis.RedissonAutoConfiguration`

启用条件：

- classpath 中存在 `org.redisson.api.RedissonClient`
- 配置了 `spring.data.redis.cluster.nodes`
- `demo.redisson.enabled=true`，或没有显式设置该开关

配置满足后，每个 Bean 再分别按缺失条件创建。例如已有自定义 `RedissonClient` 时，会复用该客户端创建其他尚未声明的 Bean。

配置示例：

```yaml
demo:
  redisson:
    enabled: true

spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:123456}
      cluster:
        nodes: ${REDIS_CLUSTER_NODES:127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002}
```

自动创建的 Bean：

| Bean | 说明 |
| --- | --- |
| `RedissonClient` | 阻塞式 Redisson 客户端。 |
| `RedissonReactiveClient` | Reactive Redisson 客户端。 |
| `redisTemplate` | key/hash key 使用 String，value/hash value 使用 Jackson JSON。 |
| `RedissonService` | 基于 Redisson 和 RedisTemplate 的常用操作封装。 |

`nodes` 可以写 `host:port`，自动补成 `redis://host:port`；也可以显式写 `redis://` 或 `rediss://`。

当前节点配置按逗号分隔的属性值读取。`RedisTemplate` 还依赖 Spring 提供的 `RedisConnectionFactory` 和公共 Jackson Mapper。`demo.redisson.enabled=false` 会关闭本模块的整组 Redisson 自动配置，但不等同于关闭 Spring 自身的 Redis 自动配置。gateway 的 dev 配置默认将此开关设为 `false`；其他服务以各自配置为准。

## RedissonService

类：`com.kfpd.cloud.common.config.redis.RedissonService`

当前封装的操作：

| 方法 | 说明 |
| --- | --- |
| `get` / `set` | 读写普通 key/value。 |
| `set(key, value, time)` | 写入普通 key/value 并设置秒级 TTL。 |
| `setIfAbsent(key, value, time, unit)` | 仅 key 不存在时写入，同时设置 TTL。 |
| `get(key, TypeReference<T>)` | 读取并转换为指定类型，适用于泛型集合等。 |
| `setBytes` / `getBytes` | 使用 `ByteArrayCodec` 写入和读取原始字节。 |
| `hasKey` / `del` / `expire` | 判断、删除、设置过期时间。 |
| `getKeys` / `delByPattern` | 按 Redis key 模式查询、删除。 |
| `getExpire` / `getMillisecondExpire` | 获取剩余 TTL，分别返回秒、毫秒。 |
| `hGet` / `hSet` / `hGetAll` / `hDel` | Hash 常用操作。 |
| `sAdd` / `sMembers` / `sRemove` | Set 常用操作。 |
| `lAdd` / `lReadAll` / `lRemove` | List 常用操作。 |
| `zsetAdd` / `zsetGet` / `zsetRemoveMember` | Sorted Set 常用操作。 |
| `incrAndGet` / `decrAndGet` / `incr` / `decr` | 原子计数器操作。 |
| `tryLock` / `lock` / `unlock` / `execute` / `tryExecute` | 分布式锁操作。 |

锁配置前缀：

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
redissonService.execute("rebuild-stats:" + userId, () -> {
    // 需要互斥执行的业务逻辑。
});
```

缓存示例：

```java
redissonService.set("demo:user:42", Map.of("username", "alice"), 5, TimeUnit.MINUTES);
Map<String, String> user = redissonService.get(
        "demo:user:42", new TypeReference<Map<String, String>>() {});

redissonService.setBytes("demo:payload:42", new byte[] {1, 2, 3}, 60);
byte[] payload = redissonService.getBytes("demo:payload:42");
```

示例中的 `Map`、`TimeUnit`、`TypeReference` 分别来自 `java.util`、`java.util.concurrent`、`com.fasterxml.jackson.core.type`。

缓存与锁的具体行为：

- `set`、`expire` 等未指定单位的 TTL 重载默认使用秒；`expire` 的时间小于等于 `0` 时删除 key。
- TTL 查询对不存在或没有有效过期时间的 key 返回 `0`，不直接返回 Redis 的负数标记。
- 普通单 key `get` / `set` 走 Redisson 默认 codec；`get(List<String>)` 走 `RedisTemplate` 的 JSON 序列化路径。同一组 key 应使用一致的读写路径；`setByte` 也是默认 codec 的对象存储，原始字节应使用 `setBytes` / `getBytes`。
- `execute` / `tryExecute` 自动拼接 `key-prefix` 并在 `finally` 中解锁；`lock` / `tryLock` / `unlock` 直接使用传入的完整 key。
- `execute` 获取锁失败时抛出 `IllegalStateException`；`tryExecute` 获取失败时返回 `Optional.empty()`，业务回调自身抛出的异常仍会向外传播。回调返回 `null` 时也会得到空 Optional。
- `execute` / `tryExecute` 默认显式设置 30 秒租约；业务执行时间需要匹配租约。原始 `lock(key)` 不指定租约，续期行为与显式租约重载不同。

带显式等待时间和租约的示例：

```java
Optional<String> result = redissonService.tryExecute(
        "rebuild-stats:42", 2, 60, TimeUnit.SECONDS,
        () -> "done");
```

## RocketMQ 自动配置

类：

- `com.kfpd.cloud.common.config.rocketmq.RocketMqAutoConfiguration`
- `com.kfpd.cloud.common.config.rocketmq.RocketMqProperties`
- `com.kfpd.cloud.common.config.rocketmq.RocketMqService`

启用条件：

- classpath 中存在 `org.apache.rocketmq.client.producer.MQProducer`
- `demo.rocketmq.enabled=true`
- 当前容器中没有自定义 `DefaultMQProducer` 或 `RocketMqService`

默认关闭，避免没有本地 broker 时服务启动失败。

配置示例：

```yaml
demo:
  rocketmq:
    enabled: ${ROCKETMQ_ENABLED:false}
    namesrv-addr: ${ROCKETMQ_NAMESRV_ADDR:localhost:9876}
    producer-group: ${ROCKETMQ_PRODUCER_GROUP:cloud-family-demo-producer}
    consumer-group: ${ROCKETMQ_CONSUMER_GROUP:cloud-family-demo-consumer}
    send-message-timeout-millis: ${ROCKETMQ_SEND_TIMEOUT_MS:3000}
    retry-times-when-send-failed: ${ROCKETMQ_RETRY_TIMES:2}
    consume-thread-min: ${ROCKETMQ_CONSUME_THREAD_MIN:1}
    consume-thread-max: ${ROCKETMQ_CONSUME_THREAD_MAX:4}
    consume-message-batch-max-size: ${ROCKETMQ_CONSUME_BATCH_SIZE:1}
```

字段说明：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 是否启用 producer 和 `RocketMqService`。 |
| `namesrv-addr` | `localhost:9876` | RocketMQ NameServer 地址。 |
| `producer-group` | `cloud-family-demo-producer` | 默认 producer group。 |
| `consumer-group` | `cloud-family-demo-consumer` | `RocketMqService.subscribe` 默认 consumer group。 |
| `send-message-timeout-millis` | `3000` | 发送超时时间。 |
| `retry-times-when-send-failed` | `2` | 同步发送失败重试次数。 |
| `consume-thread-min` | `1` | push consumer 最小消费线程数。 |
| `consume-thread-max` | `4` | push consumer 最大消费线程数。 |
| `consume-message-batch-max-size` | `1` | 消费监听器一次处理的最大消息条数。 |

`RocketMqService` 支持：

- `syncSend(...)`
- `asyncSend(...)`
- `oneWaySend(...)`
- `subscribe(...)`
- `shutdown()`

收发示例（`rocketMqService` 通过构造器注入）：

```java
// 同步发送：topic、tag、业务 key、消息正文
rocketMqService.syncSend("demo-topic", "created", "order-42", "{\"id\":42}");

// 使用指定 consumer group 订阅；启动时注册一次
rocketMqService.subscribe("demo-created-consumer", "demo-topic", "created", (messages, context) -> {
    for (var message : messages) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        // 在此处理 body；处理失败时按业务需要返回 RECONSUME_LATER。
    }
    return org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
});
```

发送方法默认使用 UTF-8，也提供 `Charset` 重载；异步发送要求传入非空 `SendCallback`。启用自动配置只启动 producer，consumer 在业务代码调用 `subscribe` 时创建和启动；关闭容器时，`RocketMqService.shutdown()` 关闭已登记的 consumers，producer 由自己的 Bean 销毁回调关闭。发送及订阅涉及的检查异常由业务调用方处理。

## Redis Key 工具

类：

- `com.kfpd.cloud.common.constant.RedisKeyConsts`
- `com.kfpd.cloud.common.utils.RedisKeyUtil`

用途：

- 统一维护 Redis key 前缀常量。
- 提供 key 拼装方法，减少业务服务中手写字符串。

示例：

```java
String key = RedisKeyUtil.redisKeyDemo(1);
// DEMO:KEY:1

String orderKey = RedisKeyConsts.getGeneratorOrderKey("demo:order", "42");
// DEMO:ORDER:42
```

工具方法会将结果转为大写。`getJoinKey(String... keys)` 跳过空白片段并直接连接，不会自动添加分隔符；`getJoinKey(separator, keys)` 的当前实现适用于非空有效片段和单字符分隔符。

## OAuth2 授权表

初始化脚本：

```text
common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

执行示例：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

脚本创建：

| 表 | 说明 |
| --- | --- |
| `oauth2_registered_client` | OAuth2 client 注册信息，`token_settings` 中保存 access/refresh token TTL。 |
| `oauth2_authorization` | access token、refresh token、授权属性等运行期授权数据。 |
| `oauth2_authorization_consent` | 授权同意记录。 |

`auth-service` 启动时会初始化 demo client。

此脚本由运维或开发者手动执行，公共自动配置不会自动建表。OAuth2 表对应的 Entity 位于 `auth-service`；其他业务表的实体由实际使用该表的服务维护，`common-service` 本身没有业务 DAO 或 Entity。

## 环境变量

本模块直接绑定的是 `demo.*`、`spring.data.redis.*` 配置属性。下面的简写环境变量通过各服务 `application-dev.yml`、`application-test.yml` 或本文示例中的 `${...}` 映射；复制配置时须保留对应占位符。仅定义一个任意名称的环境变量不会自动建立这种映射。

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
| `GATEWAY_REDISSON_ENABLED` | gateway dev 配置中的 Redisson 开关，默认 `false`。 |

RocketMQ 变量：

| 变量 | 说明 |
| --- | --- |
| `ROCKETMQ_ENABLED` | 是否启用 RocketMQ，默认 `false`。 |
| `ROCKETMQ_NAMESRV_ADDR` | NameServer 地址。 |
| `ROCKETMQ_PRODUCER_GROUP` | producer group。 |
| `ROCKETMQ_CONSUMER_GROUP` | consumer group。 |

发送超时、重试次数和消费线程数可按 RocketMQ 配置示例增加环境变量占位符；未配置时使用 `RocketMqProperties` 的默认值。

JWT 变量：

| 变量 | 说明 |
| --- | --- |
| `JWT_ISSUER` | JWT issuer。 |
| `JWT_SECRET` | JWT HS256 签名密钥。 |

## 当前服务使用方式

| 服务 | common-service 使用点 |
| --- | --- |
| `gateway-service` | JWT secret、统一错误响应、网关错误码、Header 常量。 |
| `auth-service` | JWT 配置、双数据源、OAuth2 JDBC、Redis 会话、统一异常。 |
| `partner-service` | `fa-cloud` 数据源、Redis、统一响应和异常、商家申请及商品类型校验。 |
| `manager-service` | `fa-cloud` / `fa-model` 双数据源、Redis、RocketMQ、统一响应和异常、商家审核及共用类型策略。 |
| `job-service` | `fa-cloud` 过期行程更新、`fa-model` 登录日志读取和统计写入、双事务管理器。 |

## 本地初始化

初始化 OAuth2 授权表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
```

初始化 partner VIP 用户表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < partner-service/src/main/resources/db/fa-cloud/vip-user-schema.sql
```

初始化 manager 系统管理表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < manager-service/src/main/resources/db/fa-cloud/system-management-schema.sql
```

初始化个人中心功能表、登录日志和操作日志表（登录日志脚本先创建 `fa-model` 数据库）：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < partner-service/src/main/resources/db/fa-cloud/user-profile-features.sql
mysql -h127.0.0.1 -P3306 -uroot -p < auth-service/src/main/resources/db/fa-model/auth-login-log-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p fa-model < manager-service/src/main/resources/db/fa-model/operation-log-schema.sql
```

启用登录统计任务时，继续初始化对应统计表：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < job-service/src/main/resources/db/fa-model/manager-login-stats-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < job-service/src/main/resources/db/fa-model/partner-login-stats-schema.sql
```

脚本中的库名是 `fa-cloud`、`fa-model`。如使用 test 环境的 `fa-cloud-test`、`fa-model-test`，应将脚本内的建库及 `USE` 语句和连接目标同步调整。数据库脚本执行顺序及菜单增量脚本另见[项目说明](../README.md)。

## 验证命令

只验证 `common-service`：

```bash
mvn -pl common-service test
```

编译依赖公共模块的主要服务：

```bash
mvn -pl common-service,gateway-service,auth-service,partner-service,manager-service,job-service -am compile
```

完整测试：

```bash
mvn test
```

当前单元测试覆盖：

| 测试类 | 覆盖点 |
| --- | --- |
| `EncryptedDataSourceValuesTests` | `ENC(...)` 密文解密、明文透传、加密后可解密。 |
| `CommerceRulesTests` | 商品价格及状态约束、字段白名单、文本和封面等输入校验。 |
| `CommerceTypePolicyTests` | 类型编码、经营范围分配、商品类型归属与服务端展示名称。 |

这些单元测试不验证 MySQL、Redis 或 RocketMQ 的连接。基础设施联调需要启动相应组件和业务服务，并核对当前激活的 profile。

## 常见问题

| 现象 | 核查位置与处理方式 |
| --- | --- |
| `Failed to decrypt datasource value` | 核对 `demo.datasource.crypto.secret` 与生成密文时的密钥一致，并确认配置中是完整 `ENC(...)`。 |
| JDBC URL、数据库或数据表不可用 | 核对两组 `demo.datasource.*` 连接属性、当前 profile 和初始化脚本执行结果。公共模块不负责自动执行 SQL。 |
| 找不到 `RedissonService` | 核对 `demo.redisson.enabled`、`spring.data.redis.cluster.nodes` 和业务模块是否加载公共自动配置。gateway 默认关闭该组配置。 |
| Redis 已有 key 无法按预期反序列化 | 核对写入和读取是否使用同一 codec，尤其是 Redisson 单 key 路径与 RedisTemplate 批量读取路径。 |
| `Failed to acquire redis lock` | `execute` 未在等待时间内获得锁；核对业务锁名、持有者及等待/租约配置，也可按业务需要使用 `tryExecute` 处理获取失败。 |
| 没有 `RocketMqService` Bean | 默认关闭；检查 `demo.rocketmq.enabled=true`、NameServer 连接和自定义 producer Bean。 |
| RocketMQ producer 已启动但未消费消息 | 核对业务代码是否调用 `subscribe`，以及 consumer group、topic、tag 是否与发送端匹配。 |

## 源码入口

- [商家与商品校验](src/main/java/com/kfpd/cloud/common/showcase/CommerceRules.java)、[类型策略](src/main/java/com/kfpd/cloud/common/showcase/CommerceTypePolicy.java)、[持久化契约](src/main/java/com/kfpd/cloud/common/showcase/CommerceTypeStore.java)。

- [数据源自动配置](src/main/java/com/kfpd/cloud/common/config/datasource/MultiDataSourceAutoConfiguration.java)、[连接池参数](src/main/java/com/kfpd/cloud/common/config/datasource/MultiDataSourceProperties.java)、[密文工具](src/main/java/com/kfpd/cloud/common/config/datasource/EncryptedDataSourceValues.java)。
- [Redis 自动配置](src/main/java/com/kfpd/cloud/common/config/redis/RedissonAutoConfiguration.java)、[缓存和锁方法](src/main/java/com/kfpd/cloud/common/config/redis/RedissonService.java)。
- [RocketMQ 自动配置](src/main/java/com/kfpd/cloud/common/config/rocketmq/RocketMqAutoConfiguration.java)、[收发方法](src/main/java/com/kfpd/cloud/common/config/rocketmq/RocketMqService.java)。
- [公共响应](src/main/java/com/kfpd/cloud/common/web/ApiResponse.java)、[错误码](src/main/java/com/kfpd/cloud/common/exception/ErrorCode.java)、[网关 Header](src/main/java/com/kfpd/cloud/common/web/GatewayHeaders.java)。
- [自动配置注册清单](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)、[OAuth2 建表脚本](src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql)。

## 维护约定

- 新增公共错误码时，先在 `ErrorCode` 中分配清晰模块区间，再由各服务异常处理器统一转换为 `ApiResponse`。
- 新增跨服务 Header 时，统一放入 `GatewayHeaders`，避免各服务手写字符串。
- 新增共享配置类时，优先使用 `@ConfigurationProperties`，并在 README 中补充绑定前缀和字段说明。
- 新增自动配置类时，必须写入 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 新增默认 Bean 时，优先使用 `@ConditionalOnMissingBean`，让业务服务可以覆盖公共默认实现。
- `common-service` 不依赖业务服务模块；业务逻辑不要下沉到本模块。
- 数据库脚本按逻辑库放置，例如 `db/fa-cloud/`、`db/fa-model/`。

## 生产化注意点

- 替换默认 JWT secret 和数据源密文 secret。
- 生产密码不要直接提交到配置文件，优先使用环境变量、配置中心或密钥管理系统。
- 限制下游服务只允许网关访问，防止伪造 `X-User-*` Header。
- 根据实际连接数调整 Hikari 连接池参数，避免多服务同时启动压垮数据库。
- RocketMQ 默认关闭；启用前确认 broker、topic、consumer group 和重试策略。
- 将 OAuth2 client、token TTL、scope 等安全参数纳入数据库变更流程。
- 如果需要密钥轮换或多调用方验签，建议从 HS256 切换到 RSA/ECDSA 并暴露 JWK Set。
