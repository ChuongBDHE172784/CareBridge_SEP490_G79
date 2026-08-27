package com.carebridge.backend.triage;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.TriageSessionController;
import com.carebridge.backend.triage.dto.response.TriageSessionResponse;
import com.carebridge.backend.triage.service.ITriageSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TriageSessionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CanonicalTriageSessionControllerTest {
    private static final String CANONICAL = "/api/v1/triage/sessions";
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SESSION = UUID.fromString("00000000-0000-4000-8000-000000000020");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ITriageSessionService service;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void canonicalStart_requiresAuthentication() throws Exception {
        mockMvc.perform(post(CANONICAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void canonicalStart_delegatesToTheCanonicalSessionService() throws Exception {
        when(service.start(any(), eq(USER))).thenReturn(response(1));

        mockMvc.perform(post(CANONICAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").value(SESSION.toString()))
                .andExpect(jsonPath("$.data.stateVersion").value(1));

        verify(service).start(any(), eq(USER));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void canonicalStart_requiresAnExplicitTargetAndStage() throws Exception {
        mockMvc.perform(post(CANONICAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"journeyContext":{},"message":"dau dau",
                                 "messageId":"message_1234567890","requestId":"request_1234567890",
                                 "consentContext":{},"signals":{},"measurements":{}}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "FAMILY")
    void canonicalContinueGetAndCancel_delegateWithTheAuthenticatedOwner() throws Exception {
        when(service.continueSession(any(), eq(USER))).thenReturn(response(2));
        when(service.get(SESSION, USER)).thenReturn(response(2));
        when(service.cancel(SESSION, 2, USER)).thenReturn(response(3));

        mockMvc.perform(post(CANONICAL + "/" + SESSION + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"%s","expectedStateVersion":1,
                                 "message":"khong biet","messageId":"message_1234567890",
                                 "requestId":"request_1234567890","answers":[],
                                 "signals":{},"measurements":{}}
                                """.formatted(SESSION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stateVersion").value(2));
        mockMvc.perform(get(CANONICAL + "/" + SESSION))
                .andExpect(status().isOk());
        mockMvc.perform(delete(CANONICAL + "/" + SESSION)
                        .param("expectedStateVersion", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stateVersion").value(3));

        verify(service).continueSession(any(), eq(USER));
        verify(service).get(SESSION, USER);
        verify(service).cancel(SESSION, 2, USER);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void continue_rejectsMismatchedPathAndBodyBeforeDelegation() throws Exception {
        mockMvc.perform(post(CANONICAL + "/" + SESSION + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"00000000-0000-4000-8000-000000000099",
                                 "expectedStateVersion":1,"message":"khong biet",
                                 "messageId":"message_1234567890","requestId":"request_1234567890",
                                 "answers":[],"signals":{},"measurements":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TRIAGE_SESSION_ID_MISMATCH"));

        verifyNoInteractions(service);
    }

    private static String startBody() {
        return """
                {"selectedTarget":"MOTHER","selectedStage":"PREGNANCY",
                 "journeyContext":{},"message":"dau dau",
                 "messageId":"message_1234567890","requestId":"request_1234567890",
                 "consentContext":{},"signals":{},"measurements":{}}
                """;
    }

    private static TriageSessionResponse response(int version) {
        return new TriageSessionResponse(
                SESSION, version, "MOTHER", "SYMPTOM_TRIAGE", "PREGNANCY",
                "NEEDS_MORE_INFO", "ASK_CLARIFYING_QUESTIONS", false,
                List.of("Q_GLOBAL_DANGER"), List.of(), "IN_SCOPE", List.of(), null,
                "2.2.0", "a".repeat(64), "", "UNAVAILABLE", List.of(),
                "Thong tin tham khao", Map.of());
    }
}
