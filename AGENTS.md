# MindFlow — AI Agent 全局索引与约束

## 项目概述

MindFlow 是一个**私有化 AI 知识库与笔记助手**，基于 Spring Boot + React 构建。用户可创建 Markdown 笔记，系统自动向量化存入 pgvector，支持语义搜索、RAG 问答、AI 写作辅助、以及 AI 驱动的复习与出题。

### 架构分层

#### 后端分层 (`backend/src/main/java/com/mindflow/backend/`)

| 层级 | 包路径 | 职责 |
|------|--------|------|
| Controller | `controller/` | REST API 入口，接收请求、调用 Service、返回 Response |
| Service | `service/`, `service/impl/` | 业务逻辑编排与实现 |
| Service (Flow) | `flow/` | LiteFlow 规则链组件（复习、出题等异步 AI 流程） |
| Repository | `repository/` | JPA 数据访问层 |
| Domain | `domain/` | JPA 实体，核心业务模型 |
| DTO - Request | `dto/request/` | 请求参数校验对象 |
| DTO - Response | `dto/response/` | 响应数据对象 |
| DTO - Shared | `dto/` | 共享 DTO (IntentResult, ExamQuestion, Question) |
| Security | `security/` | JWT 认证过滤器、Token 提供者、UserDetailsService |
| Config | `config/` | Spring 配置类（Security, AI, DB, Redis, LiteFlow） |
| Exception | `exception/` | 全局异常处理、错误码枚举 |
| Utils | `utils/` | 工具类（文本分块、幂等控制、Markdown） |
| Mongo | `mongo/` | MongoDB 文档与 Repository（复习快照持久化） |

#### 前端分层 (`frontend/src/`)

| 层级 | 目录 | 职责 |
|------|------|------|
| 入口 | `main.tsx`, `App.tsx` | React 应用挂载、Ant Design ConfigProvider |
| 路由 | `router/index.tsx` | 路由定义 + PrivateRoute 守卫 |
| 全局状态 | `store/index.ts` | Zustand 认证状态管理 |
| 布局 | `layouts/MainLayout.tsx` | 侧边栏菜单 + 顶部搜索栏 + 内容区 |
| 页面 | `pages/*/index.tsx` | 各功能页面组件 |
| HTTP 客户端 | `utils/request.ts` | Axios 实例，自动注入 JWT Token |

---

## 全局术语表

| 业务术语 | 包路径 / 模块 | 核心类（入口） | 说明 |
|----------|--------------|---------------|------|
| 用户认证 | `security/`, `controller/AuthController` | `AuthController`, `JwtTokenProvider`, `UserServiceImpl` | 注册/登录，JWT 签发与校验 |
| 笔记本 | `domain/Notebook`, `controller/NotebookController` | `NotebookController`, `NotebookServiceImpl` | 笔记的分组目录 |
| 笔记 | `domain/Note`, `controller/NoteController` | `NoteController`, `NoteServiceImpl` | Markdown 笔记内容，支持标签、归档 |
| 向量化 | `service/EmbeddingServiceImpl` | `EmbeddingServiceImpl`, `TextSplitter` | 笔记创建/更新后异步分块并生成向量 |
| 语义搜索 | `service/VectorStoreService` | `VectorStoreService`, `AIController.search()` | 基于 pgvector 的相似度搜索 |
| AI 问答 | `service/AIChatService` | `AIChatService`, `AIController.chat()` | 意图路由 + RAG 检索 + LLM 回答 |
| AI 写作 | `service/AIWriteService` | `AIWriteService`, `AIController.write()` | 续写、润色、摘要 |
| 意图路由 | `service/IntentRouterService` | `IntentRouterService` | 分析用户意图（CHAT/SEARCH/WRITE），提取检索词 |
| AI 复习 | `flow/*`, `service/ReviewService` | `ReviewService`, `ReviewController` | LiteFlow 驱动的 AI 复习会话 |
| AI 出题 | `flow/*`, `service/ExamService` | `ExamService`, `ExamController` | LiteFlow 驱动的 AI 试卷生成 |
| 复习快照 | `mongo/ReviewSnapshot` | `ReviewSnapshotRepository`, `SnapshotCacheManager` | MongoDB 复习状态持久化 + Redis 缓存 |

---

## 硬性约束

### Java 后端编码规范

1. **所有 DTO 必须使用 Lombok**：Request/Response 类加 `@Data`；需要 Builder 模式的加 `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`
2. **Controller 必须使用 `@RequiredArgsConstructor`**：通过构造器注入依赖，禁止 `@Autowired` 字段注入
3. **所有 Service 必须面向接口编程**：除 `AIChatService`、`ReviewService` 等直接 `@Service` 类外，优先定义接口
4. **Controller 方法必须显式接收 `Authentication` 参数**：使用 `authentication.getName()` 获取当前用户名，禁止从 SecurityContextHolder 直接读取
5. **Service 层检查数据归属**：所有跨用户操作必须验证 `resource.userId.equals(currentUserId)`，抛出 `RuntimeException("Unauthorized")`
6. **所有 JSON 字段使用 `@JdbcTypeCode(SqlTypes.JSON)`**：对应 MySQL 的 `json` 列类型
7. **AI Prompt 必须构造为 SystemMessage + UserMessage**：使用 `spring-ai` 的 `Prompt` API，禁止字符串拼接 prompt
8. **异步方法必须加 `@Async`**：如 `EmbeddingServiceImpl.embedAndStore()`，且需要在配置类启用 `@EnableAsync`
9. **LiteFlow 组件必须继承 `NodeComponent`**，加 `@Component("componentName")`，在 `flow.el.xml` 中注册链
10. **MongoDB 文档的 `@Id` 字段必须手动赋值**，如 `"review_" + sessionId`
11. **所有 `@Value` 字段必须有默认值兜底**：如 `@Value("${review.max-questions-per-session:20}")`
12. **幂等控制**：涉及重复提交风险的接口（如复习答题）必须使用 `IdempotentUtil` 做幂等检查
13. **禁止在 Controller 中直接返回 Entity**：必须通过 DTO Response 返回
14. **全局异常使用 `GlobalExceptionHandler`**：禁止 Controller 方法内捕获通用异常再返回

### 前端编码规范

1. **使用 Ant Design 5.x 组件库**，主题色 `#1677ff`
2. **API 请求必须通过 `utils/request.ts`**（自动注入 JWT Token）
3. **Zustand 管理全局状态**，禁止使用 Context API 做状态提升
4. **所有路由组件放在 `pages/` 下**，每个模块一个目录
5. **页面组件使用 React.FC 或函数组件**，统一采用 `export default function`

### 数据库约束

1. **业务数据存 MySQL**：User, Note, Notebook, ExamPaper, ReviewSession
2. **向量存 PostgreSQL + pgvector**：由 Spring AI PgVectorStore 自动管理
3. **缓存存 Redis**：幂等控制 key、活跃复习会话缓存
4. **快照存 MongoDB**：复习会话完整状态快照 collection `review_snapshots`

---

## 模块导航表

| 业务模块 | 职责描述 | 后端包/文件入口 | 前端页面 | Skill 文件 |
|----------|---------|-----------------|----------|-----------|
| **用户认证** | 注册、登录、JWT 签发 | `controller/AuthController`, `security/` | `pages/Login/`, `pages/Register/` | `.skills/skill-用户认证与权限.md` |
| **笔记管理** | CRUD、标签、归档 | `controller/NoteController`, `service/NoteServiceImpl` | `pages/Notes/` | `.skills/skill-笔记管理.md` |
| **笔记本管理** | 笔记分组 | `controller/NotebookController`, `service/NotebookServiceImpl` | （与笔记管理共用） | `.skills/skill-笔记管理.md` |
| **AI 语义搜索** | 基于 pgvector 的相似度搜索 | `controller/AIController.search()`, `service/VectorStoreService` | `pages/AISearch/` | `.skills/skill-AI语义搜索与RAG问答.md` |
| **AI 问答** | 意图路由 + RAG + LLM 回答 | `controller/AIController.chat()`, `service/AIChatService` | `pages/AIChat/` | `.skills/skill-AI语义搜索与RAG问答.md` |
| **AI 写作** | 续写、润色、摘要 | `controller/AIController.write()`, `service/AIWriteService` | `pages/AIWrite/` | `.skills/skill-AI写作助手.md` |
| **意图路由** | LLM 驱动的意图分类 | `service/IntentRouterService` | （被 AIChat 调用） | `.skills/skill-意图路由.md` |
| **AI 复习** | 选择题选笔记 → AI 出题 → 答题评分 → 针对性练习 | `controller/ReviewController`, `service/ReviewService`, `flow/*` | `pages/Review/` | `.skills/skill-AI复习与出题.md` |
| **AI 出题** | 基于笔记生成试卷 | `controller/ExamController`, `service/ExamService`, `flow/*` | `pages/Exam/` | `.skills/skill-AI复习与出题.md` |

---

## 关键 API 端点速查

| Method | Path | Controller Method | 权限 | 说明 |
|--------|------|------------------|------|------|
| POST | `/api/auth/register` | `AuthController.registerUser()` | 无需认证 | 用户注册 |
| POST | `/api/auth/login` | `AuthController.authenticateUser()` | 无需认证 | 登录获取 JWT |
| GET | `/api/notes` | `NoteController.getAllNotes()` | Bearer Token | 获取笔记列表（支持 tags 过滤） |
| POST | `/api/notes` | `NoteController.createNote()` | Bearer Token | 创建笔记（自动异步向量化） |
| PUT | `/api/notes/{id}` | `NoteController.updateNote()` | Bearer Token | 更新笔记（重新向量化） |
| DELETE | `/api/notes/{id}` | `NoteController.deleteNote()` | Bearer Token | 删除笔记 |
| GET | `/api/notes/{id}` | `NoteController.getNote()` | Bearer Token | 获取单篇笔记 |
| POST | `/api/ai/chat` | `AIController.chat()` | Bearer Token | AI 问答（意图路由 + RAG） |
| POST | `/api/ai/search` | `AIController.search()` | Bearer Token | 语义搜索 |
| POST | `/api/ai/write` | `AIController.write()` | Bearer Token | AI 写作（continue/polish/summarize） |
| POST | `/api/review/start` | `ReviewController.start()` | Bearer Token | 启动复习会话 |
| POST | `/api/review/{id}/answer` | `ReviewController.answer()` | Bearer Token | 提交答案 |
| GET | `/api/review/{id}` | `ReviewController.getSession()` | Bearer Token | 获取复习会话状态 |
| POST | `/api/review/{id}/end` | `ReviewController.endSession()` | Bearer Token | 结束复习 |
| POST | `/api/review/{id}/exam` | `ReviewController.generateExamFromReview()` | Bearer Token | 从薄弱点生成试卷 |
| POST | `/api/review/exam/generate` | `ExamController.generate()` | Bearer Token | 生成试卷 |
| GET | `/api/review/exam/list` | `ExamController.listExams()` | Bearer Token | 试卷列表 |

---

## 关键配置文件

| 文件路径 | 用途 |
|---------|------|
| `backend/src/main/resources/application.yml` | 主配置：DB、AI、JWT、LiteFlow、logging |
| `backend/src/main/resources/config/flow.el.xml` | LiteFlow 规则链定义 |
| `docker-compose.yml` | MySQL + PostgreSQL + Redis + MongoDB 容器编排 |
| `frontend/package.json` | 前端依赖和脚本 |
