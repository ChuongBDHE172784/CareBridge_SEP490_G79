package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.ConversationCallResponse;
import com.carebridge.backend.directchat.dto.response.ZegoJoinCredentialsResponse;
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
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final IZegoCloudService zegoCloudService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public ConversationCallServiceImpl(
            DirectConversationRepository conversationRepository,
            ConversationCallRepository callRepository,
            IDirectConversationPolicy policy,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            IZegoCloudService zegoCloudService,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService) {
        this(conversationRepository, callRepository, policy, expertProfileRepository,
                userRepository, zegoCloudService, eventPublisher, auditService,
                Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time/duration calculations. */
    public ConversationCallServiceImpl(
            DirectConversationRepository conversationRepository,
            ConversationCallRepository callRepository,
            IDirectConversationPolicy policy,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            IZegoCloudService zegoCloudService,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.callRepository = callRepository;
        this.policy = policy;
        this.expertProfileRepository = expertProfileRepository;
        this.userRepository = userRepository;
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
        assertWritableUnderLock(conversation);

        UUID callId = UUID.randomUUID();
        String zegoRoomId = "cb_" + callId.toString().replace("-", "");

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

        return toResponse(call);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationCallResponse getCall(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        assertReadableParticipant(call, conversation, currentUserId);
        return toResponse(call);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationCallResponse> listActiveCalls(UUID currentUserId) {
        return callRepository.findActiveForParticipant(currentUserId).stream()
                .filter(call -> canReadActiveCall(call, currentUserId))
                .map(ConversationCallServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ConversationCallResponse markRinging(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        requireCallee(call, conversation, currentUserId);
        policy.assertIsParticipant(currentUserId, conversation);
        assertWritableUnderLock(conversation);

        if (callRepository.conditionallyMarkRinging(callId) != 1) {
            throw DirectChatException.invalidCallTransition();
        }
        call.setCallStatus(CallStatus.RINGING);

        touchAndPublish(conversation, currentUserId, callId, "CALL_STATE_CHANGED", AuditAction.DIRECT_CALL_STATE_CHANGED);
        return toResponse(call);
    }

    @Override
    @Transactional
    public ConversationCallResponse answer(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        requireCallee(call, conversation, currentUserId);
        policy.assertIsParticipant(currentUserId, conversation);
        assertWritableUnderLock(conversation);

        Instant now = Instant.now(clock);
        // ADR-DCC-005: conditional UPDATE — the sole race oracle against CallTimeoutReconciliationJob.
        int affected = callRepository.conditionallyAnswer(callId, now);
        if (affected == 0) {
            throw DirectChatException.invalidCallTransition();
        }
        call.setCallStatus(CallStatus.ANSWERED);
        call.setAnsweredAt(now);

        conversationRepository.touchActivity(conversation.getId(), now);
        auditService.log(AuditAction.DIRECT_CALL_STATE_CHANGED, currentUserId, "CONVERSATION_CALL", callId.toString(), Map.of());
        eventPublisher.publishEvent(new ConversationEventDomainEvent("CALL_STATE_CHANGED", conversation.getId(), currentUserId, callId, now));

        return toResponse(call);
    }

    @Override
    @Transactional
    public ConversationCallResponse decline(UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        requireCallee(call, conversation, currentUserId);
        policy.assertIsParticipant(currentUserId, conversation);
        assertWritableUnderLock(conversation);

        Instant now = Instant.now(clock);
        if (callRepository.conditionallyDecline(callId, now) != 1) {
            throw DirectChatException.invalidCallTransition();
        }
        call.setCallStatus(CallStatus.DECLINED);
        call.setEndedAt(now);

        touchAndPublish(conversation, currentUserId, callId, "CALL_STATE_CHANGED", AuditAction.DIRECT_CALL_STATE_CHANGED);
        return toResponse(call);
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
        } else {
            // Cancelling before the call ever connected is a new-communication attempt, not a
            // cleanup — only the caller may cancel, and the write-block still applies.
            if (!currentUserId.equals(caller)) {
                throw DirectChatException.wrongCallActor();
            }
            policy.assertIsParticipant(currentUserId, conversation);
            assertWritableUnderLock(conversation);
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

        return toResponse(call);
    }

    @Override
    @Transactional
    public ZegoJoinCredentialsResponse issueJoinCredentials(
            UUID conversationId, UUID callId, UUID currentUserId) {
        ConversationCall call = loadCall(callId);
        DirectConversation conversation = loadCallConversation(conversationId, call);
        assertIdentityParticipant(currentUserId, conversation);
        if (call.getCallStatus() != CallStatus.ANSWERED) {
            throw DirectChatException.invalidCallTransition();
        }
        assertWritableUnderLock(conversation);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(DirectChatException::notParticipant);
        String zegoUserId = toZegoUserId(currentUserId);
        String displayName = safeDisplayName(user);
        ZegoTokenDto token = generateToken(call.getZegoRoomId(), zegoUserId, displayName);

        auditService.log(
                AuditAction.DIRECT_CALL_STATE_CHANGED,
                currentUserId,
                "CONVERSATION_CALL",
                callId.toString(),
                Map.of("operation", "ZEGO_JOIN_CREDENTIALS_ISSUED"));

        return ZegoJoinCredentialsResponse.builder()
                .appId(token.getAppId())
                .roomId(call.getZegoRoomId())
                .userId(zegoUserId)
                .displayName(displayName)
                .token(token.getToken())
                .expiresAt(token.getExpiresAt())
                .build();
    }

    private void requireCallee(ConversationCall call, DirectConversation conversation, UUID currentUserId) {
        UUID callee = resolveCallee(conversation, call.getInitiatedByUserId());
        if (!currentUserId.equals(callee)) {
            throw DirectChatException.wrongCallActor();
        }
    }

    private void assertWritableUnderLock(DirectConversation conversation) {
        ExpertProfile lockedExpert = expertProfileRepository
                .findByUserIdForUpdate(conversation.getExpertUserId())
                .orElseThrow(DirectChatException::expertUnavailableForWrite);
        policy.assertConversationWritable(lockedExpert);
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

    private boolean canReadActiveCall(ConversationCall call, UUID currentUserId) {
        DirectConversation conversation = conversationRepository.findById(call.getConversationId()).orElse(null);
        if (conversation == null
                || (!currentUserId.equals(conversation.getMotherUserId())
                    && !currentUserId.equals(conversation.getExpertUserId()))) {
            return false;
        }
        if (currentUserId.equals(conversation.getMotherUserId())
                || call.getCallStatus() == CallStatus.ANSWERED) {
            return true;
        }
        return expertProfileRepository.findByUserId(currentUserId)
                .map(ExpertProfile::isEligibleForConsultation)
                .orElse(false);
    }

    private void assertReadableParticipant(
            ConversationCall call, DirectConversation conversation, UUID currentUserId) {
        assertIdentityParticipant(currentUserId, conversation);
        if (currentUserId.equals(conversation.getExpertUserId())
                && call.getCallStatus() != CallStatus.ANSWERED) {
            policy.assertIsParticipant(currentUserId, conversation);
        }
    }

    private static void assertIdentityParticipant(UUID currentUserId, DirectConversation conversation) {
        if (!currentUserId.equals(conversation.getMotherUserId())
                && !currentUserId.equals(conversation.getExpertUserId())) {
            throw DirectChatException.notParticipant();
        }
    }

    private ZegoTokenDto generateToken(String roomId, String userId, String displayName) {
        try {
            return zegoCloudService.generateToken(roomId, userId, displayName);
        } catch (ZegoTokenGenerationException ex) {
            throw DirectChatException.zegoTokenFailure();
        }
    }

    private static String toZegoUserId(UUID userId) {
        return "u_" + userId.toString().replace("-", "");
    }

    private static String safeDisplayName(User user) {
        String rawName = user.getName();
        if (rawName == null || rawName.isBlank()) {
            return "CareBridge User";
        }
        String sanitized = rawName.replaceAll("\\p{Cntrl}", "").trim();
        if (sanitized.isBlank()) {
            return "CareBridge User";
        }
        return sanitized.length() <= 64 ? sanitized : sanitized.substring(0, 64);
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

    private static ConversationCallResponse toResponse(ConversationCall call) {
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
                .build();
    }
}
