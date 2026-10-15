package com.medisense.domain.repository;

import com.medisense.domain.entity.ManualReviewQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ManualReviewQueueRepository extends JpaRepository<ManualReviewQueueEntity, UUID> {
}
