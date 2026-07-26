package com.carebridge.backend.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.privacy.repository.PrivacySettingsRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class SettingsJsonbConcurrencyPostgresTest extends AbstractPostgresIntegrationTest {

    @Autowired private NotificationPreferenceRepository notificationRepository;
    @Autowired private PrivacySettingsRepository privacyRepository;
    @Autowired private BabyProfileRepository babyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void concurrentDomainPatchesPreserveEverySettingsKeyAndIgnoreUnknownNotificationType()
            throws Exception {
        UUID userId = UUID.randomUUID();
        UUID babyId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, userId, "Settings Owner", uniquePhone(), "MOTHER");
        jdbcTemplate.update("""
                update users
                   set settings_jsonb = ?::jsonb
                 where user_id = ?
                """, """
                {
                  "unrelated":{"keep":true},
                  "notifications":{
                    "REMINDER":{"pushEnabled":true,"emailEnabled":false,"inAppEnabled":true},
                    "FUTURE_NOTIFICATION":{"custom":"keep"}
                  },
                  "privacy":{
                    "profileVisibility":"FRIENDS_ONLY",
                    "analyticsConsent":true,
                    "custom":"keep"
                  }
                }
                """, userId);

        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(3)) {
            var notification = executor.submit(() -> {
                ready.countDown();
                await(start);
                return transaction().execute(status -> {
                    notificationRepository.patchChannels(
                            userId, NotificationType.REMINDER, false, null, null);
                    return true;
                });
            });
            var privacy = executor.submit(() -> {
                ready.countDown();
                await(start);
                return transaction().execute(status -> {
                    privacyRepository.patchFields(userId, null, true, null, null);
                    return true;
                });
            });
            var activeBaby = executor.submit(() -> {
                ready.countDown();
                await(start);
                return transaction().execute(status -> babyRepository.setActiveBaby(userId, babyId));
            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(notification.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(privacy.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(activeBaby.get(15, TimeUnit.SECONDS)).isOne();
        }

        JsonNode settings = objectMapper.readTree(jdbcTemplate.queryForObject(
                "select settings_jsonb::text from users where user_id = ?",
                String.class,
                userId));
        assertThat(settings.at("/unrelated/keep").asBoolean()).isTrue();
        assertThat(settings.at("/notifications/REMINDER/pushEnabled").asBoolean()).isFalse();
        assertThat(settings.at("/notifications/REMINDER/emailEnabled").asBoolean()).isFalse();
        assertThat(settings.at("/notifications/REMINDER/inAppEnabled").asBoolean()).isTrue();
        assertThat(settings.at("/notifications/FUTURE_NOTIFICATION/custom").asText()).isEqualTo("keep");
        assertThat(settings.at("/privacy/profileVisibility").asText()).isEqualTo("FRIENDS_ONLY");
        assertThat(settings.at("/privacy/locationSharingEnabled").asBoolean()).isTrue();
        assertThat(settings.at("/privacy/analyticsConsent").asBoolean()).isTrue();
        assertThat(settings.at("/privacy/custom").asText()).isEqualTo("keep");
        assertThat(settings.get("activeBabyId").asText()).isEqualTo(babyId.toString());
        assertThat(babyRepository.findActiveBabyId(userId)).contains(babyId);

        assertThat(notificationRepository.findByUserId(userId))
                .singleElement()
                .satisfies(preference ->
                        assertThat(preference.getNotificationType()).isEqualTo(NotificationType.REMINDER));

        jdbcTemplate.update("""
                update users
                   set settings_jsonb = jsonb_set(
                       settings_jsonb, '{activeBabyId}', to_jsonb('not-a-uuid'::text), true)
                 where user_id = ?
                """, userId);
        assertThat(babyRepository.findActiveBabyId(userId)).isEmpty();
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for concurrent settings update");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for settings update", exception);
        }
    }

    private String uniquePhone() {
        return "08" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }
}
