package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.response.TopicFollowResponse;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.UserTopicFollow;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.TopicHiddenException;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// UC-171: Follow Topic
@ExtendWith(MockitoExtension.class)
class TopicFollowServiceImplTest {

    @Mock UserTopicFollowRepository followRepository;
    @Mock CommunityTopicRepository topicRepository;
    @InjectMocks TopicFollowServiceImpl followService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private CommunityTopic makeTopic(boolean hidden) {
        return CommunityTopic.builder().id(TOPIC_ID).name("Topic").isHidden(hidden).sortOrder(1).build();
    }

    // TC-171-1: follow visible topic (no existing row)
    @Test
    void toggleFollow_visibleTopicNoExistingFollow_createsFollowReturnsTrue() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.empty());

        TopicFollowResponse response = followService.toggleFollow(TOPIC_ID, USER_ID);

        assertThat(response.isFollowed()).isTrue();
        assertThat(response.getTopicId()).isEqualTo(TOPIC_ID);
        verify(followRepository).save(any(UserTopicFollow.class));
        verify(followRepository, never()).delete(any());
    }

    // TC-171-2: unfollow (existing row -> delete)
    @Test
    void toggleFollow_existingFollow_deletesReturnsFalse() {
        UserTopicFollow existing = UserTopicFollow.builder().id(UUID.randomUUID()).userId(USER_ID).topicId(TOPIC_ID).build();
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.of(existing));

        TopicFollowResponse response = followService.toggleFollow(TOPIC_ID, USER_ID);

        assertThat(response.isFollowed()).isFalse();
        verify(followRepository).delete(existing);
        verify(followRepository, never()).save(any());
    }

    // TC-171-3: follow non-existent topic -> 404
    @Test
    void toggleFollow_topicNotFound_throwsCommunityTopicNotFoundException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.toggleFollow(TOPIC_ID, USER_ID))
                .isInstanceOf(CommunityTopicNotFoundException.class);

        verify(followRepository, never()).save(any());
        verify(followRepository, never()).delete(any());
    }

    // TC-171-4: follow hidden topic -> 409
    @Test
    void toggleFollow_hiddenTopic_throwsTopicHiddenException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(true)));

        assertThatThrownBy(() -> followService.toggleFollow(TOPIC_ID, USER_ID))
                .isInstanceOf(TopicHiddenException.class)
                .hasMessageContaining("COM-014");

        verify(followRepository, never()).save(any());
        verify(followRepository, never()).delete(any());
    }

    // TC-171-6: double follow call -- 1st = follow, 2nd = unfollow
    @Test
    void toggleFollow_calledTwice_togglesFollowedThenUnfollowed() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));

        // 1st call: no existing follow
        when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.empty());
        TopicFollowResponse first = followService.toggleFollow(TOPIC_ID, USER_ID);
        assertThat(first.isFollowed()).isTrue();

        // 2nd call: now a follow exists
        UserTopicFollow existing = UserTopicFollow.builder().id(UUID.randomUUID()).userId(USER_ID).topicId(TOPIC_ID).build();
        when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.of(existing));
        TopicFollowResponse second = followService.toggleFollow(TOPIC_ID, USER_ID);
        assertThat(second.isFollowed()).isFalse();
    }

    // TC-171-7 (unit-level): two different users follow the same topic independently
    @Test
    void toggleFollow_twoDifferentUsers_eachFollowsIndependently() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(makeTopic(false)));
        when(followRepository.findByUserIdAndTopicId(USER_ID, TOPIC_ID)).thenReturn(Optional.empty());
        when(followRepository.findByUserIdAndTopicId(USER_2_ID, TOPIC_ID)).thenReturn(Optional.empty());

        TopicFollowResponse r1 = followService.toggleFollow(TOPIC_ID, USER_ID);
        TopicFollowResponse r2 = followService.toggleFollow(TOPIC_ID, USER_2_ID);

        assertThat(r1.isFollowed()).isTrue();
        assertThat(r2.isFollowed()).isTrue();
        verify(followRepository).save(argThat(f -> f.getUserId().equals(USER_ID)));
        verify(followRepository).save(argThat(f -> f.getUserId().equals(USER_2_ID)));
    }
}
