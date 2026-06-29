package com.carebridge.backend.exercise.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

/**
 * Converts a {@code Map<String, Boolean>} to/from its JSON string representation
 * for storage in a {@code jsonb} column.
 *
 * <p>Handles {@code null} gracefully in both directions.
 */
@Converter
public class JsonbMapConverter implements AttributeConverter<Map<String, Boolean>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Boolean>> MAP_TYPE =
            new TypeReference<>() {
            };

    @Override
    public String convertToDatabaseColumn(Map<String, Boolean> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize answer map to JSON", ex);
        }
    }

    @Override
    public Map<String, Boolean> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize answer map from JSON", ex);
        }
    }
}
