package com.carebridge.backend.checklist.operations;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = ChecklistE2eEnvironmentAttestationController.class,
        properties = {
                "carebridge.checklist.e2e.attestation-enabled=true",
                "carebridge.checklist.e2e.disposable=true",
                "carebridge.checklist.e2e.environment-id=checklist-e2e-test-01"
        },
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ChecklistE2eEnvironmentAttestationControllerTest {

    private static final String ENDPOINT = "/api/v1/operations/checklist-e2e/attestation";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext applicationContext;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @ParameterizedTest
    @ValueSource(strings = {"SYSTEM_ADMIN", "OPERATIONS"})
    void operationsScopesReceiveDisposableEnvironmentAttestation(String role) throws Exception {
        mockMvc.perform(get(ENDPOINT).with(user("10000000-0000-0000-0000-000000000001").roles(role)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.environmentId").value("checklist-e2e-test-01"))
                .andExpect(jsonPath("$.disposable").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTHER", "FAMILY", "CONTENT_ADMIN"})
    void nonOperationsScopesAreForbidden(String role) throws Exception {
        mockMvc.perform(get(ENDPOINT).with(user("10000000-0000-0000-0000-000000000002").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(jwtTokenProvider, userRepository);
    }

    @Test
    void controllerBeanExistsOnlyForTheExplicitAttestationProfile() {
        org.assertj.core.api.Assertions.assertThat(
                        applicationContext.getBean(ChecklistE2eEnvironmentAttestationController.class))
                .isNotNull();
    }

    @Test
    void constructorFailsClosedForNonDisposableOrUnnamedEnvironment() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new ChecklistE2eEnvironmentAttestationController("valid-id", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disposable");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new ChecklistE2eEnvironmentAttestationController(" ", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("environment ID");
    }
}
