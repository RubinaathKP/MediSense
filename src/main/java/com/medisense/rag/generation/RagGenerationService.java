package com.medisense.rag.generation;

import com.medisense.rag.generation.model.MediSenseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RagGenerationService.class);

    private final GenerationOrchestrator generationOrchestrator;
    private final PromptAssembler promptAssembler;

    public RagGenerationService(GenerationOrchestrator generationOrchestrator, PromptAssembler promptAssembler) {
        this.generationOrchestrator = generationOrchestrator;
        this.promptAssembler = promptAssembler;
    }

    public String generateGroundedResponse(String userQuery, List<String> retrievedPassages) {
        log.info("Generating RAG grounded response for query.");
        MediSenseResponse response = generationOrchestrator.generateResponse(null, userQuery, "en", null);
        return response.explanationText();
    }
}
