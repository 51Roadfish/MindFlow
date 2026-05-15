# Skill: AI 复习与出题

## 路由判定

### 应该读这个 Skill 的场景
- 修改 AI 复习会话的启动、答题、评分、结束流程
- 修改 LiteFlow 规则链（review_start_chain、review_answer_chain、exam_generate_chain）
- 修改复习出题的 Prompt 模板
- 修改评分策略或决策逻辑
- 修改 MongoDB 快照机制
- 修改 Redis 幂等控制与会话缓存
- 修改试卷生成逻辑

### 不应该读这个 Skill 的场景
- 修改笔记 CRUD（应读 Skill: 笔记管理）
- 修改语义搜索与 RAG（应读 Skill: AI 语义搜索与 RAG 问答）
- 修改意图路由（应读 Skill: 意图路由）
- 修改用户认证（应读 Skill: 用户认证与权限）

## 业务领域知识

### 背景
AI 复习与出题是 MindFlow 的高级功能，利用 LiteFlow 规则引擎编排 AI 工作流，实现智能化的知识复习和考试出题。用户选择笔记后，系统自动生成题目，逐题作答并评分，最后可基于薄弱点生成针对性试卷。

### 核心概念

**复习流程术语**：
- **复习会话（ReviewSession）**：一次复习任务的生命周期，包含笔记选择、逐题作答、评分反馈
- **LiteFlow 规则链**：复习和出题流程由 LiteFlow 编排，定义在 `flow.el.xml` 中
- **快照机制**：复习会话状态存 MongoDB，活跃会话同时缓存到 Redis
- **幂等控制**：同一题不可重复提交，通过 Redis key 实现
- **上下文（ReviewContext / ExamContext）**：LiteFlow 组件间传递的数据对象
- **AI 出题（ExamPaper）**：生成完整的试卷，支持查看和删除

### LiteFlow 链定义

```xml
<!-- review_start_chain: 加载笔记 → 生成第一道题目 -->
<chain name="review_start_chain">
    THEN(loadNotesComponent, generateQuestionComponent)
</chain>

<!-- review_answer_chain: 评分 + 决策下一步 -->
<chain name="review_answer_chain">
    THEN(scoreAnswerComponent)
</chain>

<!-- exam_generate_chain: 加载笔记 → 生成整套试卷 -->
<chain name="exam_generate_chain">
    THEN(loadNotesComponent, generateExamComponent)
</chain>
```

关键代码位置：`resources/config/flow.el.xml`

### 关键业务规则与不变量

每条规则按 `规则 → 实现 → 位置 → 约束` 格式组织。

1. **复习会话幂等（同用户 + 同笔记只允许一个活跃会话）**
   → 通过唯一约束阻断冗余创建：`UNIQUE(user_id, note_ids_hash, status)` 确保活跃会话唯一
   → 实现: `ReviewSession` 实体第 11 行 `@UniqueConstraint` + `ReviewService.start()` 第 51 行 `findByUserIdAndNoteIdsHashAndStatus()` 前置检查
   → 位置: `domain/ReviewSession.java:11` | `service/ReviewService.java:50-61`
   → 约束: `note_ids_hash` 由 `hashNoteIds()` 生成 — SHA-256(sorted noteIds join)
   → 僵死会话兜底: 幂等检查发现已有活跃会话后，需验证其快照中有 `currentQuestionJson`。若无（如 AI 生成失败），则日志告警、取消旧会话、清理 Redis/Mongo 缓存，走新建逻辑。实现: `ReviewService.start()` 第 55-61 行 — `loadSnapshot()` + 判空逻辑

2. **笔记内容不持久化到 MongoDB 快照**
   → 快照仅保存答题状态，笔记原文在恢复时从 MySQL 重新加载
   → 实现: `fromSnapshot()` 第 267 行 — `ctx.setNotesContent("")`
   → 位置: `service/ReviewService.java:267-268`
   → 目的: 避免 MongoDB 与 MySQL 数据不一致，同时减小快照体积

3. **单次复习会话最大题数上限（默认 20）**
   → 同时约束了 `DecideNextComponent` 的决策边界和 `ScoreAnswerComponent` 的提前退出
   → 实现: `@Value("${review.max-questions-per-session:20}")` 注入
   → 位置: `service/ReviewService.java:42` | `flow/DecideNextComponent.java:20` | `flow/ScoreAnswerComponent.java:19`
   → 约束: 达到上限时 `nextAction` 强制设为 `"complete"`，不调 LLM

4. **每道题作答幂等（防止重复提交评分）**
   → 同一 session + 同一 questionId 只接受首次提交，后续返回异常
   → 实现: `IdempotentUtil.markAnswer()` — Redis `SETNX key "1" EX 3600`（第 23 行）
   → 位置: `utils/IdempotentUtil.java:21-25`
   → 约束: Key 模式 `"review:session:{sessionId}:q:{questionId}"`，TTL 1 小时。Redis 不可用时幂等失效

5. **AI 评分后自动决策下一步（follow_up / next_question / complete）**
   → `ScoreAnswerComponent` 单次 AI 调用同时完成评分 + 决策两步（Prompt 要求返回 `score` + `nextAction`）
   → 实现: `ScoreAnswerComponent.process()` — 评分 Prompt 第 39 行要求同时返回 score、feedback、nextAction
   → 位置: `flow/ScoreAnswerComponent.java:23-59`
   → 注意: `DecideNextComponent` 已定义但未被任何 LiteFlow 链引用（`flow.el.xml` 中 `review_answer_chain` 仅含 `scoreAnswerComponent`），当前决策完全由 ScoreAnswerComponent 内联完成

6. **课后针对性出卷只针对得分 <60 的薄弱知识点**
   → 从 `ReviewSnapshot.turns` 中筛选 `score < 60` 的轮次，构建 weakAreas 传入出卷 Prompt
   → 实现: `ExamService.generateFromReview()` 第 91-96 行 — `snapshot.getTurns().stream().filter(t → t.getScore() < 60)`
   → 位置: `service/ExamService.java:91-96`
   → 约束: 若无薄弱点（全部 ≥60），抛 `"No weak areas found — all answers scored 60 or above"`

7. **试卷删除采用软删除**
   → `ExamPaper.status` 从 `"ACTIVE"` 切换为 `"DELETED"`，保留数据可追溯
   → 实现: `ExamService.deleteExam()` 第 164 行 — `paper.setStatus("DELETED")`，跳过 `examPaperRepository.delete()`
   → 位置: `service/ExamService.java:164`
   → 约束: 列表查询需过滤 `status = "ACTIVE"`（`ExamPaperRepository.java:9` — `findByUserIdAndStatusOrderByCreatedAtDesc`）

8. **三层缓存架构：Redis → MongoDB → 全量重建**
   → 活跃会话走 Redis（30min TTL），Redis miss 从 MongoDB 懒加载，MongoDB 也 miss 则抛出异常
   → 实现: `SnapshotCacheManager.getActiveSession()` — 第 27-45 行：Redis GET → miss → Mongo findBySessionId → 回填 Redis
   → 位置: `service/SnapshotCacheManager.java:27-58`
   → 约束: Redis 不可用时会降级为每次都查 Mongo，不影响核心功能
   → 缓存驱逐: `SnapshotCacheManager.evictSession()` — 同时清理 Redis key 和 MongoDB 文档，用于僵死会话重建

### 状态枚举

| 状态 | 适用对象 | 可选值 |
|------|---------|-------|
| 复习状态 | ReviewSession.status | `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| 试卷状态 | ExamPaper.status | `ACTIVE`, `DELETED` |
| 决策动作 | ReviewContext.nextAction | `follow_up`, `next_question`, `complete` |

### 幂等控制 Key 设计

| Key 模式 | 用途 | TTL |
|---------|------|-----|
| `review:session:%d:q:%s` | 标记某道题已作答 | 3600s |
| `review:session:%d:active` | 活跃会话数据缓存 | 30min |

## 核心代码流程

### 复习启动流程

```
用户 POST /api/review/start { noteIds: [1,2,3] }
  → ReviewController.start(request, authentication)
    → ReviewService.start(request, userId)
      → hashNoteIds(noteIds)  ← SHA-256(noteIds sorted join)
      → 幂等检查: findByUserIdAndNoteIdsHashAndStatus(userId, hash, "IN_PROGRESS")
        → 已有: loadSnapshot(sessionId) ← 先 Redis 后 Mongo
          → 快照有 currentQuestionJson → 返回当前状态（正常幂等命中）
          → 快照无 currentQuestionJson → 僵死会话，取消旧会话并清理缓存，走新建逻辑
        → 无: 创建新 ReviewSession(status=IN_PROGRESS)
      → 构建 ReviewContext(sessionId, userId, noteIds)
      → flowExecutor.execute2Resp("review_start_chain", reviewCtx)
        → LoadNotesComponent.process():
            noteRepository.findAllById(noteIds)
            → 拼接: "【title】\ncontent \n\n【title2】\ncontent2"
            → 设置 reviewCtx.notesContent
        → GenerateQuestionComponent.process():
            SystemMessage: "你是一个智能考官。基于用户的笔记内容生成一道..."
            UserMessage: "笔记内容：\n" + notesContent + "\n\n已出题目：\n" + historyJson
            → AiJsonHelper.parse(response, Question.class)
            → 追加到 reviewCtx.history, 更新 turnCount
      → 更新 session.totalQuestions = turnCount
      → saveSnapshot(session, reviewCtx, null)  ← 保存到 MongoDB 并刷新 Redis
      → 返回 ReviewSessionResponse(currentQuestion, totalQuestions, ...)
```

关键代码位置：
- Controller: `controller/ReviewController.java:36-40`
- Service: `service/ReviewService.java:46-119`（含僵死会话兜底:55-61）
- LoadNotesComponent: `flow/LoadNotesComponent.java:19-46`（同时支持 ReviewContext 和 ExamContext）
- GenerateQuestionComponent: `flow/GenerateQuestionComponent.java:23-52`
- AiJsonHelper: `flow/AiJsonHelper.java:18-69`

### 答题评分流程

```
用户 POST /api/review/{sessionId}/answer { questionId, answer }
  → ReviewController.answer(sessionId, request, authentication)
    → ReviewService.answer(sessionId, request, userId)
      → IdempotentUtil.markAnswer(sessionId, questionId)  ← 幂等检查
        → Redis SETNX "review:session:<id>:q:<questionId>" 1 EX 3600
        → false = 重复提交 → throw "This question has already been answered"
      → 验证会话归属: session.userId == userId
      → restoreContext(sessionId)  ← 从 Redis → Mongo 恢复上下文
      → reviewCtx.setUserAnswer(answer)
      → flowExecutor.execute2Resp("review_answer_chain", reviewCtx)
        → ScoreAnswerComponent.process():
            SystemMessage: "你是一个评分老师。请根据题目、参考答案和学生答案进行评分..."
            → AiJsonHelper.parse(response, Map.class)
            → 设置 reviewCtx.lastScore, lastFeedback, nextAction
      → 更新 session.answeredQuestions, totalScore

      if nextAction == "complete":
        → session.status = "COMPLETED"
        → 生成总结: "复习结束！共回答 X 题，总分 Y，平均分 Z。"
        → saveSnapshot 最终状态
        → idempotentUtil.removeSession(sessionId)
        → 返回 ReviewAnswerResponse(score, feedback, nextAction="complete", summary)

      else (follow_up / next_question):
        → generateNextQuestion(reviewCtx)
            → flowExecutor.execute2Resp("review_start_chain", reviewCtx)
            → 返回最新生成的 Question
        → session.totalQuestions += 1
        → saveSnapshot + 刷新缓存
        → 返回 ReviewAnswerResponse(score, feedback, nextAction, nextQuestion)
```

关键代码位置：
- Controller: `controller/ReviewController.java:42-48`
- Service: `service/ReviewService.java:116-181`
- ScoreAnswerComponent: `flow/ScoreAnswerComponent.java:23-59`
- DecideNextComponent: `flow/DecideNextComponent.java:16-50`

### 试卷生成流程

```
用户 POST /api/review/exam/generate { noteIds: [...], questionCount: 10 }
  → ExamController.generate(request, authentication)
    → ExamService.generate(request, userId)
      → 构建 ExamContext(userId, noteIds, questionCount)
      → flowExecutor.execute2Resp("exam_generate_chain", examCtx)
        → LoadNotesComponent.process()  ← 复用同一组件
        → GenerateExamComponent.process():
            SystemMessage: "你是一个出卷老师。根据用户的笔记内容生成一套完整的试卷..."
            UserMessage: "笔记内容：\n" + notesContent + "\n\n请生成 10 道题目。"
            → AiJsonHelper.parse(response, MapWithTitle.class)
            → 设置 examCtx.examTitle, generatedExam
      → 保存 ExamPaper 到 MySQL (status=ACTIVE)
      → 返回 ExamPaperResponse(questions, title, ...)
```

关键代码位置：
- Controller: `controller/ExamController.java:30-35`
- Service: `service/ExamService.java:35-74`
- GenerateExamComponent: `flow/GenerateExamComponent.java:17-58`

### 课后针对性出卷

```
用户 POST /api/review/{sessionId}/exam
  → ReviewController.generateExamFromReview(sessionId, authentication)
    → ExamService.generateFromReview(sessionId, userId)
      → 验证 ReviewSession 归属
      → 从 Mongo 加载 ReviewSnapshot
      → 提取得分 < 60 的题目作为 weakAreas
      → 构建 ExamContext(noteIds, weakAreas, questionCount=5)
      → flowExecutor.execute2Resp("exam_generate_chain", examCtx)
      → 特殊 Prompt: "特别注意：该学生以下知识点薄弱..."
      → 保存试卷
```

关键代码位置：
- Service: `service/ExamService.java:77-139`

### 状态恢复流程

```
ReviewService.restoreContext(sessionId)
  → snapshotCache.getActiveSession(sessionId)
    → Redis GET "review:session:<id>:active"
    → 命中: objectMapper 反序列化为 ReviewSnapshot → fromSnapshot()
    → 未命中: snapshotRepository.findBySessionId(sessionId)  ← 从 Mongo 懒加载
      → 写入 Redis 缓存
      → fromSnapshot()
  → ReviewContext:
    sessionId, userId, noteIds, turnCount,
    notesContent="" (下一轮重新加载), history=[当前题目]
```

关键代码位置：
- SnapshotCacheManager: `service/SnapshotCacheManager.java:27-68`（含 evictSession:60-64）
- ReviewService.restoreContext: `service/ReviewService.java:257-271`
- ReviewService.loadSnapshot: `service/ReviewService.java:268-278`（先 Redis 后 Mongo 加载快照）

### 数据模型

**ReviewSession 表**（MySQL）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| user_id | BIGINT | 所属用户 |
| note_ids | JSON | 所选笔记 ID 列表 |
| note_ids_hash | VARCHAR(64) | SHA-256 哈希（唯一约束 + 幂等） |
| status | VARCHAR(20) | IN_PROGRESS / COMPLETED / CANCELLED |
| total_questions | INT | 总出题数 |
| answered_questions | INT | 已回答数 |
| total_score | INT | 总分 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**ExamPaper 表**（MySQL）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | 自增主键 |
| user_id | BIGINT | 所属用户 |
| title | VARCHAR(255) | 试卷标题 |
| content | LONGTEXT | JSON: questions 数组 |
| question_count | INT | 题目数量 |
| note_ids_json | JSON | 来源笔记 ID |
| status | VARCHAR(20) | ACTIVE / DELETED |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**ReviewSnapshot**（MongoDB collection: `review_snapshots`）
| 字段 | 类型 | 说明 |
|------|------|------|
| _id | String | "review_<sessionId>" |
| sessionId | Long | 关联的 ReviewSession ID |
| userId | Long | 用户 ID |
| noteIds | Array<Long> | 所选笔记 ID |
| status | String | 会话状态 |
| turns | Array<ReviewTurn> | 答题轮次历史 |
| currentQuestionJson | String | 当前题目的 JSON |
| totalScore | Int | 总分 |
| answeredQuestions | Int | 已回答数 |
| updatedAt | Date | 更新时间 |

**ReviewTurn**（内嵌文档）
| 字段 | 类型 | 说明 |
|------|------|------|
| questionId | String | 题目 ID |
| question | String | 题目内容 |
| expectedAnswer | String | 参考答案 |
| userAnswer | String | 用户答案 |
| score | Int | 得分 |
| feedback | String | 评语 |
| action | String | 决策动作 |
| timestamp | Date | 作答时间 |

## 变更指南

### 修改时机
- 需要修改评分标准 → 修改 `ScoreAnswerComponent` 或 `DecideNextComponent` 的 Prompt
- 需要修改出题策略（题型、难度）→ 修改 `GenerateQuestionComponent` 或 `GenerateExamComponent`
- 需要修改快照策略 → 修改 `ReviewService.saveSnapshot()` 和 `restoreContext()`
- 需要修改 LiteFlow 链 → 修改 `flow.el.xml` 并增加/修改对应 Component
- 需要新增题型 → 修改 `ExamQuestion` DTO + 出题 Prompt + 前端展示

### 影响检查清单
- [ ] 修改 LiteFlow 链 → 组件名称需与 `@Component("name")` 一致，`flow.el.xml` 同步更新
- [ ] 修改 ReviewContext/ExamContext → 所有读写该字段的组件和方法需同步更新
- [ ] 修改 Prompt → 输出 JSON 格式变化 → AiJsonHelper 解析逻辑需同步调整
- [ ] 修改快照机制 → MongoDB 数据结构变化 → 历史数据的向前兼容性
- [ ] 修改幂等逻辑 → Redis key 模式变化 → 清理历史 key
- [ ] 修改 ReviewSession 唯一约束 → 影响幂等检查逻辑
- [ ] 修改前端页面 → 确保前后端 DTO 字段一致

### 常见变更模式

**新增 LiteFlow 组件**：
1. 创建新的 Java 类继承 `NodeComponent`，加 `@Component("myComponent")`
2. `flow.el.xml` 中注册：`THEN(loadNotesComponent, myComponent, generateQuestionComponent)`
3. 如果组件需要读取上下文，在 `process()` 中 `((ReviewContext) getRequestData())`
4. 在上下文中添加需要的字段

**修改评分策略**：
```java
// ScoreAnswerComponent — 修改评分 Prompt 中的评分标准
String prompt = """
    你是一个评分老师。请根据题目、参考答案和学生答案进行评分。
    评分标准（新）：
    - 答出核心概念得 60 分
    - 能举例说明加 20 分
    - 有深度分析再加 20 分
    - 评分范围 0 到 100 分
    ...
    """;
```

**新增 AI 出题中的工具调用**（需引入 LiteFlow 工具节点）：
1. 在 `ExamContext` 添加工具相关字段
2. 创建新的 NodeComponent 处理工具调用
3. 在 `exam_generate_chain` 中插入 `THEN(loadNotesComponent, toolCallComponent, generateExamComponent)`

**修改缓存策略**：
```java
// 调整 SnapshotCacheManager 的 TTL
// 或者增加缓存 Layer（如 Caffeine 本地缓存）
```

### 已知限制与待办
- `LoadNotesComponent` 同时支持 `ReviewContext` 和 `ExamContext`，用 `instanceof` 判断，耦合较重。后续可引入统一接口或 LiteFlow 的多上下文支持
- 当前 `deleteByNoteId` 在 `embedAndStore` 中被注释，更新笔记时旧向量未被删除
- 出题试卷暂不支持在线作答，仅展示参考答案
- 幂等检查目前只验证 `currentQuestionJson` 非空，未验证题目的语义有效性（如 JSON 是否能正确反序列化为 Question 对象）
- `Redis` 缓存快照的 TTL 为 30 分钟，超时后降级到 MongoDB 查询，不影响功能但延迟增加
