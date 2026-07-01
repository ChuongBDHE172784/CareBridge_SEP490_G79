package com.carebridge.backend.moderation;

import com.carebridge.backend.content.dto.request.WarnOrSuspendAccountRequest;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.security.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;

/** Props Isolation Pattern (CASE 2.0) — shared by all WSA-TC test classes (UC-102 Test-Spec §4). */
final class WarnSuspendAccountTestFactory {

    private WarnSuspendAccountTestFactory() {
    }

    static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-0000000000f2");
    static final UUID TARGET_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000001");
    static final UUID SUSPENDED_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000002");
    static final UUID LAPSED_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000003");

    static final Instant T0 = Instant.parse("2026-07-01T10:00:00Z");
    static final Instant FUTURE_EXPIRY = T0.plus(14, ChronoUnit.DAYS);
    static final Instant PAST_EXPIRY = T0.minus(1, ChronoUnit.DAYS);

    static User makeUser(UUID id, Instant suspendedUntil) {
        return makeUser(id, suspendedUntil, u -> {
        });
    }

    static User makeUser(UUID id, Instant suspendedUntil, Consumer<User> overrides) {
        User u = User.builder()
                .id(id)
                .enabled(true)
                .locked(false)
                .lockedAt(null)
                .suspendedUntil(suspendedUntil)
                .build();
        overrides.accept(u);
        return u;
    }

    static WarnOrSuspendAccountRequest makeRequest(ModerationActionType actionType, String reason, Instant expiresAt) {
        return new WarnOrSuspendAccountRequest(TARGET_USER_ID, actionType, reason, expiresAt);
    }
}
