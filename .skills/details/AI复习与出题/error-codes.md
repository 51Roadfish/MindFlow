# AI 复习与出题 — 错误码

### 复习模块

| HTTP 状态码 | 错误消息 | 触发条件 |
|-------------|---------|---------|
| 400 | `This question has already been answered` | 同一道题重复提交答案 |
| 400 | `Session not found` | 复习会话 ID 不存在 |
| 400 | `Session snapshot not found` | Mongo 中未找到对应快照 |
| 400 | `Unauthorized` | 非会话属主尝试操作 |
| 400 | `Failed to generate first question` | LiteFlow review_start_chain 执行失败 |
| 400 | `Failed to score answer` | LiteFlow review_answer_chain 执行失败 |
| 400 | `Failed to serialize noteIds` | noteIds 序列化为 JSON 失败 |

### 出题模块

| HTTP 状态码 | 错误消息 | 触发条件 |
|-------------|---------|---------|
| 400 | `No questions generated` | AI 未生成任何题目 |
| 400 | `Exam paper not found` | 试卷 ID 不存在 |
| 400 | `Review session not found` | 复习会话不存在 |
| 400 | `Review snapshot not found` | 复习快照不存在 |
| 400 | `No weak areas found — all answers scored 60 or above` | 复习全部及格，无需针对性出卷 |
| 400 | `Failed to generate exam paper` | LiteFlow exam_generate_chain 执行失败 |
| 400 | `Failed to serialize exam questions` | 题目序列化为 JSON 失败 |
