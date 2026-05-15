package com.mindflow.backend.flow;

import com.mindflow.backend.dto.ExamQuestion;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("generateExamComponent")
@RequiredArgsConstructor
public class GenerateExamComponent extends NodeComponent {

    private final ChatModel chatModel;

    @Override
    public void process() throws Exception {
        ExamContext examCtx = (ExamContext) getRequestData();

        String systemPrompt = """
            你是一个出卷老师。根据用户的笔记内容生成一套完整的试卷。
            要求：
            - 题型混合：简答题和选择题
            - 选择题需包含 4 个选项
            - 每题标注分值
            - 返回 JSON 格式：
            {"title": "试卷标题", "questions": [
              {"id": 1, "type": "short_answer", "question": "...", "answer": "...", "points": 10},
              {"id": 2, "type": "multiple_choice", "question": "...", "options": ["A. ...", "B. ...", "C. ...", "D. ..."], "answer": "A", "points": 10}
            ]}
            只返回 JSON，不要多余的解释。
            """;

        String userPrompt = "笔记内容：\n" + examCtx.getNotesContent()
                + "\n\n请生成 " + examCtx.getQuestionCount() + " 道题目。"
                + (examCtx.getWeakAreas() != null && !examCtx.getWeakAreas().isEmpty()
                    ? "\n\n特别注意：该学生以下知识点薄弱，请重点出这些方向的题目：\n" + examCtx.getWeakAreas()
                    : "");

        String response = chatModel.call(new Prompt(List.of(
                new SystemMessage(systemPrompt), new UserMessage(userPrompt)
        ))).getResult().getOutput().getContent();

        MapWithTitle result = AiJsonHelper.parse(response, MapWithTitle.class);
        examCtx.setExamTitle(result.title);
        examCtx.setGeneratedExam(result.questions);
    }

    @lombok.Data
    static class MapWithTitle {
        private String title;
        private List<ExamQuestion> questions;
    }
}
