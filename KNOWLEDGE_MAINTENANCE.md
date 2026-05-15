# MindFlow 知识库防腐机制

> 知识库的生命力在于持续演进。如果 Skill 文件与代码脱节，AI Agent 将产生"幻觉"——引用已不存在的类名、已改变的流程。
>
> **核心原则：知识变更与代码变更同生命周期、分阶段审查。** 知识更新的采纳必须滞后于代码采纳。

---

## 四阶段防腐机制总览

```
阶段 1 ─ 开发中：输出实现方案 + 校验报告
    ↓ 人工确认后更新 Skill
阶段 2 ─ 开发前：沟通补充 Skill 盲区
    ↓ Agent 总结后自行更新 Skill
阶段 3 ─ 提交前：代码 diff 命中分析 → 结构化更新建议
    ↓ Review 后自动更新 Skill
阶段 4 ─ 合并到基线时：全量健康巡检
    ↓ 生成健康报告，标记腐化
```

| 阶段 | 机制 | 触发时机 | 产出 | 更新方式 |
|------|------|---------|------|---------|
| 1 | 校验报告 | 开发中，代码输出时 | 严重程度分级的不一致报告 | 人工确认后更新 |
| 2 | 沟通补充 | 开发前，探讨盲区后 | Agent 总结的新知识摘要 | Agent 自行更新 |
| 3 | Diff 影响分析 | 提交代码前 | 结构化更新建议（模块+流程级） | Review 后自动更新 |
| 4 | 全量巡检 | 合并到基线时 | 健康评分 + 腐化清单 | 人工修复 |

---

## 阶段 1 — 开发中：实现方案与校验报告

### 原理

AI Agent 输出实现方案的同时，额外生成一份校验报告。报告对比 Skill 中的已有描述与实际代码之间的差异，标注严重程度。开发者确认后，Agent 更新对应 Skill。

### 校验报告模板

```
# 知识校验报告 — {模块名}

## 变更概要
- 需求: {需求描述}
- 涉及 Skill: .skills/skill-{模块}.md
- 涉及代码: {文件列表}

## 不一致清单

| # | 严重程度 | 模块 | Skill 描述 | 实际代码 | 修正建议 |
|---|---------|------|-----------|---------|---------|
| 1 | HIGH | 笔记管理 | 软删除暂未实现 | NoteServiceImpl.java:87 仍为物理 delete | 按"常见变更模式"中的软删除6步改造 |
| 2 | MEDIUM | 笔记管理 | content_text字段预留 | 仅DDL存在，Service层无赋值 | 使用前需同步NoteServiceImpl.mapToResponse() |
| 3 | LOW | 意图路由 | DecideNextComponent参与了决策 | flow.el.xml中review_answer_chain仅含scoreAnswerComponent | 更新Skill描述，或决定是否接入 |

## 严重程度定义
- **HIGH**: Skill 描述与代码行为矛盾，会导致 Agent 产生错误判断
- **MEDIUM**: Skill 描述不准确或遗漏了关键细节
- **LOW**: Skill 描述过时但不会导致错误行为
```

### 人工确认流程

```
Agent 输出校验报告 →
  开发者 Review 确认每项差异 →
  标注"确认修复"或"已知暂不处理" →
  Agent 根据确认结果更新 Skill 文件
```

---

## 阶段 2 — 开发前：沟通补充 Skill 盲区

### 原理

开发者在需求开发前与 Agent 反复探讨业务背景、规则、约束。沟通过程中产生的新知识，Agent 必须在沟通结束后总结并追加到对应 Skill 文件。

### 适合触发此阶段的情况

- Agent 提问"这个场景的预期行为是什么？"——说明 Skill 没覆盖
- 开发者主动纠正 Agent 的理解——说明 Skill 描述不准确
- 开发者提供额外的业务文档或口头规则——说明 Skill 有缺失

### Agent 总结模板

沟通结束后，Agent 执行：

```
## 知识补充：{模块名}

### 本次补充的知识点

1. {知识点标题}
   - 背景: {为什么需要这个知识}
   - 规则: {具体的业务规则或约束}
   - 影响范围: {涉及哪些代码文件}
   - 来源: {开发者的描述/提供的文档}

2. ...

### Skill 文件更新动作

- [ ] .skills/skill-{模块}.md → 追加或修改对应章节
- [ ] .skills/details/{模块}/data-model.md（如果有数据变更）
- [ ] .skills/details/{模块}/api-contracts.md（如果有接口变更）

### 是否需要回溯修改已有 Skill

- [ ] 本次补充的知识导致之前其他模块的 Skill 描述需要修正
```

### 示例

```
## 知识补充：笔记管理

### 本次补充的知识点

1. 笔记的 content_text 字段用途
   - 背景: 当前 DDL 有 content_text 列但 Service 层从未写入
   - 规则: 该字段为纯文本版本预留，用于后续 MySQL 全文索引搜索
   - 影响范围: Note.java:34 → 纯文本需在保存时从 Markdown 剥离生成
   - 来源: 开发者口头说明

### Skill 文件更新动作

- [x] .skills/skill-笔记管理.md → 更新规则6，补充"用于全文索引"的背景
```

---

## 阶段 3 — 提交前：代码 Diff 影响分析

### 原理

代码提交前，自动分析本次代码 diff 是否命中知识更新的触发规则。命中后，Agent 自动分析变更对知识体系的影响，生成结构化的更新建议——精确到哪个业务模块、哪个核心流程的知识需要同步修改。Review 无问题后，自动更新到对应的 Skill 模块。

### 触发规则

如果本次 diff 满足以下任一条件，必须执行影响分析：

| 规则 | 条件 | 举例 |
|------|------|------|
| 接口变更 | `.java` 中新增/修改/删除了 `@RequestMapping`、`@PostMapping` 等 | 新增 API 端点 |
| 数据模型变更 | `.java` 中新增/修改/删除了 `@Entity` 的 `@Column` 字段 | 新增笔记封面图字段 |
| Prompt 变更 | `.java` 中新增/修改了包含 Prompt 模板的字符串 | 修改 RAG 问答 Prompt |
| 流程节点变更 | `.java` 中新增/删除了继承 `NodeComponent` 的类 | 新增 LiteFlow 组件 |
| LiteFlow 链变更 | `flow.el.xml` 中的 chain 定义被修改 | 调整 review_start_chain |
| 配置变更 | `application.yml` 中 AI/DB/缓存相关配置变化 | 修改 Embedding 模型 |

### 影响分析报告模板

```
## 知识影响分析 — {PR标题}

### 变更摘要
- {变更1}: {文件A} → {描述}
- {变更2}: {文件B} → {描述}

### 命中触发规则
- [x] 规则: {规则名} — 详情: {具体命中内容}

### 影响范围

| 受影响的 Skill | 影响类型 | 需要修改的章节 | 建议动作 |
|---------------|---------|--------------|---------|
| skill-笔记管理.md | 数据模型 | 关键业务规则与不变量 → 规则X | 新增字段描述 |
| skill-笔记管理.md | 代码流程 | 创建笔记流程 → 第X步 | 补充新字段赋值步骤 |
| details/笔记管理/data-model.md | 数据模型 | Note 表 | 添加字段行 |

### 详细建议

**skill-笔记管理.md — 新增规则X:**
```markdown
N. **封面图字段**
   → Note 实体新增 coverImage 字段，存储 Base64 编码的图片
   → 实现: NoteServiceImpl.createNote() 第X行赋值
   → 位置: domain/Note.java:35-38
```

### Review 确认
- [ ] 以上分析无误（Developer 确认）
- [ ] 自动更新对应 Skill 文件（Agent 执行）
```

---

## 阶段 4 — 合并到基线时：全量健康巡检

### 原理

每次代码合并到主线后，执行全量健康巡检，扫描所有代码锚点有效性、比对知识中的接口定义与实际路由、比对数据模型描述与最新表结构、统计知识覆盖度和新鲜度。生成健康报告。

### GitHub Actions Workflow

```yaml
# .github/workflows/knowledge-audit.yml
name: Knowledge Base Health Check

on:
  push:
    branches: [main, develop]
  schedule:
    - cron: '0 2 * * 1'  # 每周一凌晨
  workflow_dispatch:

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: 全量知识健康巡检
        run: |
          mkdir -p audit-report
          echo "# 知识库全量健康巡检报告" > audit-report/report.md
          echo "巡检时间: $(date)" >> audit-report/report.md
          echo "基线版本: $(git rev-parse HEAD)" >> audit-report/report.md
          echo "" >> audit-report/report.md

          # === 检查清单 ===
          # 1. 代码锚点有效性
          echo "## 1. 代码锚点有效性" >> audit-report/report.md
          echo "| Skill 文件 | 引用类型 | 引用内容 | 状态 |" >> audit-report/report.md
          echo "|-----------|---------|---------|------|" >> audit-report/report.md
          
          TOTAL_REFS=0
          VALID_REFS=0

          for skill_file in .skills/skill-*.md; do
            module=$(basename "$skill_file" .md)
            
            # 检查文件路径引用
            for ref in $(grep -oP '\`[A-Za-z0-9_/.-]+\.java:[0-9-]+\`' "$skill_file" | tr -d '`'); do
              TOTAL_REFS=$((TOTAL_REFS + 1))
              file_path=$(echo "$ref" | cut -d: -f1)
              full_path="backend/src/main/java/com/mindflow/backend/$file_path"
              if [ -f "$full_path" ]; then
                echo "| $module | 文件路径 | $ref | ✅ |" >> audit-report/report.md
                VALID_REFS=$((VALID_REFS + 1))
              else
                echo "| $module | 文件路径 | $ref | ❌ 文件不存在 |" >> audit-report/report.md
              fi
            done
          done

          # 2. API 接口比对（扫描实际路由 vs AGENTS.md 中的 API 速查表）
          echo "" >> audit-report/report.md
          echo "## 2. API 接口一致性" >> audit-report/report.md
          
          ACTUAL_ENDPOINTS=$(grep -roP '@(Post|Get|Put|Delete)Mapping\("\K[^"]+' backend/src/main/java/com/mindflow/backend/controller/ | sort)
          
          
          # 对比 AGENTS.md 中记录的端点
          while IFS= read -r endpoint; do
            if grep -q "$endpoint" AGENTS.md 2>/dev/null; then
              echo "- $endpoint ✅ 已记录" >> audit-report/report.md
            else
              echo "- $endpoint ❌ AGENTS.md 中缺失" >> audit-report/report.md
            fi
          done <<< "$ACTUAL_ENDPOINTS"

          # 3. 数据模型比对（扫描 @Column 注解 vs data-model.md）
          echo "" >> audit-report/report.md
          echo "## 3. 数据模型一致性" >> audit-report/report.md
          for data_model in .skills/details/*/data-model.md; do
            if [ -f "$data_model" ]; then
              echo "### $data_model" >> audit-report/report.md
            fi
          done

          # 4. 健康评分
          if [ "$TOTAL_REFS" -gt 0 ]; then
            SCORE=$((VALID_REFS * 100 / TOTAL_REFS))
          else
            SCORE=100
          fi
          
          echo "" >> audit-report/report.md
          echo "## 4. 总体健康评分" >> audit-report/report.md
          echo "- 代码锚点总引用数: $TOTAL_REFS" >> audit-report/report.md
          echo "- 有效引用数: $VALID_REFS" >> audit-report/report.md
          echo "- 健康评分: **${SCORE}%**" >> audit-report/report.md
          
          # 知识新鲜度：Skill 文件最后修改时间
          echo "" >> audit-report/report.md
          echo "## 5. 知识新鲜度" >> audit-report/report.md
          echo "| Skill 文件 | 最后修改时间 | 状态 |" >> audit-report/report.md
          echo "|-----------|-------------|------|" >> audit-report/report.md
          for skill_file in .skills/skill-*.md; do
            mtime=$(stat -c '%Y' "$skill_file" 2>/dev/null || stat -f '%m' "$skill_file" 2>/dev/null)
            now=$(date +%s)
            days_old=$(( (now - mtime) / 86400 ))
            if [ "$days_old" -gt 30 ]; then
              echo "| $skill_file | ${days_old}天前 | ⚠️ 超过30天未更新 |" >> audit-report/report.md
            else
              echo "| $skill_file | ${days_old}天前 | ✅ |" >> audit-report/report.md
            fi
          done

          echo "SCORE=$SCORE" >> $GITHUB_ENV

      - name: 健康评分低于阈值则创建 Issue
        if: env.SCORE < 85
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('audit-report/report.md', 'utf8');
            await github.rest.issues.create({
              owner: context.repo.owner,
              repo: context.repo.repo,
              title: `🧹 知识库健康巡检 — 评分 ${process.env.SCORE}%`,
              body: report,
              labels: ['knowledge-base', 'tech-debt']
            });

      - name: 上传巡检报告
        uses: actions/upload-artifact@v4
        with:
          name: knowledge-audit-report
          path: audit-report/report.md
```

---

## 四阶段协作流程

```
                        ┌──────────────────────────────┐
                        │     需求启动                  │
                        └──────────┬───────────────────┘
                                   │
                   ┌───────────────▼───────────────────┐
                   │  阶段 2: 沟通补充                 │
                   │  Agent 与开发者探讨 Skill 盲区    │
                   │  → Agent 总结新知识 → 更新 Skill  │
                   └───────────────┬───────────────────┘
                                   │
                   ┌───────────────▼───────────────────┐
                   │  阶段 1: 开发中                   │
                   │  Agent 输出代码 + 校验报告         │
                   │  → 开发者确认不一致项              │
                   │  → Agent 更新 Skill               │
                   └───────────────┬───────────────────┘
                                   │
                   ┌───────────────▼───────────────────┐
                   │  阶段 3: 提交前                   │
                   │  分析代码 diff 命中规则           │
                   │  → 生成结构化更新建议              │
                   │  → Review 确认 → 自动更新 Skill    │
                   └───────────────┬───────────────────┘
                                   │
                   ┌───────────────▼───────────────────┐
                   │  git commit + push                │
                   └───────────────┬───────────────────┘
                                   │
                   ┌───────────────▼───────────────────┐
                   │  阶段 4: 合并到基线时              │
                   │  全量健康巡检                      │
                   │  → 锚点/接口/数据模型比对          │
                   │  → 新鲜度统计 → 健康评分           │
                   │  → 评分<85% 自动创建 Issue         │
                   └───────────────┬───────────────────┘
                                   │
                                   ▼
                            基线发布完成
```

### 每个阶段解决的问题

| 阶段 | 解决的问题 | 发现时机 | 修复成本 |
|------|-----------|---------|---------|
| 2 沟通补充 | Agent 对业务规则理解有盲区 | 开发之前 | 最低，零代码变更 |
| 1 校验报告 | 代码与 Skill 描述不一致 | 代码输出时 | 低，上下文还在 |
| 3 Diff 影响分析 | 不知道代码变更会影响哪些知识模块 | 提交之前 | 中，需 review |
| 4 全量巡检 | 长期累积的腐化无人发现 | 合并到基线时 | 高，需逐项审计 |

### 核心原则重申

**知识更新的采纳必须滞后于代码采纳：**
- 阶段 1 的校验报告 → 人工确认后才更新 Skill
- 阶段 3 的 diff 影响分析 → Review 后才自动更新
- 阶段 2 的沟通补充 → 知识在开发前就沉淀，不存在代码被否决的问题
- 阶段 4 的巡检报告 → 只生成报告，不自动修改，需人工修复
