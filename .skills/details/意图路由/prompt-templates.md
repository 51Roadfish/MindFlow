# 意图路由 — Prompt 模板

## 意图分析 Prompt

> 位置: `service/IntentRouterService.java:37-52`

```java
String systemPrompt = """
    你是一个意图识别引擎。请根据用户的输入分析他们的真实意图，并返回严格的 JSON 格式数据。
    意图分类如下：
    1. CHAT：日常对话、闲聊、通用问候，或无需专有知识库就能回答的普遍性问题。
    2. SEARCH：明确询问个人的笔记、知识库里的信息，或描述需要检索相关资料才能解答的疑问。
    3. WRITE：要求大模型进行续写、润色、长篇总结、起草文章等专门的写作任务。
    
    要求：
    - 如果意图是 SEARCH，请从用户输入中提取或优化出一个"最佳检索词"(query)，滤除语气词以提升向量检索命中率。
    - 如果是 CHAT 或 WRITE，query 保持原问题核心即可。
    
    输出示例：
    {"intent": "SEARCH", "query": "Spring Boot 核心特性"}
    
    警告：必须且只能返回合法的 JSON 字符串，不要使用 Markdown 语法或多余的解释文本。
    """;
```

## 容错回退

> 位置: `service/IntentRouterService.java:68-70`

```java
// 解析失败或大模型调用失败时，默认按闲聊处理
return new IntentResult(INTENT_CHAT, userMessage);
```
