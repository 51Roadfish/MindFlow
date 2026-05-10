package com.mindflow.backend.service;

import com.mindflow.backend.dto.request.NoteBookCreateRequest;
import com.mindflow.backend.dto.request.NoteBookUpdateRequest;
import com.mindflow.backend.dto.response.NoteBookResponse;

import java.util.List;

public interface NotebookService {
    NoteBookResponse createNotebook(NoteBookCreateRequest request, String username);

    NoteBookResponse updateNotebook(Long id, NoteBookUpdateRequest request, String name);

    void deleteNotebook(Long id, String name);

    NoteBookResponse getNotebook(Long id, String name);

    List<NoteBookResponse> getAllNotebooks(String name);
}
