# Skill: 笔记管理

## 路由判定

### 应该读这个 Skill 的场景
- 实现/修改笔记的 CRUD 功能（创建、读取、更新、删除）
- 修改笔记本（Notebook）的分组逻辑
- 修改笔记向量化流程
- 修改标签过滤逻辑
- 修改笔记的字段结构

### 不应该读这个 Skill 的场景
- 修改 AI 问答或搜索逻辑（应读 Skill: AI 语义搜索与 RAG 问答）
- 修改用户认证（应读 Skill: 用户认证与权限）
- 修改复习/出题逻辑（应读 Skill: AI 复习与出题）
- 修改前端页面布局（无关）

## 业务领域知识

### 背景
笔记管理是 MindFlow 的核心基础能力。用户创建 Markdown 笔记后，系统自动分块并生成向量嵌入（Embedding）存入 pgvector，供后续语义搜索和 RAG 问答使用。笔记可以组织在笔记本（Notebook）下，通过标签分类。

### 核心概念
- **Note**：核心业务实体，包含标题、Markdown 内容、摘要、标签、归档状态
- **Notebook**：笔记分组容器，提供基本的分类能力
- **Tags**：JSON 数组类型标签，支持 MySQL JSON_OVERLAPS 查询
- **Embedding**：笔记创建/更新后异步触发向量化，分块后存入 pgvector
- **Async**：向量化通过 `@Async` 异步执行，不阻塞用户操作

### 关键业务规则与不变量

每条规则按 `规则 → 实现 → 位置 → 约束` 格式组织。

1. **笔记数据归属用户**
   → 所有跨越用户的数据访问通过 `findByIdAndUserId()` 强制过滤
   → 实现: `NoteRepository.findByIdAndUserId()` — 组合主键 + 用户 ID 查询
   → 位置: `repository/NoteRepository.java:12`
   → 约束: 查不到时抛 `RuntimeException("Note not found or unauthorized")`

2. **创建笔记自动异步向量化**
   → save 到 MySQL 后立即触发 `@Async` 嵌入流程，不阻塞用户响应
   → 实现: `NoteServiceImpl.createNote()` 第 62 行调用 `embeddingService.embedAndStore(note)`
   → 位置: `service/NoteServiceImpl.java:62` | `service/EmbeddingServiceImpl.java:21-36`
   → 约束: 需要 `@EnableAsync`（`config/AsyncConfig.java`）支持

3. **更新笔记重新向量化**
   → 内容变更后重新分块 + 向量化，覆盖 pgvector 中的旧分块
   → 实现: `NoteServiceImpl.updateNote()` 第 79 行调用 `embeddingService.embedAndStore(note)`
   → 位置: `service/NoteServiceImpl.java:79`
   → 注意: `deleteByNoteId(noteId)` 第 24 行被注释，旧向量未被清理，存在脏数据

4. **标签过滤使用 MySQL JSON_OVERLAPS**
   → 多标签查询取交集（笔记标签包含任一查询标签即命中）
   → 实现: `NoteRepository.findByUserIdAndTagsOverlap()` — 原生 SQL + CAST
   → 位置: `repository/NoteRepository.java:15-19`
   → 约束: 仅 MySQL 8.0+ 支持，参数需先序列化为 JSON 字符串

5. **content 字段类型 LONGTEXT（上限 4GB）**
   → 长 Markdown 内容不受 VARCHAR 长度限制
   → 实现: `@Lob @Column(columnDefinition="LONGTEXT")` 注解
   → 位置: `domain/Note.java:29-31`

6. **`content_text` 字段预留**
   → 设计用于纯文本版本（如全文索引），当前代码未写入
   → 位置: `domain/Note.java:33-35` — 仅 DDL 存在，Service 层无赋值逻辑
   → 约束: 使用前需确保 `NoteServiceImpl.mapToResponse()` + 写入逻辑同步补上

7. **删除操作为物理删除（非软删除）**
   → `deleteNote()` 直接调用 `noteRepository.delete(note)`，数据不可恢复
   → 实现: `NoteServiceImpl.deleteNote()` 第 87-89 行
   → 位置: `service/NoteServiceImpl.java:87-89`
   → 影响: 被删除笔记的向量残留在 pgvector 中（未调用 `vectorStoreService.deleteByNoteId()`）

### API 端点速查

| Method | Path | Controller | 说明 |
|--------|------|-----------|------|
| GET | `/api/notes` | `NoteController.getAllNotes()` | 列表查询，支持 `?tags=` 过滤 |
| POST | `/api/notes` | `NoteController.createNote()` | 创建笔记 |
| GET | `/api/notes/{id}` | `NoteController.getNote()` | 获取单篇 |
| PUT | `/api/notes/{id}` | `NoteController.updateNote()` | 更新 |
| DELETE | `/api/notes/{id}` | `NoteController.deleteNote()` | 删除 |
| GET | `/api/notebooks` | `NotebookController.getAllNotebooks()` | 笔记本列表 |
| POST | `/api/notebooks` | `NotebookController.createNotebook()` | 创建笔记本 |
| PUT | `/api/notebooks/{id}` | `NotebookController.updateNotebook()` | 更新笔记本 |
| DELETE | `/api/notebooks/{id}` | `NotebookController.deleteNotebook()` | 删除笔记本 |

## 核心代码流程

### 创建笔记流程

```
用户 POST /api/notes (NoteCreateRequest)
  → NoteController.createNote(request, authentication)
    → NoteServiceImpl.createNote(request, username)
      → getUserByUsername(username) → userRepository.findByUsername()
      → 构造 Note 实体并填充字段
      → noteRepository.save(note)  ← 写入 MySQL
      → embeddingService.embedAndStore(note)  ← @Async 异步向量化
        → TextSplitter.split(content, 200)  ← 每 200 字分块
        → 每个 chunk 构造 Document(userId, noteId, noteTitle, chunkIndex)
        → vectorStoreService.save(docs)  ← 写入 pgvector
    → mapToResponse(note)  ← 转换为 NoteResponse
  ← 返回 NoteResponse
```

关键代码位置：
- Controller: `controller/NoteController.java:21-24`
- Service: `service/NoteServiceImpl.java:50-64`
- Async 向量化: `service/EmbeddingServiceImpl.java:21-36`
- 文本分块: `utils/TextSplitter.java:7-14`

### 更新笔记流程

```
PUT /api/notes/{id} (NoteUpdateRequest)
  → NoteController.updateNote(id, request, authentication)
    → NoteServiceImpl.updateNote(noteId, request, username)
      → 验证笔记归属: findByIdAndUserId(noteId, userId)
      → 选择性更新非空字段 (title, content, notebookId, isArchived, tags)
      → noteRepository.save(note)
      → embeddingService.embedAndStore(note)  ← 重新向量化
    → mapToResponse(note)
  ← 返回 NoteResponse
```

关键代码位置：
- Service: `service/NoteServiceImpl.java:66-81`

### 标签过滤查询

```
GET /api/notes?tags=tag1,tag2
  → NoteController.getAllNotes(tags, authentication)
    → NoteServiceImpl.getAllNotes(username, tags)
      → 如果 tags 非空: objectMapper.writeValueAsString(tags) 序列化
      → noteRepository.findByUserIdAndTagsOverlap(userId, tagsJson)
        → SQL: SELECT * FROM note WHERE user_id = ? AND JSON_OVERLAPS(tags, CAST(? AS JSON))
      → 否则: findByUserId(userId)
    → stream().map(this::mapToResponse).collect(toList())
  ← 返回 List<NoteResponse>
```

关键代码位置：
- Repository: `repository/NoteRepository.java:15-19`

## 数据模型

### Note 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| user_id | BIGINT (FK→User) | 所属用户 |
| notebook_id | BIGINT (FK→Notebook) | 所属笔记本 |
| title | VARCHAR(255) | 笔记标题 |
| content | LONGTEXT | Markdown 内容 |
| content_text | LONGTEXT | 纯文本版本（预留） |
| summary | VARCHAR(500) | 摘要 |
| tags | JSON | 标签数组 |
| is_archived | BOOLEAN | 归档状态 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### Notebook 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| user_id | BIGINT (FK→User) | 所属用户 |
| name | VARCHAR(100) | 笔记本名称 |
| description | VARCHAR(255) | 描述 |
| created_at | DATETIME | 创建时间 |

## 变更指南

### 修改时机
- 需要增加笔记字段（如封面图、AI 生成的摘要）→ 修改 `Note.java` + `NoteCreateRequest/NoteUpdateRequest.java` + `NoteResponse.java`
- 需要修改向量分块策略 → 修改 `TextSplitter.split()` 或 `EmbeddingServiceImpl.embedAndStore()`
- 需要修改标签查询方式 → 修改 `NoteRepository`
- 启用软删除 → 修改 `NoteServiceImpl.deleteNote()` 增加 `isDeleted` 字段

### 影响检查清单
- [ ] 修改 Note 实体 → 是否影响 `NoteResponse` 映射、前端展示
- [ ] 修改向量化逻辑 → 是否影响语义搜索和 RAG 问答的检索质量
- [ ] 修改 Notebook 删除 → 级联删除旗下笔记？还是保留？
- [ ] 修改标签逻辑 → 前端标签选择组件是否需要同步更新
- [ ] 修改分块大小 → 影响检索精度和 Token 消耗

### 常见变更模式

**新增笔记字段**：
1. `domain/Note.java` 添加 `@Column` 字段
2. `dto/request/NoteCreateRequest.java` 添加校验
3. `dto/request/NoteUpdateRequest.java` 添加可选字段
4. `dto/response/NoteResponse.java` 添加字段
5. `service/NoteServiceImpl.java` 在 `createNote()` 和 `updateNote()` 中赋值
6. `service/NoteServiceImpl.mapToResponse()` 映射新字段

**启用软删除**：
1. `domain/Note.java` 添加 `private Boolean isDeleted = false;`
2. 修改 `deleteNote()`: 设置 `isDeleted = true` 而非 `noteRepository.delete(note)`
3. 修改查询: 所有 `findByUserId` 增加条件 `isDeleted = false`
4. 启动新建 `@SQLRestriction("is_deleted = false")` 注解
