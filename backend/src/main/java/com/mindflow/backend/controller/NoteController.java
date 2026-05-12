package com.mindflow.backend.controller;

import com.mindflow.backend.dto.request.NoteCreateRequest;
import com.mindflow.backend.dto.request.NoteUpdateRequest;
import com.mindflow.backend.dto.response.NoteResponse;
import com.mindflow.backend.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@RequestBody NoteCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(noteService.createNote(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable Long id, @RequestBody NoteUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(noteService.updateNote(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, Authentication authentication) {
        noteService.deleteNote(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNote(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(noteService.getNote(id, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAllNotes(
            @RequestParam(required = false) List<String> tags,
            Authentication authentication) {
        return ResponseEntity.ok(noteService.getAllNotes(authentication.getName(), tags));
    }
}
