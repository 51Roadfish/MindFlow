package com.mindflow.backend.service;

import com.mindflow.backend.dto.IntentResult;
import com.mindflow.backend.dto.response.AIChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final IntentRouterService intentRouterService;
    private final VectorStoreService vectorStoreService;
    private final AIWriteService aiWriteService;
    private final ChatModel chatModel;

    public AIChatResponse chat(Long userId, String question) {
        // 第一步：调用 IntentRouterService 分析意图
        IntentResult intentResult = intentRouterService.analyze(question);
        log.info("用户提问意图解析结果: {}", intentResult);

        String answer;
        List<String> sources = null;

        // 第二步：根据不同意图进行动态路由调度
        switch (intentResult.intent()) {
            case IntentRouterService.INTENT_WRITE:
                // 对于写作意图，调用 AIWriteService
                // 默认传递 continue，如果是具体的要求将在 prompt 或后续模型处理
                answer = aiWriteService.process("continue", question);
                break;

            case IntentRouterService.INTENT_SEARCH:
                // 对于检索意图进行 RAG：检索个人笔记，拼装上下文回答
                List<Document> docs = vectorStoreService.similaritySearch(userId, intentResult.query(), 5);
                
                if (docs == null || docs.isEmpty()) {
                    answer = "根据您当前的知识库笔记，我没有找到相关的参考信息。";
                } else {
                    String context = docs.stream()
                            .map(d -> "[" + d.getMetadata().get("noteTitle") + "]: " + d.getContent())
                            .collect(Collectors.joining("\n\n"));

                    String prompt = "你是一个知识库 AI 助手。请基于以下提取自个人知识库的笔记内容，来回答用户的原始问题。\n"
                            + "如果给出的笔记无法解答该问题，请如实回答说知识库中未包含相关内容。\n\n"
                            + "【笔记内容参考】:\n" + context + "\n\n"
                            + "【用户原始问题】: " + question;
                            
                    answer = chatModel.call(prompt);
                    
                    // 收集检索来源
                    sources = docs.stream()
                            .map(d -> (String) d.getMetadata().get("noteTitle"))
                            .distinct()
                            .collect(Collectors.toList());
                }
                break;

            case IntentRouterService.INTENT_CHAT:
            default:
                // 普通聊天意图或默认情形，不经过 RAG 检索，直接向模型请求
                answer = chatModel.call(question);
                break;
        }

        // 统一组装响应结果
        return AIChatResponse.builder()
                .intent(intentResult.intent())
                .answer(answer)
                .sources(sources != null ? sources : Collections.emptyList())
                .build();
    }
}
