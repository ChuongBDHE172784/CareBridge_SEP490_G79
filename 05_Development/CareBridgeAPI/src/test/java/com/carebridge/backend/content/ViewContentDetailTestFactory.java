package com.carebridge.backend.content;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public final class ViewContentDetailTestFactory {
    public static final UUID CONTENT_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    public static final UUID TOPIC_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID AUTHOR_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    private ViewContentDetailTestFactory() {}

    public static ContentItem makeApprovedContent() {
        return ContentItem.builder().id(CONTENT_ID).type(ContentType.ARTICLE)
                .title("Dinh dưỡng thai kỳ").body("Nội dung chi tiết về dinh dưỡng...")
                .stage(ContentStage.PREGNANCY).topicId(TOPIC_ID).status(ContentStatus.APPROVED)
                .versionNo(1).sourceLabel("WHO Guidelines 2024").authorUserId(AUTHOR_ID)
                .sources(List.of())
                .publishedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(30, ChronoUnit.DAYS)).build();
    }

    public static ContentItem makeStaleContent() {
        ContentItem item = makeApprovedContent();
        item.setUpdatedAt(Instant.now().minus(400, ChronoUnit.DAYS));
        return item;
    }

    public static ContentItem makeContentWithNullSourceLabel() {
        ContentItem item = makeApprovedContent();
        item.setSourceLabel(null);
        return item;
    }

    public static ContentItem makeContentWithNullUpdatedAt() {
        ContentItem item = makeApprovedContent();
        item.setUpdatedAt(null);
        return item;
    }
}
