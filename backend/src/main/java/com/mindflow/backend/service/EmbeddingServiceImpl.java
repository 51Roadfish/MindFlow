package com.mindflow.backend.service;

import com.mindflow.backend.domain.Note;
import com.mindflow.backend.utils.TextSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final VectorStoreService vectorStoreService;

    @Async
    @Override
    public void embedAndStore(Note note) {
        //vectorStoreService.deleteByNoteId(note.getId());
        List<String> chunks = TextSplitter.split(note.getContent(), 200);
        List<Document> docs = IntStream.range(0, chunks.size())
            .mapToObj(i -> new Document(chunks.get(i), Map.of(
                "userId", String.valueOf(note.getUserId()),
                "noteId", String.valueOf(note.getId()),
                "noteTitle", note.getTitle(),
                "chunkIndex", i
            )))
            .collect(Collectors.toList());
        vectorStoreService.save(docs);
    }
}
