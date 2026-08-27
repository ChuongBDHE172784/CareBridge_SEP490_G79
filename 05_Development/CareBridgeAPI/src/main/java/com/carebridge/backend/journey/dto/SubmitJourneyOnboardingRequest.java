package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.LifecycleGoal;
import com.carebridge.backend.journey.entity.SupportPreference;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitJourneyOnboardingRequest {

    @NotNull
    private UUID submissionId;

    @NotNull
    private LifecycleGoal lifecycleGoal;

    @NotBlank
    @Pattern(regexp = "^[a-z]{2}(?:-[A-Z]{2})?$")
    private String locale;

    @NotBlank
    @Size(max = 80)
    private String timeZone;

    @NotEmpty
    @Size(max = 4)
    private List<@NotNull SupportPreference> preferences;

    @AssertTrue(message = "Lifecycle consent is required")
    private boolean consentAccepted;

    @NotBlank
    @Pattern(regexp = "MOTHER_LIFECYCLE_V1")
    private String policyVersion;
}
