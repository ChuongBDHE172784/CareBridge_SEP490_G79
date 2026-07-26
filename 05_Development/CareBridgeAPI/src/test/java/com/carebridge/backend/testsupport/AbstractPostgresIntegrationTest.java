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
        // db/testfix holds TEST-ONLY shim migrations that let the immutable
        // post-baseline chain succeed on a fresh (baseline-path) database; see
        // src/test/resources/db/testfix/*.sql headers for the rationale.
        "spring.flyway.locations=classpath:db/migration,classpath:db/testfix",
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

    // The init script pre-creates the carebridge_migration_bridge tables that
    // post-baseline migrations still reference. On a fresh database Flyway takes
    // the baseline path (B20260724111500) and skips the pre-baseline migrations
    // that created those bridges, so without this bootstrap the very first
    // Flyway run fails in V20260724210000 ("relation ... does not exist").
    // See src/test/resources/testsupport/bridge-bootstrap.sql for details.
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withInitScript("testsupport/bridge-bootstrap.sql");

    static {
        POSTGRES.start();
    }
}
