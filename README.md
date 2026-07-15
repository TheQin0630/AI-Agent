# 合同对比 Agent（Contract Comparison Agent）

基于 LLM 的"采购合同 vs 销售合同"智能对比系统。用户上传同一标的的两份合同，系统自动抽取结构化要素、对比差异、给出风险提示，并支持对话式追问。

## 仓库结构

```
agent/
├── backend/            Spring Boot 后端（统一服务，按角色路由 API）
├── frontend/           Vue 3 用户端 SPA
├── frontend-admin/     Vue 3 管理端 SPA
└── docs/               设计文档、接口文档、样例合同
```

## 技术栈

### 后端（backend/）

| 类别 | 选型 | 版本 | 用途 |
|---|---|---|---|
| 语言 | Java | 17 | （也兼容 21） |
| 框架 | Spring Boot | 3.3.x | Web、IoC |
| 安全 | Spring Security | 6.x | 认证、权限 |
| 鉴权 | JWT (jjwt) | 0.12.x | 无状态登录 |
| ORM | MyBatis-Plus | 3.5.x | 持久化 |
| 数据库 | MySQL | 8.0 | 关系数据 |
| 连接池 | HikariCP | 内置 | |
| AI | Spring AI | 1.0.x | ChatClient + Tools + ChatMemory |
| 文档解析 | Apache Tika | 2.9.x | 统一识别 doc/docx/pdf/txt 等 |
| 文件解析 | Apache POI | 5.2.x | （可选）Tika 内部已含 |
| 工具 | Lombok | 1.18.x | 简化 POJO |
| 校验 | Jakarta Validation | 3.x | 入参校验 |
| API 文档 | springdoc-openapi | 2.x | Swagger UI |
| 构建 | Maven | 3.9+ | |

### 前端（frontend/ + frontend-admin/）

| 类别 | 选型 | 版本 | 用途 |
|---|---|---|---|
| 框架 | Vue | 3.5.x | Composition API |
| 构建 | Vite | 5.x | 开发与构建 |
| 语言 | TypeScript | 5.x | 类型 |
| 路由 | Vue Router | 4.x | |
| 状态 | Pinia | 2.x | |
| HTTP | Axios | 1.x | 带拦截器统一处理 401/500 |
| 样式 | Tailwind CSS | 4.x | `@tailwindcss/vite` 插件，按设计稿 CSS 变量主题 |
| 图标 | lucide-vue-next | 0.460.x | 图标作 Vue 组件 |
| 字体 | @fontsource/dm-sans + @fontsource/jetbrains-mono | 5.x | 设计稿原字体本地打包 |
| 工具 | dayjs | 1.x | 日期 |
| 工具 | marked | 14.x | 渲染 Agent 的 Markdown 总结 |
| 包管理 | pnpm | 9.x | |

### AI / 模型

- 通过 **Spring AI** 抽象层调用，**多模型可切换**（用 profile 控制）：
  - `openai` —— OpenAI（gpt-4o-mini / gpt-4o）
  - `deepseek` —— DeepSeek
  - `dashscope` —— 阿里云通义千问
  - `anthropic` —— Claude（可选）
- **不**做本地模型部署（MVP 不引入 Ollama）。
- 提示词策略：System Prompt 定义角色与工作流，`@Tool` 暴露 4 个工具（`extract_contract` / `get_extraction` / `compare_fields` / `get_chat_history`），用 `BeanOutputConverter` 强约束抽取输出 JSON。

### 存储

| 数据 | 存储 |
|---|---|
| 用户、任务、合同、要素、对话、风险规则、LLM 日志 | MySQL 8.0 |
| 合同原件 | 本地磁盘 `./storage/contracts/{taskId}/{side}.{ext}` |
| 抽象 | `StorageService` 接口，便于后续切 MinIO |

### 开发与运行

- **JDK**：17（也兼容 21；Spring Boot 3.3 要求 17+）
- **Node**：18+（开发环境为 v24）
- **pnpm**：9.x
- **MySQL**：8.0（本地或 Docker）
- **LLM Key**：至少一个云端 provider 的 API Key

### 测试

- 后端：JUnit 5 + Mockito + Spring Boot Test + Testcontainers (MySQL)
- Agent 工具：Mock ChatClient 测 `@Tool` 方法
- LLM 输出：开发期用真实 API 跑 2~3 份样例合同；**CI 不跑**（成本 + 不稳定）
- 前端：Vitest（关键工具函数）+ 手测

## 角色与端

| 端 | 角色 | 核心能力 |
|---|---|---|
| 用户端 `frontend/` | USER | 上传两份合同、查看对比结果、对话式追问、导出报告 |
| 管理端 `frontend-admin/` | ADMIN | 用户管理、全量任务审计、风险规则配置、LLM 调用统计 |

## 快速开始（开发期）

```bash
# 1. 准备 MySQL，建库
mysql -uroot -p -e "CREATE DATABASE contract_agent DEFAULT CHARSET utf8mb4;"

# 2. 后端
cd backend
# 编辑 src/main/resources/application-local.yml，填 LLM API Key、MySQL 密码
mvn spring-boot:run -Dspring-boot.run.profiles=local    # http://localhost:8080

# 3. 用户端
cd ../frontend
pnpm install
pnpm dev    # http://localhost:5173

# 4. 管理端
cd ../frontend-admin
pnpm install
pnpm dev    # http://localhost:5174
```

默认账号：`admin / admin123`（首次启动时初始化脚本写入，role=ADMIN）。

> 前端样式完全还原 `design/pages/` 下的 5 个设计稿（登录 / 任务列表 / 任务详情 / 管理概览 / 用户管理），采用 Google 配色主题（`#4285f4` 主色、`#f0f6ff` 侧边栏）、DM Sans + JetBrains Mono 字体、零阴影扁平卡片、8px 圆角。缺失页面（新建任务 / 个人中心 / 风险规则 / LLM 日志 / 任务审计）按同一设计系统补齐。

## 文档

- 设计文档：`docs/superpowers/specs/2026-07-13-contract-comparison-agent-design.md`
- API 文档：后端启动后访问 `http://localhost:8080/swagger-ui.html`
- 样例合同：`docs/samples/`

## 许可证

内部项目，暂未开源。
