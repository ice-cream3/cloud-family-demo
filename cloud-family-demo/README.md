# cloud-family-demo

JDK 21 + Spring Boot 4.0.8 + Spring Cloud 2025.1.3 microservice demo.

## Modules

- `common-service`: shared configuration, responses, exceptions, Redis/RocketMQ utilities, dual data sources, and commerce validation. See [common-service/README.MD](common-service/README.MD).
- `gateway-service`: Spring Cloud Gateway WebFlux gateway on port `38080`.
- `auth-service`: JWT issuing service on port `38081`.
- `partner-service`: protected partner API service on port `38082`.
- `manager-service`: protected manager API service on port `38083`.
- `job-service`: XXL-JOB executor service on HTTP port `38084` and executor port `39999`.
- `frontend`: standalone React frontend on port `35173`, separated from backend services.
- `mobile-frontend`: standalone responsive partner frontend for PC Web and H5 on port `35175`.

System users, roles, permissions, menus, and their relationships are manager-service features backed by the `fa-cloud` MySQL database. API user profile data is stored in `fa-cloud.vip_user`.

## Features

- Gateway-first API access with protected API and manager routes.
- API user and manager user login flows with JWT access and refresh tokens.
- Local JWT validation before protected requests are routed to backend services.
- Manager dashboard and system management APIs for users, roles, permissions, and menus.
- Lightweight XXL-JOB admin, executor registration, manual trigger APIs, and demo job handlers.
- Shared common module for cross-service configuration, constants, and infrastructure setup.
- Merchant applications and manager review, merchant type management, product publishing and moderation.
- Merchant discovery by enabled type and keyword, with server-side pagination.
- Membership configuration with permission-controlled account password reset and operation logs.

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

## 环境与配置

所有后端构建命令在仓库根目录执行，前端命令在对应前端目录执行。

| 依赖 | 用途与配置入口 |
| --- | --- |
| JDK 21、Maven | 根 `pom.xml` 定义 Java 目标版本及后端依赖版本。 |
| Node.js、npm | 两个 React/Vite 应用独立安装、构建；使用符合各前端依赖要求的 Node.js 版本。 |
| MySQL 8 | `fa-cloud` 保存账号、权限及业务数据，`fa-model` 保存登录日志、操作日志和统计。 |
| Redis Cluster | 各服务按 `spring.data.redis.cluster.nodes` 配置；公共客户端启用条件见 common README。 |
| RocketMQ（按需） | 默认关闭，使用消息能力时配置 NameServer 并启用 `ROCKETMQ_ENABLED`。 |
| XXL-JOB Admin（按需） | 调度 job-service；执行器默认指向本地 `38088/xxl-job-admin`。 |

默认 profile 为 `dev`，服务配置位于各模块 `src/main/resources/application.yml` 和 `application-dev.yml`；测试连接配置位于 `application-test.yml`。用 `SPRING_PROFILES_ACTIVE` 切换环境。

- 数据源使用 `FA_CLOUD_DB_URL/USERNAME/PASSWORD`、`FA_MODEL_DB_URL/USERNAME/PASSWORD`；用户名和密码支持明文及 `ENC(...)`，密钥由 `DATASOURCE_CRYPTO_SECRET` 提供。
- Redis 使用 `REDIS_CLUSTER_NODES`、`REDIS_PASSWORD`；会话存储可用 `AUTH_SESSION_STORE=redis` 切换。
- auth 与 gateway 的 JWT 配置必须一致；网关与下游的 `demo.gateway.internal-token` 也必须一致。
- 两组数据源均由公共模块创建。配置、Bean 名、事务边界和环境变量详见 [common README](common-service/README.MD)。

## 数据库初始化与升级

SQL 脚本由开发者执行，公共模块不会在启动时自动迁移。新环境先创建基础表，再初始化功能和菜单。以下为基础初始化顺序（连接账号按实际环境替换）：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < partner-service/src/main/resources/db/fa-cloud/vip-user-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < manager-service/src/main/resources/db/fa-cloud/system-management-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < common-service/src/main/resources/db/fa-cloud/spring-security-oauth2-authorization-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < partner-service/src/main/resources/db/fa-cloud/user-profile-features.sql
mysql -h127.0.0.1 -P3306 -uroot -p < auth-service/src/main/resources/db/fa-model/auth-login-log-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p fa-model < manager-service/src/main/resources/db/fa-model/operation-log-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p < manager-service/src/main/resources/db/fa-cloud/partner-management-menu.sql
```

商家表、类型关联和功能菜单按下文及专题文档继续初始化；登录统计表按任务章节初始化。旧环境应根据已有表结构选择增量脚本，不能依靠重新执行 `CREATE TABLE IF NOT EXISTS` 更新已有约束。脚本中的 `USE fa-cloud` / `USE fa-model` 不会随 Spring profile 自动变化，测试库执行前需核对脚本的目标库。

## Build

```bash
mvn clean package
```

Build each frontend separately (the Maven build does not build frontend assets):

```bash
cd frontend
npm install
npm run build
```

```bash
cd mobile-frontend
npm ci
npm run build
```

## Run

Install the shared module once before starting individual services from the Maven reactor:

```bash
mvn -pl common-service -am install
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

Start the independent partner mobile frontend in another terminal:

```bash
cd mobile-frontend
npm install
npm run dev
```

The partner mobile frontend runs at `http://localhost:35175` and uses the same gateway proxy.

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
- `tripExpirationJobHandler`: changes ended `OPEN`, `PENDING`, and `BOOKED` trip slots in `fa-cloud.user_trip_slot` to `EXPIRED`, using the database date/time. Existing `NO_SHOW` records are preserved. The task is safe to retry; already expired rows are skipped.

For `tripExpirationJobHandler`, create an XXL-JOB task with handler `tripExpirationJobHandler`, cron `0 * * * * ?` (every minute), no job parameters, and a single-executor route such as `FIRST` with serial blocking. Enable the task in XXL-JOB Admin after deploying job-service. The local lightweight admin supports manual triggering only. This job uses the existing `faCloudJdbcTemplate` and `faCloudTransactionManager`; no additional table or status-column migration is required. Applicant and review information remain available, including no-show reporting for previously approved bookings after expiration.

For `managerLoginStats10mJobHandler`, use a 10-minute cron such as `0 0/10 * * * ?`.
Without a job param it calculates the previous complete 10-minute window. To recalculate
a specific window, pass the window end as an ISO timestamp, for example `2026-09-13T10:20:00Z`.
For `partnerLoginStats5mJobHandler`, use a 5-minute cron such as `0 0/5 * * * ?`.
Create the source login log tables with `auth-service/src/main/resources/db/fa-model/auth-login-log-schema.sql`
and the target stats tables with `job-service/src/main/resources/db/fa-model/manager-login-stats-schema.sql`
and `job-service/src/main/resources/db/fa-model/partner-login-stats-schema.sql`.

## Try

Request an API challenge first and complete it in the user frontend. Login and registration require the challenge ID and answer; the challenge endpoint also accepts `{"purpose":"api_register"}` for registration.

```bash
curl -s -X POST http://localhost:38080/auth/api/login/challenge
```

Login as an API user through the gateway:

```bash
curl -s -X POST http://localhost:38080/auth/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"demo","antiAbuseChallengeId":"<challengeId>","antiAbuseAnswer":"<answer>"}'
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

Configure the enabled login challenge schemes:

Configure `mobile-frontend` login/registration and `frontend` manager login independently. An empty effective scheme list randomly selects from all five schemes; configuring one side does not affect the other. The current application.yml defaults API challenges to all schemes and manager challenges to graphic_set. Configure `auth-service/src/main/resources/application.yml`:

```yaml
demo:
  auth:
    login:
      api: # mobile-frontend login and registration
        enabled-challenge-schemes:
          - image_slider
          - image_reassemble
      manager: # frontend manager login
        enabled-challenge-schemes:
          - graphic_sequence
          - image_slider
```

You can also set the comma-separated environment variables `AUTH_API_LOGIN_CHALLENGE_SCHEMES` and `AUTH_MANAGER_LOGIN_CHALLENGE_SCHEMES` independently using the default configuration. These replace the previous shared `demo.auth.login.enabled-challenge-schemes` / `AUTH_LOGIN_CHALLENGE_SCHEMES` setting. A single value fixes the challenge type. Supported values are `graphic_set` (select three shapes), `graphic_sequence` (select three in order), `graphic_pair` (select two shapes), `image_reassemble` (swap four tiles), and `image_slider` (slider puzzle). Unknown values fail configuration binding at startup. Restart auth-service after changing the configuration.

Request a manager login challenge first (the response expires after three minutes and can be used once):

```bash
curl -s -X POST http://localhost:38080/auth/manager/login/challenge
```

Complete the returned image challenge in the management login page, then submit its ID and answer with the credentials. Graphic/puzzle answers are comma-separated option values; slider answers are horizontal offsets in the original image's pixels. Manager challenges are bound to the client IP and User-Agent and cannot be replaced with API login or registration challenges.

Login as a manager user:

```bash
curl -s -X POST http://localhost:38080/auth/manager/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager","password":"demo","antiAbuseChallengeId":"<challengeId>","antiAbuseAnswer":"<answer>"}'
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
| POST | `http://localhost:38080/auth/api/login/challenge` | API login or registration challenge (`{"purpose":"api_register"}` for registration) |
| POST | `http://localhost:38080/auth/api/login` | API user login |
| POST | `http://localhost:38080/auth/api/register` | API user registration |
| POST | `http://localhost:38080/auth/manager/login/challenge` | Create manager login image challenge |
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

## 商家申请与精品展示

商家、商品、经营类型由 partner-service 和 manager-service 共用 `common-service.showcase` 规则，账号登录仍由 auth-service 负责。

1. 后台维护商家类型字典，用户申请时必须选择 1–20 个已启用类型，可多选。
2. 提交后进入 `PENDING`，用户仅可查看；后台商家管理通过“审核”确认资料和类型，可通过或驳回。
3. `ACTIVE` 为审核通过且启用，只有该状态及启用账号的商家公开展示并允许新增商品（包括草稿）。`REJECTED` 可修改后重新申请，`BLOCKED` 由后台停用；用户不能修改已通过或停用商家的资料。
4. 商品类型必须属于商家经营范围且启用。移除或停用类型会下架相关商品，恢复类型不会自动上架。
5. 精品展示支持“全部商家”及启用类型筛选，可叠加关键词；切换类型回到第一页，列表和总数均由后端筛选。

| 接口（均为 POST，通过网关访问） | 用途 |
| --- | --- |
| `/api/users/commerce/types` | 查询已启用类型。 |
| `/api/users/commerce/shops/page` | 按 `keyword`、`typeCode`、`pageNum`、`pageSize` 查询商家；不传类型表示全部。 |
| `/api/users/commerce/mine/shop/save` | 提交申请或驳回后重提；资料包含 `name`、`description`、`contact`、`coverUrl`、必填 `typeCodes`。 |
| `/api/manager/partner/features/commerce-shops/page` | 后台商家列表，可按审核状态查询。 |
| `/api/manager/partner/features/commerce-shops/update/{id}` | 后台保存商家资料、审核状态及类型，要求 `partner:showcase:edit`。 |

后台菜单统一为 `Function Management → 商家管理 → 商家类型管理 / 商家管理 / 商品管理 / 精品展示`。执行 [function-commerce-menu.sql](manager-service/src/main/resources/db/fa-cloud/function-commerce-menu.sql) 迁移已有菜单，并为 `SUPER_ADMIN` 补齐四个页面、按钮和关联接口权限；该脚本在下列页面及类型菜单初始化后执行，可重复执行。页面路由保持不变，重新登录刷新菜单。

SQL 位于 `manager-service/src/main/resources/db/fa-cloud/`。新库依次执行 `showcase-schema.sql`、`showcase-menu.sql`（包含精品模块权限）、`showcase-commerce-schema.sql`、`showcase-shop-types.sql`、`showcase-commerce-menu.sql`、`showcase-shop-types-menu.sql`；菜单依赖基础系统管理表，旧内容模块说明见下列专题文档。

已有环境按实际结构补齐迁移：

| 脚本 | 适用变更 |
| --- | --- |
| `showcase-shop-type-code.sql` | 旧类型表增加稳定的唯一业务编码。 |
| `showcase-shop-type-rel-code.sql` | 旧商家类型关联由 `type_id` 迁移为 `type_code`。 |
| `showcase-commerce-moderation-reason.sql` | 旧商品表增加后台停用原因。 |
| `showcase-commerce-blocked-offline.sql` | 将系统停用商品统一置为下架。 |
| `showcase-shop-approval.sql` | 旧商家表状态约束加入 `PENDING/REJECTED`，默认状态改为待审核。 |

**旧库上线申请功能前必须执行审核状态迁移。** 否则保存申请会报 `Check constraint 'chk_showcase_shop_status' is violated`。迁移保留已有商家状态。部署后重启 partner-service、manager-service，并更新移动端及管理端。

## 通用业务字典

后台 `Function Management → 字典管理` 包含“字典类型”和“字典项”，用于配置各业务的编码、展示名称、说明、排序与启停。类型编码全局唯一，项编码在类型内唯一；编码创建后不可修改。业务端通过 `POST /api/users/dictionaries/{typeCode}/options` 读取启用选项，返回 `value/label`。

执行 [system-dictionary.sql](manager-service/src/main/resources/db/fa-cloud/system-dictionary.sql) 创建表和菜单，默认赋予 `SUPER_ADMIN` 查询、新增、修改、删除权限。各业务需显式接入该接口；商家类型接入 `MERCHANT_TYPE` 通用字典，随后执行 `merchant-type-dictionary.sql`。部署与接口示例见 [通用字典说明](docs/system-dictionary.md)。

## 会员中心重置密码

后台“会员中心配置”提供“重置会员密码”。接口为 `POST /api/manager/partner/features/memberships/password/reset/{会员记录ID}`，请求体为 `{"password":"新密码"}`，需要独立权限 `partner:membership:reset-password`。

服务端先按会员记录查找关联 VIP 账号，再复用密码重置服务和操作日志，不能将会员记录 ID 当成用户 ID。会员方案、权益和到期时间保持不变。

- 为 `SUPER_ADMIN` 补齐按钮和接口权限：执行 [membership-password-reset-super-admin.sql](manager-service/src/main/resources/db/fa-cloud/membership-password-reset-super-admin.sql)，不影响其他角色已有授权。
- 通用初始化脚本 [membership-password-reset-menu.sql](manager-service/src/main/resources/db/fa-cloud/membership-password-reset-menu.sql) 默认授权 `SUPER_ADMIN`、`MANAGER`；按所需授权范围选择脚本。
- 执行后重新登录后台刷新权限，详情见 [会员密码重置说明](docs/membership-password-reset.md)。

## 验证与排错

```bash
# 公共模块单元测试，无需启动中间件
mvn -pl common-service -am test

# 商家流程和会员重置密码的定向回归
mvn -pl partner-service,manager-service -am test -Dtest=CommerceServiceTests,MembershipPasswordTests -Dsurefire.failIfNoSpecifiedTests=false
```

完整后端测试使用 `mvn test`；两个前端分别执行 `npm run build`，需要检查风格时执行 `npm run lint`。单元测试及前端构建不能替代真实数据库约束和接口验证。

| 现象 | 检查方式 |
| --- | --- |
| 改码后仍返回旧校验提示 | 确认目标服务已重新构建并重启，核对实际监听端口与日志中的启动时间。 |
| 商家申请报状态约束错误 | 检查目标数据库是否执行 `showcase-shop-approval.sql`，不要只修改建表脚本。 |
| 商家或商品不展示 | 检查账号、商家审核状态、商品上下架与后台停用状态，以及类型是否启用且属于商家。 |
| 后台无按钮或返回 403 | 同时检查角色菜单和接口权限，授权后重新登录。 |
| 前端请求失败 | 前端 `/auth`、`/api` 代理到 gateway，查看 gateway 和目标业务服务日志；日志默认在 `logs/<服务名>/<profile>/`。 |

## 专题文档

- [公共模块配置与扩展](common-service/README.MD)
- [管理端使用说明](frontend/README.md)
- [用户端使用说明](mobile-frontend/README.md)


精品展示的功能、表结构、接口、权限和部署说明见 [docs/showcase.md](docs/showcase.md)。

商家与用户自营商品业务见 [docs/showcase-commerce.md](docs/showcase-commerce.md)，前台精品入口现按商家浏览商品。

商家类型、多类型经营范围和商品类型约束见 [docs/showcase-shop-types.md](docs/showcase-shop-types.md)。
