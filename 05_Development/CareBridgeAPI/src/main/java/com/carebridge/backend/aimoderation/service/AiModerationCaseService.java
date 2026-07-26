package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.CasePriority;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates or attaches the moderation case for a non-SAFE assessment (ADR-005 attach-first
 * dedup). A pre-existing open case — user-reported or automated — is reused: the assessment
 * links to it and only its priority may be raised. report_source, reason_code, reporter and
 * description of a USER case are never overwritten, so "Người dùng + AI" provenance stays
 * intact. AI can only ever open a PENDING case; it never hides content or touches accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiModerationCaseService {

    private static final List<ReportStatus> OPEN_STATUSES = List.of(ReportStatus.PENDING, ReportStatus.IN_REVIEW);
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ContentReportRepository contentReportRepository;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID createOrAttachCase(ReportTargetType targetType, UUID targetId, CaseDecision decision,
            UUID assessmentId, String explanation) {
        List<ContentReport> openCases =
                contentReportRepository.findOpenCasesForUpdate(targetId, targetType, OPEN_STATUSES);
        if (!openCases.isEmpty()) {
            ContentReport existing = openCases.get(0);
            if (rank(decision.priority()) > rank(existing.getPriority())) {
                existing.setPriority(decision.priority());
                contentReportRepository.save(existing);
            }
            auditService.log(AuditAction.AI_CASE_CREATED, (UUID) null, "ContentReport",
                    existing.getId().toString(),
                    "attached=true assessmentId=" + assessmentId + " policyCode=" + decision.primaryPolicyCode()
                            + " priority=" + decision.priority());
            return existing.getId();
        }

        ContentReport created = contentReportRepository.save(ContentReport.builder()
                .targetId(targetId)
                .targetType(targetType)
                .status(ReportStatus.PENDING)
                .reportSource(ReportSource.AUTOMATED)
                .category(decision.reportCategory().name())
                .description(truncate(explanation))
                .priority(decision.priority())
                .reporterUserId(null)
                .createdAt(Instant.now())
                .build());

        auditService.log(AuditAction.AI_CASE_CREATED, (UUID) null, "ContentReport",
                created.getId().toString(),
                "attached=false assessmentId=" + assessmentId + " targetType=" + targetType
                        + " targetId=" + targetId + " policyCode=" + decision.primaryPolicyCode()
                        + " priority=" + decision.priority());
        return created.getId();
    }

    private static int rank(CasePriority priority) {
        return priority == null ? 0 : priority.ordinal();
    }

    private static String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "Được hệ thống AI đánh dấu để kiểm duyệt viên xem xét";
        }
        return text.length() <= MAX_DESCRIPTION_LENGTH ? text : text.substring(0, MAX_DESCRIPTION_LENGTH);
    }
}
