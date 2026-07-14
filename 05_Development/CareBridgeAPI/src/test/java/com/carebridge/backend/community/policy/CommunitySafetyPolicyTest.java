package com.carebridge.backend.community.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.policy.TriageRedFlagPolicy;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunitySafetyPolicyTest {

    @Mock private UserRepository userRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private CommunityQuestionRepository questionRepository;
    @Mock private CommunityAnswerRepository answerRepository;
    @Mock private ContentReportRepository contentReportRepository;
    @Mock private TriageRedFlagPolicy redFlagPolicy;
    @Mock private AuditService auditService;

    @Test
    void autoReportIfRedFlag_marksCreatedReportAsAutomated() {
        UUID reporterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(redFlagPolicy.isRedFlag("unsafe advice")).thenReturn(true);
        when(contentReportRepository.existsByReporterUserIdAndTargetIdAndStatus(
                reporterId, targetId, ReportStatus.PENDING)).thenReturn(false);
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(invocation -> {
            ContentReport report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        newPolicy().autoReportIfRedFlag(reporterId, targetId, ReportTargetType.QUESTION, "unsafe advice");

        ArgumentCaptor<ContentReport> reportCaptor = ArgumentCaptor.forClass(ContentReport.class);
        verify(contentReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getReportSource()).isEqualTo(ReportSource.AUTOMATED);
        verify(auditService).log(eq(com.carebridge.backend.audit.entity.AuditAction.CONTENT_REPORTED),
                eq(reporterId), eq("ContentReport"), any(), any());
    }

    private CommunitySafetyPolicy newPolicy() {
        return new CommunitySafetyPolicy(userRepository, expertProfileRepository, questionRepository,
                answerRepository, contentReportRepository, redFlagPolicy, auditService);
    }
}
