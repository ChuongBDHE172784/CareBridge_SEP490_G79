package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.common.dev.DevDataSeeder;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Minimum academic gate: verify the dev seed exposes enough fallback and targeted fixtures. */
@EnabledOnOs(OS.WINDOWS)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "carebridge.dev-seed.enabled=true",
        "carebridge.dev-seed.password=Synthetic-Only-Strong-Passphrase-6-10",
        "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecommendationDevSeederEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DevDataSeeder seeder;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Test
    void devSeedHasThreeFallbacksAndOneTargetedArticlePerMaternalStageAndIsIdempotent() throws Exception {
        List<String> stages = List.of("PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM");
        for (String stage : stages) {
            assertThat(count("""
                    select count(*) from content_items
                     where stage = '%s' and content_type = 'ARTICLE' and status = 'APPROVED'
                       and eligible_from_week is null and eligible_to_week is null
                       and not exists (
                           select 1 from content_item_topics cit
                           join community_topics ct on ct.id = cit.topic_id
                           where cit.content_item_id = content_items.content_item_id
                             and ct.slug like 'rec-%%')
                    """.formatted(stage))).isGreaterThanOrEqualTo(3L);
            assertThat(count("""
                    select count(*) from content_items
                     where stage = '%s' and content_type = 'ARTICLE' and status = 'APPROVED'
                       and exists (
                           select 1 from content_item_topics cit
                           join community_topics ct on ct.id = cit.topic_id
                           where cit.content_item_id = content_items.content_item_id
                             and ct.slug like 'rec-%%')
                    """.formatted(stage))).isGreaterThanOrEqualTo(1L);
        }

        long before = count("select count(*) from content_items");
        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertThat(count("select count(*) from content_items")).isEqualTo(before);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
