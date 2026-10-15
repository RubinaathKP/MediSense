package com.medisense.domain.repository;

import com.medisense.domain.entity.EvalCaseResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvalCaseResultRepository extends JpaRepository<EvalCaseResultEntity, UUID> {
    List<EvalCaseResultEntity> findByRunId(String runId);
    List<EvalCaseResultEntity> findByRunIdAndPassed(String runId, Boolean passed);
}
