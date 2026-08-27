package com.carebridge.backend.vaccination;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.vaccination.config.VaccinationProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MF-03 against a real PostgreSQL 18 with the whole Flyway chain applied.
 *
 * <p>The unit tests can only prove the intent; they mock the repository away, so they cannot
 * see that {@code vaccination_records.care_subject_id} is NOT NULL and was previously unmapped
 * by the entity — every JPA insert of a vaccination record would have failed here. Registering
 * a baby end-to-end through the service is what demonstrates the book actually persists.
 */
class VaccinationBookEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private IBabyService babyService;
    @Autowired private VaccinationProperties properties;
    @Autowired private NotificationRecordRepository notificationRecordRepository;

    private UUID anyUser() {
        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM users ORDER BY created_at LIMIT 1", UUID.class);
    }

    private CreateBabyProfileResponse registerBaby(LocalDate birthDate, UUID owner) {
        CreateBabyProfileRequest request = new CreateBabyProfileRequest();
        request.setNickname("Bean " + UUID.randomUUID());
        request.setBirthDate(birthDate);
        return babyService.createBabyProfile(request, owner);
    }

    @Test
    void migrationSeedsASingleCoherentCatalogueVersion() {
        List<Map<String, Object>> catalogue = jdbcTemplate.queryForList("""
                SELECT vaccine_name, dose_number, offset_days
                  FROM vaccination_schedules
                 WHERE schedule_version = ?
                 ORDER BY offset_days, dose_number, vaccine_name
                """, properties.getScheduleVersion());

        assertThat(catalogue).hasSize(30);
        // Every dose is uniquely addressable by (vaccine_name, dose_number) — that pair is the
        // key the book is materialised and de-duplicated on.
        assertThat(catalogue.stream()
                .map(row -> row.get("vaccine_name") + "|" + row.get("dose_number"))
                .distinct()
                .count())
                .isEqualTo(30L);
        assertThat(catalogue.getFirst()).containsEntry("offset_days", 0);
        assertThat(catalogue.getLast()).containsEntry("offset_days", 540);
    }

    @Test
    void registeringABabyMaterialisesTheWholeExpectedBookFromTheBirthDate() {
        UUID owner = anyUser();
        LocalDate birthDate = LocalDate.now().minusDays(10);

        CreateBabyProfileResponse baby = registerBaby(birthDate, owner);

        List<Map<String, Object>> book = jdbcTemplate.queryForList("""
                SELECT r.vaccine_name, r.dose_number, r.scheduled_date, r.status,
                       r.care_subject_id, r.vaccination_schedule_id, s.offset_days
                  FROM vaccination_records r
                  JOIN vaccination_schedules s ON s.vaccination_schedule_id = r.vaccination_schedule_id
                 WHERE r.baby_id = ?
                 ORDER BY s.offset_days, r.dose_number, r.vaccine_name
                """, baby.getId());

        assertThat(book).hasSize(30);
        assertThat(book).allSatisfy(row -> {
            assertThat(row).containsEntry("status", "SCHEDULED");
            // care_subject_id is NOT NULL and must carry the same identity as baby_id.
            assertThat(row.get("care_subject_id")).isEqualTo(baby.getId());
            int offsetDays = (Integer) row.get("offset_days");
            assertThat(((java.sql.Date) row.get("scheduled_date")).toLocalDate())
                    .isEqualTo(birthDate.plusDays(offsetDays));
        });
    }

    @Test
    void theBookOnlyCoversTheActiveCatalogueNotTheLegacyRows() {
        UUID owner = anyUser();
        CreateBabyProfileResponse baby = registerBaby(LocalDate.now().minusDays(3), owner);

        Long fromOtherVersions = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM vaccination_records r
                  JOIN vaccination_schedules s ON s.vaccination_schedule_id = r.vaccination_schedule_id
                 WHERE r.baby_id = ? AND s.schedule_version <> ?
                """, Long.class, baby.getId(), properties.getScheduleVersion());

        assertThat(fromOtherVersions).isZero();
    }

    @Test
    void theReminderMilestoneLookupResolvesAgainstTheJsonbMetadata() {
        UUID owner = anyUser();
        UUID vaccinationRecordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO notification_records (
                    id, user_id, type, title, body, reference_id, reference_type,
                    status, channel, attempt_count, metadata, created_at, updated_at)
                VALUES (gen_random_uuid(), ?, 'REMINDER', 'Sắp đến lịch tiêm của bé',
                        'Bean có lịch tiêm', ?, 'VACCINATION', 'SENT', 'PUSH', 1,
                        jsonb_build_object(
                            'vaccinationRecordId', CAST(? AS text),
                            'daysBefore', '7'),
                        now(), now())
                """, owner, vaccinationRecordId, vaccinationRecordId);

        // The dedupe key is (record, lead): the delivered 7-day milestone is found, the
        // 3-day milestone of the same dose is not.
        assertThat(notificationRecordRepository
                .findVaccinationReminderByRecordAndLead(vaccinationRecordId, "7"))
                .isPresent();
        assertThat(notificationRecordRepository
                .findVaccinationReminderByRecordAndLead(vaccinationRecordId, "3"))
                .isEmpty();
        assertThat(notificationRecordRepository
                .findVaccinationReminderByRecordAndLead(UUID.randomUUID(), "7"))
                .isEmpty();
    }

    @Test
    void aCorrectedBirthDateMovesTheStillScheduledDoses() {
        UUID owner = anyUser();
        LocalDate birthDate = LocalDate.now().minusDays(20);
        CreateBabyProfileResponse baby = registerBaby(birthDate, owner);

        LocalDate corrected = birthDate.plusDays(3);
        var update = new com.carebridge.backend.baby.dto.UpdateBabyProfileRequest();
        update.setBirthDate(corrected);
        babyService.updateBabyProfile(baby.getId(), update, owner);

        java.sql.Date bcg = jdbcTemplate.queryForObject("""
                SELECT r.scheduled_date
                  FROM vaccination_records r
                  JOIN vaccination_schedules s ON s.vaccination_schedule_id = r.vaccination_schedule_id
                 WHERE r.baby_id = ? AND s.offset_days = 0 AND r.vaccine_name = 'Lao (BCG)'
                """, java.sql.Date.class, baby.getId());

        assertThat(bcg.toLocalDate()).isEqualTo(corrected);
    }
}
