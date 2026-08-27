package com.carebridge.backend.consultation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.event.ConsultationRequestDomainEvent;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.policy.ConsultationRequestPolicy;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.consultation.repository.ConsultationRequestWriter;
import com.carebridge.backend.consultation.service.CreateConsultationRequestResult;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ConsultationRequestServiceImplCreateTest {

    private static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");

    @Mock private ConsultationRequestRepository repository;
    @Mock private ConsultationRequestWriter writer;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private ExpertAvailabilityRepository expertAvailabilityRepository;
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
                expertAvailabilityRepository,
                userRepository,
                policy,
                directConversationService,
                eventPublisher,
                auditService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                48);
    }

    @Test
    void createsNewPendingRequestUnderExpertRowLock() {
        CreateConsultationRequestRequest request = request(UUID.randomUUID(), "Nutrition");
        ConsultationRequest created = pending(request, UUID.randomUUID());
        ExpertProfile expert = eligibleExpert();
        when(repository.findByRequesterUserIdAndClientRequestId(
                        MOTHER_ID, request.getClientRequestId()))
                .thenReturn(Optional.empty(), Optional.empty());
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(expert));
        User account = eligibleExpertAccount();
        when(userRepository.findByIdForUpdate(EXPERT_USER_ID))
                .thenReturn(Optional.of(account));
        when(writer.insertIfAbsent(any()))
                .thenReturn(new ConsultationRequestWriter.InsertResult(created.getId(), true));
        when(repository.findById(created.getId())).thenReturn(Optional.of(created));

        CreateConsultationRequestResult result = service.create(request, MOTHER_ID);

        assertThat(result.created()).isTrue();
        assertThat(result.response().getId()).isEqualTo(created.getId());
        assertThat(result.response().getStatus()).isEqualTo("PENDING");
        assertThat(result.response().getExpiresAt()).isEqualTo(NOW.plusSeconds(48L * 3600));
        verify(expertProfileRepository).findByIdForUpdate(EXPERT_PROFILE_ID);
        verify(userRepository).findByIdForUpdate(EXPERT_USER_ID);
        verify(policy).assertExpertEligibleForConsultation(expert, account, NOW);
        verify(writer).insertIfAbsent(any());
        verify(eventPublisher).publishEvent(any(ConsultationRequestDomainEvent.class));
    }

    @Test
    void returnsExistingSamePayloadBeforeExpertLockEvenAfterTrustLoss() {
        UUID key = UUID.randomUUID();
        CreateConsultationRequestRequest request = request(key, "Nutrition");
        ConsultationRequest existing = pending(request, UUID.randomUUID());
        when(repository.findByRequesterUserIdAndClientRequestId(MOTHER_ID, key))
                .thenReturn(Optional.of(existing));

        CreateConsultationRequestResult result = service.create(request, MOTHER_ID);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getId()).isEqualTo(existing.getId());
        verify(expertProfileRepository, never()).findByIdForUpdate(any());
        verify(writer, never()).insertIfAbsent(any());
        verify(eventPublisher, never()).publishEvent(any(ConsultationRequestDomainEvent.class));
        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        UUID key = UUID.randomUUID();
        CreateConsultationRequestRequest request = request(key, "Different topic");
        ConsultationRequest existing = pending(request(key, "Original topic"), UUID.randomUUID());
        when(repository.findByRequesterUserIdAndClientRequestId(MOTHER_ID, key))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(request, MOTHER_ID))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("CONREQ-009"));

        verify(expertProfileRepository, never()).findByIdForUpdate(any());
        verify(writer, never()).insertIfAbsent(any());
    }

    @Test
    void missingExpertFailsWithoutInsertOrEvent() {
        CreateConsultationRequestRequest request = request(UUID.randomUUID(), "Nutrition");
        when(repository.findByRequesterUserIdAndClientRequestId(
                        MOTHER_ID, request.getClientRequestId()))
                .thenReturn(Optional.empty());
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, MOTHER_ID))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("CONREQ-006"));

        verify(writer, never()).insertIfAbsent(any());
        verify(eventPublisher, never()).publishEvent(any(ConsultationRequestDomainEvent.class));
    }

    @Test
    void acceptsExactFutureAvailabilityAndRejectsAStaleSelection() {
        Instant start = NOW.plusSeconds(3600);
        CreateConsultationRequestRequest valid = request(UUID.randomUUID(), "Nutrition");
        valid.setPreferredWindowStart(start);
        valid.setPreferredWindowEnd(start.plusSeconds(3600));
        when(repository.findByRequesterUserIdAndClientRequestId(MOTHER_ID, valid.getClientRequestId()))
                .thenReturn(Optional.empty(), Optional.empty());
        when(expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID))
                .thenReturn(Optional.of(eligibleExpert()));
        when(userRepository.findByIdForUpdate(EXPERT_USER_ID))
                .thenReturn(Optional.of(eligibleExpertAccount()));
        when(expertAvailabilityRepository.existsByExpertProfileIdAndStartAtAndEndAtAndStatus(
                EXPERT_PROFILE_ID, start, start.plusSeconds(3600), AvailabilityStatus.AVAILABLE))
                .thenReturn(false);

        assertThatThrownBy(() -> service.create(valid, MOTHER_ID))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CONREQ-010"));

        verify(writer, never()).insertIfAbsent(any());
    }

    private static CreateConsultationRequestRequest request(UUID key, String topic) {
        return CreateConsultationRequestRequest.builder()
                .clientRequestId(key)
                .expertProfileId(EXPERT_PROFILE_ID)
                .topic(topic)
                .description("Please advise on a feeding schedule.")
                .build();
    }

    private static ConsultationRequest pending(
            CreateConsultationRequestRequest request, UUID id) {
        return ConsultationRequest.builder()
                .id(id)
                .requesterUserId(MOTHER_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .clientRequestId(request.getClientRequestId())
                .topic(request.getTopic())
                .description(request.getDescription())
                .status(ConsultationRequestStatus.PENDING)
                .expiresAt(NOW.plusSeconds(48L * 3600))
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static ExpertProfile eligibleExpert() {
        return ExpertProfile.builder()
                .expertProfileId(EXPERT_PROFILE_ID)
                .userId(EXPERT_USER_ID)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

    private static User eligibleExpertAccount() {
        return User.builder()
                .id(EXPERT_USER_ID)
                .enabled(true)
                .locked(false)
                .build();
    }
}
