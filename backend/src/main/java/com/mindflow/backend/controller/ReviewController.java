package com.mindflow.backend.controller;

import com.mindflow.backend.dto.request.ReviewAnswerRequest;
import com.mindflow.backend.dto.request.ReviewStartRequest;
import com.mindflow.backend.domain.ReviewSession;
import com.mindflow.backend.dto.response.ReviewAnswerResponse;
import com.mindflow.backend.dto.response.ReviewSessionResponse;
import com.mindflow.backend.repository.UserRepository;
import com.mindflow.backend.dto.response.ExamPaperResponse;
import com.mindflow.backend.service.ExamService;
import com.mindflow.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ExamService examService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    @PostMapping("/start")
    public ResponseEntity<ReviewSessionResponse> start(@RequestBody ReviewStartRequest request,
                                                       Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(reviewService.start(request, userId));
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<ReviewAnswerResponse> answer(@PathVariable Long sessionId,
                                                       @RequestBody ReviewAnswerRequest request,
                                                       Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(reviewService.answer(sessionId, request, userId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ReviewSessionResponse> getSession(@PathVariable Long sessionId,
                                                            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(reviewService.getSession(sessionId, userId));
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ReviewSessionResponse> endSession(@PathVariable Long sessionId,
                                                            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(reviewService.endSession(sessionId, userId));
    }

    @PostMapping("/{sessionId}/exam")
    public ResponseEntity<ExamPaperResponse> generateExamFromReview(@PathVariable Long sessionId,
                                                                     Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(examService.generateFromReview(sessionId, userId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReviewSession>> history(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(reviewService.history(userId));
    }
}
