package com.mindflow.backend.repository;

import com.mindflow.backend.domain.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    List<ExamPaper> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
