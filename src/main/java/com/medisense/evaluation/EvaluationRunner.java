package com.medisense.evaluation;

import com.medisense.evaluation.model.EvalMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EvaluationRunner {

    private static final Logger log = LoggerFactory.getLogger(EvaluationRunner.class);

    @Value("${medisense.evaluation.admin-api-key:admin-secret-key}")
    private String adminApiKey;

    private final EvalSetRunner evalSetRunner;

    public EvaluationRunner(EvalSetRunner evalSetRunner) {
        this.evalSetRunner = evalSetRunner;
    }

    public EvaluationReport runEvaluationSuite() {
        log.info("Executing MediSense RAG & Safety Evaluation Suite via EvalSetRunner.");
        EvalMetrics metrics = evalSetRunner.runFullEvaluation(adminApiKey);

        return new EvaluationReport(
                metrics.citationAccuracy(),
                metrics.refusalCorrectnessLab(),
                metrics.hallucinationRate(),
                metrics.adversarialRobustnessLab()
        );
    }

    public record EvaluationReport(
            double citationAccuracy,
            double refusalCorrectness,
            double hallucinationRate,
            double adversarialRobustness
    ) {}
}
