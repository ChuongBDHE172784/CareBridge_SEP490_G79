package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.request.AiPolicyTestRequest;
import com.carebridge.backend.aimoderation.dto.request.CreateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.request.UpdateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyPageResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyTestResponse;
import java.util.UUID;

public interface AiPolicyService {

    AiPolicyPageResponse listPolicies(Boolean active, int page, int size);

    AiPolicyResponse createPolicy(CreateAiPolicyRequest request, UUID actorUserId);

    AiPolicyResponse updatePolicy(UUID policyId, UpdateAiPolicyRequest request, UUID actorUserId);

    AiPolicyResponse updatePolicyStatus(UUID policyId, boolean active, UUID actorUserId);

    AiPolicyTestResponse testPolicies(AiPolicyTestRequest request, UUID actorUserId);
}
