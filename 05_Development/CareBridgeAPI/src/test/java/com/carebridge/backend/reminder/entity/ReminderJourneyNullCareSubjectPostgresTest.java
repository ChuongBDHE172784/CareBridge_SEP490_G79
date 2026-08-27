package com.carebridge.backend.reminder.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Regression for journey UUIDs being incorrectly synthesized into the care-subject FK. */
@EnabledOnOs(OS.WINDOWS)
class ReminderJourneyNullCareSubjectPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private MotherJourneyRepository journeyRepository;

    private UUID owner;
    private UUID subject;
    private UUID journey;
    private UUID reminder;

    @BeforeEach
    void seedJourneyReminderWithExplicitNullCareSubject() {
        owner = UUID.randomUUID();
        subject = UUID.randomUUID();
        reminder = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, owner, "Reminder owner",
                String.format("09%08d", Math.floorMod(owner.hashCode(), 100_000_000)), "MOTHER");
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'Reminder owner', 'ACTIVE', now(), now())
                """, subject, owner, owner);
        journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(owner)
                .careSubjectId(subject)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 6, 1))
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .build()).getId();
        jdbcTemplate.update("update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey, subject);
        jdbcTemplate.update("""
                insert into care_tasks (
                    task_id, task_type, owner_user_id, care_subject_id, journey_id, title,
                    scheduled_at, item_type, status, origin, target_subject, created_at, updated_at)
                values (?, 'SCHEDULED_REMINDER', ?, null, ?, 'Journey reminder', ?,
                        'APPOINTMENT', 'PENDING', 'USER_CREATED', 'MOTHER', now(), now())
                """, reminder, owner, journey, Timestamp.from(Instant.parse("2026-08-03T01:00:00Z")));
    }

    @AfterEach
    void cleanFixture() {
        DataSource provisionerDataSource = POSTGRES.getPostgresDatabase();
        JdbcTemplate provisioner = new JdbcTemplate(provisionerDataSource);
        new TransactionTemplate(new DataSourceTransactionManager(provisionerDataSource))
                .executeWithoutResult(status -> {
                    provisioner.execute("set local session_replication_role = replica");
                    if (reminder != null) {
                        provisioner.update(
                                "delete from reminder_occurrence_aliases where reminder_definition_id=?", reminder);
                        provisioner.update("delete from care_tasks where task_id=?", reminder);
                    }
                    if (journey != null) {
                        provisioner.update(
                                "delete from checklist_context_authorities where care_context_id=?", journey);
                        provisioner.update("delete from mother_journeys where journey_id=?", journey);
                    }
                    if (subject != null) {
                        provisioner.update("delete from care_subjects where care_subject_id=?", subject);
                    }
                    if (owner != null) {
                        provisioner.update("delete from users where user_id=?", owner);
                    }
                });
    }

    @Test
    void completingJourneyReminderKeepsNullCareSubjectInsteadOfSynthesizingJourneyId() {
        Reminder entity = reminderRepository.findById(reminder).orElseThrow();
        assertThat(entity.getCareSubjectId()).isNull();

        entity.setStatus(ReminderStatus.COMPLETED);
        reminderRepository.saveAndFlush(entity);

        assertThat(jdbcTemplate.queryForObject(
                "select status from care_tasks where task_id=?", String.class, reminder))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "select care_subject_id is null from care_tasks where task_id=?", Boolean.class, reminder))
                .isTrue();
    }
}
