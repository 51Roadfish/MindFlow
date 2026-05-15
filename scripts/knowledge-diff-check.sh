#!/bin/bash
# scripts/knowledge-diff-check.sh
# 阶段 3：提交前 Diff 影响分析
# 分析 git diff 是否命中知识更新触发规则，输出结构化报告
# 使用方法: bash scripts/knowledge-diff-check.sh
# 输出: knowledge-diff-report.md

SKILLS_DIR=".skills"
DIFF_FILE=$(mktemp)
REPORT="knowledge-diff-report.md"

git diff --cached --diff-filter=ACMR > "$DIFF_FILE"

echo "# 知识影响分析报告" > "$REPORT"
echo "生成时间: $(date)" >> "$REPORT"
echo "提交信息: $(git log -1 --pretty=%s 2>/dev/null || echo '未提交')" >> "$REPORT"
echo "" >> "$REPORT"

HIT_COUNT=0

# 触发规则 1: 接口变更
ENDPOINT_CHANGES=$(grep -oP '@(Post|Get|Put|Delete)Mapping\("[^"]+"' "$DIFF_FILE")
if [ -n "$ENDPOINT_CHANGES" ]; then
  HIT_COUNT=$((HIT_COUNT + 1))
  echo "## 命中规则: 接口变更" >> "$REPORT"
  echo '```' >> "$REPORT"
  echo "$ENDPOINT_CHANGES" >> "$REPORT"
  echo '```' >> "$REPORT"
  echo "建议更新: AGENTS.md 中的 API 速查表 + 对应模块的 api-contracts.md" >> "$REPORT"
  echo "" >> "$REPORT"
fi

# 触发规则 2: 数据模型变更
COLUMN_CHANGES=$(grep -oP '@Column\([^)]*\)' "$DIFF_FILE")
FIELD_CHANGES=$(grep -oP 'private\s+\S+\s+\w+\s*;' "$DIFF_FILE")
if [ -n "$COLUMN_CHANGES" ] || [ -n "$FIELD_CHANGES" ]; then
  HIT_COUNT=$((HIT_COUNT + 1))
  echo "## 命中规则: 数据模型变更" >> "$REPORT"

  ENTITY_FILE=$(grep -l '@Entity' "$DIFF_FILE" 2>/dev/null || grep -oP '^\+\+\+ b/\K[^ ]+' "$DIFF_FILE" | grep -v /dev/null)
  if [ -n "$ENTITY_FILE" ]; then
    echo "变更实体: $ENTITY_FILE" >> "$REPORT"
    echo "建议更新: 对应模块 data-model.md + skill 主文件中的关键业务规则" >> "$REPORT"
  fi
  echo "" >> "$REPORT"
fi

# 触发规则 3: Prompt 变更
PROMPT_CHANGES=$(grep -i 'prompt\|SystemMessage\|UserMessage' "$DIFF_FILE")
if [ -n "$PROMPT_CHANGES" ]; then
  HIT_COUNT=$((HIT_COUNT + 1))
  echo "## 命中规则: Prompt 模板变更" >> "$REPORT"
  echo "影响模块: $(grep -oP '^\+\+\+ b/\K[^ ]+' "$DIFF_FILE" | grep -v /dev/null)" >> "$REPORT"
  echo "建议更新: 对应模块 details/ 下的 prompt-templates.md" >> "$REPORT"
  echo "" >> "$REPORT"
fi

# 触发规则 4: LiteFlow 链变更
LF_CHANGES=$(grep -E '(chain name=|NodeComponent)' "$DIFF_FILE")
if [ -n "$LF_CHANGES" ]; then
  HIT_COUNT=$((HIT_COUNT + 1))
  echo "## 命中规则: LiteFlow 链/组件变更" >> "$REPORT"
  echo '```' >> "$REPORT"
  echo "$LF_CHANGES" >> "$REPORT"
  echo '```' >> "$REPORT"
  echo "建议更新: skill-AI复习与出题.md — LiteFlow 链定义 + 核心代码流程" >> "$REPORT"
  echo "" >> "$REPORT"
fi

if [ "$HIT_COUNT" -eq 0 ]; then
  echo "本次提交未命中任何知识更新触发规则" >> "$REPORT"
fi

echo "---" >> "$REPORT"
echo "共命中 $HIT_COUNT 条规则" >> "$REPORT"

rm "$DIFF_FILE"
cat "$REPORT"
