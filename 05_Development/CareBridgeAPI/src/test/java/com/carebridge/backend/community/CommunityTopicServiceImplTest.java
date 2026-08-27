package com.carebridge.backend.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

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
import com.carebridge.backend.community.service.CommunityTopicServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.carebridge.backend.community.CommunityTopicTestFactory.makeCreateTopicRequest;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class CommunityTopicServiceImplTest {

    @Mock
    private CommunityTopicRepository topicRepository;

    @Spy
    private CommunityTopicMapper topicMapper = new CommunityTopicMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private UserTopicFollowRepository topicFollowRepository;

    @Mock
    private CommunityQuestionRepository questionRepository;

    @InjectMocks
    private CommunityTopicServiceImpl topicService;

    private static final UUID MODERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    private CreateCommunityTopicRequest makeCreateRequest(String name) {
        return CreateCommunityTopicRequest.builder()
                .name(name)
                .description("Chủ đề về " + name)
                .icon("pregnant_woman")
                .type(TopicType.CATEGORY)
                .sortOrder(1)
                .build();
    }

    private CommunityTopic makeExistingTopic(UUID id, String name, boolean hidden) {
        return CommunityTopic.builder()
                .id(id)
                .name(name)
                .description("Mô tả " + name)
                .type(TopicType.CATEGORY)
                .parentId(null)
                .isHidden(hidden)
                .sortOrder(1)
                .createdBy(MODERATOR_ID)
                .build();
    }

    // COM-TC-001: Tạo topic hợp lệ — Happy Path
    @Test
    void createTopic_validRequest_shouldPersistAndReturnResponse() {
        CreateCommunityTopicRequest request = makeCreateRequest("Thai kỳ khỏe mạnh");
        when(topicRepository.existsByNameIgnoreCase("Thai kỳ khỏe mạnh")).thenReturn(false);
        UUID generatedId = UUID.randomUUID();
        CommunityTopic saved = makeExistingTopic(generatedId, "Thai kỳ khỏe mạnh", false);
        when(topicRepository.save(any(CommunityTopic.class))).thenReturn(saved);

        CommunityTopicResponse response = topicService.createTopic(MODERATOR_ID, request);

        assertNotNull(response);
        assertEquals("Thai kỳ khỏe mạnh", response.getName());
        assertFalse(response.isHidden());
        verify(topicRepository).save(any(CommunityTopic.class));
        verify(auditService).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID),
                eq("CommunityTopic"), anyString(), eq("created"));
    }

    // COM-TC-001 sub: entity được tạo với createdBy = moderatorId, isHidden = false
    @Test
    void createTopic_validRequest_entityHasCorrectDefaults() {
        CreateCommunityTopicRequest request = makeCreateRequest("Chăm sóc bé");
        when(topicRepository.existsByNameIgnoreCase("Chăm sóc bé")).thenReturn(false);
        CommunityTopic saved = makeExistingTopic(UUID.randomUUID(), "Chăm sóc bé", false);
        when(topicRepository.save(any(CommunityTopic.class))).thenReturn(saved);

        topicService.createTopic(MODERATOR_ID, request);

        verify(topicRepository).save(any(CommunityTopic.class));
        // createdBy và isHidden được kiểm tra qua mapper spy
        verify(topicMapper).toEntity(request, MODERATOR_ID);
    }

    // COM-TC-002: name trùng (case-insensitive) → DuplicateTopicNameException
    @Test
    void createTopic_duplicateName_shouldThrowDuplicateTopicNameException() {
        CreateCommunityTopicRequest request = makeCreateRequest("dinh dưỡng");
        when(topicRepository.existsByNameIgnoreCase("dinh dưỡng")).thenReturn(true);

        assertThrows(DuplicateTopicNameException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));

        verify(topicRepository, never()).save(any());
    }

    // COM-TC-005: Ẩn topic — soft delete (isHidden=true, record still exists)
    @Test
    void updateTopic_hideRequest_shouldSetIsHiddenTrue() {
        UUID topicId = UUID.randomUUID();
        CommunityTopic existing = makeExistingTopic(topicId, "Dinh dưỡng", false);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .isHidden(true)
                .build();
        CommunityTopicResponse response = topicService.updateTopic(topicId, MODERATOR_ID, request);

        // Soft delete: isHidden=true, record not deleted
        verify(topicRepository).save(any(CommunityTopic.class));
        verify(topicRepository, never()).delete(any());
        assertEquals(topicId, response.getId());
    }

    // COM-TC-010: updateTopic — thay đổi name thành công
    @Test
    void updateTopic_nameChange_shouldUpdateAndReturn() {
        UUID topicId = UUID.randomUUID();
        CommunityTopic existing = makeExistingTopic(topicId, "Tên cũ", false);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));
        when(topicRepository.existsByNameIgnoreCaseAndIdNot("Tên mới", topicId)).thenReturn(false);
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .name("Tên mới")
                .build();
        CommunityTopicResponse response = topicService.updateTopic(topicId, MODERATOR_ID, request);

        assertEquals("Tên mới", response.getName());
        verify(topicRepository).save(any(CommunityTopic.class));
    }

    // updateTopic với name trùng → DuplicateTopicNameException
    @Test
    void updateTopic_nameConflict_shouldThrowDuplicateException() {
        UUID topicId = UUID.randomUUID();
        CommunityTopic existing = makeExistingTopic(topicId, "Topic A", false);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));
        when(topicRepository.existsByNameIgnoreCaseAndIdNot("CHĂM SÓC BÉ", topicId)).thenReturn(true);

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .name("CHĂM SÓC BÉ")
                .build();
        assertThrows(DuplicateTopicNameException.class,
                () -> topicService.updateTopic(topicId, MODERATOR_ID, request));

        verify(topicRepository, never()).save(any());
    }

    // updateTopic — topic không tồn tại → ResourceNotFoundException
    @Test
    void updateTopic_topicNotFound_shouldThrowResourceNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(topicRepository.findById(topicId)).thenReturn(Optional.empty());

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .isHidden(true)
                .build();
        assertThrows(ResourceNotFoundException.class,
                () -> topicService.updateTopic(topicId, MODERATOR_ID, request));
    }

    // getTopics(false) — chỉ trả non-hidden
    @Test
    void getTopics_includeHiddenFalse_shouldCallFilteredRepository() {
        CommunityTopic visible = makeExistingTopic(UUID.randomUUID(), "Thai kỳ", false);
        when(topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc()).thenReturn(List.of(visible));
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());

        List<CommunityTopicResponse> topics = topicService.getTopics(false, null, CURRENT_USER_ID);

        assertEquals(1, topics.size());
        verify(topicRepository).findAllByIsHiddenFalseOrderBySortOrderAsc();
        verify(topicRepository, never()).findAllByOrderBySortOrderAsc();
    }

    // getTopics(true) — trả tất cả kể cả hidden
    @Test
    void getTopics_includeHiddenTrue_shouldCallAllTopicsRepository() {
        CommunityTopic visible = makeExistingTopic(UUID.randomUUID(), "Thai kỳ", false);
        CommunityTopic hidden = makeExistingTopic(UUID.randomUUID(), "Ẩn", true);
        when(topicRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(visible, hidden));
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());

        List<CommunityTopicResponse> topics = topicService.getTopics(true, null, CURRENT_USER_ID);

        assertEquals(2, topics.size());
        verify(topicRepository).findAllByOrderBySortOrderAsc();
        verify(topicRepository, never()).findAllByIsHiddenFalseOrderBySortOrderAsc();
    }

    // ===================== CommunityTopicManagement (taxonomy: type/slug/parentId/questionCount) =====================

    // COM-TC-002: slug collision -> auto-suffix -2
    @Test
    void createTopic_slugCollision_shouldSuffixWithDash2() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(r -> r.name("Sức khỏe tinh thần"));
        when(topicRepository.existsByNameIgnoreCase("Sức khỏe tinh thần")).thenReturn(false);
        when(topicRepository.existsBySlug("suc-khoe-tinh-than")).thenReturn(true);
        when(topicRepository.existsBySlug("suc-khoe-tinh-than-2")).thenReturn(false);
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(this::echoWithGeneratedId);

        topicService.createTopic(MODERATOR_ID, request);

        org.mockito.ArgumentCaptor<CommunityTopic> captor = org.mockito.ArgumentCaptor.forClass(CommunityTopic.class);
        verify(topicRepository).save(captor.capture());
        assertEquals("suc-khoe-tinh-than-2", captor.getValue().getSlug());
    }

    // COM-TC-003: two consecutive slug collisions -> suffix -3
    @Test
    void createTopic_slugCollisionTwice_shouldSuffixWithDash3() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(r -> r.name("Sức khỏe tinh thần"));
        when(topicRepository.existsByNameIgnoreCase("Sức khỏe tinh thần")).thenReturn(false);
        when(topicRepository.existsBySlug("suc-khoe-tinh-than")).thenReturn(true);
        when(topicRepository.existsBySlug("suc-khoe-tinh-than-2")).thenReturn(true);
        when(topicRepository.existsBySlug("suc-khoe-tinh-than-3")).thenReturn(false);
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(this::echoWithGeneratedId);

        topicService.createTopic(MODERATOR_ID, request);

        org.mockito.ArgumentCaptor<CommunityTopic> captor = org.mockito.ArgumentCaptor.forClass(CommunityTopic.class);
        verify(topicRepository).save(captor.capture());
        assertEquals("suc-khoe-tinh-than-3", captor.getValue().getSlug());
    }

    // COM-TC-004: renaming a topic recomputes its slug
    @Test
    void updateTopic_nameChange_shouldRecomputeSlug() {
        UUID topicId = UUID.randomUUID();
        CommunityTopic existing = CommunityTopicTestFactory.makeCategory(t -> {
            t.setId(topicId);
            t.setName("Dinh dưỡng thai kỳ");
            t.setSlug("dinh-duong-thai-ky");
        });
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));
        when(topicRepository.existsByNameIgnoreCaseAndIdNot("Dinh dưỡng khi mang thai", topicId)).thenReturn(false);
        when(topicRepository.existsBySlugAndIdNot("dinh-duong-khi-mang-thai", topicId)).thenReturn(false);
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .name("Dinh dưỡng khi mang thai")
                .build();
        CommunityTopicResponse response = topicService.updateTopic(topicId, MODERATOR_ID, request);

        assertEquals("dinh-duong-khi-mang-thai", response.getSlug());
        verify(topicRepository).existsBySlugAndIdNot("dinh-duong-khi-mang-thai", topicId);
    }

    // COM-TC-005: TOPIC without its mandatory CATEGORY parent is rejected (ADR-COM-020)
    @Test
    void createTopic_topicTypeWithoutParentId_shouldThrowInvalidHierarchy() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(null));

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));

        verify(topicRepository, never()).save(any());
    }

    // COM-TC-006: CATEGORY is a root and therefore has no parentId.
    @Test
    void createTopic_categoryWithoutParentId_shouldSucceed() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.name("Ăn uống").type(TopicType.CATEGORY).parentId(null));
        when(topicRepository.existsByNameIgnoreCase("Ăn uống")).thenReturn(false);
        when(topicRepository.existsBySlug("an-uong")).thenReturn(false);
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(this::echoWithGeneratedId);

        CommunityTopicResponse response = topicService.createTopic(MODERATOR_ID, request);

        assertEquals(TopicType.CATEGORY, response.getType());
        assertNull(response.getParentId());
        verify(topicRepository).save(any(CommunityTopic.class));
    }

    // COM-TC-007: a TOPIC parent must resolve as a visible CATEGORY.
    @Test
    void createTopic_topicParentIsNotCategoryType_shouldThrowInvalidHierarchy() {
        UUID nonCategoryId = UUID.randomUUID();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(nonCategoryId));
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(nonCategoryId, TopicType.CATEGORY))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));

        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(nonCategoryId, TopicType.CATEGORY);
        verify(topicRepository, never()).save(any());
    }

    // COM-TC-008: parentId pointing to a hidden CATEGORY is rejected.
    @Test
    void createTopic_parentCategoryIsHidden_shouldThrowInvalidHierarchy() {
        UUID hiddenCategoryId = UUID.randomUUID();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(hiddenCategoryId));
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(hiddenCategoryId, TopicType.CATEGORY))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(hiddenCategoryId, TopicType.CATEGORY);
    }

    // COM-TC-009: valid TOPIC under an existing CATEGORY succeeds with questionCount=0.
    @Test
    void createTopic_validTopicUnderCategory_shouldSucceedWithZeroQuestionCount() {
        CommunityTopic parentCategory = CommunityTopicTestFactory.makeCategory();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.name("Dinh dưỡng thai kỳ").type(TopicType.TOPIC).parentId(parentCategory.getId()));
        when(topicRepository.existsByNameIgnoreCase("Dinh dưỡng thai kỳ")).thenReturn(false);
        when(topicRepository.existsBySlug("dinh-duong-thai-ky")).thenReturn(false);
        when(topicRepository.findByIdAndTypeAndIsHiddenFalse(parentCategory.getId(), TopicType.CATEGORY))
                .thenReturn(Optional.of(parentCategory));
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(this::echoWithGeneratedId);

        CommunityTopicResponse response = topicService.createTopic(MODERATOR_ID, request);

        assertEquals(TopicType.TOPIC, response.getType());
        assertEquals(parentCategory.getId(), response.getParentId());
        assertNotNull(response.getSlug());
        assertEquals(0L, response.getQuestionCount());
        assertFalse(response.isHidden());
    }

    // COM-TC-010: questionCount only counts APPROVED questions, batched (no N+1)
    @Test
    void getTopics_shouldHydrateQuestionCountFromApprovedOnly() {
        CommunityTopic topic = CommunityTopicTestFactory.makeTopic();
        when(topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc()).thenReturn(List.of(topic));
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());
        TopicQuestionCountProjection projection = mockProjection(topic.getId(), 3L);
        when(questionRepository.countApprovedQuestionsByTopicIds(anyList())).thenReturn(List.of(projection));

        List<CommunityTopicResponse> topics = topicService.getTopics(false, null, CURRENT_USER_ID);

        assertEquals(3L, topics.get(0).getQuestionCount());
        verify(questionRepository, org.mockito.Mockito.times(1)).countApprovedQuestionsByTopicIds(anyList());
    }

    // COM-TC-012: type is immutable after creation (COM-017).
    @Test
    void updateTopic_changeType_shouldThrowImmutableTypeError() {
        UUID topicId = UUID.randomUUID();
        CommunityTopic existing = CommunityTopicTestFactory.makeCategory();
        existing.setId(topicId);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .type(TopicType.TOPIC)
                .parentId(UUID.randomUUID())
                .build();

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> topicService.updateTopic(topicId, MODERATOR_ID, request));
        assertEquals("ImmutableTopicTypeException", error.getClass().getSimpleName());

        verify(topicRepository, never()).save(any());
    }

    // COM-TC-020: Amendment 2 contract — TOPIC cannot be a root.
    @Test
    void createTopic_amendment2TopicWithoutParent_shouldReject() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(null));

        assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
        verify(topicRepository, never()).save(any());
    }

    // COM-TC-021: TAG/TOPIC cannot serve as a TOPIC's parent.
    @Test
    void createTopic_amendment2NonCategoryParent_shouldReject() {
        UUID parentId = UUID.randomUUID();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(parentId));
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(parentId, TopicType.CATEGORY))
                .thenReturn(Optional.empty());

        assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(parentId, TopicType.CATEGORY);
        verify(topicRepository, never()).save(any());
    }

    // COM-TC-022: hidden CATEGORY is not a valid parent.
    @Test
    void createTopic_amendment2HiddenCategoryParent_shouldReject() {
        UUID parentId = UUID.randomUUID();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(parentId));
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(parentId, TopicType.CATEGORY))
                .thenReturn(Optional.empty());

        assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(parentId, TopicType.CATEGORY);
    }

    // COM-TC-023: valid visible CATEGORY parent is accepted.
    @Test
    void createTopic_amendment2VisibleCategoryParent_shouldPersist() {
        CommunityTopic category = CommunityTopicTestFactory.makeCategory();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.name("Dinh dưỡng thai kỳ").type(TopicType.TOPIC).parentId(category.getId()));
        when(topicRepository.findByIdAndTypeAndIsHiddenFalse(category.getId(), TopicType.CATEGORY))
                .thenReturn(Optional.of(category));
        when(topicRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(topicRepository.existsBySlug("dinh-duong-thai-ky")).thenReturn(false);
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(this::echoWithGeneratedId);

        CommunityTopicResponse response = topicService.createTopic(MODERATOR_ID, request);

        assertEquals(TopicType.TOPIC, response.getType());
        assertEquals(category.getId(), response.getParentId());
    }

    // COM-TC-024: CATEGORY is always top-level.
    @Test
    void createTopic_amendment2CategoryWithParent_shouldReject() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.CATEGORY).parentId(UUID.randomUUID()));

        assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
        verify(topicRepository, never()).save(any());
    }

    // COM-TC-025: TAG remains flat.
    @Test
    void createTopic_amendment2TagWithParent_shouldReject() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TAG).parentId(UUID.randomUUID()));

        assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
        verify(topicRepository, never()).save(any());
    }

    // COM-TC-027: child rows block deletion for every target type.
    @Test
    void deleteTopic_withChildRow_shouldReject() {
        CommunityTopic target = CommunityTopicTestFactory.makeCategory();
        when(topicRepository.findById(target.getId())).thenReturn(Optional.of(target));
        stubBooleanMethod(topicRepository, "existsByParentId", target.getId(), true);

        assertDeleteThrowsNamed(target.getId(), "TopicHasDependentsException");
        verify(topicRepository, never()).delete(any());
    }

    // COM-TC-028/030: a row of any type with zero dependents can be deleted.
    @Test
    void deleteTopic_withoutDependents_shouldDeleteAndAudit() {
        CommunityTopic target = CommunityTopicTestFactory.makeTopic();
        when(topicRepository.findById(target.getId())).thenReturn(Optional.of(target));
        stubAllDependencyChecks(target.getId(), false, false, false);

        invokeDelete(target.getId());

        verify(topicRepository).delete(target);
        verify(auditService).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID),
                eq("CommunityTopic"), eq(target.getId().toString()), anyString());
    }

    // COM-TC-029: question dependency blocks deletion.
    @Test
    void deleteTopic_withQuestion_shouldReject() {
        CommunityTopic target = CommunityTopicTestFactory.makeTopic();
        when(topicRepository.findById(target.getId())).thenReturn(Optional.of(target));
        stubAllDependencyChecks(target.getId(), false, true, false);

        assertDeleteThrowsNamed(target.getId(), "TopicHasDependentsException");
        verify(topicRepository, never()).delete(any());
    }

    // COM-TC-032: active follow dependency blocks TOPIC deletion.
    @Test
    void deleteTopic_withFollow_shouldReject() {
        CommunityTopic target = CommunityTopicTestFactory.makeTopic();
        when(topicRepository.findById(target.getId())).thenReturn(Optional.of(target));
        stubAllDependencyChecks(target.getId(), false, false, true);

        assertDeleteThrowsNamed(target.getId(), "TopicHasDependentsException");
        verify(topicRepository, never()).delete(any());
    }

    // COM-TC-033: legacy CATEGORY/TAG question/follow dependencies are type-agnostic.
    @Test
    void deleteTopic_legacyNonTopicDependencies_shouldRejectForEveryTypeAndKind() {
        for (TopicType type : List.of(TopicType.CATEGORY, TopicType.TAG)) {
            for (int dependentKind = 0; dependentKind < 2; dependentKind++) {
                CommunityTopic target = CommunityTopicTestFactory.makeCategory(t -> t.setType(type));
                when(topicRepository.findById(target.getId())).thenReturn(Optional.of(target));
                stubAllDependencyChecks(target.getId(), false, dependentKind == 0, dependentKind == 1);

                assertDeleteThrowsNamed(target.getId(), "TopicHasDependentsException");
            }
        }
        verify(topicRepository, never()).delete(any());
    }

    private void stubAllDependencyChecks(UUID id, boolean children, boolean questions, boolean follows) {
        stubBooleanMethod(topicRepository, "existsByParentId", id, children);
        stubBooleanMethod(questionRepository, "existsByTopicId", id, questions);
        stubBooleanMethod(topicFollowRepository, "existsByTopicId", id, follows);
    }

    private void stubBooleanMethod(Object mock, String methodName, UUID id, boolean result) {
        try {
            Method method = mock.getClass().getMethod(methodName, UUID.class);
            org.mockito.Mockito.when(method.invoke(mock, id)).thenReturn(result);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Missing planned repository contract: " + methodName + "(UUID)", e);
        }
    }

    private void assertDeleteThrowsNamed(UUID id, String expectedSimpleName) {
        Throwable error = assertThrows(Throwable.class, () -> invokeDelete(id));
        assertEquals(expectedSimpleName, error.getClass().getSimpleName());
    }

    private void invokeDelete(UUID id) {
        try {
            Method method = topicService.getClass().getMethod("deleteTopic", UUID.class, UUID.class);
            method.invoke(topicService, id, MODERATOR_ID);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Missing planned service contract: deleteTopic(UUID, UUID)", e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }

    private TopicQuestionCountProjection mockProjection(UUID topicId, long cnt) {
        TopicQuestionCountProjection projection = org.mockito.Mockito.mock(TopicQuestionCountProjection.class);
        when(projection.getTopicId()).thenReturn(topicId);
        when(projection.getCnt()).thenReturn(cnt);
        return projection;
    }

    // Mirrors what real JPA save() does for a brand-new entity (assigns @GeneratedValue id) —
    // the mocked repository just echoes the argument, so the id must be filled in here.
    private CommunityTopic echoWithGeneratedId(org.mockito.invocation.InvocationOnMock invocation) {
        CommunityTopic entity = invocation.getArgument(0);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return entity;
    }
}
