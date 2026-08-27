package com.carebridge.backend.integration.gemini.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAnswerResponse {

    private String answer;
    private String disclaimer;
    private List<RagSource> sources;
    private boolean fallback;
    private LocalDateTime generatedAt;
}
