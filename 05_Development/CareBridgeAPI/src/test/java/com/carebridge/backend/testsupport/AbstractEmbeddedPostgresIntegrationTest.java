package com.carebridge.backend.testsupport;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/** Real PostgreSQL 18 integration base for Windows environments without Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        // The P1/P2 writer barrier is intentionally armed before Flyway opens
        // its migration transaction. Production supplies this from the
        // deployment runner; embedded tests model that bootstrap explicitly.
        "spring.flyway.init-sql=DO $$ BEGIN PERFORM set_config('carebridge.checklist_v1_writes_frozen', 'true', false); PERFORM set_config('carebridge.checklist_p1_p2_role', 'MIGRATION', false); END $$;",
        "spring.flyway.baseline-on-migrate=false",
        "spring.flyway.out-of-order=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.hikari.maximum-pool-size=6",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.idle-timeout=10000",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "carebridge.mail.from-address=noreply@carebridge.test",
        "carebridge.mail.from-name=CareBridge",
        "carebridge.zego.app-id=1",
        "carebridge.zego.server-secret=synthetic-test-secret",
        "carebridge.dev-seed.enabled=false"
})
public abstract class AbstractEmbeddedPostgresIntegrationTest {

    protected static final EmbeddedPostgres POSTGRES = startPostgres();

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

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }
}
