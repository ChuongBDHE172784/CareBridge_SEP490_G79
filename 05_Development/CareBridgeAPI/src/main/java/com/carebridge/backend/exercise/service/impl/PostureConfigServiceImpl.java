package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.AdminPostureConfigResponse;
import com.carebridge.backend.exercise.dto.CreatePostureConfigRequest;
import com.carebridge.backend.exercise.dto.PostureConfigResponse;
import com.carebridge.backend.exercise.dto.UpdatePostureConfigRequest;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidPostureConfigException;
import com.carebridge.backend.exercise.exception.PostureConfigNotFoundException;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.service.IPostureConfigService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostureConfigServiceImpl implements IPostureConfigService {

    private final ExerciseRepository exerciseRepository;
    private final PostureAnalysisConfigRepository postureConfigRepository;
    private final AuditService auditService;

    // === EXISTING (unchanged, Mother-facing, consumed by UC180 — RG-3 / constraint C1) ===
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PostureConfigResponse> getActiveConfig(UUID exerciseId) {
        PregnancyExercise exercise = exerciseRepository
                .findByExerciseIdAndStatus(exerciseId, ExerciseStatus.PUBLISHED)
                .orElseThrow(ExerciseNotFoundException::notFound);

        if (!Boolean.TRUE.equals(exercise.getSupportsPostureAnalysis())) {
            throw ExerciseNotFoundException.notFound();
        }

        PostureAnalysisConfig config = postureConfigRepository
                .findActiveConfigByExerciseId(exerciseId, OffsetDateTime.now())
                .orElseThrow(() ->
                        new ResourceNotFoundException("No active posture analysis configuration found"));

        PostureConfigResponse response = PostureConfigResponse.builder()
                .postureConfigId(config.getPostureConfigId())
                .exerciseId(config.getExerciseId())
                .analysisMode(config.getAnalysisMode())
                .ruleOrModelVersion(config.getRuleOrModelVersion())
                .confidenceThreshold(config.getConfidenceThreshold())
                .feedbackLevel(config.getFeedbackLevel())
                .effectiveFrom(config.getEffectiveFrom())
                .build();

        return ApiResponse.success(response);
    }

    // === NEW (UC186 admin write side) ===

    @Override
    @Transactional
    public ApiResponse<AdminPostureConfigResponse> createConfig(
            CreatePostureConfigRequest request, UUID adminUserId) {
        PregnancyExercise exercise = exerciseRepository
                .findById(request.getExerciseId())
                .orElseThrow(ExerciseNotFoundException::notFound);

        if (!Boolean.TRUE.equals(exercise.getSupportsPostureAnalysis())) {
            throw InvalidPostureConfigException.doesNotSupportPostureAnalysis();
        }

        if (postureConfigRepository.existsByExerciseId(request.getExerciseId())) {
            throw InvalidPostureConfigException.alreadyExists();
        }

        OffsetDateTime now = OffsetDateTime.now();
        PostureAnalysisConfig config = PostureAnalysisConfig.builder()
                .postureConfigId(UUID.randomUUID())
                .exerciseId(request.getExerciseId())
                .configuredBy(adminUserId)
                .analysisMode(request.getAnalysisMode().name())
                .ruleOrModelVersion(request.getRuleOrModelVersion())
                .confidenceThreshold(request.getConfidenceThreshold())
                .feedbackLevel(request.getFeedbackLevel() != null
                        ? request.getFeedbackLevel().name() : null)
                .configJson(request.getConfigJson())
                .effectiveFrom(now)
                .effectiveTo(null)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        PostureAnalysisConfig saved = postureConfigRepository.save(config);

        auditService.log(
                AuditAction.POSTURE_CONFIG_CREATED,
                adminUserId,
                "PostureAnalysisConfig",
                saved.getPostureConfigId().toString(),
                toAuditDetails(saved, null));

        return ApiResponse.success(toResponse(saved), "Posture analysis config created successfully");
    }

    @Override
    @Transactional
    public ApiResponse<AdminPostureConfigResponse> createNewVersion(
            UUID exerciseId, UpdatePostureConfigRequest request, UUID adminUserId) {
        PostureAnalysisConfig previous = postureConfigRepository
                .findByExerciseIdAndStatus(exerciseId, "ACTIVE")
                .orElseThrow(PostureConfigNotFoundException::noActiveConfig);

        OffsetDateTime now = OffsetDateTime.now();

        PostureAnalysisConfig newVersion = PostureAnalysisConfig.builder()
                .postureConfigId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .configuredBy(adminUserId)
                .analysisMode(request.getAnalysisMode().name())
                .ruleOrModelVersion(request.getRuleOrModelVersion())
                .confidenceThreshold(request.getConfidenceThreshold())
                .feedbackLevel(request.getFeedbackLevel() != null
                        ? request.getFeedbackLevel().name() : null)
                .configJson(request.getConfigJson())
                .effectiveFrom(now)
                .effectiveTo(null)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        PostureAnalysisConfig savedNewVersion = postureConfigRepository.save(newVersion);

        // Supersede the previous ACTIVE row — only status/effectiveTo flip, analysis
        // parameters remain byte-for-byte unchanged (ADR-PAC-002, Logic Issue L1).
        previous.setStatus("SUPERSEDED");
        previous.setEffectiveTo(now);
        previous.setUpdatedAt(now);
        postureConfigRepository.save(previous);

        auditService.log(
                AuditAction.POSTURE_CONFIG_UPDATED,
                adminUserId,
                "PostureAnalysisConfig",
                savedNewVersion.getPostureConfigId().toString(),
                toAuditDetails(savedNewVersion, previous.getPostureConfigId()));

        return ApiResponse.success(
                toResponse(savedNewVersion), "New posture analysis config version created and activated");
    }

    @Override
    @Transactional
    public ApiResponse<AdminPostureConfigResponse> activateVersion(UUID postureConfigId, UUID adminUserId) {
        PostureAnalysisConfig target = postureConfigRepository
                .findById(postureConfigId)
                .orElseThrow(PostureConfigNotFoundException::notFound);

        if ("ACTIVE".equals(target.getStatus())) {
            // Idempotent no-op — no state change, no audit entry.
            return ApiResponse.success(toResponse(target), "Posture analysis config version activated");
        }

        OffsetDateTime now = OffsetDateTime.now();

        postureConfigRepository
                .findByExerciseIdAndStatus(target.getExerciseId(), "ACTIVE")
                .ifPresent(currentlyActive -> {
                    currentlyActive.setStatus("SUPERSEDED");
                    currentlyActive.setEffectiveTo(now);
                    currentlyActive.setUpdatedAt(now);
                    postureConfigRepository.save(currentlyActive);
                });

        target.setStatus("ACTIVE");
        target.setEffectiveFrom(now);
        target.setEffectiveTo(null);
        target.setUpdatedAt(now);
        PostureAnalysisConfig saved = postureConfigRepository.save(target);

        auditService.log(
                AuditAction.POSTURE_CONFIG_ACTIVATED,
                adminUserId,
                "PostureAnalysisConfig",
                saved.getPostureConfigId().toString(),
                toAuditDetails(saved, null));

        return ApiResponse.success(toResponse(saved), "Posture analysis config version activated");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<AdminPostureConfigResponse>> listVersions(UUID exerciseId) {
        List<AdminPostureConfigResponse> versions = postureConfigRepository
                .findAllByExerciseIdOrderByEffectiveFromDesc(exerciseId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(versions);
    }

    private AdminPostureConfigResponse toResponse(PostureAnalysisConfig config) {
        return AdminPostureConfigResponse.builder()
                .postureConfigId(config.getPostureConfigId())
                .exerciseId(config.getExerciseId())
                .configuredBy(config.getConfiguredBy())
                .analysisMode(config.getAnalysisMode())
                .ruleOrModelVersion(config.getRuleOrModelVersion())
                .confidenceThreshold(config.getConfidenceThreshold())
                .feedbackLevel(config.getFeedbackLevel())
                .configJson(config.getConfigJson())
                .effectiveFrom(config.getEffectiveFrom())
                .effectiveTo(config.getEffectiveTo())
                .status(config.getStatus())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private java.util.Map<String, Object> toAuditDetails(
            PostureAnalysisConfig config, UUID supersededConfigId) {
        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("exerciseId", config.getExerciseId());
        details.put("analysisMode", config.getAnalysisMode());
        details.put("ruleOrModelVersion", config.getRuleOrModelVersion());
        details.put("confidenceThreshold", config.getConfidenceThreshold());
        details.put("feedbackLevel", config.getFeedbackLevel());
        details.put("status", config.getStatus());
        if (supersededConfigId != null) {
            details.put("supersededConfigId", supersededConfigId);
        }
        return details;
    }
}
