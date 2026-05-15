# AI 复习与出题 — 数据模型

## ReviewSession 表 (MySQL)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 会话 ID |
| user_id | BIGINT | FK → User.id, NOT NULL | 所属用户 |
| note_ids | JSON | - | 所选笔记 ID 数组，如 `[1, 2, 3]` |
| note_ids_hash | VARCHAR(64) | UNIQUE with (user_id, status) | SHA-256(sorted noteIds) |
| status | VARCHAR(20) | - | 可选值: IN_PROGRESS, COMPLETED, CANCELLED |
| total_questions | INT | DEFAULT 0 | 总出题数 |
| answered_questions | INT | DEFAULT 0 | 已回答数 |
| total_score | INT | DEFAULT 0 | 总分 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**唯一约束：** `UNIQUE(user_id, note_ids_hash, status)` — 同用户 + 同笔记 + IN_PROGRESS 只允许一条

## ExamPaper 表 (MySQL)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 试卷 ID |
| user_id | BIGINT | FK → User.id, NOT NULL | 所属用户 |
| title | VARCHAR(255) | - | 试卷标题（AI 生成或默认） |
| content | LONGTEXT | - | JSON 格式的题目数组 |
| question_count | INT | - | 题目数量 |
| note_ids_json | JSON | - | 来源笔记 ID |
| status | VARCHAR(20) | - | ACTIVE / DELETED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

## ReviewSnapshot (MongoDB, collection: `review_snapshots`)

| 字段 | 类型 | 说明 |
|------|------|------|
| _id | String | 格式 `"review_<sessionId>"` |
| sessionId | Long | 关联的 ReviewSession ID |
| userId | Long | 用户 ID |
| noteIds | Array<Long> | 所选笔记 ID |
| status | String | IN_PROGRESS / COMPLETED / CANCELLED |
| turns | Array<ReviewTurn> | 答题轮次历史 |
| currentQuestionJson | String | 当前题目 JSON 序列化 |
| totalScore | Int | 总分 |
| answeredQuestions | Int | 已回答数 |
| updatedAt | Date | 最后更新时间 |

### ReviewTurn (内嵌文档)

| 字段 | 类型 | 说明 |
|------|------|------|
| questionId | String | 题目 UUID |
| question | String | 题目内容 |
| expectedAnswer | String | 参考答案 |
| userAnswer | String | 用户答案 |
| score | Int | 得分 (0-100) |
| feedback | String | AI 评语 |
| action | String | follow_up / next_question / complete |
| timestamp | Date | 作答时间戳 |

## Redis Key 设计

| Key 模式 | 值 | TTL | 用途 |
|---------|-----|-----|------|
| `review:session:{sessionId}:q:{questionId}` | `"1"` | 3600s | 题目幂等标记 |
| `review:session:{sessionId}:active` | Snapshot JSON | 30min | 活跃会话缓存 |

## Context 数据模型

### ReviewContext (LiteFlow 组件间传递)

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | Long | 会话 ID |
| userId | Long | 用户 ID |
| noteIds | List<Long> | 所选笔记 ID |
| notesContent | String | 笔记拼接文本（由 LoadNotesComponent 填充） |
| userAnswer | String | 用户待评分的答案 |
| lastScore | int | 上轮得分 |
| lastFeedback | String | 上轮评语 |
| nextAction | String | follow_up / next_question / complete |
| turnCount | int | 已出题数 |
| history | List<Question> | 已出题目列表 |

### ExamContext (LiteFlow 组件间传递)

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| noteIds | List<Long> | 所选笔记 ID |
| notesContent | String | 笔记拼接文本 |
| questionCount | int | 要生成的题目数（默认 10） |
| examTitle | String | 试卷标题（AI 生成） |
| weakAreas | String | 薄弱点描述（复习后针对性出卷时使用） |
| generatedExam | List<ExamQuestion> | 生成的试卷题目 |
