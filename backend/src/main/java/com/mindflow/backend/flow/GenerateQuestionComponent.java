package com.mindflow.backend.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.backend.dto.Question;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("generateQuestionComponent")
@RequiredArgsConstructor
public class GenerateQuestionComponent extends NodeComponent {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public void process() throws Exception {
        ReviewContext reviewCtx = (ReviewContext) getRequestData();

        String notesContent = reviewCtx.getNotesContent();
        String historyJson = objectMapper.writeValueAsString(reviewCtx.getHistory());

        String systemPrompt = """
            你是一个智能考官。基于用户的笔记内容生成一道有助于理解与记忆的题目。
            要求：
            - 题目应覆盖笔记中的关键知识点
            - 避免与已出题目重复
            - 返回严格的 JSON 格式：{"question": "...", "expectedAnswer": "..."}
            只返回 JSON，不要多余的解释。
            """;

        String userPrompt = "笔记内容：\n" + notesContent
                + "\n\n已出题目：\n" + historyJson;

        String response = chatModel.call(new Prompt(List.of(
                new SystemMessage(systemPrompt), new UserMessage(userPrompt)
        ))).getResult().getOutput().getContent();

        Question q = AiJsonHelper.parse(response, Question.class);
        q.setQuestionId(UUID.randomUUID().toString());

        reviewCtx.getHistory().add(q);
        reviewCtx.setTurnCount(reviewCtx.getHistory().size());
    }
}
