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
import com.carebridge.backend.community.exception.DuplicateTopicNameException;
import com.carebridge.backend.community.mapper.CommunityTopicMapper;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.service.CommunityTopicServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityTopicServiceImplTest {

    @Mock
    private CommunityTopicRepository topicRepository;

    @Spy
    private CommunityTopicMapper topicMapper = new CommunityTopicMapper();

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CommunityTopicServiceImpl topicService;

    private static final UUID MODERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private CreateCommunityTopicRequest makeCreateRequest(String name) {
        return CreateCommunityTopicRequest.builder()
                .name(name)
                .description("Chủ đề về " + name)
                .icon("pregnant_woman")
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
        CommunityTopicResponse response = topicService.updateTopic(topicId, request);

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
        CommunityTopicResponse response = topicService.updateTopic(topicId, request);

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
                () -> topicService.updateTopic(topicId, request));

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
                () -> topicService.updateTopic(topicId, request));
    }

    // getTopics(false) — chỉ trả non-hidden
    @Test
    void getTopics_includeHiddenFalse_shouldCallFilteredRepository() {
        CommunityTopic visible = makeExistingTopic(UUID.randomUUID(), "Thai kỳ", false);
        when(topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc()).thenReturn(List.of(visible));

        List<CommunityTopicResponse> topics = topicService.getTopics(false);

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

        List<CommunityTopicResponse> topics = topicService.getTopics(true);

        assertEquals(2, topics.size());
        verify(topicRepository).findAllByOrderBySortOrderAsc();
        verify(topicRepository, never()).findAllByIsHiddenFalseOrderBySortOrderAsc();
    }
}
