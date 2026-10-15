package com.medisense.domain.repository;

import com.medisense.domain.entity.DrugLookupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrugLookupRepository extends JpaRepository<DrugLookupEntity, UUID> {
    Optional<DrugLookupEntity> findByBrandNameIgnoreCase(String brandName);
}
