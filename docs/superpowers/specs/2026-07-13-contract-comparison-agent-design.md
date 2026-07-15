# 合同对比 Agent 设计文档

- **日期**：2026-07-13
- **项目**：Contract Comparison Agent
- **目标仓库**：`C:\Users\xingchen\Desktop\agent\`
- **状态**：已通过需求澄清与方案对齐，进入实施规划阶段

## 1. 背景与目标

业务方需要对**同一标的**的采购合同与销售合同做对比，传统人工比对效率低、易遗漏。需要一个系统：

1. 上传两份合同后自动抽取关键要素
2. 按字段做差异对比，给出风险评级
3. 给出可读的风险总结
4. 支持用户对话式追问

下游存在另一个 Agent（采购申请 Agent）会消费本系统抽取出的结构化要素，因此本系统抽取的字段必须**严格对齐**下游约定的 schema。

## 2. 范围

### 2.1 In Scope（MVP）

- 用户注册 / 登录（JWT）
- 上传采购 + 销售两份合同（≤20MB，Tika 可识别）
- 文档解析（Tika 自动识别 doc/docx/pdf/txt 等）
- LLM 结构化抽取（10 个业务字段）
- 字段级对比 + 风险规则引擎
- 风险总结（Agent 生成 Markdown）
- 对话式追问
- 任务列表、详情、Markdown 报告导出
- 管理端：用户管理、全量任务审计、风险规则 CRUD、LLM 调用日志
- 多模型切换（OpenAI / DeepSeek / 通义千问 / Anthropic）

### 2.2 Out of Scope（明确不做）

- 实时流式响应（SSE / WebSocket）—— 阻塞调用
- 多语言 —— 仅中文
- OCR 扫描件 PDF —— Tika 解析失败直接报错
- 合同模板管理 —— 与本项目无关
- 定时任务 / 异步队列 —— 阻塞 + 简单重试
- 细粒度权限 —— 仅 USER / ADMIN 两级
- 本地 LLM 部署 —— 不引入 Ollama
- PDF 报告高保真渲染 —— 仅做能用的转换

## 3. 技术栈

### 3.1 后端

| 类别 | 选型 | 版本 |
|---|---|---|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.3.x |
| 安全 | Spring Security | 6.x |
| 鉴权 | JWT (jjwt) | 0.12.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0 |
| AI | Spring AI | 1.0.x |
| 文档解析 | Apache Tika | 2.9.x |
| Lombok | Lombok | 1.18.x |
| 校验 | Jakarta Validation | 3.x |
| API 文档 | springdoc-openapi | 2.x |
| 构建 | Maven | 3.9+ |

### 3.2 前端

| 类别 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 框架 | Vue | 3.5.x | Composition API + `<script setup>` |
| 构建 | Vite | 5.x | 双 SPA：frontend(5173) / frontend-admin(5174) |
| 语言 | TypeScript | 5.x | strict 模式 |
| 路由 | Vue Router | 4.x | createRouter + 路由守卫鉴权 |
| 状态 | Pinia | 2.x | auth / toast store |
| HTTP | Axios | 1.x | 拦截器统一处理 401/500/code≠0 |
| 样式 | Tailwind CSS | 4.x | `@tailwindcss/vite` 插件，非 browser build |
| 图标 | lucide-vue-next | 0.460.x | 图标作 Vue 组件，替代设计稿 mask-image SVG |
| 字体 | @fontsource/dm-sans + @fontsource/jetbrains-mono | 5.x | 设计稿原字体，本地打包 |
| 提示 | 自实现 Toast store | — | 替代 ElMessage，与设计系统风格一致 |
| 工具 | dayjs / marked | 1.x / 14.x | 时间格式化 + Markdown 渲染 |
| 包管理 | pnpm | 9.x | |

> **设计基线**：UI 完全还原 `design/` 目录下的 5 个设计稿（登录 / 任务列表 / 任务详情 / 管理概览 / 用户管理）。设计系统源：Google 配色主题（`#4285f4` 主色、`#f0f6ff` 侧边栏、`#4285f4` 图表主色、`#ea4335` 风险红、`#34a853` 安全绿、`#fbbc05` 警示黄）、DM Sans + JetBrains Mono 字体、零阴影扁平卡片（`--shadow-opacity: 0`）、8px 圆角（`--radius: 0.5rem`）、3.84px 间距基准（`--spacing: 0.24rem`）。CSS 变量从设计稿 `<style id="theme-vars">` 提取到 `src/styles/theme.css`，再通过 `@theme inline` 别名层暴露给 Tailwind v4。

### 3.3 存储

- 关系数据：MySQL 8.0
- 合同原件：本地磁盘 `./storage/contracts/{taskId}/{side}.{ext}`
- 抽象层：`StorageService` 接口，便于后续切 MinIO

### 3.4 LLM

- 通过 Spring AI 抽象
- Provider 可选：OpenAI / DeepSeek / 通义千问 / Anthropic
- 用 profile 切换：`application-{profile}.yml`

## 4. 仓库结构

```
agent/
├── backend/            Spring Boot 后端（统一服务，按角色路由）
├── frontend/           Vue 3 用户端 SPA
├── frontend-admin/     Vue 3 管理端 SPA
└── docs/
    ├── api.md                    REST API 文档
    ├── samples/                  样例合同
    └── superpowers/
        └── specs/
            └── 2026-07-13-contract-comparison-agent-design.md   本文件
```

## 5. 系统架构

```
┌─────────────────────────────────────────────────────┐
│  表现层（两个独立 Vue SPA）                          │
│  ┌──────────────────┐    ┌────────────────────┐     │
│  │ frontend (用户)   │    │ frontend-admin     │     │
│  └──────────────────┘    └────────────────────┘     │
└──────────────┬─────────────────────┬────────────────┘
               │ HTTP/JSON + JWT      │
               ▼                      ▼
┌──────────────────────────────────────────────────────┐
│  后端层（单一 Spring Boot 服务）                       │
│  Security Filter (JWT)                                │
│  ├─ User API    ├─ Admin API    ├─ Upload API         │
│  Application Service                                  │
│  ├─ ContractService  ├─ ComparisonService             │
│  ├─ ExtractionService  ├─ ChatService                 │
│  Agent 核心（Spring AI ChatClient）                   │
│  ├─ System Prompt + Tools (@Tool)                    │
│  基础设施：Tika / Spring AI / 文件存储 / MyBatis-Plus │
└──────────────┬─────────────────────┬────────────────┘
               ▼                     ▼
       ┌──────────────┐      ┌──────────────┐
       │  MySQL 8.x   │      │ 本地磁盘      │
       └──────────────┘      │ ./storage/   │
                              └──────────────┘
                                       │
                                       ▼
                              ┌────────────────┐
                              │ 云端 LLM API   │
                              └────────────────┘
```

**关键决策：**
- 后端单服务、双前端：避免重复部署，按 JWT 角色路由
- Agent = `ChatClient` + `@Tool` + `ChatMemory`：LLM 自主决定调用顺序
- 文件存储走接口抽象，便于后续切对象存储
- 多模型通过 profile 切换，无需改代码

## 6. 数据模型（7 张表）

### 6.1 `user` —— 用户

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| username | VARCHAR(64) UNIQUE | 登录名 |
| password_hash | VARCHAR(255) | BCrypt |
| role | VARCHAR(16) | `USER` / `ADMIN` |
| enabled | TINYINT | 0/1，禁用账号 |
| created_at | DATETIME | |

### 6.2 `comparison_task` —— 对比任务（聚合根）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT FK → user.id | |
| title | VARCHAR(255) | 任务名 |
| status | VARCHAR(16) | `PENDING` / `RUNNING` / `DONE` / `FAILED` / `CONFIRMED` |
| summary | TEXT | Agent 生成的总结 |
| risk_level | VARCHAR(16) | `LOW` / `MEDIUM` / `HIGH` |
| confirmed_at | DATETIME | 人工确认创建采购单的时间（仅 CONFIRMED 状态有值） |
| confirmed_by | BIGINT FK → user.id | 确认人 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

> 状态流转：`PENDING → RUNNING → DONE → CONFIRMED`（用户在任务详情页点击「确认创建采购单」后）；任意阶段失败转 `FAILED`。
> 只有 `DONE` 状态可被确认；确认后 BUY 侧 extraction 的 `application_status` 从 `待提交` 升为 `已确认`，`message` 改为 `已人工确认创建采购单`。

### 6.3 `contract` —— 合同原件元数据

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| task_id | BIGINT FK → comparison_task.id | |
| side | VARCHAR(8) | `BUY` / `SELL` |
| original_filename | VARCHAR(255) | |
| stored_path | VARCHAR(512) | `./storage/contracts/{taskId}/{side}.{ext}` |
| mime_type | VARCHAR(64) | Tika 识别结果 |
| extracted_text | LONGTEXT | Tika 解析的纯文本 |
| file_size | BIGINT | 字节 |
| uploaded_at | DATETIME | |

### 6.4 `extraction` —— 抽取结果（强类型列）

每份合同对应一行，按下游 Agent 约定的字段直接建列：

| 字段 | 类型 | 来源 | 说明 |
|---|---|---|---|
| id | BIGINT PK | | |
| contract_id | BIGINT FK | | 一份合同一行 |
| **supplier_name** | VARCHAR(255) | LLM 抽取 | 供应商名称 |
| **item_name** | VARCHAR(255) | LLM 抽取 | 商品标准名称（→ product_name） |
| **item_model** | VARCHAR(255) | LLM 抽取 | 规格型号（→ product_model） |
| **unit** | VARCHAR(32) | LLM 抽取 | 单位 |
| **quantity** | DECIMAL(18,4) | LLM 抽取 | 采购数量 |
| **purchase_unit_price** | DECIMAL(18,4) | LLM 抽取 | 采购单价 |
| **purchase_total_amount** | DECIMAL(18,4) | LLM 抽取 | 采购总金额 |
| **expected_delivery_date** | DATE | LLM 抽取 | 预计交付日期 |
| **payment_terms** | TEXT | LLM 抽取 | 付款条款 |
| **delivery_location** | VARCHAR(255) | LLM 抽取 | 交付地点 |
| **application_no** | VARCHAR(64) | 系统填 | 采购申请单编号（PA+yyyyMMddHHmmss+4 位随机数） |
| **application_title** | VARCHAR(255) | 系统填 | `{supplier_name}-{item_name}` |
| **apply_date** | DATE | 系统填 | 任务创建当天 |
| **application_type** | VARCHAR(32) | 系统填 | `商品采购` |
| **currency** | VARCHAR(8) | 系统填 | `CNY` |
| **application_status** | VARCHAR(16) | 系统填 | `待提交` |
| **create_time** | DATETIME | 系统填 | 创建时间（抽取落库时刻） |
| **message** | VARCHAR(255) | 系统填 | 创建结果说明（初值 `抽取成功`） |
| confidence | DECIMAL(3,2) | LLM 自评 | 0~1 |
| raw_quote | TEXT | LLM 抽取 | 原文引用（溯源用） |
| extracted_at | DATETIME | | |

> 粗体 = 核心业务字段（共 10 个）；其余 8 个 `application_*` / `currency` / `create_time` / `message` 由后端按固定规则填充，**不**让 LLM 生成，避免幻觉。
> 最终参与对比的字段共 18 个（即除去 `confidence` / `raw_quote` / `extracted_at` 三个元数据字段之外的全部业务字段）。

### 6.5 `chat_message` —— 对话历史

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| task_id | BIGINT FK → comparison_task.id | |
| role | VARCHAR(16) | `USER` / `ASSISTANT` / `TOOL` |
| content | TEXT | |
| tool_calls | JSON | LLM 调用的工具名+参数（nullable） |
| created_at | DATETIME | |

### 6.6 `risk_rule` —— 风险规则（管理端配置）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| field_key | VARCHAR(64) | 字段名 |
| operator | VARCHAR(8) | `GT` / `LT` / `EQ` / `NE` / `PCT_GT` |
| threshold_value | DECIMAL(18,4) | 阈值 |
| risk_level | VARCHAR(16) | 触发后的等级 |
| enabled | TINYINT | 是否启用 |
| remark | VARCHAR(255) | 备注 |
| updated_at | DATETIME | |
| updated_by | BIGINT FK → user.id | 最后修改人 |

**预置规则（DB 初始化脚本插入）：**

| field_key | operator | threshold_value | risk_level | 说明 |
|---|---|---|---|---|
| purchaseTotalAmount | PCT_GT | 0.05 | HIGH | 金额差 > 5% |
| quantity | NE | 0 | HIGH | 数量不一致 |
| expectedDeliveryDate | GT | 7 | MEDIUM | 日期差 > 7 天 |
| paymentTerms | EQ | "" | MEDIUM | 付款条款缺失 |
| deliveryLocation | NE | "" | MEDIUM | 交付地点不一致 |

### 6.7 `llm_call_log` —— LLM 调用日志

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| task_id | BIGINT FK → comparison_task.id | |
| user_id | BIGINT FK → user.id | |
| model | VARCHAR(64) | |
| prompt_tokens | INT | |
| completion_tokens | INT | |
| total_tokens | INT | |
| duration_ms | INT | |
| status | VARCHAR(16) | `OK` / `FAIL` |
| error_msg | TEXT | |
| created_at | DATETIME | |

### 6.8 关系图

```
user (1) ──< (N) comparison_task
                │
                ├──< (N) contract (2 行：BUY + SELL)
                │           │
                │           └──< (1) extraction    ← 强类型字段
                │
                ├──< (N) chat_message
                │
                └──< (N) llm_call_log

risk_rule (独立表)
user (1) ──< (N) llm_call_log
user (1) ──< (N) risk_rule.updated_by
```

## 7. API 设计

详见 `docs/api.md`。要点：

- 统一响应：`{ code, message, data }`
- 错误码：1001/1002/1003/2001/4001/4002/5001/9999
- 任务创建异步（202），其他接口阻塞
- 对话接口阻塞非流式
- 上传 `multipart/form-data`，单文件 ≤ 20MB
- 任务重跑仅对 `FAILED` 开放
- 任务确认创建采购单 `POST /api/tasks/{id}/confirm` 仅对 `DONE` 开放，确认后状态转 `CONFIRMED`

## 8. Agent 设计

### 8.1 形态

```
┌────────────────────────────────────────────────────┐
│  ChatClient (Spring AI)                            │
│  ├─ System Prompt                                  │
│  ├─ Tools（@Tool 注解）                            │
│  │   1. extract_contract(contractId)               │
│  │   2. get_extraction(contractId)                 │
│  │   3. compare_fields(taskId)                     │
│  │   4. get_chat_history(taskId)                   │
│  ├─ ChatMemory（按 taskId 隔离的 MessageWindow）    │
│  └─ 调用：call() 非流式                            │
└────────────────────────────────────────────────────┘
```

### 8.2 工作流

**任务创建后：**
```
LLM 自主规划：
  1. extract_contract(buyId)   → 抽取采购
  2. extract_contract(sellId)  → 抽取销售
  3. compare_fields(taskId)    → 调规则引擎 + 求 risk_level
  4. 生成 summary 落库 → 状态 DONE
```

**用户追问：**
```
ChatMemory 自动附带历史
LLM 推理：可能直接答 / 调 get_extraction / 调 get_chat_history
返回自然语言回答
```

### 8.3 System Prompt 草案

```
你是「合同对比助手」，负责对比同一标的的采购合同与销售合同。

# 工作流（仅在任务刚创建时执行）
1. 调 extract_contract 抽取采购合同
2. 调 extract_contract 抽取销售合同
3. 调 compare_fields 做字段对比
4. 把 compare_fields 的结果用自然语言总结，输出 3 段：
   - 核心差异（金额/数量/日期）
   - 风险提示（按 HIGH/MEDIUM 排序）
   - 建议（人话，不堆术语）

# 追问时
- 用户问"X 是什么"时，优先调 get_extraction
- 用户问"为什么有风险"时，引用 raw_quote 字段
- 永远不要编造数据，没抽到就说"该字段在原文中未明确"

# 输出约束
- 中文 / ≤800 字 / 金额保留两位 / 风险等级用 🟢🟡🔴 emoji
- 不重复用户已知内容
```

### 8.4 抽取 JSON Schema

```json
{
  "supplier_name": "string|null",
  "item_name":     "string|null",
  "item_model":    "string|null",
  "unit":          "string|null",
  "quantity":      "number|null",
  "purchase_unit_price":   "number|null",
  "purchase_total_amount": "number|null",
  "expected_delivery_date": "YYYY-MM-DD|null",
  "payment_terms":   "string|null",
  "delivery_location": "string|null"
}
```

用 `BeanOutputConverter` 强约束，反序列化失败时**重试 1 次**，仍失败则对应字段置 `null` + `raw_quote="抽取失败"`。

### 8.5 自动填充规则

| 字段 | 规则 |
|---|---|
| application_title | `{supplier_name}-{item_name}`，任一缺失则降级 |
| apply_date | 任务创建当天 |
| application_type | `商品采购`（写死） |
| currency | `CNY`（写死） |
| application_status | `待提交`（写死） |

## 9. 对比与风险引擎

### 9.1 字段差异

10 个业务字段两两比对，结果为：
- `MATCH` —— 值一致
- `DIFFER` —— 值不一致
- `MISSING` —— 某侧未抽到（null）

### 9.2 风险规则

按 `risk_rule` 表动态求 `risk_level`：

| operator | 含义 |
|---|---|
| `EQ` | 字段值等于阈值 |
| `NE` | 字段值不等于阈值 |
| `GT` | 字段差（绝对值）大于阈值（数值型） |
| `LT` | 字段差小于阈值 |
| `PCT_GT` | `|buy - sell| / min(buy, sell)` 大于阈值（百分比） |

字段比较时 `null` 视为 `MISSING`，与 `EQ ""` / `NE ""` 配合检查"缺失"型规则。

### 9.3 最终 risk_level

取**所有命中规则的最高等级**（HIGH > MEDIUM > LOW）。无规则命中 = `LOW`。

## 10. 前端要点

### 10.0 设计基线（首要约束）

- **设计稿来源**：`design/pages/` 目录下的 5 个 HTML 文件（登录 / 任务列表 / 任务详情 / 管理概览 / 用户管理），其 `<style id="theme-vars">` 块为唯一设计 Token 来源。
- **视觉风格**：Google dashboard 配色（`#4285f4` 主色、`#f0f6ff` 侧边栏）、零阴影扁平卡片（`--shadow-opacity: 0`，仅靠 1px border 划分层次）、8px 圆角、3.84px 间距基准。
- **字体**：DM Sans（正文）+ JetBrains Mono（代码 / 数字 / ID）。
- **图标**：设计稿用 `mask-image: url("../assets/image_*.svg")` 引用 SVG，Vue 实现统一替换为 `lucide-vue-next` 组件（映射见下表）。
- **Tailwind v4**：用 `@tailwindcss/vite` 插件，`src/styles/theme.css` 通过 `@theme inline` 别名层把设计稿 CSS 变量暴露给 Tailwind 工具类（如 `bg-[var(--sidebar)]` / `text-[var(--muted-foreground)]`）。**不**使用 browser build CDN。
- **缺失页面（新建任务 / 个人中心 / 风险规则 / LLM 日志）**：按设计系统补齐 —— 复用同一套 CSS 变量、同一套卡片/表格/Modal 样式、同一套 spacing/radius token，不引入新设计语言。

#### 图标映射（设计稿 SVG → lucide-vue-next）

| 设计稿 SVG | 用途 | Lucide 组件 |
|---|---|---|
| image_0 | 用户任务导航 | `ListTodo` |
| image_1 / image_9 / image_18 | 管理概览导航 | `LayoutDashboard` |
| image_2 | 搜索 | `Search` |
| image_3 / image_10 | 通知 / 风险提示 | `Bell` / `TriangleAlert` |
| image_4 | 新建对比 | `Plus` |
| image_5 / image_12 | 导出报告 | `Download` |
| image_6 / image_11 | 上一页 / 面包屑返回 | `ChevronLeft` |
| image_7 | 下一页 | `ChevronRight` |
| image_13 | AI 风险摘要 | `Sparkles` |
| image_14 | 交付日期 | `Calendar` |
| image_15 | 付款条款 | `CreditCard` |
| image_16 | 对话追问 | `MessageCircle` |
| image_17 | 发送 | `SendHorizontal` |
| image_19 | 编辑 | `PenLine` |

### 10.1 用户端（frontend/）

| 页面 | 路由 | 关键内容 | 设计稿对应 |
|---|---|---|---|
| 登录/注册 | `/login` | 登录/注册 Tab 切换，居中 max-w-sm 卡片，brand mark "CC" | `design/pages/登录.html` |
| 任务列表 | `/tasks` | 侧边栏 260px + 顶栏 76px + Hero + filter chips + data-table + pagination | `design/pages/任务列表.html` |
| 新建任务 | `/tasks/new` | 两上传框 + 任务名 + 提交（按设计系统补齐） | — |
| 任务详情 | `/tasks/:id` | 面包屑 + 标题行 + AI 风险摘要卡 + 双列对比卡片 + 字段差异表 + 对话追问区 | `design/pages/任务详情.html` |
| 个人中心 | `/me` | 改密码、登出（按设计系统补齐） | — |

### 10.2 管理端（frontend-admin/）

| 页面 | 路由 | 关键内容 | 设计稿对应 |
|---|---|---|---|
| 登录 | `/login` | 共用登录页，USER 角色拒绝，按 role 跳转 | `design/pages/登录.html` |
| 管理概览 | `/` | 4 列 stats-grid + panels-grid（CSS 柱状图 + 最近活动）+ 最近任务表 | `design/pages/管理概览.html` |
| 用户管理 | `/users` | filter bar + 用户表格（角色徽标 / 状态徽标 / 操作 / 禁用行半透明）+ 编辑 Modal | `design/pages/用户管理.html` |
| 任务审计 | `/tasks` | 全量任务，可点开任意用户任务详情（按设计系统补齐） | — |
| 风险规则 | `/risk-rules` | CRUD 阈值（按设计系统补齐） | — |
| LLM 日志 | `/llm-logs` | 调用日志列表 + 详情抽屉（按设计系统补齐） | — |

### 10.3 要素对比表示例

```
字段              |  采购合同     |  销售合同     |  差异
------------------+--------------+--------------+--------
商品名称          |  螺丝 M8×20  |  螺丝 M8×20  |  ✅ 一致
采购单价          |  ¥0.50       |  ¥0.80       |  ⚠️ 差 60%
付款条款          |  月结 30 天   |  （未提及）   |  🔴 风险
```

### 10.4 替代方案说明

- **Element Plus → Tailwind v4 + 自实现组件**：原计划用 Element Plus 提供表格 / 表单 / Dialog / Message，现改为按设计稿样式自实现轻量组件（DataTable、StatusBadge、RiskBadge、FilterChips、Pagination、Modal、Toast store），保证像素级还原设计稿。缺失能力用 `lucide-vue-next` + dayjs + marked 补齐。
- **ElMessage → Toast store**：自实现 `useToastStore()`，固定在视口右上角，3 秒自动消失，样式与设计稿的卡片/圆角/border 体系一致。
- **跨端跳转**：保持双 SPA 架构，用户端侧边栏「管理概览」与管理端侧边栏「用户任务」通过跳链（`window.location.href` 切换端口）实现，不合并为单一 SPA。

## 11. 错误处理

### 11.1 后端

```json
{ "code": 4001, "message": "合同文本解析失败", "data": null }
```

| 场景 | HTTP | code |
|---|---|---|
| 参数校验失败 | 400 | 1001 |
| 未登录 / Token 过期 | 401 | 1002 |
| 无权限 | 403 | 1003 |
| 合同解析失败 | 422 | 4001 |
| 抽取反序列化失败 | 422 | 4002 |
| LLM 调用失败（已重试 1 次） | 502 | 5001 |
| 任务不存在 | 404 | 2001 |
| 系统异常 | 500 | 9999 |

### 11.2 前端

- 401 → 清 localStorage，跳登录
- 5001 → 任务状态 `FAILED`，详情页给"重跑"按钮
- 字段缺失 → 表格显示"—" + tooltip 提示原文位置

## 12. 测试

| 层 | 工具 | 覆盖 |
|---|---|---|
| 后端单元 | JUnit 5 + Mockito | Service 层：Tika 解析、规则引擎、风险计算 |
| 后端集成 | Spring Boot Test + Testcontainers (MySQL) | Repository + Controller |
| Agent 工具 | Mock ChatClient 测 `@Tool` | 4 个工具的入参/出参、异常路径 |
| LLM 输出 | 真实 API + 2~3 份样例 | 开发期手动跑，CI 不跑 |
| 前端 | Vitest（关键工具函数） + 手测 | |

**样例合同**：`docs/samples/`，含 1 份采购 + 1 份销售 + 1 份"故意有冲突"（演示 risk_level=HIGH）。

## 13. 部署与运行

- 后端： `./mvnw spring-boot:run`，默认 8080，依赖本地 MySQL
- 用户端： `pnpm dev`，5173，代理 `/api` → `http://localhost:8080`
- 管理端： `pnpm dev`，5174
- 环境变量： `OPENAI_API_KEY` / `DEEPSEEK_API_KEY` / `DASHSCOPE_API_KEY` 等
- 默认账号： `admin / admin123`（启动时初始化，role=ADMIN）

## 14. 风险与后续

- **LLM 抽取质量**：依赖模型与 prompt，先用 gpt-4o-mini / DeepSeek 跑通，后续做 prompt 调优或 fine-tune
- **长合同超 token**：合同 > 50k 字时考虑切片（chunking + map-reduce），MVP 不做
- **多模型一致性**：不同模型对同一份合同抽取可能不一致，先在单模型上稳定后再扩
- **下游 Agent 对接**：`extraction` 表结构已对齐，下游可直接读 `extraction` 行的 10 个业务字段
- **存储可替换性**：`StorageService` 接口预留，后续切 MinIO 不影响业务
