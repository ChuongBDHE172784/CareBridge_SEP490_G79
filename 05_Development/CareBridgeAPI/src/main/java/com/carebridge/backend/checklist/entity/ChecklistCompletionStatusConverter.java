package com.carebridge.backend.checklist.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChecklistCompletionStatusConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean completed) {
        return Boolean.TRUE.equals(completed) ? "COMPLETED" : "OPEN";
    }

    @Override
    public Boolean convertToEntityAttribute(String status) {
        return "COMPLETED".equals(status);
    }
}
