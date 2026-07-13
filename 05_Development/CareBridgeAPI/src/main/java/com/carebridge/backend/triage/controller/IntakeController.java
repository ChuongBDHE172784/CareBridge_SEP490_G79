package com.carebridge.backend.triage.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/triage/intake")
@RequiredArgsConstructor
public class IntakeController {

    private static final Logger log = LoggerFactory.getLogger(IntakeController.class);

    private final ITriageService triageService;
    private final ChildTriageAiClient childTriageAiClient;
    private final TriageGraphService triageGraphService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<IntakeSessionResponse>> runIntake(
            @Valid @RequestBody RunIntakeRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        IntakeSessionResponse response = triageService.runIntake(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<TriageResultResponse>> getResult(
            @PathVariable UUID sessionId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(triageService.getResult(sessionId, userId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<IntakeSessionResponse>>> listSessions(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(triageService.listSessions(userId)));
    }

    @PostMapping("/conversation/start")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startConversation(
            @RequestBody Map<String, Object> request,
            Principal principal) {
        SecurityUtils.requireCurrentUserId(principal);
        try {
            return ResponseEntity.ok(ApiResponse.success(readJsonObject(childTriageAiClient.startIntake(request))));
        } catch (Exception e) {
            log.warn("AI triage conversation start unavailable, using Java fallback: {}", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.success(fallbackConversationResult(request, true)));
        }
    }

    @PostMapping("/conversation/continue")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> continueConversation(
            @RequestBody Map<String, Object> request,
            Principal principal) {
        SecurityUtils.requireCurrentUserId(principal);
        try {
            return ResponseEntity.ok(ApiResponse.success(readJsonObject(childTriageAiClient.continueIntake(request))));
        } catch (Exception e) {
            log.warn("AI triage conversation continue unavailable, using Java fallback: {}", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.success(fallbackConversationResult(request, false)));
        }
    }

    private Map<String, Object> readJsonObject(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid AI triage conversation response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fallbackConversationResult(Map<String, Object> request, boolean start) {
        Object current = request.get("currentIntake");
        Map<String, Object> intakeMap = current instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map)
                : new LinkedHashMap<>();
        if (start && request.get("initialText") != null && intakeMap.get("parentFreeText") == null) {
            intakeMap.put("parentFreeText", request.get("initialText"));
            intakeMap.put("symptoms", request.get("initialText"));
        }
        RunIntakeRequest intake = objectMapper.convertValue(intakeMap, RunIntakeRequest.class);
        ChildTriageResult result = triageGraphService.run(intake);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "TRIAGE_COMPLETE");
        response.put("intakeSessionId", request.getOrDefault("intakeSessionId", UUID.randomUUID().toString()));
        response.put("mergedIntake", intakeMap);
        response.put("assistantMessage", "CareBridge da hoan tat phan loai bang Java fallback.");
        response.put("questions", List.of());
        response.put("round", request.getOrDefault("round", 1));
        response.put("triageResult", objectMapper.convertValue(result, new TypeReference<Map<String, Object>>() {}));
        return response;
    }
}
