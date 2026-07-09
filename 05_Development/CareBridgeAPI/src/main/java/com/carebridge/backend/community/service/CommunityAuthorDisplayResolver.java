package com.carebridge.backend.community.service;

import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.profile.repository.ProfileRepository;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Cascade priority for the name shown next to a community post/answer:
// CommunityProfile.displayName (public community profile) -> UserProfile.displayName
// (private account profile) -> User.name (registration name) -> null (caller applies the
// final "Người dùng"/"Thành viên" fallback text).
@Component
@RequiredArgsConstructor
public class CommunityAuthorDisplayResolver {

    private final CommunityProfileRepository communityProfileRepository;
    private final ProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public String resolve(UUID authorId) {
        if (authorId == null) {
            return null;
        }
        return resolveBatch(Set.of(authorId)).get(authorId);
    }

    public Map<UUID, String> resolveBatch(Collection<UUID> authorIds) {
        Set<UUID> ids = authorIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> names = new HashMap<>();
        communityProfileRepository.findAllByUserIdIn(ids).forEach(p -> putIfPresent(names, p.getUserId(), p.getDisplayName()));

        Set<UUID> missing = missingFrom(ids, names);
        if (!missing.isEmpty()) {
            userProfileRepository.findAllByUserIdIn(missing).forEach(p -> putIfPresent(names, p.getUserId(), p.getDisplayName()));
        }

        Set<UUID> stillMissing = missingFrom(ids, names);
        if (!stillMissing.isEmpty()) {
            userRepository.findAllById(stillMissing).forEach(u -> putIfPresent(names, u.getId(), u.getName()));
        }

        return names;
    }

    private static Set<UUID> missingFrom(Set<UUID> ids, Map<UUID, String> resolved) {
        return ids.stream().filter(id -> !resolved.containsKey(id)).collect(Collectors.toSet());
    }

    private static void putIfPresent(Map<UUID, String> names, UUID userId, String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            names.put(userId, displayName);
        }
    }
}
