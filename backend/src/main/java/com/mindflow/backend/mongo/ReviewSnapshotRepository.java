package com.mindflow.backend.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReviewSnapshotRepository extends MongoRepository<ReviewSnapshot, String> {
    Optional<ReviewSnapshot> findBySessionId(Long sessionId);
}
