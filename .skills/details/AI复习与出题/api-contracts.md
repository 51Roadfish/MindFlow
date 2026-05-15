# AI 复习与出题 — API Contracts

## POST /api/review/start

启动复习会话。幂等：同用户 + 同笔记组合的活跃会话只创建一次。

**Request Body:**
```json
{
  "noteIds": [1, 2, 3],
  "tags": ["Java", "Spring"]
}
```

**Response 200:**
```json
{
  "id": 1,
  "status": "IN_PROGRESS",
  "currentQuestion": {
    "questionId": "uuid-string",
    "question": "请解释 Spring Boot 自动配置的原理。",
    "expectedAnswer": "Spring Boot 通过 @EnableAutoConfiguration 注解..."
  },
  "totalQuestions": 1,
  "answeredQuestions": 0,
  "totalScore": 0,
  "maxQuestions": 20
}
```

## POST /api/review/{sessionId}/answer

提交答案。幂等：同一 questionId 不可重复提交。

**Path Parameters:**
| 参数 | 类型 | 说明 |
|------|------|------|
| sessionId | Long | 复习会话 ID |

**Request Body:**
```json
{
  "questionId": "uuid-string",
  "answer": "Spring Boot 自动配置通过 @EnableAutoConfiguration 注解实现..."
}
```

**Response 200 (继续作答):**
```json
{
  "score": 85,
  "feedback": "理解基本正确，可以进一步说明条件注解 @Conditional 的作用。",
  "nextAction": "follow_up",
  "nextQuestion": {
    "questionId": "new-uuid",
    "question": "那么 @ConditionalOnMissingBean 和 @ConditionalOnClass 有什么区别？",
    "expectedAnswer": "..."
  },
  "summary": null
}
```

**Response 200 (完成):**
```json
{
  "score": 90,
  "feedback": "回答正确，理解深入。",
  "nextAction": "complete",
  "nextQuestion": null,
  "summary": "复习结束！共回答 5 题，总分 420，平均分 84。"
}
```

## GET /api/review/{sessionId}

获取复习会话当前状态。

**Response 200:** `ReviewSessionResponse`

## POST /api/review/{sessionId}/end

结束复习会话。

**Response 200:** `ReviewSessionResponse`

## GET /api/review/history

获取当前用户的复习历史。

**Response 200:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "noteIdsJson": "[1,2,3]",
    "noteIdsHash": "sha256...",
    "status": "COMPLETED",
    "totalQuestions": 5,
    "answeredQuestions": 5,
    "totalScore": 420,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

## POST /api/review/{sessionId}/exam

根据复习薄弱点生成针对性试卷（得分 < 60 的题目）。

**Response 200:** `ExamPaperResponse`

## POST /api/review/exam/generate

根据指定笔记生成试卷。

**Request Body:**
```json
{
  "noteIds": [1, 2, 3],
  "tags": [],
  "questionCount": 10
}
```

**Response 200:**
```json
{
  "id": 1,
  "title": "Spring Boot 核心知识测试",
  "questionCount": 10,
  "questions": [
    {
      "id": 1,
      "type": "short_answer",
      "question": "什么是 Spring Boot 自动配置？",
      "options": null,
      "answer": "Spring Boot 自动配置是...",
      "points": 10
    },
    {
      "id": 2,
      "type": "multiple_choice",
      "question": "以下哪个不是 Spring Boot 的核心特性？",
      "options": [
        "A. 自动配置",
        "B. 起步依赖",
        "C. EJB 容器",
        "D. Actuator 监控"
      ],
      "answer": "C",
      "points": 10
    }
  ],
  "createdAt": "2024-01-15T00:00:00"
}
```

## GET /api/review/exam/list

获取当前用户的所有活跃试卷列表。

**Response 200:** `List<ExamPaperResponse>`

## GET /api/review/exam/{examId}

获取单份试卷详情。

## DELETE /api/review/exam/{examId}

删除试卷（软删除）。

**Response 204:** No Content
