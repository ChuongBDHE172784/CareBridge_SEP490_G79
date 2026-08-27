package com.carebridge.backend.identity.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.ReviewAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.request.SubmitAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.entity.AccountLockAppeal;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import com.carebridge.backend.identity.admin.repository.AccountLockAppealRepository;
import com.carebridge.backend.identity.admin.service.impl.AccountLockAppealServiceImpl;
import com.carebridge.backend.identity.admin.testsupport.AdminGovernanceTestFactory;
import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountLockAppealServiceImplTest {
    @Mock private AccountLockAppealRepository appealRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuditService auditService;

    private AccountLockAppealServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountLockAppealServiceImpl(
                appealRepository, userRepository, jwtTokenProvider, auditService);
    }

    @Test
    void submit_validAdministrativeLock_createsPendingAppeal() {
        UUID episodeId = UUID.randomUUID();
        User user = lockedUser(episodeId);
        when(jwtTokenProvider.validateAppealToken("appeal-token"))
                .thenReturn(new JwtTokenProvider.AppealTokenClaims(user.getId(), episodeId));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(appealRepository.existsByUserIdAndLockEpisodeId(user.getId(), episodeId)).thenReturn(false);
        when(appealRepository.save(any(AccountLockAppeal.class))).thenAnswer(invocation -> {
            AccountLockAppeal appeal = invocation.getArgument(0);
            appeal.setId(UUID.randomUUID());
            return appeal;
        });

        var response = service.submit(new SubmitAccountLockAppealRequest("appeal-token", "  Xin xem xét lại  "));

        assertThat(response.status()).isEqualTo(AccountLockAppealStatus.PENDING);
        assertThat(response.reason()).isEqualTo("Xin xem xét lại");
        assertThat(response.lockReason()).isEqualTo("Vi phạm quy định cộng đồng");
    }

    @Test
    void submit_invalidToken_isRejectedWithoutUserLookup() {
        when(jwtTokenProvider.validateAppealToken("bad-token")).thenReturn(null);

        assertThatThrownBy(() -> service.submit(
                new SubmitAccountLockAppealRequest("bad-token", "Xin xem xét")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid or expired appeal token");

        verify(userRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void submit_staleEpisode_isRejected() {
        UUID currentEpisode = UUID.randomUUID();
        UUID staleEpisode = UUID.randomUUID();
        User user = lockedUser(currentEpisode);
        when(jwtTokenProvider.validateAppealToken("appeal-token"))
                .thenReturn(new JwtTokenProvider.AppealTokenClaims(user.getId(), staleEpisode));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.submit(
                new SubmitAccountLockAppealRequest("appeal-token", "Xin xem xét")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Lock episode is no longer active");
    }

    @Test
    void submit_duplicateAppealForSameLockEpisode_isRejected() {
        UUID episodeId = UUID.randomUUID();
        User user = lockedUser(episodeId);
        when(jwtTokenProvider.validateAppealToken("appeal-token"))
                .thenReturn(new JwtTokenProvider.AppealTokenClaims(user.getId(), episodeId));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(appealRepository.existsByUserIdAndLockEpisodeId(user.getId(), episodeId)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(
                new SubmitAccountLockAppealRequest("appeal-token", "Xin xem xét")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("appeal already exists for this lock episode");
    }

    @Test
    void review_approve_unlocksAccountAndApprovesAppeal() {
        UUID episodeId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        User user = lockedUser(episodeId);
        AccountLockAppeal appeal = pendingAppeal(user, episodeId);
        when(appealRepository.findByIdAndStatus(appeal.getId(), AccountLockAppealStatus.PENDING))
                .thenReturn(Optional.of(appeal));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(appealRepository.save(appeal)).thenReturn(appeal);
        when(userRepository.save(user)).thenReturn(user);

        var response = service.review(reviewerId, appeal.getId(),
                new ReviewAccountLockAppealRequest(
                        ReviewAccountLockAppealRequest.Decision.APPROVE, "Đã xác minh"));

        assertThat(response.status()).isEqualTo(AccountLockAppealStatus.APPROVED);
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getLockType()).isNull();
        assertThat(user.getLockReason()).isNull();
        assertThat(user.getLockEpisodeId()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void review_reject_preservesAdministrativeLock() {
        UUID episodeId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        User user = lockedUser(episodeId);
        AccountLockAppeal appeal = pendingAppeal(user, episodeId);
        when(appealRepository.findByIdAndStatus(appeal.getId(), AccountLockAppealStatus.PENDING))
                .thenReturn(Optional.of(appeal));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(appealRepository.save(appeal)).thenReturn(appeal);

        var response = service.review(reviewerId, appeal.getId(),
                new ReviewAccountLockAppealRequest(
                        ReviewAccountLockAppealRequest.Decision.REJECT, "Chưa đủ căn cứ"));

        assertThat(response.status()).isEqualTo(AccountLockAppealStatus.REJECTED);
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockType()).isEqualTo(AccountLockType.ADMIN);
        assertThat(user.getLockEpisodeId()).isEqualTo(episodeId);
        verify(userRepository, never()).save(any());
    }

    private User lockedUser(UUID episodeId) {
        return AdminGovernanceTestFactory.makeUser(Role.MODERATOR, user -> {
            user.setLocked(true);
            user.setLockedAt(Instant.now());
            user.setLockType(AccountLockType.ADMIN);
            user.setLockReason("Vi phạm quy định cộng đồng");
            user.setLockedBy(UUID.randomUUID());
            user.setLockEpisodeId(episodeId);
        });
    }

    private AccountLockAppeal pendingAppeal(User user, UUID episodeId) {
        return AccountLockAppeal.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .lockEpisodeId(episodeId)
                .reason("Xin xem xét lại")
                .status(AccountLockAppealStatus.PENDING)
                .submittedAt(Instant.now())
                .build();
    }
}

