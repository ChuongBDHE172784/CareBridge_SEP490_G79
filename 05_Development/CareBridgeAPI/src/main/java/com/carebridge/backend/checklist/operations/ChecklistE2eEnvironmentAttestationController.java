package com.carebridge.backend.checklist.operations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-side safety attestation required before the destructive checklist live-E2E harness runs.
 * The bean is absent unless the disposable environment explicitly enables it.
 */
@RestController
@RequestMapping("/api/v1/operations/checklist-e2e")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS')")
@ConditionalOnProperty(
        prefix = "carebridge.checklist.e2e",
        name = "attestation-enabled",
        havingValue = "true")
public class ChecklistE2eEnvironmentAttestationController {

    private final Attestation response;

    public ChecklistE2eEnvironmentAttestationController(
            @Value("${carebridge.checklist.e2e.environment-id:}") String environmentId,
            @Value("${carebridge.checklist.e2e.disposable:false}") boolean disposable) {
        if (!disposable) {
            throw new IllegalStateException(
                    "Checklist E2E attestation requires an explicitly disposable environment");
        }
        if (environmentId == null || environmentId.isBlank()) {
            throw new IllegalStateException(
                    "Checklist E2E attestation requires a non-blank environment ID");
        }
        this.response = new Attestation(environmentId.trim(), true);
    }

    @GetMapping("/attestation")
    public ResponseEntity<Attestation> attest() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    public record Attestation(String environmentId, boolean disposable) {}
}
