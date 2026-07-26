package com.carebridge.backend.expert;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ExpertDirectoryEligibilityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ExpertProfileRepository repository;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void everyDirectoryQueryFiltersApprovedAndActiveWithoutSearchOrPaginationBypass() {
        UUID eligibleOb = seedExpert(
                "Dr Eligible OB", "Sản khoa", "APPROVED", "ACTIVE", 4.9);
        UUID suspendedOb =
                seedExpert("Dr Suspended OB", "Sản khoa", "APPROVED", "SUSPENDED", 5.0);
        UUID pendingOb =
                seedExpert("Dr Pending OB", "Sản khoa", "PENDING", "ACTIVE", 4.8);
        UUID eligiblePediatric = seedExpert(
                "Dr Eligible Pediatric", "Nhi khoa", "APPROVED", "ACTIVE", 4.7);
        UUID disabledExpert = seedExpert(
                "Dr Disabled Account", "Account Disabled", "APPROVED", "ACTIVE", 4.6);
        UUID lockedExpert = seedExpert(
                "Dr Locked Account", "Account Locked", "APPROVED", "ACTIVE", 4.5);
        UUID suspendedExpert = seedExpert(
                "Dr Suspended Account", "Account Suspended", "APPROVED", "ACTIVE", 4.4);
        UUID suspensionEndedExpert = seedExpert(
                "Dr Suspension Ended", "Account Restored", "APPROVED", "ACTIVE", 4.3);
        setAccountState(disabledExpert, false, false, null);
        setAccountState(lockedExpert, true, true, null);
        setAccountState(suspendedExpert, true, false, "now() + interval '1 day'");
        setAccountState(suspensionEndedExpert, true, false, "now() - interval '1 second'");

        assertThat(repository.findVerifiedPublic())
                .extracting(profile -> profile.getExpertProfileId())
                .contains(eligibleOb, eligiblePediatric, suspensionEndedExpert)
                .doesNotContain(
                        suspendedOb,
                        pendingOb,
                        disabledExpert,
                        lockedExpert,
                        suspendedExpert);
        assertThat(repository.findVerifiedBySpecialty("Sản khoa"))
                .extracting(profile -> profile.getExpertProfileId())
                .contains(eligibleOb)
                .doesNotContain(suspendedOb, pendingOb);
        assertThat(repository.searchDirectory(
                                "Sản khoa", "Eligible", PageRequest.of(0, 1))
                        .getContent())
                .extracting(profile -> profile.getExpertProfileId())
                .containsExactly(eligibleOb);
        assertThat(repository.searchDirectory(
                                null, "Suspended", PageRequest.of(0, 20))
                        .getContent())
                .isEmpty();
        assertThat(repository.searchDirectory(
                                null, "Account", PageRequest.of(0, 20))
                        .getContent())
                .extracting(profile -> profile.getExpertProfileId())
                .doesNotContain(disabledExpert, lockedExpert, suspendedExpert);
        assertThat(repository.findApprovedSpecialties())
                .contains("Sản khoa", "Nhi khoa", "Account Restored")
                .doesNotContain("Account Disabled", "Account Locked", "Account Suspended");
    }

    private UUID seedExpert(
            String name,
            String specialty,
            String verificationStatus,
            String trustStatus,
            double rating) {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        String phone = uniquePhone();
        jdbcTemplate.update("""
                INSERT INTO persons(person_id, display_name, phone_number, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                """, userId, name, phone);
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, person_id, full_name, phone, role, enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'EXPERT', true, false, now(), now())
                """, userId, userId, name, phone);
        jdbcTemplate.update("""
                INSERT INTO professional_profiles
                    (professional_profile_id, user_id, specialty, verification_status, trust_status,
                     rating_avg, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, now(), now())
                """, profileId, userId, specialty, verificationStatus, trustStatus, rating);
        return profileId;
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private void setAccountState(
            UUID profileId, boolean enabled, boolean locked, String suspendedUntilExpression) {
        String suspendedUntil = suspendedUntilExpression == null
                ? "NULL"
                : suspendedUntilExpression;
        jdbcTemplate.update(
                "UPDATE users SET enabled = ?, locked = ?, suspended_until = "
                        + suspendedUntil
                        + " WHERE user_id = (SELECT user_id FROM professional_profiles "
                        + "WHERE professional_profile_id = ?)",
                enabled,
                locked,
                profileId);
    }
}
