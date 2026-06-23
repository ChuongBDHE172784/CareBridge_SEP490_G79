package com.carebridge.backend.content.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.dto.response.ChecklistItemResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.mapper.ContentMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// CNT82-TC-003
class ContentMapperTest {

    private final ContentMapper contentMapper = new ContentMapper();

    private static final UUID CONTENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private ContentItem makeContentItem(ContentStatus status, ContentStage stage, ContentType type) {
        return ContentItem.builder()
                .id(CONTENT_ID)
                .type(type)
                .title("Test Content Title")
                .body("Test body content")
                .stage(stage)
                .status(status)
                .versionNo(1)
                .authorUserId(1L)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ── CNT82-TC-003a: toListResponse — authorId excluded (BR-PRIVACY) ────────
    @Test
    void toListResponse_shouldNotIncludeAuthorId() {
        ContentItem item = makeContentItem(ContentStatus.APPROVED, ContentStage.PREGNANCY, ContentType.ARTICLE);

        ContentListResponse response = contentMapper.toListResponse(item);

        assertThat(response.getId()).isEqualTo(item.getId());
        assertThat(response.getTitle()).isEqualTo(item.getTitle());
        assertThat(response.getStage()).isEqualTo(item.getStage());
        assertThat(response.getPublishedAt()).isEqualTo(item.getPublishedAt());
        // BR-PRIVACY: ContentListResponse must not declare authorId field
        assertThat(ContentListResponse.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("authorId");
    }

    @Test
    void toListResponse_shouldMapAllRequiredFields() {
        ContentItem item = makeContentItem(ContentStatus.APPROVED, ContentStage.PREGNANCY, ContentType.ARTICLE);

        ContentListResponse response = contentMapper.toListResponse(item);

        assertThat(response.getId()).isEqualTo(CONTENT_ID);
        assertThat(response.getType()).isEqualTo(ContentType.ARTICLE);
        assertThat(response.getTitle()).isEqualTo("Test Content Title");
        assertThat(response.getStage()).isEqualTo(ContentStage.PREGNANCY);
    }

    // ── CNT82-TC-003b: toDetailResponse — authorId excluded (BR-PRIVACY) ─────
    @Test
    void toDetailResponse_shouldNotIncludeAuthorId() {
        ContentItem item = makeContentItem(ContentStatus.APPROVED, ContentStage.PREGNANCY, ContentType.ARTICLE);

        ContentDetailResponse response = contentMapper.toDetailResponse(item);

        assertThat(ContentDetailResponse.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("authorId");
        assertThat(response.getBody()).isEqualTo(item.getBody());
        assertThat(response.getVersion()).isEqualTo(item.getVersionNo());
    }

    @Test
    void toDetailResponse_shouldMapVersionFromVersionNo() {
        ContentItem item = makeContentItem(ContentStatus.APPROVED, ContentStage.PREGNANCY, ContentType.FAQ);

        ContentDetailResponse response = contentMapper.toDetailResponse(item);

        assertThat(response.getVersion()).isEqualTo(1);
    }

    // ── toChecklistTemplateResponse — items mapped correctly ──────────────────
    @Test
    void toChecklistTemplateResponse_shouldMapItemsInOrder() {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .name("Checklist Test")
                .stage(ContentStage.PREGNANCY)
                .description("Description test")
                .createdAt(Instant.now())
                .build();

        ChecklistItem item1 = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .itemText("Item 1")
                .order(1)
                .isRequired(true)
                .createdAt(Instant.now())
                .build();

        ChecklistItem item2 = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .itemText("Item 2")
                .order(2)
                .isRequired(false)
                .createdAt(Instant.now())
                .build();

        ChecklistTemplateResponse response = contentMapper.toChecklistTemplateResponse(template, List.of(item1, item2));

        assertThat(response.getId()).isEqualTo(template.getId());
        assertThat(response.getName()).isEqualTo("Checklist Test");
        assertThat(response.getDescription()).isEqualTo("Description test");
        assertThat(response.getItems()).hasSize(2);

        ChecklistItemResponse firstItem = response.getItems().get(0);
        assertThat(firstItem.getOrder()).isEqualTo(1);
        assertThat(firstItem.getIsRequired()).isTrue();
        assertThat(firstItem.getItemText()).isEqualTo("Item 1");
    }

    @Test
    void toChecklistTemplateResponse_withEmptyItems_returnsEmptyList() {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .name("Empty Checklist")
                .stage(ContentStage.POSTPARTUM)
                .createdAt(Instant.now())
                .build();

        ChecklistTemplateResponse response = contentMapper.toChecklistTemplateResponse(template, List.of());

        assertThat(response.getItems()).isNotNull().isEmpty();
    }
}
