# 商家类型与商品类型约束

后台新增 Partner Management → 商家类型管理。类型包含唯一业务编码 type_code、展示名称（最多32字，允许重名）、说明、排序和启用状态，拥有独立查询、新增、修改、删除权限。

## 业务规则

- 一个商家至少选择1个、最多20个已启用类型；创建、编辑店铺资料时均必选，可多选。
- 商品必须指定 typeCode，且该类型属于当前商家已选范围，同时处于启用状态。商品名称、价格、库存等规则保持不变。
- 商品 category 展示名称由服务端按类型编码确定，客户端填写的 category 不能改变类型归属。
- 编码规范为大写，以字母开头，仅含字母、数字和下划线，1至64位，创建后不可修改。名称只展示，不作为关联或业务匹配依据。
- 移除商家类型时，已上架且不再属于经营范围的商品自动下架。商品记录保留，可查看后换为有效类型再上架。
- 后台停用类型时，相关已上架商品自动下架。恢复类型不会自动上架；商家需手动操作。
- 类型改名时同步更新商品展示名称。类型被商家或商品引用时禁止删除，管理员可选择停用。
- 原有“系统停用”仍由后台独立控制，商家改类型不能解除系统停用。
- 后端校验在数据库事务内完成，更新商品和修改商家类型时锁定商家及类型关联。公开商品查询也校验类型有效性。

## 页面

- 商家类型管理：字典新增、编辑、启停、删除及批量删除。
- 后台商家管理：勾选多个经营类型，列表展示已选类型名称；已停用类型可取消选择。
- 后台商品管理：选择所属商家后，商品类型选项仅显示该商家已选且启用的类型。
- 用户“我的店铺与商品”：店铺创建/编辑时勾选类型；商品新增/编辑使用受限下拉框，不能自由填写类型。类型失效时提示重新选择。
- 精品展示的商家卡片和商家资料展示经营类型。

## 表结构

| 表/字段 | 用途与约束 |
| --- | --- |
| showcase_shop_type | id内部主键、type_code业务唯一且非空、name展示名称允许重名、description、status（ENABLED/DISABLED）、sort_order、created_at、updated_at |
| showcase_shop_type_rel | shop_id/type_code 联合主键，按 type_code 逻辑关联类型字典；包含 (type_code,shop_id) 索引；shop删除时业务代码清理关联，类型编码更新与删除受引用限制 |
| showcase_product.type_id | 内部ID逻辑关联字典；业务使用 type_code，旧 category 字段仅作为展示名称同步维护 |

完整建表及迁移脚本：`manager-service/src/main/resources/db/fa-cloud/showcase-shop-types.sql`。

迁移自动将历史商品 category 转为类型，补齐商家经营类型、商品类型ID；没有商品的历史店铺分配“综合”，可之后修改。迁移不覆盖已配置类型，不自动重新上架任何商品。脚本可重复执行，需在已有 showcase_shop/showcase_product 表基础上执行。

## 接口变更

- `POST /api/users/commerce/types`：已启用的商家类型。
- `POST /api/users/commerce/mine/shop/save`：必填 `typeCodes: string[]`，商家关联不再接受 typeIds。
- 用户和后台商品写入：必填 `typeCode`，兼容旧客户端的内部 typeId，提供编码时优先使用编码。商品 category 由后端推导。
- 商家返回 `types`（含 typeCode）、`typeCodes` 和展示字段 `typeNames`，不再返回关联字段 typeIds。
- 商品列表按 `typeCode` 筛选，不再按 category 展示名称匹配；分类选项返回 `{typeCode,name}`，同名类型不会合并。
- 后台字典沿用 `/api/manager/partner/features/commerce-types`，支持 `/page`、新增根路径、`/update/{id}`、`/delete/{id}`。
- `POST /api/manager/partner/shop-types/options`：不传shopId时返回全部启用类型，传shopId时返回该商家已选且启用的类型，供后台编辑弹窗使用。

## 初始化与授权

1. 执行 showcase-shop-types.sql。
2. 执行 showcase-shop-types-menu.sql，初始化菜单、按钮及 `partner:shop-type:view/add/edit/delete` 权限。
3. 先前已安装类型管理的环境执行增量脚本 showcase-shop-type-code.sql：按原ID回填 TYPE_<id>，新增编码非空及唯一约束，移除名称唯一约束。本地已执行。
4. 先前使用 type_id 商家关联的环境执行 showcase-shop-type-rel-code.sql：通过旧ID找到编码回填，移除旧外键并替换主键、索引，删除关联表的 type_id 列；本地已迁移，原有关联数量保持一致。重复执行不会重复创建关联。
5. 重启 partner-service、manager-service，更新两端前端；后台重新登录刷新权限。关联表迁移需要与两个业务服务的新版本同时发布。

本地 fa-cloud 已执行这两个脚本。SUPER_ADMIN、MANAGER 已授予类型管理菜单及接口权限。其他角色通过角色管理分配。

## 验证

- 两端前端构建。
- Maven 自动测试：类型必选、去重、停用类型拒绝、类型归属校验、类型名称不可伪造、商家类型返回、独立权限及引用删除保护。
- 真实HTTP回归：14组场景，覆盖唯一且不可修改的编码、同名类型、改名后按编码筛选，以及商家多选类型、空类型拒绝、商品越界类型、类型移除和停用、越权、系统停用及商品删除。另用临时数据库验证新安装、重复迁移和旧数据回填。测试数据和临时服务均清理。
