# Skill: 意图路由（Agentic RAG）

## 路由判定

### 应该读这个 Skill 的场景
- 修改/优化意图分类逻辑（CHAT / SEARCH / WRITE / REFUSE）
- 修改 Embedding + kNN 分类模板或阈值
- 修改 Guardrail 正则规则（注入检测）
- 修改意图路由后的调度逻辑（含流式和非流式两条路径）
- 新增/修改意图模板示例

### 不应该读这个 Skill 的场景
- 修改 RAG 问答流程（应读 Skill: AI 语义搜索与 RAG 问答）
- 修改 AI 写作（应读 Skill: AI 写作助手）
- 修改笔记 CRUD（应读 Skill: 笔记管理）
- 修改前端页面（无关）

## 业务领域知识

### 背景
意图路由是 MindFlow AI 问答的"入口大脑"。用户的每条提问先经过意图路由服务，由 **Embedding + kNN** 分类器判断用户意图（闲聊 / 检索知识库 / 写作任务 / 拒绝），然后再分发给对应的处理模块。这是一个轻量级的 Agentic RAG 实现——无额外 LLM 推理开销，延迟约 30ms。

### 核心概念
- **Embedding + kNN 分类**：用 bge-large-zh-v1.5（或 text-embedding-ada-002）将用户 query 编码为向量，与预计算的模板向量计算余弦相似度，取最近邻意图
- **余弦相似度阈值（0.45）**：低于此阈值的 query 被判定为 REFUSE，不调用任何 LLM
- **IntentTemplates**：每个意图 12-14 条示例文本，启动时批量计算 embedding 缓存
- **Guardrail 预过滤**：正则匹配注入/越狱模式，在分类前拦截
- **容错回退**：Embedding 调用失败时默认返回 CHAT 意图，保证用户体验不中断

### 意图定义

| 意图常量 | 值 | 路由目标 | 触发场景 |
|---------|-----|---------|---------|
| `INTENT_CHAT` | `"CHAT"` | `chatModel.call(question)` 直接回答 | 闲聊、通用知识问答、问候 |
| `INTENT_SEARCH` | `"SEARCH"` | 检索 pgvector → RAG 回答 | "我笔记里关于..."、"根据我的知识库..." |
| `INTENT_WRITE` | `"WRITE"` | `AIWriteService.process()` | "帮我写..."、"续写..."、"润色..." |
| `INTENT_REFUSE` | `"REFUSE"` | 返回固定拒绝消息，不调 LLM | 注入攻击、越狱、违法请求、无关话题 |

### 关键业务规则与不变量

每条规则按 `规则 → 实现 → 位置 → 约束` 格式组织。

1. **意图路由由 Guardrail + kNN 两级组成**
   → Guardrail 先正则检测注入/越狱（微秒级），通过后再走 Embedding + kNN 分类
   → 实现: `IntentRouterService.analyze()` — guardrailService.check() → intentClassifier.classify()
   → 位置: `service/IntentRouterService.java:17-30` | `service/GuardrailService.java:19-47`(正则列表) | `service/IntentClassifier.java:28-64`(kNN 分类)
   → 约束: 任一阶段命中 REFUSE 即不调 LLM，直接返回拒绝消息

2. **模板向量在启动时预计算**
   → `IntentClassifier.init()` 调用 `embeddingModel.embed(texts)` 批量生成，缓存在内存中
   → 位置: `service/IntentClassifier.java:18-27`
   → 约束: 修改 IntentTemplates 需重启服务；维度随 Embedding 模型自动适配

3. **SEARCH 意图的 query 即为用户原始输入**
   → kNN 分类不再像 LLM 方案那样提取/优化检索词，直接传用户原文
   → 实现: `IntentClassifier.classify()` 第 51-52 行 — "if SEARCH, query = userMessage"
   → 位置: `service/IntentClassifier.java:51-52`
   → 约束: 向量检索依赖 Embedding 模型的语义理解来匹配，不再做关键词提取

4. **Embedding 调用失败时默认回退 CHAT 意图**
   → 任何异常（网络超时、模型不可用）不回抛，保证用户体验
   → 实现: catch 块 — `return new IntentResult(INTENT_CHAT, userMessage)`
   → 位置: `service/IntentClassifier.java:59-62`
   → 日志: `log.error("IntentClassifier: embedding failed, fallback to CHAT", e)`

5. **REFUSE 不调用任何 LLM**
   → 拒绝消息为固定字符串，不经过 ChatModel
   → 实现: `AIChatService.chat()` 和 `chatStream()` 中 `case INTENT_REFUSE:` 直接赋值
   → 位置: `service/AIChatService.java:40-41`(阻塞) | `service/AIChatService.java:97-98`(流式)
   → 约束: 修改拒绝文案只需改一处常量 `REFUSE_MESSAGE`

6. **ChatModel/ObjectMapper 不再用于意图路由**
   → 原 LLM 方案已完全移除，IntentRouterService 不再依赖 ChatModel
   → 位置: `service/IntentRouterService.java` — 仅依赖 IntentClassifier + GuardrailService

## 核心代码流程

### 意图分析流程

```
用户消息
  → IntentRouterService.analyze(question)
    → GuardrailService.check(question)        ← 正则检测注入/越狱
      → 命中: 返回 IntentResult(REFUSE, question)
    → IntentClassifier.classify(question)      ← Embedding + kNN
      → embeddingModel.embed(question)         ← 编码为向量
      → 余弦相似度 vs 所有模板向量
      → 最佳匹配 < 0.45 → REFUSE
      → 最佳匹配 >= 0.45 → 对应意图
      → Embedding 异常 → CHAT（兜底）
    ← 返回 IntentResult(intent, query)
  ← AIChatService 根据 intent 调度后续处理
```

关键代码位置：
- IntentRouterService: `service/IntentRouterService.java:17-30`
- IntentClassifier: `service/IntentClassifier.java:28-64`
- GuardrailService: `service/GuardrailService.java:19-47`
- IntentTemplates: `service/IntentTemplates.java` — 38 条模板 + 阈值常量
- IntentResult: `dto/IntentResult.java:3` — `public record IntentResult(String intent, String query) {}`
- ChatService 调度（阻塞）: `service/AIChatService.java:35-84`
- ChatService 调度（流式 SSE）: `service/AIChatService.java:93-124`

## 变更指南

### 修改时机
- 新增业务意图（如 "REVIEW"）→ 修改 IntentTemplates 添加模板 + IntentRouterService 常量 + AIChatService 调度
- 优化分类准确率 → 修改 IntentTemplates 中的示例文本（增加/调整模板）
- 调整阈值 → 修改 `IntentTemplates.SIMILARITY_THRESHOLD`
- 增强 Guardrail → 修改 `GuardrailService.BLOCKED_PATTERNS` 增加正则
- 切换 Embedding 模型 → 自动适配（模板向量在 PostConstruct 时重建）

### 影响检查清单
- [ ] 修改 IntentTemplates → 需要重启服务（启动时预计算 embedding）
- [ ] 修改 Guardrail 正则 → 需要重启，注意误报率
- [ ] 修改阈值 → 过低会增加误召回（不该回答的问题被放行），过高会增加误拒绝
- [ ] 修改意图常量 → AIChatService 的 switch 分支需同步更新
- [ ] 切换 Embedding 模型 → 维度变化自动适配，无需改代码

### 常见变更模式

**新增意图类型（如 REVIEW）**：
```java
// 1. IntentRouterService 添加常量
public static final String INTENT_REVIEW = "REVIEW";

// 2. IntentTemplates 添加模板示例
new IntentTemplate(INTENT_REVIEW, "帮我复习一下最近记的笔记"),
new IntentTemplate(INTENT_REVIEW, "根据我的笔记出几道题"),

// 3. AIChatService 添加 switch 分支
case IntentRouterService.INTENT_REVIEW:
    answer = reviewService.handleReviewRequest(question, userId);
    break;
```

**调整分类阈值**：
```java
// IntentTemplates.java
public static final double SIMILARITY_THRESHOLD = 0.50d; // 调高 -> 更严格
```

**增强 Guardrail 规则**：
```java
// GuardrailService.java BLOCKED_PATTERNS
Pattern.compile("请(?:删除|修改|覆盖).*(?:记忆|知识|数据)", Pattern.CASE_INSENSITIVE)
```
