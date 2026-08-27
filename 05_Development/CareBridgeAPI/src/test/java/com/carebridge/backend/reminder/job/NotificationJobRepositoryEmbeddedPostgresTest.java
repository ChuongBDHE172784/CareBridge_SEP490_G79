package com.carebridge.backend.reminder.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R11a contract for the consolidated queue (V3 §3.8, plan §4.9).
 *
 * <p>Runs against real PostgreSQL because the things worth proving here — partial
 * unique indexes, the discriminator CHECK, and a conditional-update claim under
 * concurrency — are all database behaviour that an in-memory test would fake.
 */
class NotificationJobRepositoryEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    private static final EnumSet<AppointmentNotificationJobStatus> ACTIVE =
            EnumSet.of(AppointmentNotificationJobStatus.PENDING,
                    AppointmentNotificationJobStatus.PROCESSING);

    @Autowired private NotificationJobRepository jobRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private UUID scheduleId;
    private UUID reminderId;
    private Instant now;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM notification_jobs");
        now = Instant.now();

        UUID ownerId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users ORDER BY created_at LIMIT 1", UUID.class);

        scheduleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO reminder_schedules
                    (schedule_id, owner_user_id, title, time_zone, start_date, active, local_times)
                VALUES (?, ?, 'Uống thuốc', 'Asia/Ho_Chi_Minh', current_date, true,
                        ARRAY['08:00']::time[])
                """, scheduleId, ownerId);

        reminderId = jdbcTemplate.queryForObject(
                "SELECT task_id FROM care_tasks ORDER BY created_at LIMIT 1", UUID.class);
    }

    private NotificationJob scheduleJob(LocalTime at) {
        return jobRepository.save(NotificationJob.builder()
                .jobType(NotificationJobType.REMINDER_SCHEDULE)
                .scheduleId(scheduleId).scheduleRevision(1L)
                .occurrenceDate(LocalDate.now()).localTime(at).timeZone("Asia/Ho_Chi_Minh")
                .dueAt(now.minusSeconds(60)).nextAttemptAt(now.minusSeconds(60))
                .status(AppointmentNotificationJobStatus.PENDING)
                .attemptCount(0).createdAt(now).updatedAt(now)
                .build());
    }

    private NotificationJob appointmentJob(UUID occurrenceId, int offsetMinutes) {
        return jobRepository.save(NotificationJob.builder()
                .jobType(NotificationJobType.APPOINTMENT)
                .reminderId(reminderId).occurrenceId(occurrenceId)
                .occurrenceGeneration(0L).occurrenceScheduledAt(now.plusSeconds(3600))
                .configRevision(1L).offsetMinutes(offsetMinutes)
                .dueAt(now.minusSeconds(60)).nextAttemptAt(now.minusSeconds(60))
                .status(AppointmentNotificationJobStatus.PENDING)
                .attemptCount(0).createdAt(now).updatedAt(now)
                .build());
    }

    @Test
    @DisplayName("Each branch keeps its own identity, and neither constrains the other")
    void partialUniqueIdentitiesAreEnforcedPerBranch() {
        scheduleJob(LocalTime.of(8, 0));
        assertThatDuplicateIsRejected(() -> scheduleJob(LocalTime.of(8, 0)));

        // A different time is a different occurrence — still allowed.
        assertThat(scheduleJob(LocalTime.of(9, 0)).getId()).isNotNull();

        UUID occurrenceId = UUID.randomUUID();
        appointmentJob(occurrenceId, -30);
        assertThatDuplicateIsRejected(() -> appointmentJob(occurrenceId, -30));

        // Same occurrence, different offset — a separate notification.
        assertThat(appointmentJob(occurrenceId, -60).getId()).isNotNull();
    }

    private void assertThatDuplicateIsRejected(Runnable insert) {
        try {
            insert.run();
            jdbcTemplate.execute("SELECT 1");
            throw new AssertionError("expected the partial unique index to reject the duplicate");
        } catch (DataIntegrityViolationException expected) {
            // The identity held.
        }
    }

    @Test
    @DisplayName("The discriminator CHECK refuses a row carrying both branches")
    void aRowCannotCarryBothBranches() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_constraint
                 WHERE conrelid = 'public.notification_jobs'::regclass
                   AND conname = 'notification_jobs_branch_ck'
                """, Long.class)).isEqualTo(1L);

        try {
            jdbcTemplate.update("""
                    INSERT INTO notification_jobs
                        (job_id, job_type, due_at, status, attempt_count, next_attempt_at,
                         created_at, updated_at,
                         schedule_id, schedule_revision, occurrence_date, local_time, time_zone,
                         reminder_id, occurrence_id, occurrence_generation,
                         occurrence_scheduled_at, config_revision, offset_minutes)
                    VALUES (gen_random_uuid(), 'REMINDER_SCHEDULE', now(), 'PENDING', 0, now(),
                            now(), now(),
                            ?, 1, current_date, '08:00', 'Asia/Ho_Chi_Minh',
                            ?, gen_random_uuid(), 0, now(), 1, -30)
                    """, scheduleId, reminderId);
            throw new AssertionError("expected the branch CHECK to reject a two-branch row");
        } catch (DataIntegrityViolationException expected) {
            // Exactly one branch may be populated.
        }
    }

    @Test
    @DisplayName("A worker never claims a job of the other type")
    void claimIsIsolatedByDiscriminator() {
        NotificationJob schedule = scheduleJob(LocalTime.of(8, 0));
        NotificationJob appointment = appointmentJob(UUID.randomUUID(), -30);

        List<UUID> scheduleClaimable = jobRepository.findClaimableIds(
                NotificationJobType.REMINDER_SCHEDULE,
                AppointmentNotificationJobStatus.PENDING, now, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(scheduleClaimable).containsExactly(schedule.getId());

        // Handing the appointment id to the schedule worker must change nothing.
        int stolen = transactionTemplate.execute(status -> jobRepository.claim(
                appointment.getId(), NotificationJobType.REMINDER_SCHEDULE, "worker-schedule", now,
                AppointmentNotificationJobStatus.PENDING, AppointmentNotificationJobStatus.PROCESSING));
        assertThat(stolen).isZero();

        assertThat(jobRepository.findById(appointment.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentNotificationJobStatus.PENDING);
    }

    @Test
    @DisplayName("Two workers racing for one job: exactly one wins")
    void concurrentClaimGrantsTheJobExactlyOnce() throws Exception {
        NotificationJob job = scheduleJob(LocalTime.of(8, 0));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> racers = List.of(
                    claimTask(job.getId(), "worker-a"),
                    claimTask(job.getId(), "worker-b"),
                    claimTask(job.getId(), "worker-c"),
                    claimTask(job.getId(), "worker-d"));

            int winners = 0;
            for (Future<Integer> result : pool.invokeAll(racers)) {
                winners += result.get();
            }

            assertThat(winners)
                    .as("a duplicated claim would send the same notification twice")
                    .isEqualTo(1);
        } finally {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        NotificationJob claimed = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(AppointmentNotificationJobStatus.PROCESSING);
        assertThat(claimed.getAttemptCount()).isEqualTo(1);
        assertThat(claimed.getLockedBy()).isNotNull();
    }

    private Callable<Integer> claimTask(UUID jobId, String workerId) {
        return () -> transactionTemplate.execute(status -> jobRepository.claim(
                jobId, NotificationJobType.REMINDER_SCHEDULE, workerId, now,
                AppointmentNotificationJobStatus.PENDING,
                AppointmentNotificationJobStatus.PROCESSING));
    }

    @Test
    @DisplayName("A stale lock is requeued only for its own job type")
    void staleLockRequeueIsScopedByType() {
        NotificationJob schedule = scheduleJob(LocalTime.of(8, 0));
        NotificationJob appointment = appointmentJob(UUID.randomUUID(), -30);

        Instant lockedLongAgo = now.minusSeconds(3600);
        jdbcTemplate.update("""
                UPDATE notification_jobs
                   SET status = 'PROCESSING', locked_by = 'dead-worker', locked_at = ?
                 WHERE job_id IN (?, ?)
                """, java.sql.Timestamp.from(lockedLongAgo), schedule.getId(), appointment.getId());

        int requeued = transactionTemplate.execute(status -> jobRepository.requeueStale(
                NotificationJobType.REMINDER_SCHEDULE, now.minusSeconds(600), now,
                AppointmentNotificationJobStatus.PENDING, AppointmentNotificationJobStatus.PROCESSING));

        assertThat(requeued).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_jobs WHERE job_id = ?", String.class, schedule.getId()))
                .isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_jobs WHERE job_id = ?", String.class, appointment.getId()))
                .isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("Cancelling one schedule's jobs leaves the other type untouched")
    void cancellationIsScopedByBranch() {
        NotificationJob schedule = scheduleJob(LocalTime.of(8, 0));
        NotificationJob appointment = appointmentJob(UUID.randomUUID(), -30);

        int cancelled = transactionTemplate.execute(status -> jobRepository.cancelActiveByScheduleId(
                scheduleId, ACTIVE, AppointmentNotificationJobStatus.CANCELLED, now));

        assertThat(cancelled).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_jobs WHERE job_id = ?", String.class, schedule.getId()))
                .isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_jobs WHERE job_id = ?", String.class, appointment.getId()))
                .isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Obsolete-revision cancellation spares the current revision")
    void obsoleteRevisionCancellationKeepsTheCurrentOne() {
        NotificationJob current = scheduleJob(LocalTime.of(8, 0));

        NotificationJob obsolete = jobRepository.save(NotificationJob.builder()
                .jobType(NotificationJobType.REMINDER_SCHEDULE)
                .scheduleId(scheduleId).scheduleRevision(2L)
                .occurrenceDate(LocalDate.now()).localTime(LocalTime.of(8, 0))
                .timeZone("Asia/Ho_Chi_Minh")
                .dueAt(now).nextAttemptAt(now)
                .status(AppointmentNotificationJobStatus.PENDING)
                .attemptCount(0).createdAt(now).updatedAt(now)
                .build());

        int cancelled = transactionTemplate.execute(status ->
                jobRepository.cancelObsoleteScheduleRevisions(
                        scheduleId, 2L, ACTIVE, AppointmentNotificationJobStatus.CANCELLED, now));

        assertThat(cancelled).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_jobs WHERE job_id = ?", String.class, current.getId()))
                .isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_jobs WHERE job_id = ?", String.class, obsolete.getId()))
                .isEqualTo("PENDING");
    }
}
