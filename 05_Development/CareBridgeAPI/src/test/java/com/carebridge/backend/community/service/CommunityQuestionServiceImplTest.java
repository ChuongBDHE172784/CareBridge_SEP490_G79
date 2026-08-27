package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.aimoderation.service.AiScanEnqueueService.EnqueueResult;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.file.service.IFileService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CommunityQuestionServiceImplTest {

    @Mock
    private CommunityQuestionRepository questionRepository;

    @Mock
    private CommunityTopicRepository topicRepository;

    @Mock
    private CommunityAnswerRepository answerRepository;

    @Mock
    private CommunityBookmarkRepository bookmarkRepository;

    @Mock
    private CommunityAnswerLikeRepository answerLikeRepository;

    @Mock
    private CommunityQuestionLikeRepository questionLikeRepository;

    @Spy
    private CommunityQuestionMapper questionMapper = new CommunityQuestionMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private CommunitySafetyPolicy communitySafetyPolicy;

    @Mock
    private CommunityAuthorDisplayResolver authorDisplayResolver;

    @Mock
    private ExpertProfileRepository expertProfileRepository;

    @Mock
    private com.carebridge.backend.aimoderation.service.AiScanEnqueueService aiScanEnqueueService;

    @Mock
    private IFileService fileService;

    @InjectMocks
    private CommunityQuestionServiceImpl questionService;

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CreateCommunityQuestionRequest makeRequest() {
        CreateCommunityQuestionRequest req = new CreateCommunityQuestionRequest();
        req.setTopicId(TOPIC_ID);
        req.setTitle("Valid test question title");
        req.setBody("This is a valid test body with enough characters");
        req.setStage(PregnancyStage.PREGNANCY);
        req.setPregnancyWeek(20);
        req.setUrgency(UrgencyLevel.NORMAL);
        req.setIsAnonymous(false);
        return req;
    }

    private CreateCommunityQuestionRequest makeRequest(Consumer<CreateCommunityQuestionRequest> overrides) {
        CreateCommunityQuestionRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }

    private CommunityTopic makeTopic(boolean isHidden) {
        return CommunityTopic.builder()
                .id(TOPIC_ID)
                .name("Thai kỳ")
                .type(TopicType.TOPIC)
                .parentId(UUID.randomUUID())
                .isHidden(isHidden)
                .build();
    }

    private CommunityQuestion savedQuestion() {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(TOPIC_ID)
                .authorId(AUTHOR_ID)
                .title("Valid test question title")
                .body("This is a valid test body with enough characters")
                .stage(PregnancyStage.PREGNANCY)
                .pregnancyWeek((short) 20)
                .urgency(UrgencyLevel.NORMAL)
                .anonymous(false)
                .status(QuestionStatus.AI_PENDING)
                .likeCount(0)
                .answerCount(0)
                .build();
    }

    // AI-first: storage starts AI_PENDING while the backward-compatible API says PENDING.
    @Test
    void createQuestion_validRequest_returnsPendingStatus() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(makeTopic(false)));
        CommunityQuestion saved = savedQuestion();
        when(questionRepository.save(any())).thenReturn(saved);

        CommunityQuestionResponse response = questionService.createQuestion(AUTHOR_ID, makeRequest());

        // AI_PENDING is private and must not enter the Moderator PENDING queue.
        verify(questionRepository, times(1)).save(argThat(q ->
                q.getStatus() == QuestionStatus.AI_PENDING && q.getAuthorId().equals(AUTHOR_ID)
        ));
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void createQuestion_aiUnavailable_entersHumanPendingQueue() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(makeTopic(false)));
        when(questionRepository.save(any())).thenAnswer(inv -> {
            CommunityQuestion question = inv.getArgument(0);
            if (question.getId() == null) question.setId(UUID.randomUUID());
            return question;
        });
        when(aiScanEnqueueService.enqueueScan(any(), any(), any()))
                .thenReturn(EnqueueResult.HUMAN_REVIEW_REQUIRED);

        CommunityQuestionResponse response = questionService.createQuestion(AUTHOR_ID, makeRequest());

        verify(questionRepository, times(2)).save(any());
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void createQuestion_withImagesVerifiesTheyBelongToAuthor() {
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/question.jpg";
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(makeTopic(false)));
        when(questionRepository.save(any())).thenReturn(savedQuestion());
        CreateCommunityQuestionRequest request = makeRequest(req -> req.setImageUrls(List.of(imageUrl)));

        questionService.createQuestion(AUTHOR_ID, request);

        verify(fileService).assertCommunityImagesOwned(List.of(imageUrl), AUTHOR_ID);
    }

    // COM-TC-001 sub: entity defaults — likeCount=0, answerCount=0
    @Test
    void createQuestion_validRequest_entityHasZeroCountDefaults() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(makeTopic(false)));
        CommunityQuestion saved = savedQuestion();
        when(questionRepository.save(any())).thenReturn(saved);

        questionService.createQuestion(AUTHOR_ID, makeRequest());

        verify(questionRepository).save(argThat(q ->
                q.getLikeCount() == 0 && q.getAnswerCount() == 0
        ));
    }

    // COM-TC-001 sub: audit event emitted after successful creation
    @Test
    void createQuestion_validRequest_auditLogEmitted() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(makeTopic(false)));
        CommunityQuestion saved = savedQuestion();
        when(questionRepository.save(any())).thenReturn(saved);

        questionService.createQuestion(AUTHOR_ID, makeRequest());

        verify(auditService).log(eq(AuditAction.COMMUNITY_QUESTION_CREATED), eq(AUTHOR_ID),
                eq("CommunityQuestion"), any(), eq("created"));
    }

    // COM-TC-003: Hidden topic → CommunityTopicNotFoundException (COM-003)
    @Test
    void createQuestion_hiddenTopic_throwsCommunityTopicNotFoundException() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.createQuestion(AUTHOR_ID, makeRequest()))
                .isInstanceOf(CommunityTopicNotFoundException.class)
                .hasMessageContaining("COM-003");
        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC);

        // BR-COM-002: must not save when topic is invalid
        verify(questionRepository, never()).save(any());
    }

    // COM-TC-003 sub: non-existent topicId → same exception
    @Test
    void createQuestion_nonExistentTopicId_throwsCommunityTopicNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(unknownId, TopicType.TOPIC))
                .thenReturn(Optional.empty());

        CreateCommunityQuestionRequest req = makeRequest(r -> r.setTopicId(unknownId));

        assertThatThrownBy(() -> questionService.createQuestion(AUTHOR_ID, req))
                .isInstanceOf(CommunityTopicNotFoundException.class);

        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(unknownId, TopicType.TOPIC);
        verify(questionRepository, never()).save(any());
    }

    // COM-TC anonymous: authorId stored in DB even when isAnonymous=true
    @Test
    void createQuestion_anonymous_authorIdStoredInDb() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(makeTopic(false)));
        CommunityQuestion saved = CommunityQuestion.builder()
                .id(UUID.randomUUID()).topicId(TOPIC_ID).authorId(AUTHOR_ID)
                .anonymous(true).status(QuestionStatus.PENDING).build();
        when(questionRepository.save(any())).thenReturn(saved);

        CreateCommunityQuestionRequest req = makeRequest(r -> r.setIsAnonymous(true));
        questionService.createQuestion(AUTHOR_ID, req);

        // ADR-COM-002: authorId MUST be saved to DB
        verify(questionRepository).save(argThat(q -> q.getAuthorId().equals(AUTHOR_ID)));
    }

    // COM-TC-031: CATEGORY is not a valid question target (COM-003 semantics).
    @Test
    void createQuestion_categoryTarget_throwsCommunityTopicNotFoundException() {
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.createQuestion(AUTHOR_ID, makeRequest()))
                .isInstanceOf(CommunityTopicNotFoundException.class)
                .hasMessageContaining("COM-003");
        verify(topicRepository).findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC);
        verify(questionRepository, never()).save(any());
    }

    // COM-TC-037: regression — a visible TOPIC still accepts question creation.
    @Test
    void createQuestion_visibleTopicTarget_succeedsAfterHierarchyMigration() {
        CommunityTopic topic = makeTopic(false);
        lenient().when(topicRepository.findByIdAndTypeAndIsHiddenFalse(TOPIC_ID, TopicType.TOPIC))
                .thenReturn(Optional.of(topic));
        when(questionRepository.save(any())).thenReturn(savedQuestion());

        CommunityQuestionResponse response = questionService.createQuestion(AUTHOR_ID, makeRequest());

        assertThat(response.getId()).isNotNull();
        verify(questionRepository).save(argThat(question -> TOPIC_ID.equals(question.getTopicId())));
    }

    // ===================== UC-199: Question Detail =====================

    private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private CommunityQuestion makeApprovedQuestion(boolean anonymous) {
        return CommunityQuestion.builder()
                .id(QUESTION_ID)
                .topicId(TOPIC_ID)
                .authorId(AUTHOR_ID)
                .title("Valid test question title")
                .body("This is a valid test body with enough characters")
                .anonymous(anonymous)
                .status(QuestionStatus.APPROVED)
                .build();
    }

    private void stubEmptyHydration() {
        when(answerRepository.findVisibleAnswersForDetail(any(), any()))
                .thenReturn(List.of());
        when(answerLikeRepository.findLikedAnswerIds(any(), any())).thenReturn(Set.of());
    }

    // The author viewing their own anonymous question must still get their real authorId back —
    // the mobile "is this my question" / edit-button check depends on it. Only OTHER viewers see
    // authorId masked to null. Regression test for the edit-button-disappears-when-anonymous bug.
    @Test
    void getQuestionDetail_ownAnonymousQuestion_authorIdExposedToAuthor() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeApprovedQuestion(true)));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        stubEmptyHydration();

        CommunityQuestionDetailResponse response = questionService.getQuestionDetail(QUESTION_ID, AUTHOR_ID);

        assertThat(response.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(response.getAuthorDisplay()).isEqualTo("Thành viên ẩn danh");
    }

    @Test
    void getQuestionDetail_ownAiPendingQuestion_remainsPrivatelyVisibleToAuthor() {
        CommunityQuestion question = makeApprovedQuestion(false);
        question.setStatus(QuestionStatus.AI_PENDING);
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        stubEmptyHydration();

        CommunityQuestionDetailResponse response = questionService.getQuestionDetail(QUESTION_ID, AUTHOR_ID);

        assertThat(response.getId()).isEqualTo(QUESTION_ID);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    // A different viewer must never see the real authorId of an anonymous question (ADR-COM-002).
    @Test
    void getQuestionDetail_anonymousQuestionViewedByOtherUser_authorIdMasked() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeApprovedQuestion(true)));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        stubEmptyHydration();

        CommunityQuestionDetailResponse response = questionService.getQuestionDetail(QUESTION_ID, OTHER_USER_ID);

        assertThat(response.getAuthorId()).isNull();
    }

    // Non-anonymous question: real display name resolved from the cascade, not the generic label.
    @Test
    void getQuestionDetail_notAnonymous_resolvesRealAuthorDisplayName() {
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(makeApprovedQuestion(false)));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        stubEmptyHydration();
        when(authorDisplayResolver.resolve(AUTHOR_ID)).thenReturn("Nguyễn Thị A");

        CommunityQuestionDetailResponse response = questionService.getQuestionDetail(QUESTION_ID, OTHER_USER_ID);

        assertThat(response.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(response.getAuthorDisplay()).isEqualTo("Nguyễn Thị A");
    }

    @Test
    void getMyQuestions_scopesByAuthorAndExcludesDeleted() {
        CommunityQuestion pending = savedQuestion();
        when(questionRepository.findAllByAuthorIdAndStatusNotOrderByCreatedAtDesc(
                eq(AUTHOR_ID), eq(QuestionStatus.DELETED), eq(PageRequest.of(1, 2))))
                .thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(1, 2), 3));

        var response = questionService.getMyQuestions(AUTHOR_ID, 1, 2);

        assertThat(response.getData()).extracting(CommunityQuestionResponse::getStatus)
                .containsExactly("PENDING");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(3);
        verify(questionRepository).findAllByAuthorIdAndStatusNotOrderByCreatedAtDesc(
                AUTHOR_ID, QuestionStatus.DELETED, PageRequest.of(1, 2));
    }
}
