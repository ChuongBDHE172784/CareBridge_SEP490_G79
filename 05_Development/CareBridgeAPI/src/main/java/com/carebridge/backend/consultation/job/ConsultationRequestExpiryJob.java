package com.carebridge.backend.consultation.job;

import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultationRequestExpiryJob {

    private final IConsultationRequestService service;
    private final DirectConversationRepository conversationRepository;

    @Scheduled(fixedDelayString = "${carebridge.consultation-request.expiry-job-delay-ms:60000}")
    public void expireOverdue() {
        service.expireOverdueRequests();
        // Cung mot nhip: yeu cau qua han thi het hieu luc, va buoi tu van qua gio thi
        // dong chat. Hai viec deu la "thoi gian da troi qua, dong lai".
        conversationRepository.closeConversationsPastConsultationWindow();
    }
}
