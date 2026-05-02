package com.mindflow.backend.service;

import com.mindflow.backend.domain.Note;

public interface EmbeddingService {
    void embedAndStore(Note note);
}
