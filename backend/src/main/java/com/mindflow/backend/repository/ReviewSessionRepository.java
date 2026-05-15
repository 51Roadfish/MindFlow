package com.mindflow.backend.repository;

import com.mindflow.backend.domain.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewSessionRepository extends JpaRepository<ReviewSession, Long> {
    Optional<ReviewSession> findByUserIdAndNoteIdsHashAndStatus(Long userId, String noteIdsHash, String status);
    List<ReviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
