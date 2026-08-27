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
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.ImmutableTopicTypeException;
import com.carebridge.backend.community.exception.TopicHasDependentsException;
import com.carebridge.backend.community.mapper.CommunityTopicMapper;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.TopicQuestionCountProjection;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import com.carebridge.backend.community.util.SlugGenerator;
import com.carebridge.backend.recommendation.exception.RecommendationException;
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
        rejectReservedRecommendationSlug(topic.getSlug());
        topic = topicRepository.save(topic);
        auditService.log(AuditAction.MODERATION_ACTION, createdBy, "CommunityTopic", topic.getId().toString(), "created");
        return hydrateSingle(topic, createdBy);
    }

    @Override
    @Transactional
    public CommunityTopicResponse updateTopic(UUID id, UUID updatedBy, UpdateCommunityTopicRequest request) {
        CommunityTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community topic not found: " + id));

        // Catalog rows are immutable through the general topic API. Check the
        // persisted slug before applying a rename so a rec-* row cannot escape
        // the reserved namespace by changing its display name.
        rejectReservedRecommendationSlug(topic.getSlug());

        if (request.getType() != null && request.getType() != topic.getType()) {
            throw new ImmutableTopicTypeException(
                    "Community topic type is immutable after creation: " + topic.getType());
        }

        if (request.getName() != null
                && !request.getName().trim().equalsIgnoreCase(topic.getName())
                && topicRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateTopicNameException(request.getName());
        }

        TopicType effectiveType = topic.getType();
        UUID effectiveParentId = request.getParentId() != null ? request.getParentId() : topic.getParentId();
        validateHierarchy(effectiveType, effectiveParentId);

        boolean nameChanged = request.getName() != null
                && !request.getName().trim().equalsIgnoreCase(topic.getName());

        topicMapper.applyUpdate(topic, request);

        if (nameChanged) {
            topic.setSlug(resolveUniqueSlug(topic.getName(), id));
        }

        rejectReservedRecommendationSlug(topic.getSlug());

        topic = topicRepository.save(topic);
        auditService.log(AuditAction.MODERATION_ACTION, updatedBy, "CommunityTopic", id.toString(), "updated");
        return hydrateSingle(topic, updatedBy);
    }

    @Override
    @Transactional
    public void deleteTopic(UUID id, UUID deletedBy) {
        CommunityTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new CommunityTopicNotFoundException(id.toString()));

        rejectReservedRecommendationSlug(topic.getSlug());

        boolean hasChildren = topicRepository.existsByParentId(id);
        boolean hasQuestions = questionRepository.existsByTopicId(id);
        boolean hasFollows = topicFollowRepository.existsByTopicId(id);
        if (hasChildren || hasQuestions || hasFollows) {
            throw new TopicHasDependentsException(
                    "Community topic has dependents and cannot be deleted: " + id);
        }

        topicRepository.delete(topic);
        auditService.log(AuditAction.MODERATION_ACTION, deletedBy,
                "CommunityTopic", id.toString(), "deleted");
    }

    // ADR-COM-020: CATEGORY/TAG are roots; TOPIC requires an existing visible CATEGORY parent.
    private void validateHierarchy(TopicType type, UUID parentId) {
        if (type == TopicType.TOPIC) {
            if (parentId == null) {
                throw new InvalidTopicHierarchyException("A TOPIC must belong to a parent CATEGORY");
            }
            topicRepository.findByIdAndTypeAndIsHiddenFalse(parentId, TopicType.CATEGORY)
                    .orElseThrow(() -> new InvalidTopicHierarchyException(
                            "parentId must reference an existing, visible CATEGORY: " + parentId));
            return;
        }
        if (parentId != null) {
            throw new InvalidTopicHierarchyException(type + " cannot have a parent: " + parentId);
        }
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

    private void rejectReservedRecommendationSlug(String slug) {
        if (slug != null && slug.startsWith("rec-")) {
            throw new RecommendationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_TAG_NAMESPACE_RESERVED",
                    "The rec-* topic namespace is reserved for the recommendation catalog");
        }
    }

    private CommunityTopicResponse hydrateSingle(CommunityTopic topic, UUID currentUserId) {
        return hydrate(List.of(topic), currentUserId).get(0);
    }
}
