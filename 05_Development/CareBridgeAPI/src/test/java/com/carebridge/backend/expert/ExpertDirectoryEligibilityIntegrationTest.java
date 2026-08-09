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
        String phone = uniquePhone();
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, person_id, full_name, display_name, phone, role,
                     specialty, verification_status, trust_status, rating_avg,
                     enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'EXPERT', ?, ?, ?, ?, true, false, now(), now())
                """, userId, userId, name, name, phone,
                specialty, verificationStatus, trustStatus, rating);
        UUID specialtyId = UUID.nameUUIDFromBytes(
                ("expert-directory:" + specialty).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        jdbcTemplate.update("""
                INSERT INTO specialties(specialty_id, code, name, is_active, created_at)
                VALUES (?, ?, ?, true, now())
                ON CONFLICT (specialty_id) DO NOTHING
                """, specialtyId, "DIR_" + specialtyId.toString().replace("-", ""), specialty);
        jdbcTemplate.update("""
                INSERT INTO professional_specialties
                    (professional_profile_id, specialty_id, is_primary, created_at)
                VALUES (?, ?, true, now())
                """, userId, specialtyId);
        return userId;
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private void setAccountState(
            UUID expertUserId, boolean enabled, boolean locked, String suspendedUntilExpression) {
        // Suspension is written the way the application persists it (User.suspendedUntil is
        // @Transient, canonically stored in users.settings_jsonb->>'suspendedUntil') AND into
        // the physical users.suspended_until column, so every directory predicate is exercised.
        String suspendedUntil = suspendedUntilExpression == null
                ? "NULL"
                : suspendedUntilExpression;
        String settingsJsonb = suspendedUntilExpression == null
                ? "(coalesce(settings_jsonb, '{}'::jsonb) - 'suspendedUntil')"
                : "jsonb_set(coalesce(settings_jsonb, '{}'::jsonb), '{suspendedUntil}', "
                        + "to_jsonb((" + suspendedUntilExpression + ")::timestamptz::text))";
        jdbcTemplate.update(
                "UPDATE users SET enabled = ?, locked = ?, lock_type = CASE WHEN ? THEN 'TEMPORARY' ELSE NULL END, locked_at = CASE WHEN ? THEN now() ELSE NULL END, suspended_until = "
                        + suspendedUntil
                        + ", settings_jsonb = "
                        + settingsJsonb
                        + " WHERE user_id = ?",
                enabled,
                locked,
                locked,
                locked,
                expertUserId);
    }
}
