package com.carebridge.backend.consultation.context.service;

import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.HandoffParticipantResponse;
import com.carebridge.backend.consultation.context.dto.HandoffPreviewResponse;
import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import java.util.UUID;

public interface ITriageExpertHandoffService {

    HandoffPreviewResponse preview(UUID intakeSessionId, UUID ownerUserId);

    HandoffCreateResponse create(
            UUID intakeSessionId,
            TriageExpertHandoffCreateRequest request,
            UUID ownerUserId);

    HandoffParticipantResponse read(UUID consultationRequestId, UUID currentUserId);
}
