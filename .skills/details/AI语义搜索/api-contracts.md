# AI 语义搜索与 RAG 问答 — API Contracts

## POST /api/ai/search

语义搜索。直接进行向量相似度检索，不经过 LLM。

**Request Body:**
```json
{
  "query": "string (required, 自然语言搜索词)"
}
```

**Response 200:**
```json
[
  {
    "id": "...",
    "content": "Spring Boot 的核心特性包括自动配置...",
    "metadata": {
      "userId": "1",
      "noteId": "10",
      "noteTitle": "Spring Boot 学习笔记",
      "chunkIndex": 0
    }
  }
]
```

**说明：** 返回的格式为 Spring AI Document 列表的序列化，实际前端按 `title` 和 `content` 字段展示。

## POST /api/ai/chat

AI 问答。先意图路由分析，再根据意图执行 RAG 检索或直接 LLM 回答。

**Request Body:**
```json
{
  "question": "string (required)"
}
```

**Response 200 (CHAT 意图):**
```json
{
  "intent": "CHAT",
  "answer": "你好！我是 MindFlow AI 助手。",
  "sources": []
}
```

**Response 200 (SEARCH 意图):**
```json
{
  "intent": "SEARCH",
  "answer": "根据你的笔记，Spring Boot 的核心特性包括：1. 自动配置...",
  "sources": ["Spring Boot 学习笔记", "Spring Boot 进阶"]
}
```

**Response 200 (WRITE 意图):**
```json
{
  "intent": "WRITE",
  "answer": "接续你的内容，微服务架构的部署策略包括...",
  "sources": []
}
```
