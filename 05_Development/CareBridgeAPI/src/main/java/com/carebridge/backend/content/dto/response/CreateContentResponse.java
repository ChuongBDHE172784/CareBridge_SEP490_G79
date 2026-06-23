package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateContentResponse {

    private UUID id;
    private ContentType type;
    private String title;
    private ContentStage stage;
    private String status;
    private Integer version;
    private Instant createdAt;
}
