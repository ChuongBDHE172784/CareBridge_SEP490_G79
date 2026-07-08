package com.carebridge.backend.community;

import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.mapper.CommunityTopicMapper;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import com.carebridge.backend.community.service.CommunityTopicServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityTopicSearchServiceImplTest {

    @Mock CommunityTopicRepository topicRepository;
    @Mock CommunityTopicMapper topicMapper;
    @Mock UserTopicFollowRepository topicFollowRepository;
    @InjectMocks CommunityTopicServiceImpl topicService;

    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @Test
    void searchTopics_withKeyword_returnsMatchingTopics() {
        CommunityTopic topic = makeTopic("Dinh dưỡng thai kỳ");
        CommunityTopicResponse response = CommunityTopicResponse.builder().id(topic.getId()).name(topic.getName()).build();
        when(topicRepository.searchByKeyword("dinh")).thenReturn(List.of(topic));
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());
        when(topicMapper.toResponse(topic, false)).thenReturn(response);

        List<CommunityTopicResponse> result = topicService.searchTopics("dinh", false, CURRENT_USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Dinh dưỡng thai kỳ");
    }

    @Test
    void searchTopics_withNullKeyword_delegatesToGetTopics() {
        CommunityTopic topic = makeTopic("Any Topic");
        CommunityTopicResponse response = CommunityTopicResponse.builder().id(topic.getId()).name(topic.getName()).build();
        when(topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc()).thenReturn(List.of(topic));
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());
        when(topicMapper.toResponse(topic, false)).thenReturn(response);

        List<CommunityTopicResponse> result = topicService.searchTopics(null, false, CURRENT_USER_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchTopics_withBlankKeyword_delegatesToGetTopics() {
        when(topicRepository.findAllByIsHiddenFalseOrderBySortOrderAsc()).thenReturn(List.of());
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());

        List<CommunityTopicResponse> result = topicService.searchTopics("   ", false, CURRENT_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void searchTopics_noMatch_returnsEmpty() {
        when(topicRepository.searchByKeyword("xyz_no_match")).thenReturn(List.of());
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());

        List<CommunityTopicResponse> result = topicService.searchTopics("xyz_no_match", false, CURRENT_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void searchTopics_moderatorIncludeHidden_usesHiddenQuery() {
        CommunityTopic hiddenTopic = makeTopic("Hidden Topic");
        hiddenTopic.setHidden(true);
        CommunityTopicResponse response = CommunityTopicResponse.builder().id(hiddenTopic.getId()).name(hiddenTopic.getName()).build();
        when(topicRepository.searchByKeywordIncludingHidden("hidden")).thenReturn(List.of(hiddenTopic));
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());
        when(topicMapper.toResponse(hiddenTopic, false)).thenReturn(response);

        List<CommunityTopicResponse> result = topicService.searchTopics("hidden", true, CURRENT_USER_ID);

        assertThat(result).hasSize(1);
        verify(topicRepository).searchByKeywordIncludingHidden("hidden");
        verify(topicRepository, never()).searchByKeyword(any());
    }

    @Test
    void searchTopics_nonModeratorIncludeHiddenFalse_usesNonHiddenQuery() {
        when(topicRepository.searchByKeyword("test")).thenReturn(List.of());
        when(topicFollowRepository.findFollowedTopicIds(any(), any())).thenReturn(Set.of());

        topicService.searchTopics("test", false, CURRENT_USER_ID);

        verify(topicRepository).searchByKeyword("test");
        verify(topicRepository, never()).searchByKeywordIncludingHidden(any());
    }

    private CommunityTopic makeTopic(String name) {
        return CommunityTopic.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isHidden(false)
                .sortOrder(0)
                .build();
    }
}
