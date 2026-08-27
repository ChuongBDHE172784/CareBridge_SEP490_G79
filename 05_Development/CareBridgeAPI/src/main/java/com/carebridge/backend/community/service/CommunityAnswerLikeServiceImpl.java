package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.response.LikeToggleResponse;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityAnswerLike;
import com.carebridge.backend.community.policy.CommunitySafetyPolicy;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityAnswerLikeServiceImpl implements CommunityAnswerLikeService {

    private final CommunityAnswerLikeRepository likeRepository;
    private final CommunityAnswerRepository answerRepository;
    private final AuditService auditService;
    private final CommunitySafetyPolicy communitySafetyPolicy;

    @Override
    @Transactional
    public LikeToggleResponse toggleLike(UUID userId, UUID answerId) {
        CommunityAnswer answer = communitySafetyPolicy.requireVisibleAnswer(userId, answerId);

        if (likeRepository.existsByUserIdAndAnswerId(userId, answerId)) {
            CommunityAnswerLike like = likeRepository.findByUserIdAndAnswerId(userId, answerId)
                    .orElseThrow();
            likeRepository.delete(like);
            answer.setLikeCount(Math.max(0, answer.getLikeCount() - 1));
            answerRepository.save(answer);
            auditService.log(AuditAction.COMMUNITY_ANSWER_UNLIKED, userId,
                    "CommunityAnswer", answerId.toString(), "unliked");
            return LikeToggleResponse.builder()
                    .liked(false)
                    .likeCount(answer.getLikeCount())
                    .answerId(answerId)
                    .build();
        } else {
            CommunityAnswerLike like = CommunityAnswerLike.builder()
                    .userId(userId)
                    .answerId(answerId)
                    .build();
            likeRepository.save(like);
            answer.setLikeCount(answer.getLikeCount() + 1);
            answerRepository.save(answer);
            auditService.log(AuditAction.COMMUNITY_ANSWER_LIKED, userId,
                    "CommunityAnswer", answerId.toString(), "liked");
            return LikeToggleResponse.builder()
                    .liked(true)
                    .likeCount(answer.getLikeCount())
                    .answerId(answerId)
                    .build();
        }
    }
}
