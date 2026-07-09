package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.profile.repository.ProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// This is the actual name-resolution logic behind the community display-name fix: real
// end-to-end proof that a bare User.name (no CommunityProfile/UserProfile at all — the
// DevDataSeeder shape for every seeded test account) surfaces as the displayed name, rather
// than the generic "Người dùng"/"Thành viên" fallback the user reported seeing.
@ExtendWith(MockitoExtension.class)
class CommunityAuthorDisplayResolverTest {

    @Mock
    private CommunityProfileRepository communityProfileRepository;

    @Mock
    private ProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommunityAuthorDisplayResolver resolver;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private User makeUser(String name) {
        return User.builder().id(USER_ID).name(name).build();
    }

    // DevDataSeeder shape: only users.full_name is populated, no CommunityProfile/UserProfile row.
    @Test
    void resolve_onlyUserFullNamePresent_returnsUserName() {
        when(communityProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of());
        when(userProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of());
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(makeUser("Nguyễn Thị A")));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isEqualTo("Nguyễn Thị A");
    }

    // CommunityProfile.displayName takes priority over the other two when present.
    @Test
    void resolve_communityProfilePresent_takesPriorityOverUserProfileAndUser() {
        CommunityProfile profile = CommunityProfile.builder().userId(USER_ID).displayName("Community Name").build();
        when(communityProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of(profile));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isEqualTo("Community Name");
    }

    // CommunityProfile absent/blank falls through to UserProfile.displayName before User.name.
    @Test
    void resolve_communityProfileBlank_fallsThroughToUserProfile() {
        CommunityProfile blankProfile = CommunityProfile.builder().userId(USER_ID).displayName("  ").build();
        when(communityProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of(blankProfile));
        UserProfile userProfile = UserProfile.builder().userId(USER_ID).displayName("Profile Name").build();
        when(userProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of(userProfile));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isEqualTo("Profile Name");
    }

    // No name anywhere in the cascade → null, letting the caller apply the generic fallback text.
    @Test
    void resolve_noNameAnywhere_returnsNull() {
        when(communityProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of());
        when(userProfileRepository.findAllByUserIdIn(Set.of(USER_ID))).thenReturn(List.of());
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(makeUser(null)));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isNull();
    }

    @Test
    void resolve_nullAuthorId_returnsNullWithoutQuerying() {
        String result = resolver.resolve(null);

        assertThat(result).isNull();
    }

    @Test
    void resolveBatch_emptyInput_returnsEmptyMapWithoutQuerying() {
        Map<UUID, String> result = resolver.resolveBatch(Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void resolveBatch_mixedSources_resolvesEachIndependently() {
        UUID communityUser = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
        UUID userProfileUser = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
        UUID plainUser = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
        Set<UUID> ids = Set.of(communityUser, userProfileUser, plainUser);

        when(communityProfileRepository.findAllByUserIdIn(ids)).thenReturn(
                List.of(CommunityProfile.builder().userId(communityUser).displayName("Community").build()));
        when(userProfileRepository.findAllByUserIdIn(any())).thenReturn(
                List.of(UserProfile.builder().userId(userProfileUser).displayName("Profile").build()));
        when(userRepository.findAllById(any())).thenReturn(
                List.of(User.builder().id(plainUser).name("Plain Name").build()));

        Map<UUID, String> result = resolver.resolveBatch(ids);

        assertThat(result)
                .containsEntry(communityUser, "Community")
                .containsEntry(userProfileUser, "Profile")
                .containsEntry(plainUser, "Plain Name");
    }
}
