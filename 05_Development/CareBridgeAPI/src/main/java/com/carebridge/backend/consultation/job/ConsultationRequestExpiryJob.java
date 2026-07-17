package com.carebridge.backend.consultation.job;

import com.carebridge.backend.consultation.service.IConsultationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultationRequestExpiryJob {

    private final IConsultationRequestService service;

    @Scheduled(fixedDelayString = "${carebridge.consultation-request.expiry-job-delay-ms:60000}")
    public void expireOverdue() {
        service.expireOverdueRequests();
    }
}
