package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.exception.QuestionNotEditableException;
import com.carebridge.backend.community.exception.QuestionLockedException;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.mapper.CommunityAnswerMapper;
import com.carebridge.backend.community.mapper.CommunityQuestionMapper;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.content.entity.ReportTargetType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityQuestionServiceImpl implements CommunityQuestionService {

 private final CommunityQuestionRepository questionRepository;
 private final CommunityTopicRepository topicRepository;
 private final CommunityAnswerRepository answerRepository;
 private final CommunityBookmarkRepository bookmarkRepository;
 private final CommunityAnswerLikeRepository answerLikeRepository;
 private final CommunityQuestionLikeRepository questionLikeRepository;
 private final CommunityQuestionMapper questionMapper;
 private final CommunityAnswerMapper answerMapper;
 private final AuditService auditService;
 private final CommunitySafetyPolicy communitySafetyPolicy;
 private final com.carebridge.backend.aimoderation.service.AiScanEnqueueService aiScanEnqueueService;
 private final CommunityAuthorDisplayResolver authorDisplayResolver;
 private final ExpertProfileRepository expertProfileRepository;

 @Override
 @Transactional(readOnly = true)
 public CommunityQuestionDetailResponse getQuestionDetail(UUID questionId, UUID currentUserId) {
 // UC-198/199 consistency fix: a PENDING question is visible in detail under the same rule
 // as the feed (see CommunityQuestionRepository.findFeedVisible) — visible to its own author
 // only, not to any authenticated user. Previously ANY user could open ANY PENDING question's
 // detail directly by ID, bypassing the feed's per-author visibility rule.
 CommunityQuestion question = questionRepository.findById(questionId)
 .filter(q -> q.getStatus() == QuestionStatus.APPROVED
 || (q.getStatus() == QuestionStatus.PENDING && q.getAuthorId().equals(currentUserId)))
 .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

 String topicName = topicRepository.findById(question.getTopicId())
 .map(t -> t.getName())
 .orElse("");

 // Fetch question author's display name
 String questionAuthorDisplay = authorDisplayResolver.resolve(question.getAuthorId());

 List<CommunityAnswer> answerEntities = answerRepository
 .findAllByQuestionIdAndStatusOrderByCreatedAtDesc(questionId, AnswerStatus.APPROVED);

 // Batch fetch display names for all answer authors to avoid N+1
 Set<UUID> answerAuthorIds = answerEntities.stream()
 .map(CommunityAnswer::getAuthorId)
 .collect(Collectors.toSet());
 Map<UUID, String> answerAuthorNames = authorDisplayResolver.resolveBatch(answerAuthorIds);

 // TV4 integration: batch-resolve expertProfileId by userId for all answer authors.
 // Only authors with an APPROVED expert profile get their ID populated; others get null.
 Map<UUID, UUID> expertProfileIds = expertProfileRepository
 .findByUserIdIn(answerAuthorIds).stream()
 .filter(ep -> ep.getVerificationStatus() == com.carebridge.backend.expert.verificationstatus.VerificationStatus.APPROVED)
 .collect(Collectors.toMap(ExpertProfile::getUserId, ExpertProfile::getExpertProfileId));

 // UC-59 hydration fix: batch-check the current viewer's likes to avoid N+1
 List<UUID> answerIds = answerEntities.stream().map(CommunityAnswer::getId).toList();
 Set<UUID> likedAnswerIds = answerLikeRepository.findLikedAnswerIds(currentUserId, answerIds);

 List<CommunityAnswerResponse> answers = answerEntities.stream()
 .map(a -> answerMapper.toResponse(a,
 answerAuthorNames.get(a.getAuthorId()),
 likedAnswerIds.contains(a.getId()),
 expertProfileIds.get(a.getAuthorId())))
 .toList();

 // UC-58 hydration fix: current viewer's bookmark state for this question
 boolean isBookmarked = bookmarkRepository.existsByUserIdAndQuestionId(currentUserId, questionId);

 // Current viewer's like state for this question
 boolean isLiked = questionLikeRepository.existsByUserIdAndQuestionId(currentUserId, questionId);

 return questionMapper.toDetailResponse(
 question, topicName, questionAuthorDisplay, answers, isBookmarked, isLiked, currentUserId);
 }

 @Override
 @Transactional
 public CommunityQuestionResponse createQuestion(UUID authorId, CreateCommunityQuestionRequest request) {
 communitySafetyPolicy.requirePostingAllowed(authorId);

 // ADR-COM-021: questions may target only visible TOPIC rows.
 topicRepository.findByIdAndTypeAndIsHiddenFalse(request.getTopicId(), TopicType.TOPIC)
 .orElseThrow(() -> new CommunityTopicNotFoundException(request.getTopicId().toString()));

 CommunityQuestion question = questionMapper.toEntity(request, authorId);
 question = questionRepository.save(question);
 aiScanEnqueueService.enqueueScan(ReportTargetType.QUESTION, question.getId(), question.getTitle() + "\n" + question.getBody());

 auditService.log(AuditAction.COMMUNITY_QUESTION_CREATED, authorId, "CommunityQuestion", question.getId().toString(), "created");

 return questionMapper.toResponse(question);
 }

 @Override
 @Transactional
 public CommunityQuestionResponse editQuestion(UUID authorId, UUID questionId, UpdateCommunityQuestionRequest request) {
 CommunityQuestion question = questionRepository.findById(questionId)
 .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

 if (!question.getAuthorId().equals(authorId)) {
 throw new AccessDeniedException("Only the author can edit this question");
 }

 if (question.getStatus() == QuestionStatus.LOCKED
 || question.getStatus() == QuestionStatus.HIDDEN
 || question.getStatus() == QuestionStatus.DELETED) {
 throw new QuestionNotEditableException(questionId.toString());
 }

 if (request.getTitle() != null) question.setTitle(request.getTitle());
 if (request.getBody() != null) question.setBody(request.getBody());
 if (request.getIsAnonymous() != null) question.setAnonymous(request.getIsAnonymous());
 if (request.getUrgency() != null) question.setUrgency(request.getUrgency());
 question.setStatus(QuestionStatus.PENDING);

 question = questionRepository.save(question);
 aiScanEnqueueService.enqueueScan(ReportTargetType.QUESTION, question.getId(), question.getTitle() + "\n" + question.getBody());
 auditService.log(AuditAction.COMMUNITY_QUESTION_EDITED, authorId, "CommunityQuestion", question.getId().toString(), "edited");

 return questionMapper.toResponse(question);
 }

 @Override
 @Transactional
 public void deleteQuestion(UUID questionId, UUID callerId, boolean isModeratorCaller) {
 CommunityQuestion question = questionRepository.findById(questionId)
 .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

 if (question.getStatus() == QuestionStatus.LOCKED) {
 throw new QuestionLockedException(questionId.toString());
 }

 if (!question.getAuthorId().equals(callerId) && !isModeratorCaller) {
 throw new AccessDeniedException("You do not own this question");
 }

 question.setStatus(QuestionStatus.DELETED);
 questionRepository.save(question);

 auditService.log(AuditAction.COMMUNITY_QUESTION_DELETED, callerId, "CommunityQuestion", questionId.toString(), "deleted");
 }
}
