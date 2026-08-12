package com.carebridge.backend.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.dto.request.AddSecurityNoteRequest;
import com.carebridge.backend.audit.dto.request.ReviewSecurityEventRequest;
import com.carebridge.backend.audit.dto.request.ResolveSecurityIncidentRequest;
import com.carebridge.backend.audit.dto.response.SecurityEventResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.SecurityEvent;
import com.carebridge.backend.audit.entity.SecurityEventNote;
import com.carebridge.backend.audit.repository.SecurityEventNoteRepository;
import com.carebridge.backend.audit.repository.SecurityEventRepository;
import com.carebridge.backend.audit.service.impl.SecurityIncidentServiceImpl;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * UC174 (SEC174-TC-001/003/004/008/009) + UC175 (SEC175-TC-002/003) —
 * gap-check tests against the existing SecurityIncidentServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class SecurityIncidentServiceImplTest {

    @Mock private SecurityEventRepository securityEventRepository;
    @Mock private SecurityEventNoteRepository securityEventNoteRepository;
    @Mock private AuditService auditService;

    private SecurityIncidentServiceImpl newService() {
        return new SecurityIncidentServiceImpl(securityEventRepository, securityEventNoteRepository, auditService);
    }

    // SEC174-TC-001 / TC-003
    @Test
    void searchEvents_filtersByUserId_delegatesToRepository() {
        SecurityIncidentServiceImpl service = newService();
        UUID knownUserId = UUID.randomUUID();
        SecurityEvent event = SecurityEvent.builder()
                .id(1L).userId(knownUserId).ipAddress("203.0.113.10")
                .severity("HIGH").status("OPEN").occurredAt(Instant.now())
                .build();
        when(securityEventRepository.search(eq(knownUserId), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<SecurityEventResponse> result = service.searchEvents(
                knownUserId, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo(knownUserId);
    }

    // SEC174-TC-008
    @Test
    void searchEvents_noMatches_returnsEmptyPage() {
        SecurityIncidentServiceImpl service = newService();
        when(securityEventRepository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<SecurityEventResponse> result = service.searchEvents(
                UUID.randomUUID(), null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // SEC174-TC-009
    @Test
    void getTimeline_returnsEventsOrderedByOccurredAt() {
        SecurityIncidentServiceImpl service = newService();
        UUID correlationId = UUID.randomUUID();
        SecurityEvent e1 = SecurityEvent.builder().id(1L).correlationId(correlationId).occurredAt(Instant.now()).build();
        when(securityEventRepository.findByCorrelationIdOrderByOccurredAtAsc(correlationId))
                .thenReturn(List.of(e1));

        List<SecurityEventResponse> timeline = service.getTimeline(correlationId);

        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).correlationId()).isEqualTo(correlationId);
    }

    // SEC175-TC-003 — status transition happy path, audited
    @Test
    void reviewEvent_validStatus_updatesAndAudits() {
        SecurityIncidentServiceImpl service = newService();
        UUID reviewerId = UUID.randomUUID();
        SecurityEvent event = SecurityEvent.builder().id(5L).status("OPEN").occurredAt(Instant.now()).build();
        when(securityEventRepository.findById(5L)).thenReturn(Optional.of(event));

        SecurityEventResponse response = service.reviewEvent(5L, new ReviewSecurityEventRequest("RESOLVED"), reviewerId);

        verify(securityEventRepository).updateStatus(eq(5L), eq("RESOLVED"), eq(reviewerId), any());
        verify(auditService, times(1)).log(
                eq(AuditAction.SECURITY_EVENT_REVIEWED), eq(reviewerId), eq("SecurityEvent"), eq("5"), any());
        assertThat(response.status()).isEqualTo("RESOLVED");
    }

    @Test
    void reviewEvent_unknownEventId_throwsResourceNotFound() {
        SecurityIncidentServiceImpl service = newService();
        when(securityEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewEvent(999L, new ReviewSecurityEventRequest("RESOLVED"), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEvent_knownId_returnsDetail() {
        SecurityIncidentServiceImpl service = newService();
        SecurityEvent event = SecurityEvent.builder().id(7L).status("OPEN").occurredAt(Instant.now()).build();
        when(securityEventRepository.findById(7L)).thenReturn(Optional.of(event));

        assertThat(service.getEvent(7L).id()).isEqualTo(7L);
    }

    @Test
    void resolveEvent_appendsResolutionNote_updatesStatusAndAudits() {
        SecurityIncidentServiceImpl service = newService();
        UUID reviewerId = UUID.randomUUID();
        SecurityEvent event = SecurityEvent.builder().id(5L).status("UNDER_REVIEW").occurredAt(Instant.now()).build();
        when(securityEventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(securityEventNoteRepository.save(any(SecurityEventNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ResolveSecurityIncidentRequest request = new ResolveSecurityIncidentRequest(
                "MISCONFIGURATION", "Configuration corrected and access was verified safe.",
                "One administrator session", List.of("Revoked affected session"), false, true);

        SecurityEventResponse response = service.resolveEvent(5L, request, reviewerId);

        verify(securityEventNoteRepository).save(org.mockito.ArgumentMatchers.argThat(note ->
                note.getNoteText().contains("Root cause: MISCONFIGURATION")
                        && note.getNoteText().contains("Summary: Configuration corrected")));
        verify(securityEventRepository).updateStatus(eq(5L), eq("RESOLVED"), eq(reviewerId), any());
        verify(auditService).log(eq(AuditAction.SECURITY_EVENT_REVIEWED), eq(reviewerId),
                eq("SecurityEvent"), eq("5"), eq("RESOLVED"));
        assertThat(response.status()).isEqualTo("RESOLVED");
    }

    @Test
    void resolveEvent_closedIncident_isRejectedWithoutNewEvidence() {
        SecurityIncidentServiceImpl service = newService();
        when(securityEventRepository.findById(5L)).thenReturn(Optional.of(
                SecurityEvent.builder().id(5L).status("RESOLVED").build()));
        ResolveSecurityIncidentRequest request = new ResolveSecurityIncidentRequest(
                "MISCONFIGURATION", "Configuration corrected and access was verified safe.",
                "One administrator session", List.of("Revoked affected session"), false, true);

        assertThatThrownBy(() -> service.resolveEvent(5L, request, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(securityEventNoteRepository);
    }

    // SEC175-TC-002
    @Test
    void addNote_happyPath_savesAndAudits() {
        SecurityIncidentServiceImpl service = newService();
        UUID authorId = UUID.randomUUID();
        when(securityEventRepository.existsById(5L)).thenReturn(true);
        when(securityEventNoteRepository.save(any(SecurityEventNote.class))).thenAnswer(inv -> {
            SecurityEventNote n = inv.getArgument(0);
            n.setNoteId(UUID.randomUUID());
            n.setCreatedAt(Instant.now());
            return n;
        });

        var response = service.addNote(5L, new AddSecurityNoteRequest("Investigated, looks benign"), authorId);

        assertThat(response.noteText()).isEqualTo("Investigated, looks benign");
        verify(auditService, times(1)).log(
                eq(AuditAction.SECURITY_NOTE_ADDED), eq(authorId), eq("SecurityEvent"), eq("5"), any());
    }

    @Test
    void addNote_unknownEventId_throwsResourceNotFound() {
        SecurityIncidentServiceImpl service = newService();
        when(securityEventRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.addNote(999L, new AddSecurityNoteRequest("x"), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
