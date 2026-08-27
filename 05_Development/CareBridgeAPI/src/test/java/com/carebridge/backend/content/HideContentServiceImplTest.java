package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.content.dto.request.HideContentRequest;
import com.carebridge.backend.content.dto.response.HideContentResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.AdminContentServiceImpl;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

// UCT-TC-1201a, 1201c, 1202, 1203, 1205, 1206 (service-level), 1207 (UC-107 CB-CONTENT-TEST-006)
// Note: TDS's UCT-TC-1201b (PENDING_REVIEW -> ARCHIVED) is NOT implemented — ContentStatus has no
// PENDING_REVIEW value in this codebase (verified — enum is DRAFT/APPROVED/ARCHIVED only). The transition
// guard is implemented as "any non-ARCHIVED status -> ARCHIVED" (ADR-002's actual intent), which is exercised
// here via the two statuses that do exist (DRAFT, APPROVED).
@ExtendWith(MockitoExtension.class)
class HideContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private CommunityTopicRepository communityTopicRepository;

    @Spy
    private ContentMapper contentMapper = new ContentMapper();

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminContentServiceImpl adminContentService;

    private static final UUID ADMIN_ID = UUID.fromString("f1700000-0000-0000-0000-0000000000ad");
    private static final UUID D1 = UUID.fromString("f1700000-0000-0000-0000-000000000001");
    private static final UUID D3 = UUID.fromString("f1700000-0000-0000-0000-000000000003");
    private static final UUID D4 = UUID.fromString("f1700000-0000-0000-0000-000000000004");

    private final Principal principal = () -> ADMIN_ID.toString();

    private ContentItem makeItem(UUID id, ContentStatus status) {
        return ContentItem.builder()
                .id(id).title("A").body("body text").stage(ContentStage.PREGNANCY).type(ContentType.ARTICLE)
                .status(status).versionNo(1).authorUserId(UUID.randomUUID()).build();
    }

    private HideContentRequest makeRequest(String reason) {
        return new HideContentRequest(reason);
    }

    // UCT-TC-1201a
    @Test
    void hideContent_draftToArchived_happyPath() {
        ContentItem item = makeItem(D1, ContentStatus.DRAFT);
        when(contentRepository.findById(D1)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        HideContentResponse response = adminContentService.hideContent(D1, makeRequest("Thông tin lỗi thời"), principal);

        assertEquals(ContentStatus.DRAFT, response.previousStatus());
        assertEquals(ContentStatus.ARCHIVED, response.newStatus());
        assertEquals("Thông tin lỗi thời", response.reason());
        assertEquals(ADMIN_ID, response.hiddenByAdminId());
    }

    // UCT-TC-1201c
    @Test
    void hideContent_approvedToArchived_happyPath() {
        ContentItem item = makeItem(D3, ContentStatus.APPROVED);
        when(contentRepository.findById(D3)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        HideContentResponse response = adminContentService.hideContent(D3, makeRequest("reason"), principal);

        assertEquals(ContentStatus.APPROVED, response.previousStatus());
        assertEquals(ContentStatus.ARCHIVED, response.newStatus());
    }

    // UCT-TC-1202
    @Test
    void hideContent_alreadyArchived_throwsCnt006() {
        ContentItem item = makeItem(D4, ContentStatus.ARCHIVED);
        when(contentRepository.findById(D4)).thenReturn(Optional.of(item));

        ContentException ex = assertThrows(ContentException.class,
                () -> adminContentService.hideContent(D4, makeRequest("reason"), principal));

        assertEquals("CNT-006", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    // UCT-TC-1203
    @Test
    void hideContent_notFound_throwsCnt003() {
        when(contentRepository.findById(D1)).thenReturn(Optional.empty());

        ContentException ex = assertThrows(ContentException.class,
                () -> adminContentService.hideContent(D1, makeRequest("reason"), principal));

        assertEquals("CNT-003", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    // UCT-TC-1205 (CRITICAL — field-preservation)
    @Test
    void hideContent_onlyStatusChanges_otherFieldsPreserved() {
        ContentItem item = makeItem(D3, ContentStatus.APPROVED);
        UUID originalAuthor = item.getAuthorUserId();
        when(contentRepository.findById(D3)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ContentItem> captor = ArgumentCaptor.forClass(ContentItem.class);
        adminContentService.hideContent(D3, makeRequest("reason"), principal);

        verify(contentRepository).save(captor.capture());
        ContentItem saved = captor.getValue();
        assertEquals("A", saved.getTitle());
        assertEquals("body text", saved.getBody());
        assertEquals(ContentStage.PREGNANCY, saved.getStage());
        assertEquals(ContentType.ARTICLE, saved.getType());
        assertEquals(1, saved.getVersionNo());
        assertEquals(originalAuthor, saved.getAuthorUserId());
        assertEquals(ContentStatus.ARCHIVED, saved.getStatus());
    }

    // UCT-TC-1206 (CRITICAL — adapted from Testcontainers row-count assertion to a code-level guarantee:
    // no Testcontainers/real-DB harness exists in this codebase, same finding as UC-100/101/102/106.
    // The direct code-level equivalent of "row never physically deleted" is that delete()/deleteById()
    // are never invoked on this path — verified here.)
    @Test
    void hideContent_neverCallsDelete() {
        ContentItem item = makeItem(D3, ContentStatus.APPROVED);
        when(contentRepository.findById(D3)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        adminContentService.hideContent(D3, makeRequest("reason"), principal);

        verify(contentRepository, never()).delete(any());
        verify(contentRepository, never()).deleteById(any());
        verify(contentRepository).save(any(ContentItem.class));
    }

    // UCT-TC-1207
    @Test
    void hideContent_onSuccess_auditServiceLogCalledOnceWithContentHidden() {
        ContentItem item = makeItem(D3, ContentStatus.APPROVED);
        when(contentRepository.findById(D3)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        adminContentService.hideContent(D3, makeRequest("reason"), principal);

        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.CONTENT_HIDDEN),
                org.mockito.ArgumentMatchers.eq(ADMIN_ID),
                org.mockito.ArgumentMatchers.eq("ContentItem"),
                org.mockito.ArgumentMatchers.eq(D3.toString()),
                any());
    }
}
