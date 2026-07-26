package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository-level atomicity of the status-guarded UPDATEs (H2 MODE=PostgreSQL). The guarded
 * UPDATE returns 1 for exactly one caller; every later attempt sees the changed status and
 * returns 0 — the same row-level guarantee Postgres provides under concurrency.
 */
// Dedicated in-memory DB: the default shared `testdb` H2 instance is dropped whenever another
// cached Spring context is evicted and closed (create-drop), which would randomly empty this
// test's schema mid-suite. A unique database name makes this context's schema self-owned.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aimoderation_claim_testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=test",
        "spring.datasource.driver-class-name=org.h2.Driver",
})
@Transactional
class AiClaimAtomicityIntegrationTest {

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Autowired
    private ContentReportRepository contentReportRepository;

    @Autowired
    private AiContentScanJobRepository jobRepository;

    private static final UUID MODERATOR_A = UUID.randomUUID();
    private static final UUID MODERATOR_B = UUID.randomUUID();

    // Scenario 17: only one moderator can claim a PENDING report
    @Test
    void claimReport_secondClaimerLoses() {
        ContentReport report = contentReportRepository.save(ContentReport.builder()
                .targetId(UUID.randomUUID())
                .targetType(ReportTargetType.QUESTION)
                .status(ReportStatus.PENDING)
                .reportSource(ReportSource.USER)
                .category("SPAM")
                .createdAt(Instant.now())
                .build());
        Instant now = Instant.now();

        int first = contentReportRepository.claimReport(report.getId(), MODERATOR_A, now,
                ReportStatus.PENDING, ReportStatus.IN_REVIEW);
        int second = contentReportRepository.claimReport(report.getId(), MODERATOR_B, now,
                ReportStatus.PENDING, ReportStatus.IN_REVIEW);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        ContentReport reloaded = contentReportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(reloaded.getAssignedModeratorId()).isEqualTo(MODERATOR_A);
        assertThat(reloaded.getClaimedAt()).isNotNull();
    }

    // Scenario 18: release is only possible for the claiming moderator
    @Test
    void releaseReport_onlyClaimerSucceeds() {
        ContentReport report = contentReportRepository.save(ContentReport.builder()
                .targetId(UUID.randomUUID())
                .targetType(ReportTargetType.ANSWER)
                .status(ReportStatus.PENDING)
                .reportSource(ReportSource.USER)
                .category("HARASSMENT")
                .createdAt(Instant.now())
                .build());
        Instant now = Instant.now();
        contentReportRepository.claimReport(report.getId(), MODERATOR_A, now,
                ReportStatus.PENDING, ReportStatus.IN_REVIEW);

        int wrongModerator = contentReportRepository.releaseReport(report.getId(), MODERATOR_B, now,
                ReportStatus.PENDING, ReportStatus.IN_REVIEW);
        int claimer = contentReportRepository.releaseReport(report.getId(), MODERATOR_A, now,
                ReportStatus.PENDING, ReportStatus.IN_REVIEW);

        assertThat(wrongModerator).isZero();
        assertThat(claimer).isEqualTo(1);
        ContentReport reloaded = contentReportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(reloaded.getAssignedModeratorId()).isNull();
        assertThat(reloaded.getClaimedAt()).isNull();
    }

    // Scenario E: two workers claiming the same scan job — one wins, one loses
    @Test
    void claimScanJob_secondWorkerLoses() {
        AiContentScanJob job = jobRepository.save(AiContentScanJob.builder()
                .targetType(ReportTargetType.QUESTION)
                .targetId(UUID.randomUUID())
                .contentHash("abc123")
                .status(AiScanJobStatus.QUEUED)
                .nextAttemptAt(Instant.now().minusSeconds(1))
                .build());
        Instant now = Instant.now();

        int workerOne = jobRepository.claim(job.getId(), "worker-1", now,
                AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING);
        int workerTwo = jobRepository.claim(job.getId(), "worker-2", now,
                AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING);

        assertThat(workerOne).isEqualTo(1);
        assertThat(workerTwo).isZero();
        AiContentScanJob reloaded = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AiScanJobStatus.PROCESSING);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getLockedBy()).isEqualTo("worker-1");
    }

    // A future next_attempt_at (backoff) keeps the job unclaimable until due
    @Test
    void claimScanJob_notDueYet_cannotBeClaimed() {
        AiContentScanJob job = jobRepository.save(AiContentScanJob.builder()
                .targetType(ReportTargetType.ANSWER)
                .targetId(UUID.randomUUID())
                .contentHash("def456")
                .status(AiScanJobStatus.QUEUED)
                .nextAttemptAt(Instant.now().plusSeconds(3600))
                .build());

        int claimed = jobRepository.claim(job.getId(), "worker-1", Instant.now(),
                AiScanJobStatus.QUEUED, AiScanJobStatus.PROCESSING);

        assertThat(claimed).isZero();
    }
}
