package com.medisense.domain.repository;

import com.medisense.domain.entity.ConsentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequestEntity, UUID> {
    Optional<ConsentRequestEntity> findByConsentRequestId(String consentRequestId);
    Optional<ConsentRequestEntity> findByConsentArtefactId(String consentArtefactId);
    Optional<ConsentRequestEntity> findByTransactionId(String transactionId);
}
