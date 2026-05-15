package com.mindflow.backend.flow;

import com.mindflow.backend.domain.Note;
import com.mindflow.backend.repository.NoteRepository;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("loadNotesComponent")
@RequiredArgsConstructor
public class LoadNotesComponent extends NodeComponent {

    private final NoteRepository noteRepository;

    @Override
    public void process() {
        Object data = getRequestData();
        List<Long> noteIds;
        Long userId;

        if (data instanceof ReviewContext reviewCtx) {
            noteIds = reviewCtx.getNoteIds();
            userId = reviewCtx.getUserId();
        } else if (data instanceof ExamContext examCtx) {
            noteIds = examCtx.getNoteIds();
            userId = examCtx.getUserId();
        } else {
            return;
        }

        List<Note> notes = noteRepository.findAllById(noteIds);
        String content = notes.stream()
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> "【" + n.getTitle() + "】\n" + (n.getContent() != null ? n.getContent() : ""))
                .collect(Collectors.joining("\n\n"));

        if (data instanceof ReviewContext reviewCtx) {
            reviewCtx.setNotesContent(content);
        } else if (data instanceof ExamContext examCtx) {
            examCtx.setNotesContent(content);
        }
    }
}
