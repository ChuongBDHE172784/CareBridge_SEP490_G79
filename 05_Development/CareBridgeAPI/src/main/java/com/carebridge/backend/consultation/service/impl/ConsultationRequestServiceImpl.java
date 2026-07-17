package com.carebridge.backend.consultation.service.impl;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestPendingSummaryResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestSummaryResponse;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.event.ConsultationRequestDomainEvent;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.policy.ConsultationRequestPolicy;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.consultation.repository.ConsultationRequestWriter;
import com.carebridge.backend.consultation.service.CreateConsultationRequestResult;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConsultationRequestServiceImpl implements IConsultationRequestService {

    private final ConsultationRequestRepository repository;
    private final ConsultationRequestWriter writer;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final ConsultationRequestPolicy policy;
    private final IDirectConversationService directConversationService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Clock clock;
    private final int expiryHours;

    @Autowired
    public ConsultationRequestServiceImpl(
            ConsultationRequestRepository repository,
            ConsultationRequestWriter writer,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            ConsultationRequestPolicy policy,
            IDirectConversationService directConversationService,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService,
            @Value("${carebridge.consultation-request.expiry-hours:48}") int expiryHours) {
        this(
                repository,
                writer,
                expertProfileRepository,
                userRepository,
                policy,
                directConversationService,
                eventPublisher,
                auditService,
                Clock.systemUTC(),
                expiryHours);
    }

    public ConsultationRequestServiceImpl(
            ConsultationRequestRepository repository,
            ConsultationRequestWriter writer,
            ExpertProfileRepository expertProfileRepository,
            UserRepository userRepository,
            ConsultationRequestPolicy policy,
            IDirectConversationService directConversationService,
            ApplicationEventPublisher eventPublisher,
            AuditService auditService,
            Clock clock,
            int expiryHours) {
        this.repository = repository;
        this.writer = writer;
        this.expertProfileRepository = expertProfileRepository;
        this.userRepository = userRepository;
        this.policy = policy;
        this.directConversationService = directConversationService;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.clock = clock;
        this.expiryHours = expiryHours;
    }

    @Override
    public CreateConsultationRequestResult create(
            CreateConsultationRequestRequest request, UUID requesterUserId) {
        Optional<ConsultationRequest> existing =
                repository.findByRequesterUserIdAndClientRequestId(
                        requesterUserId, request.getClientRequestId());
        if (existing.isPresent()) {
            assertSameIdempotentPayload(existing.get(), request);
            return new CreateConsultationRequestResult(
                    toResponse(existing.get(), requesterUserId), false);
        }

        ExpertProfile lockedExpert = expertProfileRepository
                .findByIdForUpdate(request.getExpertProfileId())
                .orElseThrow(ConsultationRequestException::expertNotFound);

        existing = repository.findByRequesterUserIdAndClientRequestId(
                requesterUserId, request.getClientRequestId());
        if (existing.isPresent()) {
            assertSameIdempotentPayload(existing.get(), request);
            return new CreateConsultationRequestResult(
                    toResponse(existing.get(), requesterUserId), false);
        }

        policy.assertExpertEligibleForConsultation(lockedExpert);
        Instant now = clock.instant();
        ConsultationRequest candidate = ConsultationRequest.builder()
                .id(UUID.randomUUID())
                .requesterUserId(requesterUserId)
                .expertProfileId(request.getExpertProfileId())
                .clientRequestId(request.getClientRequestId())
                .topic(request.getTopic())
                .description(request.getDescription())
                .preferredWindowStart(request.getPreferredWindowStart())
                .preferredWindowEnd(request.getPreferredWindowEnd())
                .status(ConsultationRequestStatus.PENDING)
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .createdAt(now)
                .updatedAt(now)
                .build();

        ConsultationRequestWriter.InsertResult inserted = writer.insertIfAbsent(candidate);
        if (!inserted.created()) {
            ConsultationRequest winner = repository
                    .findByRequesterUserIdAndClientRequestId(
                            requesterUserId, request.getClientRequestId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency conflict winner was not visible"));
            assertSameIdempotentPayload(winner, request);
            return new CreateConsultationRequestResult(
                    toResponse(winner, requesterUserId), false);
        }

        ConsultationRequest created = repository.findById(inserted.requestId())
                .orElse(candidate);
        publish("REQUEST_CREATED", created.getId(), requesterUserId, "USER");
        audit("REQUEST_CREATED", created.getId(), requesterUserId);
        return new CreateConsultationRequestResult(
                toResponse(created, requesterUserId), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationRequestSummaryResponse> listMine(
            UUID requesterUserId, ConsultationRequestStatus status, Pageable pageable) {
        Page<ConsultationRequest> requests = status == null
                ? repository.findByRequesterUserId(requesterUserId, pageable)
                : repository.findByRequesterUserIdAndStatus(requesterUserId, status, pageable);
        return mapSummaries(requests, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationRequestSummaryResponse> listAssigned(
            UUID expertUserId, ConsultationRequestStatus status, Pageable pageable) {
        ExpertProfile expert = expertProfileRepository.findByUserId(expertUserId)
                .orElseThrow(ConsultationRequestException::expertNotFound);
        Page<ConsultationRequest> requests = status == null
                ? repository.findByExpertProfileId(expert.getExpertProfileId(), pageable)
                : repository.findByExpertProfileIdAndStatus(
                        expert.getExpertProfileId(), status, pageable);
        return mapSummaries(requests, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationRequestResponse getById(UUID id, UUID currentUserId) {
        ConsultationRequest request = findRequest(id);
        UUID assignedExpertUserId = assignedExpertUserId(request);
        policy.assertCanView(request, currentUserId, assignedExpertUserId);
        return toResponse(request, currentUserId);
    }

    @Override
    public ConsultationRequestResponse accept(UUID id, UUID expertUserId) {
        ConsultationRequest request = findRequest(id);
        ExpertProfile lockedExpert = expertProfileRepository
                .findByIdForUpdate(request.getExpertProfileId())
                .orElseThrow(ConsultationRequestException::notFound);
        policy.assertCanRespond(request, expertUserId, lockedExpert.getUserId());
        if (!lockedExpert.isEligibleForConsultation()) {
            throw ConsultationRequestException.expertNoLongerEligible();
        }

        UUID conversationId = directConversationService
                .findOrCreate(request.getRequesterUserId(), request.getExpertProfileId())
                .conversation()
                .getConversationId();
        Instant now = clock.instant();
        requireTransition(repository.tryTransition(
                id,
                ConsultationRequestStatus.ACCEPTED,
                now,
                expertUserId,
                null,
                conversationId));
        ConsultationRequest updated = findRequest(id);
        publish("REQUEST_ACCEPTED", id, expertUserId, "USER");
        audit("REQUEST_ACCEPTED", id, expertUserId);
        return toResponse(updated, expertUserId);
    }

    @Override
    public ConsultationRequestResponse reject(UUID id, UUID expertUserId, String reason) {
        ConsultationRequest request = findRequest(id);
        policy.assertCanRespond(request, expertUserId, assignedExpertUserId(request));
        Instant now = clock.instant();
        requireTransition(repository.tryTransition(
                id,
                ConsultationRequestStatus.REJECTED,
                now,
                expertUserId,
                normalizeOptional(reason),
                null));
        ConsultationRequest updated = findRequest(id);
        publish("REQUEST_REJECTED", id, expertUserId, "USER");
        audit("REQUEST_REJECTED", id, expertUserId);
        return toResponse(updated, expertUserId);
    }

    @Override
    public ConsultationRequestResponse cancel(UUID id, UUID requesterUserId) {
        ConsultationRequest request = findRequest(id);
        policy.assertCanCancel(request, requesterUserId);
        Instant now = clock.instant();
        requireTransition(repository.tryTransition(
                id,
                ConsultationRequestStatus.CANCELLED,
                now,
                requesterUserId,
                null,
                null));
        ConsultationRequest updated = findRequest(id);
        publish("REQUEST_CANCELLED", id, requesterUserId, "USER");
        audit("REQUEST_CANCELLED", id, requesterUserId);
        return toResponse(updated, requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationRequestPendingSummaryResponse pendingSummary(UUID expertUserId) {
        ExpertProfile expert = expertProfileRepository.findByUserId(expertUserId)
                .orElseThrow(ConsultationRequestException::expertNotFound);
        return new ConsultationRequestPendingSummaryResponse(
                repository.countByExpertProfileIdAndStatus(
                        expert.getExpertProfileId(), ConsultationRequestStatus.PENDING));
    }

    @Override
    public int expireOverdueRequests() {
        Instant now = clock.instant();
        int transitioned = 0;
        for (UUID id : repository.findExpiredIds(now, PageRequest.of(0, 100))) {
            int updated = repository.tryTransition(
                    id,
                    ConsultationRequestStatus.EXPIRED,
                    now,
                    null,
                    null,
                    null);
            if (updated == 1) {
                transitioned++;
                publish("REQUEST_EXPIRED", id, null, "SYSTEM");
                audit("REQUEST_EXPIRED", id, null);
            }
        }
        return transitioned;
    }

    private ConsultationRequest findRequest(UUID id) {
        return repository.findById(id).orElseThrow(ConsultationRequestException::notFound);
    }

    private UUID assignedExpertUserId(ConsultationRequest request) {
        return repository.findAssignedExpertUserId(request.getExpertProfileId())
                .orElseThrow(ConsultationRequestException::notFound);
    }

    private ConsultationRequestResponse toResponse(
            ConsultationRequest request, UUID currentUserId) {
        UUID counterpartId;
        if (currentUserId.equals(request.getRequesterUserId())) {
            counterpartId = repository.findAssignedExpertUserId(request.getExpertProfileId())
                    .orElse(null);
        } else {
            counterpartId = request.getRequesterUserId();
        }
        User counterpart = counterpartId == null
                ? null
                : userRepository.findById(counterpartId).orElse(null);
        return ConsultationRequestResponse.builder()
                .id(request.getId())
                .expertProfileId(request.getExpertProfileId())
                .counterpartDisplayName(counterpart == null ? null : counterpart.getName())
                .counterpartAvatarUrl(counterpart == null ? null : counterpart.getAvatarUrl())
                .topic(request.getTopic())
                .description(request.getDescription())
                .preferredWindowStart(request.getPreferredWindowStart())
                .preferredWindowEnd(request.getPreferredWindowEnd())
                .status(request.getStatus().name())
                .rejectReason(request.getRejectReason())
                .directConversationId(request.getDirectConversationId())
                .respondedAt(request.getRespondedAt())
                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private Page<ConsultationRequestSummaryResponse> mapSummaries(
            Page<ConsultationRequest> requests, boolean motherView) {
        Map<UUID, UUID> counterpartIds = new HashMap<>();
        if (motherView) {
            Set<UUID> expertProfileIds = requests.getContent().stream()
                    .map(ConsultationRequest::getExpertProfileId)
                    .collect(Collectors.toSet());
            Map<UUID, UUID> expertUserIds = expertProfileRepository
                    .findAllById(expertProfileIds)
                    .stream()
                    .collect(Collectors.toMap(
                            ExpertProfile::getExpertProfileId,
                            ExpertProfile::getUserId));
            for (ConsultationRequest request : requests.getContent()) {
                counterpartIds.put(
                        request.getId(), expertUserIds.get(request.getExpertProfileId()));
            }
        } else {
            for (ConsultationRequest request : requests.getContent()) {
                counterpartIds.put(request.getId(), request.getRequesterUserId());
            }
        }
        Set<UUID> userIds = counterpartIds.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return requests.map(request -> {
            User counterpart = users.get(counterpartIds.get(request.getId()));
            return new ConsultationRequestSummaryResponse(
                    request.getId(),
                    counterpart == null ? null : counterpart.getName(),
                    request.getTopic(),
                    request.getStatus().name(),
                    request.getCreatedAt());
        });
    }

    private void assertSameIdempotentPayload(
            ConsultationRequest existing, CreateConsultationRequestRequest request) {
        if (!Objects.equals(existing.getExpertProfileId(), request.getExpertProfileId())
                || !Objects.equals(existing.getTopic(), request.getTopic())
                || !Objects.equals(existing.getDescription(), request.getDescription())
                || !Objects.equals(
                        existing.getPreferredWindowStart(), request.getPreferredWindowStart())
                || !Objects.equals(
                        existing.getPreferredWindowEnd(), request.getPreferredWindowEnd())) {
            throw ConsultationRequestException.idempotencyConflict();
        }
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void requireTransition(int updated) {
        if (updated != 1) {
            throw ConsultationRequestException.invalidTransition();
        }
    }

    private void publish(String eventType, UUID requestId, UUID actorUserId, String actorType) {
        eventPublisher.publishEvent(new ConsultationRequestDomainEvent(
                eventType, requestId, actorUserId, actorType, clock.instant()));
    }

    private void audit(String eventType, UUID requestId, UUID actorUserId) {
        auditService.log(
                AuditAction.MODERATION_ACTION,
                actorUserId,
                "CONSULTATION_REQUEST",
                requestId.toString(),
                Map.of("eventType", eventType));
    }
}
