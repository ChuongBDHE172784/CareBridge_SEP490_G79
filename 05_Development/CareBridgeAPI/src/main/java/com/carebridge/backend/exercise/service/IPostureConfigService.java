package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureConfigResponse;
import java.util.UUID;

public interface IPostureConfigService {

    ApiResponse<PostureConfigResponse> getActiveConfig(UUID exerciseId);
}
