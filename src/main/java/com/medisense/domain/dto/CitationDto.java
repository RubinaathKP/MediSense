package com.medisense.domain.dto;

public class CitationDto {
    private String sourceName;
    private Integer publicationYear;
    private String sectionOrTestValue;
    private String excerpt;

    public CitationDto() {}

    public CitationDto(String sourceName, Integer publicationYear, String sectionOrTestValue, String excerpt) {
        this.sourceName = sourceName;
        this.publicationYear = publicationYear;
        this.sectionOrTestValue = sectionOrTestValue;
        this.excerpt = excerpt;
    }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public String getSectionOrTestValue() { return sectionOrTestValue; }
    public void setSectionOrTestValue(String sectionOrTestValue) { this.sectionOrTestValue = sectionOrTestValue; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sourceName;
        private Integer publicationYear;
        private String sectionOrTestValue;
        private String excerpt;

        public Builder sourceName(String sourceName) { this.sourceName = sourceName; return this; }
        public Builder publicationYear(Integer publicationYear) { this.publicationYear = publicationYear; return this; }
        public Builder sectionOrTestValue(String sectionOrTestValue) { this.sectionOrTestValue = sectionOrTestValue; return this; }
        public Builder excerpt(String excerpt) { this.excerpt = excerpt; return this; }

        public CitationDto build() {
            return new CitationDto(sourceName, publicationYear, sectionOrTestValue, excerpt);
        }
    }
}
