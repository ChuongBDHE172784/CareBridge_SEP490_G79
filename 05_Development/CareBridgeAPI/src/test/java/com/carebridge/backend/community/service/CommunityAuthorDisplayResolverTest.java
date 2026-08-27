package com.carebridge.backend.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class CommunityAuthorDisplayResolverTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommunityAuthorDisplayResolver resolver;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private User makeUser(String name, String displayName) {
        return User.builder().id(USER_ID).name(name).displayName(displayName).build();
    }

    @Test
    void resolve_onlyUserFullNamePresent_returnsUserName() {
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(makeUser("Nguyễn Thị A", null)));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isEqualTo("Nguyễn Thị A");
    }

    @Test
    void resolve_userDisplayNamePresent_takesPriorityOverName() {
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(makeUser("Nguyễn Thị A", "Mẹ Bé Sâu")));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isEqualTo("Mẹ Bé Sâu");
    }

    @Test
    void resolve_userDisplayNameBlank_fallsThroughToName() {
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(makeUser("Profile Name", "  ")));

        String result = resolver.resolve(USER_ID);

        assertThat(result).isEqualTo("Profile Name");
    }

    @Test
    void resolve_noNameAnywhere_returnsNull() {
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(makeUser(null, null)));

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
    void resolveBatch_mixedUsers_resolvesEachIndependently() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
        Set<UUID> ids = Set.of(u1, u2);

        when(userRepository.findAllById(ids)).thenReturn(
                List.of(
                        User.builder().id(u1).displayName("Community Nickname").name("Real Name 1").build(),
                        User.builder().id(u2).name("Real Name 2").build()));

        Map<UUID, String> result = resolver.resolveBatch(ids);

        assertThat(result)
                .containsEntry(u1, "Community Nickname")
                .containsEntry(u2, "Real Name 2");
    }
}
