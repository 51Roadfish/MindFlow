package com.mindflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindflow.backend.domain.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
}
