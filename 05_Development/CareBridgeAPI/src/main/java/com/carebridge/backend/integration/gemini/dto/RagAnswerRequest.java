package com.carebridge.backend.integration.gemini.dto;

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
public class RagAnswerRequest {

    private String query;
    private UserStage userStage;
    private UUID topicId;
    private Integer maxContextChunks;
}
