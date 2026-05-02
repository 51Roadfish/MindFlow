package com.mindflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIChatService {

    private final VectorStoreService vectorStoreService;
    private final ChatModel chatModel;

    public String chat(Long userId, String question) {
        List<Document> docs = vectorStoreService.similaritySearch(userId, question, 5);
        String context = docs.stream().map(d -> "[" + d.getMetadata().get("noteTitle") + "]: " + d.getContent()).collect(Collectors.joining("\\n\\n"));
        
        String prompt = "You are a knowledge base assistant. Use the following notes to answer the user's question. If you don't know, say so. Notes:\\n" + context + "\\n\\nQuestion: " + question;
        
        return chatModel.call(prompt);
    }
}
