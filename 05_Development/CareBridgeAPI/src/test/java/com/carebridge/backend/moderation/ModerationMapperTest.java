package com.carebridge.backend.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.mapper.ModerationMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// MOD-TC-SEC-004: full content body not exposed in response
class ModerationMapperTest {

    private final ModerationMapper mapper = new ModerationMapper();

    private ContentReport makeReport(UUID id, UUID targetId, ReportTargetType type) {
        ContentReport r = new ContentReport();
        r.setId(id);
        r.setTargetId(targetId);
        r.setTargetType(type);
        r.setStatus(ReportStatus.PENDING);
        r.setCategory("Inappropriate content");
        r.setCreatedAt(Instant.now());
        return r;
    }

    // MOD-TC-SEC-004: preview > 200 chars is truncated (C4 constraint, TDS §4.3 Security)
    @Test
    void toQueueItemResponse_longPreview_isTruncatedTo200Chars() {
        ContentReport report = makeReport(UUID.randomUUID(), UUID.randomUUID(), ReportTargetType.CONTENT);
        String longBody = "X".repeat(1000);

        ModerationQueueItemResponse result = mapper.toQueueItemResponse(report, longBody, 2L);

        // C4: preview must not expose full body
        assertThat(result.contentPreview()).hasSizeLessThanOrEqualTo(200);
        assertThat(result.contentPreview()).endsWith("...");
    }

    // MOD-TC-SEC-004: preview within 200 chars is not modified
    @Test
    void toQueueItemResponse_shortPreview_notTruncated() {
        ContentReport report = makeReport(UUID.randomUUID(), UUID.randomUUID(), ReportTargetType.QUESTION);
        String shortBody = "Short preview text";

        ModerationQueueItemResponse result = mapper.toQueueItemResponse(report, shortBody, 1L);

        assertThat(result.contentPreview()).isEqualTo(shortBody);
    }

    // Mapper correctly maps all fields from ContentReport
    @Test
    void toQueueItemResponse_mapsAllFieldsCorrectly() {
        UUID reportId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        ContentReport report = makeReport(reportId, targetId, ReportTargetType.ANSWER);

        ModerationQueueItemResponse result = mapper.toQueueItemResponse(report, "Preview", 5L);

        assertThat(result.id()).isEqualTo(reportId);
        assertThat(result.targetType()).isEqualTo(ReportTargetType.ANSWER);
        assertThat(result.reportCount()).isEqualTo(5L);
        assertThat(result.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(result.reportReason()).isEqualTo("Inappropriate content");
    }
}
