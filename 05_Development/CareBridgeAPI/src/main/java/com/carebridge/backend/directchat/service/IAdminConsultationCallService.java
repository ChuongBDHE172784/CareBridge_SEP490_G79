package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.request.AdminConsultationCallSearchQuery;
import com.carebridge.backend.directchat.dto.response.AdminConsultationCallSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminConsultationCallService {

    Page<AdminConsultationCallSummaryResponse> searchCalls(AdminConsultationCallSearchQuery query, Pageable pageable);

    AdminConsultationCallSummaryResponse getCallDetail(UUID callId);

    String getRecordingPresignedUrl(UUID callId, UUID adminUserId);

    void deleteRecording(UUID callId, UUID adminUserId);
}
