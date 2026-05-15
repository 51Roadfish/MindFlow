# AI 语义搜索与 RAG 问答 — Prompt 模板

## RAG 问答 Prompt

> 位置: `service/AIChatService.java:52-55`

```java
String prompt = "你是一个知识库 AI 助手。请基于以下提取自个人知识库的笔记内容，来回答用户的原始问题。\n"
        + "如果给出的笔记无法解答该问题，请如实回答说知识库中未包含相关内容。\n\n"
        + "【笔记内容参考】:\n" + context + "\n\n"
        + "【用户原始问题】: " + question;
```

**变量说明：**
- `context`：检索到的笔记 Document 拼接，格式为 `[noteTitle]: chunkContent \n\n`
- `question`：用户的原始问题
