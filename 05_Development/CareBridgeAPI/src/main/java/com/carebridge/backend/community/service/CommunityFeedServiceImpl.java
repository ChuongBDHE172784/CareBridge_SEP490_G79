package com.carebridge.backend.community.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.mapper.CommunityFeedMapper;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityFeedServiceImpl implements CommunityFeedService {

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final CommunityTopicRepository topicRepository;
    private final CommunityFeedMapper feedMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CommunityFeedItemResponse> getFeed(UUID topicId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<CommunityQuestion> questions;
        if (topicId != null) {
            questions = questionRepository.findAllByStatusAndTopicIdOrderByCreatedAtDesc(
                    QuestionStatus.APPROVED, topicId, pageable);
        } else {
            questions = questionRepository.findAllByStatusOrderByCreatedAtDesc(
                    QuestionStatus.APPROVED, pageable);
        }

        // Batch fetch topic names to avoid N+1
        Set<UUID> topicIds = questions.stream()
                .map(CommunityQuestion::getTopicId)
                .collect(Collectors.toSet());
        Map<UUID, String> topicNames = topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(
                        t -> t.getId(),
                        t -> t.getName()));

        // Batch check expert answers to avoid N+1
        List<UUID> questionIds = questions.stream()
                .map(CommunityQuestion::getId)
                .collect(Collectors.toList());
        Set<UUID> expertAnsweredIds = answerRepository.findQuestionIdsWithExpertAnswer(questionIds);

        Page<CommunityFeedItemResponse> feedPage = questions.map(q -> {
            String tName = topicNames.getOrDefault(q.getTopicId(), "");
            boolean hasExpert = expertAnsweredIds.contains(q.getId());
            return feedMapper.toFeedItem(q, tName, null, hasExpert);
        });

        return PaginatedResponse.of(feedPage);
    }
}
