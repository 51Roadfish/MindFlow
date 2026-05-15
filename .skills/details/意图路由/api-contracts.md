# 意图路由 — API Contracts

意图路由无独立 API 端点。作为内部服务被 `AIChatService` 调用。

## 内部接口

### `IntentRouterService.analyze(String userMessage) → IntentResult`

**输入：**
- `userMessage`: 用户原始提问文本

**输出：**
```java
public record IntentResult(String intent, String query) {}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| intent | String | `"CHAT"` / `"SEARCH"` / `"WRITE"` |
| query | String | 优化后的检索词（SEARCH 意图时）或原问题 |

**响应 JSON 示例（由 LLM 返回）：**
```json
{"intent": "SEARCH", "query": "微服务架构 部署策略"}
```
