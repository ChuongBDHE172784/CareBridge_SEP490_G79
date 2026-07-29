package com.carebridge.backend.content.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentStageConverter implements AttributeConverter<ContentStage, String> {

    @Override
    public String convertToDatabaseColumn(ContentStage attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public ContentStage convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        if ("BABY_CARE".equalsIgnoreCase(dbData)) {
            return ContentStage.POSTPARTUM;
        }
        try {
            return ContentStage.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContentStage.POSTPARTUM;
        }
    }
}
