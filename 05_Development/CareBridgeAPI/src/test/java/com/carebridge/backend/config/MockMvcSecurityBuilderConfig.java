package com.carebridge.backend.config;

import com.carebridge.backend.systemconfiguration.security.MaintenanceModeFilter;
import com.carebridge.backend.systemconfiguration.service.SystemMaintenanceModeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Fallback;

import static org.mockito.Mockito.mock;
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
    @Fallback
    SystemMaintenanceModeService mockMvcSystemMaintenanceModeService() {
        return mock(SystemMaintenanceModeService.class);
    }

    @Bean("mockMvcJackson2ObjectMapper")
    @Fallback
    ObjectMapper mockMvcJackson2ObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean("mockMvcMaintenanceModeFilter")
    @Fallback
    MaintenanceModeFilter mockMvcMaintenanceModeFilter(
            SystemMaintenanceModeService maintenanceModeService, ObjectMapper objectMapper) {
        return new MaintenanceModeFilter(maintenanceModeService, objectMapper);
    }

    @Bean
    MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
        return builder -> builder.apply(springSecurity());
    }
}
