package com.mindflow.backend.service;

import com.mindflow.backend.dto.request.NoteCreateRequest;
import com.mindflow.backend.dto.request.NoteUpdateRequest;
import com.mindflow.backend.dto.response.NoteResponse;
import java.util.List;

public interface NoteService {
    NoteResponse createNote(NoteCreateRequest request, String username);
    NoteResponse updateNote(Long noteId, NoteUpdateRequest request, String username);
    void deleteNote(Long noteId, String username);
    NoteResponse getNote(Long noteId, String username);
    List<NoteResponse> getAllNotes(String username);
}
