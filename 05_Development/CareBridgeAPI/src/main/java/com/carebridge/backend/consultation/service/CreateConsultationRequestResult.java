package com.carebridge.backend.consultation.service;

import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;

public record CreateConsultationRequestResult(
        ConsultationRequestResponse response,
        boolean created) {
}
