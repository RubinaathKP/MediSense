package com.medisense.domain.repository;

import com.medisense.domain.entity.ParentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ParentChunkRepository extends JpaRepository<ParentChunkEntity, UUID> {
    List<ParentChunkEntity> findByDocumentId(UUID documentId);
}
