package com.medisense.domain.repository;

import com.medisense.domain.entity.IngestionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionJobRepository extends JpaRepository<IngestionJobEntity, UUID> {
    Optional<IngestionJobEntity> findByChecksum(String checksum);
    boolean existsByChecksumAndStatus(String checksum, String status);
}
