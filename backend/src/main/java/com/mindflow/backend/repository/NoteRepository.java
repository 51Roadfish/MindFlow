package com.mindflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindflow.backend.domain.Note;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    Optional<Note> findByIdAndUserId(Long id, Long userId);
    List<Note> findByUserId(Long userId);

    @Query(value = "SELECT * FROM note WHERE user_id = :userId " +
            "AND JSON_OVERLAPS(tags, CAST(:tagsJson AS JSON))",
            nativeQuery = true)
    List<Note> findByUserIdAndTagsOverlap(@Param("userId") Long userId,
                                          @Param("tagsJson") String tagsJson);
}
