package com.carebridge.backend.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void parseDotenv_StripsQuotesAndIgnoresComments() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                # local development configuration
                JWT_ACTIVE_KEY_ID=test-key-2026-07
                JWT_PRIVATE_KEY=MIIEvSyntheticBase64==
                JWT_PUBLIC_KEYS=test-key-2026-07:MIIBSyntheticBase64==
                SUPABASE_DB_URL="jdbc:postgresql://localhost:5432/postgres?sslmode=require&prepareThreshold=0"
                MAIL_PASSWORD='app password'
                export CAREBRIDGE_DEV_SEED_ENABLED=true
                INVALID-KEY=ignored
                """);

        Map<String, Object> values = DotenvEnvironmentPostProcessor.parseDotenv(dotenv);

        assertThat(values)
                .containsEntry("JWT_ACTIVE_KEY_ID", "test-key-2026-07")
                .containsEntry("JWT_PRIVATE_KEY", "MIIEvSyntheticBase64==")
                .containsEntry("JWT_PUBLIC_KEYS", "test-key-2026-07:MIIBSyntheticBase64==")
                .containsEntry("SUPABASE_DB_URL",
                        "jdbc:postgresql://localhost:5432/postgres?sslmode=require&prepareThreshold=0")
                .containsEntry("MAIL_PASSWORD", "app password")
                .containsEntry("CAREBRIDGE_DEV_SEED_ENABLED", "true")
                .doesNotContainKey("INVALID-KEY");
    }

    @Test
    void postProcessEnvironment_DotenvOutranksConfigDataByDefault() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "SUPABASE_DB_URL=jdbc:postgresql://live.example.test:5432/postgres");
        StandardEnvironment environment = environmentWithConfigData(Map.of(
                "SUPABASE_DB_URL", "jdbc:postgresql://127.0.0.1:5432/test"));

        new DotenvEnvironmentPostProcessor(dotenv)
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("SUPABASE_DB_URL"))
                .isEqualTo("jdbc:postgresql://live.example.test:5432/postgres");
    }

    @Test
    void postProcessEnvironment_WhenDisabled_DoesNotLoadDotenv() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "SUPABASE_DB_URL=jdbc:postgresql://live.example.test:5432/postgres");
        StandardEnvironment environment = environmentWithConfigData(Map.of(
                "carebridge.dotenv.enabled", false,
                "SUPABASE_DB_URL", "jdbc:postgresql://127.0.0.1:5432/test"));

        new DotenvEnvironmentPostProcessor(dotenv)
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().contains("carebridgeDotenv")).isFalse();
        assertThat(environment.getProperty("SUPABASE_DB_URL"))
                .isEqualTo("jdbc:postgresql://127.0.0.1:5432/test");
    }

    @Test
    void postProcessEnvironment_SystemEnvironmentStillOutranksDotenv() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "SUPABASE_DB_URL=jdbc:postgresql://dotenv.example.test:5432/postgres");
        StandardEnvironment environment = environmentWithConfigData(Map.of());
        environment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                        "SUPABASE_DB_URL", "jdbc:postgresql://system.example.test:5432/postgres")));

        new DotenvEnvironmentPostProcessor(dotenv)
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("SUPABASE_DB_URL"))
                .isEqualTo("jdbc:postgresql://system.example.test:5432/postgres");
    }

    private StandardEnvironment environmentWithConfigData(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("configData", properties));
        return environment;
    }
}
