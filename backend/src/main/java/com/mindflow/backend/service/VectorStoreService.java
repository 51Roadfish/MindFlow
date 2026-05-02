package com.mindflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;

    public List<Document> similaritySearch(Long userId, String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(topK)
                        .withFilterExpression("userId == '" + userId + "'")
        );
    }

    public void save(List<Document> documents) {
        vectorStore.add(documents);
    }

    // 直接根据过滤条件删除，不会发空字符串给 Embedding API
    public void deleteByNoteId(Long noteId) {
        vectorStore.delete(Collections.singletonList("noteId == '" + noteId + "'"));
    }
}