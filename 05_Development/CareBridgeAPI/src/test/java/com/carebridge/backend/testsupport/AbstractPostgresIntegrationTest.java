package com.carebridge.backend.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared base for real-database integration tests.
 *
 * <p>Spins up a single PostgreSQL container for the whole JVM (singleton pattern:
 * started once in a static initializer, reused by every subclass, torn down by
 * Ryuk on JVM exit). The datasource is wired to the container automatically via
 * {@link ServiceConnection}, and Spring Boot then runs the real Flyway migrations
 * (classpath:db/migration) against it.
 *
 * <p>The default {@code src/test/resources/application.yaml} targets H2 (Flyway
 * disabled, {@code create-drop}, H2 dialect). This base overrides those so the
 * context runs against Postgres exactly like production: Flyway migrations create
 * the schema and Hibernate merely validates it.
 *
 * <p>Subclasses add {@code @Transactional} (or manage their own cleanup) and inject
 * {@code MockMvc} / repositories to exercise the real controllers and services.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/migration/phase2",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.out-of-order=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        // Neutralise the H2 driver hard-coded in the test application.yaml; the
        // container connection details supplied by @ServiceConnection take over.
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        // Every distinct @MockitoBean set creates a separately cached Spring
        // context, hence a separate Hikari pool, while all contexts share this
        // one PostgreSQL container. Keep each test pool small and elastic so a
        // full suite cannot exhaust PostgreSQL's connection limit. Four still
        // supports the integration tests that exercise three concurrent writers.
        "spring.datasource.hikari.maximum-pool-size=4",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.idle-timeout=10000",
        // Provide a mail host so JavaMailSender autoconfigures and GmailEmailService
        // can be constructed. Tests still mock EmailService/SmsService so nothing is
        // actually sent; this just keeps the context bootable without mocking.
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "carebridge.mail.from-address=noreply@carebridge.test",
        "carebridge.mail.from-name=CareBridge",
        // Full application contexts also construct the direct-chat/Zego boundary.
        // Synthetic values keep integration tests hermetic; no external call is made.
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret",
        // Keep the DB clean and deterministic: each test class seeds its own data.
        // (Leaving the dev seeder on would re-insert the same accounts for every
        // Spring context sharing this singleton container and hit unique clashes.)
        "carebridge.dev-seed.enabled=false"
})
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
        try {
            // The checklist migrations refuse to run unless deployment has already
            // created the NOLOGIN owner roles they hand objects to — Flyway itself has
            // no CREATEROLE dependency by design. Provision them before the context
            // boots, exactly as the embedded-Postgres suites do.
            EmbeddedPostgresRoleFixture.provision(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not provision checklist database roles on the test container", exception);
        }
    }
}
