package com.carebridge.backend.directchat.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.entity.CallStatus;
import com.carebridge.backend.directchat.entity.CallType;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CallTimeoutReconciliationJobTest {

    @Mock private ConversationCallRepository callRepository;
    @Mock private DirectConversationRepository conversationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    private final Instant now = Instant.parse("2026-07-15T08:00:00Z");
    private CallTimeoutReconciliationJob job;

    @BeforeEach
    void setUp() {
        job = new CallTimeoutReconciliationJob(callRepository, conversationRepository, eventPublisher,
                auditService, Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(job, "timeoutSeconds", 45L);
    }

    @Test
    void reconcileMissedCalls_conditionalUpdateWins_touchesActivityAuditsAndPublishes() {
        ConversationCall call = timedOutCall();
        when(callRepository.findRingingCallsInitiatedBefore(now.minusSeconds(45))).thenReturn(List.of(call));
        when(callRepository.conditionallyMarkMissed(call.getId(), now)).thenReturn(1);

        job.reconcileMissedCalls();

        verify(conversationRepository).touchActivity(call.getConversationId(), now);
        verify(auditService).log(AuditAction.DIRECT_CALL_MISSED_BY_TIMEOUT, call.getInitiatedByUserId(),
                "CONVERSATION_CALL", call.getId().toString(), java.util.Map.of());
        ArgumentCaptor<ConversationEventDomainEvent> event =
                ArgumentCaptor.forClass(ConversationEventDomainEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo("CALL_STATE_CHANGED");
        assertThat(event.getValue().resourceId()).isEqualTo(call.getId());
    }

    @Test
    void reconcileMissedCalls_answerWinsRace_doesNotEmitSideEffects() {
        ConversationCall call = timedOutCall();
        when(callRepository.findRingingCallsInitiatedBefore(now.minusSeconds(45))).thenReturn(List.of(call));
        when(callRepository.conditionallyMarkMissed(call.getId(), now)).thenReturn(0);

        job.reconcileMissedCalls();

        verify(conversationRepository, never()).touchActivity(call.getConversationId(), now);
        verify(auditService, never()).log(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private ConversationCall timedOutCall() {
        return ConversationCall.builder()
                .id(UUID.randomUUID())
                .conversationId(UUID.randomUUID())
                .initiatedByUserId(UUID.randomUUID())
                .callType(CallType.VIDEO)
                .callStatus(CallStatus.RINGING)
                .zegoRoomId(UUID.randomUUID().toString())
                .initiatedAt(now.minusSeconds(46))
                .createdAt(now.minusSeconds(46))
                .build();
    }
}
