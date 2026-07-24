package com.carebridge.backend.consultation.context.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TriageExpertHandoffCreateRequest {

    private UUID clientRequestId;
    private UUID expertProfileId;
    private Boolean consentAccepted;
    private String consentPolicyVersion;
    @Setter(lombok.AccessLevel.NONE)
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public TriageExpertHandoffCreateRequest(
            UUID clientRequestId,
            UUID expertProfileId,
            Boolean consentAccepted,
            String consentPolicyVersion) {
        this.clientRequestId = clientRequestId;
        this.expertProfileId = expertProfileId;
        this.consentAccepted = consentAccepted;
        this.consentPolicyVersion = consentPolicyVersion;
    }

    @JsonAnySetter
    void captureUnknownField(String name, Object value) {
        unknownFields.put(name, value);
    }
}
