package com.mindflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindflow.backend.domain.Note;
import java.util.Optional;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    Optional<Note> findByIdAndUserId(Long id, Long userId);
    List<Note> findByUserId(Long userId);
}
