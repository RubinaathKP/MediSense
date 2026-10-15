package com.medisense.domain.repository;

import com.medisense.domain.entity.ResponseRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResponseRecordRepository extends JpaRepository<ResponseRecordEntity, UUID> {
    Optional<ResponseRecordEntity> findByQueryId(UUID queryId);
}
