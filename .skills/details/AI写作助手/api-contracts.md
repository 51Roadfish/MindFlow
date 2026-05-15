# AI 写作助手 — API Contracts

## POST /api/ai/write

AI 写作处理。支持续写、润色、摘要。

**Request Body:**
```json
{
  "action": "continue | polish | summarize (required)",
  "content": "string (required, 需要处理的文本内容)"
}
```

**action 说明：**
| 值 | 行为 | 描述 |
|-----|------|------|
| `continue` | 续写 | 继续完善 Markdown 笔记内容 |
| `polish` | 润色 | 使文本更流畅和专业 |
| `summarize` | 摘要 | 100 字以内的摘要 |

**Response 200:**
```json
{
  "result": "续写/润色/摘要后的文本内容..."
}
```

**Response 400:**
```json
{
  "error": "Unsupported action"
}
```
