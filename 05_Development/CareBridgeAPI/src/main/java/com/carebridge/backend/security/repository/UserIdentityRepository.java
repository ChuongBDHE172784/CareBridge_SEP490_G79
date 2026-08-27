package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.entity.UserIdentity;
import com.carebridge.backend.security.federation.FederatedProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserIdentityRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public Optional<UserIdentity> findByProviderAndProviderSubject(
            FederatedProvider provider, String providerSubject) {
        return find("""
                SELECT u.user_id, identity::text
                  FROM users u
                  CROSS JOIN LATERAL jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                 WHERE identity->>'provider' = ?
                   AND identity->>'providerSubject' = ?
                 LIMIT 1
                """, provider.name(), providerSubject);
    }

    public Optional<UserIdentity> findByUserIdAndProvider(UUID userId, FederatedProvider provider) {
        return find("""
                SELECT u.user_id, identity::text
                  FROM users u
                  CROSS JOIN LATERAL jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                 WHERE u.user_id = ?
                   AND identity->>'provider' = ?
                 LIMIT 1
                """, userId, provider.name());
    }

    public UserIdentity save(UserIdentity identity) {
        User user = identity.getUser();
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Federated identity requires a persisted user");
        }
        lockProviderSubject(identity.getProvider().name() + ':' + identity.getProviderSubject());
        lockUserProvider(user.getId() + ":" + identity.getProvider().name());
        findByProviderAndProviderSubject(identity.getProvider(), identity.getProviderSubject())
                .filter(existing -> !user.getId().equals(existing.getUser().getId()))
                .ifPresent(existing -> {
                    throw new DataIntegrityViolationException(
                            "Federated provider subject is already owned by another user");
                });
        findByUserIdAndProvider(user.getId(), identity.getProvider())
                .filter(existing -> !identity.getProviderSubject().equals(existing.getProviderSubject()))
                .ifPresent(existing -> {
                    throw new DataIntegrityViolationException(
                            "User already has another identity for this provider");
                });
        Instant now = Instant.now();
        if (identity.getId() == null) {
            identity.setId(UUID.nameUUIDFromBytes(
                    (identity.getProvider().name() + ":" + identity.getProviderSubject())
                            .getBytes(StandardCharsets.UTF_8)));
        }
        if (identity.getCreatedAt() == null) identity.setCreatedAt(now);
        if (identity.getLastUsedAt() == null) identity.setLastUsedAt(now);

        String payload = toJson(java.util.List.of(Map.of(
                "id", identity.getId().toString(),
                "provider", identity.getProvider().name(),
                "providerSubject", identity.getProviderSubject(),
                "providerEmail", identity.getProviderEmail() == null ? "" : identity.getProviderEmail(),
                "providerPhone", identity.getProviderPhone() == null ? "" : identity.getProviderPhone(),
                "createdAt", identity.getCreatedAt().toString(),
                "lastUsedAt", identity.getLastUsedAt().toString())));
        int updated = jdbcTemplate.update("""
                UPDATE users
                   SET social_identities = (
                         SELECT coalesce(jsonb_agg(item), '[]'::jsonb)
                           FROM jsonb_array_elements(coalesce(users.social_identities, '[]'::jsonb)) item
                          WHERE item->>'provider' <> ?
                       ) || cast(? as jsonb),
                       updated_at = now()
                 WHERE user_id = ?
                """, identity.getProvider().name(), payload, user.getId());
        if (updated != 1) {
            throw new IllegalArgumentException("Federated identity requires a flushed user row");
        }
        return identity;
    }

    public UserIdentity saveAndFlush(UserIdentity identity) {
        return save(identity);
    }

    public void lockProviderSubject(String identityKey) {
        advisoryLock(identityKey);
    }

    public void lockUserProvider(String userProviderKey) {
        advisoryLock(userProviderKey);
    }

    private void advisoryLock(String key) {
        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                Object.class, key);
    }

    private Optional<UserIdentity> find(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();
            UUID userId = rs.getObject(1, UUID.class);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return Optional.empty();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> value = objectMapper.readValue(rs.getString(2), Map.class);
                FederatedProvider provider = FederatedProvider.valueOf(value.get("provider").toString());
                return Optional.of(UserIdentity.builder()
                        .id(uuid(value.get("id")))
                        .user(user)
                        .provider(provider)
                        .providerSubject(text(value.get("providerSubject")))
                        .providerEmail(blankToNull(text(value.get("providerEmail"))))
                        .providerPhone(blankToNull(text(value.get("providerPhone"))))
                        .createdAt(instant(value.get("createdAt")))
                        .lastUsedAt(instant(value.get("lastUsedAt")))
                        .build());
            } catch (JsonProcessingException | RuntimeException ex) {
                throw new IllegalStateException("Invalid users.social_identities payload", ex);
            }
        }, args);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot encode federated identity", ex);
        }
    }

    private static UUID uuid(Object value) {
        return value == null || value.toString().isBlank() ? null : UUID.fromString(value.toString());
    }

    private static Instant instant(Object value) {
        return value == null || value.toString().isBlank() ? null : Instant.parse(value.toString());
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
