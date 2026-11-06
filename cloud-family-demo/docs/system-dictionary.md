# 通用业务字典

后台菜单：`Function Management → 字典管理 → 字典类型 / 字典项`。

先创建业务类型，例如 `AFTER_SALE_REASON`（售后原因），再在字典项中选择该类型，配置 `QUALITY_ISSUE`（质量问题）、`WRONG_ITEM`（发错商品）等选项。业务保存字典项编码，页面用名称展示。新增字典不会自动改变已有业务表或业务校验；接入业务需显式查询字典并校验允许值。

## 字段与规则

| 字段 | 规则 |
| --- | --- |
| 类型编码 `typeCode` | 全局唯一，创建后不可修改。 |
| 字典项编码 `itemCode` | 同一类型内唯一，创建后不可修改；所属类型也不可修改。 |
| 名称 `name` | 必填，最多100字。 |
| 说明 `description` | 可选，最多500字。 |
| 排序 `sortOrder` | 非负整数，越小越靠前。 |
| 状态 `status` | `ENABLED` / `DISABLED`，支持后台启停。 |

编码会去除首尾空白并转大写，以字母开头，仅含字母、数字、下划线，最多64位。类型下有字典项时禁止删除，先删除项或停用类型。字典被任意业务数据引用的检查需由业务接入实现，已使用的字典项建议停用以保留历史编码。类型停用不会修改各字典项的原状态；重新启用类型后，其已启用项重新出现在业务选项中。

商家类型统一通过 `MERCHANT_TYPE` 字典项维护，保留商品经营范围、逻辑关联约束和自动下架规则。执行 `merchant-type-dictionary.sql`（在通用字典和商家类型建表脚本之后）迁移已有类型并补齐水果店、超市；重复执行不覆盖已有字典配置。字典启停、名称同步到商家类型；已被商家或商品引用的项禁止删除，请停用。frontend 和 mobile-frontend 均从商家类型接口获取同一份启用选项。

## 初始化与权限

在基础系统管理表已初始化后执行：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < manager-service/src/main/resources/db/fa-cloud/system-dictionary.sql
```

脚本创建 `sys_dict_type` / `sys_dict_item`、菜单、按钮及 `system:dict:view/add/edit/delete` 权限，默认赋予 `SUPER_ADMIN`。可重复执行，不覆盖已有字典数据。部署 manager-service、partner-service 和 frontend 后重新登录刷新权限。其他管理角色通过角色管理分配菜单及对应接口权限，修改或新增的角色也需要查询权限以加载类型选项。

## 接口

以下均为 POST，并通过 gateway 访问。后台列表使用表单或查询参数，写入使用 JSON。

| 路径 | 说明 |
| --- | --- |
| `/api/manager/system/dictionaries/{types或items}/page` | 分页，参数 `pageNum/pageSize/keyword/status`；关键词匹配类型编码、名称及字典项编码。 |
| `/api/manager/system/dictionaries/type-options` | 编辑字典项时加载所属类型。 |
| `/api/manager/system/dictionaries/{types或items}` | 新增。 |
| `/api/manager/system/dictionaries/{types或items}/update/{id}` | 修改名称、排序、说明及状态；提交原编码。 |
| `/api/manager/system/dictionaries/{types或items}/delete/{id}` | 删除，类型存在项时拒绝。 |
| `/api/users/dictionaries/{typeCode}/options` | 登录用户查询业务选项，仅返回类型和项均启用的记录。 |

业务选项返回统一 `ApiResponse`，`data` 示例：

```json
[
  {"value":"QUALITY_ISSUE","label":"质量问题","description":"商品质量异常","sortOrder":1}
]
```

空类型、不存在或停用的类型返回空列表。字典写入与删除记录到现有操作日志，业务模块 `SYSTEM`，业务类型 `DICTIONARY_TYPES` / `DICTIONARY_ITEMS`。

## 验证

```bash
mvn -pl manager-service -am test -Dtest=DictionaryServiceTests -Dsurefire.failIfNoSpecifiedTests=false
npm --prefix frontend run build
```

实际接口回归覆盖独立权限、类型和项新增、重复编码、编码不可修改、列表、类型和项启停、业务选项过滤、类型删除保护；临时测试字典在完成后删除。

已有字典表如需补齐字段描述，执行 `manager-service/src/main/resources/db/fa-cloud/system-dictionary-column-comments.sql`；新建表脚本已包含全部字段注释。

## 电商大类

执行 `ecommerce-dictionary-category.sql`（在 `merchant-type-dictionary.sql` 后），为 `sys_dict_type` 增加带注释的 `parent_type_code`。默认层级：`ECOMMERCE（电商大类） → FOOD（食品类） → CAKE_SHOP（蛋糕店） / RESTAURANT（餐厅） / SNACK_SHOP（小吃店）`。

字典类型页面可选择上级大类，创建后不可变；仅支持大类、子类型两级类型，子类型下维护字典项。大类下有子类型时禁止删除，也不能直接添加字典项。食品类可继续添加其他食品商家类型；商品分类字典与商家类型分开维护。`MERCHANT_TYPE` 历史商家类型继续有效。

`ECOMMERCE` 下各子类型的字典项同步为可选商家类型，商家类型编码跨电商分类唯一，名称最多32字。停用电商大类、子类型或字典项都会同步禁用类型并下架相关商品，重新启用不会自动上架。已被商家或商品引用的字典项禁止删除。

完整电商分类初始化执行 `ecommerce-dictionary-catalog.sql`（在层级迁移脚本之后），包含13个经营大类及49种设计商家类型。脚本保留已有启停状态与编码，将历史水果店、超市归入生鲜农产、商超便利；旧 CAKE、FRUIT 编码随后通过合并脚本统一。可重复执行，不重复新增。

旧商家类型统一执行 `merchant-type-code-merge.sql`（在完整分类初始化后）：`CAKE → CAKE_SHOP`、`FRUIT → FRUIT_STORE`。保留已有目标类型ID，将店铺关联和商品类型迁移至目标，商品分类展示名称同步为蛋糕店、水果店；商品ID、状态、库存等业务数据不变。迁移成功后删除旧商家类型及其字典项。脚本使用事务及目标类型存在性检查，缺少目标类型或目标字典项时拒绝执行，可重复执行。

## 商家类型页面维护

商家类型管理页面支持新增、修改、删除及批量删除，沿用独立的 `partner:shop-type:add/edit/delete` 权限，无需额外授予通用字典权限。新增时选择电商经营大类；类型编码及所属大类创建后不可修改。页面修改通过字典服务同步商家类型，停用会下架相关商品。已被店铺或商品引用时禁止删除，未引用类型删除时同时删除对应字典项。后台提供 `/api/manager/partner/shop-types/categories` 加载经营大类，供商家类型查询、新增和修改权限使用。
