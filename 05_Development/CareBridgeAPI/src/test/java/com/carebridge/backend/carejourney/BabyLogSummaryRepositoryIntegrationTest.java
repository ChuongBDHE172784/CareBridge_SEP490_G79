package com.carebridge.backend.carejourney;

import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.repository.LogTypeAggregateRow;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BabyLogSummaryRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant FROM = Instant.parse("2026-07-14T03:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-15T03:00:00Z");

    @Autowired private BabyDailyLogRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID ownerId;
    private UUID babyId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        babyId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, ownerId, "Summary owner", null, "MOTHER");
        jdbcTemplate.update(
                "INSERT INTO care_subjects "
                        + "(care_subject_id, person_id, owner_user_id, subject_type, nickname, status) "
                        + "VALUES (?, ?, ?, 'BABY', 'Summary baby', 'ACTIVE')",
                babyId, ownerId, ownerId);
    }

    @Test
    void aggregateUsesEventTimeHalfOpenWindowAndCreatedAtFallback() {
        insertLog("DIAPER", FROM, FROM.plusSeconds(60), "1", "count", "lower-bound");
        insertLog("DIAPER", TO, FROM.plusSeconds(120), "2", "count", "upper-bound");
        insertLog("DIAPER", FROM.minusSeconds(1), FROM.plusSeconds(180), "4", "count", "back-entered-old");
        insertLog("DIAPER", FROM.plusSeconds(240), FROM.minusSeconds(3600), "8", "count", "recent-old-create");
        insertLog("DIAPER", null, FROM.plusSeconds(300), "16", "count", "legacy-fallback");

        LogTypeAggregateRow diaper = repository.aggregateByLogType(babyId, FROM, TO).stream()
                .filter(row -> "DIAPER".equals(row.getLogType()))
                .findFirst()
                .orElseThrow();

        assertThat(diaper.getCount()).isEqualTo(3);
        assertThat(diaper.getTotalQuantity()).isEqualByComparingTo("25");
    }

    @Test
    void medicineNotesUseTheSameEventTimeMembershipAsAggregate() {
        insertLog("MEDICINE", FROM, FROM.plusSeconds(60), "1", "dose", "lower-bound");
        insertLog("MEDICINE", TO, FROM.plusSeconds(120), "1", "dose", "upper-bound");
        insertLog("MEDICINE", FROM.minusSeconds(1), FROM.plusSeconds(180), "1", "dose", "back-entered-old");
        insertLog("MEDICINE", null, FROM.plusSeconds(240), "1", "dose", "legacy-fallback");

        LogTypeAggregateRow medicine = repository.aggregateByLogType(babyId, FROM, TO).stream()
                .filter(row -> "MEDICINE".equals(row.getLogType()))
                .findFirst()
                .orElseThrow();
        List<String> notes = repository.findNotesByLogTypeAndPeriod(
                babyId, "MEDICINE", FROM, TO);

        assertThat(medicine.getCount()).isEqualTo(2);
        assertThat(notes).containsExactlyInAnyOrder("lower-bound", "legacy-fallback");
        assertThat(notes).hasSize((int) medicine.getCount());
    }

    private void insertLog(
            String logType,
            Instant startedAt,
            Instant createdAt,
            String quantity,
            String unit,
            String note) {
        Timestamp startedTimestamp = startedAt == null ? null : Timestamp.from(startedAt);
        Timestamp createdTimestamp = Timestamp.from(createdAt);
        jdbcTemplate.update(
                """
                INSERT INTO care_logs (
                    care_log_id, care_subject_id, log_type, started_at, quantity,
                    unit, note, recorded_by, status, payload_jsonb, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', '{}'::jsonb, ?, ?)
                """,
                UUID.randomUUID(), babyId, logType, startedTimestamp, new BigDecimal(quantity),
                unit, note, ownerId, createdTimestamp, createdTimestamp);
    }
}
