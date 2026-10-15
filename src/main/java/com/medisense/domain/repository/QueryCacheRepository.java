package com.medisense.domain.repository;

import com.medisense.domain.entity.QueryCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryCacheRepository extends JpaRepository<QueryCacheEntity, UUID> {
    Optional<QueryCacheEntity> findByQueryHash(String queryHash);
}
