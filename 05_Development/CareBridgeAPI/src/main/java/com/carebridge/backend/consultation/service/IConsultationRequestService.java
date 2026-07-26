package com.carebridge.backend.consultation.service;

import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestPendingSummaryResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestSummaryResponse;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IConsultationRequestService {
    CreateConsultationRequestResult create(
            CreateConsultationRequestRequest request, UUID requesterUserId);

    Page<ConsultationRequestSummaryResponse> listMine(
            UUID requesterUserId, ConsultationRequestStatus status, Pageable pageable);

    Page<ConsultationRequestSummaryResponse> listAssigned(
            UUID expertUserId, ConsultationRequestStatus status, Pageable pageable);

    ConsultationRequestResponse getById(UUID id, UUID currentUserId);

    ConsultationRequestResponse accept(UUID id, UUID expertUserId);

    ConsultationRequestResponse reject(UUID id, UUID expertUserId, String reason);

    ConsultationRequestResponse cancel(UUID id, UUID requesterUserId);

    ConsultationRequestPendingSummaryResponse pendingSummary(UUID expertUserId);

    int expireOverdueRequests();
}
