package com.medisense.domain.repository;

import com.medisense.domain.entity.SourceDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SourceDocumentRepository extends JpaRepository<SourceDocumentEntity, UUID> {
    List<SourceDocumentEntity> findBySessionId(UUID sessionId);
    java.util.Optional<SourceDocumentEntity> findByContentHash(String contentHash);
}
