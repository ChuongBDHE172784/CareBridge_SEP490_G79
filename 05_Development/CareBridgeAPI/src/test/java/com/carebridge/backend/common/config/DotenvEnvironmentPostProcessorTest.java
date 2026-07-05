package com.carebridge.backend.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void parseDotenv_StripsQuotesAndIgnoresComments() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                # local development configuration
                JWT_SECRET=plain-secret-value
                SUPABASE_DB_URL="jdbc:postgresql://localhost:5432/postgres?sslmode=require&prepareThreshold=0"
                MAIL_PASSWORD='app password'
                export CAREBRIDGE_DEV_SEED_ENABLED=true
                INVALID-KEY=ignored
                """);

        Map<String, Object> values = DotenvEnvironmentPostProcessor.parseDotenv(dotenv);

        assertThat(values)
                .containsEntry("JWT_SECRET", "plain-secret-value")
                .containsEntry("SUPABASE_DB_URL",
                        "jdbc:postgresql://localhost:5432/postgres?sslmode=require&prepareThreshold=0")
                .containsEntry("MAIL_PASSWORD", "app password")
                .containsEntry("CAREBRIDGE_DEV_SEED_ENABLED", "true")
                .doesNotContainKey("INVALID-KEY");
    }
}
