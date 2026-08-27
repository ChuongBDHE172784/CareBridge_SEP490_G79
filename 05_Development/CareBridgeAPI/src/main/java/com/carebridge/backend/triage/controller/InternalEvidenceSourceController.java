package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.response.ApprovedEvidenceSourceResponse;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service registry. It is intentionally separate from the parent API and
 * guarded by a deployment secret; the Python service never receives admin authority.
 */
@RestController
@RequestMapping("/internal/api/v1/triage/evidence-sources")
@RequiredArgsConstructor
public class InternalEvidenceSourceController {
    private final EvidenceSourceService evidenceSourceService;

    @Value("${ai.triage-service.internal-api-key:}")
    private String internalApiKey;

    @GetMapping("/approved")
    public ApiResponse<List<ApprovedEvidenceSourceResponse>> approved(
            @RequestParam String stage,
            @RequestHeader(name = "X-CareBridge-Internal-Key", required = false) String suppliedKey) {
        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(suppliedKey)) {
            throw new TriageException(HttpStatus.UNAUTHORIZED, "TRIAGE-013", "Invalid internal service credential");
        }
        TriageStage resolved;
        try {
            resolved = TriageStage.valueOf(legacyEvidenceStage(stage));
        } catch (Exception exception) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-011", "Invalid triage stage");
        }
        return ApiResponse.success(evidenceSourceService.approvedForStage(resolved.name()).stream()
                .map(ApprovedEvidenceSourceResponse::from)
                .toList());
    }

    private static String legacyEvidenceStage(String stage) {
        String normalized = stage == null ? "" : stage.trim().toUpperCase();
        return switch (normalized) {
            case "POSSIBLE_PREGNANCY" -> "PRECONCEPTION";
            case "POSTPARTUM_MOTHER" -> "POSTPARTUM";
            case "INFANT_0_12M" -> "INFANT";
            case "TODDLER_12_24M" -> "TODDLER";
            default -> normalized;
        };
    }
}
