# API 文档

合同对比 Agent 后端 REST API。基于 JWT 鉴权，按角色（`USER` / `ADMIN`）路由。

- **Base URL**：`http://localhost:8080`
- **认证**：除 `/api/auth/**` 外所有接口需 `Authorization: Bearer <token>` 头
- **数据格式**：JSON（`Content-Type: application/json; charset=utf-8`），文件上传除外（`multipart/form-data`）
- **统一响应**：
  ```json
  { "code": 0, "message": "ok", "data": <T> }
  ```
  错误时 `code` 非 0，`data` 为 `null`。

## 错误码

| HTTP | code | 含义 |
|---|---|---|
| 400 | 1001 | 参数校验失败 |
| 401 | 1002 | 未登录 / Token 过期 / 无效 |
| 403 | 1003 | 无权限 |
| 404 | 2001 | 资源不存在 |
| 422 | 4001 | 合同解析失败 |
| 422 | 4002 | 抽取反序列化失败 |
| 502 | 5001 | LLM 调用失败（已重试 1 次） |
| 500 | 9999 | 系统异常 |

---

## 1. 认证（公开）

### 1.1 注册

```
POST /api/auth/register
```

请求：
```json
{
  "username": "zhangsan",
  "password": "Passw0rd!"
}
```
响应（201）：
```json
{ "code": 0, "message": "ok", "data": { "id": 7, "username": "zhangsan", "role": "USER" } }
```

### 1.2 登录

```
POST /api/auth/login
```
请求同注册。响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "user": { "id": 7, "username": "zhangsan", "role": "USER" }
  }
}
```

### 1.3 当前用户

```
GET /api/auth/me
```
响应：
```json
{ "code": 0, "message": "ok", "data": { "id": 7, "username": "zhangsan", "role": "USER" } }
```

---

## 2. 用户端（ROLE_USER）

### 2.1 创建对比任务

```
POST /api/tasks
Content-Type: multipart/form-data
```

表单字段：
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | string | 是 | 任务名（≤255） |
| buyFile | file | 是 | 采购合同（≤20MB，Tika 可识别） |
| sellFile | file | 是 | 销售合同（≤20MB） |

响应（202，任务进入异步处理）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "title": "7月螺丝采购",
    "status": "PENDING",
    "createdAt": "2026-07-13T10:30:00Z"
  }
}
```

### 2.2 我的任务列表

```
GET /api/tasks?page=1&size=20&status=DONE
```

查询参数：
| 名称 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | int | 否 | 默认 1 |
| size | int | 否 | 默认 20，≤100 |
| status | enum | 否 | `PENDING` / `RUNNING` / `DONE` / `FAILED` |

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 32,
    "items": [
      {
        "id": 1001, "title": "7月螺丝采购",
        "status": "DONE", "riskLevel": "HIGH",
        "createdAt": "...", "updatedAt": "..."
      }
    ]
  }
}
```

### 2.3 任务详情

```
GET /api/tasks/{id}
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "title": "7月螺丝采购",
    "status": "DONE",
    "riskLevel": "HIGH",
    "summary": "🔴 金额差 60%…\n🟡 交付日期相差 10 天…",
    "createdAt": "...",
    "updatedAt": "...",
    "buy": {
      "id": 2001,
      "filename": "purchase_july.docx",
      "extraction": {
        "supplierName": "华东五金",
        "itemName": "螺丝 M8×20",
        "itemModel": "M8×20",
        "unit": "个",
        "quantity": 10000,
        "purchaseUnitPrice": 0.50,
        "purchaseTotalAmount": 5000.00,
        "expectedDeliveryDate": "2026-07-25",
        "paymentTerms": "月结 30 天",
        "deliveryLocation": "上海仓",
        "applicationTitle": "华东五金-螺丝 M8×20",
        "applyDate": "2026-07-13",
        "applicationType": "商品采购",
        "currency": "CNY",
        "applicationStatus": "待提交",
        "confidence": 0.92,
        "rawQuote": "采购单价人民币 0.50 元…"
      }
    },
    "sell": { "id": 2002, "filename": "sales_july.docx", "extraction": { ... } },
    "differences": [
      { "field": "purchaseUnitPrice", "buy": 0.50, "sell": 0.80, "status": "DIFFER", "risk": "HIGH" },
      { "field": "purchaseTotalAmount", "buy": 5000, "sell": 8000, "status": "DIFFER", "risk": "HIGH" },
      { "field": "itemName", "buy": "螺丝 M8×20", "sell": "螺丝 M8×20", "status": "MATCH", "risk": null },
      { "field": "paymentTerms", "buy": "月结 30 天", "sell": null, "status": "MISSING", "risk": "MEDIUM" }
    ]
  }
}
```

### 2.4 对话式追问

```
POST /api/tasks/{id}/chat
```

请求：
```json
{ "content": "采购价和销售价为什么差这么多？" }
```

响应（同步阻塞，等 Agent 跑完）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "reply": "根据原文，采购合同第 3 条约定单价 0.50 元，销售合同第 2 条约定 0.80 元，差额 0.30 元（60%）。建议核实是否存在中间商加价或采购价偏低漏报…",
    "toolCalls": [
      { "tool": "get_extraction", "args": { "contractId": 2001 } }
    ]
  }
}
```

### 2.5 历史消息

```
GET /api/tasks/{id}/messages?page=1&size=50
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 6,
    "items": [
      { "id": 1, "role": "USER",      "content": "采购价和销售价…", "createdAt": "..." },
      { "id": 2, "role": "ASSISTANT", "content": "根据原文…",       "createdAt": "..." }
    ]
  }
}
```

### 2.6 导出对比报告

```
GET /api/tasks/{id}/report?format=md
```

查询参数：
| 名称 | 类型 | 必填 | 说明 |
|---|---|---|---|
| format | enum | 否 | `md`（默认）/ `pdf` |

- `md`：直接返回 Markdown 文本（`Content-Type: text/markdown`）
- `pdf`：返回 `application/pdf` 二进制

### 2.7 重跑失败任务

```
POST /api/tasks/{id}/retry
```

仅任务状态为 `FAILED` 时可用。响应：同 2.1。

### 2.8 确认创建采购单

```
POST /api/tasks/{id}/confirm
```

仅任务状态为 `DONE` 时可用。确认后：
- 任务状态由 `DONE` 转为 `CONFIRMED`
- `confirmed_at` / `confirmed_by` 写入
- BUY 侧 extraction 的 `application_status` 由 `待提交` 升为 `已确认`
- `message` 改为 `已人工确认创建采购单`

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "status": "CONFIRMED",
    "applicationNo": "PA202607131430250123",
    "applicationStatus": "已确认",
    "confirmedAt": "2026-07-13T14:30:25"
  }
}
```

错误情况：
- 任务非 `DONE` 状态 → 400，`{ code: 1003, message: "仅 DONE 状态可确认创建采购单" }`
- 无权访问该任务 → 403

---

## 3. 管理端（ROLE_ADMIN）

### 3.1 概览统计

```
GET /api/admin/stats
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userCount": 28,
    "taskCount": 156,
    "todayLlmCalls": 47,
    "todayTokens": 184320,
    "tasksByStatus": { "PENDING": 3, "RUNNING": 1, "DONE": 124, "FAILED": 4, "CONFIRMED": 24 }
  }
}
```

### 3.2 用户列表

```
GET /api/admin/users?page=1&size=20&keyword=zhang
```

查询参数：
| 名称 | 类型 | 必填 |
|---|---|---|
| page | int | 否 |
| size | int | 否 |
| keyword | string | 否，匹配 username |
| enabled | bool | 否 |

响应：同 2.2 结构。

### 3.3 修改用户

```
PATCH /api/admin/users/{id}
```

请求（任选字段）：
```json
{ "enabled": false }
```
或
```json
{ "role": "ADMIN" }
```

响应：
```json
{ "code": 0, "message": "ok", "data": { "id": 7, "username": "zhangsan", "role": "ADMIN", "enabled": false } }
```

### 3.4 全量任务审计

```
GET /api/admin/tasks?page=1&size=20&userId=7&status=DONE&riskLevel=HIGH&from=2026-07-01&to=2026-07-13
```

查询参数：
| 名称 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page / size | int | 否 | |
| userId | long | 否 | 过滤用户 |
| status | enum | 否 | |
| riskLevel | enum | 否 | |
| from / to | date | 否 | `YYYY-MM-DD`，按 created_at |

响应：同 2.2。

### 3.5 任意任务详情

```
GET /api/admin/tasks/{id}
```
响应同 2.3。

### 3.6 风险规则列表

```
GET /api/admin/risk-rules
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [
      { "id": 1, "fieldKey": "purchaseTotalAmount", "operator": "PCT_GT", "thresholdValue": 0.05, "riskLevel": "HIGH",   "enabled": true,  "remark": "金额差 > 5%" },
      { "id": 2, "fieldKey": "quantity",            "operator": "NE",     "thresholdValue": 0,    "riskLevel": "HIGH",   "enabled": true,  "remark": "数量不一致" },
      { "id": 3, "fieldKey": "expectedDeliveryDate","operator": "GT",     "thresholdValue": 7,    "riskLevel": "MEDIUM", "enabled": true,  "remark": "日期差 > 7 天" },
      { "id": 4, "fieldKey": "paymentTerms",        "operator": "EQ",     "thresholdValue": "",   "riskLevel": "MEDIUM", "enabled": true,  "remark": "付款条款缺失" },
      { "id": 5, "fieldKey": "deliveryLocation",    "operator": "NE",     "thresholdValue": "",   "riskLevel": "MEDIUM", "enabled": true,  "remark": "交付地点不一致" }
    ]
  }
}
```

### 3.7 新建风险规则

```
POST /api/admin/risk-rules
```

请求：
```json
{
  "fieldKey": "purchaseUnitPrice",
  "operator": "PCT_GT",
  "thresholdValue": 0.10,
  "riskLevel": "MEDIUM",
  "enabled": true,
  "remark": "单价差 > 10%"
}
```

### 3.8 修改风险规则

```
PATCH /api/admin/risk-rules/{id}
```

请求体同 3.7（任选字段）。

### 3.9 删除风险规则

```
DELETE /api/admin/risk-rules/{id}
```

### 3.10 LLM 调用日志

```
GET /api/admin/llm-logs?page=1&size=20&userId=7&model=gpt-4o-mini&from=2026-07-13&to=2026-07-13
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 47,
    "items": [
      { "id": 9001, "taskId": 1001, "userId": 7, "model": "gpt-4o-mini", "promptTokens": 1240, "completionTokens": 380, "totalTokens": 1620, "durationMs": 4521, "status": "OK", "createdAt": "..." }
    ]
  }
}
```

---

## 4. 字段对照（API 字段名 ↔ DB 字段名 ↔ JSON 字段名）

| API 字段（camelCase） | DB 字段（snake_case） | 抽取 JSON 字段 |
|---|---|---|
| supplierName | supplier_name | supplier_name |
| itemName | item_name | item_name |
| itemModel | item_model | item_model |
| unit | unit | unit |
| quantity | quantity | quantity |
| purchaseUnitPrice | purchase_unit_price | purchase_unit_price |
| purchaseTotalAmount | purchase_total_amount | purchase_total_amount |
| expectedDeliveryDate | expected_delivery_date | expected_delivery_date |
| paymentTerms | payment_terms | payment_terms |
| deliveryLocation | delivery_location | delivery_location |
| applicationTitle | application_title | —（系统填） |
| applyDate | apply_date | —（系统填） |
| applicationType | application_type | —（系统填） |
| currency | currency | —（系统填） |
| applicationStatus | application_status | —（系统填） |

---

## 5. 调用示例（curl）

```bash
# 登录
TOKEN=$(curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

# 创建任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=7月螺丝采购" \
  -F "buyFile=@./purchase.docx" \
  -F "sellFile=@./sales.docx"

# 追问
curl -X POST http://localhost:8080/api/tasks/1001/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"content":"采购价和销售价为什么差这么多？"}'
```

---

## 6. Swagger UI

后端启动后访问：`http://localhost:8080/swagger-ui.html`
