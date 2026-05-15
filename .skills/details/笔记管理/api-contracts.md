# 笔记管理 — API Contracts

## GET /api/notes

获取当前用户的所有笔记列表。支持按标签过滤。

**Query Parameters:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tags | List<String> | 否 | 按标签交集过滤，多个标签用逗号分隔 |

**Response 200:**
```json
[
  {
    "id": 1,
    "title": "Spring Boot 学习笔记",
    "content": "# Spring Boot\n\nSpring Boot 是...",
    "summary": "Spring Boot 核心概念总结",
    "tags": ["Java", "Spring"],
    "notebookId": 1,
    "isArchived": false,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-15T00:00:00"
  }
]
```

## POST /api/notes

创建新笔记（自动触发异步向量化）。

**Request Body:**
```json
{
  "title": "string (required, max 255)",
  "content": "string (required, LONGTEXT)",
  "notebookId": 1,
  "tags": ["string"]
}
```

**Response 200:** `NoteResponse` (同 GET 单条)

## GET /api/notes/{id}

获取单篇笔记详情。

**Path Parameters:**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 笔记 ID |

**Response 200:** `NoteResponse`

**Response 400:** `{ "error": "Note not found or unauthorized" }`

## PUT /api/notes/{id}

更新笔记。

**Request Body:**
```json
{
  "title": "string (optional)",
  "content": "string (optional)",
  "notebookId": 1 (optional),
  "isArchived": false (optional),
  "tags": ["string"] (optional)
}
```

**Response 200:** `NoteResponse`

## DELETE /api/notes/{id}

删除笔记（物理删除）。

**Response 204:** No Content

## GET /api/notebooks

获取所有笔记本列表。

**Response 200:**
```json
[
  {
    "id": 1,
    "name": "技术学习",
    "description": "技术相关笔记",
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

## POST /api/notebooks

创建笔记本。

**Request Body:**
```json
{
  "name": "string (required, max 100)",
  "description": "string (optional, max 255)"
}
```

## PUT /api/notebooks/{id}

更新笔记本。

**Request Body:** 同创建（字段可选）

## DELETE /api/notebooks/{id}

删除笔记本。
