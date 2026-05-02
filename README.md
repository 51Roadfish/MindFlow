# MindFlow - 私有化 AI 知识库与笔记助手

**MindFlow** 是一个**支持语义搜索、AI 问答和智能写作的个人知识管理平台**。让知识管理真正成为你的“第二大脑”。

## 📖 背景

提供一个智能笔记本。期末复习？面试准备？记忆搜索？通通一网打尽。

## ✨ 核心特性

### 已实现功能
- **Markdown 笔记管理**：创建、编辑、标签、笔记本分组
- **AI 语义搜索**：用自然语言搜索笔记
- **RAG 智能问答**：基于你记过的笔记回答问题，并引用原文出处
- **AI 写作助手**：续写、润色、总结笔记内容
- **异步向量化**：笔记保存后自动在后台生成向量，不阻塞操作
- **多平台模型集成**：支持 OpenAI 兼容的 LLM（当前使用智谱 AI Chat API + 硅基流动 Embedding API）

### 架构亮点
- **DDD 领域驱动设计**：清晰的分层和领域模型，易于扩展
- **可插拔的技能系统（开发中）**：AI 写作、搜索等能力抽象为“技能”，未来可由 Agent 动态编排
- **跨会话记忆（规划中）**：让 AI 记住你的偏好和历史对话，越用越聪明
- **多模态支持（规划中）**：支持 PDF、Word 等文件上传与智能解析

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.x + Spring AI 1.0.0-M4 |
| **认证授权** | Spring Security + JWT |
| **数据库** | MySQL (业务数据) + PostgreSQL + pgvector (向量存储) |
| **缓存** | Redis |
| **AI 模型** | 智谱 AI GLM-4 Flash (对话)、硅基流动 BAAI/bge-large-zh-v1.5 (嵌入) |
| **前端** | React 18 + Ant Design / shadcn/ui (待完善) |
| **构建部署** | Maven、Docker Compose |

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- PostgreSQL 15+ 且启用 `pgvector` 扩展
- Redis (可选，用于缓存)
- Node.js 20+ (前端开发需要)

### 1. 克隆项目
```bash
git clone https://github.com/your-username/mindflow.git
cd mindflow
```

### 2. 初始化数据库
- 在 MySQL 中创建 `mindflow` 数据库
- 在 PostgreSQL 中创建 `mindflow` 数据库并执行：
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```
（如果表未自动创建，可参考项目中的 `init.sql`）

### 3. 配置环境变量
```
OPENAI_API_KEY=你的LLM API密钥
MYSQL_PASSWORD=你的MySQL密码
PG_PASSWORD=你的PostgreSQL密码
```
项目默认使用 **智谱 AI** 的对话模型和 **硅基流动** 的嵌入模型。你可以修改 `application.yml` 中的 `base-url` 和模型名称来适配其他服务。

### 4. 启动后端
```bash
cd backend
mvn spring-boot:run
```
服务将在 `http://localhost:8080` 启动。

### 5. 启动前端（可选）
```bash
cd frontend
npm install
npm run dev
```
前端开发服务器将在 `http://localhost:5173` 运行。

### 6. 测试接口
用 Postman 或 curl 测试以下端点：
- 注册：`POST /api/auth/register`
- 登录：`POST /api/auth/login` （获取 JWT Token）
- 创建笔记：`POST /api/notes`
- 语义搜索：`POST /api/ai/search`
- AI 问答：`POST /api/ai/chat`
- AI 写作：`POST /api/ai/write`

（所有 `/api/**` 接口都需要在 Header 中添加 `Authorization: Bearer <token>`）

## 🗺 后续计划

### 短期（1-2 个月）
- ✅ 完善前端界面（React 三栏布局，Markdown 编辑器，AI 侧边栏）
- ✅ 支持 PDF / Word 文件上传与内容解析
- ✅ 实现跨会话记忆系统（Hermes 记忆思想落地）

### 中期（3-6 个月）
- ✅ 技能插件化：将搜索、写作、代码执行等抽象为标准技能接口，支持动态路由
- ✅ Agent 自进化机制：定时分析历史对话，提炼优化 Prompt 和技能模板
- ✅ 多模态支持：图片上传后调用视觉模型生成描述，纳入向量检索

### 长期
- ✅ 支持本地模型部署（如 Ollama、Chinese-CLIP），完全离线可用
- ✅ 实现 MCP 服务治理，集成企业微信、Jira 等外部工具
- ✅ 多用户协同与团队知识库

## 🤝 贡献

欢迎提交 Issue 和 PR！项目目前由个人维护，有任何建议和想法都可以在 GitHub 上交流。

## 📄 许可证

[MIT License](LICENSE)
