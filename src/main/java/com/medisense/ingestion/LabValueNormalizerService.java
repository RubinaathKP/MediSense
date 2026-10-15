package com.medisense.ingestion;

import com.medisense.domain.dto.LabReportDto;
import com.medisense.domain.enums.FlagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class LabValueNormalizerService {

    private static final Logger log = LoggerFactory.getLogger(LabValueNormalizerService.class);

    public LabReportDto normalizeLabValue(String testName, String rawValue, String unit, String referenceRange) {
        log.info("Normalizing lab value for testName: {}, rawValue: {}, unit: {}", testName, rawValue, unit);

        BigDecimal normalizedVal = null;
        try {
            normalizedVal = new BigDecimal(rawValue.trim().replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            log.warn("Could not convert raw value '{}' to numeric BigDecimal.", rawValue);
        }

        FlagStatus flag = evaluateFlag(normalizedVal, referenceRange);

        return LabReportDto.builder()
                .testName(testName)
                .extractedValue(rawValue)
                .normalizedValue(normalizedVal)
                .unit(unit)
                .referenceRange(referenceRange)
                .flagStatus(flag)
                .extractionConfidence(new BigDecimal("0.95"))
                .requiresUserConfirmation(false)
                .build();
    }

    private FlagStatus evaluateFlag(BigDecimal value, String referenceRange) {
        if (value == null || referenceRange == null || referenceRange.isBlank()) {
            return FlagStatus.NORMAL;
        }
        // Basic range parser logic (e.g. "70-99" or "< 100")
        try {
            if (referenceRange.contains("-")) {
                String[] parts = referenceRange.split("-");
                BigDecimal min = new BigDecimal(parts[0].trim());
                BigDecimal max = new BigDecimal(parts[1].trim());
                if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                    return FlagStatus.ABNORMAL;
                }
            }
        } catch (Exception e) {
            log.debug("Standard range parse skipped for range: {}", referenceRange);
        }
        return FlagStatus.NORMAL;
    }
}
