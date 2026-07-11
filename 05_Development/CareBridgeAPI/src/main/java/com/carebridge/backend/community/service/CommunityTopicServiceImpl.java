package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.exception.DuplicateTopicNameException;
import com.carebridge.backend.community.mapper.CommunityTopicMapper;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityTopicServiceImpl implements CommunityTopicService {

    private final CommunityTopicRepository topicRepository;
    private final CommunityTopicMapper topicMapper;
    private final AuditService auditService;
    private final UserTopicFollowRepository topicFollowRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityTopicResponse> getTopics(boolean includeHidden, UUID currentUserId) {
        List<CommunityTopic> topics = includeHidden
                ? topicRepository.findAllByOrderBySortOrderAsc()
                : topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc();
        return toResponsesWithFollowState(topics, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityTopicResponse> searchTopics(String keyword, boolean includeHidden, UUID currentUserId) {
        if (keyword == null || keyword.isBlank()) {
            return getTopics(includeHidden, currentUserId);
        }
        String trimmedKeyword = keyword.trim();
        List<CommunityTopic> topics = includeHidden
                ? topicRepository.searchByKeywordIncludingHidden(trimmedKeyword)
                : topicRepository.searchByKeyword(trimmedKeyword);
        return toResponsesWithFollowState(topics, currentUserId);
    }

    // UC-171 hydration fix: batch-check follow state to avoid N+1
    private List<CommunityTopicResponse> toResponsesWithFollowState(List<CommunityTopic> topics, UUID currentUserId) {
        List<UUID> topicIds = topics.stream().map(CommunityTopic::getId).toList();
        Set<UUID> followedIds = topicFollowRepository.findFollowedTopicIds(currentUserId, topicIds);
        return topics.stream()
                .map(t -> topicMapper.toResponse(t, followedIds.contains(t.getId())))
                .toList();
    }

    @Override
    @Transactional
    public CommunityTopicResponse createTopic(UUID createdBy, CreateCommunityTopicRequest request) {
        if (topicRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateTopicNameException(request.getName());
        }
        CommunityTopic topic = topicMapper.toEntity(request, createdBy);
        topic = topicRepository.save(topic);
        auditService.log(AuditAction.MODERATION_ACTION, createdBy, "CommunityTopic", topic.getId().toString(), "created");
        return topicMapper.toResponse(topic);
    }

    @Override
    @Transactional
    public CommunityTopicResponse updateTopic(UUID id, UUID updatedBy, UpdateCommunityTopicRequest request) {
        CommunityTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community topic not found: " + id));

        if (request.getName() != null
                && !request.getName().trim().equalsIgnoreCase(topic.getName())
                && topicRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateTopicNameException(request.getName());
        }

        topicMapper.applyUpdate(topic, request);
        topic = topicRepository.save(topic);
        auditService.log(AuditAction.MODERATION_ACTION, updatedBy, "CommunityTopic", id.toString(), "updated");
        return topicMapper.toResponse(topic);
    }
}
