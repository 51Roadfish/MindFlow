# AI 写作助手 — 错误码

| HTTP 状态码 | 错误消息 | 触发条件 |
|-------------|---------|---------|
| 400 | `Unsupported action` | action 不是 continue/polish/summarize |
| 500 | 模型调用异常 | OpenAI API 调用失败或超时 |
