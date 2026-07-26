package com.carebridge.backend.triage;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.IntakeController;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageContinuationService;
import com.carebridge.backend.triage.service.ITriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — THMC-TC-15 controller boundary (CRITICAL, CWE-602).
 * Oracle: BR-THMC-006 / TDS §15.2 smuggled-field sample. The service-layer half of
 * THMC-TC-15 lives in TriageServiceHealthMemoryContextTest.
 */
@WebMvcTest(
        value = IntakeController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class IntakeControllerHealthContextTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ITriageService triageService;
    @MockitoBean private ITriageContinuationService continuationService;
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private TriageGraphService triageGraphService;
    @MockitoBean private ObjectMapper objectMapper;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final String USER_A = "00000000-0000-0000-0000-0000000000a1";
    private static final String BABY_1 = "00000000-0000-0000-0000-00000000c001";

    @Test
    @WithMockUser(username = USER_A, roles = "MOTHER")
    void thmcTc15_smuggledHealthContext_isInertAtTheRequestBoundary() throws Exception {
        // Attack simulation: valid one-shot body augmented with a client-supplied healthContext
        when(triageService.runIntake(any(RunIntakeRequest.class), eq(UUID.fromString(USER_A))))
                .thenReturn(IntakeSessionResponse.builder()
                        .sessionId(UUID.fromString("00000000-0000-0000-0000-00000000e001"))
                        .stage("INFANT")
                        .status("COMPLETED")
                        .riskLevel("GREEN")
                        .build());

        mockMvc.perform(post("/api/v1/triage/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"INFANT\",\"babyProfileId\":\"" + BABY_1 + "\","
                                + "\"symptomList\":[\"ho\"],"
                                + "\"healthContext\":[{\"summaryText\":\"INJECTED_FOREIGN_HISTORY\"}]}"))
                // Normal flow status — NOT 400: unknown field ignored / never mapped
                .andExpect(status().isCreated());

        ArgumentCaptor<RunIntakeRequest> requestCaptor = ArgumentCaptor.forClass(RunIntakeRequest.class);
        verify(triageService).runIntake(requestCaptor.capture(), eq(UUID.fromString(USER_A)));
        RunIntakeRequest bound = requestCaptor.getValue();
        assertThat(bound.getStage()).isEqualTo(TriageStage.INFANT);
        assertThat(bound.getBabyProfileId()).isEqualTo(UUID.fromString(BABY_1));
        // Guard by design (C4): the public DTO has NO healthContext property to map into
        assertThat(Arrays.stream(RunIntakeRequest.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("healthContext");
    }
}
