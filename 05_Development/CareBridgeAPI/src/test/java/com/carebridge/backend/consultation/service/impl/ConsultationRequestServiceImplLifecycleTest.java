package com.carebridge.backend.consultation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.event.ConsultationRequestDomainEvent;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.policy.ConsultationRequestPolicy;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.consultation.repository.ConsultationRequestWriter;
import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.service.FindOrCreateConversationResult;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ConsultationRequestServiceImplLifecycleTest {

    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    // Canonical consolidation: ExpertProfile is mapped onto the users table, so the expert
    // profile id and the expert user id are the same identifier.
    private static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID EXPERT_PROFILE_ID = EXPERT_USER_ID;
    private static final UUID CONVERSATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");

    @Mock private ConsultationRequestRepository repository;
    @Mock private ConsultationRequestWriter writer;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConsultationRequestPolicy policy;
    @Mock private IDirectConversationService directConversationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    private ConsultationRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultationRequestServiceImpl(
                repository,
                writer,
                expertProfileRepository,
                userRepository,
                policy,
                directConversationService,
                eventPublisher,
                auditService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                48);
    }

    @Test
    void acceptLocksEligibleExpertBeforeOpeningConversationAndTransitioning() {
        ConsultationRequest pending = request(ConsultationRequestStatus.PENDING, null);
        ConsultationRequest accepted = request(ConsultationRequestStatus.ACCEPTED, CONVERSATION_ID);
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending), Optional.of(accepted));
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(expert));
        User expertAccount = User.builder().id(EXPERT_USER_ID).enabled(true).locked(false).build();
        when(userRepository.findByIdForUpdate(EXPERT_USER_ID))
                .thenReturn(Optional.of(expertAccount));
        when(directConversationService.findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID))
                .thenReturn(new FindOrCreateConversationResult(
                        new DirectConversationResponse(
                                CONVERSATION_ID, MOTHER_ID, EXPERT_USER_ID,
                                "ACTIVE", NOW, NOW, true),
                        true));
        when(repository.tryTransition(
                        REQUEST_ID,
                        ConsultationRequestStatus.ACCEPTED,
                        NOW,
                        EXPERT_USER_ID,
                        null,
                        CONVERSATION_ID))
                .thenReturn(1);

        var response = service.accept(REQUEST_ID, EXPERT_USER_ID);

        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getDirectConversationId()).isEqualTo(CONVERSATION_ID);
        verify(expertProfileRepository).findByIdForUpdate(EXPERT_PROFILE_ID);
        verify(policy).assertExpertStillEligibleForConsultation(expert, expertAccount, NOW);
        verify(directConversationService).findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID);
        verify(eventPublisher).publishEvent(any(ConsultationRequestDomainEvent.class));
    }

    @Test
    void acceptStopsBeforeSideEffectsWhenLockedExpertIsIneligible() {
        ConsultationRequest pending = request(ConsultationRequestStatus.PENDING, null);
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.REVOKED);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(expert));

        assertThatThrownBy(() -> service.accept(REQUEST_ID, EXPERT_USER_ID))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("CONREQ-004"));

        verify(directConversationService, never()).findOrCreate(any(), any());
        verify(repository, never()).tryTransition(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(ConsultationRequestDomainEvent.class));
    }

    @Test
    void acceptStopsBeforeSideEffectsWhenExpertAccountIsDisabled() {
        ConsultationRequest pending = request(ConsultationRequestStatus.PENDING, null);
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        User disabledAccount = User.builder()
                .id(EXPERT_USER_ID)
                .enabled(false)
                .locked(false)
                .build();
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(expert));
        when(userRepository.findByIdForUpdate(EXPERT_USER_ID))
                .thenReturn(Optional.of(disabledAccount));
        doThrow(ConsultationRequestException.expertNoLongerEligible())
                .when(policy)
                .assertExpertStillEligibleForConsultation(expert, disabledAccount, NOW);

        assertThatThrownBy(() -> service.accept(REQUEST_ID, EXPERT_USER_ID))
                .isInstanceOfSatisfying(
                        ConsultationRequestException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("CONREQ-004"));

        verify(directConversationService, never()).findOrCreate(any(), any());
        verify(repository, never()).tryTransition(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(ConsultationRequestDomainEvent.class));
    }

    @Test
    void rejectAndCancelDoNotGateOnExpertEligibility() {
        ConsultationRequest pending = request(ConsultationRequestStatus.PENDING, null);
        ConsultationRequest rejected = request(ConsultationRequestStatus.REJECTED, null);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending), Optional.of(rejected));
        when(repository.findAssignedExpertUserId(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(EXPERT_USER_ID), Optional.empty());
        when(repository.tryTransition(
                        REQUEST_ID,
                        ConsultationRequestStatus.REJECTED,
                        NOW,
                        EXPERT_USER_ID,
                        "Outside my scope",
                        null))
                .thenReturn(1);

        assertThat(service.reject(REQUEST_ID, EXPERT_USER_ID, " Outside my scope ").getStatus())
                .isEqualTo("REJECTED");
        verify(expertProfileRepository, never()).findByIdForUpdate(any());

        ConsultationRequest cancelled = request(ConsultationRequestStatus.CANCELLED, null);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending), Optional.of(cancelled));
        when(repository.tryTransition(
                        REQUEST_ID,
                        ConsultationRequestStatus.CANCELLED,
                        NOW,
                        MOTHER_ID,
                        null,
                        null))
                .thenReturn(1);
        assertThat(service.cancel(REQUEST_ID, MOTHER_ID).getStatus()).isEqualTo("CANCELLED");
        verifyNoInteractions(expertProfileRepository);
    }

    @Test
    void transitionLoserGetsConreq005WithoutPublishing() {
        ConsultationRequest pending = request(ConsultationRequestStatus.PENDING, null);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
        when(repository.tryTransition(
                        eq(REQUEST_ID),
                        eq(ConsultationRequestStatus.CANCELLED),
                        eq(NOW),
                        eq(MOTHER_ID),
                        eq(null),
                        eq(null)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.cancel(REQUEST_ID, MOTHER_ID))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("CONREQ-005"));
        verify(eventPublisher, never()).publishEvent(any(ConsultationRequestDomainEvent.class));
    }

    @Test
    void expiryPublishesSystemEventOnlyForRowsThatTransitioned() {
        UUID lostRace = UUID.randomUUID();
        when(repository.findExpiredIds(eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(REQUEST_ID, lostRace));
        when(repository.tryTransition(
                        REQUEST_ID, ConsultationRequestStatus.EXPIRED, NOW, null, null, null))
                .thenReturn(1);
        when(repository.tryTransition(
                        lostRace, ConsultationRequestStatus.EXPIRED, NOW, null, null, null))
                .thenReturn(0);

        assertThat(service.expireOverdueRequests()).isEqualTo(1);

        ArgumentCaptor<ConsultationRequestDomainEvent> eventCaptor =
                ArgumentCaptor.forClass(ConsultationRequestDomainEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("REQUEST_EXPIRED");
        assertThat(eventCaptor.getValue().actorType()).isEqualTo("SYSTEM");
        assertThat(eventCaptor.getValue().actorUserId()).isNull();
        verifyNoInteractions(expertProfileRepository);
    }

    @Test
    void pendingSummaryUsesAssignedExpertProfile() {
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.REVOKED);
        when(expertProfileRepository.findByUserId(EXPERT_USER_ID))
                .thenReturn(Optional.of(expert));
        when(repository.countByExpertProfileIdAndStatus(
                        EXPERT_PROFILE_ID, ConsultationRequestStatus.PENDING))
                .thenReturn(3L);

        assertThat(service.pendingSummary(EXPERT_USER_ID).pendingCount()).isEqualTo(3);
        verify(expertProfileRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void getByIdReturnsRoleCorrectCounterpartForMotherAndExpert() {
        ConsultationRequest pending = request(ConsultationRequestStatus.PENDING, null);
        User mother = User.builder().id(MOTHER_ID).name("Mother A").avatarUrl("mother.png").build();
        User expert = User.builder()
                .id(EXPERT_USER_ID)
                .name("Expert B")
                .avatarUrl("expert.png")
                .build();
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
        when(repository.findAssignedExpertUserId(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(EXPERT_USER_ID));
        when(userRepository.findById(EXPERT_USER_ID)).thenReturn(Optional.of(expert));

        var motherView = service.getById(REQUEST_ID, MOTHER_ID);
        assertThat(motherView.getCounterpartDisplayName()).isEqualTo("Expert B");
        assertThat(motherView.getCounterpartAvatarUrl()).isEqualTo("expert.png");

        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
        when(userRepository.findById(MOTHER_ID)).thenReturn(Optional.of(mother));
        var expertView = service.getById(REQUEST_ID, EXPERT_USER_ID);
        assertThat(expertView.getCounterpartDisplayName()).isEqualTo("Mother A");
        assertThat(expertView.getCounterpartAvatarUrl()).isEqualTo("mother.png");
    }

    @Test
    void getByIdMissingRequestUsesConreq007() {
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(REQUEST_ID, MOTHER_ID))
                .isInstanceOfSatisfying(
                        ConsultationRequestException.class,
                        ex -> {
                            assertThat(ex.getCode()).isEqualTo("CONREQ-007");
                            assertThat(ex.getHttpStatus().value()).isEqualTo(404);
                        });
        verifyNoInteractions(policy, userRepository, expertProfileRepository);
    }

    private static ConsultationRequest request(
            ConsultationRequestStatus status, UUID conversationId) {
        return ConsultationRequest.builder()
                .id(REQUEST_ID)
                .requesterUserId(MOTHER_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .clientRequestId(UUID.randomUUID())
                .topic("Nutrition")
                .description("Description")
                .status(status)
                .directConversationId(conversationId)
                .respondedAt(status == ConsultationRequestStatus.PENDING ? null : NOW)
                .expiresAt(NOW.plusSeconds(3600))
                .createdAt(NOW.minusSeconds(3600))
                .updatedAt(NOW)
                .build();
    }

    private static ExpertProfile expert(
            VerificationStatus verificationStatus, TrustStatus trustStatus) {
        return ExpertProfile.builder()
                .userId(EXPERT_USER_ID)
                .verificationStatus(verificationStatus)
                .trustStatus(trustStatus)
                .build();
    }
}
