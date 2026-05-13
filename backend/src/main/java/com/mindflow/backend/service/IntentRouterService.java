package com.mindflow.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.backend.dto.IntentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 意图路由服务：用于通过大模型分析用户的真实意图，并提取检索词。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRouterService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // 意图常量定义
    public static final String INTENT_CHAT = "CHAT";
    public static final String INTENT_SEARCH = "SEARCH";
    public static final String INTENT_WRITE = "WRITE";

    /**
     * 分析用户消息意图
     * @param userMessage 用户输入的文本
     * @return 意图和优化后的查询词
     */
    public IntentResult analyze(String userMessage) {
        String systemPrompt = """
            你是一个意图识别引擎。请根据用户的输入分析他们的真实意图，并返回严格的 JSON 格式数据。
            意图分类如下：
            1. CHAT：日常对话、闲聊、通用问候，或无需专有知识库就能回答的普遍性问题。
            2. SEARCH：明确询问个人的笔记、知识库里的信息，或描述需要检索相关资料才能解答的疑问。
            3. WRITE：要求大模型进行续写、润色、长篇总结、起草文章等专门的写作任务。
            
            要求：
            - 如果意图是 SEARCH，请从用户输入中提取或优化出一个“最佳检索词”(query)，滤除语气词以提升向量检索命中率。
            - 如果是 CHAT 或 WRITE，query 保持原问题核心即可。
            
            输出示例：
            {"intent": "SEARCH", "query": "Spring Boot 核心特性"}
            
            警告：必须且只能返回合法的 JSON 字符串，不要使用 Markdown 语法或多余的解释文本。
            """;
            
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
            ));
            
            String response = chatModel.call(prompt).getResult().getOutput().getContent();
            
            // 清理可能存在的 markdown code block 等杂余信息
            response = response.replaceAll("```json", "").replaceAll("```", "").trim();
            
            return objectMapper.readValue(response, IntentResult.class);
        } catch (Exception e) {
            log.error("分析用户意图失败，将回退到默认的 CHAT 意图", e);
            // 解析失败或大模型调用失败时，默认按闲聊处理
            return new IntentResult(INTENT_CHAT, userMessage);
        }
    }
}
