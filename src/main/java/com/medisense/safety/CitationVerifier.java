package com.medisense.safety;

import com.medisense.safety.exception.HallucinatedCitationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CitationVerifier {

    private static final Logger log = LoggerFactory.getLogger(CitationVerifier.class);

    private static final Pattern CITATION_TAG_PATTERN =
            Pattern.compile("\\[Source:\\s*([^\\]]+)\\]|\\[([a-f0-9\\-]{36}|chunk-[^\\]]+|ICMR[^\\]]*)\\]", Pattern.CASE_INSENSITIVE);

    public record CitationVerificationResult(
            boolean isGrounded,
            Set<String> extractedCitations,
            Set<String> hallucinatedCitations,
            String refusalResponse
    ) {}

    public CitationVerificationResult verifyCitations(String generatedResponse, List<String> retrievedChunkIds) {
        log.info("Stage 3 - CitationVerifier: Verifying response citations against {} retrieved chunk IDs.",
                retrievedChunkIds != null ? retrievedChunkIds.size() : 0);

        if (generatedResponse == null || generatedResponse.isBlank()) {
            return new CitationVerificationResult(true, Collections.emptySet(), Collections.emptySet(), null);
        }

        Set<String> retrievedSet = new HashSet<>();
        if (retrievedChunkIds != null) {
            for (String id : retrievedChunkIds) {
                if (id != null) retrievedSet.add(id.toLowerCase().trim());
            }
        }

        Set<String> extractedCitations = new HashSet<>();
        Set<String> hallucinatedCitations = new HashSet<>();

        Matcher matcher = CITATION_TAG_PATTERN.matcher(generatedResponse);
        while (matcher.find()) {
            String citation = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (citation != null) {
                String cleanCitation = citation.trim();
                extractedCitations.add(cleanCitation);

                boolean matched = false;
                String lowerCitation = cleanCitation.toLowerCase();

                for (String retrievedId : retrievedSet) {
                    if (retrievedId.equals(lowerCitation) || retrievedId.contains(lowerCitation) || lowerCitation.contains(retrievedId)) {
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    hallucinatedCitations.add(cleanCitation);
                }
            }
        }

        if (!hallucinatedCitations.isEmpty()) {
            log.error("Stage 3 Citation Verification FAILED! Hallucinated citations detected: {}", hallucinatedCitations);
            return new CitationVerificationResult(
                    false,
                    extractedCitations,
                    hallucinatedCitations,
                    SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT
            );
        }

        log.info("Stage 3 - CitationVerifier PASSED. Verified {} citations cleanly.", extractedCitations.size());
        return new CitationVerificationResult(true, extractedCitations, Collections.emptySet(), null);
    }

    public void verifyOrThrow(String generatedResponse, List<String> retrievedChunkIds) {
        CitationVerificationResult result = verifyCitations(generatedResponse, retrievedChunkIds);
        if (!result.isGrounded()) {
            throw new HallucinatedCitationException(
                    "Hallucinated citations detected in response text: " + result.hallucinatedCitations()
            );
        }
    }

    public boolean verifyCitationsGrounded(String generatedResponse, List<String> retrievedPassages) {
        return verifyCitations(generatedResponse, retrievedPassages).isGrounded();
    }
}
