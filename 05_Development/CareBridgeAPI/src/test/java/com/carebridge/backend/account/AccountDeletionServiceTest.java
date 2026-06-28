package com.carebridge.backend.account;

import com.carebridge.backend.account.dto.request.RequestAccountDeletionRequest;
import com.carebridge.backend.account.dto.response.AccountDeletionRequestResponse;
import com.carebridge.backend.account.entity.AccountDeletionRequest;
import com.carebridge.backend.account.entity.DeletionStatus;
import com.carebridge.backend.account.repository.AccountDeletionRequestRepository;
import com.carebridge.backend.account.service.AccountDeletionService;
import com.carebridge.backend.account.service.impl.AccountDeletionServiceImpl;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.AuthorizationException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountDeletionServiceTest {

    private AccountDeletionService deletionService;
    private AccountDeletionRequestRepository deletionRequestRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        deletionRequestRepository = mock(AccountDeletionRequestRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditService = mock(AuditService.class);
        deletionService = new AccountDeletionServiceImpl(
                deletionRequestRepository, userRepository, passwordEncoder, auditService);
    }

    private User buildUser(UUID userId, Role role) {
        return User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(role)
                .enabled(true)
                .accountStatus("ACTIVE")
                .build();
    }

    private RequestAccountDeletionRequest buildRequest(String password, String reason) {
        RequestAccountDeletionRequest req = new RequestAccountDeletionRequest();
        req.setConfirmPassword(password);
        req.setReason(reason);
        return req;
    }

    @Test
    @DisplayName("DEL-TC-001: requestDeletion success for regular user")
    void requestDeletion_validUser_createsRequest() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Role.MOTHER);

        AccountDeletionRequest saved = AccountDeletionRequest.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(DeletionStatus.PENDING)
                .reason("I want to leave")
                .requestedAt(Instant.now())
                .scheduledFor(Instant.now().plusSeconds(30 * 86400L))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PENDING)).thenReturn(false);
        when(passwordEncoder.matches("correctpassword", "$2a$12$hashedpassword")).thenReturn(true);
        when(deletionRequestRepository.save(any(AccountDeletionRequest.class))).thenReturn(saved);

        AccountDeletionRequestResponse response = deletionService.requestDeletion(
                userId, buildRequest("correctpassword", "I want to leave"));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(DeletionStatus.PENDING);
        assertThat(response.getScheduledFor()).isNotNull();
        assertThat(response.getScheduledFor()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("DEL-TC-002: SYSTEM_ADMIN blocked from deletion")
    void requestDeletion_systemAdmin_throwsAuthorizationException() {
        UUID userId = UUID.randomUUID();
        User admin = buildUser(userId, Role.SYSTEM_ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> deletionService.requestDeletion(userId, buildRequest("pass", null)))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("SYSTEM_ADMIN");
    }

    @Test
    @DisplayName("DEL-TC-003: Already has pending request throws ValidationException")
    void requestDeletion_pendingExists_throwsValidationException() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Role.MOTHER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> deletionService.requestDeletion(userId, buildRequest("pass", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DEL-001");
    }

    @Test
    @DisplayName("DEL-TC-004: Wrong password throws AuthenticationException")
    void requestDeletion_wrongPassword_throwsAuthenticationException() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Role.MOTHER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PENDING)).thenReturn(false);
        when(passwordEncoder.matches("wrong", "$2a$12$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> deletionService.requestDeletion(userId, buildRequest("wrong", null)))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("DEL-002");
    }

    @Test
    @DisplayName("DEL-TC-005: cancelDeletion cancels pending request")
    void cancelDeletion_pendingExists_cancels() {
        UUID userId = UUID.randomUUID();
        AccountDeletionRequest pending = AccountDeletionRequest.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(DeletionStatus.PENDING)
                .requestedAt(Instant.now().minusSeconds(3600))
                .scheduledFor(Instant.now().plusSeconds(30 * 86400L - 3600))
                .build();

        AccountDeletionRequest cancelled = AccountDeletionRequest.builder()
                .id(pending.getId())
                .userId(userId)
                .status(DeletionStatus.CANCELLED)
                .requestedAt(pending.getRequestedAt())
                .scheduledFor(pending.getScheduledFor())
                .processedAt(Instant.now())
                .build();

        when(deletionRequestRepository.findByUserIdAndStatus(userId, DeletionStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(deletionRequestRepository.save(any(AccountDeletionRequest.class))).thenReturn(cancelled);

        AccountDeletionRequestResponse response = deletionService.cancelDeletion(userId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(DeletionStatus.CANCELLED);

        ArgumentCaptor<AccountDeletionRequest> captor = ArgumentCaptor.forClass(AccountDeletionRequest.class);
        verify(deletionRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeletionStatus.CANCELLED);
    }

    @Test
    @DisplayName("DEL-TC-006: cancelDeletion throws when no pending request")
    void cancelDeletion_noPendingRequest_throwsNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(deletionRequestRepository.findByUserIdAndStatus(userId, DeletionStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deletionService.cancelDeletion(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No pending deletion request");
    }

    @Test
    @DisplayName("DEL-TC-007: requestDeletion schedules 30 days grace period")
    void requestDeletion_scheduledFor30DaysFromNow() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Role.MOTHER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PENDING)).thenReturn(false);
        when(passwordEncoder.matches("pass", "$2a$12$hashedpassword")).thenReturn(true);
        when(deletionRequestRepository.save(any(AccountDeletionRequest.class))).thenAnswer(inv -> {
            AccountDeletionRequest req = inv.getArgument(0);
            return AccountDeletionRequest.builder()
                    .id(UUID.randomUUID())
                    .userId(req.getUserId())
                    .status(req.getStatus())
                    .reason(req.getReason())
                    .requestedAt(req.getRequestedAt())
                    .scheduledFor(req.getScheduledFor())
                    .build();
        });

        Instant beforeCall = Instant.now();
        deletionService.requestDeletion(userId, buildRequest("pass", null));

        ArgumentCaptor<AccountDeletionRequest> captor = ArgumentCaptor.forClass(AccountDeletionRequest.class);
        verify(deletionRequestRepository).save(captor.capture());
        AccountDeletionRequest savedRequest = captor.getValue();

        // scheduledFor should be approximately 30 days from now
        Instant expectedMin = beforeCall.plusSeconds(29 * 86400L);
        Instant expectedMax = Instant.now().plusSeconds(31 * 86400L);
        assertThat(savedRequest.getScheduledFor()).isAfter(expectedMin);
        assertThat(savedRequest.getScheduledFor()).isBefore(expectedMax);
    }
}
