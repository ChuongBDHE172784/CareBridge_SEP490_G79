package com.carebridge.backend.content.service;

import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentPreviewServiceImpl implements ContentPreviewService {

    private static final int MAX_PREVIEW_LENGTH = 200;
    private static final String ELLIPSIS = "...";

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final ContentRepository contentRepository;

    @Override
    public String fetchPreview(UUID targetId, ReportTargetType targetType) {
        if (targetId == null || targetType == null) {
            return "";
        }
        String raw = switch (targetType) {
            case QUESTION -> questionRepository.findById(targetId)
                    .map(q -> q.getBody())
                    .orElse("");
            case ANSWER -> answerRepository.findById(targetId)
                    .map(a -> a.getBody())
                    .orElse("");
            case CONTENT -> contentRepository.findById(targetId)
                    .map(c -> c.getTitle() != null ? c.getTitle() : "")
                    .orElse("");
            // ACCOUNT/EXPERT/USER targets are User rows, not content — no preview text applies.
            case ACCOUNT, EXPERT, USER -> "";
        };
        return truncate(raw);
    }

    @Override
    public Map<UUID, String> batchFetchPreviews(Iterable<UUID> targetIds, ReportTargetType targetType) {
        Map<UUID, String> result = new LinkedHashMap<>();
        for (UUID id : targetIds) {
            result.put(id, fetchPreview(id, targetType));
        }
        return result;
    }

    private String truncate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= MAX_PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_PREVIEW_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
    }
}
