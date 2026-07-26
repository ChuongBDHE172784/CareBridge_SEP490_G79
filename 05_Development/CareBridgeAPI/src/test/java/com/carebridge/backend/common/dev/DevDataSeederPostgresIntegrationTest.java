package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
        properties = {
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
            "carebridge.dev-seed.password=Test@1234"
        })
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevDataSeederPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.1-alpine");

    @Autowired
    private DevDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Test
    void seedsCanonicalFixturesAndRemainsIdempotent() throws Exception {
        assertThat(count("SELECT count(*) FROM users WHERE email LIKE '%@carebridge.dev'"))
                .isEqualTo(14L);
        assertThat(count("""
                SELECT count(*)
                  FROM users u
                  JOIN persons p ON p.person_id = u.person_id
                  JOIN care_subjects cs
                    ON cs.owner_user_id = u.user_id
                   AND cs.person_id = p.person_id
                   AND cs.subject_type = 'MOTHER'
                  JOIN mother_journeys j
                    ON j.care_subject_id = cs.care_subject_id
                   AND j.owner_user_id = u.user_id
                   AND cs.mother_journey_id = j.journey_id
                 WHERE u.email IN ('mother3@carebridge.dev', 'mother4@carebridge.dev')
                """)).isEqualTo(2L);
        assertThat(count("""
                SELECT count(*)
                  FROM growth_measurements gm
                  JOIN care_subjects cs ON cs.care_subject_id = gm.care_subject_id
                 WHERE cs.subject_type = 'BABY'
                """)).isEqualTo(5L);
        assertThat(count("""
                SELECT count(*)
                  FROM expert_credentials ec
                  JOIN professional_profiles pp
                    ON pp.professional_profile_id = ec.professional_profile_id
                  JOIN users u ON u.user_id = pp.user_id
                 WHERE u.email IN ('expert2@carebridge.dev', 'expert3@carebridge.dev')
                """)).isEqualTo(2L);
        assertThat(count("""
                SELECT count(*)
                  FROM expert_availability ea
                  JOIN professional_profiles pp
                    ON pp.professional_profile_id = ea.professional_profile_id
                  JOIN users u ON u.user_id = pp.user_id
                 WHERE u.email IN ('expert2@carebridge.dev', 'expert3@carebridge.dev')
                """)).isEqualTo(2L);

        List<Long> beforeSecondRun = fixtureCounts();
        seeder.run(new DefaultApplicationArguments());
        assertThat(fixtureCounts()).isEqualTo(beforeSecondRun);
    }

    private List<Long> fixtureCounts() {
        return List.of(
                count("SELECT count(*) FROM users"),
                count("SELECT count(*) FROM persons"),
                count("SELECT count(*) FROM care_subjects"),
                count("SELECT count(*) FROM mother_journeys"),
                count("SELECT count(*) FROM growth_measurements"),
                count("SELECT count(*) FROM care_logs"),
                count("SELECT count(*) FROM professional_profiles"),
                count("SELECT count(*) FROM expert_credentials"),
                count("SELECT count(*) FROM expert_availability"),
                count("SELECT count(*) FROM community_content"),
                count("SELECT count(*) FROM content_items"));
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }
}
