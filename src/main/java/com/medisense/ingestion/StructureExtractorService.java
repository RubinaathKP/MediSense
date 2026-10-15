package com.medisense.ingestion;

import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.ParsedDocument;
import com.medisense.ingestion.model.StructuredNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class StructureExtractorService {

    private static final Logger log = LoggerFactory.getLogger(StructureExtractorService.class);

    private static final Pattern CHAPTER_PATTERN = Pattern.compile("(?i)^Chapter\\s+(\\d+)(.*)$");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\s+(.*)$");
    private static final Pattern SUBSECTION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\s+(.*)$");

    public DocumentStructure extractStructure(CleanedDocument cleanedDocument) {
        log.info("Stage 4 - StructureExtractor: Extracting document hierarchy tree for '{}'.", cleanedDocument.getDocumentName());

        List<StructuredNode> rootNodes = new ArrayList<>();
        int nodeCounter = 0;

        StructuredNode currentChapter = null;
        StructuredNode currentSection = null;
        StructuredNode currentSubsection = null;

        int chapterIdx = 0;
        int sectionIdx = 0;
        int subsectionIdx = 0;
        int paragraphIdx = 0;

        for (String paragraph : cleanedDocument.getCleanedParagraphs()) {
            var chapterMatcher = CHAPTER_PATTERN.matcher(paragraph);
            var subsectionMatcher = SUBSECTION_PATTERN.matcher(paragraph);
            var sectionMatcher = SECTION_PATTERN.matcher(paragraph);

            if (chapterMatcher.find()) {
                chapterIdx++;
                sectionIdx = 0;
                subsectionIdx = 0;
                paragraphIdx = 0;

                String posPath = String.valueOf(chapterIdx);
                currentChapter = StructuredNode.builder()
                        .level(NodeLevel.CHAPTER)
                        .heading(paragraph.trim())
                        .textContent(paragraph.trim())
                        .positionPath(posPath)
                        .children(new ArrayList<>())
                        .build();

                rootNodes.add(currentChapter);
                currentSection = null;
                currentSubsection = null;
                nodeCounter++;

            } else if (subsectionMatcher.find()) {
                subsectionIdx++;
                paragraphIdx = 0;
                String posPath = (currentSection != null ? currentSection.getPositionPath() : (chapterIdx + ".1")) + "." + subsectionIdx;

                currentSubsection = StructuredNode.builder()
                        .level(NodeLevel.SUBSECTION)
                        .heading(paragraph.trim())
                        .textContent(paragraph.trim())
                        .positionPath(posPath)
                        .children(new ArrayList<>())
                        .build();

                if (currentSection != null) {
                    currentSection.getChildren().add(currentSubsection);
                } else if (currentChapter != null) {
                    currentChapter.getChildren().add(currentSubsection);
                } else {
                    rootNodes.add(currentSubsection);
                }
                nodeCounter++;

            } else if (sectionMatcher.find()) {
                sectionIdx++;
                subsectionIdx = 0;
                paragraphIdx = 0;

                String posPath = (currentChapter != null ? currentChapter.getPositionPath() : "1") + "." + sectionIdx;

                currentSection = StructuredNode.builder()
                        .level(NodeLevel.SECTION)
                        .heading(paragraph.trim())
                        .textContent(paragraph.trim())
                        .positionPath(posPath)
                        .children(new ArrayList<>())
                        .build();

                if (currentChapter != null) {
                    currentChapter.getChildren().add(currentSection);
                } else {
                    rootNodes.add(currentSection);
                }
                currentSubsection = null;
                nodeCounter++;

            } else {
                paragraphIdx++;
                String parentPath = currentSubsection != null ? currentSubsection.getPositionPath() :
                        (currentSection != null ? currentSection.getPositionPath() :
                                (currentChapter != null ? currentChapter.getPositionPath() : "1"));
                String posPath = parentPath + ".p" + paragraphIdx;

                StructuredNode pNode = StructuredNode.builder()
                        .level(NodeLevel.PARAGRAPH)
                        .heading(null)
                        .textContent(paragraph.trim())
                        .positionPath(posPath)
                        .children(new ArrayList<>())
                        .build();

                if (currentSubsection != null) {
                    currentSubsection.getChildren().add(pNode);
                } else if (currentSection != null) {
                    currentSection.getChildren().add(pNode);
                } else if (currentChapter != null) {
                    currentChapter.getChildren().add(pNode);
                } else {
                    // When headings are missing, create a default section node and attach paragraph text
                    if (currentSection == null) {
                        currentSection = StructuredNode.builder()
                                .level(NodeLevel.SECTION)
                                .heading("General")
                                .textContent("Default Section")
                                .positionPath("1.1")
                                .children(new ArrayList<>())
                                .build();
                        rootNodes.add(currentSection);
                        nodeCounter++;
                    }
                    currentSection.getChildren().add(pNode);
                }
                nodeCounter++;
            }
        }

        log.info("Stage 4 - StructureExtractor completed for '{}'. Generated {} root hierarchical nodes, {} total nodes.",
                cleanedDocument.getDocumentName(), rootNodes.size(), nodeCounter);

        return DocumentStructure.builder()
                .rootNodes(rootNodes)
                .totalNodeCount(nodeCounter)
                .build();
    }

    public DocumentStructure extractStructure(ParsedDocument parsedDocument) {
        if (parsedDocument == null) {
            return new DocumentStructure(new ArrayList<>(), 0);
        }
        String text = parsedDocument.getRawText() != null ? parsedDocument.getRawText() : "";
        List<String> paragraphs = new ArrayList<>();
        for (String p : text.split("\\n\\n+")) {
            String trimmed = p.trim();
            if (!trimmed.isBlank()) {
                paragraphs.add(trimmed);
            }
        }
        CleanedDocument cleaned = CleanedDocument.builder()
                .documentName(parsedDocument.getDocumentName())
                .rawCleanedText(text)
                .cleanedParagraphs(paragraphs)
                .build();
        return extractStructure(cleaned);
    }
}
