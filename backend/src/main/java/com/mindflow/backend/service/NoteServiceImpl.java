package com.mindflow.backend.service;

import com.mindflow.backend.domain.Note;
import com.mindflow.backend.domain.User;
import com.mindflow.backend.dto.request.NoteCreateRequest;
import com.mindflow.backend.dto.request.NoteUpdateRequest;
import com.mindflow.backend.dto.response.NoteResponse;
import com.mindflow.backend.repository.NoteRepository;
import com.mindflow.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private NoteResponse mapToResponse(Note note) {
        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setTitle(note.getTitle());
        response.setContent(note.getContent());
        response.setSummary(note.getSummary());
        response.setTags(note.getTags());
        response.setNotebookId(note.getNotebookId());
        response.setIsArchived(note.getIsArchived());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
    }

    @Override
    public NoteResponse createNote(NoteCreateRequest request, String username) {
        log.info("Creating note for user '{}': title='{}'", username, request.getTitle());
        User user = getUserByUsername(username);
        Note note = new Note();
        note.setUserId(user.getId());
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setNotebookId(request.getNotebookId());
        note.setTags(request.getTags());
        note.setIsArchived(false);

        note = noteRepository.save(note);
        embeddingService.embedAndStore(note);
        return mapToResponse(note);
    }

    @Override
    public NoteResponse updateNote(Long noteId, NoteUpdateRequest request, String username) {
        User user = getUserByUsername(username);
        Note note = noteRepository.findByIdAndUserId(noteId, user.getId())
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));

        if (request.getTitle() != null) note.setTitle(request.getTitle());
        if (request.getContent() != null) note.setContent(request.getContent());
        if (request.getNotebookId() != null) note.setNotebookId(request.getNotebookId());
        if (request.getIsArchived() != null) note.setIsArchived(request.getIsArchived());
        if (request.getTags() != null) note.setTags(request.getTags());

        note = noteRepository.save(note);
        embeddingService.embedAndStore(note);
        return mapToResponse(note);
    }

    @Override
    public void deleteNote(Long noteId, String username) {
        User user = getUserByUsername(username);
        Note note = noteRepository.findByIdAndUserId(noteId, user.getId())
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));
        noteRepository.delete(note);
    }

    @Override
    public NoteResponse getNote(Long noteId, String username) {
        User user = getUserByUsername(username);
        Note note = noteRepository.findByIdAndUserId(noteId, user.getId())
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));
        return mapToResponse(note);
    }

    @Override
    public List<NoteResponse> getAllNotes(String username, List<String> tags) {
        User user = getUserByUsername(username);
        List<Note> notes;

        if (tags != null && !tags.isEmpty()) {
            String tagsJson;
            try {
                tagsJson = objectMapper.writeValueAsString(tags);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize tags", e);
            }
            notes = noteRepository.findByUserIdAndTagsOverlap(user.getId(), tagsJson);
        } else {
            notes = noteRepository.findByUserId(user.getId());
        }

        return notes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
