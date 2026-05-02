package com.mindflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIWriteService {

    private final ChatModel chatModel;

    public String process(String action, String content) {
        String prompt;
        switch (action.toLowerCase()) {
            case "continue": prompt = "Please continue writing the following markdown note: " + content; break;
            case "polish": prompt = "Polish the following markdown content to be more fluent and professional: " + content; break;
            case "summarize": prompt = "Summarize the following markdown content in under 100 words: " + content; break;
            default: throw new IllegalArgumentException("Unsupported action");
        }
        return chatModel.call(prompt);
    }
}
