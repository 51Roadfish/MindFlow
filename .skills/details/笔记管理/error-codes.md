# 笔记管理 — 错误码

| HTTP 状态码 | 错误消息 | 触发条件 |
|-------------|---------|---------|
| 400 | `Note not found or unauthorized` | 笔记不存在或不属于当前用户 |
| 400 | `Notebook not found` | 笔记本不存在或不属于当前用户 |
| 400 | `User not found` | 用户名不存在 |
| 400 | `Failed to serialize tags` | Tags 序列化为 JSON 失败 |
