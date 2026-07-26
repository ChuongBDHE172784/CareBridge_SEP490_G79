package com.carebridge.backend.consultation.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConsultationRequestPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    private ConsultationRequestPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ConsultationRequestPolicy();
    }

    @Test
    void eligibleExpertIsAccepted() {
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.ACTIVE);

        assertThatCode(() -> policy.assertExpertEligibleForConsultation(expert))
                .doesNotThrowAnyException();
    }

    @Test
    void eligibleExpertAccountIsAcceptedAtSubmitTime() {
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        User account = account(true, false, null);

        assertThatCode(() -> policy.assertExpertEligibleForConsultation(expert, account, NOW))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("ineligibleAccountStates")
    void disabledLockedOrCurrentlySuspendedExpertAccountIsRejected(
            boolean enabled, boolean locked, Instant suspendedUntil) {
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        User account = account(enabled, locked, suspendedUntil);

        assertThatThrownBy(
                        () -> policy.assertExpertEligibleForConsultation(expert, account, NOW))
                .isInstanceOfSatisfying(
                        ConsultationRequestException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getCode())
                                .isEqualTo("CONREQ-002"));
    }

    @ParameterizedTest
    @MethodSource("ineligibleAccountStates")
    void disabledLockedOrCurrentlySuspendedExpertAccountIsRejectedAtAcceptTime(
            boolean enabled, boolean locked, Instant suspendedUntil) {
        ExpertProfile expert = expert(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        User account = account(enabled, locked, suspendedUntil);

        assertThatThrownBy(
                        () -> policy.assertExpertStillEligibleForConsultation(
                                expert, account, NOW))
                .isInstanceOfSatisfying(
                        ConsultationRequestException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getCode())
                                .isEqualTo("CONREQ-004"));
    }

    @ParameterizedTest
    @MethodSource("ineligibleStates")
    void everyIneligibleCombinationIsRejected(
            VerificationStatus verificationStatus, TrustStatus trustStatus) {
        ExpertProfile expert = expert(verificationStatus, trustStatus);

        assertThatThrownBy(() -> policy.assertExpertEligibleForConsultation(expert))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getCode())
                                .isEqualTo("CONREQ-002"));
    }

    @Test
    void outsiderAndWrongActorsCollapseToNotFound() {
        UUID motherId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        ConsultationRequest request = ConsultationRequest.builder()
                .requesterUserId(motherId)
                .build();

        assertThatThrownBy(() -> policy.assertCanView(request, UUID.randomUUID(), expertId))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> {
                            org.assertj.core.api.Assertions.assertThat(ex.getCode())
                                    .isEqualTo("CONREQ-007");
                            org.assertj.core.api.Assertions.assertThat(ex.getHttpStatus().value())
                                    .isEqualTo(404);
                        });
        assertThatThrownBy(() -> policy.assertCanRespond(request, UUID.randomUUID(), expertId))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getCode())
                                .isEqualTo("CONREQ-007"));
        assertThatThrownBy(() -> policy.assertCanCancel(request, UUID.randomUUID()))
                .isInstanceOfSatisfying(ConsultationRequestException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getCode())
                                .isEqualTo("CONREQ-007"));
    }

    @Test
    void participantsCanViewAndOwnersCanAct() {
        UUID motherId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        ConsultationRequest request = ConsultationRequest.builder()
                .requesterUserId(motherId)
                .build();

        assertThatCode(() -> policy.assertCanView(request, motherId, expertId))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanView(request, expertId, expertId))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanRespond(request, expertId, expertId))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanCancel(request, motherId))
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> ineligibleStates() {
        return Stream.of(
                Arguments.of(VerificationStatus.APPROVED, TrustStatus.SUSPENDED),
                Arguments.of(VerificationStatus.APPROVED, TrustStatus.REVOKED),
                Arguments.of(VerificationStatus.PENDING, TrustStatus.ACTIVE),
                Arguments.of(VerificationStatus.UNDER_REVIEW, TrustStatus.ACTIVE),
                Arguments.of(VerificationStatus.REJECTED, TrustStatus.ACTIVE),
                Arguments.of(VerificationStatus.SUSPENDED, TrustStatus.ACTIVE),
                Arguments.of(VerificationStatus.EXPIRED, TrustStatus.ACTIVE),
                Arguments.of(VerificationStatus.APPROVED, null));
    }

    private static Stream<Arguments> ineligibleAccountStates() {
        return Stream.of(
                Arguments.of(false, false, null),
                Arguments.of(true, true, null),
                Arguments.of(true, false, NOW.plusSeconds(1)));
    }

    private static ExpertProfile expert(
            VerificationStatus verificationStatus, TrustStatus trustStatus) {
        return ExpertProfile.builder()
                .verificationStatus(verificationStatus)
                .trustStatus(trustStatus)
                .build();
    }

    private static User account(boolean enabled, boolean locked, Instant suspendedUntil) {
        return User.builder()
                .enabled(enabled)
                .locked(locked)
                .suspendedUntil(suspendedUntil)
                .build();
    }
}
