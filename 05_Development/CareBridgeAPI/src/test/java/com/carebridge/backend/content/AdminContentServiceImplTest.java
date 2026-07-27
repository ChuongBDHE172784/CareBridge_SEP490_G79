package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.request.UpdateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.policy.HtmlContentSanitizer;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.AdminContentServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private CommunityTopicRepository communityTopicRepository;

    @Spy
    private ContentMapper contentMapper = new ContentMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private HtmlContentSanitizer htmlContentSanitizer;

    @InjectMocks
    private AdminContentServiceImpl adminContentService;

    // RTE-TC-007 covers the "sanitizer actually used" assertion explicitly; other tests in this
    // class don't care about body content, so default to identity to avoid noise.
    @BeforeEach
    void stubSanitizerAsIdentity() {
        lenient().when(htmlContentSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private CreateContentRequest makeRequest(ContentType type, ContentStage stage, UUID topicId) {
        return CreateContentRequest.builder()
                .type(type)
                .title("Dinh dưỡng thai kỳ tuần 12")
                .body("Nội dung chi tiết...")
                .stage(stage)
                .topicId(topicId)
                .build();
    }

    private ContentItem makeSavedEntity(UUID id, ContentType type, ContentStage stage) {
        return ContentItem.builder()
                .id(id)
                .type(type)
                .title("Dinh dưỡng thai kỳ tuần 12")
                .body("Nội dung chi tiết...")
                .stage(stage)
                .status(ContentStatus.DRAFT)
                .versionNo(1)
                .authorUserId(ADMIN_USER_ID)
                .createdAt(Instant.now())
                .build();
    }

    // CNT-TC-001: Tạo ARTICLE hợp lệ — Happy Path (TC-COND-001)
    @Test
    void createContent_validArticle_returnsResponseWithDraftStatusAndVersion1() {
        UUID topicId = UUID.randomUUID();
        CreateContentRequest request = makeRequest(ContentType.ARTICLE, ContentStage.PREGNANCY, topicId);

        when(communityTopicRepository.existsById(topicId)).thenReturn(true);
        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID savedId = UUID.randomUUID();
        when(contentRepository.save(any(ContentItem.class)))
                .thenReturn(makeSavedEntity(savedId, ContentType.ARTICLE, ContentStage.PREGNANCY));

        CreateContentResponse response = adminContentService.createContent(request, ADMIN_USER_ID);

        assertNotNull(response.getId());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(1, response.getVersion());
        assertEquals(ContentType.ARTICLE, response.getType());
        assertEquals(ContentStage.PREGNANCY, response.getStage());
        verify(contentRepository).save(any(ContentItem.class));
        verify(auditService).log(
                eq(AuditAction.CONTENT_CREATED),
                eq(ADMIN_USER_ID),
                eq("ContentItem"),
                anyString(),
                eq("created"));
    }

    // CNT-TC-002: Tạo FAQ hợp lệ — topicId null (TC-COND-002)
    @Test
    void createContent_validFaqWithoutTopicId_returnsSuccess() {
        CreateContentRequest request = makeRequest(ContentType.FAQ, ContentStage.POSTPARTUM, null);

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID savedId = UUID.randomUUID();
        when(contentRepository.save(any(ContentItem.class)))
                .thenReturn(makeSavedEntity(savedId, ContentType.FAQ, ContentStage.POSTPARTUM));

        CreateContentResponse response = adminContentService.createContent(request, ADMIN_USER_ID);

        assertEquals("DRAFT", response.getStatus());
        assertEquals(ContentType.FAQ, response.getType());
        assertEquals(savedId, response.getId());
        verify(communityTopicRepository, never()).existsById(any());
    }

    // CNT-TC-003: Tạo CHECKLIST hợp lệ — body optional (TC-COND-003)
    @Test
    void createContent_validChecklist_returnsSuccess() {
        CreateContentRequest request = CreateContentRequest.builder()
                .type(ContentType.CHECKLIST)
                .title("Checklist khám thai tháng 1")
                .stage(ContentStage.BABY_CARE)
                .build();

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID savedId = UUID.randomUUID();
        ContentItem saved = ContentItem.builder()
                .id(savedId)
                .type(ContentType.CHECKLIST)
                .title("Checklist khám thai tháng 1")
                .stage(ContentStage.BABY_CARE)
                .status(ContentStatus.DRAFT)
                .versionNo(1)
                .authorUserId(ADMIN_USER_ID)
                .createdAt(Instant.now())
                .build();
        when(contentRepository.save(any(ContentItem.class))).thenReturn(saved);

        CreateContentResponse response = adminContentService.createContent(request, ADMIN_USER_ID);

        assertEquals("DRAFT", response.getStatus());
        assertEquals(1, response.getVersion());
        assertEquals(ContentType.CHECKLIST, response.getType());
    }

    // CNT-TC-004: status invariant — entity được save luôn có status=DRAFT, version=1 (TC-COND-004)
    @Test
    void createContent_entityPassedToSave_alwaysHasDraftStatusAndVersion1() {
        CreateContentRequest request = makeRequest(ContentType.ARTICLE, ContentStage.PREGNANCY, null);

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID savedId = UUID.randomUUID();
        when(contentRepository.save(any(ContentItem.class)))
                .thenReturn(makeSavedEntity(savedId, ContentType.ARTICLE, ContentStage.PREGNANCY));

        adminContentService.createContent(request, ADMIN_USER_ID);

        ArgumentCaptor<ContentItem> captor = forClass(ContentItem.class);
        verify(contentRepository).save(captor.capture());
        assertEquals(ContentStatus.DRAFT, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getVersionNo());
    }

    // CNT-TC-004 sub: authorUserId trong entity = param, không phải từ request
    @Test
    void createContent_authorUserIdSetFromParam_notFromRequest() {
        CreateContentRequest request = makeRequest(ContentType.FAQ, ContentStage.PREGNANCY, null);

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID savedId = UUID.randomUUID();
        when(contentRepository.save(any(ContentItem.class)))
                .thenReturn(makeSavedEntity(savedId, ContentType.FAQ, ContentStage.PREGNANCY));

        adminContentService.createContent(request, ADMIN_USER_ID);

        ArgumentCaptor<ContentItem> captor = forClass(ContentItem.class);
        verify(contentRepository).save(captor.capture());
        assertEquals(ADMIN_USER_ID, captor.getValue().getAuthorUserId());
    }

    // CNT-TC-008: topicId không tồn tại → ContentException CNT-003 (TC-COND-008)
    @Test
    void createContent_topicIdNotFound_throwsContentExceptionCnt003() {
        UUID nonExistentTopicId = UUID.randomUUID();
        CreateContentRequest request = makeRequest(ContentType.ARTICLE, ContentStage.PREGNANCY, nonExistentTopicId);

        when(communityTopicRepository.existsById(nonExistentTopicId)).thenReturn(false);

        ContentException ex = assertThrows(ContentException.class,
                () -> adminContentService.createContent(request, ADMIN_USER_ID));

        assertEquals("CNT-003", ex.getCode());
        verify(contentRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // Duplicate title+stage+type → ContentException CNT-002 (TC-COND-...; TC-INT-003 service layer)
    @Test
    void createContent_duplicateTitleStageType_throwsContentExceptionCnt002() {
        CreateContentRequest request = makeRequest(ContentType.ARTICLE, ContentStage.PREGNANCY, null);

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.of(makeSavedEntity(UUID.randomUUID(), ContentType.ARTICLE, ContentStage.PREGNANCY)));

        ContentException ex = assertThrows(ContentException.class,
                () -> adminContentService.createContent(request, ADMIN_USER_ID));

        assertEquals("CNT-002", ex.getCode());
        verify(contentRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // AuditService.log gọi sau save thành công (CNT-TC-INT-001 service layer)
    @Test
    void createContent_onSuccess_auditServiceLogCalledWithContentCreated() {
        CreateContentRequest request = makeRequest(ContentType.ARTICLE, ContentStage.PREGNANCY, null);

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        UUID savedId = UUID.randomUUID();
        ContentItem saved = makeSavedEntity(savedId, ContentType.ARTICLE, ContentStage.PREGNANCY);
        when(contentRepository.save(any(ContentItem.class))).thenReturn(saved);

        adminContentService.createContent(request, ADMIN_USER_ID);

        verify(auditService).log(
                AuditAction.CONTENT_CREATED,
                ADMIN_USER_ID,
                "ContentItem",
                savedId.toString(),
                "created");
    }

    // RTE-TC-007: createContent() gọi sanitizer đúng 1 lần, entity lưu dùng OUTPUT của sanitizer
    // chứ không phải input thô — xem ContentRichTextEditor_Test-Spec.md
    @Test
    void createContent_bodySanitized_savedEntityUsesSanitizerOutputNotRawInput() {
        CreateContentRequest request = makeRequest(ContentType.ARTICLE, ContentStage.PREGNANCY, null);
        String sanitizedBody = "<p>an toàn</p>";
        when(htmlContentSanitizer.sanitize("Nội dung chi tiết...")).thenReturn(sanitizedBody);

        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> {
            ContentItem arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return arg;
        });

        adminContentService.createContent(request, ADMIN_USER_ID);

        verify(htmlContentSanitizer).sanitize("Nội dung chi tiết...");
        ArgumentCaptor<ContentItem> captor = forClass(ContentItem.class);
        verify(contentRepository).save(captor.capture());
        assertEquals(sanitizedBody, captor.getValue().getBody());
    }

    @Test
    void updateContent_resubmitReturnedDraft_clearsLatestReviewFeedback() {
        UUID contentId = UUID.randomUUID();
        ContentItem item = ContentItem.builder()
                .id(contentId)
                .type(ContentType.ARTICLE)
                .title("Nội dung cần sửa")
                .body("Nội dung")
                .stage(ContentStage.PREGNANCY)
                .status(ContentStatus.DRAFT)
                .versionNo(2)
                .revisionReason("Bổ sung nguồn")
                .revisionRequestedAt(Instant.now())
                .revisionRequestedBy(UUID.randomUUID())
                .revisionRequestedVersion(2)
                .build();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        adminContentService.updateContent(contentId, new UpdateContentRequest(
                item.getTitle(), item.getBody(), item.getStage(), null,
                ContentStatus.PENDING_REVIEW, null, null), () -> ADMIN_USER_ID.toString());

        assertEquals(ContentStatus.PENDING_REVIEW, item.getStatus());
        assertEquals(null, item.getRevisionReason());
        assertEquals(null, item.getRevisionRequestedAt());
        assertEquals(null, item.getRevisionRequestedBy());
        assertEquals(null, item.getRevisionRequestedVersion());
    }

    @Test
    void updateContent_saveReturnedDraft_retainsLatestReviewFeedback() {
        UUID contentId = UUID.randomUUID();
        Instant requestedAt = Instant.now();
        UUID requestedBy = UUID.randomUUID();
        ContentItem item = ContentItem.builder()
                .id(contentId).type(ContentType.FAQ).title("FAQ cần sửa").body("Nội dung")
                .stage(ContentStage.PREGNANCY).status(ContentStatus.DRAFT).versionNo(2)
                .revisionReason("Viết rõ câu trả lời").revisionRequestedAt(requestedAt)
                .revisionRequestedBy(requestedBy).revisionRequestedVersion(2).build();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        adminContentService.updateContent(contentId, new UpdateContentRequest(
                item.getTitle(), item.getBody(), item.getStage(), null,
                ContentStatus.DRAFT, null, null), () -> ADMIN_USER_ID.toString());

        assertEquals("Viết rõ câu trả lời", item.getRevisionReason());
        assertEquals(requestedAt, item.getRevisionRequestedAt());
        assertEquals(requestedBy, item.getRevisionRequestedBy());
        assertEquals(2, item.getRevisionRequestedVersion());
    }
}
