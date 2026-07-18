# 审批中心（Approval Center）

采购合同与销售合同智能对比、采购申请确认和正式采购订单管理系统。

## 目录

- `backend/`：Spring Boot API、Flyway 数据库迁移和合同处理逻辑
- `frontend/`：用户端 Vue 3 应用（端口 `5173`）
- `frontend-admin/`：管理端 Vue 3 应用（端口 `5174`）
- `dify/`：采购经理数字员工 Dify 工作流 DSL
- `docker-compose.yml`：本地与服务器部署配置
- `.env.example`：部署所需的环境变量模板

## 快速部署

1. 准备 Docker 与 Docker Compose。
2. 复制环境变量模板并填写真实值：

   ```powershell
   Copy-Item .env.example .env
   ```

3. 构建并启动：

   ```powershell
   docker compose up -d --build
   ```

4. 访问用户端 `http://localhost:5173`，管理端 `http://localhost:5174`。

首次启动时，Flyway 会自动执行 `backend/src/main/resources/db/migration/` 下的数据库迁移。合同文件和 MySQL 数据使用 Docker 卷持久化。

## 安全说明

- `.env` 仅保存本机或服务器的真实配置，已被 Git 忽略。
- 请为 `MYSQL_ROOT_PASSWORD`、`JWT_SECRET` 和 `INTERNAL_API_KEY` 设置独立的强随机值。
- 发布 Dify DSL 前，请将内部 API 密钥配置为 Dify 环境变量；不要在 DSL 中填写真实密钥。
