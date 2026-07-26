package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.CreateReportRequest;
import com.carebridge.backend.content.dto.response.CreateReportResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ReportException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-14 Report Content or Account (CB-MOD-IMP-014 §8/§17 C1-C8). Rolling 24h rate limit
 * (max 5 reports per reporter+target) and anti-duplicate (max 1 PENDING report per
 * reporter+target) are enforced via {@link ContentReportRepository}'s dedicated indexed
 * queries (idx_content_reports_rate_limit / idx_content_reports_duplicate).
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int MAX_REPORTS_PER_24H = 5;

    private final ContentReportRepository contentReportRepository;
    private final CommunityQuestionRepository communityQuestionRepository;
    private final CommunityAnswerRepository communityAnswerRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public CreateReportResponse createReport(CreateReportRequest request, UUID reporterUserId) {
        validateTarget(request.getTargetType(), request.getTargetId(), reporterUserId);

        Instant since = Instant.now().minus(Duration.ofHours(24));
        int recentCount = contentReportRepository.countByReporterUserIdAndTargetIdAndCreatedAtAfter(
                reporterUserId, request.getTargetId(), since);
        if (recentCount >= MAX_REPORTS_PER_24H) {
            throw ReportException.rateLimitExceeded();
        }

        boolean duplicatePending = contentReportRepository.existsByReporterUserIdAndTargetIdAndStatusIn(
                reporterUserId, request.getTargetId(),
                java.util.List.of(ReportStatus.PENDING, ReportStatus.IN_REVIEW));
        if (duplicatePending) {
            throw ReportException.duplicatePending();
        }

        ContentReport report = ContentReport.builder()
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .status(ReportStatus.PENDING)
                .category(request.getCategory().name())
                .description(request.getDescription())
                .reporterUserId(reporterUserId)
                .createdAt(Instant.now())
                .build();

        ContentReport saved = contentReportRepository.save(report);

        auditService.log(AuditAction.CONTENT_REPORTED, reporterUserId, "ContentReport",
                saved.getId().toString(),
                "targetType=" + saved.getTargetType() + " targetId=" + saved.getTargetId());

        return CreateReportResponse.builder()
                .reportId(saved.getId())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private void validateTarget(ReportTargetType targetType, UUID targetId, UUID reporterUserId) {
        boolean exists = switch (targetType) {
            case QUESTION -> communityQuestionRepository.findById(targetId)
                    .filter(q -> q.getStatus() == QuestionStatus.APPROVED
                            || (q.getStatus() == QuestionStatus.PENDING
                            && q.getAuthorId().equals(reporterUserId)))
                    .isPresent();
            case ANSWER -> communityAnswerRepository.findById(targetId)
                    .filter(a -> a.getStatus() == AnswerStatus.APPROVED)
                    .map(CommunityAnswer::getQuestionId)
                    .flatMap(communityQuestionRepository::findById)
                    .filter(q -> q.getStatus() == QuestionStatus.APPROVED)
                    .isPresent();
            case CONTENT -> contentRepository.existsById(targetId);
            case EXPERT, USER -> userRepository.existsById(targetId);
            case ACCOUNT -> throw ReportException.targetNotFound(targetId.toString());
        };
        if (!exists) {
            throw ReportException.targetNotFound(targetId.toString());
        }
    }
}
