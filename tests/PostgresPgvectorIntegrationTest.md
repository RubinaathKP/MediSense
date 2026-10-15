# Test Specification: PostgresPgvectorIntegrationTest

## 1. Test Suite Overview
- **Component Under Test:** PostgreSQL Relational Database & pgvector Extension
- **Category:** Integration Test (Database Infrastructure & Vector Store)
- **Framework:** Spring Boot Test, Testcontainers (`pgvector/pgvector:pg16`), AssertJ
- **File Location:** `src/test/java/com/medisense/integration/PostgresPgvectorIntegrationTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.integration;

import com.medisense.IntegrationTestBase;
import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.domain.repository.UserRepository;
import com.medisense.fixtures.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresPgvectorIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SourceDocumentRepository sourceDocumentRepository;

    @Autowired
    private ParentChunkRepository parentChunkRepository;

    @Autowired
    private ChildChunkRepository childChunkRepository;

    @Test
    @Transactional
    @DisplayName("Should persist and retrieve UserEntity with unique constraints in live Postgres")
    void shouldPersistAndRetrieveUser() {
        UserEntity user = TestDataBuilder.aUser()
                .email("pg.test@medisense.health")
                .fullName("Pooja Nair")
                .build();

        UserEntity saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        Optional<UserEntity> retrieved = userRepository.findByEmail("pg.test@medisense.health");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getFullName()).isEqualTo("Pooja Nair");
    }

    @Test
    @Transactional
    @DisplayName("Should persist SourceDocument with Parent-Child chunk hierarchy and vector embedding string")
    void shouldPersistDocumentChunkHierarchyWithEmbeddings() {
        SourceDocumentEntity doc = sourceDocumentRepository.save(
                TestDataBuilder.aSourceDocument()
                        .documentName("ICMR_Hypertension_2024.pdf")
                        .build()
        );

        ParentChunkEntity parent = parentChunkRepository.save(
                TestDataBuilder.aParentChunk()
                        .document(doc)
                        .content("Hypertension diagnostic guidelines: SBP >= 140 mmHg.")
                        .build()
        );

        ChildChunkEntity child = childChunkRepository.save(
                TestDataBuilder.aChildChunk()
                        .parentChunk(parent)
                        .content("Systolic blood pressure above 140 mmHg indicates stage 1 hypertension.")
                        .embedding("[0.25, -0.11, 0.94]")
                        .build()
        );

        assertThat(child.getId()).isNotNull();
        Optional<ChildChunkEntity> retrievedChild = childChunkRepository.findById(child.getId());
        assertThat(retrievedChild).isPresent();
        assertThat(retrievedChild.get().getParentChunk().getId()).isEqualTo(parent.getId());
        assertThat(retrievedChild.get().getEmbedding()).contains("0.25");
    }
}
```
