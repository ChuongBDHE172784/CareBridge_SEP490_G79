package com.carebridge.backend.family.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FamilyPermission {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private boolean calendar;
    private boolean logs;
    private boolean alerts;
    private boolean records;
    @JsonProperty("CHECKLIST_VIEW")
    private boolean checklistView;
    @JsonProperty("CHECKLIST_COMPLETE")
    private boolean checklistComplete;
    private boolean quickNotes;
    private boolean quickNoteWeight;
    private boolean quickNoteHydration;
    private boolean quickNoteEpds;
    private boolean quickNoteFetalMovement;
    private boolean quickNoteBloodPressure;
    private boolean quickNoteBloodGlucose;

    @JsonIgnore
    private final Map<String, Object> additionalPermissions = new LinkedHashMap<>();

    public FamilyPermission(boolean calendar, boolean logs, boolean alerts, boolean records) {
        this(calendar, logs, alerts, records, false, false);
    }

    public FamilyPermission(
            boolean calendar,
            boolean logs,
            boolean alerts,
            boolean records,
            boolean checklistView,
            boolean checklistComplete) {
        this(calendar, logs, alerts, records, checklistView, checklistComplete,
                false, false, false, false, false);
    }

    public FamilyPermission(
            boolean calendar,
            boolean logs,
            boolean alerts,
            boolean records,
            boolean checklistView,
            boolean checklistComplete,
            boolean quickNotes,
            boolean quickNoteWeight,
            boolean quickNoteHydration,
            boolean quickNoteEpds,
            boolean quickNoteFetalMovement) {
        this(calendar, logs, alerts, records, checklistView, checklistComplete,
                quickNotes, quickNoteWeight, quickNoteHydration, quickNoteEpds,
                quickNoteFetalMovement, false, false);
    }

    public FamilyPermission(
            boolean calendar,
            boolean logs,
            boolean alerts,
            boolean records,
            boolean checklistView,
            boolean checklistComplete,
            boolean quickNotes,
            boolean quickNoteWeight,
            boolean quickNoteHydration,
            boolean quickNoteEpds,
            boolean quickNoteFetalMovement,
            boolean quickNoteBloodPressure,
            boolean quickNoteBloodGlucose) {
        this.calendar = calendar;
        this.logs = logs;
        this.alerts = alerts;
        this.records = records;
        this.checklistView = checklistView;
        this.checklistComplete = checklistComplete;
        this.quickNotes = quickNotes;
        this.quickNoteWeight = quickNoteWeight;
        this.quickNoteHydration = quickNoteHydration;
        this.quickNoteEpds = quickNoteEpds;
        this.quickNoteFetalMovement = quickNoteFetalMovement;
        this.quickNoteBloodPressure = quickNoteBloodPressure;
        this.quickNoteBloodGlucose = quickNoteBloodGlucose;
    }

    @JsonAnySetter
    public void putAdditionalPermission(String name, Object value) {
        additionalPermissions.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> additionalPermissions() {
        return additionalPermissions;
    }

    public void copyAdditionalPermissionsFrom(FamilyPermission source) {
        if (source != null) {
            additionalPermissions.putAll(source.additionalPermissions);
        }
    }

    public static FamilyPermission defaults() {
        return new FamilyPermission(
                false, false, false, false, false, false,
                false, false, false, false, false, false, false);
    }

    public static FamilyPermission fromJson(String json) {
        if (json == null || json.isBlank()) {
            return defaults();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isObject()
                    || hasNonBooleanPermission(root, "calendar")
                    || hasNonBooleanPermission(root, "logs")
                    || hasNonBooleanPermission(root, "alerts")
                    || hasNonBooleanPermission(root, "records")
                    || hasNonBooleanPermission(root, "CHECKLIST_VIEW")
                    || hasNonBooleanPermission(root, "CHECKLIST_COMPLETE")
                    || hasNonBooleanPermission(root, "checklist_view")
                    || hasNonBooleanPermission(root, "checklist_complete")
                    || hasNonBooleanPermission(root, "quickNotes")
                    || hasNonBooleanPermission(root, "quickNoteWeight")
                    || hasNonBooleanPermission(root, "quickNoteHydration")
                    || hasNonBooleanPermission(root, "quickNoteEpds")
                    || hasNonBooleanPermission(root, "quickNoteFetalMovement")
                    || hasNonBooleanPermission(root, "quickNoteBloodPressure")
                    || hasNonBooleanPermission(root, "quickNoteBloodGlucose")) {
                return defaults();
            }
            return MAPPER.treeToValue(root, FamilyPermission.class);
        } catch (JsonProcessingException e) {
            return defaults();
        }
    }

    private static boolean hasNonBooleanPermission(JsonNode root, String fieldName) {
        return root.has(fieldName) && !root.get(fieldName).isBoolean();
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"calendar\":false,\"logs\":false,\"alerts\":false,\"records\":false,"
                    + "\"CHECKLIST_VIEW\":false,\"CHECKLIST_COMPLETE\":false,"
                    + "\"quickNotes\":false,\"quickNoteWeight\":false,\"quickNoteHydration\":false,"
                    + "\"quickNoteEpds\":false,\"quickNoteFetalMovement\":false,"
                    + "\"quickNoteBloodPressure\":false,\"quickNoteBloodGlucose\":false}";
        }
    }
}
