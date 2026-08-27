package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertContentApprovalQueueItem {
    private UUID id;
    private String kind; // "CONTENT" or "CHECKLIST"
    private ContentType type; // ARTICLE, FAQ, CHECKLIST
    private String title;
    private ContentStage stage;
    private String status;
    private Integer versionNo;
    private Long itemCount;
    private String summary;
    private String sourceLabel;
    private Instant assignedAt;
    private Instant updatedAt;
    private Instant createdAt;
}
