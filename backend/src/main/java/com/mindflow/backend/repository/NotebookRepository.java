package com.mindflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindflow.backend.domain.Notebook;

public interface NotebookRepository extends JpaRepository<Notebook, Long> {}
