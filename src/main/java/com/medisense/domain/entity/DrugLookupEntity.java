package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "drug_name_lookup")
public class DrugLookupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "brand_name", nullable = false, unique = true)
    private String brandName;

    @Column(name = "generic_name", nullable = false)
    private String genericName;

    @Column(name = "drug_class", nullable = false)
    private String drugClass;

    @Column(name = "version_tag", length = 50)
    private String versionTag = "v1.0";

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public DrugLookupEntity() {}

    public DrugLookupEntity(UUID id, String brandName, String genericName, String drugClass, String versionTag, OffsetDateTime createdAt) {
        this.id = id;
        this.brandName = brandName;
        this.genericName = genericName;
        this.drugClass = drugClass;
        this.versionTag = versionTag != null ? versionTag : "v1.0";
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDrugClass() { return drugClass; }
    public void setDrugClass(String drugClass) { this.drugClass = drugClass; }

    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String brandName;
        private String genericName;
        private String drugClass;
        private String versionTag = "v1.0";
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder brandName(String brandName) { this.brandName = brandName; return this; }
        public Builder genericName(String genericName) { this.genericName = genericName; return this; }
        public Builder drugClass(String drugClass) { this.drugClass = drugClass; return this; }
        public Builder versionTag(String versionTag) { this.versionTag = versionTag; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DrugLookupEntity build() {
            return new DrugLookupEntity(id, brandName, genericName, drugClass, versionTag, createdAt);
        }
    }
}
