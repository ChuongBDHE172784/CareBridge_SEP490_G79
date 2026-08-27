package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CB-TRIAGE-CONSENT-IMP-001 §8.1 — accept-disclaimer input DTO.
 *
 * <p>Deliberately has NO owner/user field: {@code owner_user_id} always comes from the JWT
 * {@code SecurityContext} (BR-TDC-006). Unknown JSON fields are ignored by default Jackson
 * binding and can never designate another owner (TDC-TC-16).
 */
@Getter
@Setter
@NoArgsConstructor
public class AcceptTriageConsentRequest {

    /** The version the client DISPLAYED to the user. Matches {@code policy_version varchar(80)}. */
    @NotBlank
    @Size(max = 80)
    private String policyVersion;

    /** Optional, e.g. {@code "vi"}. Matches {@code data_permissions.locale varchar(20)}. */
    @Size(max = 20)
    private String locale;
}
