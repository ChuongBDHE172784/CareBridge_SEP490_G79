package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminContentServiceImpl implements AdminContentService {

    private final ContentRepository contentRepository;
    private final CommunityTopicRepository communityTopicRepository;
    private final ContentMapper contentMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public CreateContentResponse createContent(CreateContentRequest request, java.util.UUID authorUserId) {
        if (request.getTopicId() != null
                && !communityTopicRepository.existsById(request.getTopicId())) {
            throw ContentException.topicNotFound(request.getTopicId().toString());
        }

        contentRepository.findByTitleIgnoreCaseAndStageAndType(
                        request.getTitle(), request.getStage(), request.getType())
                .ifPresent(existing -> {
                    throw ContentException.duplicateContent();
                });

        ContentItem entity = contentMapper.toEntity(request, authorUserId);
        entity = contentRepository.save(entity);

        auditService.log(AuditAction.CONTENT_CREATED, authorUserId,
                "ContentItem", entity.getId().toString(), "created");

        return contentMapper.toCreateResponse(entity);
    }
}
