package com.mindflow.backend.controller;

import com.mindflow.backend.dto.request.AIChatRequest;
import com.mindflow.backend.dto.request.AIWriteRequest;
import com.mindflow.backend.repository.UserRepository;
import com.mindflow.backend.service.AIChatService;
import com.mindflow.backend.service.AIWriteService;
import com.mindflow.backend.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
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

    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody AIChatRequest request,
                                 Authentication authentication) {
        Long userId = getUserId(authentication);
        SseEmitter emitter = new SseEmitter(180_000L);

        Flux<String> stream = aiChatService.chatStream(userId, request.getQuestion());

        reactor.core.Disposable disposable = stream.subscribe(
            token -> {
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            error -> {
                try {
                    emitter.send(SseEmitter.event().data("[ERROR]" + error.getMessage()));
                } catch (IOException e) { /* ignore */ }
                emitter.completeWithError(error);
            },
            () -> {
                try {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                } catch (IOException e) { /* ignore */ }
                emitter.complete();
            }
        );

        emitter.onCompletion(disposable::dispose);
        emitter.onTimeout(disposable::dispose);
        emitter.onError(e -> disposable.dispose());

        return emitter;
    }

    @PostMapping("/write")
    public ResponseEntity<?> write(@RequestBody AIWriteRequest request,
                                   Authentication authentication) {
        String response = aiWriteService.process(request.getAction(), request.getContent());
        return ResponseEntity.ok(Map.of("result", response));
    }
}