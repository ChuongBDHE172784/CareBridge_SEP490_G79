package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.AdminPostureConfigResponse;
import com.carebridge.backend.exercise.dto.CreatePostureConfigRequest;
import com.carebridge.backend.exercise.dto.PostureConfigResponse;
import com.carebridge.backend.exercise.dto.UpdatePostureConfigRequest;
import java.util.List;
import java.util.UUID;

public interface IPostureConfigService {

    // --- EXISTING (unchanged, Mother-facing, consumed by UC180) ---
    ApiResponse<PostureConfigResponse> getActiveConfig(UUID exerciseId);

    // --- NEW (UC186 admin write side) ---

    /**
     * Creates the first posture analysis config for an exercise. Always status=ACTIVE.
     * @throws com.carebridge.backend.exercise.exception.ExerciseNotFoundException (EX-001) if exerciseId does not exist
     * @throws com.carebridge.backend.exercise.exception.InvalidPostureConfigException (PAC-005) if exercise.supportsPostureAnalysis == false
     * @throws com.carebridge.backend.exercise.exception.InvalidPostureConfigException (PAC-006) if a config already exists for this exercise
     */
    ApiResponse<AdminPostureConfigResponse> createConfig(CreatePostureConfigRequest request, UUID adminUserId);

    /**
     * Creates a new version (supersedes the current ACTIVE row for this exercise).
     * @throws com.carebridge.backend.exercise.exception.PostureConfigNotFoundException (PAC-004) if no ACTIVE config exists yet
     */
    ApiResponse<AdminPostureConfigResponse> createNewVersion(
            UUID exerciseId, UpdatePostureConfigRequest request, UUID adminUserId);

    /**
     * Activates a specific existing version, superseding whatever was previously active
     * for the same exercise. Idempotent if the target is already ACTIVE.
     * @throws com.carebridge.backend.exercise.exception.PostureConfigNotFoundException (PAC-004) if postureConfigId does not exist
     */
    ApiResponse<AdminPostureConfigResponse> activateVersion(UUID postureConfigId, UUID adminUserId);

    /**
     * Lists the full version history (ACTIVE + SUPERSEDED) for an exercise, newest first.
     */
    ApiResponse<List<AdminPostureConfigResponse>> listVersions(UUID exerciseId);
}
