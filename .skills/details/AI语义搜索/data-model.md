# AI 语义搜索与 RAG 问答 — 数据模型

## pgvector 向量存储 (PostgreSQL)

由 Spring AI PgVectorStore 自动管理，表名为 `vector_store`。

| 列名 | 类型 | 说明 |
|------|------|------|
| id | UUID | 文档 ID |
| content | TEXT | 分块后的文本内容 |
| metadata | JSONB | 元数据（userId, noteId, noteTitle, chunkIndex） |
| embedding | vector(1024) | 1024 维浮点向量 |

**元数据格式：**
```json
{
  "userId": "1",
  "noteId": "10",
  "noteTitle": "Spring Boot 学习笔记",
  "chunkIndex": 2
}
```

**索引：** HNSW 索引 + COSINE_DISTANCE 距离算法

## 配置参考

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
