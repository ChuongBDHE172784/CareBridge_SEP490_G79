package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.controller.DirectLoginController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class DirectLoginDisabledIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment()
                    .setActiveProfiles("prod", "dev", "test"))
            .withPropertyValues("carebridge.auth.login-direct-enabled=true")
            .withUserConfiguration(CandidateConfiguration.class);

    @Test
    void loginDirectBeanIsAbsentWhenProductionIsMixedWithLocalProfiles() {
        contextRunner.run(context -> assertThat(
                context.getBeansOfType(DirectLoginController.class)).isEmpty());
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DirectLoginController.class)
    static class CandidateConfiguration {
    }
}
