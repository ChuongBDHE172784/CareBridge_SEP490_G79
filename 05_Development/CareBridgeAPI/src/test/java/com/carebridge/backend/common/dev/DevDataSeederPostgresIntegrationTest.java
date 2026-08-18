package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.out-of-order=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "carebridge.mail.from-address=noreply@carebridge.test",
        "carebridge.mail.from-name=CareBridge",
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret",
        "carebridge.dev-seed.enabled=true",
        "carebridge.dev-seed.password=Synthetic-Only-Strong-Passphrase-6-10",
        "spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevDataSeederPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.1-alpine");

    static {
        POSTGRES.start();
        try {
            com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture.provision(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not provision dev-seed database roles", exception);
        }
    }

    private static final List<String> EXPECTED_SEED_EMAILS = List.of(
            "admin@carebridge.dev", "moderator@carebridge.dev", "content@carebridge.dev",
            "expert@carebridge.dev", "mother@carebridge.dev", "family@carebridge.dev",
            "mother2@carebridge.dev", "mother3@carebridge.dev", "mother4@carebridge.dev",
            "mother5@carebridge.dev", "mother6@carebridge.dev", "family2@carebridge.dev",
            "family3@carebridge.dev", "family4@carebridge.dev", "family5@carebridge.dev",
            "family6@carebridge.dev", "expert2@carebridge.dev", "expert3@carebridge.dev",
            "expert4@carebridge.dev", "expert5@carebridge.dev", "expert6@carebridge.dev");

    @Autowired
    private DevDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Test
    void seedsAccountsProfilesAndCommunityDemoAndRemainsIdempotent() {
        assertSeedAccountsAndProfiles();
        assertNoDomainFixtures();
        assertCommunityModerationFixtures();
        assertExpertConsultationFixtures();

        List<Long> domainCountsBefore = domainTableCounts();
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertSeedAccountsAndProfiles();
        assertNoDomainFixtures();
        assertCommunityModerationFixtures();
        assertExpertConsultationFixtures();
        assertThat(domainTableCounts()).isEqualTo(domainCountsBefore);
    }

    private void assertCommunityModerationFixtures() {
        assertThat(count("SELECT count(*) FROM community_topics WHERE id::text LIKE 'c1000000-0000-4000-8000-%'"))
                .isEqualTo(20L);
        assertThat(count("SELECT count(*) FROM community_topics WHERE id::text LIKE 'c2000000-0000-4000-8000-%'"))
                .isEqualTo(40L);
        assertThat(count("SELECT count(*) FROM community_content WHERE content_id::text LIKE 'c3000000-0000-4000-8000-%'"))
                .isEqualTo(64L);
        assertThat(count("SELECT count(*) FROM community_content WHERE content_id::text LIKE 'c4000000-0000-4000-8000-%'"))
                .isEqualTo(84L);
        assertThat(count("SELECT count(*) FROM moderation_cases WHERE moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%'"))
                .isEqualTo(24L);
        assertThat(count("SELECT count(*) FROM ai_content_scan_jobs WHERE job_id::text LIKE 'c7000000-0000-4000-8000-%'"))
                .isEqualTo(44L);
        assertThat(count("SELECT count(*) FROM ai_content_assessments WHERE assessment_id::text LIKE 'c8000000-0000-4000-8000-%'"))
                .isEqualTo(24L);
        assertThat(count("""
                SELECT count(*) FROM community_content
                 WHERE content_id::text LIKE 'c4000000-0000-4000-8000-%'
                   AND experience_tag IS NOT NULL
                   AND experience_tag NOT IN ('Trải nghiệm thực tế', 'Chăm sóc hằng ngày',
                                              'Hỗ trợ tinh thần', 'Thông tin tham khảo')
                """)).isZero();
    }

    private void assertExpertConsultationFixtures() {
        assertThat(count("SELECT count(*) FROM expert_availability WHERE availability_id::text LIKE '90000000-0000-4000-8000-%'"))
                .isGreaterThanOrEqualTo(30L);
        assertThat(count("SELECT count(*) FROM expert_consultation_requests WHERE id::text LIKE '94000000-0000-4000-8000-%'"))
                .isGreaterThanOrEqualTo(8L);
        assertThat(count("SELECT count(*) FROM consultation_bookings WHERE booking_id::text LIKE '91000000-0000-4000-8000-%'"))
                .isGreaterThanOrEqualTo(5L);
        assertThat(count("SELECT count(*) FROM direct_conversations WHERE conversation_id::text LIKE '93000000-0000-4000-8000-%'"))
                .isGreaterThanOrEqualTo(2L);
    }

    private void assertSeedAccountsAndProfiles() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT email FROM users WHERE email LIKE '%@carebridge.dev' ORDER BY email
                """, String.class)).containsExactlyInAnyOrderElementsOf(EXPECTED_SEED_EMAILS);
        assertThat(count("""
                SELECT count(*) FROM users
                 WHERE email LIKE '%@carebridge.dev'
                   AND phone IS NOT NULL
                   AND date_of_birth IS NOT NULL
                   AND area IS NOT NULL
                   AND enabled = TRUE
                   AND account_status = 'ACTIVE'
                   AND email_verified = TRUE
                   AND phone_verified = TRUE
                """)).isEqualTo(21L);
        assertThat(count("SELECT count(DISTINCT phone) FROM users WHERE email LIKE '%@carebridge.dev'"))
                .isEqualTo(21L);
        assertThat(seedNames()).containsExactlyInAnyOrderEntriesOf(expectedSeedNames());
        assertThat(count("""
                SELECT count(*) FROM users
                 WHERE email LIKE 'expert%@carebridge.dev'
                   AND verification_status = 'APPROVED'
                   AND trust_status = 'ACTIVE'
                   AND specialty IS NOT NULL
                   AND professional_title IS NOT NULL
                   AND experience_years IS NOT NULL
                   AND workplace IS NOT NULL
                   AND consultation_scope IS NOT NULL
                   AND consultation_fee_vnd IS NOT NULL
                """)).isEqualTo(6L);
    }

    private void assertNoDomainFixtures() {
        assertThat(count("SELECT count(*) FROM care_subjects")).isZero();
        assertThat(count("SELECT count(*) FROM mother_journeys")).isZero();
        assertThat(count("SELECT count(*) FROM care_groups")).isZero();
        assertThat(count("SELECT count(*) FROM care_group_members")).isZero();
        assertThat(count("SELECT count(*) FROM care_tasks")).isZero();
        assertThat(count("SELECT count(*) FROM health_observations")).isZero();
        assertThat(count("SELECT count(*) FROM expert_credentials")).isZero();
    }

    private List<Long> domainTableCounts() {
        return List.of(
                count("SELECT count(*) FROM care_subjects"),
                count("SELECT count(*) FROM mother_journeys"),
                count("SELECT count(*) FROM care_groups"),
                count("SELECT count(*) FROM care_group_members"),
                count("SELECT count(*) FROM care_tasks"),
                count("SELECT count(*) FROM health_observations"),
                count("SELECT count(*) FROM expert_credentials"),
                count("SELECT count(*) FROM expert_availability"),
                count("SELECT count(*) FROM expert_consultation_requests"),
                count("SELECT count(*) FROM consultation_bookings"));
    }

    private Map<String, String> seedNames() {
        return jdbcTemplate.query("""
                SELECT email, full_name FROM users WHERE email LIKE '%@carebridge.dev'
                """, resultSet -> {
            Map<String, String> names = new LinkedHashMap<>();
            while (resultSet.next()) {
                names.put(resultSet.getString("email"), resultSet.getString("full_name"));
            }
            return names;
        });
    }

    private static Map<String, String> expectedSeedNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("admin@carebridge.dev", "System Admin");
        names.put("moderator@carebridge.dev", "Moderator");
        names.put("content@carebridge.dev", "Content Admin");
        names.put("expert@carebridge.dev", "BS Đỗ Hải Long");
        names.put("mother@carebridge.dev", "Mẹ Bầu 1");
        names.put("family@carebridge.dev", "Chồng Mẹ Bầu 1");
        for (int index = 2; index <= 6; index++) {
            names.put("mother" + index + "@carebridge.dev", index == 2 ? "Mẹ Bầu 2" : "Mẹ bầu " + index);
            names.put("family" + index + "@carebridge.dev", "Chồng Mẹ Bầu " + index);
        }
        names.put("expert2@carebridge.dev", "BS Trần Thị Thu Nga");
        names.put("expert3@carebridge.dev", "BS Trần Văn Hoàng");
        names.put("expert4@carebridge.dev", "BS Nguyễn Văn Minh");
        names.put("expert5@carebridge.dev", "BS Nguyễn Thị Lan");
        names.put("expert6@carebridge.dev", "BS Lê Văn Bình");
        return Map.copyOf(names);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }
}
