package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.entity.ContentStatus;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// BR-PRIVACY: authorId intentionally excluded
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDetailResponse {

    private UUID id;
    private ContentType type;
    private String title;
    private String body;
    private ContentStage stage;
    private UUID topicId;
    private Integer version;
    private ContentStatus status;
    private String sourceLabel;
    private Instant publishedAt;
    private Instant updatedAt;
    private Instant createdAt;
    private List<ContentSourceResponse> sources;
    private boolean contentStale;
}
