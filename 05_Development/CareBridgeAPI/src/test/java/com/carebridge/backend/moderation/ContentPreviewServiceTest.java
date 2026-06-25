package com.carebridge.backend.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentPreviewServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// MOD-TC-003: Preview truncation test
@ExtendWith(MockitoExtension.class)
class ContentPreviewServiceTest {

    @Mock
    private CommunityQuestionRepository questionRepository;

    @Mock
    private CommunityAnswerRepository answerRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private ContentPreviewServiceImpl contentPreviewService;

    // MOD-TC-003: content > 200 chars is truncated to <= 200 chars with "..."
    @Test
    void fetchPreview_longContent_isTruncatedTo200Chars() {
        // BR-MOD-002, C4: preview truncate at 200 chars
        UUID targetId = UUID.randomUUID();
        String longBody = "A".repeat(500);

        CommunityQuestion question = CommunityQuestion.builder()
                .id(targetId)
                .body(longBody)
                .title("Test question title")
                .build();
        when(questionRepository.findById(targetId)).thenReturn(Optional.of(question));

        String preview = contentPreviewService.fetchPreview(targetId, ReportTargetType.QUESTION);

        // C4: Preview must be <= 200 chars (§4.3 Security)
        assertThat(preview).hasSizeLessThanOrEqualTo(200);
        assertThat(preview).endsWith("...");
    }

    // MOD-TC-003 (variant): content <= 200 chars is not truncated
    @Test
    void fetchPreview_shortContent_notTruncated() {
        // C4: no truncation indicator when content is within limit
        UUID targetId = UUID.randomUUID();
        String shortBody = "Short body text";

        CommunityQuestion question = CommunityQuestion.builder()
                .id(targetId)
                .body(shortBody)
                .title("Test")
                .build();
        when(questionRepository.findById(targetId)).thenReturn(Optional.of(question));

        String preview = contentPreviewService.fetchPreview(targetId, ReportTargetType.QUESTION);

        assertThat(preview).isEqualTo(shortBody);
        assertThat(preview).doesNotEndWith("...");
    }

    // MOD-TC-003 (answer): fetch answer body preview
    @Test
    void fetchPreview_answerType_returnsAnswerBody() {
        UUID targetId = UUID.randomUUID();
        String body = "Answer body text that is short";

        CommunityAnswer answer = CommunityAnswer.builder()
                .id(targetId)
                .body(body)
                .build();
        when(answerRepository.findById(targetId)).thenReturn(Optional.of(answer));

        String preview = contentPreviewService.fetchPreview(targetId, ReportTargetType.ANSWER);

        assertThat(preview).isEqualTo(body);
    }

    // MOD-TC-003 (content item): fetch ContentItem title preview
    @Test
    void fetchPreview_contentType_returnsContentTitle() {
        UUID targetId = UUID.randomUUID();
        String title = "Article title";

        ContentItem item = ContentItem.builder()
                .id(targetId)
                .title(title)
                .body("Full body")
                .build();
        when(contentRepository.findById(targetId)).thenReturn(Optional.of(item));

        String preview = contentPreviewService.fetchPreview(targetId, ReportTargetType.CONTENT);

        assertThat(preview).isEqualTo(title);
    }

    // MOD-TC-003 (unknown target): returns empty string when target not found
    @Test
    void fetchPreview_targetNotFound_returnsEmptyString() {
        UUID targetId = UUID.randomUUID();
        when(questionRepository.findById(targetId)).thenReturn(Optional.empty());

        String preview = contentPreviewService.fetchPreview(targetId, ReportTargetType.QUESTION);

        assertThat(preview).isEmpty();
    }
}
