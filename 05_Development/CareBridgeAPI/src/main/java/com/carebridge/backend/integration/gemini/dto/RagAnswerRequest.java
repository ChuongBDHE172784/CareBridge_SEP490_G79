package com.carebridge.backend.integration.gemini.dto;

import java.util.List;
import java.util.Map;
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

    // Conversation context below is optional and used only by the maternal RAG
    // service. The assistant answers a follow-up like "còn bao lâu nữa?" very
    // differently with the preceding turns and the gestational week in hand, so
    // the client sends them and the backend forwards them unchanged.

    /** Current gestational week, when the caller is a mother who has one. */
    private Integer gestationalAgeWeeks;

    /** Free-form onboarding survey answers (history, risk factors). */
    private Map<String, Object> surveyProfile;

    /** Latest vitals the mother logged, for context rather than for screening. */
    private Map<String, Object> recentMetrics;

    /** Earlier turns of this chat, oldest first. */
    private List<ConversationTurn> conversationHistory;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationTurn {
        /** "user" or "assistant". */
        private String role;
        private String content;
    }
}
