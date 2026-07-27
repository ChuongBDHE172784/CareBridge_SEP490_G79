package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
public class ContentListResponse {

    private UUID id;
    private ContentType type;
    private String title;
    private String summary;
    private ContentStage stage;
    private UUID topicId;
    private List<UUID> tagIds;
    private Instant publishedAt;
}
