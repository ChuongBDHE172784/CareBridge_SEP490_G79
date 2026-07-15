package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.service.FindOrCreateConversationResult;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectConversationServiceImpl implements IDirectConversationService {

    private final DirectConversationRepository conversationRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final IDirectConversationPolicy policy;
    private final DirectConversationWriter writer;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public DirectConversationServiceImpl(
            DirectConversationRepository conversationRepository,
            ExpertProfileRepository expertProfileRepository,
            IDirectConversationPolicy policy,
            DirectConversationWriter writer,
            AuditService auditService) {
        this(conversationRepository, expertProfileRepository, policy, writer, auditService, Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time calculations. */
    public DirectConversationServiceImpl(
            DirectConversationRepository conversationRepository,
            ExpertProfileRepository expertProfileRepository,
            IDirectConversationPolicy policy,
            DirectConversationWriter writer,
            AuditService auditService,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.expertProfileRepository = expertProfileRepository;
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
    public List<DirectConversationSummaryResponse> listMyConversations(UUID currentUserId) {
        return conversationRepository.findByMotherUserIdOrExpertUserId(currentUserId, currentUserId).stream()
                .map(conversation -> {
                    boolean viewerIsMother = currentUserId.equals(conversation.getMotherUserId());
                    UUID counterpartUserId = viewerIsMother ? conversation.getExpertUserId() : conversation.getMotherUserId();
                    String counterpartRole = viewerIsMother ? "EXPERT" : "MOTHER";
                    return new DirectConversationSummaryResponse(
                            conversation.getId(),
                            counterpartUserId,
                            counterpartRole,
                            conversation.getLastActivityAt(),
                            isExpertAvailable(conversation.getExpertUserId()));
                })
                .toList();
    }

    @Override
    public DirectConversationResponse getConversation(UUID conversationId, UUID currentUserId) {
        DirectConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(DirectChatException::conversationNotFound);
        policy.assertIsParticipant(currentUserId, conversation);
        return toResponse(conversation, isExpertAvailable(conversation.getExpertUserId()));
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
