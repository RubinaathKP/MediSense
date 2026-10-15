package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "source_documents")
public class SourceDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionEntity session;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "version_tag", length = 50)
    private String versionTag;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public SourceDocumentEntity() {}

    public SourceDocumentEntity(UUID id, SessionEntity session, String documentName, String documentType, String sourceType, String contentHash, Integer publicationYear, String versionTag, OffsetDateTime createdAt) {
        this.id = id;
        this.session = session;
        this.documentName = documentName;
        this.documentType = documentType;
        this.sourceType = sourceType;
        this.contentHash = contentHash;
        this.publicationYear = publicationYear;
        this.versionTag = versionTag;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private SessionEntity session;
        private String documentName;
        private String documentType;
        private String sourceType;
        private String contentHash;
        private Integer publicationYear;
        private String versionTag;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder session(SessionEntity session) { this.session = session; return this; }
        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public Builder contentHash(String contentHash) { this.contentHash = contentHash; return this; }
        public Builder publicationYear(Integer publicationYear) { this.publicationYear = publicationYear; return this; }
        public Builder versionTag(String versionTag) { this.versionTag = versionTag; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public SourceDocumentEntity build() {
            return new SourceDocumentEntity(id, session, documentName, documentType, sourceType, contentHash, publicationYear, versionTag, createdAt);
        }
    }
}
