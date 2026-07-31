package com.carebridge.backend.family.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyPermission {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private boolean calendar;
    private boolean logs;
    private boolean alerts;
    private boolean records;
    private boolean quickNotes;
    private boolean quickNoteWeight;
    private boolean quickNoteHydration;
    private boolean quickNoteEpds;
    private boolean quickNoteFetalMovement;

    public static FamilyPermission defaults() {
        return new FamilyPermission(false, false, false, false,
                false, false, false, false, false);
    }

    public static FamilyPermission fromJson(String json) {
        if (json == null || json.isBlank()) {
            return defaults();
        }
        try {
            return MAPPER.readValue(json, FamilyPermission.class);
        } catch (JsonProcessingException e) {
            return defaults();
        }
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"calendar\":false,\"logs\":false,\"alerts\":false,\"records\":false,"
                    + "\"quickNotes\":false,\"quickNoteWeight\":false,\"quickNoteHydration\":false,"
                    + "\"quickNoteEpds\":false,\"quickNoteFetalMovement\":false}";
        }
    }
}
