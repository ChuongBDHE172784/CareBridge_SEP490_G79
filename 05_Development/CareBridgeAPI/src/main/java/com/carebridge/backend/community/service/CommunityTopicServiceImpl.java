package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.exception.DuplicateTopicNameException;
import com.carebridge.backend.community.exception.InvalidTopicHierarchyException;
import com.carebridge.backend.community.mapper.CommunityTopicMapper;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.TopicQuestionCountProjection;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import com.carebridge.backend.community.util.SlugGenerator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
    private final CommunityQuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityTopicResponse> getTopics(boolean includeHidden, TopicType type, UUID currentUserId) {
        List<CommunityTopic> topics = fetchTopics(includeHidden, type);
        return hydrate(topics, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityTopicResponse> searchTopics(String keyword, boolean includeHidden, TopicType type, UUID currentUserId) {
        if (keyword == null || keyword.isBlank()) {
            return getTopics(includeHidden, type, currentUserId);
        }
        String trimmedKeyword = keyword.trim();
        List<CommunityTopic> topics = includeHidden
                ? topicRepository.searchByKeywordIncludingHidden(trimmedKeyword)
                : topicRepository.searchByKeyword(trimmedKeyword);
        if (type != null) {
            topics = topics.stream().filter(t -> t.getType() == type).toList();
        }
        return hydrate(topics, currentUserId);
    }

    // type == null keeps the pre-existing (untyped) query methods for backward compatibility with
    // callers that don't care about taxonomy (e.g. existing tests / any caller predating ADR-COM-017).
    private List<CommunityTopic> fetchTopics(boolean includeHidden, TopicType type) {
        if (type == null) {
            return includeHidden
                    ? topicRepository.findAllByOrderBySortOrderAsc()
                    : topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc();
        }
        return includeHidden
                ? topicRepository.findAllByTypeOrderBySortOrderAsc(type)
                : topicRepository.findAllByIsHiddenFalseAndTypeOrderBySortOrderAsc(type);
    }

    // UC-171 hydration fix (batch follow state) + ADR-COM-015 (batch APPROVED question count) —
    // both computed with exactly one query each for the whole topic list, no N+1.
    private List<CommunityTopicResponse> hydrate(List<CommunityTopic> topics, UUID currentUserId) {
        if (topics.isEmpty()) {
            return List.of();
        }
        List<UUID> topicIds = topics.stream().map(CommunityTopic::getId).toList();
        Set<UUID> followedIds = topicFollowRepository.findFollowedTopicIds(currentUserId, topicIds);
        Map<UUID, Long> countByTopicId = questionRepository.countApprovedQuestionsByTopicIds(topicIds).stream()
                .collect(Collectors.toMap(TopicQuestionCountProjection::getTopicId, TopicQuestionCountProjection::getCnt));
        return topics.stream()
                .map(t -> topicMapper.toResponse(t, followedIds.contains(t.getId())).toBuilder()
                        .questionCount(countByTopicId.getOrDefault(t.getId(), 0L))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public CommunityTopicResponse createTopic(UUID createdBy, CreateCommunityTopicRequest request) {
        if (topicRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateTopicNameException(request.getName());
        }
        validateHierarchy(request.getType(), request.getParentId());

        CommunityTopic topic = topicMapper.toEntity(request, createdBy);
        topic.setSlug(resolveUniqueSlug(request.getName(), null));
        topic = topicRepository.save(topic);
        auditService.log(AuditAction.MODERATION_ACTION, createdBy, "CommunityTopic", topic.getId().toString(), "created");
        return hydrateSingle(topic, createdBy);
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

        TopicType effectiveType = request.getType() != null ? request.getType() : topic.getType();
        UUID effectiveParentId = request.getParentId() != null ? request.getParentId() : topic.getParentId();
        // If this request is switching type to TOPIC, parentId must not remain set — whether it
        // came from the request or is inherited from the existing entity (TDS §9.2).
        if (request.getType() == TopicType.TOPIC && effectiveParentId != null) {
            throw new InvalidTopicHierarchyException(
                    "Cannot set type=TOPIC while parentId is still set: " + effectiveParentId);
        }
        validateHierarchy(effectiveType, effectiveParentId);

        boolean nameChanged = request.getName() != null
                && !request.getName().trim().equalsIgnoreCase(topic.getName());

        topicMapper.applyUpdate(topic, request);

        if (nameChanged) {
            topic.setSlug(resolveUniqueSlug(topic.getName(), id));
        }

        topic = topicRepository.save(topic);
        auditService.log(AuditAction.MODERATION_ACTION, updatedBy, "CommunityTopic", id.toString(), "updated");
        return hydrateSingle(topic, updatedBy);
    }

    // ADR-COM-016 (revised): TOPIC must never have a parent. CATEGORY/TAG parentId is OPTIONAL —
    // if provided, it must reference an existing, non-hidden TOPIC (parentId=null is valid for
    // CATEGORY/TAG too, e.g. ContentCategoryController's flat categories).
    private void validateHierarchy(TopicType type, UUID parentId) {
        if (type == TopicType.TOPIC) {
            if (parentId != null) {
                throw new InvalidTopicHierarchyException("A TOPIC cannot have a parent: " + parentId);
            }
            return;
        }
        if (parentId == null) {
            return;
        }
        topicRepository.findByIdAndTypeAndIsHiddenFalse(parentId, TopicType.TOPIC)
                .orElseThrow(() -> new InvalidTopicHierarchyException(
                        "parentId must reference an existing, visible TOPIC: " + parentId));
    }

    // ADR-COM-018: base slug from the name, auto-suffixed -2, -3, ... on collision.
    // excludeId lets update() skip colliding with the topic's own current slug.
    private String resolveUniqueSlug(String name, UUID excludeId) {
        String base = SlugGenerator.generate(name);
        String candidate = base;
        int suffix = 2;
        Predicate<String> collides = excludeId == null
                ? topicRepository::existsBySlug
                : slug -> topicRepository.existsBySlugAndIdNot(slug, excludeId);
        while (collides.test(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private CommunityTopicResponse hydrateSingle(CommunityTopic topic, UUID currentUserId) {
        return hydrate(List.of(topic), currentUserId).get(0);
    }
}
