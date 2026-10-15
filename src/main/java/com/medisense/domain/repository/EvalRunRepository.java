package com.medisense.domain.repository;

import com.medisense.domain.entity.EvalRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvalRunRepository extends JpaRepository<EvalRunEntity, UUID> {
    Optional<EvalRunEntity> findByRunId(String runId);
    Optional<EvalRunEntity> findTopByOrderByStartedAtDesc();
}
