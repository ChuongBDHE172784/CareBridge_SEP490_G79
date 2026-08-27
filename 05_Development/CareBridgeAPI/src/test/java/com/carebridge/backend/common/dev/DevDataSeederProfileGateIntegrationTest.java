package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class DevDataSeederProfileGateIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment()
                    .setActiveProfiles("prod", "dev"))
            .withPropertyValues(
                    "carebridge.dev-seed.enabled=true",
                    "carebridge.dev-seed.password=Synthetic-Only-Strong-Passphrase-6-10")
            .withUserConfiguration(CandidateConfiguration.class);

    @Test
    void seedBeanIsAbsentWhenProductionAndDevProfilesAreBothActive() {
        contextRunner.run(context -> assertThat(
                context.getBeansOfType(DevDataSeeder.class)).isEmpty());
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DevDataSeeder.class)
    static class CandidateConfiguration {
    }
}
