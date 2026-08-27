package com.carebridge.backend.triage.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CB-TRIAGE-CONSENT-IMP-001 (ADR-TDC-003) — config-backed canonical disclaimer version/text.
 *
 * <p>Single source of truth for the AI-triage disclaimer: {@code carebridge.triage.disclaimer.*}
 * in {@code application.yaml}. Mirrors the {@code TriageExpertHandoffPolicy.POLICY_VERSION}
 * constant-holder pattern. Pure component — no I/O beyond configuration injection.
 *
 * <p>Wording of the default text is an {@code Open} item pending product + DPO sign-off
 * (ADR-TDC-003); a version bump here forces fleet-wide re-consent (BR-TDC-002).
 */
@Component
public class TriageDisclaimerPolicy {

    public static final String PERMISSION_KIND = "AI_TRIAGE_DISCLAIMER"; // 20 ≤ varchar(30)
    public static final String SCOPE_TYPE = "TRIAGE";                    // ≤ varchar(50)
    public static final String SCOPE_TEXT = "ELECTIVE_AI_TRIAGE_INTAKE_ONLY";
    public static final String PURPOSE = "AI_TRIAGE_GUIDANCE";           // ≤ varchar(255)
    public static final String REQUIRED_TEXT =
            "Thông tin từ AI chỉ mang tính chất tham khảo, bạn cần tham vấn trực tiếp "
                    + "Bác sĩ/Chuyên gia Y tế khi có triệu chứng bất thường.";

    private final String version;
    private final String text;

    public TriageDisclaimerPolicy(
            @Value("${carebridge.triage.disclaimer.version:AI_TRIAGE_DISCLAIMER_V2}") String version,
            @Value("${carebridge.triage.disclaimer.text:"
                    + "Thông tin từ AI chỉ mang tính chất tham khảo, bạn cần tham vấn trực tiếp "
                    + "Bác sĩ/Chuyên gia Y tế khi có triệu chứng bất thường.}") String text) {
        this.version = version;
        this.text = text;
    }

    /** Currently effective disclaimer version — {@code carebridge.triage.disclaimer.version}. */
    public String currentVersion() {
        return version;
    }

    /** Canonical dialog text — {@code carebridge.triage.disclaimer.text}. */
    public String disclaimerText() {
        return text;
    }

    /** SHA-256 hex (64 chars ≤ varchar(255)) of the accepted text — consent evidence key. */
    public String evidenceKeyFor(String acceptedText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(acceptedText.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for consent evidence keys", e);
        }
    }
}
