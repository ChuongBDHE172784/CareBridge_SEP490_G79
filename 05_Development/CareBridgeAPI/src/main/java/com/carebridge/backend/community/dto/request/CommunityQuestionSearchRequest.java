package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import lombok.Data;

import java.util.UUID;

@Data
public class CommunityQuestionSearchRequest {
    private String keyword;
    private UUID topicId;
    private PregnancyStage stage;
    private UrgencyLevel urgency;
    private Boolean hasExpertAnswer;
    private int page = 0;
    private int size = 20;
}
