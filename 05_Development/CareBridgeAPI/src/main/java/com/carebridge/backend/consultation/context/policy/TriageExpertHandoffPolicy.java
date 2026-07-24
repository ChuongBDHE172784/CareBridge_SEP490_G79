package com.carebridge.backend.consultation.context.policy;

import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import com.carebridge.backend.consultation.context.exception.TriageExpertHandoffException;
import java.text.Normalizer;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TriageExpertHandoffPolicy {

    public static final String POLICY_VERSION = "YELLOW_EXPERT_CONTEXT_V1";
    public static final String TOPIC = "YELLOW triage expert support";
    public static final String DESCRIPTION =
            "Consented minimum YELLOW triage context is available in the protected context view.";
    public static final String CONSENT_SCOPE =
            "riskLevel,stage,riskSummary,approvedCitationMetadata";

    private static final List<String> SHARED_FIELDS = List.of(
            "YELLOW risk",
            "Lifecycle stage",
            "Risk summary",
            "Approved source metadata");
    private static final List<String> EXCLUDED_FIELDS = List.of(
            "Raw answers or symptoms",
            "Normalized symptoms",
            "Red flags",
            "Claims",
            "Health notes",
            "AI payload",
            "Identifiers or tokens",
            "Route or origin data",
            "Pending or unreviewed sources",
            "Surplus health data");

    public void assertCreateRequest(TriageExpertHandoffCreateRequest request) {
        if (request == null
                || request.getClientRequestId() == null
                || request.getExpertProfileId() == null
                || request.getConsentAccepted() == null
                || !request.getConsentAccepted()
                || request.getConsentPolicyVersion() == null
                || request.getConsentPolicyVersion().isBlank()
                || !request.getUnknownFields().isEmpty()) {
            throw TriageExpertHandoffException.invalidRequest();
        }
        if (!POLICY_VERSION.equals(request.getConsentPolicyVersion())) {
            throw TriageExpertHandoffException.consentPolicyChanged();
        }
    }

    public String sanitizeSummary(String summary) {
        if (summary == null) {
            throw TriageExpertHandoffException.noApprovedContext();
        }

        String normalized = Normalizer.normalize(summary, Normalizer.Form.NFC);
        StringBuilder canonical = new StringBuilder(normalized.length());
        boolean previousWasSpace = true;
        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                if (!previousWasSpace) {
                    canonical.append(' ');
                    previousWasSpace = true;
                }
                continue;
            }
            int type = Character.getType(codePoint);
            if (type == Character.CONTROL || type == Character.FORMAT) {
                if (!previousWasSpace) {
                    canonical.append(' ');
                    previousWasSpace = true;
                }
                continue;
            }
            canonical.appendCodePoint(codePoint);
            previousWasSpace = false;
        }

        int length = canonical.length();
        if (length > 0 && canonical.charAt(length - 1) == ' ') {
            canonical.setLength(length - 1);
        }
        if (canonical.isEmpty()) {
            throw TriageExpertHandoffException.noApprovedContext();
        }

        int codePointCount = canonical.codePointCount(0, canonical.length());
        if (codePointCount <= 500) {
            return canonical.toString();
        }
        int end = canonical.offsetByCodePoints(0, 499);
        return canonical.substring(0, end) + "…";
    }

    public List<String> sharedFields() {
        return SHARED_FIELDS;
    }

    public List<String> excludedFields() {
        return EXCLUDED_FIELDS;
    }
}
