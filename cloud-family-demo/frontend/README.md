# Cloud Family Frontend

独立 React 前端工程，和 Spring Boot 后端 Maven 多模块分离。

## 环境版本

已安装完成：

```bash
node --version
# v26.8.2

npm --version
# 11.19.1
```

## 开发

```bash
cd frontend
npm install
npm run dev
```

默认开发地址：

```text
http://localhost:35173
```

## 后端访问方式

前端不直接访问 `manager-service` 或 `partner-service`，只访问 gateway：

- `/auth/**`
- `/api/**`

开发环境由 `vite.config.ts` 代理到 gateway，默认目标：

```text
http://localhost:38080
```

如需修改：

```bash
VITE_GATEWAY_URL=http://localhost:38080 npm run dev
```

生产构建时可以使用 `VITE_API_BASE_URL` 指向 gateway 地址：

```bash
VITE_API_BASE_URL=https://api.example.com npm run build
```
