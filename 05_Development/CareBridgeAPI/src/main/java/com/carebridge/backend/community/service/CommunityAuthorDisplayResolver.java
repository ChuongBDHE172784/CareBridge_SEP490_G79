package com.carebridge.backend.community.service;

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
// User.displayName -> User.name -> null (caller applies final fallback text).
@Component
@RequiredArgsConstructor
public class CommunityAuthorDisplayResolver {

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
        userRepository.findAllById(ids).forEach(u -> {
            String name = u.getDisplayName();
            if (name == null || name.isBlank()) {
                name = u.getName();
            }
            putIfPresent(names, u.getId(), name);
        });

        return names;
    }

    private static void putIfPresent(Map<UUID, String> names, UUID userId, String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            names.put(userId, displayName);
        }
    }
}
