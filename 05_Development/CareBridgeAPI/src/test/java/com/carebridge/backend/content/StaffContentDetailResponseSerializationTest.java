package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StaffContentDetailResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ContentMapper mapper = new ContentMapper();

    @Test
    void publicDetailOmitsFeedbackWhileStaffDetailFlattensIt() throws Exception {
        ContentItem item = ContentItem.builder()
                .id(UUID.randomUUID())
                .type(ContentType.ARTICLE)
                .title("Nội dung cần sửa")
                .body("Nội dung")
                .stage(ContentStage.PREGNANCY)
                .status(ContentStatus.DRAFT)
                .versionNo(2)
                .revisionReason("Bổ sung nguồn")
                .revisionRequestedAt(Instant.parse("2026-07-27T10:00:00Z"))
                .revisionRequestedBy(UUID.randomUUID())
                .revisionRequestedVersion(2)
                .build();

        String publicJson = objectMapper.writeValueAsString(mapper.toDetailResponse(item));
        String staffJson = objectMapper.writeValueAsString(mapper.toStaffDetailResponse(item));

        assertFalse(publicJson.contains("latestReviewFeedback"));
        assertFalse(publicJson.contains("requestedBy"));
        assertTrue(staffJson.contains("\"title\":\"Nội dung cần sửa\""));
        assertTrue(staffJson.contains("\"latestReviewFeedback\""));
        assertTrue(staffJson.contains("\"reason\":\"Bổ sung nguồn\""));
    }
}

