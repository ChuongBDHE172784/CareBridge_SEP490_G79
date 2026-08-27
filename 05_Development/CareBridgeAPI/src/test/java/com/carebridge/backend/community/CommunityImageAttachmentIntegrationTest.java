package com.carebridge.backend.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CommunityImageAttachmentIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CommunityQuestionRepository questionRepository;
    @Autowired private CommunityAnswerRepository answerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void imageUrls_questionAndAnswer_roundTripThroughSharedCommunityContentTable() {
        UUID authorId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, authorId, "Community image author", null, "MOTHER");
        List<String> questionImages = List.of(
                "https://res.cloudinary.com/demo/image/upload/question.jpg");
        List<String> answerImages = List.of(
                "https://res.cloudinary.com/demo/image/upload/answer.jpg");

        CommunityQuestion question = questionRepository.saveAndFlush(CommunityQuestion.builder()
                .authorId(authorId)
                .title("Question with an image")
                .body("Question body with enough detail")
                .status(QuestionStatus.APPROVED)
                .imageUrls(questionImages)
                .build());
        CommunityAnswer answer = answerRepository.saveAndFlush(CommunityAnswer.builder()
                .questionId(question.getId())
                .authorId(authorId)
                .body("Answer body with enough detail")
                .status(AnswerStatus.APPROVED)
                .imageUrls(answerImages)
                .build());

        assertThat(questionRepository.findById(question.getId()).orElseThrow().getImageUrls())
                .containsExactlyElementsOf(questionImages);
        assertThat(answerRepository.findById(answer.getId()).orElseThrow().getImageUrls())
                .containsExactlyElementsOf(answerImages);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT jsonb_array_length(image_urls) FROM community_content WHERE content_id = ?",
                Integer.class,
                question.getId()))
                .isEqualTo(1);
    }
}
