package com.carebridge.backend.consultation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.policy.ConsultationRequestPolicy;
import com.carebridge.backend.consultation.repository.ConsultationRequestRepository;
import com.carebridge.backend.consultation.repository.ConsultationRequestWriter;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultationRequestServiceImplListTest {

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
                Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC),
                48);
    }

    @Test
    void motherListBatchLoadsExpertProfilesAndCounterpartUsersWithoutNPlusOne() {
        UUID motherId = UUID.randomUUID();
        // Canonical consolidation: ExpertProfile is mapped onto users, so profile id == user id.
        UUID expertA = UUID.randomUUID();
        UUID expertB = UUID.randomUUID();
        UUID profileA = expertA;
        UUID profileB = expertB;
        var pageable = PageRequest.of(0, 20);
        when(repository.findByRequesterUserId(motherId, pageable))
                .thenReturn(new PageImpl<>(
                        List.of(
                                request(motherId, profileA, "Nutrition"),
                                request(motherId, profileB, "Postpartum")),
                        pageable,
                        2));
        when(expertProfileRepository.findAllById(any()))
                .thenReturn(List.of(
                        ExpertProfile.builder().userId(expertA).build(),
                        ExpertProfile.builder().userId(expertB).build()));
        when(userRepository.findAllById(any())).thenReturn(List.of(
                User.builder().id(expertA).name("Expert A").build(),
                User.builder().id(expertB).name("Expert B").build()));

        var result = service.listMine(motherId, null, pageable);

        assertThat(result.getContent())
                .extracting(response -> response.getCounterpartDisplayName())
                .containsExactly("Expert A", "Expert B");
        verify(expertProfileRepository).findAllById(any());
        verify(expertProfileRepository, never()).findById(any());
        verify(userRepository).findAllById(any());
    }

    @Test
    void assignedListBatchLoadsMotherCounterpartsWithoutNPlusOne() {
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = expertUserId;
        UUID motherA = UUID.randomUUID();
        UUID motherB = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        when(expertProfileRepository.findByUserId(expertUserId))
                .thenReturn(java.util.Optional.of(ExpertProfile.builder()
                        .userId(expertUserId)
                        .build()));
        when(repository.findByExpertProfileId(expertProfileId, pageable))
                .thenReturn(new PageImpl<>(
                        List.of(
                                request(motherA, expertProfileId, "Nutrition"),
                                request(motherB, expertProfileId, "Postpartum")),
                        pageable,
                        2));
        when(userRepository.findAllById(any())).thenReturn(List.of(
                User.builder().id(motherA).name("Mother A").build(),
                User.builder().id(motherB).name("Mother B").build()));

        var result = service.listAssigned(expertUserId, null, pageable);

        assertThat(result.getContent())
                .extracting(response -> response.getCounterpartDisplayName())
                .containsExactly("Mother A", "Mother B");
        verify(expertProfileRepository).findByUserId(expertUserId);
        verify(userRepository).findAllById(any());
        verify(userRepository, never()).findById(any());
    }

    private static ConsultationRequest request(
            UUID motherId, UUID expertProfileId, String topic) {
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        return ConsultationRequest.builder()
                .id(UUID.randomUUID())
                .requesterUserId(motherId)
                .expertProfileId(expertProfileId)
                .clientRequestId(UUID.randomUUID())
                .topic(topic)
                .description("Description")
                .status(ConsultationRequestStatus.PENDING)
                .expiresAt(now.plusSeconds(3600))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
