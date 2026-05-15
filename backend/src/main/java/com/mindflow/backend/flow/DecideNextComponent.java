package com.mindflow.backend.flow;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("decideNextComponent")
@RequiredArgsConstructor
public class DecideNextComponent extends NodeComponent {

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

        String prompt = """
            你是一个教学决策者。根据学生的答题情况决定下一步：
            - "follow_up"：针对同一知识点追问更深入的问题（得分 < 80 或概念有遗漏）
            - "next_question"：切换到新的知识点出题
            - "complete"：所有知识点已覆盖或达到最大题数
            返回 JSON：{"action": "...", "reason": "..."}
            只返回 JSON。
            当前得分：%d
            已答题数：%d
            最大题数：%d
            """.formatted(reviewCtx.getLastScore(), reviewCtx.getTurnCount(), maxQuestions);

        String response = chatModel.call(new Prompt(List.of(
                new SystemMessage(prompt)
        ))).getResult().getOutput().getContent();

        Map<String, String> result = AiJsonHelper.parse(response, Map.class);
        reviewCtx.setNextAction(result.getOrDefault("action", "next_question"));
    }
}
