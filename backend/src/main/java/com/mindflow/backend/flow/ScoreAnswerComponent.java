package com.mindflow.backend.flow;

import com.mindflow.backend.dto.Question;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("scoreAnswerComponent")
@RequiredArgsConstructor
public class ScoreAnswerComponent extends NodeComponent {

    private final ChatModel chatModel;

    @Value("${review.max-questions-per-session:20}")
    private int maxQuestions;

    @Override
    public void process() throws Exception {
        ReviewContext reviewCtx = (ReviewContext) getRequestData();

        if (reviewCtx.getTurnCount() >= maxQuestions) {
            reviewCtx.setNextAction("complete");
            return;
        }

        Question current = reviewCtx.getHistory().isEmpty() ? null
                : reviewCtx.getHistory().get(reviewCtx.getHistory().size() - 1);

        String prompt = """
            你是一个评分老师。请根据题目、参考答案和学生答案进行评分，并决定下一步。
            评分标准：
            - 理解核心概念即可得分，不要求与参考答案完全一致
            - 评分范围 0 到 100 分
            决定下一步：
            - "follow_up"：针对同一知识点追问（得分 < 80 或概念有遗漏）
            - "next_question"：切换到新知识点
            - "complete"：所有知识点已覆盖
            返回 JSON 格式：{"score": 整数, "feedback": "评语", "nextAction": "follow_up/next_question/complete"}
            只返回 JSON，不要多余的解释。
            题目：%s
            参考答案：%s
            学生答案：%s
            """.formatted(current != null ? current.getQuestion() : "",
                current != null ? current.getExpectedAnswer() : "",
                reviewCtx.getUserAnswer());

        String response = chatModel.call(prompt);

        Map<String, Object> result = AiJsonHelper.parse(response, Map.class);
        reviewCtx.setLastScore(((Number) result.getOrDefault("score", 0)).intValue());
        reviewCtx.setLastFeedback((String) result.getOrDefault("feedback", ""));
        reviewCtx.setNextAction((String) result.getOrDefault("nextAction", "next_question"));
    }
}
