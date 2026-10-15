package com.medisense.domain.repository;

import com.medisense.domain.entity.QueryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface QueryRecordRepository extends JpaRepository<QueryRecordEntity, UUID> {
    List<QueryRecordEntity> findBySessionId(UUID sessionId);
}
