package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSearchRequest {

    private String keyword;
    private ContentStage stage;
    private UUID topicId;
    private ContentType type;
}
