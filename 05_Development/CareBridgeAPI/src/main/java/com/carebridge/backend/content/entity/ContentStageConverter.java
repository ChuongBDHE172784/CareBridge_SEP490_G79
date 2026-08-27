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
        try {
            return ContentStage.valueOf(dbData.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown content stage: " + dbData, e);
        }
    }
}
