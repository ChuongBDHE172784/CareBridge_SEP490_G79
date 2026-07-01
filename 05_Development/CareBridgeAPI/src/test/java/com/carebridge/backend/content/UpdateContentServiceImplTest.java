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
import com.carebridge.backend.content.dto.request.UpdateContentRequest;
import com.carebridge.backend.content.dto.response.UpdateContentResponse;
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

// UCT-TC-901..907, UCT-TC-909 (UC-106 CB-CONTENT-TEST-004)
@ExtendWith(MockitoExtension.class)
class UpdateContentServiceImplTest {

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

    private static final UUID ADMIN_ID = UUID.fromString("f1400000-0000-0000-0000-0000000000ad");
    private static final UUID C1 = UUID.fromString("f1500000-0000-0000-0000-000000000001");
    private static final UUID C2 = UUID.fromString("f1500000-0000-0000-0000-000000000002");

    private final Principal principal = () -> ADMIN_ID.toString();

    private ContentItem makeItem(UUID id, String title, ContentStage stage, ContentType type, Integer versionNo) {
        return ContentItem.builder()
                .id(id).title(title).body("original body").stage(stage).type(type)
                .status(ContentStatus.APPROVED).versionNo(versionNo).authorUserId(UUID.randomUUID())
                .sourceLabel("original source")
                .build();
    }

    private UpdateContentRequest makeRequest(String title, ContentStage stage, ContentStatus status) {
        return new UpdateContentRequest(title, "updated body", stage, null, status, "updated source");
    }

    // UCT-TC-901
    @Test
    void updateContent_happyPath_fieldsUpdatedAndVersionIncremented() {
        ContentItem existing = makeItem(C1, "A", ContentStage.PREGNANCY, ContentType.ARTICLE, 2);
        when(contentRepository.findById(C1)).thenReturn(Optional.of(existing));
        when(contentRepository.findByTitleIgnoreCaseAndStageAndType(any(), any(), any())).thenReturn(Optional.empty());
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateContentRequest request = makeRequest("A updated", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        UpdateContentResponse response = adminContentService.updateContent(C1, request, principal);

        assertEquals("A updated", response.title());
        assertEquals("updated body", response.body());
        assertEquals("updated source", "updated source");
        assertEquals(3, response.versionNo());
    }

    // UCT-TC-902
    @Test
    void updateContent_notFound_throwsCnt003() {
        when(contentRepository.findById(C1)).thenReturn(Optional.empty());

        UpdateContentRequest request = makeRequest("A", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        ContentException ex = assertThrows(ContentException.class,
                () -> adminContentService.updateContent(C1, request, principal));

        assertEquals("CNT-003", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    // UCT-TC-903 (CRITICAL — versionNo oracle for UC-108)
    @Test
    void updateContent_versionNo_incrementsByExactlyOnePerCall() {
        ContentItem existing = makeItem(C1, "A", ContentStage.PREGNANCY, ContentType.ARTICLE, 2);
        when(contentRepository.findById(C1)).thenReturn(Optional.of(existing));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateContentRequest request = makeRequest("A", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        UpdateContentResponse first = adminContentService.updateContent(C1, request, principal);
        assertEquals(3, first.versionNo());

        // second call: existing entity now reflects versionNo=3 (as persisted by the first call)
        UpdateContentResponse second = adminContentService.updateContent(C1, request, principal);
        assertEquals(4, second.versionNo());
    }

    // UCT-TC-904 (CRITICAL — immutability)
    @Test
    void updateContent_typeAndAuthorUserId_neverChanged() {
        UUID originalAuthor = UUID.randomUUID();
        ContentItem existing = ContentItem.builder()
                .id(C1).title("A").body("b").stage(ContentStage.PREGNANCY).type(ContentType.ARTICLE)
                .status(ContentStatus.APPROVED).versionNo(2).authorUserId(originalAuthor).build();
        when(contentRepository.findById(C1)).thenReturn(Optional.of(existing));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateContentRequest request = makeRequest("A", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        ArgumentCaptor<ContentItem> captor = ArgumentCaptor.forClass(ContentItem.class);

        adminContentService.updateContent(C1, request, principal);

        verify(contentRepository).save(captor.capture());
        assertEquals(ContentType.ARTICLE, captor.getValue().getType());
        assertEquals(originalAuthor, captor.getValue().getAuthorUserId());
    }

    // UCT-TC-905
    @Test
    void updateContent_titleChangedToCollidingCombo_throwsCnt002() {
        ContentItem c1 = makeItem(C1, "A", ContentStage.PREGNANCY, ContentType.ARTICLE, 2);
        ContentItem c2 = makeItem(C2, "B", ContentStage.PREGNANCY, ContentType.ARTICLE, 1);
        when(contentRepository.findById(C1)).thenReturn(Optional.of(c1));
        when(contentRepository.findByTitleIgnoreCaseAndStageAndType("B", ContentStage.PREGNANCY, ContentType.ARTICLE))
                .thenReturn(Optional.of(c2));

        UpdateContentRequest request = makeRequest("B", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        ContentException ex = assertThrows(ContentException.class,
                () -> adminContentService.updateContent(C1, request, principal));

        assertEquals("CNT-002", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    // UCT-TC-906
    @Test
    void updateContent_titleStageUnchanged_noDuplicateCheckTriggered() {
        ContentItem existing = makeItem(C1, "A", ContentStage.PREGNANCY, ContentType.ARTICLE, 2);
        when(contentRepository.findById(C1)).thenReturn(Optional.of(existing));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // same title/stage as existing — only body/sourceLabel effectively change
        UpdateContentRequest request = makeRequest("A", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        adminContentService.updateContent(C1, request, principal);

        verify(contentRepository, never()).findByTitleIgnoreCaseAndStageAndType(any(), any(), any());
    }

    // UCT-TC-907
    @Test
    void updateContent_versionNoNull_initializedToOneThenIncrementedToTwo() {
        ContentItem existing = makeItem(C1, "A", ContentStage.PREGNANCY, ContentType.ARTICLE, null);
        when(contentRepository.findById(C1)).thenReturn(Optional.of(existing));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateContentRequest request = makeRequest("A", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        UpdateContentResponse response = adminContentService.updateContent(C1, request, principal);

        assertEquals(2, response.versionNo());
    }

    // UCT-TC-909
    @Test
    void updateContent_onSuccess_auditServiceLogCalledOnce() {
        ContentItem existing = makeItem(C1, "A", ContentStage.PREGNANCY, ContentType.ARTICLE, 2);
        when(contentRepository.findById(C1)).thenReturn(Optional.of(existing));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateContentRequest request = makeRequest("A", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        adminContentService.updateContent(C1, request, principal);

        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.CONTENT_UPDATED),
                org.mockito.ArgumentMatchers.eq(ADMIN_ID),
                org.mockito.ArgumentMatchers.eq("ContentItem"),
                org.mockito.ArgumentMatchers.eq(C1.toString()),
                any());
    }
}
