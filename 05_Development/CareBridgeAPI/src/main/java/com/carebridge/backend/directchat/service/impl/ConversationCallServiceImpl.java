package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.ConversationCallResponse;
import com.carebridge.backend.directchat.entity.CallStatus;
import com.carebridge.backend.directchat.entity.CallType;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.service.IConversationCallService;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.integration.zegocloud.ZegoTokenDto;
import com.carebridge.backend.integration.zegocloud.ZegoTokenGenerationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationCallServiceImpl implements IConversationCallService {

    private final DirectConversationRepository conversationRepository;
    private final ConversationCallRepository callRepository;
    private final IDirectConversationPolicy policy;
    private final IZegoCloudService zegoCloudService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public ConversationCallServiceImpl(
            DirectConversationRepository conversationRepository,
            ConversationCallRepository callRepository,
            IDirectConversationPolicy policy,
            IZegoCloudService zegoCloudService,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService) {
        this(conversationRepository, callRepository, policy, zegoCloudService, eventPublisher, auditService,
                Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time/duration calculations. */
    public ConversationCallServiceImpl(
            DirectConversationRepository conversationRepository,
            ConversationCallRepository callRepository,
            IDirectConversationPolicy policy,
            IZegoCloudService zegoCloudService,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.callRepository = callRepository;
        this.policy = policy;
        this.zegoCloudService = zegoCloudService;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ConversationCallResponse initiateCall(UUID conversationId, UUID callerUserId, CallType type) {
        DirectConversation conversation = loadConversation(conversationId);
        policy.assertIsParticipant(callerUserId, conversation);
        policy.assertConversationWritable(conversation);

        UUID callId = UUID.randomUUID();
        String zegoRoomId = callId.toString();
        ZegoTokenDto token = generateToken(zegoRoomId, callerUserId);

        Instant now = Instant.now(clock);
        ConversationCall call = ConversationCall.builder()
                .id(callId)
                .conversationId(conversationId)
                .initiatedByUserId(callerUserId)
                .callType(type)
                .callStatus(CallStatus.INITIATED)
                .zegoRoomId(zegoRoomId)
                .initiatedAt(now)
                .createdAt(now)
                .build();
        callRepository.save(call);

        conversationRepository.touchActivity(conversationId, now);
        auditService.log(AuditAction.DIRECT_CALL_INITIATED, callerUserId, "CONVERSATION_CALL", callId.toString(), Map.of());
        eventPublisher.publishEvent(new ConversationEventDomainEvent("CALL_INITIATED", conversationId, callerUserId, callId, now));

        return toResponse(call, token);
    }

    @Override
    @Transactional
    public ConversationCallResponse markRinging(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        requireCallee(call, conversation, currentUserId);
        policy.assertIsParticipant(currentUserId, conversation);
        policy.assertConversationWritable(conversation);

        if (callRepository.conditionallyMarkRinging(callId) != 1) {
            throw DirectChatException.invalidCallTransition();
        }
        call.setCallStatus(CallStatus.RINGING);

        touchAndPublish(conversation, currentUserId, callId, "CALL_STATE_CHANGED", AuditAction.DIRECT_CALL_STATE_CHANGED);
        return toResponse(call, null);
    }

    @Override
    @Transactional
    public ConversationCallResponse answer(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        requireCallee(call, conversation, currentUserId);
        policy.assertIsParticipant(currentUserId, conversation);
        policy.assertConversationWritable(conversation);

        Instant now = Instant.now(clock);
        // ADR-DCC-005: conditional UPDATE — the sole race oracle against CallTimeoutReconciliationJob.
        int affected = callRepository.conditionallyAnswer(callId, now);
        if (affected == 0) {
            throw DirectChatException.invalidCallTransition();
        }
        call.setCallStatus(CallStatus.ANSWERED);
        call.setAnsweredAt(now);

        ZegoTokenDto token = generateToken(call.getZegoRoomId(), currentUserId);
        conversationRepository.touchActivity(conversation.getId(), now);
        auditService.log(AuditAction.DIRECT_CALL_STATE_CHANGED, currentUserId, "CONVERSATION_CALL", callId.toString(), Map.of());
        eventPublisher.publishEvent(new ConversationEventDomainEvent("CALL_STATE_CHANGED", conversation.getId(), currentUserId, callId, now));

        return toResponse(call, token);
    }

    @Override
    @Transactional
    public ConversationCallResponse decline(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        requireCallee(call, conversation, currentUserId);
        policy.assertIsParticipant(currentUserId, conversation);
        policy.assertConversationWritable(conversation);

        Instant now = Instant.now(clock);
        if (callRepository.conditionallyDecline(callId, now) != 1) {
            throw DirectChatException.invalidCallTransition();
        }
        call.setCallStatus(CallStatus.DECLINED);
        call.setEndedAt(now);

        touchAndPublish(conversation, currentUserId, callId, "CALL_STATE_CHANGED", AuditAction.DIRECT_CALL_STATE_CHANGED);
        return toResponse(call, null);
    }

    @Override
    @Transactional
    public ConversationCallResponse end(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        UUID caller = call.getInitiatedByUserId();
        UUID callee = resolveCallee(conversation, caller);

        CallStatus status = call.getCallStatus();
        boolean answered = status == CallStatus.ANSWERED;
        boolean cancellable = status == CallStatus.INITIATED || status == CallStatus.RINGING;
        if (!answered && !cancellable) {
            throw DirectChatException.invalidCallTransition();
        }

        if (answered) {
            // ADR-DCC-007 §2: closing an already-connected call is exempt from the write-block —
            // either party may end it, and assertConversationWritable() is intentionally skipped.
            if (!currentUserId.equals(caller) && !currentUserId.equals(callee)) {
                throw DirectChatException.wrongCallActor();
            }
            policy.assertIsParticipant(currentUserId, conversation);
        } else {
            // Cancelling before the call ever connected is a new-communication attempt, not a
            // cleanup — only the caller may cancel, and the write-block still applies.
            if (!currentUserId.equals(caller)) {
                throw DirectChatException.wrongCallActor();
            }
            policy.assertIsParticipant(currentUserId, conversation);
            policy.assertConversationWritable(conversation);
        }

        Instant now = Instant.now(clock);
        if (answered) {
            int durationSeconds = Math.max(0, Math.toIntExact(Duration.between(call.getAnsweredAt(), now).getSeconds()));
            if (callRepository.conditionallyEndAnswered(callId, now, durationSeconds) != 1) {
                throw DirectChatException.invalidCallTransition();
            }
            call.setCallStatus(CallStatus.ENDED);
            call.setDurationSeconds(durationSeconds);
        } else {
            if (callRepository.conditionallyCancel(callId, now) != 1) {
                throw DirectChatException.invalidCallTransition();
            }
            call.setCallStatus(CallStatus.CANCELLED);
        }
        call.setEndedAt(now);

        conversationRepository.touchActivity(conversation.getId(), now);
        auditService.log(AuditAction.DIRECT_CALL_STATE_CHANGED, currentUserId, "CONVERSATION_CALL", callId.toString(), Map.of());
        eventPublisher.publishEvent(new ConversationEventDomainEvent("CALL_STATE_CHANGED", conversation.getId(), currentUserId, callId, now));

        return toResponse(call, null);
    }

    private void requireCallee(ConversationCall call, DirectConversation conversation, UUID currentUserId) {
        UUID callee = resolveCallee(conversation, call.getInitiatedByUserId());
        if (!currentUserId.equals(callee)) {
            throw DirectChatException.wrongCallActor();
        }
    }

    private static UUID resolveCallee(DirectConversation conversation, UUID callerUserId) {
        return callerUserId.equals(conversation.getMotherUserId())
                ? conversation.getExpertUserId()
                : conversation.getMotherUserId();
    }

    private void touchAndPublish(DirectConversation conversation, UUID actorUserId, UUID callId, String eventType, AuditAction action) {
        Instant now = Instant.now(clock);
        conversationRepository.touchActivity(conversation.getId(), now);
        auditService.log(action, actorUserId, "CONVERSATION_CALL", callId.toString(), Map.of());
        eventPublisher.publishEvent(new ConversationEventDomainEvent(eventType, conversation.getId(), actorUserId, callId, now));
    }

    private ZegoTokenDto generateToken(String roomId, UUID userId) {
        try {
            return zegoCloudService.generateToken(roomId, userId.toString(), userId.toString());
        } catch (ZegoTokenGenerationException ex) {
            throw DirectChatException.zegoTokenFailure();
        }
    }

    private DirectConversation loadConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId).orElseThrow(DirectChatException::conversationNotFound);
    }

    private ConversationCall loadCall(UUID callId) {
        return callRepository.findById(callId).orElseThrow(DirectChatException::callNotFound);
    }

    private DirectConversation loadCallConversation(UUID requestedConversationId, ConversationCall call) {
        if (!requestedConversationId.equals(call.getConversationId())) {
            throw DirectChatException.callNotFound();
        }
        return loadConversation(requestedConversationId);
    }

    private static ConversationCallResponse toResponse(ConversationCall call, ZegoTokenDto token) {
        return ConversationCallResponse.builder()
                .callId(call.getId())
                .conversationId(call.getConversationId())
                .initiatedByUserId(call.getInitiatedByUserId())
                .callType(call.getCallType().name())
                .callStatus(call.getCallStatus().name())
                .initiatedAt(call.getInitiatedAt())
                .answeredAt(call.getAnsweredAt())
                .endedAt(call.getEndedAt())
                .durationSeconds(call.getDurationSeconds())
                .zegoRoomId(call.getZegoRoomId())
                .zegoToken(token == null ? null : token.getToken())
                .zegoAppId(token == null ? null : token.getAppId())
                .tokenExpiresAt(token == null ? null : token.getExpiresAt())
                .build();
    }
}
