# astrodc — 天然卫星数据中心

Vue 前端与 Java 后端，后端集成 AI Agent。

- `frontend/`：前端代码。
- `backend/`：后端代码。
- `docs/`：配置与观测报告说明。
- `scripts/`：本机启动、隧道和健康检查。

## 运行与构建

需要 Node.js、Java 17+ 和 MySQL。后端凭据配置见 [配置说明](docs/configuration.md)。

在 `frontend` 中执行 `npm ci` 安装依赖，`npm run serve` 启动前端，`npm run build` 构建。
在 `backend` 中执行 `.\mvnw.cmd spring-boot:run` 启动后端，`.\mvnw.cmd package -DskipTests=false` 构建并测试。

本机 PowerShell 的 `on` 命令可同时启动前后端。
前端地址：<http://localhost:8080>；后端地址：<http://localhost:8088>。

## Cloudflare 与健康检查

Pages 项目为 `astrodc`，前端发布目录为 `frontend/dist`。目前部署暂停，前端 `npm run deploy` 会直接停止。
后端经 Cloudflare Tunnel 提供 <https://api.astrodc.top>，Windows 自动恢复任务使用 `scripts/start-backend.ps1`。

在项目根目录执行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/check-health.ps1` 检查网站与 API。
隧道开机入口使用 `scripts/start-tunnel.ps1`，凭据保存在用户的 `.cloudflared` 目录。
