package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.response.QuestionLikeToggleResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityQuestionLike;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityQuestionLikeServiceImpl implements CommunityQuestionLikeService {

    private final CommunityQuestionLikeRepository likeRepository;
    private final CommunityQuestionRepository questionRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public QuestionLikeToggleResponse toggleLike(UUID userId, UUID questionId) {
        CommunityQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId.toString()));

        if (likeRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            CommunityQuestionLike like = likeRepository.findByUserIdAndQuestionId(userId, questionId)
                    .orElseThrow();
            likeRepository.delete(like);
            question.setLikeCount(Math.max(0, question.getLikeCount() - 1));
            questionRepository.save(question);
            auditService.log(AuditAction.COMMUNITY_QUESTION_UNLIKED, userId,
                    "CommunityQuestion", questionId.toString(), "unliked");
            return QuestionLikeToggleResponse.builder()
                    .liked(false)
                    .likeCount(question.getLikeCount())
                    .questionId(questionId)
                    .build();
        } else {
            CommunityQuestionLike like = CommunityQuestionLike.builder()
                    .userId(userId)
                    .questionId(questionId)
                    .build();
            likeRepository.save(like);
            question.setLikeCount(question.getLikeCount() + 1);
            questionRepository.save(question);
            auditService.log(AuditAction.COMMUNITY_QUESTION_LIKED, userId,
                    "CommunityQuestion", questionId.toString(), "liked");
            return QuestionLikeToggleResponse.builder()
                    .liked(true)
                    .likeCount(question.getLikeCount())
                    .questionId(questionId)
                    .build();
        }
    }
}
