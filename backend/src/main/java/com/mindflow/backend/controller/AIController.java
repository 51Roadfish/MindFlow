package com.mindflow.backend.controller;

import com.mindflow.backend.dto.request.AIChatRequest;
import com.mindflow.backend.dto.request.AIWriteRequest;
import com.mindflow.backend.repository.UserRepository;
import com.mindflow.backend.service.AIChatService;
import com.mindflow.backend.service.AIWriteService;
import com.mindflow.backend.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final VectorStoreService vectorStoreService;
    private final AIChatService aiChatService;
    private final AIWriteService aiWriteService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> request,
                                    Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(
                vectorStoreService.similaritySearch(userId, request.get("query"), 10));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AIChatRequest request,
                                  Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(aiChatService.chat(userId, request.getQuestion()));
    }

    @PostMapping("/write")
    public ResponseEntity<?> write(@RequestBody AIWriteRequest request,
                                   Authentication authentication) {
        String response = aiWriteService.process(request.getAction(), request.getContent());
        return ResponseEntity.ok(Map.of("result", response));
    }
}