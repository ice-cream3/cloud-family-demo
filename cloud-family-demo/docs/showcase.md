# 精品展示（旧版内容）

用户店铺与商品的新业务见 [showcase-commerce.md](showcase-commerce.md)。移动端精品入口现采用“商家 → 商品”浏览，本文描述保留的旧版内容接口。

精品展示由管理端维护，移动端首页和工具中心提供入口。内容用于展示精选文章、作品、产品介绍等，无需修改代码即可新增分类和精品。

## 使用流程

1. 管理员登录后台，在 Partner Management → 精品展示中新增内容。
2. 填写标题、分类、摘要、正文；封面为可选图片 URL。排序默认为 0，数值越小越靠前，相同排序按 ID 倒序。
3. 新内容默认为草稿。状态改为“上架”并保存后，移动端即可浏览。
4. 下架后从前台列表、分类和详情接口同时隐藏。重新上架时刷新上架时间；编辑已上架内容会立即生效。
5. 支持标题/分类关键词、状态筛选，以及修改、单条删除、当前页批量删除。删除沿用管理端统一确认弹窗和操作日志。
6. 移动端支持标题/摘要搜索、分类筛选、分页、详情阅读、图片加载失败占位、错误重试与空状态。进入精品展示会写入当前用户最近使用。

分类直接填写名称，前台分类取已上架内容中的去重名称。正文保存为纯文本并保留换行，不执行 HTML。封面支持 http(s) 图片地址和以单个 `/` 开头的站内路径；本期不包含文件上传。

## 数据库设计

数据库：`fa-cloud`；表：`showcase_item`。管理端与用户端复用该内容表，避免内容同步。

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| id | bigint 自增主键 | 精品 ID |
| title | varchar(100) 非空 | 标题 |
| category | varchar(32) 非空 | 分类名称 |
| cover_url | varchar(1024) 可空 | 封面 URL |
| summary | varchar(300) 非空 | 列表摘要 |
| content | mediumtext 非空 | 详情正文，接口最多 100000 字符 |
| status | varchar(16) 非空 | DRAFT 草稿 / PUBLISHED 上架 / OFFLINE 下架 |
| sort_order | int 非空，默认 0 | 越小越靠前 |
| published_at | timestamp 可空 | 最近一次从非上架变为上架的时间 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 最近修改时间，数据库自动更新 |

索引：`(status, sort_order, id)` 用于已上架内容排序，`(category, status)` 用于分类筛选。数据库 CHECK 约束状态取值；后端校验必填、长度、图片地址、状态和整数排序。关键词使用参数绑定的 LIKE 查询。

建表脚本：`manager-service/src/main/resources/db/fa-cloud/showcase-schema.sql`。
菜单权限脚本：`manager-service/src/main/resources/db/fa-cloud/showcase-menu.sql`。

脚本均可重复执行，不写入示例精品，不清空已有数据。已有同名但结构不同的表需单独迁移，CREATE IF NOT EXISTS 不会覆盖已有表结构。

## 接口

统一使用 POST 和项目现有 ApiResponse 格式。所有请求通过网关并携带登录令牌。

| 接口 | 请求 | 说明 |
| --- | --- | --- |
| `/api/users/showcase/page` | JSON：pageNum、pageSize、category、keyword | 只返回上架内容；默认每页12条，上限100条；列表不含正文 |
| `/api/users/showcase/categories` | 无 | 已上架内容的分类 |
| `/api/users/showcase/detail/{id}` | 无 | 上架内容详情；不存在或下架返回404 |
| `/api/manager/partner/features/showcase/page` | 表单：pageNum、pageSize、keyword、status | 后台所有状态分页 |
| `/api/manager/partner/features/showcase` | JSON 内容字段 | 新增 |
| `/api/manager/partner/features/showcase/update/{id}` | JSON 内容字段 | 修改、上下架 |
| `/api/manager/partner/features/showcase/delete/{id}` | 无 | 删除；批量操作复用此接口并逐条报告结果 |

写入字段：title、category、coverUrl、summary、content、status、sortOrder。时间和主键由数据库维护。发布状态查询在 SQL 中约束，直接请求未上架 ID 也无法获取正文。

后台使用独立权限 `partner:showcase:view/add/edit/delete`；新增菜单和按钮绑定对应权限，后端用方法权限校验。初始化脚本为现有 SUPER_ADMIN、MANAGER 角色授权。其他角色由管理员通过已有角色菜单、角色权限功能配置。

## 部署

1. 在目标 `fa-cloud` 数据库依次执行 showcase-schema.sql、showcase-menu.sql。
2. 重启 manager-service、partner-service，更新 frontend、mobile-frontend。
3. 管理员退出后重新登录以刷新权限和菜单。
4. 在后台新增一条精品并设为“上架”，从移动端首页进入“精品展示”验证。

本地数据库已执行建表与菜单脚本。新增接口位于已有 `/api/users/**`、`/api/manager/**` 网关路由下，无需修改 auth-service 配置。

## 验证

- `npm run build`（frontend、mobile-frontend）。
- `mvn -pl manager-service,partner-service -am test`：输入约束、默认状态、修改删除失败、分页边界、详情不存在等单元测试。
- 本地 MySQL 使用真实 MyBatis mapper 做事务验证：草稿隐藏、上架读取、分类、搜索、分页、下架隐藏、后台筛选、删除，验证后回滚测试数据。
