package com.mindflow.backend.controller;

import com.mindflow.backend.dto.request.ExamGenerateRequest;
import com.mindflow.backend.dto.response.ExamPaperResponse;
import com.mindflow.backend.repository.UserRepository;
import com.mindflow.backend.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    @PostMapping("/generate")
    public ResponseEntity<ExamPaperResponse> generate(@Valid @RequestBody ExamGenerateRequest request,
                                                      Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(examService.generate(request, userId));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamPaperResponse> getExam(@PathVariable Long examId,
                                                     Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(examService.getExam(examId, userId));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ExamPaperResponse>> listExams(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(examService.listExams(userId));
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long examId,
                                           Authentication authentication) {
        Long userId = getUserId(authentication);
        examService.deleteExam(examId, userId);
        return ResponseEntity.noContent().build();
    }
}
