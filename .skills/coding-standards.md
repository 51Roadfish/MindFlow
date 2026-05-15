# MindFlow Coding Standards & Engineering Principles

> 通用编程规范，可跨项目复用。侧重软件开发的最佳实践，非 MindFlow 特有的约束见 `AGENTS.md`。

## 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| Java 类 | UpperCamelCase | `NoteServiceImpl`, `JwtTokenProvider` |
| Java 方法 | lowerCamelCase | `getAllNotes`, `embedAndStore` |
| Java 常量 | UPPER_SNAKE_CASE | `INTENT_CHAT`, `INTENT_SEARCH` |
| Java 包 | all lowercase, dot-separated | `com.mindflow.backend.service` |
| REST 路径 | lowercase, kebab-case | `/api/review/exam/generate` |
| 表名 | snake_case | `review_session`, `exam_paper` |
| 列名 | snake_case | `note_ids_hash`, `question_count` |
| JSON 字段 | lowerCamelCase | `questionId`, `nextAction` |
| React 组件 | UpperCamelCase | `AIChat`, `MainLayout` |
| React 文件 | PascalCase (页面) | `index.tsx` (页面文件本身用 kebab 目录) |

## 异常处理规范

1. **业务异常抛出 `RuntimeException`**：消息内容应面向客户端可读，如 `"User not found"`, `"Unauthorized"`
2. **全局统一处理**：`GlobalExceptionHandler` 捕获所有 Exception，返回 `{ "error": message }`
3. **不在 Controller 中 try-catch**：除极少数需要特殊响应码的场景（如 AuthController.registerUser），其余全部交给 `GlobalExceptionHandler`
4. **异常消息使用英文**：便于日志聚合，前端可自行根据 status code 显示中文
5. **资源归属校验失败统一抛出 `RuntimeException("Unauthorized")`**

## 测试规范

1. **Service 层单元测试**：使用 Mockito + JUnit 5，Mock 所有 Repository 和外部依赖
2. **Controller 层测试**：使用 `@WebMvcTest` + MockMvc
3. **AI 相关测试**：Mock ChatModel，使用固定响应验证 Prompt 构造正确性
4. **LiteFlow 链路测试**：使用 `FlowExecutor` 测试完整 chain 执行
5. **禁止在测试中调用真实 AI API**：所有 AI 调用必须 Mock

## 日志规范

1. **使用 Lombok `@Slf4j`**：每个 Service 类都需要日志注解
2. **关键业务操作必须记录日志**：如创建笔记、"Creating note for user '{}': title='{}'"
3. **AI 调用结果必须记录**：如用户提问意图解析结果
4. **异常必须记录堆栈**：`log.error("Failed to ...", e)`
5. **日志级别使用**：INFO（业务操作）、DEBUG（调试细节）、WARN（可恢复问题）、ERROR（不可恢复问题）

## API 设计规范

1. **统一前缀**：所有后端 API 以 `/api/` 开头
2. **RESTful 设计**：资源 URL + HTTP Method 表达操作
3. **统一响应格式**：成功返回直接数据，失败返回 `{ "error": "message" }`
4. **分页查询**：使用 `?page=0&size=20` 标准参数，返回 `PageResponse` 封装
5. **Swagger 文档**：Request DTO 使用 `@Schema` 注解描述字段

## 数据库操作规范

1. **软删除优先**：如 ExamPaper 使用 `status = "DELETED"` 而非物理删除
2. **JSON 列类型**：MySQL JSON 列用 `@JdbcTypeCode(SqlTypes.JSON)` 映射
3. **时间戳自动管理**：Entity 使用 `@PrePersist` / `@PreUpdate` 或 `@CreatedDate` 自动维护
4. **幂等操作**：关键写入操作（如复习答题）必须防重
5. **异步非阻塞**：耗时操作（如向量化）使用 `@Async` 异步执行

## 安全规范

1. **密码加密**：使用 BCryptPasswordEncoder，禁止明文存储
2. **JWT 签发**：使用 HS512 签名，包含 subject(username) + 签发时间 + 过期时间
3. **Token 传递**：通过 Authorization Header: `Bearer <token>`
4. **认证端点开放**：`/api/auth/**` 不要求认证
5. **跨用户隔离**：所有查询必须带上当前用户 ID 过滤

## 版本控制规范

1. **Commit 消息格式**：`type(scope): description`，如 `feat(review): add AI scoring component`
2. **分支策略**：`main`（稳定）、`develop`（开发）、`feature/*`（特性）、`fix/*`（修复）
3. **配置不入库**：敏感配置（API Key、密码）通过环境变量注入，`application.yml` 中使用 `${}` 占位符
