package com.medisense.fixtures;

import com.medisense.audit.AuditEvent;
import com.medisense.audit.Severity;
import com.medisense.domain.entity.*;
import com.medisense.domain.enums.FlagStatus;
import com.medisense.domain.enums.QueryCategory;
import com.medisense.domain.enums.SafetyDecision;

import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.ingestion.model.*;
import com.medisense.rag.chunking.model.*;
import com.medisense.rag.reranking.model.RerankedChunk;
import com.medisense.rag.generation.model.MediSenseResponse;
import com.medisense.rag.generation.model.CitationDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Fluent test data builder for constructing all MediSense domain entities with realistic clinical defaults.
 */
public class TestDataBuilder {

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public static SessionBuilder aSession() {
        return new SessionBuilder();
    }

    public static SourceDocumentBuilder aSourceDocument() {
        return new SourceDocumentBuilder();
    }

    public static ParentChunkBuilder aParentChunk() {
        return new ParentChunkBuilder();
    }

    public static ChildChunkBuilder aChildChunk() {
        return new ChildChunkBuilder();
    }

    public static LabReportValueBuilder aLabReportValue() {
        return new LabReportValueBuilder();
    }

    public static PrescriptionBuilder aPrescription() {
        return new PrescriptionBuilder();
    }

    public static QueryRecordBuilder aQueryRecord() {
        return new QueryRecordBuilder();
    }

    public static ResponseRecordBuilder aResponseRecord() {
        return new ResponseRecordBuilder();
    }

    public static AuditLogBuilder anAuditLog() {
        return new AuditLogBuilder();
    }

    public static DrugLookupBuilder aDrugLookup() {
        return new DrugLookupBuilder();
    }

    public static ParsedDocumentBuilder aParsedDocument() {
        return new ParsedDocumentBuilder();
    }

    public static CleanedDocumentBuilder aCleanedDocument() {
        return new CleanedDocumentBuilder();
    }

    public static StructuredNodeBuilder aStructuredNode() {
        return new StructuredNodeBuilder();
    }

    public static DocumentStructureBuilder aDocumentStructure() {
        return new DocumentStructureBuilder();
    }

    public static IngestionResultBuilder anIngestionResult() {
        return new IngestionResultBuilder();
    }

    public static RagParentChunkBuilder aRagParentChunk() {
        return new RagParentChunkBuilder();
    }

    public static RagChildChunkBuilder aRagChildChunk() {
        return new RagChildChunkBuilder();
    }

    public static ChunkPairBuilder aChunkPair() {
        return new ChunkPairBuilder();
    }

    public static RerankedChunkBuilder aRerankedChunk() {
        return new RerankedChunkBuilder();
    }

    public static MediSenseResponseBuilder aMediSenseResponse() {
        return new MediSenseResponseBuilder();
    }

    // ==========================================
    // Nested Builders
    // ==========================================

    public static class UserBuilder {
        private UUID id = UUID.randomUUID();
        private String email = "patient_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        private String passwordHash = "$2a$10$wK1/vIeFv.7R5g.MockHashedPasswordStringForTests";
        private String fullName = "Dr. Ananya Sharma";
        private String languagePreference = "en";
        private Integer failedAttempts = 0;
        private OffsetDateTime lockedUntil = null;

        public UserBuilder id(UUID id) { this.id = id; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public UserBuilder languagePreference(String lang) { this.languagePreference = lang; return this; }
        public UserBuilder failedAttempts(int count) { this.failedAttempts = count; return this; }

        public UserEntity build() {
            return new UserEntity(id, email, passwordHash, fullName, languagePreference, failedAttempts, lockedUntil, OffsetDateTime.now(), OffsetDateTime.now());
        }
    }

    public static class SessionBuilder {
        private UUID id = UUID.randomUUID();
        private UserEntity user = aUser().build();
        private String sessionToken = "session-tok-" + UUID.randomUUID();
        private String languageCode = "en";
        private OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
        private Boolean active = true;

        public SessionBuilder id(UUID id) { this.id = id; return this; }
        public SessionBuilder user(UserEntity user) { this.user = user; return this; }
        public SessionBuilder sessionToken(String tok) { this.sessionToken = tok; return this; }
        public SessionBuilder languageCode(String code) { this.languageCode = code; return this; }
        public SessionBuilder active(boolean active) { this.active = active; return this; }

        public SessionEntity build() {
            return new SessionEntity(id, user, sessionToken, languageCode, OffsetDateTime.now(), expiresAt, active);
        }
    }

    public static class SourceDocumentBuilder {
        private UUID id = UUID.randomUUID();
        private SessionEntity session = null;
        private String documentName = "ICMR_Lipid_Management_Guidelines_2024.pdf";
        private String documentType = "LAB_GUIDELINE";
        private String sourceType = "CORPUS";
        private String contentHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        private Integer publicationYear = 2024;
        private String versionTag = "v2.1";

        public SourceDocumentBuilder id(UUID id) { this.id = id; return this; }
        public SourceDocumentBuilder session(SessionEntity session) { this.session = session; return this; }
        public SourceDocumentBuilder documentName(String name) { this.documentName = name; return this; }
        public SourceDocumentBuilder sourceType(String type) { this.sourceType = type; return this; }
        public SourceDocumentBuilder publicationYear(int year) { this.publicationYear = year; return this; }

        public SourceDocumentEntity build() {
            return new SourceDocumentEntity(id, session, documentName, documentType, sourceType, contentHash, publicationYear, versionTag, OffsetDateTime.now());
        }
    }

    public static class ParentChunkBuilder {
        private UUID id = UUID.randomUUID();
        private SourceDocumentEntity document = aSourceDocument().build();
        private Integer chunkIndex = 0;
        private String content = "Lipid Guidelines Chapter 2: LDL cholesterol should be monitored every 6 months for adult patients with cardiovascular risk.";
        private String metadata = "{\"section\": \"Chapter 2\", \"domain\": \"lipid_management\"}";

        public ParentChunkBuilder id(UUID id) { this.id = id; return this; }
        public ParentChunkBuilder document(SourceDocumentEntity doc) { this.document = doc; return this; }
        public ParentChunkBuilder chunkIndex(int idx) { this.chunkIndex = idx; return this; }
        public ParentChunkBuilder content(String content) { this.content = content; return this; }

        public ParentChunkEntity build() {
            return new ParentChunkEntity(id, document, chunkIndex, content, metadata, OffsetDateTime.now());
        }
    }

    public static class ChildChunkBuilder {
        private UUID id = UUID.randomUUID();
        private ParentChunkEntity parentChunk = aParentChunk().build();
        private Integer chunkIndex = 0;
        private String content = "LDL cholesterol levels above 130 mg/dL are considered borderline high and warrant lifestyle evaluation.";
        private String embedding = "[0.12, -0.45, 0.88]";
        private String metadata = "{\"section_path\": \"2.1\", \"token_count\": 120}";
        private Boolean isActive = true;

        public ChildChunkBuilder id(UUID id) { this.id = id; return this; }
        public ChildChunkBuilder parentChunk(ParentChunkEntity parent) { this.parentChunk = parent; return this; }
        public ChildChunkBuilder chunkIndex(int idx) { this.chunkIndex = idx; return this; }
        public ChildChunkBuilder content(String content) { this.content = content; return this; }
        public ChildChunkBuilder embedding(String vectorStr) { this.embedding = vectorStr; return this; }

        public ChildChunkEntity build() {
            return new ChildChunkEntity(id, parentChunk, chunkIndex, content, embedding, metadata, isActive, OffsetDateTime.now());
        }
    }

    public static class LabReportValueBuilder {
        private UUID id = UUID.randomUUID();
        private SourceDocumentEntity document = aSourceDocument().build();
        private SessionEntity session = aSession().build();
        private String testName = "LDL Cholesterol";
        private String extractedValue = "142 mg/dL";
        private BigDecimal normalizedValue = new BigDecimal("142.0");
        private String unit = "mg/dL";
        private String referenceRange = "< 100 mg/dL";
        private FlagStatus flagStatus = FlagStatus.BORDERLINE;
        private BigDecimal extractionConfidence = new BigDecimal("0.9650");
        private Boolean requiresUserConfirmation = false;

        public LabReportValueBuilder id(UUID id) { this.id = id; return this; }
        public LabReportValueBuilder testName(String name) { this.testName = name; return this; }
        public LabReportValueBuilder extractedValue(String val) { this.extractedValue = val; return this; }
        public LabReportValueBuilder normalizedValue(BigDecimal val) { this.normalizedValue = val; return this; }
        public LabReportValueBuilder flagStatus(FlagStatus status) { this.flagStatus = status; return this; }
        public LabReportValueBuilder confidence(BigDecimal conf) { this.extractionConfidence = conf; return this; }

        public LabReportValueEntity build() {
            return new LabReportValueEntity(id, document, session, testName, extractedValue, normalizedValue, unit, referenceRange, flagStatus, extractionConfidence, requiresUserConfirmation, OffsetDateTime.now());
        }
    }

    public static class PrescriptionBuilder {
        private UUID id = UUID.randomUUID();
        private SourceDocumentEntity document = aSourceDocument().build();
        private SessionEntity session = aSession().build();
        private String brandName = "Glycomet 500 SR";
        private String genericName = "Metformin Hydrochloride";
        private String drugClass = "Biguanide (Antidiabetic)";
        private String dosage = "500mg";
        private String frequency = "Once daily";
        private String timing = "After dinner";
        private String duration = "30 days";
        private String prescribingSpecialty = "Endocrinology";
        private BigDecimal extractionConfidence = new BigDecimal("0.9450");
        private Boolean requiresUserConfirmation = false;

        public PrescriptionBuilder id(UUID id) { this.id = id; return this; }
        public PrescriptionBuilder brandName(String name) { this.brandName = name; return this; }
        public PrescriptionBuilder genericName(String generic) { this.genericName = generic; return this; }
        public PrescriptionBuilder dosage(String dosage) { this.dosage = dosage; return this; }

        public PrescriptionEntity build() {
            return new PrescriptionEntity(id, document, session, brandName, genericName, drugClass, dosage, frequency, timing, duration, prescribingSpecialty, extractionConfidence, requiresUserConfirmation, OffsetDateTime.now());
        }
    }

    public static class QueryRecordBuilder {
        private UUID id = UUID.randomUUID();
        private SessionEntity session = aSession().build();
        private String rawQuery = "what does borderline LDL mean for my heart?";
        private QueryCategory category = QueryCategory.REPORT_EXPLANATION;
        private String detectedLanguage = "en";

        public QueryRecordBuilder id(UUID id) { this.id = id; return this; }
        public QueryRecordBuilder session(SessionEntity sess) { this.session = sess; return this; }
        public QueryRecordBuilder rawQuery(String q) { this.rawQuery = q; return this; }
        public QueryRecordBuilder category(QueryCategory cat) { this.category = cat; return this; }
        public QueryRecordBuilder detectedLanguage(String lang) { this.detectedLanguage = lang; return this; }

        public QueryRecordEntity build() {
            return new QueryRecordEntity(id, session, rawQuery, category, detectedLanguage, OffsetDateTime.now());
        }
    }

    public static class ResponseRecordBuilder {
        private UUID id = UUID.randomUUID();
        private QueryRecordEntity query = aQueryRecord().build();
        private String groundedAnswer = "Borderline LDL (130-159 mg/dL) indicates elevated risk. Consult your physician for personalized evaluation.";
        private String translatedAnswer = null;
        private SafetyDecision safetyDecision = SafetyDecision.PASSED;
        private String citations = "[{\"source_id\":\"ICMR-LIPID-01\",\"page_number\":12}]";
        private Long latencyMs = 280L;

        public ResponseRecordBuilder id(UUID id) { this.id = id; return this; }
        public ResponseRecordBuilder query(QueryRecordEntity query) { this.query = query; return this; }
        public ResponseRecordBuilder groundedAnswer(String answer) { this.groundedAnswer = answer; return this; }
        public ResponseRecordBuilder safetyDecision(SafetyDecision dec) { this.safetyDecision = dec; return this; }

        public ResponseRecordEntity build() {
            return new ResponseRecordEntity(id, query, groundedAnswer, translatedAnswer, safetyDecision, citations, latencyMs, OffsetDateTime.now());
        }
    }

    public static class AuditLogBuilder {
        private UUID logId = UUID.randomUUID();
        private AuditEvent eventType = AuditEvent.QUERY_INTERCEPTED;
        private UUID sessionId = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();
        private UUID queryId = UUID.randomUUID();
        private String consentGrantId = "CONSENT-GR-991";
        private String ipAddress = "127.0.0.1";
        private String eventDetail = "{\"reason\": \"prescriptive query intercepted\"}";
        private Severity severity = Severity.WARN;

        public AuditLogBuilder eventType(AuditEvent type) { this.eventType = type; return this; }
        public AuditLogBuilder severity(Severity sev) { this.severity = sev; return this; }
        public AuditLogBuilder eventDetail(String detail) { this.eventDetail = detail; return this; }

        public AuditLogEntity build() {
            return new AuditLogEntity(logId, eventType, sessionId, userId, queryId, consentGrantId, ipAddress, eventDetail, severity, OffsetDateTime.now());
        }
    }

    public static class DrugLookupBuilder {
        private UUID id = UUID.randomUUID();
        private String brandName = "Lipitor";
        private String genericName = "Atorvastatin Calcium";
        private String drugClass = "HMG-CoA Reductase Inhibitor (Statin)";
        private String versionTag = "v1.0";

        public DrugLookupBuilder brandName(String brand) { this.brandName = brand; return this; }
        public DrugLookupBuilder genericName(String generic) { this.genericName = generic; return this; }
        public DrugLookupBuilder drugClass(String clazz) { this.drugClass = clazz; return this; }

        public DrugLookupEntity build() {
            return new DrugLookupEntity(id, brandName, genericName, drugClass, versionTag, OffsetDateTime.now());
        }
    }

    public static class ParsedDocumentBuilder {
        private String documentName = "test_document.pdf";
        private String rawText = "MEDISENSE CLINICAL LABORATORY REPORT\nPatient Name: Ramesh Patel\nLDL Cholesterol: 145 mg/dL";
        private DocumentStructure structureTree = new DocumentStructure(new ArrayList<>(), 0);
        private List<String> extractedTablesJson = new ArrayList<>();
        private double extractionConfidence = 0.95;
        private int tokenCount = 150;
        private Map<String, Object> metadata = new HashMap<>();

        public ParsedDocumentBuilder documentName(String name) { this.documentName = name; return this; }
        public ParsedDocumentBuilder rawText(String text) { this.rawText = text; return this; }
        public ParsedDocumentBuilder structureTree(DocumentStructure tree) { this.structureTree = tree; return this; }
        public ParsedDocumentBuilder extractedTablesJson(List<String> tables) { this.extractedTablesJson = tables; return this; }
        public ParsedDocumentBuilder extractionConfidence(double conf) { this.extractionConfidence = conf; return this; }
        public ParsedDocumentBuilder tokenCount(int count) { this.tokenCount = count; return this; }
        public ParsedDocumentBuilder metadata(Map<String, Object> meta) { this.metadata = meta; return this; }

        public ParsedDocument build() {
            ParsedDocument doc = new ParsedDocument(documentName, rawText, structureTree, extractedTablesJson, extractionConfidence, tokenCount);
            doc.setMetadata(metadata);
            return doc;
        }
    }

    public static class CleanedDocumentBuilder {
        private String documentName = "test_document.pdf";
        private String rawCleanedText = "MEDISENSE CLINICAL LABORATORY REPORT. Patient Name: Ramesh Patel. LDL Cholesterol: 145 mg/dL";
        private List<String> sectionHeadings = new ArrayList<>();
        private List<String> cleanedParagraphs = new ArrayList<>();
        private Map<String, String> mappedAbbreviations = new HashMap<>();

        public CleanedDocumentBuilder documentName(String name) { this.documentName = name; return this; }
        public CleanedDocumentBuilder rawCleanedText(String text) { this.rawCleanedText = text; return this; }
        public CleanedDocumentBuilder sectionHeadings(List<String> headings) { this.sectionHeadings = headings; return this; }
        public CleanedDocumentBuilder cleanedParagraphs(List<String> paragraphs) { this.cleanedParagraphs = paragraphs; return this; }
        public CleanedDocumentBuilder mappedAbbreviations(Map<String, String> abbr) { this.mappedAbbreviations = abbr; return this; }

        public CleanedDocument build() {
            CleanedDocument doc = new CleanedDocument(documentName, rawCleanedText, sectionHeadings, cleanedParagraphs);
            doc.setMappedAbbreviations(mappedAbbreviations);
            return doc;
        }
    }

    public static class StructuredNodeBuilder {
        private NodeLevel level = NodeLevel.SECTION;
        private String heading = "Chapter 1 Lipid Management";
        private String textContent = "Chapter 1 Lipid Management. Guidelines recommend target LDL below 100 mg/dL.";
        private String positionPath = "1.1";
        private List<StructuredNode> children = new ArrayList<>();

        public StructuredNodeBuilder level(NodeLevel level) { this.level = level; return this; }
        public StructuredNodeBuilder heading(String heading) { this.heading = heading; return this; }
        public StructuredNodeBuilder textContent(String text) { this.textContent = text; return this; }
        public StructuredNodeBuilder positionPath(String path) { this.positionPath = path; return this; }
        public StructuredNodeBuilder children(List<StructuredNode> children) { this.children = children; return this; }

        public StructuredNode build() {
            return StructuredNode.builder()
                    .level(level)
                    .heading(heading)
                    .textContent(textContent)
                    .positionPath(positionPath)
                    .children(children)
                    .build();
        }
    }

    public static class DocumentStructureBuilder {
        private List<StructuredNode> rootNodes = new ArrayList<>();
        private int totalNodeCount = 0;

        public DocumentStructureBuilder rootNodes(List<StructuredNode> nodes) {
            this.rootNodes = nodes;
            this.totalNodeCount = nodes != null ? nodes.size() : 0;
            return this;
        }
        public DocumentStructureBuilder totalNodeCount(int count) { this.totalNodeCount = count; return this; }

        public DocumentStructure build() {
            return new DocumentStructure(rootNodes, totalNodeCount);
        }
    }

    public static class IngestionResultBuilder {
        private String documentName = "test_document.pdf";
        private List<StructuredNode> safeNodes = new ArrayList<>();
        private List<String> excludedNodesLog = new ArrayList<>();
        private int totalNodesScanned = 10;
        private int excludedNodeCount = 0;

        public IngestionResultBuilder documentName(String name) { this.documentName = name; return this; }
        public IngestionResultBuilder safeNodes(List<StructuredNode> nodes) { this.safeNodes = nodes; return this; }
        public IngestionResultBuilder excludedNodesLog(List<String> log) { this.excludedNodesLog = log; return this; }
        public IngestionResultBuilder totalNodesScanned(int count) { this.totalNodesScanned = count; return this; }
        public IngestionResultBuilder excludedNodeCount(int count) { this.excludedNodeCount = count; return this; }

        public IngestionResult build() {
            return IngestionResult.builder()
                    .documentName(documentName)
                    .safeNodes(safeNodes)
                    .excludedNodesLog(excludedNodesLog)
                    .totalNodesScanned(totalNodesScanned)
                    .excludedNodeCount(excludedNodeCount)
                    .build();
        }
    }

    public static class RagParentChunkBuilder {
        private UUID parentChunkId = UUID.randomUUID();
        private UUID docId = UUID.randomUUID();
        private String sourceName = "ICMR_Lipid_Guidelines.pdf";
        private String sectionHeading = "Chapter 1: Lipid Goals";
        private String parentText = "Full guideline section content regarding cardiovascular evaluation and LDL monitoring.";
        private int tokenCount = 450;
        private int sectionIndex = 1;

        public RagParentChunkBuilder parentChunkId(UUID id) { this.parentChunkId = id; return this; }
        public RagParentChunkBuilder docId(UUID id) { this.docId = id; return this; }
        public RagParentChunkBuilder sourceName(String name) { this.sourceName = name; return this; }
        public RagParentChunkBuilder sectionHeading(String heading) { this.sectionHeading = heading; return this; }
        public RagParentChunkBuilder parentText(String text) { this.parentText = text; return this; }
        public RagParentChunkBuilder tokenCount(int count) { this.tokenCount = count; return this; }
        public RagParentChunkBuilder sectionIndex(int idx) { this.sectionIndex = idx; return this; }

        public ParentChunk build() {
            return ParentChunk.builder()
                    .parentChunkId(parentChunkId)
                    .docId(docId)
                    .sourceName(sourceName)
                    .sectionHeading(sectionHeading)
                    .parentText(parentText)
                    .tokenCount(tokenCount)
                    .sectionIndex(sectionIndex)
                    .build();
        }
    }

    public static class RagChildChunkBuilder {
        private UUID chunkId = UUID.randomUUID();
        private UUID parentChunkId = UUID.randomUUID();
        private UUID docId = UUID.randomUUID();
        private String sourceName = "ICMR_Lipid_Guidelines.pdf";
        private int sourceYear = 2024;
        private EvidenceBasis evidenceBasis = EvidenceBasis.CLINICAL_TRIAL_BACKED;
        private String sourceType = "GUIDELINE";
        private String sectionHeading = "Chapter 1: Lipid Goals";
        private List<String> domainTags = List.of("CARDIOLOGY", "LIPID_MANAGEMENT");
        private String chunkText = "Borderline LDL cholesterol levels (130-159 mg/dL) require dietary intervention.";
        private int tokenCount = 120;
        private int chunkIndex = 0;
        private String language = "en";
        private boolean isTable = false;
        private List<String> tableHeader = Collections.emptyList();

        public RagChildChunkBuilder chunkId(UUID id) { this.chunkId = id; return this; }
        public RagChildChunkBuilder parentChunkId(UUID id) { this.parentChunkId = id; return this; }
        public RagChildChunkBuilder docId(UUID id) { this.docId = id; return this; }
        public RagChildChunkBuilder sourceName(String name) { this.sourceName = name; return this; }
        public RagChildChunkBuilder evidenceBasis(EvidenceBasis basis) { this.evidenceBasis = basis; return this; }
        public RagChildChunkBuilder domainTags(List<String> tags) { this.domainTags = tags; return this; }
        public RagChildChunkBuilder chunkText(String text) { this.chunkText = text; return this; }
        public RagChildChunkBuilder tokenCount(int count) { this.tokenCount = count; return this; }
        public RagChildChunkBuilder chunkIndex(int idx) { this.chunkIndex = idx; return this; }
        public RagChildChunkBuilder isTable(boolean table) { this.isTable = table; return this; }

        public ChildChunk build() {
            return ChildChunk.builder()
                    .chunkId(chunkId)
                    .parentChunkId(parentChunkId)
                    .docId(docId)
                    .sourceName(sourceName)
                    .sourceYear(sourceYear)
                    .evidenceBasis(evidenceBasis)
                    .sourceType(sourceType)
                    .sectionHeading(sectionHeading)
                    .domainTags(domainTags)
                    .chunkText(chunkText)
                    .tokenCount(tokenCount)
                    .chunkIndex(chunkIndex)
                    .language(language)
                    .isTable(isTable)
                    .tableHeader(tableHeader)
                    .build();
        }
    }

    public static class ChunkPairBuilder {
        private ParentChunk parentChunk = aRagParentChunk().build();
        private List<ChildChunk> childChunks = List.of(aRagChildChunk().build());

        public ChunkPairBuilder parentChunk(ParentChunk parent) { this.parentChunk = parent; return this; }
        public ChunkPairBuilder childChunks(List<ChildChunk> children) { this.childChunks = children; return this; }

        public ChunkPair build() {
            return new ChunkPair(parentChunk, childChunks);
        }
    }

    public static class RerankedChunkBuilder {
        private UUID chunkId = UUID.randomUUID();
        private String sectionHeading = "Chapter 1: Lipid Management";
        private String content = "Borderline LDL cholesterol levels (130-159 mg/dL) require dietary intervention.";
        private double score = 0.92;
        private int rank = 1;
        private String source = "ICMR_Lipid_Guidelines.pdf";
        private boolean isReranked = true;

        public RerankedChunkBuilder chunkId(UUID id) { this.chunkId = id; return this; }
        public RerankedChunkBuilder sectionHeading(String heading) { this.sectionHeading = heading; return this; }
        public RerankedChunkBuilder content(String content) { this.content = content; return this; }
        public RerankedChunkBuilder score(double score) { this.score = score; return this; }
        public RerankedChunkBuilder rank(int rank) { this.rank = rank; return this; }
        public RerankedChunkBuilder source(String source) { this.source = source; return this; }
        public RerankedChunkBuilder isReranked(boolean isReranked) { this.isReranked = isReranked; return this; }

        public RerankedChunk build() {
            return new RerankedChunk(chunkId, sectionHeading, content, score, rank, source, isReranked);
        }
    }

    public static class MediSenseResponseBuilder {
        private String explanationText = "Elevated LDL cholesterol levels increase cardiovascular risk.";
        private List<CitationDto> citations = List.of(new CitationDto("chunk-101", "AHA Lipid Guidelines", 2024, "clinical_trial"));
        private boolean isReferral = false;
        private String languageOut = "en";
        private boolean insufficientContext = false;

        public MediSenseResponseBuilder explanationText(String text) { this.explanationText = text; return this; }
        public MediSenseResponseBuilder citations(List<CitationDto> citations) { this.citations = citations; return this; }
        public MediSenseResponseBuilder isReferral(boolean isReferral) { this.isReferral = isReferral; return this; }
        public MediSenseResponseBuilder languageOut(String lang) { this.languageOut = lang; return this; }
        public MediSenseResponseBuilder insufficientContext(boolean insufficient) { this.insufficientContext = insufficient; return this; }

        public MediSenseResponse build() {
            return new MediSenseResponse(explanationText, citations, isReferral, languageOut, insufficientContext);
        }
    }
}
