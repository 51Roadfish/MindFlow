# Skill: 意图路由（Agentic RAG）

## 路由判定

### 应该读这个 Skill 的场景
- 修改/优化意图分类逻辑（CHAT / SEARCH / WRITE）
- 修改意图分析 Prompt
- 新增意图类型
- 修改意图路由后的调度逻辑（含流式和非流式两条路径）
- 优化检索词提取（query 提取策略）

### 不应该读这个 Skill 的场景
- 修改 RAG 问答流程（应读 Skill: AI 语义搜索与 RAG 问答）
- 修改 AI 写作（应读 Skill: AI 写作助手）
- 修改笔记 CRUD（应读 Skill: 笔记管理）
- 修改前端页面（无关）

## 业务领域知识

### 背景
意图路由是 MindFlow AI 问答的"入口大脑"。用户的每条提问先经过意图路由服务，由 LLM 判断用户意图（闲聊 / 检索知识库 / 写作任务），然后再分发给对应的处理模块。这是一个轻量级的 Agentic RAG 实现——模型自主决定是否需要检索。

### 核心概念
- **LLM-as-Classifier**：用大语言模型代替传统分类器做意图识别
- **检索词提取（Query Extraction）**：当意图为 SEARCH 时，模型从用户提问中提取/优化出最有利于向量检索的关键词
- **JSON Mode**：意图路由要求模型严格返回 JSON 格式，通过 System Prompt 约束
- **容错回退**：解析失败时默认返回 CHAT 意图，保证用户体验不中断

### 意图定义

| 意图常量 | 值 | 路由目标 | 触发场景 |
|---------|-----|---------|---------|
| `INTENT_CHAT` | `"CHAT"` | `chatModel.call(question)` 直接回答 | 闲聊、通用知识问答、问候 |
| `INTENT_SEARCH` | `"SEARCH"` | 检索 pgvector → RAG 回答 | "我笔记里关于..."、"根据我的知识库..." |
| `INTENT_WRITE` | `"WRITE"` | `AIWriteService.process()` | "帮我写..."、"续写..."、"润色..." |

### 关键业务规则与不变量

每条规则按 `规则 → 实现 → 位置 → 约束` 格式组织。

1. **意图路由只负责分析，不负责执行**
   → 返回 `IntentResult(intent, query)`，由 `AIChatService` 根据 intent 值 switch 调度到不同模块
   → 实现: `IntentRouterService.analyze()` → `IntentResult` → `AIChatService.chat()` 第 34 行 switch（阻塞）/ `AIChatService.chatStream()` 第 90 行 switch（流式）
   → 位置: `service/IntentRouterService.java:36-71` | `service/AIChatService.java:34-72`(阻塞) + `service/AIChatService.java:86-119`(流式)
   → 约束: 新增意图需同步修改四处 — IntentRouterService 常量 + Prompt + AIChatService.chat() switch + AIChatService.chatStream() switch

2. **SEARCH 意图必须附带优化后的检索词 query**
   → 模型需从用户提问中提取核心关键词，去除语气词和冗余修饰，提升向量检索命中率
   → 实现: IntentRouterService Prompt 第 45-46 行 — "如果意图是 SEARCH，请从用户输入中提取或优化出一个'最佳检索词'(query)，滤除语气词"
   → 位置: `service/IntentRouterService.java:45-46`（Prompt 文本）

3. **LLM 响应必须为严格 JSON，非 JSON 内容触发容错回退**
   → Prompt 第 51 行强制要求"必须且只能返回合法的 JSON 字符串"，但 LLM 可能输出 ```json 包裹或其他噪音
   → 实现: 第 63 行 `response.replaceAll("```json", "").replaceAll("```", "").trim()` 清理后解析
   → 位置: `service/IntentRouterService.java:63-64`
   → 约束: 若清理后仍解析失败，走第 68-70 行容错回退

4. **JSON 解析失败且清理失败时，默认回退 CHAT 意图**
   → 任何异常（LLM 超时、JSON 格式错误、模型返回非 JSON）均不回抛，保证用户体验不中断
   → 实现: catch 块 — `return new IntentResult(INTENT_CHAT, userMessage)`
   → 位置: `service/IntentRouterService.java:67-70`
   → 日志: `log.error("分析用户意图失败，将回退到默认的 CHAT 意图", e)` 第 66 行

5. **去除 ```json 标记和 Markdown 包裹**
   → LLM 倾向于用 Markdown code block 包裹 JSON，需在解析前剥离
   → 实现: 第 63 行正则清理
   → 位置: `service/IntentRouterService.java:63`

6. **无状态，每次分析独立**
   → `IntentRouterService` 无成员变量，`analyze()` 每次调用创建新的 Prompt 实例
   → 约束: 不支持对话上下文理解，每条消息独立分析

## 核心代码流程

### 意图分析流程

```
AIChatService.chat(userId, question)
  → IntentRouterService.analyze(question)

    → 构造 SystemMessage(意图分析 Prompt)
        "你是一个意图识别引擎。请根据用户的输入分析他们的真实意图...
         1. CHAT：日常对话...
         2. SEARCH：明确询问个人的笔记...
         3. WRITE：要求大模型进行续写、润色...
         输出示例：{"intent": "SEARCH", "query": "Spring Boot 核心特性"}
         警告：必须且只能返回合法的 JSON 字符串..."

    → 构造 UserMessage(question)
    → chatModel.call(new Prompt([systemMsg, userMsg]))
    → 清理 response 中的 ```json 标记
    → objectMapper.readValue(response, IntentResult.class)

    → 成功: 返回 IntentResult(intent, query)
    → 失败: 记录 log，返回 IntentResult(INTENT_CHAT, userMessage)  ← 容错回退

  ← IntentRouterService 返回 IntentResult
  ← AIChatService 根据 intent 调度后续处理
```

关键代码位置：
- IntentRouterService: `service/IntentRouterService.java:36-71`
- IntentResult: `dto/IntentResult.java:3` — `public record IntentResult(String intent, String query) {}`
- ChatService 调度（阻塞）: `service/AIChatService.java:25-80`
- ChatService 调度（流式 SSE）: `service/AIChatService.java:86-119`
- Controller SSE 端点: `controller/AIController.java:52-82`（返回 `SseEmitter`）

## 变更指南

### 修改时机
- 新增业务意图（如 "REVIEW"、"EXAM"）→ 修改 IntentRouterService 常量 + Prompt + AIChatService 调度
- 优化意图识别准确率 → 修改 System Prompt（增加 few-shot 示例、强化边界描述）
- 修改 query 提取策略 → 修改 Prompt 中对 query 的要求描述
- JSON 解析容错升级 → 修改清理逻辑或使用 `AiJsonHelper` 替代

### 影响检查清单
- [ ] 修改意图类型常量 → `AIChatService.chat()` 中 switch 分支是否同步更新
- [ ] 修改 Prompt → 是否需要更新前端显示的意图标识
- [ ] 修改 JSON 解析 → 测试各种 LLM 返回格式异常情况（多余文本、非标准 JSON）
- [ ] 新增意图 → 是否需要新的 Service/模块来执行该意图
- [ ] 删除意图 → 现有调用该意图的用户提问如何处理

### 常见变更模式

**新增意图类型（如 REVIEW）**：
```java
// 1. IntentRouterService 添加常量
public static final String INTENT_REVIEW = "REVIEW";

// 2. 修改 Prompt 添加分类描述
// "4. REVIEW：要求对笔记进行复习、出题测试..."

// 3. AIChatService 添加 switch 分支
case IntentRouterService.INTENT_REVIEW:
    // 转发到 ReviewService
    answer = reviewService.handleReviewRequest(question, userId);
    break;
```

**优化 query 提取**：
```java
// 在 Prompt 中强化对 query 的要求
// "如果意图是 SEARCH，提取出的检索词应：
//  - 去除人称代词和语气词
//  - 保留核心名词和关键修饰词
//  - 长度控制在 3-15 个中文词
//  - 示例：'我笔记里关于微服务架构的部署策略' → '微服务架构 部署策略'"
```

**使用 AiJsonHelper 增强容错**：
```java
// 替代 objectMapper.readValue，利用 AiJsonHelper 的自动修复能力
String cleaned = response.replaceAll("```[jJ][sS][oO][nN]?", "").replaceAll("```", "").trim();
IntentResult result = AiJsonHelper.parse(cleaned, IntentResult.class);
```
