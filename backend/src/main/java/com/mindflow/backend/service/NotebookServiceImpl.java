package com.mindflow.backend.service;

import com.mindflow.backend.domain.Notebook;
import com.mindflow.backend.domain.User;
import com.mindflow.backend.dto.request.NoteBookCreateRequest;
import com.mindflow.backend.dto.request.NoteBookUpdateRequest;
import com.mindflow.backend.dto.response.NoteBookResponse;
import com.mindflow.backend.repository.NotebookRepository;
import com.mindflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotebookServiceImpl implements NotebookService {
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;

    private NoteBookResponse mapToResponse(Notebook notebook) {
            NoteBookResponse response = new NoteBookResponse();
            response.setId(notebook.getId());
            response.setName(notebook.getName());
            response.setDescription(notebook.getDescription());
            response.setCreatedAt(notebook.getCreatedAt());
        return response;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public NoteBookResponse createNotebook(NoteBookCreateRequest request, String username) {
        User user = getUserByUsername(username);
        Notebook notebook = new Notebook();
        notebook.setName(request.getName());
        notebook.setDescription(request.getDescription());
        notebook.setUserId(user.getId());
        notebook = notebookRepository.save(notebook);
        return mapToResponse(notebook);
    }

    @Override
    public NoteBookResponse updateNotebook(Long notebookId, NoteBookUpdateRequest request, String username) {
        User user = getUserByUsername(username);
        Notebook notebook = notebookRepository.findByIdAndUserId(notebookId,user.getId())
                .orElseThrow(() -> new RuntimeException("Notebook not found"));
        if(request.getName() != null) {
            notebook.setName(request.getName());
        }
        if(request.getDescription() != null) {
            notebook.setDescription(request.getDescription());
        }
        notebook = notebookRepository.save(notebook);
        return mapToResponse(notebook);
    }

    @Override
    public void deleteNotebook(Long notebookId, String username) {
        User user = getUserByUsername(username);
        Notebook notebook = notebookRepository.findByIdAndUserId(notebookId,user.getId())
                .orElseThrow(() -> new RuntimeException("Notebook not found"));
        notebookRepository.delete(notebook);
    }

    @Override
    public NoteBookResponse getNotebook(Long notebookId, String username) {
        User user=getUserByUsername(username);
        Notebook notebook = notebookRepository.findByIdAndUserId(notebookId,user.getId())
                .orElseThrow(() -> new RuntimeException("Notebook not found"));
        return mapToResponse(notebook);
    }

    @Override
    public List<NoteBookResponse> getAllNotebooks(String username) {
        User user = getUserByUsername(username);
        return notebookRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
