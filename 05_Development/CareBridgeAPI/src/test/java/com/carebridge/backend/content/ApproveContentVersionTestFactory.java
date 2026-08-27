package com.carebridge.backend.content;

import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import java.util.UUID;

// CASE 2.0 Props Isolation Boilerplate — CB-CONTENT-TEST-005 §4
final class ApproveContentVersionTestFactory {

    static final UUID ADMIN_ID = UUID.fromString("f1600000-0000-0000-0000-0000000000ad");
    static final UUID AUTHOR_ID = UUID.fromString("f1600000-0000-0000-0000-0000000000a1");
    static final UUID CONTENT_ID = UUID.fromString("f1700000-0000-0000-0000-000000000001");

    private ApproveContentVersionTestFactory() {
    }

    static ContentItem makeItem(ContentStatus status, Integer versionNo) {
        return ContentItem.builder()
                .id(CONTENT_ID)
                .title("Bài viết")
                .status(status)
                .versionNo(versionNo)
                .authorUserId(AUTHOR_ID)
                .type(ContentType.ARTICLE)
                .build();
    }

    static ContentDecisionRequest makeRequest(ContentDecision decision, String reason) {
        return new ContentDecisionRequest(decision, reason);
    }
}
