package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.dto.response.UnreadSummaryResponse;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository.LastMessageRow;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository.ReadCursor;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.directchat.service.FindOrCreateConversationResult;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectConversationServiceImpl implements IDirectConversationService {

    private static final int PREVIEW_MAX_LENGTH = 120;

    private final DirectConversationRepository conversationRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final DirectMessageRepository messageRepository;
    private final ConversationSummaryAggregateRepository aggregateRepository;
    private final IDirectConversationPolicy policy;
    private final DirectConversationWriter writer;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public DirectConversationServiceImpl(
            DirectConversationRepository conversationRepository,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            DirectMessageRepository messageRepository,
            ConversationSummaryAggregateRepository aggregateRepository,
            IDirectConversationPolicy policy,
            DirectConversationWriter writer,
            AuditService auditService) {
        this(conversationRepository, expertProfileRepository, userRepository, messageRepository, aggregateRepository,
                policy, writer, auditService, Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time calculations. */
    public DirectConversationServiceImpl(
            DirectConversationRepository conversationRepository,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            DirectMessageRepository messageRepository,
            ConversationSummaryAggregateRepository aggregateRepository,
            IDirectConversationPolicy policy,
            DirectConversationWriter writer,
            AuditService auditService,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.expertProfileRepository = expertProfileRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.aggregateRepository = aggregateRepository;
        this.policy = policy;
        this.writer = writer;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FindOrCreateConversationResult findOrCreate(UUID motherUserId, UUID expertProfileId) {
        ExpertProfile expertProfile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(DirectChatException::expertProfileNotFound);
        // BR-DCC-003: gate creating NEW conversation intent. Existing conversations remain
        // reachable via GET regardless of current Expert status (ADR-DCC-007 — reads are open).
        policy.assertExpertVerified(expertProfile);
        UUID expertUserId = expertProfile.getUserId();

        Optional<DirectConversation> existing =
                conversationRepository.findByMotherUserIdAndExpertUserId(motherUserId, expertUserId);
        if (existing.isPresent()) {
            return new FindOrCreateConversationResult(toResponse(existing.get(), true), false);
        }

        DirectConversation toInsert = DirectConversation.builder()
                .id(UUID.randomUUID())
                .motherUserId(motherUserId)
                .expertUserId(expertUserId)
                .status("ACTIVE")
                .createdAt(Instant.now(clock))
                .build();
        if (writer.insertIfAbsent(toInsert)) {
            auditService.log(AuditAction.DIRECT_CONVERSATION_OPENED, motherUserId,
                    "DIRECT_CONVERSATION", toInsert.getId().toString(), Map.of());
            return new FindOrCreateConversationResult(toResponse(toInsert, true), true);
        }
        DirectConversation winner = conversationRepository
                .findByMotherUserIdAndExpertUserId(motherUserId, expertUserId)
                .orElseThrow(DirectChatException::conversationNotFound);
        return new FindOrCreateConversationResult(toResponse(winner, true), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectConversationSummaryResponse> listMyConversations(UUID currentUserId) {
        // ADR-MEDI-002 mục 6 — exactly 5 fixed queries below, regardless of conversation count (C1).
        List<DirectConversation> conversations = conversationRepository
                .findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(currentUserId, currentUserId); // 1/5
        if (conversations.isEmpty()) {
            return List.of();
        }

        Set<UUID> counterpartUserIds = new HashSet<>();
        Set<UUID> expertUserIds = new HashSet<>(); // every conversation's OWN expert, not just counterparts
        Set<UUID> conversationIds = new HashSet<>();
        for (DirectConversation c : conversations) {
            boolean viewerIsMother = currentUserId.equals(c.getMotherUserId());
            counterpartUserIds.add(viewerIsMother ? c.getExpertUserId() : c.getMotherUserId());
            expertUserIds.add(c.getExpertUserId());
            conversationIds.add(c.getId());
        }

        Map<UUID, User> usersById = userRepository.findAllById(counterpartUserIds).stream() // 2/5
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<UUID, ExpertProfile> expertProfilesByUserId = expertProfileRepository.findByUserIdIn(expertUserIds).stream() // 3/5
                .collect(Collectors.toMap(ExpertProfile::getUserId, ep -> ep));
        List<UUID> conversationIdList = List.copyOf(conversationIds);
        Map<UUID, LastMessageRow> lastMessages = aggregateRepository.fetchLastMessages(conversationIdList); // 4/5
        Map<UUID, Integer> unreadCounts = aggregateRepository.fetchUnreadCounts(conversationIdList, currentUserId); // 5/5

        return conversations.stream()
                .map(c -> toSummary(c, currentUserId, usersById, expertProfilesByUserId, lastMessages, unreadCounts))
                .toList();
    }

    private DirectConversationSummaryResponse toSummary(
            DirectConversation conversation,
            UUID currentUserId,
            Map<UUID, User> usersById,
            Map<UUID, ExpertProfile> expertProfilesByUserId,
            Map<UUID, LastMessageRow> lastMessages,
            Map<UUID, Integer> unreadCounts) {
        boolean viewerIsMother = currentUserId.equals(conversation.getMotherUserId());
        UUID counterpartUserId = viewerIsMother ? conversation.getExpertUserId() : conversation.getMotherUserId();
        String counterpartRole = viewerIsMother ? "EXPERT" : "MOTHER";
        User counterpartUser = usersById.get(counterpartUserId);
        ExpertProfile conversationExpert = expertProfilesByUserId.get(conversation.getExpertUserId());
        String counterpartSpecialty = "EXPERT".equals(counterpartRole) && conversationExpert != null
                ? conversationExpert.getSpecialty() : null;
        LastMessageRow lastMessage = lastMessages.get(conversation.getId());
        int unreadCount = unreadCounts.getOrDefault(conversation.getId(), 0);
        boolean expertAvailable = conversationExpert != null
                && conversationExpert.getVerificationStatus() == VerificationStatus.APPROVED;

        return DirectConversationSummaryResponse.builder()
                .conversationId(conversation.getId())
                .counterpartUserId(counterpartUserId)
                .counterpartRole(counterpartRole)
                .lastActivityAt(conversation.getLastActivityAt())
                .expertAvailable(expertAvailable)
                .counterpartDisplayName(counterpartUser != null ? counterpartUser.getName() : null)
                .counterpartAvatarUrl(counterpartUser != null ? counterpartUser.getAvatarUrl() : null)
                .counterpartSpecialty(counterpartSpecialty)
                .lastMessagePreview(lastMessage != null ? truncate(lastMessage.messageBody()) : null)
                .lastMessageAt(lastMessage != null ? lastMessage.createdAt() : null)
                .unreadCount(unreadCount)
                .conversationStatus(conversation.getStatus())
                .build();
    }

    private static String truncate(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= PREVIEW_MAX_LENGTH ? body : body.substring(0, PREVIEW_MAX_LENGTH);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectConversationResponse getConversation(UUID conversationId, UUID currentUserId) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(currentUserId, conversation);
        return toResponse(conversation, isExpertAvailable(conversation.getExpertUserId()));
    }

    @Override
    @Transactional
    public ReadCursor markRead(UUID conversationId, UUID currentUserId, UUID lastSeenMessageId) {
        // ADR-MEDI-003 mục 3 — fixed order: conversation lookup, then participant check, then
        // lastSeenMessageId ownership check. Each step only runs if the previous one passed, so a
        // caller always gets the failure that actually explains their request.
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(currentUserId, conversation);
        // assertConversationWritable is intentionally NOT called here (C6) — marking history as
        // read must still work after the Expert loses APPROVED (ADR-DCC-007 mục 3).
        DirectMessage resolvedMessage = messageRepository.findByIdAndConversationId(lastSeenMessageId, conversationId)
                .orElseThrow(DirectChatException::messageNotInConversation);

        boolean mother = currentUserId.equals(conversation.getMotherUserId());
        return aggregateRepository.advanceReadCursor(conversationId, currentUserId, mother,
                resolvedMessage.getCreatedAt(), resolvedMessage.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadSummaryResponse getUnreadSummary(UUID currentUserId) {
        List<DirectConversation> conversations =
                conversationRepository.findByMotherUserIdOrExpertUserId(currentUserId, currentUserId);
        if (conversations.isEmpty()) {
            return new UnreadSummaryResponse(0, 0);
        }
        List<UUID> conversationIds = conversations.stream().map(DirectConversation::getId).toList();
        Map<UUID, Integer> unreadCounts = aggregateRepository.fetchUnreadCounts(conversationIds, currentUserId);
        int unreadConversationCount = (int) unreadCounts.values().stream().filter(count -> count > 0).count();
        int totalUnreadMessageCount = unreadCounts.values().stream().mapToInt(Integer::intValue).sum();
        return new UnreadSummaryResponse(unreadConversationCount, totalUnreadMessageCount);
    }

    private DirectConversationResponse toResponse(DirectConversation conversation, boolean expertAvailable) {
        return new DirectConversationResponse(
                conversation.getId(),
                conversation.getMotherUserId(),
                conversation.getExpertUserId(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getLastActivityAt(),
                expertAvailable);
    }

    private boolean isExpertAvailable(UUID expertUserId) {
        return expertProfileRepository.findByUserId(expertUserId)
                .map(ep -> ep.getVerificationStatus() == VerificationStatus.APPROVED)
                .orElse(false);
    }
}
