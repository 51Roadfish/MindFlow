# 用户认证 — 数据模型

## User 表 (MySQL)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt 加密后的密码 |
| email | VARCHAR(100) | - | 邮箱 |
| created_at | DATETIME | NOT NULL | 注册时间 |
