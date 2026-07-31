package com.carebridge.backend.family.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.LinkedHashMap;
import java.util.Map;

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
        this.calendar = calendar;
        this.logs = logs;
        this.alerts = alerts;
        this.records = records;
        this.checklistView = checklistView;
        this.checklistComplete = checklistComplete;
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
        return new FamilyPermission(false, false, false, false);
    }

    public static FamilyPermission fromJson(String json) {
        if (json == null || json.isBlank()) {
            return defaults();
        }
        try {
            var root = MAPPER.readTree(json);
            if (!root.isObject()
                    || hasNonBooleanPermission(root, "CHECKLIST_VIEW")
                    || hasNonBooleanPermission(root, "CHECKLIST_COMPLETE")
                    || hasNonBooleanPermission(root, "checklist_view")
                    || hasNonBooleanPermission(root, "checklist_complete")) {
                return defaults();
            }
            return MAPPER.treeToValue(root, FamilyPermission.class);
        } catch (JsonProcessingException e) {
            return defaults();
        }
    }

    private static boolean hasNonBooleanPermission(
            com.fasterxml.jackson.databind.JsonNode root,
            String fieldName) {
        return root.has(fieldName) && !root.get(fieldName).isBoolean();
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"calendar\":false,\"logs\":false,\"alerts\":false,\"records\":false}";
        }
    }
}
