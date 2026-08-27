package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.UnpublishRequest;
import com.carebridge.backend.content.dto.response.UnpublishResponse;
import com.carebridge.backend.content.entity.*;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ContentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentUnpublishServiceImpl implements ContentUnpublishService {
    private final ContentRepository contentRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public UnpublishResponse unpublish(UUID id, UnpublishRequest request, UUID adminId) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw ContentException.unpublishReasonRequired();
        }
        ContentItem item = contentRepository.findById(id).orElseThrow(ContentException::contentNotFound);
        if (item.getStatus() != ContentStatus.APPROVED) throw ContentException.notCurrentlyPublished();
        Instant publishedAt = item.getPublishedAt();
        item.setStatus(ContentStatus.ARCHIVED);
        ContentItem saved = contentRepository.save(item);
        Instant unpublishedAt = Instant.now();
        auditService.log(AuditAction.CONTENT_UNPUBLISHED, adminId, "CONTENT_ITEM", id.toString(), "reason=" + request.reason());
        return new UnpublishResponse(saved.getId(), ContentStatus.APPROVED, ContentStatus.ARCHIVED,
                publishedAt, adminId, request.reason(), unpublishedAt);
    }
}
