package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.dto.CreateExerciseRequest;
import com.carebridge.backend.exercise.dto.UpdateExerciseRequest;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.policy.ExercisePublishReadinessPolicy;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.service.IAdminExerciseService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminExerciseServiceImpl implements IAdminExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;
    private final AuditService auditService;
    private final ExercisePublishReadinessPolicy publishReadinessPolicy;

    @Override
    @Transactional
    public AdminExerciseResponse create(CreateExerciseRequest request, UUID adminUserId) {
        PregnancyExercise entity = exerciseMapper.toEntity(request, adminUserId);
        PregnancyExercise saved = exerciseRepository.save(entity);

        auditService.log(
                AuditAction.EXERCISE_CREATED,
                adminUserId,
                "PregnancyExercise",
                saved.getExerciseId().toString(),
                auditDetails(saved));

        return exerciseMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExerciseResponse update(UUID exerciseId, UpdateExerciseRequest request, UUID adminUserId) {
        PregnancyExercise entity = exerciseRepository
                .findById(exerciseId)
                .orElseThrow(ExerciseNotFoundException::notFound);

        // ADR-EXERCISE-ADMIN-004: an explicit blank string is rejected; null means "unchanged".
        if (request.getSafetyWarning() != null && request.getSafetyWarning().isBlank()) {
            throw InvalidExerciseStateException.safetyWarningCannotBeBlanked();
        }

        exerciseMapper.applyUpdate(entity, request);
        entity.setVersionNo(entity.getVersionNo() + 1);
        PregnancyExercise saved = exerciseRepository.save(entity);

        auditService.log(
                AuditAction.EXERCISE_UPDATED,
                adminUserId,
                "PregnancyExercise",
                saved.getExerciseId().toString(),
                auditDetails(saved));

        return exerciseMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExerciseResponse activate(UUID exerciseId, UUID adminUserId) {
        PregnancyExercise entity = exerciseRepository
                .findById(exerciseId)
                .orElseThrow(ExerciseNotFoundException::notFound);

        if (entity.getStatus() == ExerciseStatus.PUBLISHED) {
            // Idempotent no-op.
            return exerciseMapper.toAdminResponse(entity);
        }

        publishReadinessPolicy.verifyReady(entity);

        entity.setStatus(ExerciseStatus.PUBLISHED);
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        PregnancyExercise saved = exerciseRepository.save(entity);

        auditService.log(
                AuditAction.EXERCISE_ACTIVATED,
                adminUserId,
                "PregnancyExercise",
                saved.getExerciseId().toString(),
                auditDetails(saved));

        return exerciseMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExerciseResponse disable(UUID exerciseId, UUID adminUserId) {
        PregnancyExercise entity = exerciseRepository
                .findById(exerciseId)
                .orElseThrow(ExerciseNotFoundException::notFound);

        if (entity.getStatus() == ExerciseStatus.ARCHIVED) {
            // Idempotent no-op.
            return exerciseMapper.toAdminResponse(entity);
        }

        entity.setStatus(ExerciseStatus.ARCHIVED);
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        PregnancyExercise saved = exerciseRepository.save(entity);

        auditService.log(
                AuditAction.EXERCISE_DISABLED,
                adminUserId,
                "PregnancyExercise",
                saved.getExerciseId().toString(),
                auditDetails(saved));

        return exerciseMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminExerciseResponse getById(UUID exerciseId) {
        PregnancyExercise entity = exerciseRepository
                .findById(exerciseId)
                .orElseThrow(ExerciseNotFoundException::notFound);
        return exerciseMapper.toAdminResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<AdminExerciseResponse> list(
            ExerciseStatus status, TrimesterScope trimester, DifficultyLevel difficulty, int page, int size) {
        var pageResult = exerciseRepository
                .findAllByFilters(status, trimester, difficulty, PageRequest.of(page, size))
                .map(exerciseMapper::toAdminResponse);
        return PaginatedResponse.of(pageResult);
    }

    private Map<String, Object> auditDetails(PregnancyExercise entity) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("title", entity.getTitle());
        details.put("status", entity.getStatus());
        details.put("versionNo", entity.getVersionNo());
        return details;
    }
}
