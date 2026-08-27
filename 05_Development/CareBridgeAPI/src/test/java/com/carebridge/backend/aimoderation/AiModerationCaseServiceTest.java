package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.aimoderation.service.AiModerationCaseService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiModerationCaseServiceTest {

    @Mock
    private ContentReportRepository contentReportRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AiModerationCaseService caseService;

    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();

    private static CaseDecision decision(CasePriority priority) {
        return new CaseDecision(true, priority, ReportCategory.SPAM, "SPAM_ADVERTISING");
    }

    // Scenario 10: no open case → a new AUTOMATED PENDING case, never any enforcement
    @Test
    void noOpenCase_createsAutomatedPendingCase() {
        when(contentReportRepository.findOpenCasesForUpdate(eq(TARGET_ID), eq(ReportTargetType.QUESTION), any()))
                .thenReturn(List.of());
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> {
            ContentReport report = inv.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        caseService.createOrAttachCase(ReportTargetType.QUESTION, TARGET_ID,
                decision(CasePriority.HIGH), ASSESSMENT_ID, "spam quảng cáo");

        ArgumentCaptor<ContentReport> captor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository).save(captor.capture());
        ContentReport created = captor.getValue();
        assertThat(created.getReportSource()).isEqualTo(ReportSource.AUTOMATED);
        assertThat(created.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(created.getPriority()).isEqualTo(CasePriority.HIGH);
        assertThat(created.getReporterUserId()).isNull();
        assertThat(created.getCategory()).isEqualTo("SPAM");
        verify(auditService).log(eq(AuditAction.AI_CASE_CREATED), isNull(UUID.class), eq("ContentReport"),
                anyString(), any());
    }

    // Scenario 16: an existing USER report is attached to, its provenance never overwritten
    @Test
    void existingUserCase_isAttached_sourceAndCategoryPreserved() {
        ContentReport userCase = ContentReport.builder()
                .id(UUID.randomUUID())
                .targetId(TARGET_ID)
                .targetType(ReportTargetType.QUESTION)
                .status(ReportStatus.PENDING)
                .reportSource(ReportSource.USER)
                .category("HARASSMENT")
                .reporterUserId(UUID.randomUUID())
                .priority(CasePriority.NORMAL)
                .createdAt(Instant.now())
                .build();
        when(contentReportRepository.findOpenCasesForUpdate(eq(TARGET_ID), eq(ReportTargetType.QUESTION), any()))
                .thenReturn(List.of(userCase));
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID caseId = caseService.createOrAttachCase(ReportTargetType.QUESTION, TARGET_ID,
                decision(CasePriority.URGENT), ASSESSMENT_ID, "explanation");

        assertThat(caseId).isEqualTo(userCase.getId());
        // Only priority may be raised — source, category and reporter stay untouched
        assertThat(userCase.getReportSource()).isEqualTo(ReportSource.USER);
        assertThat(userCase.getCategory()).isEqualTo("HARASSMENT");
        assertThat(userCase.getReporterUserId()).isNotNull();
        assertThat(userCase.getPriority()).isEqualTo(CasePriority.URGENT);
    }

    // Scenario 14/E: rerun with an already-URGENT case never downgrades and creates nothing new
    @Test
    void existingHigherPriorityCase_isNotDowngraded() {
        ContentReport existing = ContentReport.builder()
                .id(UUID.randomUUID())
                .targetId(TARGET_ID)
                .targetType(ReportTargetType.ANSWER)
                .status(ReportStatus.IN_REVIEW)
                .reportSource(ReportSource.AUTOMATED)
                .category("SPAM")
                .priority(CasePriority.URGENT)
                .createdAt(Instant.now())
                .build();
        when(contentReportRepository.findOpenCasesForUpdate(eq(TARGET_ID), eq(ReportTargetType.ANSWER), any()))
                .thenReturn(List.of(existing));

        UUID caseId = caseService.createOrAttachCase(ReportTargetType.ANSWER, TARGET_ID,
                decision(CasePriority.NORMAL), ASSESSMENT_ID, "explanation");

        assertThat(caseId).isEqualTo(existing.getId());
        assertThat(existing.getPriority()).isEqualTo(CasePriority.URGENT);
    }

    // Scenario 20: audit detail carries IDs/codes only — never the scanned text
    @Test
    void auditDetails_containOnlyIdsAndCodes() {
        String rawContent = "toàn bộ nội dung nhạy cảm của người dùng";
        when(contentReportRepository.findOpenCasesForUpdate(any(), any(), any())).thenReturn(List.of());
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> {
            ContentReport report = inv.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        caseService.createOrAttachCase(ReportTargetType.QUESTION, TARGET_ID,
                decision(CasePriority.NORMAL), ASSESSMENT_ID, "mô tả ngắn");

        ArgumentCaptor<Object> details = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(any(), isNull(UUID.class), anyString(), anyString(), details.capture());
        assertThat(String.valueOf(details.getValue())).doesNotContain(rawContent);
    }
}
