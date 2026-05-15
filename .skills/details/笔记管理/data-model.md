# 笔记管理 — 数据模型

## Note 表 (MySQL)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 笔记 ID |
| user_id | BIGINT | FK → User.id, NOT NULL | 所属用户 |
| notebook_id | BIGINT | FK → Notebook.id | 所属笔记本 |
| title | VARCHAR(255) | NOT NULL | 笔记标题 |
| content | LONGTEXT | NOT NULL | Markdown 内容 |
| content_text | LONGTEXT | - | 纯文本版本（预留） |
| summary | VARCHAR(500) | - | AI 或用户设定的摘要 |
| tags | JSON | - | 标签数组，如 ["Java", "Spring"] |
| is_archived | BOOLEAN | DEFAULT FALSE | 归档状态 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引：**
- `idx_user_id` on `user_id`
- `idx_user_notebook` on `(user_id, notebook_id)`

## Notebook 表 (MySQL)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 笔记本 ID |
| user_id | BIGINT | FK → User.id, NOT NULL | 所属用户 |
| name | VARCHAR(100) | NOT NULL | 笔记本名称 |
| description | VARCHAR(255) | - | 描述 |
| created_at | DATETIME | NOT NULL | 创建时间 |
