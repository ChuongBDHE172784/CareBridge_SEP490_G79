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

    /**
     * Set by the maternal RAG service when the question carries danger signs that
     * belong in front of a clinician rather than an assistant. The mobile client
     * turns this into the escalation prompt, so it has to survive the hop through
     * the backend instead of being dropped on the floor.
     */
    private boolean needExpertConsultation;

    /** Danger signs severe enough to warrant going in, not just consulting. */
    private boolean hasCriticalWarning;

    /** Follow-up questions the assistant offers so the mother can keep going. */
    private List<String> suggestedFollowups;
}
