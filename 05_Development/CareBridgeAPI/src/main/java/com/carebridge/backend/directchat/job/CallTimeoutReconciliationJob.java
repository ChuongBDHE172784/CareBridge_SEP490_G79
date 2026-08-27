package com.carebridge.backend.directchat.job;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** ADR-DCC-005: only place MISSED is ever set (BR-DCC-011) — no client endpoint sets it. */
@Component
public class CallTimeoutReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(CallTimeoutReconciliationJob.class);

    private final ConversationCallRepository callRepository;
    private final DirectConversationRepository conversationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;

    @Value("${carebridge.directchat.call-ring-timeout-seconds:45}")
    private long timeoutSeconds;

    @Autowired
    public CallTimeoutReconciliationJob(ConversationCallRepository callRepository,
            DirectConversationRepository conversationRepository, ApplicationEventPublisher eventPublisher,
            AuditService auditService) {
        this(callRepository, conversationRepository, eventPublisher, auditService, Clock.systemDefaultZone());
    }

    /** Test constructor. */
    public CallTimeoutReconciliationJob(ConversationCallRepository callRepository,
            DirectConversationRepository conversationRepository, ApplicationEventPublisher eventPublisher,
            AuditService auditService, Clock clock) {
        this.callRepository = callRepository;
        this.conversationRepository = conversationRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void reconcileMissedCalls() {
        Instant cutoff = Instant.now(clock).minusSeconds(timeoutSeconds);
        for (ConversationCall call : callRepository.findRingingCallsInitiatedBefore(cutoff)) {
            // Conditional UPDATE — if this returns 0, a concurrent PATCH /answer already won
            // the race (ADR-DCC-005); silently skip, no error.
            Instant now = Instant.now(clock);
            int affected = callRepository.conditionallyMarkMissed(call.getId(), now);
            if (affected != 1) {
                continue;
            }
            conversationRepository.touchActivity(call.getConversationId(), now);
            auditService.log(AuditAction.DIRECT_CALL_MISSED_BY_TIMEOUT, call.getInitiatedByUserId(),
                    "CONVERSATION_CALL", call.getId().toString(), Map.of());
            eventPublisher.publishEvent(new ConversationEventDomainEvent(
                    "CALL_STATE_CHANGED", call.getConversationId(), call.getInitiatedByUserId(), call.getId(), now));
            log.info("Call {} marked MISSED by timeout reconciliation", call.getId());
        }
    }
}
