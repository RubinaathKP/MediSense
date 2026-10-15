package com.medisense.domain.repository;

import com.medisense.domain.entity.ChildChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChildChunkRepository extends JpaRepository<ChildChunkEntity, UUID> {

    List<ChildChunkEntity> findByParentChunkId(UUID parentChunkId);

    @Query(value = "SELECT * FROM child_chunks ORDER BY embedding <=> CAST(:vector AS vector) LIMIT :k", nativeQuery = true)
    List<ChildChunkEntity> findNearestNeighbors(@Param("vector") String vectorString, @Param("k") int k);
}
