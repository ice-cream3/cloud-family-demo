# 会员中心重置密码

先执行 `manager-service/src/main/resources/db/fa-cloud/membership-password-reset-menu.sql` 初始化按钮和独立权限（可重复执行），部署后重新登录刷新权限。默认赋予 SUPER_ADMIN、MANAGER；其他角色可分配 `partner:membership:reset-password`。

会员中心配置的操作栏新增“重置会员密码”。选择会员记录，输入新密码并提交，重置其关联 VIP 用户账号的登录密码。会员记录 ID 不作为用户 ID 使用；关联账号不存在时返回错误。沿用现有 VIP 用户密码重置服务及 PASSWORD_RESET 操作日志，不修改会员方案、到期时间或权益。

接口：`POST /api/manager/partner/features/memberships/password/reset/{会员记录ID}`，请求体 `{"password":"新密码"}`。
