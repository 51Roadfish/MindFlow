# Skill: AI 写作助手

## 路由判定

### 应该读这个 Skill 的场景
- 修改 AI 写作功能（续写、润色、摘要）
- 新增写作操作类型（如翻译、扩写、缩写）
- 修改写作 Prompt 模板
- 修改前端 AI 写作页面

### 不应该读这个 Skill 的场景
- 修改笔记 CRUD（应读 Skill: 笔记管理）
- 修改语义搜索或 RAG 问答（应读 Skill: AI 语义搜索与 RAG 问答）
- 修改意图路由（应读 Skill: 意图路由）
- 修改复习/出题逻辑（应读 Skill: AI 复习与出题）

## 业务领域知识

### 背景
AI 写作助手提供基于 LLM 的文本处理能力，支持三种预设操作：续写（continue）、润色（polish）、摘要（summarize）。用户选择操作类型并输入原始文本，系统调用 LLM 处理后返回结果。

### 核心概念
- **操作指令**：continue / polish / summarize，通过 `action` 参数区分
- **无 RAG 参与**：AI 写作不依赖用户笔记库，仅基于 LLM 自身能力
- **无意图路由**：写作请求直接走 `AIWriteService`，不经 `IntentRouterService`（但聊天中的写作意图会通过意图路由转发过来）

### 关键业务规则与不变量

每条规则按 `规则 → 实现 → 位置 → 约束` 格式组织。

1. **支持三种预设操作**
   → `continue`（续写）、`polish`（润色）、`summarize`（100 字以内摘要）
   → 实现: `AIWriteService.process()` — switch 分支匹配 action 字符串，拼接对应英文 Prompt 后调 LLM
   → 位置: `service/AIWriteService.java:17-22`
   → 约束: action 值在前端硬编码（`AIWrite/index.tsx:26-28`），新增需两端同步

2. **不支持的 action 立即抛异常**
   → switch default 分支抛出 `IllegalArgumentException`
   → 实现: `AIWriteService.java` 第 21 行 — `throw new IllegalArgumentException("Unsupported action")`
   → 位置: `service/AIWriteService.java:21`
   → 约束: `GlobalExceptionHandler` 捕获后返回 400 + `{ "error": e.getMessage() }`

3. **输出纯文本，无格式校验**
   → LLM 返回的 String 直接包装为 `{ "result": "..." }` 返回，不校验 Markdown 合法性
   → 实现: `AIController.write()` 第 51 行 — `ResponseEntity.ok(Map.of("result", response))`
   → 位置: `controller/AIController.java:51`

4. **无对话历史，每次请求独立**
   → `AIWriteService` 无成员变量，process() 不读写任何持久化存储
   → 约束: 不支持多轮连续写作，每次需用户提供完整上下文

5. **无用户归属检查，通用功能**
   → process() 方法不接收 userId 参数，不涉及数据隔离
   → 对比: `AIController.write()` 虽然接收 `Authentication`，但仅用于日志审计，不做权限校验
   → 位置: `controller/AIController.java:48-53` — authentication 参数存在但未被使用

## 核心代码流程

### AI 写作流程

```
用户 POST /api/ai/write { "action": "continue", "content": "..." }
  → AIController.write(request, authentication)
    → AIWriteService.process(action, content)
      → switch(action.toLowerCase()):
          "continue"  → "Please continue writing the following markdown note: " + content
          "polish"    → "Polish the following markdown content to be more fluent and professional: " + content
          "summarize" → "Summarize the following markdown content in under 100 words: " + content
          default     → throw IllegalArgumentException
      → chatModel.call(prompt)  ← LLM 生成
    ← 返回 String
  ← 返回 { "result": "..." }
```

关键代码位置：
- Controller: `controller/AIController.java:48-53`
- Service: `service/AIWriteService.java:15-24`

### 从 AI 聊天中的写作请求

当用户在聊天输入写作相关请求时：
```
用户 POST /api/ai/chat { "question": "帮我续写这篇文章..." }
  → IntentRouterService.analyze() → INTENT_WRITE
  → AIChatService.chat() → aiWriteService.process("continue", question)
  → 返回 AIChatResponse(intent="WRITE", answer=result, sources=[])
```

关键代码位置：
- AIChatService 转发: `service/AIChatService.java:37-39`

## 变更指南

### 修改时机
- 需要新增写作类型（如翻译、扩写）→ 修改 `AIWriteService.process()`
- 需要优化写作质量 → 修改 Prompt 模板（使用 SystemMessage + UserMessage 结构）
- 需要支持多轮写作对话 → 需重构为带上下文的状态管理

### 影响检查清单
- [ ] 修改 action 类型 → 前端选择器是否需要同步更新
- [ ] 修改 Prompt → 输出格式是否变化（如需要 Markdown）
- [ ] 从聊天中触发的写作（INTENT_WRITE）→ 确保意图路由正确识别
- [ ] 新增操作类型 → 是否需要在前端新增枚举选项

### 常见变更模式

**新增操作类型（如翻译）**：
```java
// AIWriteService.process()
case "translate":
    prompt = "Translate the following content to English, keep markdown format: " + content;
    break;
```

**重构 Prompt 为结构化格式**：
```java
// 从字符串拼接改为 SystemMessage + UserMessage
SystemMessage sysMsg = new SystemMessage("You are a writing assistant. Continue the following markdown note naturally.");
UserMessage userMsg = new UserMessage(content);
String response = chatModel.call(new Prompt(List.of(sysMsg, userMsg)));
```
