package com.medisense.domain.mapper;

import com.medisense.domain.dto.LabReportDto;
import com.medisense.domain.dto.PrescriptionDto;
import com.medisense.domain.entity.LabReportValueEntity;
import com.medisense.domain.entity.PrescriptionEntity;
import org.springframework.stereotype.Component;

@Component
public class MediSenseMapper {

    public LabReportDto toLabReportDto(LabReportValueEntity entity) {
        if (entity == null) return null;
        return LabReportDto.builder()
                .testName(entity.getTestName())
                .extractedValue(entity.getExtractedValue())
                .normalizedValue(entity.getNormalizedValue())
                .unit(entity.getUnit())
                .referenceRange(entity.getReferenceRange())
                .flagStatus(entity.getFlagStatus())
                .extractionConfidence(entity.getExtractionConfidence())
                .requiresUserConfirmation(entity.getRequiresUserConfirmation())
                .build();
    }

    public LabReportValueEntity toLabReportValueEntity(LabReportDto dto) {
        if (dto == null) return null;
        return LabReportValueEntity.builder()
                .testName(dto.getTestName())
                .extractedValue(dto.getExtractedValue())
                .normalizedValue(dto.getNormalizedValue())
                .unit(dto.getUnit())
                .referenceRange(dto.getReferenceRange())
                .flagStatus(dto.getFlagStatus())
                .extractionConfidence(dto.getExtractionConfidence())
                .requiresUserConfirmation(dto.getRequiresUserConfirmation())
                .build();
    }

    public PrescriptionDto toPrescriptionDto(PrescriptionEntity entity) {
        if (entity == null) return null;
        return PrescriptionDto.builder()
                .brandName(entity.getBrandName())
                .genericName(entity.getGenericName())
                .drugClass(entity.getDrugClass())
                .dosage(entity.getDosage())
                .frequency(entity.getFrequency())
                .timing(entity.getTiming())
                .duration(entity.getDuration())
                .prescribingSpecialty(entity.getPrescribingSpecialty())
                .extractionConfidence(entity.getExtractionConfidence())
                .requiresUserConfirmation(entity.getRequiresUserConfirmation())
                .build();
    }

    public PrescriptionEntity toPrescriptionEntity(PrescriptionDto dto) {
        if (dto == null) return null;
        return PrescriptionEntity.builder()
                .brandName(dto.getBrandName())
                .genericName(dto.getGenericName())
                .drugClass(dto.getDrugClass())
                .dosage(dto.getDosage())
                .frequency(dto.getFrequency())
                .timing(dto.getTiming())
                .duration(dto.getDuration())
                .prescribingSpecialty(dto.getPrescribingSpecialty())
                .extractionConfidence(dto.getExtractionConfidence())
                .requiresUserConfirmation(dto.getRequiresUserConfirmation())
                .build();
    }
}
