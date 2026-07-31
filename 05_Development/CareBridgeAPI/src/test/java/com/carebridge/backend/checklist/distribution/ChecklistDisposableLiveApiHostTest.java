package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.testsupport.EmbeddedPostgresRoleFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Opt-in disposable live-API host for CHK-042/043. The test blocks until the operator creates
 * the configured stop file, so it is disabled unless CHK_E2E_HOST_RUN=true is explicit.
 */
@EnabledOnOs(OS.WINDOWS)
@EnabledIfEnvironmentVariable(named = "CHK_E2E_HOST_RUN", matches = "true")
@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=false",
        "spring.flyway.out-of-order=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.hikari.maximum-pool-size=8",
        "spring.datasource.hikari.minimum-idle=0",
        // The dev mail bean and test mail bean intentionally share the same contract in this
        // opt-in live host; the override stays scoped to this test ApplicationContext only.
        "spring.main.allow-bean-definition-overriding=true",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "carebridge.mail.from-address=noreply@carebridge.test",
        "carebridge.mail.from-name=CareBridge",
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-e2e-secret",
        "carebridge.dev-seed.enabled=true",
        "carebridge.dev-seed.extended-content-enabled=false",
        "carebridge.firebase.firestore.enabled=false",
        "carebridge.firebase.auth-emulator-enabled=false"
})
class ChecklistDisposableLiveApiHostTest {

    private static final int PORT = 18080;
    private static final Duration MAX_HOST_LIFETIME = Duration.ofMinutes(25);

    @Autowired private AuthService authService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String environmentId = requiredEnvironment("CHK_E2E_SERVER_ENVIRONMENT_ID");
        if (!environmentId.matches("^e2e-[A-Za-z0-9][A-Za-z0-9._:-]{2,79}$")) {
            throw new IllegalStateException("CHK_E2E_SERVER_ENVIRONMENT_ID_INVALID");
        }
        registry.add("server.address", () -> "127.0.0.1");
        registry.add("server.port", () -> PORT);
        registry.add("spring.datasource.url", () -> postgres().getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("carebridge.dev-seed.password",
                () -> requiredEnvironment("CAREBRIDGE_DEV_SEED_PASSWORD"));
        registry.add("carebridge.checklist.e2e.attestation-enabled", () -> true);
        registry.add("carebridge.checklist.e2e.disposable", () -> true);
        registry.add("carebridge.checklist.e2e.environment-id", () -> environmentId);
    }

    @AfterAll
    static void closePostgres() throws Exception {
        postgres().close();
    }

    @Test
    void servesDisposableApiUntilOperatorSignalsStop() throws Exception {
        Path definesPath = absoluteExternalPath("CHK_E2E_DEFINES_FILE");
        Path readyPath = absoluteExternalPath("CHK_E2E_READY_FILE");
        Path stopPath = absoluteExternalPath("CHK_E2E_STOP_FILE");
        assertThat(Set.of(definesPath, readyPath, stopPath))
                .as("defines, ready, and stop artifacts must be distinct")
                .hasSize(3);

        try {
            assertThat(Files.exists(stopPath)).as("stale stop signal").isFalse();
            assertDisposableSeedBoundary();
            seedReviewedContextMappings();
            Map<String, String> defines = new LinkedHashMap<>();
            defines.put("API_BASE_URL", "http://10.0.2.2:" + PORT);
            defines.put("CHK_API_E2E", "true");
            defines.put("CHK_E2E_ENVIRONMENT", "DISPOSABLE_NON_PRODUCTION");
            defines.put("CHK_E2E_SERVER_ENVIRONMENT_ID",
                    requiredEnvironment("CHK_E2E_SERVER_ENVIRONMENT_ID"));
            defines.put("CHK_E2E_EXPECTED_API_BASE_URL", "http://10.0.2.2:" + PORT);
            defines.put("CHK_E2E_DEVICE_ACK", "DEDICATED_DEVICE_CONFIRMED");
            defines.put("CHK_E2E_CREDENTIAL_ARTIFACT_ACK", "COMPILED_CREDENTIAL_ARTIFACT_ACCEPTED");
            defines.put("CHK_E2E_ALLOW_LOOPBACK_HTTP", "LOOPBACK_ONLY_CONFIRMED");
            addSession(defines, "CONTENT_ADMIN", "content@carebridge.dev");
            addSession(defines, "ADMIN", "admin@carebridge.dev");
            addSession(defines, "MOTHER", "mother4@carebridge.dev");
            addSession(defines, "FAMILY", "family3@carebridge.dev");
            addSession(defines, "ISOLATION_FAMILY", "family2@carebridge.dev");

            Files.createDirectories(definesPath.getParent());
            objectMapper.writeValue(definesPath.toFile(), defines);
            Files.writeString(readyPath, "READY http://127.0.0.1:" + PORT + System.lineSeparator());

            Instant deadline = Instant.now().plus(MAX_HOST_LIFETIME);
            while (!Files.exists(stopPath) && Instant.now().isBefore(deadline)) {
                Thread.sleep(500L);
            }
            assertThat(Files.exists(stopPath)).as("operator stop signal before host deadline").isTrue();
        } finally {
            deleteArtifactsBestEffort(List.of(definesPath, readyPath, stopPath));
        }
    }

    private void assertDisposableSeedBoundary() {
        Long acceptedPairMembers = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM care_group_members member
                  JOIN care_groups group_row ON group_row.care_group_id = member.care_group_id
                  JOIN users actor ON actor.user_id = member.user_id
                  JOIN users owner ON owner.user_id = group_row.owner_user_id
                 WHERE member.invitation_status='ACCEPTED'
                   AND owner.email='mother4@carebridge.dev'
                   AND actor.email IN ('mother4@carebridge.dev', 'family3@carebridge.dev')
                """, Long.class);
        assertThat(acceptedPairMembers).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM community_content", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM content_items", Long.class)).isEqualTo(2L);
    }

    private static void deleteArtifactsBestEffort(List<Path> paths) {
        RuntimeException firstFailure = null;
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception exception) {
                if (firstFailure == null) {
                    firstFailure = new IllegalStateException("CHK_E2E_ARTIFACT_CLEANUP_FAILED", exception);
                } else {
                    firstFailure.addSuppressed(exception);
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    private void addSession(Map<String, String> defines, String key, String email) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(requiredEnvironment("CAREBRIDGE_DEV_SEED_PASSWORD"));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        servletRequest.addHeader("User-Agent", "CareBridge-Disposable-E2E");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        try {
            AuthResponse response = authService.login(request);
            assertThat(response.getUser()).isNotNull();
            defines.put("CHK_" + key + "_ACCESS_TOKEN", response.getAccessToken());
            defines.put("CHK_" + key + "_REFRESH_TOKEN", response.getRefreshToken());
            defines.put("CHK_" + key + "_USER_ID", response.getUser().getId().toString());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void seedReviewedContextMappings() {
        jdbcTemplate.update("""
                INSERT INTO checklist_care_group_contexts (
                    care_group_id, owner_user_id, care_context_type, care_context_id,
                    review_status, distribution_blocked, reviewed_at, reviewed_by)
                SELECT group_row.care_group_id, group_row.owner_user_id, 'JOURNEY',
                       group_row.linked_journey_id, 'REVIEWED', false, now(), admin_user.user_id
                  FROM care_groups group_row
                  CROSS JOIN users admin_user
                 WHERE group_row.status='ACTIVE'
                   AND group_row.linked_journey_id IS NOT NULL
                   AND admin_user.email='admin@carebridge.dev'
                ON CONFLICT (care_group_id, care_context_type, care_context_id)
                DO UPDATE SET review_status='REVIEWED', distribution_blocked=false,
                              block_reason_code=NULL, reviewed_at=EXCLUDED.reviewed_at,
                              reviewed_by=EXCLUDED.reviewed_by
                """);
        jdbcTemplate.update("""
                INSERT INTO checklist_care_group_contexts (
                    care_group_id, owner_user_id, care_context_type, care_context_id,
                    review_status, distribution_blocked, reviewed_at, reviewed_by)
                SELECT group_row.care_group_id, group_row.owner_user_id, 'BABY',
                       group_row.linked_baby_profile_id, 'REVIEWED', false, now(), admin_user.user_id
                  FROM care_groups group_row
                  CROSS JOIN users admin_user
                 WHERE group_row.status='ACTIVE'
                   AND group_row.linked_baby_profile_id IS NOT NULL
                   AND admin_user.email='admin@carebridge.dev'
                ON CONFLICT (care_group_id, care_context_type, care_context_id)
                DO UPDATE SET review_status='REVIEWED', distribution_blocked=false,
                              block_reason_code=NULL, reviewed_at=EXCLUDED.reviewed_at,
                              reviewed_by=EXCLUDED.reviewed_by
                """);
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setServerConfig("max_connections", "100")
                    .start();
            EmbeddedPostgresRoleFixture.provision(postgres.getPostgresDatabase());
            return postgres;
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static EmbeddedPostgres postgres() {
        return PostgresHolder.INSTANCE;
    }

    private static Path absoluteExternalPath(String name) {
        Path configured = Path.of(requiredEnvironment(name));
        if (!configured.isAbsolute()) {
            throw new IllegalStateException(name + "_ABSOLUTE_PATH_REQUIRED");
        }
        Path normalized = configured.normalize();
        Path repositoryRoot = existingCanonicalDirectory("CHK_E2E_REPOSITORY_ROOT");
        if (!Files.isDirectory(repositoryRoot.resolve(".git"))
                || !Files.isDirectory(repositoryRoot.resolve("05_Development"))) {
            throw new IllegalStateException("CHK_E2E_REPOSITORY_ROOT_INVALID");
        }
        Path configuredArtifactRoot = Path.of(requiredEnvironment("CHK_E2E_ARTIFACT_ROOT"));
        if (!configuredArtifactRoot.isAbsolute()) {
            throw new IllegalStateException("CHK_E2E_ARTIFACT_ROOT_ABSOLUTE_PATH_REQUIRED");
        }
        try {
            Files.createDirectories(configuredArtifactRoot);
        } catch (Exception exception) {
            throw new IllegalStateException("CHK_E2E_ARTIFACT_ROOT_UNAVAILABLE", exception);
        }
        Path artifactRoot = existingCanonicalDirectory("CHK_E2E_ARTIFACT_ROOT");
        if (artifactRoot.startsWith(repositoryRoot)) {
            throw new IllegalStateException("CHK_E2E_ARTIFACT_ROOT_MUST_BE_OUTSIDE_REPOSITORY");
        }
        if (!artifactRoot.equals(normalized.getParent()) || Files.isSymbolicLink(normalized)) {
            throw new IllegalStateException(name + "_OUTSIDE_ARTIFACT_ROOT");
        }
        return normalized;
    }

    private static Path existingCanonicalDirectory(String name) {
        Path configured = Path.of(requiredEnvironment(name));
        if (!configured.isAbsolute()) {
            throw new IllegalStateException(name + "_ABSOLUTE_PATH_REQUIRED");
        }
        try {
            return configured.toRealPath();
        } catch (Exception exception) {
            throw new IllegalStateException(name + "_CANONICAL_DIRECTORY_REQUIRED", exception);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + "_REQUIRED");
        }
        return value;
    }

    private static final class PostgresHolder {
        private static final EmbeddedPostgres INSTANCE = startPostgres();
    }
}
