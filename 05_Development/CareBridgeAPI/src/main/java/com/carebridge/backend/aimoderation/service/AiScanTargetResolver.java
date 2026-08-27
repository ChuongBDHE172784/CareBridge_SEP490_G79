package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fetches the current text of a scannable target. Scope is deliberately limited to public
 * community content (QUESTION/ANSWER) and published library CONTENT — never private chat,
 * consultations, health memory, triage details or any consent-protected record.
 */
@Component
@RequiredArgsConstructor
public class AiScanTargetResolver {

    public record TargetContent(String text, String skipReason) {

        public boolean isPresent() {
            return skipReason == null;
        }

        public static TargetContent of(String text) {
            return new TargetContent(text, null);
        }

        public static TargetContent skipped(String reason) {
            return new TargetContent(null, reason);
        }
    }

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final ContentRepository contentRepository;

    @Transactional(readOnly = true)
    public TargetContent resolve(ReportTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case QUESTION -> questionRepository.findById(targetId)
                    .map(q -> q.getStatus() == QuestionStatus.DELETED
                            ? TargetContent.skipped("TARGET_GONE")
                            : TargetContent.of(joinTitleAndBody(q.getTitle(), q.getBody())))
                    .orElseGet(() -> TargetContent.skipped("TARGET_GONE"));
            case ANSWER -> answerRepository.findById(targetId)
                    .map(a -> a.getStatus() == AnswerStatus.DELETED
                            ? TargetContent.skipped("TARGET_GONE")
                            : TargetContent.of(a.getBody()))
                    .orElseGet(() -> TargetContent.skipped("TARGET_GONE"));
            case CONTENT -> contentRepository.findById(targetId)
                    .map(c -> c.getStatus() == ContentStatus.ARCHIVED || c.getStatus() == ContentStatus.DRAFT
                            ? TargetContent.skipped("TARGET_UNPUBLISHED")
                            : TargetContent.of(joinTitleAndBody(c.getTitle(), c.getBody())))
                    .orElseGet(() -> TargetContent.skipped("TARGET_GONE"));
            default -> TargetContent.skipped("UNSUPPORTED_TARGET");
        };
    }

    public static String joinTitleAndBody(String title, String body) {
        if (title == null || title.isBlank()) {
            return body != null ? body : "";
        }
        return body == null || body.isBlank() ? title : title + "\n" + body;
    }
}
