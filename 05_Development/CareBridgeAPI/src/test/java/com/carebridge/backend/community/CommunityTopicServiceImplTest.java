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
                .type(TopicType.TOPIC)
                .sortOrder(1)
                .build();
    }

    private CommunityTopic makeExistingTopic(UUID id, String name, boolean hidden) {
        return CommunityTopic.builder()
                .id(id)
                .name(name)
                .description("Mô tả " + name)
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
        CommunityTopic existing = CommunityTopicTestFactory.makeTopic(t -> {
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

    // COM-TC-005: TOPIC with a parentId is rejected
    @Test
    void createTopic_topicTypeWithParentId_shouldThrowInvalidHierarchy() {
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TOPIC).parentId(UUID.randomUUID()));

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));

        verify(topicRepository, never()).save(any());
    }

    // COM-TC-006 (ADR-COM-016 revised): CATEGORY without a parentId is ACCEPTED — parentId is optional
    // so the pre-existing ContentCategoryController (flat categories, no parent) keeps working.
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

    // COM-TC-007: parentId pointing to a non-TOPIC entity is rejected
    @Test
    void createTopic_parentIsNotTopicType_shouldThrowInvalidHierarchy() {
        UUID categoryId = UUID.randomUUID();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.TAG).parentId(categoryId));
        when(topicRepository.findByIdAndTypeAndIsHiddenFalse(categoryId, TopicType.TOPIC))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));

        verify(topicRepository, never()).save(any());
    }

    // COM-TC-008: parentId pointing to a hidden TOPIC is rejected
    @Test
    void createTopic_parentTopicIsHidden_shouldThrowInvalidHierarchy() {
        UUID hiddenTopicId = UUID.randomUUID();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.type(TopicType.CATEGORY).parentId(hiddenTopicId));
        when(topicRepository.findByIdAndTypeAndIsHiddenFalse(hiddenTopicId, TopicType.TOPIC))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.createTopic(MODERATOR_ID, request));
    }

    // COM-TC-009: valid CATEGORY under an existing TOPIC succeeds with questionCount=0
    @Test
    void createTopic_validCategoryUnderTopic_shouldSucceedWithZeroQuestionCount() {
        CommunityTopic parentTopic = CommunityTopicTestFactory.makeTopic();
        CreateCommunityTopicRequest request = makeCreateTopicRequest(
                r -> r.name("Ăn uống").type(TopicType.CATEGORY).parentId(parentTopic.getId()));
        when(topicRepository.existsByNameIgnoreCase("Ăn uống")).thenReturn(false);
        when(topicRepository.existsBySlug("an-uong")).thenReturn(false);
        when(topicRepository.findByIdAndTypeAndIsHiddenFalse(parentTopic.getId(), TopicType.TOPIC))
                .thenReturn(Optional.of(parentTopic));
        when(topicRepository.save(any(CommunityTopic.class))).thenAnswer(this::echoWithGeneratedId);

        CommunityTopicResponse response = topicService.createTopic(MODERATOR_ID, request);

        assertEquals(TopicType.CATEGORY, response.getType());
        assertEquals(parentTopic.getId(), response.getParentId());
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

    // COM-TC-012: changing type to TOPIC while parentId is still set in the same request is rejected
    @Test
    void updateTopic_changeToTopicWithParentIdStillSet_shouldThrowInvalidHierarchy() {
        UUID topicId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        CommunityTopic existing = CommunityTopicTestFactory.makeCategory(parentId);
        existing.setId(topicId);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));

        UpdateCommunityTopicRequest request = UpdateCommunityTopicRequest.builder()
                .type(TopicType.TOPIC)
                .parentId(parentId)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(InvalidTopicHierarchyException.class,
                () -> topicService.updateTopic(topicId, MODERATOR_ID, request));

        verify(topicRepository, never()).save(any());
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
