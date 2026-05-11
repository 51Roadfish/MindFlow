package com.mindflow.backend.controller;

import com.mindflow.backend.dto.request.NoteBookCreateRequest;
import com.mindflow.backend.dto.request.NoteBookUpdateRequest;
import com.mindflow.backend.dto.response.NoteBookResponse;
import com.mindflow.backend.service.NotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    @PostMapping
    public ResponseEntity<NoteBookResponse> createNotebook(@RequestBody NoteBookCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(notebookService.createNotebook(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteBookResponse> updateNotebook(@PathVariable Long id, @RequestBody NoteBookUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(notebookService.updateNotebook(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotebook(@PathVariable Long id, Authentication authentication) {
        notebookService.deleteNotebook(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteBookResponse> getNotebook(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(notebookService.getNotebook(id, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<NoteBookResponse>> getAllNotebooks(Authentication authentication) {
        return ResponseEntity.ok(notebookService.getAllNotebooks(authentication.getName()));
    }
}
