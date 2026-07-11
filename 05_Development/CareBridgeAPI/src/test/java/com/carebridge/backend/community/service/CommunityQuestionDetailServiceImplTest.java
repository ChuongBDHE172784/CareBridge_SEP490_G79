package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityQuestionDetailServiceImplTest {

    @Mock CommunityQuestionRepository questionRepository;
    @Mock CommunityTopicRepository topicRepository;
    @Mock CommunityAnswerRepository answerRepository;
    @Mock CommunityBookmarkRepository bookmarkRepository;
    @Mock CommunityAnswerLikeRepository answerLikeRepository;
    @Mock CommunityQuestionLikeRepository questionLikeRepository;
    @Mock CommunityQuestionMapper questionMapper;
    @Mock CommunityAnswerMapper answerMapper;
    @Mock AuditService auditService;
    @Mock CommunityAuthorDisplayResolver authorDisplayResolver;
    @InjectMocks CommunityQuestionServiceImpl questionService;

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @BeforeEach
    void setUp() {
        lenient().when(authorDisplayResolver.resolve(any())).thenReturn(null);
        lenient().when(authorDisplayResolver.resolveBatch(any())).thenReturn(Map.of());
    }

    private CommunityQuestion makeApprovedQuestion() {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .topicId(TOPIC_ID)
                .authorId(AUTHOR_ID)
                .title("Test Question Title")
                .body("Test question body content")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED)
                .anonymous(false)
                .answerCount(2)
                .likeCount(5)
                .createdAt(Instant.now())
                .build();
    }

    private CommunityTopic makeTopic() {
        return CommunityTopic.builder()
                .id(TOPIC_ID)
                .name("Dinh dưỡng")
                .isHidden(false)
                .build();
    }

    private CommunityAnswer makeAnswer(UUID questionId) {
        return CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(questionId)
                .authorId(UUID.randomUUID())
                .body("Answer body content")
                .status(AnswerStatus.APPROVED)
                .likeCount(1)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getQuestionDetail_approvedQuestion_returnsDetail() {
        CommunityQuestion question = makeApprovedQuestion();
        CommunityTopic topic = makeTopic();
        CommunityAnswer answer = makeAnswer(QUESTION_ID);
        CommunityAnswerResponse answerResponse = CommunityAnswerResponse.builder()
                .id(answer.getId()).questionId(QUESTION_ID).body(answer.getBody()).build();
        CommunityQuestionDetailResponse expectedDetail = CommunityQuestionDetailResponse.builder()
                .id(QUESTION_ID).title("Test Question Title").topicName("Dinh dưỡng")
                .answers(List.of(answerResponse)).build();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, AnswerStatus.APPROVED))
                .thenReturn(List.of(answer));
        when(answerLikeRepository.findLikedAnswerIds(eq(CURRENT_USER_ID), any())).thenReturn(Set.of());
        when(bookmarkRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(false);
        when(answerMapper.toResponse(eq(answer), any(), eq(false), any())).thenReturn(answerResponse);
        when(questionMapper.toDetailResponse(
                eq(question), eq("Dinh dưỡng"), any(), any(), eq(false), eq(false), eq(CURRENT_USER_ID)))
                .thenReturn(expectedDetail);

        CommunityQuestionDetailResponse result = questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(QUESTION_ID);
        assertThat(result.getTopicName()).isEqualTo("Dinh dưỡng");
        assertThat(result.getAnswers()).hasSize(1);
    }

    @Test
    void getQuestionDetail_questionNotFound_throwsNotFound() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessageContaining("COM-006");
    }

    @Test
    void getQuestionDetail_pendingQuestion_throwsNotFound() {
        CommunityQuestion pending = makeApprovedQuestion();
        pending.setStatus(QuestionStatus.PENDING);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void getQuestionDetail_hiddenQuestion_throwsNotFound() {
        CommunityQuestion hidden = makeApprovedQuestion();
        hidden.setStatus(QuestionStatus.HIDDEN);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void getQuestionDetail_anonymousQuestion_masksAuthorId() {
        CommunityQuestion question = makeApprovedQuestion();
        question.setAnonymous(true);
        CommunityTopic topic = makeTopic();
        CommunityQuestionDetailResponse maskedDetail = CommunityQuestionDetailResponse.builder()
                .id(QUESTION_ID).anonymous(true).authorId(null).topicName("Dinh dưỡng")
                .answers(List.of()).build();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, AnswerStatus.APPROVED))
                .thenReturn(List.of());
        when(answerLikeRepository.findLikedAnswerIds(eq(CURRENT_USER_ID), any())).thenReturn(Set.of());
        when(bookmarkRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionMapper.toDetailResponse(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(maskedDetail);

        CommunityQuestionDetailResponse result = questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID);

        assertThat(result.isAnonymous()).isTrue();
        assertThat(result.getAuthorId()).isNull();
    }

    @Test
    void getQuestionDetail_noAnswers_returnsEmptyAnswerList() {
        CommunityQuestion question = makeApprovedQuestion();
        CommunityTopic topic = makeTopic();
        CommunityQuestionDetailResponse detail = CommunityQuestionDetailResponse.builder()
                .id(QUESTION_ID).answers(List.of()).build();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, AnswerStatus.APPROVED))
                .thenReturn(List.of());
        when(answerLikeRepository.findLikedAnswerIds(eq(CURRENT_USER_ID), any())).thenReturn(Set.of());
        when(bookmarkRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionMapper.toDetailResponse(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(detail);

        CommunityQuestionDetailResponse result = questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID);

        assertThat(result.getAnswers()).isEmpty();
    }

    @Test
    void getQuestionDetail_topicNotFound_usesEmptyTopicName() {
        CommunityQuestion question = makeApprovedQuestion();
        CommunityQuestionDetailResponse detail = CommunityQuestionDetailResponse.builder()
                .id(QUESTION_ID).topicName("").answers(List.of()).build();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());
        when(answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, AnswerStatus.APPROVED))
                .thenReturn(List.of());
        when(answerLikeRepository.findLikedAnswerIds(eq(CURRENT_USER_ID), any())).thenReturn(Set.of());
        when(bookmarkRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionMapper.toDetailResponse(any(), eq(""), any(), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(detail);

        CommunityQuestionDetailResponse result = questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID);

        assertThat(result).isNotNull();
        verify(questionMapper).toDetailResponse(any(), eq(""), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    // New: bookmarked question hydrates isBookmarked=true (UC-58 hydration fix regression guard)
    @Test
    void getQuestionDetail_bookmarkedByCurrentUser_hydratesIsBookmarkedTrue() {
        CommunityQuestion question = makeApprovedQuestion();
        CommunityTopic topic = makeTopic();
        CommunityQuestionDetailResponse detail = CommunityQuestionDetailResponse.builder()
                .id(QUESTION_ID).isBookmarked(true).answers(List.of()).build();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, AnswerStatus.APPROVED))
                .thenReturn(List.of());
        when(answerLikeRepository.findLikedAnswerIds(eq(CURRENT_USER_ID), any())).thenReturn(Set.of());
        when(bookmarkRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(true);
        when(questionMapper.toDetailResponse(any(), any(), any(), any(), eq(true), anyBoolean(), any()))
                .thenReturn(detail);

        CommunityQuestionDetailResponse result = questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID);

        assertThat(result.isBookmarked()).isTrue();
        verify(questionMapper).toDetailResponse(any(), any(), any(), any(), eq(true), anyBoolean(), any());
    }

    // COMQL-TC-011: liked question hydrates isLiked=true for the current viewer
    @Test
    void getQuestionDetail_likedByCurrentUser_hydratesIsLikedTrue() {
        CommunityQuestion question = makeApprovedQuestion();
        CommunityTopic topic = makeTopic();
        CommunityQuestionDetailResponse detail = CommunityQuestionDetailResponse.builder()
                .id(QUESTION_ID).isLiked(true).answers(List.of()).build();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(answerRepository.findAllByQuestionIdAndStatusOrderByCreatedAtDesc(QUESTION_ID, AnswerStatus.APPROVED))
                .thenReturn(List.of());
        when(answerLikeRepository.findLikedAnswerIds(eq(CURRENT_USER_ID), any())).thenReturn(Set.of());
        when(bookmarkRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(false);
        when(questionLikeRepository.existsByUserIdAndQuestionId(CURRENT_USER_ID, QUESTION_ID)).thenReturn(true);
        when(questionMapper.toDetailResponse(any(), any(), any(), any(), anyBoolean(), eq(true), any()))
                .thenReturn(detail);

        CommunityQuestionDetailResponse result = questionService.getQuestionDetail(QUESTION_ID, CURRENT_USER_ID);

        assertThat(result.isLiked()).isTrue();
        verify(questionMapper).toDetailResponse(any(), any(), any(), any(), anyBoolean(), eq(true), any());
    }
}
