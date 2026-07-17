package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.dto.response.TimelineItemResponse;
import com.carebridge.backend.directchat.dto.response.TimelinePageResponse;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.entity.MessageType;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.mapper.TimelineCursorCodec;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.ConversationTimelineRepository;
import com.carebridge.backend.directchat.repository.ConversationTimelineRepository.TimelineRow;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.directchat.service.IDirectMessageService;
import com.carebridge.backend.directchat.service.SendDirectMessageResult;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectMessageServiceImpl implements IDirectMessageService {

    private static final int MAX_BODY_LENGTH = 2000;

    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final ConversationCallRepository callRepository;
    private final ConversationTimelineRepository timelineRepository;
    private final DirectMessageWriter messageWriter;
    private final IDirectConversationPolicy policy;
    private final ExpertProfileRepository expertProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public DirectMessageServiceImpl(
            DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            ConversationCallRepository callRepository,
            ConversationTimelineRepository timelineRepository,
            DirectMessageWriter messageWriter,
            IDirectConversationPolicy policy,
            ExpertProfileRepository expertProfileRepository,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService) {
        this(conversationRepository, messageRepository, callRepository, timelineRepository, messageWriter, policy,
                expertProfileRepository,
                eventPublisher, auditService, Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time calculations. */
    public DirectMessageServiceImpl(
            DirectConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            ConversationCallRepository callRepository,
            ConversationTimelineRepository timelineRepository,
            DirectMessageWriter messageWriter,
            IDirectConversationPolicy policy,
            ExpertProfileRepository expertProfileRepository,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.callRepository = callRepository;
        this.timelineRepository = timelineRepository;
        this.messageWriter = messageWriter;
        this.policy = policy;
        this.expertProfileRepository = expertProfileRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SendDirectMessageResult sendMessage(UUID conversationId, UUID senderUserId, SendDirectMessageRequest request) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(senderUserId, conversation);
        ExpertProfile lockedExpert = expertProfileRepository
                .findByUserIdForUpdate(conversation.getExpertUserId())
                .orElseThrow(DirectChatException::expertUnavailableForWrite);
        policy.assertConversationWritable(lockedExpert);

        String trimmedBody = request.getMessageBody() == null ? "" : request.getMessageBody().trim();
        if (trimmedBody.isEmpty() || trimmedBody.length() > MAX_BODY_LENGTH) {
            throw DirectChatException.invalidMessageBody();
        }

        Optional<DirectMessage> existing = messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                conversationId, senderUserId, request.getClientMessageId());
        if (existing.isPresent()) {
            assertSameIdempotentPayload(existing.get(), trimmedBody);
            return new SendDirectMessageResult(toItemResponse(existing.get()), false);
        }

        DirectMessage toInsert = DirectMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderUserId(senderUserId)
                .clientMessageId(request.getClientMessageId())
                .messageType(MessageType.TEXT)
                .messageBody(trimmedBody)
                .createdAt(Instant.now(clock))
                .build();

        boolean created = messageWriter.insertIfAbsent(toInsert);
        DirectMessage saved = toInsert;
        if (!created) {
            saved = messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                            conversationId, senderUserId, request.getClientMessageId())
                    .orElseThrow(DirectChatException::idempotencyConflict);
            assertSameIdempotentPayload(saved, trimmedBody);
        }

        if (created) {
            Instant now = Instant.now(clock);
            conversationRepository.touchActivity(conversationId, now); // BR-DCC-014
            auditService.log(AuditAction.DIRECT_MESSAGE_SENT, senderUserId, "DIRECT_MESSAGE", saved.getId().toString(), Map.of());
            eventPublisher.publishEvent(new ConversationEventDomainEvent(
                    "MESSAGE_SENT", conversationId, senderUserId, saved.getId(), now));
        }

        return new SendDirectMessageResult(toItemResponse(saved), created);
    }

    @Override
    @Transactional(readOnly = true)
    public TimelinePageResponse getTimeline(UUID conversationId, UUID currentUserId, String after, String before, int limit) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(currentUserId, conversation);

        int boundedLimit = Math.max(1, Math.min(limit, 100));

        if (after != null && before != null) {
            throw DirectChatException.conflictingCursors();
        }

        if (after != null) {
            TimelineCursorCodec.DecodedCursor cursor = decodeCursor(after);
            List<TimelineRow> rows = timelineRepository.fetchAfter(
                    conversationId, cursor.sortTs(), cursor.kind(), cursor.resourceId(), boundedLimit + 1);
            boolean hasMoreNewer = rows.size() > boundedLimit;
            List<TimelineRow> page = hasMoreNewer ? rows.subList(0, boundedLimit) : rows;
            return buildPage(page, hasMoreNewer, true);
        }

        if (before != null) {
            TimelineCursorCodec.DecodedCursor cursor = decodeCursor(before);
            List<TimelineRow> rowsDesc = timelineRepository.fetchBefore(
                    conversationId, cursor.sortTs(), cursor.kind(), cursor.resourceId(), boundedLimit + 1);
            boolean hasMoreOlder = rowsDesc.size() > boundedLimit;
            List<TimelineRow> trimmedDesc = hasMoreOlder ? rowsDesc.subList(0, boundedLimit) : rowsDesc;
            List<TimelineRow> ascending = new ArrayList<>(trimmedDesc);
            java.util.Collections.reverse(ascending);
            return buildPage(ascending, true, hasMoreOlder);
        }

        // No cursor — reopen conversation: latest page.
        List<TimelineRow> rowsDesc = timelineRepository.fetchLatest(conversationId, boundedLimit + 1);
        boolean hasMoreOlder = rowsDesc.size() > boundedLimit;
        List<TimelineRow> trimmedDesc = hasMoreOlder ? rowsDesc.subList(0, boundedLimit) : rowsDesc;
        List<TimelineRow> ascending = new ArrayList<>(trimmedDesc);
        java.util.Collections.reverse(ascending);
        return buildPage(ascending, false, hasMoreOlder);
    }

    private static TimelineCursorCodec.DecodedCursor decodeCursor(String cursor) {
        try {
            return TimelineCursorCodec.decode(cursor);
        } catch (IllegalArgumentException ex) {
            throw DirectChatException.invalidCursor();
        }
    }

    private static void assertSameIdempotentPayload(DirectMessage existing, String requestedBody) {
        if (existing.getMessageType() != MessageType.TEXT || !existing.getMessageBody().equals(requestedBody)) {
            throw DirectChatException.idempotencyConflict();
        }
    }

    private TimelinePageResponse buildPage(List<TimelineRow> ascendingRows, boolean hasMoreNewer, boolean hasMoreOlder) {
        List<TimelineItemResponse> items = hydrate(ascendingRows);
        String nextCursor = ascendingRows.isEmpty() ? null : encodeCursor(ascendingRows.get(ascendingRows.size() - 1));
        String previousCursor = ascendingRows.isEmpty() ? null : encodeCursor(ascendingRows.get(0));
        return new TimelinePageResponse(items, nextCursor, hasMoreNewer, previousCursor, hasMoreOlder);
    }

    private static String encodeCursor(TimelineRow row) {
        return TimelineCursorCodec.encode(row.sortTs(), row.kind(), row.resourceId());
    }

    private List<TimelineItemResponse> hydrate(List<TimelineRow> rows) {
        List<UUID> messageIds = rows.stream().filter(r -> "MESSAGE".equals(r.kind())).map(TimelineRow::resourceId).toList();
        List<UUID> callIds = rows.stream().filter(r -> "CALL_EVENT".equals(r.kind())).map(TimelineRow::resourceId).toList();

        Map<UUID, DirectMessage> messagesById = new HashMap<>();
        if (!messageIds.isEmpty()) {
            messageRepository.findAllById(messageIds).forEach(m -> messagesById.put(m.getId(), m));
        }
        Map<UUID, ConversationCall> callsById = new HashMap<>();
        if (!callIds.isEmpty()) {
            callRepository.findAllById(callIds).forEach(c -> callsById.put(c.getId(), c));
        }

        List<TimelineItemResponse> items = new ArrayList<>();
        for (TimelineRow row : rows) {
            if ("MESSAGE".equals(row.kind())) {
                DirectMessage m = messagesById.get(row.resourceId());
                if (m != null) {
                    items.add(toItemResponse(m));
                }
            } else {
                ConversationCall c = callsById.get(row.resourceId());
                if (c != null) {
                    items.add(toItemResponse(c));
                }
            }
        }
        return items;
    }

    private TimelineItemResponse toItemResponse(DirectMessage message) {
        return TimelineItemResponse.builder()
                .kind("MESSAGE")
                .messageId(message.getId())
                .clientMessageId(message.getClientMessageId())
                .senderUserId(message.getSenderUserId())
                .messageType(message.getMessageType().name())
                .messageBody(message.getMessageBody())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private TimelineItemResponse toItemResponse(ConversationCall call) {
        return TimelineItemResponse.builder()
                .kind("CALL_EVENT")
                .callId(call.getId())
                .callType(call.getCallType().name())
                .callStatus(call.getCallStatus().name())
                .initiatedByUserId(call.getInitiatedByUserId())
                .durationSeconds(call.getDurationSeconds())
                .initiatedAt(call.getInitiatedAt())
                .answeredAt(call.getAnsweredAt())
                .endedAt(call.getEndedAt())
                .build();
    }
}
