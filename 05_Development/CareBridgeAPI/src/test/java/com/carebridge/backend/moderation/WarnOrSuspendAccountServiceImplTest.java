package com.carebridge.backend.moderation;

import static com.carebridge.backend.moderation.WarnSuspendAccountTestFactory.FUTURE_EXPIRY;
import static com.carebridge.backend.moderation.WarnSuspendAccountTestFactory.MODERATOR_ID;
import static com.carebridge.backend.moderation.WarnSuspendAccountTestFactory.PAST_EXPIRY;
import static com.carebridge.backend.moderation.WarnSuspendAccountTestFactory.TARGET_USER_ID;
import static com.carebridge.backend.moderation.WarnSuspendAccountTestFactory.makeRequest;
import static com.carebridge.backend.moderation.WarnSuspendAccountTestFactory.makeUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.WarnOrSuspendAccountRequest;
import com.carebridge.backend.content.dto.response.WarnOrSuspendAccountResponse;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.content.service.ContentPreviewService;
import com.carebridge.backend.content.service.ModerationServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// WSA-TC-201..213 (UC-102 CB-MOD-TEST-004)
@ExtendWith(MockitoExtension.class)
class WarnOrSuspendAccountServiceImplTest {

    @Mock
    private ContentReportRepository contentReportRepository;

    @Mock
    private ContentPreviewService contentPreviewService;

    private final ModerationMapper moderationMapper = new ModerationMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private CommunityQuestionRepository communityQuestionRepository;

    @Mock
    private CommunityAnswerRepository communityAnswerRepository;

    @Mock
    private ModerationActionRepository moderationActionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = () -> MODERATOR_ID.toString();
        org.mockito.Mockito.lenient().when(moderationActionRepository.save(any(ModerationAction.class)))
                .thenAnswer(inv -> {
                    ModerationAction action = inv.getArgument(0);
                    if (action.getId() == null) {
                        action.setId(java.util.UUID.randomUUID());
                    }
                    return action;
                });
    }

    // WSA-TC-201
    @Test
    void moderateAccount_warn_recordsActionAndDoesNotMutateUser() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(makeUser(TARGET_USER_ID, null)));

        WarnOrSuspendAccountRequest request =
                makeRequest(ModerationActionType.WARN, "Ngôn từ không phù hợp, lần đầu", null);
        WarnOrSuspendAccountResponse response = moderationService.moderateAccount(request, principal);

        assertThat(response.actionType()).isEqualTo(ModerationActionType.WARN);
        assertThat(response.accountSuspended()).isFalse();
        assertThat(response.expiresAt()).isNull();

        ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(ModerationActionType.WARN);
        assertThat(captor.getValue().getTargetType()).isEqualTo(ReportTargetType.ACCOUNT);
        assertThat(captor.getValue().getReportId()).isNull();

        verify(userRepository, never()).save(any());
        verify(auditService, times(1)).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID),
                eq("ACCOUNT"), eq(TARGET_USER_ID.toString()), any());
    }

    // WSA-TC-202
    @Test
    void moderateAccount_suspend_setsSuspendedUntilOnly() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(makeUser(TARGET_USER_ID, null)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        WarnOrSuspendAccountRequest request =
                makeRequest(ModerationActionType.SUSPEND, "Vi phạm lặp lại sau cảnh báo", FUTURE_EXPIRY);
        WarnOrSuspendAccountResponse response = moderationService.moderateAccount(request, principal);

        assertThat(response.actionType()).isEqualTo(ModerationActionType.SUSPEND);
        assertThat(response.accountSuspended()).isTrue();
        assertThat(response.expiresAt()).isEqualTo(FUTURE_EXPIRY);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getSuspendedUntil()).isEqualTo(FUTURE_EXPIRY);
        assertThat(saved.isLocked()).isFalse();
        assertThat(saved.getLockedAt()).isNull();
        assertThat(saved.isEnabled()).isTrue();

        ArgumentCaptor<ModerationAction> actionCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository, times(1)).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ModerationActionType.SUSPEND);
        assertThat(actionCaptor.getValue().getTargetType()).isEqualTo(ReportTargetType.ACCOUNT);
        assertThat(actionCaptor.getValue().getReportId()).isNull();
        assertThat(actionCaptor.getValue().getExpiresAt()).isEqualTo(FUTURE_EXPIRY);
    }

    // WSA-TC-203
    @Test
    void moderateAccount_contentActionTypes_rejectedWithMod016() {
        for (ModerationActionType actionType : new ModerationActionType[]{
                ModerationActionType.APPROVE, ModerationActionType.HIDE, ModerationActionType.LOCK}) {
            WarnOrSuspendAccountRequest request = makeRequest(actionType, "x", null);
            assertThatThrownBy(() -> moderationService.moderateAccount(request, principal))
                    .isInstanceOf(ModerationException.class)
                    .extracting(ex -> ((ModerationException) ex).getCode())
                    .isEqualTo("MOD-016");
        }
        verify(userRepository, never()).save(any());
        verify(moderationActionRepository, never()).save(any());
    }

    // WSA-TC-204
    @Test
    void moderateAccount_warnWithBlankReason_rejectedWithMod017() {
        WarnOrSuspendAccountRequest nullReason = makeRequest(ModerationActionType.WARN, null, null);
        assertThatThrownBy(() -> moderationService.moderateAccount(nullReason, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-017");

        WarnOrSuspendAccountRequest blankReason = makeRequest(ModerationActionType.WARN, "   ", null);
        assertThatThrownBy(() -> moderationService.moderateAccount(blankReason, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-017");
    }

    // WSA-TC-205
    @Test
    void moderateAccount_suspendWithBlankReason_rejectedWithMod017() {
        WarnOrSuspendAccountRequest nullReason = makeRequest(ModerationActionType.SUSPEND, null, FUTURE_EXPIRY);
        assertThatThrownBy(() -> moderationService.moderateAccount(nullReason, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-017");

        WarnOrSuspendAccountRequest blankReason = makeRequest(ModerationActionType.SUSPEND, "  ", FUTURE_EXPIRY);
        assertThatThrownBy(() -> moderationService.moderateAccount(blankReason, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-017");

        verify(userRepository, never()).save(any());
    }

    // WSA-TC-206
    @Test
    void moderateAccount_suspendMissingExpiresAt_rejectedWithMod018() {
        WarnOrSuspendAccountRequest request = makeRequest(ModerationActionType.SUSPEND, "reason hợp lệ", null);
        assertThatThrownBy(() -> moderationService.moderateAccount(request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-018");
        verify(userRepository, never()).save(any());
    }

    // WSA-TC-207
    @Test
    void moderateAccount_suspendExpiresAtPastOrNow_rejectedWithMod018() {
        WarnOrSuspendAccountRequest pastRequest = makeRequest(ModerationActionType.SUSPEND, "reason", PAST_EXPIRY);
        assertThatThrownBy(() -> moderationService.moderateAccount(pastRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-018");

        // "Exactly now" boundary: capture Instant.now() and use it immediately as expiresAt. No Clock
        // abstraction exists anywhere in this codebase (verified — AuthenticationPolicy also compares
        // against a live Instant.now()), so this test relies on wall-clock ordering: by the time the
        // service reads its own Instant.now(), the captured value is already <= that reading.
        java.time.Instant nowExpiry = java.time.Instant.now();
        WarnOrSuspendAccountRequest nowRequest = makeRequest(ModerationActionType.SUSPEND, "reason", nowExpiry);
        assertThatThrownBy(() -> moderationService.moderateAccount(nowRequest, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-018");

        verify(userRepository, never()).save(any());
    }

    // WSA-TC-208
    @Test
    void moderateAccount_warnWithExpiresAt_rejectedWithMod019() {
        WarnOrSuspendAccountRequest request = makeRequest(ModerationActionType.WARN, "reason", FUTURE_EXPIRY);
        assertThatThrownBy(() -> moderationService.moderateAccount(request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-019");
        verify(moderationActionRepository, never()).save(any());
    }

    // WSA-TC-209 (ADR-007, Accepted)
    @Test
    void moderateAccount_selfAction_rejectedWithMod020BeforeAnyLookup() {
        WarnOrSuspendAccountRequest request =
                new WarnOrSuspendAccountRequest(MODERATOR_ID, ModerationActionType.SUSPEND, "reason", FUTURE_EXPIRY);

        assertThatThrownBy(() -> moderationService.moderateAccount(request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-020");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verify(moderationActionRepository, never()).save(any());
    }

    // WSA-TC-210
    @Test
    void moderateAccount_targetUserNotFound_rejectedWithMod015() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

        WarnOrSuspendAccountRequest request = makeRequest(ModerationActionType.WARN, "reason", null);
        assertThatThrownBy(() -> moderationService.moderateAccount(request, principal))
                .isInstanceOf(ModerationException.class)
                .extracting(ex -> ((ModerationException) ex).getCode())
                .isEqualTo("MOD-015");

        verify(moderationActionRepository, never()).save(any());
    }

    // WSA-TC-211
    @Test
    void moderateAccount_actionShape_reportIdNullTargetTypeAccount() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(makeUser(TARGET_USER_ID, null)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        WarnOrSuspendAccountRequest request = makeRequest(ModerationActionType.SUSPEND, "reason", FUTURE_EXPIRY);
        moderationService.moderateAccount(request, principal);

        ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(captor.capture());
        assertThat(captor.getValue().getReportId()).isNull();
        assertThat(captor.getValue().getTargetType()).isEqualTo(ReportTargetType.ACCOUNT);
        assertThat(captor.getValue().getTargetId()).isEqualTo(TARGET_USER_ID);
    }

    // WSA-TC-212
    @Test
    void moderateAccount_auditLoggedExactlyOnceForBothActionTypes() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(makeUser(TARGET_USER_ID, null)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.moderateAccount(
                makeRequest(ModerationActionType.SUSPEND, "reason", FUTURE_EXPIRY), principal);
        moderationService.moderateAccount(
                makeRequest(ModerationActionType.WARN, "reason", null), principal);

        verify(auditService, times(2)).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID),
                eq("ACCOUNT"), eq(TARGET_USER_ID.toString()), any());
    }

    // WSA-TC-213
    @Test
    void moderateAccount_warn_neverCallsUserRepositorySave() {
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(makeUser(TARGET_USER_ID, null)));

        moderationService.moderateAccount(makeRequest(ModerationActionType.WARN, "reason", null), principal);

        verify(userRepository, never()).save(any());
    }
}
