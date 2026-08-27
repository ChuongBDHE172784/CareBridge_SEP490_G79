package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// BR-PRIVACY: authorId intentionally excluded; topicName resolved from topicId
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentSearchResponse {

    private UUID id;
    private ContentType type;
    private String title;
    private ContentStage stage;
    private String topicName;
    private Instant publishedAt;
}
