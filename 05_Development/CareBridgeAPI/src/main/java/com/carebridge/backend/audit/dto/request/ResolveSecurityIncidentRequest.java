package com.carebridge.backend.audit.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ResolveSecurityIncidentRequest(
        @NotBlank @Size(max = 100) String rootCause,
        @NotBlank @Size(min = 20, max = 5000) String summary,
        @NotBlank @Size(max = 500) String affectedScope,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 500) String> remediationTasks,
        boolean notifyAffected,
        @AssertTrue(message = "Resolution must be explicitly confirmed") boolean confirmed
) {}
