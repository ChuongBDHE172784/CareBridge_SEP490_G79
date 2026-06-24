package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationServiceImpl implements ModerationService {

    private final ContentReportRepository contentReportRepository;
    private final ContentPreviewService contentPreviewService;
    private final ModerationMapper moderationMapper;
    private final AuditService auditService;

    @Override
    public ModerationQueueResponse getModerationQueue(ModerationQueueFilter filter, Principal principal) {
        // C5: Always sort by createdAt DESC (BR-MOD-003)
        PageRequest pageable = PageRequest.of(
                filter.page(),
                filter.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // ADR-001: query ContentReport first, then fetch previews
        Page<ContentReport> page;
        if (filter.targetType() != null) {
            page = contentReportRepository.findByStatusAndTargetType(
                    filter.status(), filter.targetType(), pageable);
        } else {
            page = contentReportRepository.findByStatus(filter.status(), pageable);
        }

        List<ModerationQueueItemResponse> items = page.getContent().stream()
                .map(report -> {
                    // C4: preview truncated inside ContentPreviewService
                    String preview = contentPreviewService.fetchPreview(
                            report.getTargetId(), report.getTargetType());
                    long count = contentReportRepository.countByTargetIdAndStatus(
                            report.getTargetId(), report.getStatus());
                    return moderationMapper.toQueueItemResponse(report, preview, count);
                })
                .toList();

        // C2: AuditService.log() after every successful queue view (ADR-003)
        String userId = principal != null ? principal.getName() : null;
        auditService.log(AuditAction.MODERATION_QUEUE_VIEWED, userId, null,
                "filter=" + filter.targetType() + "/" + filter.status() + " count=" + page.getTotalElements());

        return moderationMapper.toQueueResponse(page, items);
    }
}
