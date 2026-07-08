package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.dto.response.FamilyAlertDetailResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.impl.EmergencyService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyServiceTest {

    @Mock
    private IEmergencySessionRepository emergencySessionRepository;

    @Mock
    private IFamilyAlertLogRepository familyAlertLogRepository;

    @Mock
    private FamilyMemberPort familyMemberPort;

    @Mock
    private LocationConsentPort locationConsentPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EmergencyService emergencyService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");

    @Test
    void openFlow_noActiveSession_shouldCreateNew() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        EmergencySession saved = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.save(any())).thenReturn(saved);

        EmergencySessionResponse result = emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verify(emergencySessionRepository).save(any(EmergencySession.class));
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void openFlow_activeSessionExists_shouldReturnExisting() {
        // Idempotent — return existing ACTIVE session
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        EmergencySessionResponse result = emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verify(emergencySessionRepository, never()).save(any());
        assertThat(result.getSessionId()).isEqualTo(existing.getId());
    }

    @Test
    void openFlow_shouldPublishEmergencySessionOpenedEvent() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(emergencySessionRepository.save(any())).thenReturn(EmergencyTestFactory.makeActiveSession());

        emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        ArgumentCaptor<EmergencySessionOpened> captor = ArgumentCaptor.forClass(EmergencySessionOpened.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EmergencySessionOpened.class);
    }

    @Test
    void getAlertDetail_owner_shouldReturnDetailWithoutFamilyMembershipCheck() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().name("Mai").build()));
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());

        FamilyAlertDetailResponse result = emergencyService.getAlertDetail(session.getId(), USER_ID);

        assertThat(result.getMotherName()).isEqualTo("Mai");
        assertThat(result.getSessionId()).isEqualTo(session.getId());
        verify(familyMemberPort, never()).isFamilyMember(any(), any());
    }

    @Test
    void getAlertDetail_acceptedFamilyMember_shouldReturnDetail() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(familyMemberPort.isFamilyMember(USER_ID, FAMILY_ID)).thenReturn(true);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());

        FamilyAlertDetailResponse result = emergencyService.getAlertDetail(session.getId(), FAMILY_ID);

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isNull();
    }

    @Test
    void getAlertDetail_strangerNotInFamily_shouldThrowForbidden() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(familyMemberPort.isFamilyMember(USER_ID, STRANGER_ID)).thenReturn(false);

        assertThatThrownBy(() -> emergencyService.getAlertDetail(session.getId(), STRANGER_ID))
                .isInstanceOf(EmergencyException.class)
                .satisfies(e -> assertThat(((EmergencyException) e).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAlertDetail_noConsent_shouldHideLocation() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setUserLatitude(java.math.BigDecimal.valueOf(10.77));
        session.setUserLongitude(java.math.BigDecimal.valueOf(106.70));
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());

        FamilyAlertDetailResponse result = emergencyService.getAlertDetail(session.getId(), USER_ID);

        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
        assertThat(result.isLocationIncluded()).isFalse();
    }
}
