# astrodc — 天然卫星数据中心

前端与 Java 后端统一维护在本仓库。两边原有 Git 提交历史均已保留。

```text
astrodc/
├── frontend/     Vue 2 网站
├── backend/      Spring Boot / Java 17 后端（包含 AI Agent）
├── docs/         配置及观测报告说明
├── scripts/      启动、测试、部署脚本
└── deployment.json
```

## 本地启动

需要 Node.js、Java 17+ 和本机 MySQL。先在 frontend 执行 `npm ci`。
后端凭据使用环境变量或 `backend/config/application-private.yml`，详见 [配置说明](docs/configuration.md)。

在总目录执行 `npm run dev`，后台启动两个服务：

- 前端：<http://localhost:8080>
- 后端：<http://localhost:8088>

单独在前台运行：`npm run dev:frontend`、`npm run dev:backend`。
日志写入忽略目录 `.runtime/`。

## 验证与构建

`npm run test:frontend` 运行前端验证，`npm run test:backend` 运行后端测试。
`npm run build` 输出 `frontend/dist`；`npm run build:backend` 输出后端 JAR。

## Cloudflare

现有 Pages 项目 `astrodc` 使用手动上传。当前新部署已暂停：`deployment.json` 中 `paused: true` 会阻止部署脚本。
现有线上版本继续提供服务。需要发布时明确将此值改成 false，再执行 `npm run deploy`。
前端部署目录为 `frontend/dist`；后端由本机 Java 服务运行，经已有 Cloudflare Tunnel 提供 `api.astrodc.top`。

Windows 自动恢复任务应指向总目录的 `scripts/start-backend.ps1`，工作目录设为 `backend`。

## 迁移备份

旧仓库的本机副本及原启动配置保存在忽略目录 `.migration/`，便于回退。旧的前后端路径保留为目录链接，实际工作文件统一位于本总目录。Git 操作请在 astrodc 总目录执行。
