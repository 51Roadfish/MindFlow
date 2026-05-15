package com.mindflow.backend.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_session",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "note_ids_hash", "status"}))
public class ReviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "note_ids", columnDefinition = "json")
    private String noteIdsJson;

    @Column(name = "note_ids_hash", length = 64)
    private String noteIdsHash;

    @Column(length = 20)
    private String status;  // IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "total_questions")
    private int totalQuestions;

    @Column(name = "answered_questions")
    private int answeredQuestions;

    @Column(name = "total_score")
    private int totalScore;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
