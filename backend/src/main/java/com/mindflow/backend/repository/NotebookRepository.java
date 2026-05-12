package com.mindflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindflow.backend.domain.Notebook;

import java.util.List;
import java.util.Optional;

public interface NotebookRepository extends JpaRepository<Notebook, Long> {
    Optional<Notebook> findByIdAndUserId(Long id, Long userId);
    List<Notebook> findByUserId(Long userId);
}
