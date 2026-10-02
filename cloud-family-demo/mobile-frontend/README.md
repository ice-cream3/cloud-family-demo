# Cloud Family Mobile Frontend

独立的 partner 用户端前端工程，使用 React + Vite + TypeScript。工程同时适配 PC Web 和 H5，不包含管理端后台代码。

## 功能

- partner 用户登录：`POST /auth/api/login`
- partner 用户注册：`POST /auth/api/register`
- 登录态本地保存和失效清理
- 当前用户信息：`POST /api/users/me`
- partner 服务状态：`POST /api/users/health`
- 响应式首页基础布局：桌面侧边栏、移动端底部导航、账号概览、快捷入口、最近动态

## 开发

```bash
cd mobile-frontend
npm install
npm run dev
```

默认开发地址：

```text
http://localhost:35175
```

开发环境代理到 gateway，默认目标：

```text
http://localhost:38080
```

如需覆盖：

```bash
VITE_GATEWAY_URL=http://localhost:38080 npm run dev
```

生产构建时可以使用 `VITE_API_BASE_URL` 指向 gateway 地址：

```bash
VITE_API_BASE_URL=https://api.example.com npm run build
```

## 构建

```bash
npm run build
```
