package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.dto.request.AddSecurityNoteRequest;
import com.carebridge.backend.audit.dto.request.ReviewSecurityEventRequest;
import com.carebridge.backend.audit.dto.response.SecurityEventNoteResponse;
import com.carebridge.backend.audit.dto.response.SecurityEventResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SecurityIncidentService {

    Page<SecurityEventResponse> searchEvents(
            UUID userId,
            String eventType,
            String severity,
            String status,
            String ipAddress,
            Instant from,
            Instant to,
            Pageable pageable);

    List<SecurityEventResponse> getTimeline(UUID correlationId);

    SecurityEventResponse reviewEvent(Long eventId, ReviewSecurityEventRequest request, UUID reviewerId);

    SecurityEventNoteResponse addNote(Long eventId, AddSecurityNoteRequest request, UUID authorId);

    List<SecurityEventNoteResponse> getNotes(Long eventId);
}
