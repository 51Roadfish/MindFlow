package com.mindflow.backend.service;

import com.mindflow.backend.dto.IntentResult;
import com.mindflow.backend.dto.response.AIChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    private final StreamingChatModel streamingChatModel;

    private static final String TOPIC_BOUNDARY_SYSTEM_PROMPT =
            "你是一个专注于学习、知识管理和技术讨论的 AI 助手。你可以回答日常闲聊问题，"
            + "但涉及违法、有害、暴力、色情或与学习知识无关的敏感话题，请礼貌拒绝回答。";

    private static final String REFUSE_MESSAGE = "抱歉，我无法回答这个问题。请确保问题与笔记内容、写作辅助或日常合理话题相关。";

    public AIChatResponse chat(Long userId, String question) {
        IntentResult intentResult = intentRouterService.analyze(question);
        log.info("用户提问意图解析结果: {}", intentResult);

        String answer;
        List<String> sources = null;

        switch (intentResult.intent()) {
            case IntentRouterService.INTENT_REFUSE:
                answer = REFUSE_MESSAGE;
                break;

            case IntentRouterService.INTENT_WRITE:
                answer = aiWriteService.process("continue", question);
                break;

            case IntentRouterService.INTENT_SEARCH:
                List<Document> docs = vectorStoreService.similaritySearch(userId, intentResult.query(), 5);

                if (docs == null || docs.isEmpty()) {
                    answer = "根据您当前的知识库笔记，我没有找到相关的参考信息。";
                } else {
                    String context = docs.stream()
                            .map(d -> "[" + d.getMetadata().get("noteTitle") + "]: " + d.getContent())
                            .collect(Collectors.joining("\n\n"));

                    String systemPrompt = TOPIC_BOUNDARY_SYSTEM_PROMPT + "\n"
                            + "你是一个知识库 AI 助手。请基于以下提取自个人知识库的笔记内容，来回答用户的原始问题。\n"
                            + "如果给出的笔记无法解答该问题，请如实回答说知识库中未包含相关内容。";

                    String userPrompt = "【笔记内容参考】:\n" + context + "\n\n"
                            + "【用户原始问题】: " + question;

                    Prompt prompt = new Prompt(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userPrompt)
                    ));
                    answer = chatModel.call(prompt).getResult().getOutput().getContent();

                    sources = docs.stream()
                            .map(d -> (String) d.getMetadata().get("noteTitle"))
                            .distinct()
                            .collect(Collectors.toList());
                }
                break;

            case IntentRouterService.INTENT_CHAT:
            default:
                Prompt chatPrompt = new Prompt(List.of(
                        new SystemMessage(TOPIC_BOUNDARY_SYSTEM_PROMPT),
                        new UserMessage(question)
                ));
                answer = chatModel.call(chatPrompt).getResult().getOutput().getContent();
                break;
        }

        return AIChatResponse.builder()
                .intent(intentResult.intent())
                .answer(answer)
                .sources(sources != null ? sources : Collections.emptyList())
                .build();
    }

    public Flux<String> chatStream(Long userId, String question) {
        IntentResult intentResult = intentRouterService.analyze(question);
        log.info("流式问答意图解析结果: {}", intentResult);

        switch (intentResult.intent()) {
            case IntentRouterService.INTENT_REFUSE:
                return Flux.just(REFUSE_MESSAGE);

            case IntentRouterService.INTENT_WRITE:
                String writeResult = aiWriteService.process("continue", question);
                return Flux.just(writeResult);

            case IntentRouterService.INTENT_SEARCH:
                List<Document> docs = vectorStoreService.similaritySearch(userId, intentResult.query(), 5);

                if (docs == null || docs.isEmpty()) {
                    return Flux.just("根据您当前的知识库笔记，我没有找到相关的参考信息。");
                }

                String context = docs.stream()
                        .map(d -> "[" + d.getMetadata().get("noteTitle") + "]: " + d.getContent())
                        .collect(Collectors.joining("\n\n"));

                String systemPrompt = TOPIC_BOUNDARY_SYSTEM_PROMPT + "\n"
                        + "你是一个知识库 AI 助手。请基于以下提取自个人知识库的笔记内容，来回答用户的原始问题。\n"
                        + "如果给出的笔记无法解答该问题，请如实回答说知识库中未包含相关内容。";

                String userPrompt = "【笔记内容参考】:\n" + context + "\n\n"
                        + "【用户原始问题】: " + question;

                Prompt ragPrompt = new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                ));

                return streamingChatModel.stream(ragPrompt)
                        .map(r -> r.getResult().getOutput().getContent());

            case IntentRouterService.INTENT_CHAT:
            default:
                Prompt chatPrompt = new Prompt(List.of(
                        new SystemMessage(TOPIC_BOUNDARY_SYSTEM_PROMPT),
                        new UserMessage(question)
                ));
                return streamingChatModel.stream(chatPrompt)
                        .map(r -> r.getResult().getOutput().getContent());
        }
    }
}
