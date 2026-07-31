package com.carebridge.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Test configuration that applies springSecurity() to MockMvc.
 *
 * Required in Spring Boot 4.0 @WebMvcTest because MockMvcSecurityAutoConfiguration
 * is not included in AutoConfigureMockMvc.imports for the new webmvc test module.
 * Without this, @WithMockUser and SecurityMockMvcRequestPostProcessors have no effect.
 */
@TestConfiguration
public class MockMvcSecurityBuilderConfig {

    @Bean
    MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
        return builder -> builder.apply(springSecurity());
    }
}
