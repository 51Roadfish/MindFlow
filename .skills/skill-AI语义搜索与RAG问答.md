# Skill: AI 语义搜索与 RAG 问答

## 路由判定

### 应该读这个 Skill 的场景
- 修改语义搜索逻辑（向量相似度检索）
- 修改 RAG 问答流程（检索 → 拼接 Prompt → LLM 回答）
- 修改 VectorStore 配置（pgvector 连接、索引类型、距离算法）
- 修改搜索结果排序或过滤
- 修改向量检索的 topK、filterExpression
- 优化检索质量或性能

### 不应该读这个 Skill 的场景
- 修改笔记 CRUD（应读 Skill: 笔记管理）
- 修改意图路由（应读 Skill: 意图路由）
- 修改 AI 写作（应读 Skill: AI 写作助手）
- 修改前端页面样式（无关）

## 业务领域知识

### 背景
语义搜索和 RAG 问答是 MindFlow 的两大 AI 核心能力。两者都基于 **pgvector** 向量数据库，通过将用户查询转换为向量，在笔记的向量空间中检索最相似的内容片段。

### 核心概念
- **pgvector**：PostgreSQL 的向量扩展插件，存储 1024 维浮点向量（由 `BAAI/bge-large-zh-v1.5` 嵌入模型生成）
- **COSINE_DISTANCE**：向量距离算法，衡量语义相似度
- **HNSW 索引**：pgvector 的近似最近邻搜索索引，提升检索性能
- **Filter Expression**：Spring AI 的过滤表达式，如 `"userId == '123'"` 确保跨用户隔离
- **TopK**：最多返回的匹配文档数（搜索=10，问答=5）
- **RAG (Retrieval-Augmented Generation)**：检索增强生成，先检索相关笔记，再让 LLM 基于笔记内容回答问题

### 关键业务规则与不变量

每条规则按 `规则 → 实现 → 位置 → 约束` 格式组织。

1. **向量数据与用户绑定**
   → 每个 Document 的 metadata 携带 `userId`，检索时通过 filterExpression 强制过滤
   → 实现: `EmbeddingServiceImpl.embedAndStore()` 注入 `userId`（第 28 行）→ `VectorStoreService.similaritySearch()` 附加 `withFilterExpression("userId == '" + userId + "'")`（第 22 行）
   → 位置: `service/EmbeddingServiceImpl.java:28` | `service/VectorStoreService.java:20-23`
   → 约束: filterExpression 为字符串拼接，注意 SQL 注入（userId 取自后端认证上下文，安全）

2. **语义搜索不经 LLM，直接返回 Document 列表**
   → 搜索仅做向量相似度检索，不经过 AI 模型
   → 实现: `AIController.search()` → `vectorStoreService.similaritySearch()`
   → 位置: `controller/AIController.java:37-38`
   → 约束: 返回的 List<Document> 序列化格式与前端 `AISearch/index.tsx:42-43` 中的 `item.title || item.metadata?.noteTitle` 兼容

3. **RAG 问答必须在返回中附带引用来源**
   → 回答时列出匹配的笔记标题，用户可溯源
   → 实现: `AIChatService.chat()` SEARCH 分支第 59-63 行 — `docs.stream().map(d → noteTitle).distinct()`
   → 位置: `service/AIChatService.java:59-63`
   → 约束: sources 为空列表时前端不显示引用区块（`AIChat/index.tsx:53-56`

4. **RAG 检索为空时返回兜底消息，不调 LLM**
   → 无检索结果时直接返回固定字符串，避免模型硬答
   → 实现: `AIChatService.chat()` 第 45-47 行
   → 位置: `service/AIChatService.java:45-47`

5. **pgvector 连接独立于业务 MySQL 连接池**
   → 使用独立 HikariDataSource，配置参数独立
   → 实现: `PgVectorConfig.vectorStore()` — 新建 `HikariDataSource` 指向 PostgreSQL
   → 位置: `config/PgVectorConfig.java:52-65`
   → 注意: 每次 `vectorStore()` Bean 创建都新建 DataSource，与 `pgJdbcTemplate()` 的 DataSource 不共享

6. **Embedding 模型维度固定 1024**
   → 当前使用 `BAAI/bge-large-zh-v1.5`，输出 1024 维向量
   → 配置: `application.yml:47` — `dimensions: 1024`
   → 约束: 切换 Embedding 模型需确认新模型维度，且需要重建 pgvector 索引

### 数据模型（pgvector）
向量表由 Spring AI PgVectorStore 自动管理（`initialize-schema: true`），表结构如下：
- `vector_store`：包含 id、content、metadata、embedding 列
- metadata JSON 字段：`{ "userId": "123", "noteId": "456", "noteTitle": "...", "chunkIndex": 0 }`

## 核心代码流程

### 语义搜索流程

```
用户 POST /api/ai/search { "query": "..." }
  → AIController.search(request, authentication)
    → getUserId(authentication)  ← 解析当前用户 ID
    → VectorStoreService.similaritySearch(userId, query, topK=10)
      → vectorStore.similaritySearch(
          SearchRequest.query(query).withTopK(10)
            .withFilterExpression("userId == '" + userId + "'"))
      ← 返回 List<Document>  (按相似度降序排列)
  ← 返回 Document 列表 [{ content, metadata }, ...]
```

关键代码位置：
- Controller: `controller/AIController.java:33-39`
- VectorStoreService: `service/VectorStoreService.java:18-24`

### RAG 问答流程（阻塞）

```
用户 POST /api/ai/chat { "question": "..." }
  → AIController.chat(request, authentication)
    → AIChatService.chat(userId, question)
      → IntentRouterService.analyze(question)  ← 意图分析（见 Skill: 意图路由）

      当意图 = SEARCH:
      → VectorStoreService.similaritySearch(userId, intentResult.query(), topK=5)
      → 拼接上下文: "[noteTitle]: chunkContent \n\n [noteTitle2]: ..."
      → 构造 RAG Prompt:
          System: "你是一个知识库 AI 助手。请基于以下提取自个人知识库的笔记内容..."
          User: context + "\n\n【用户原始问题】: " + question
      → chatModel.call(prompt)  ← LLM 生成回答
      → 收集来源: docs.stream().map(d → noteTitle).distinct()
      → 返回 AIChatResponse(intent="SEARCH", answer, sources=[...])

      当意图 = CHAT:
      → chatModel.call(question)  ← 直接 LLM 回答，不经 RAG
      → 返回 AIChatResponse(intent="CHAT", answer, sources=[])

      当意图 = WRITE:
      → aiWriteService.process("continue", question)  ← 委派给写作服务
      → 返回 AIChatResponse(intent="WRITE", answer, sources=[])
```

### RAG 问答流程（流式 SSE）

```
用户 POST /api/ai/chat/stream { "question": "..." }
  → AIController.chatStream(request, authentication)
    → 返回 SseEmitter(180s timeout)
    → AIChatService.chatStream(userId, question)
      → IntentRouterService.analyze(question)  ← 同阻塞流程

      当意图 = SEARCH:
      → VectorStoreService.similaritySearch(userId, intentResult.query(), topK=5)
      → 拼接 RAG Prompt（同上）
      → streamingChatModel.stream(new Prompt(ragPrompt))
        → Flux<ChatResponse> 逐 token 发出
      → 每个 token 通过 SseEmitter.event().data(token) 推给客户端
      → 最终发送 data:[DONE] 表示结束

      当意图 = CHAT:
      → streamingChatModel.stream(new Prompt(question))
        → Flux<ChatResponse> 逐 token 发出

      当意图 = WRITE:
      → 阻塞调用 aiWriteService.process()，结果作为单 token 发送

    → 前端 response.body.getReader() 读取 ReadableStream
    → 按 \n\n 分割 SSE 帧，解析 data: 前缀
    → 逐 token setMessages() 更新 UI（打字机效果）
    → 收到 data:[DONE] 停止读取
```

SSE 格式：
```
data:<token1>\n\n
data:<token2>\n\n
data:[DONE]\n\n
```

关键代码位置：
- Controller（阻塞）: `controller/AIController.java:43-48`
- Controller（流式 SSE）: `controller/AIController.java:52-82` — 返回 `SseEmitter`，Nginx 需配置 `proxy_http_version 1.1` + `proxy_buffering off`
- AIChatService（阻塞）: `service/AIChatService.java:29-84`
- AIChatService（流式 SSE）: `service/AIChatService.java:86-119`
- 前端流式解析: `pages/AIChat/index.tsx:56-85` — `ReadableStream.getReader()` + SSE 帧解析
- VectorStoreService: `service/VectorStoreService.java:18-24`

### PgVectorStore 配置

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        host: localhost
        port: 5432
        database: mindflow
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1024
        initialize-schema: true
```

关键代码位置：
- Config: `config/PgVectorConfig.java:12-66`

## 变更指南

### 修改时机
- 需要切换 Embedding 模型 → 修改应用配置 + 确认 dimensions 是否变化
- 需要修改检索策略（如 Rerank、Hybrid Search）→ 修改 `VectorStoreService` 或新增检索组件
- 需要优化 RAG Prompt → 修改 `AIChatService.chat()` 中的 Prompt 模板
- 需要调整 TopK 值 → 修改 `similaritySearch` 调用参数

### 影响检查清单
- [ ] 修改 VectorStore 配置 → 是否影响现有索引数据？需要重建索引？
- [ ] 修改 Embedding 模型 → dimensions 是否变化？需重建向量库
- [ ] 修改 RAG Prompt → 回答质量是否退化？需要 A/B 测试
- [ ] 修改 Filter Expression → 用户数据隔离是否完整
- [ ] 修改 pgvector 连接 → 影响搜索 + RAG 问答 + 异步向量化三条路径

### 常见变更模式

**SSE 流式输出**：
后端使用 `SseEmitter`（而非 `Flux<String>` + `TEXT_EVENT_STREAM_VALUE`），由 Spring MVC 原生处理 SSE 格式。每个 `emitter.send(SseEmitter.event().data(token))` 生成 `data:token\n\n`。需确保：
- Nginx 配置 `proxy_http_version 1.1` + `proxy_buffering off`
- `SseEmitter` 设置合理 timeout（当前 180s）
- 注册 `onCompletion/onTimeout/onError` 回调清理 Flux 订阅

**增加 Rerank 重排序**：
```java
// 在 VectorStoreService 中新增方法
public List<Document> similaritySearchWithRerank(Long userId, String query, int topK) {
    List<Document> docs = similaritySearch(userId, query, topK * 2);
    // 调用 Rerank API 重新排序
    return reranker.rerank(query, docs).subList(0, topK);
}
```

**切换到 Hybrid Search (向量 + 关键词)**：
```java
// 需要同时进行 pgvector 搜索和 MySQL 全文搜索，然后融合结果
```

**修改 RAG Prompt 模板**：
```java
// 在 AIChatService.chat() 中修改 Prompt
String prompt = """
    你是一个知识库 AI 助手，名叫 MindFlow。请严格基于以下笔记内容回答问题。
    如果笔记内容无法回答问题，请说"我的知识库中没有相关信息"。
    请用中文回答，并在回答末尾列出参考笔记标题。

    【笔记内容参考】:
    %s

    【用户问题】: %s
    """.formatted(context, question);
```
