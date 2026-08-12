package com.carebridge.backend.audit.service.impl;

import com.carebridge.backend.audit.dto.request.AddSecurityNoteRequest;
import com.carebridge.backend.audit.dto.request.ReviewSecurityEventRequest;
import com.carebridge.backend.audit.dto.request.ResolveSecurityIncidentRequest;
import com.carebridge.backend.audit.dto.response.SecurityEventNoteResponse;
import com.carebridge.backend.audit.dto.response.SecurityEventResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.SecurityEvent;
import com.carebridge.backend.audit.entity.SecurityEventNote;
import com.carebridge.backend.audit.entity.SecurityEventType;
import com.carebridge.backend.audit.repository.SecurityEventNoteRepository;
import com.carebridge.backend.audit.repository.SecurityEventRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.SecurityIncidentService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityIncidentServiceImpl implements SecurityIncidentService {

    private final SecurityEventRepository securityEventRepository;
    private final SecurityEventNoteRepository securityEventNoteRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<SecurityEventResponse> searchEvents(
            UUID userId, String eventType, String severity, String status,
            String ipAddress, Instant from, Instant to, Pageable pageable) {

        SecurityEventType typeEnum = null;
        if (eventType != null && !eventType.isBlank()) {
            typeEnum = SecurityEventType.valueOf(eventType.toUpperCase());
        }

        return securityEventRepository.search(userId, typeEnum, severity, status, ipAddress, from, to, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityEventResponse> getTimeline(UUID correlationId) {
        return securityEventRepository.findByCorrelationIdOrderByOccurredAtAsc(correlationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityEventResponse getEvent(Long eventId) {
        return securityEventRepository.findById(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Security event not found: " + eventId));
    }

    @Override
    @Transactional
    public SecurityEventResponse reviewEvent(Long eventId, ReviewSecurityEventRequest request, UUID reviewerId) {
        SecurityEvent event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Security event not found: " + eventId));

        Instant now = Instant.now();
        securityEventRepository.updateStatus(eventId, request.status(), reviewerId, now);

        event.setStatus(request.status());
        event.setReviewedBy(reviewerId);
        event.setReviewedAt(now);

        auditService.log(AuditAction.SECURITY_EVENT_REVIEWED, reviewerId, "SecurityEvent",
                eventId.toString(), request.status());

        return toResponse(event);
    }

    @Override
    @Transactional
    public SecurityEventResponse resolveEvent(
            Long eventId, ResolveSecurityIncidentRequest request, UUID reviewerId) {
        SecurityEvent event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Security event not found: " + eventId));
        if ("RESOLVED".equals(event.getStatus()) || "FALSE_POSITIVE".equals(event.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "SECURITY_INCIDENT_CLOSED",
                    "Security incident is already closed");
        }

        String resolutionNote = "[RESOLUTION]\n"
                + "Root cause: " + request.rootCause().trim() + "\n"
                + "Affected scope: " + request.affectedScope().trim() + "\n"
                + "Notify affected: " + request.notifyAffected() + "\n"
                + "Remediation: " + String.join(" | ", request.remediationTasks()) + "\n"
                + "Summary: " + request.summary().trim();
        SecurityEventNote note = SecurityEventNote.builder()
                .eventId(eventId)
                .authorId(reviewerId)
                .noteText(resolutionNote)
                .build();
        securityEventNoteRepository.save(note);

        String targetStatus = "FALSE_POSITIVE".equals(request.rootCause()) ? "FALSE_POSITIVE" : "RESOLVED";
        Instant now = Instant.now();
        securityEventRepository.updateStatus(eventId, targetStatus, reviewerId, now);
        event.setStatus(targetStatus);
        event.setReviewedBy(reviewerId);
        event.setReviewedAt(now);

        auditService.log(AuditAction.SECURITY_EVENT_REVIEWED, reviewerId, "SecurityEvent",
                eventId.toString(), targetStatus);
        return toResponse(event);
    }

    @Override
    @Transactional
    public SecurityEventNoteResponse addNote(Long eventId, AddSecurityNoteRequest request, UUID authorId) {
        if (!securityEventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Security event not found: " + eventId);
        }

        SecurityEventNote note = SecurityEventNote.builder()
                .eventId(eventId)
                .authorId(authorId)
                .noteText(request.noteText().trim())
                .build();

        SecurityEventNote saved = securityEventNoteRepository.save(note);
        auditService.log(AuditAction.SECURITY_NOTE_ADDED, authorId, "SecurityEvent", eventId.toString(), null);

        return toNoteResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityEventNoteResponse> getNotes(Long eventId) {
        if (!securityEventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Security event not found: " + eventId);
        }
        return securityEventNoteRepository.findByEventIdOrderByCreatedAtAsc(eventId)
                .stream()
                .map(this::toNoteResponse)
                .toList();
    }

    private SecurityEventResponse toResponse(SecurityEvent e) {
        return new SecurityEventResponse(
                e.getId(),
                e.getEventType() != null ? e.getEventType().name() : null,
                e.getUserId(),
                e.getIpAddress(),
                e.getSeverity(),
                e.getStatus(),
                e.getDetails(),
                e.getCorrelationId(),
                e.getReviewedBy(),
                e.getReviewedAt(),
                e.getOccurredAt()
        );
    }

    private SecurityEventNoteResponse toNoteResponse(SecurityEventNote n) {
        return new SecurityEventNoteResponse(
                n.getNoteId(),
                n.getEventId(),
                n.getAuthorId(),
                n.getNoteText(),
                n.getCreatedAt()
        );
    }
}
