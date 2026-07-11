package com.carebridge.backend.journey;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.impl.JourneyServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyServiceImplTest {

    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @InjectMocks private JourneyServiceImpl journeyService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private CreateJourneyRequest makeRequest() {
        CreateJourneyRequest req = new CreateJourneyRequest();
        req.setJourneyType(JourneyType.PREGNANCY);
        req.setStartDate(LocalDate.now());
        req.setEstimatedDueDate(LocalDate.now().plusMonths(9));
        return req;
    }

    private MotherJourney savedJourney(UUID id, JourneyType type) {
        return MotherJourney.builder()
                .id(id)
                .ownerUserId(CALLER_ID)
                .journeyType(type)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.now())
                .build();
    }

    private void givenMotherUser() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(User.builder()
                .id(CALLER_ID)
                .role(Role.MOTHER)
                .build()));
    }

    // JOURNEY-TC-001: Happy path — PREGNANCY journey created, status=ACTIVE
    @Test
    void createJourney_validRequest_returns201WithActiveStatus() {
        givenMotherUser();
        when(journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(
                CALLER_ID, JourneyType.PREGNANCY, JourneyStatus.ACTIVE)).thenReturn(false);
        UUID newId = UUID.randomUUID();
        when(journeyRepository.saveAndFlush(any())).thenReturn(savedJourney(newId, JourneyType.PREGNANCY));

        CreateJourneyResponse resp = journeyService.createJourney(makeRequest(), CALLER_ID);

        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getStatus()).isEqualTo("ACTIVE");
        assertThat(resp.getJourneyType()).isEqualTo("PREGNANCY");
    }

    // JOURNEY-TC-002: Duplicate active journey → JOURNEY-002 / 409
    @Test
    void createJourney_duplicateActiveJourney_throwsBusinessException409() {
        givenMotherUser();
        when(journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(
                CALLER_ID, JourneyType.PREGNANCY, JourneyStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> journeyService.createJourney(makeRequest(), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("JOURNEY-002");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(journeyRepository, never()).save(any());
    }

    // JOURNEY-TC-003: Audit event emitted on success
    @Test
    void createJourney_success_auditEventEmitted() {
        givenMotherUser();
        when(journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenReturn(savedJourney(UUID.randomUUID(), JourneyType.PREGNANCY));

        journeyService.createJourney(makeRequest(), CALLER_ID);

        verify(auditService).log(eq(AuditAction.JOURNEY_CREATED), eq(CALLER_ID),
                eq("MotherJourney"), any(), any());
    }

    // JOURNEY-TC-004: accountId from JWT never from body (ownerUserId = callerId)
    @Test
    void createJourney_ownerUserIdSetToCallerId() {
        givenMotherUser();
        when(journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenReturn(savedJourney(UUID.randomUUID(), JourneyType.PREGNANCY));

        journeyService.createJourney(makeRequest(), CALLER_ID);

        verify(journeyRepository).saveAndFlush(argThat(j -> j.getOwnerUserId().equals(CALLER_ID)));
    }

    @Test
    void createJourney_unassignedRole_assignsMotherBeforeCreatingJourney() {
        User unassigned = User.builder().id(CALLER_ID).build();
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(unassigned));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(any(), any(), any())).thenReturn(false);
        when(journeyRepository.saveAndFlush(any())).thenReturn(savedJourney(UUID.randomUUID(), JourneyType.PREGNANCY));

        journeyService.createJourney(makeRequest(), CALLER_ID);

        verify(userRepository).save(argThat(user -> user.getRole() == Role.MOTHER));
    }
}
