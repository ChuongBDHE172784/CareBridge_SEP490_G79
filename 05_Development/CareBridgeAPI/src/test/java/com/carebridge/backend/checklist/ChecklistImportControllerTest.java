package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.carebridge.backend.checklist.controller.UserChecklistItemController;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.exception.ChecklistControllerExceptionHandler;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.GlobalExceptionHandler;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Real import HTTP validation/security contracts for TC-009/023 and SEC-001/004. */
@WebMvcTest(
        value = UserChecklistItemController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ChecklistImportControllerTest {

    private static final String USER_ID = "69000000-0000-0000-0000-000000000001";
    private static final String IMPORT_URL = "/api/v1/user-checklist-items/import";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IUserChecklistItemService checklistService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void r69_007_scopedAdviceHasExplicitPrecedenceAndFreezesChecklistValidationCode()
            throws Exception {
        Order order = ChecklistControllerExceptionHandler.class.getAnnotation(Order.class);
        assertThat(order).as("checklist advice must declare deterministic precedence").isNotNull();
        assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);

        for (String invalidBody : List.of(
                "{\"templateItemIds\":[]}",
                "{\"templateItemIds\":[null]}",
                "{\"templateItemIds\":[not-json]}")) {
            mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("CHECKLIST-001"));
        }
        verifyNoInteractions(checklistService);
    }

    @Test
    void uc82_69_tc_009_missingJwtIs401AndFamilyIs403() throws Exception {
        mockMvc.perform(post(IMPORT_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("FAMILY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(checklistService);
    }

    @Test
    void uc82_69_tc_009_motherValidRequestReachesService() throws Exception {
        when(checklistService.importFromTemplate(any(), any())).thenReturn(List.of());
        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void uc82_69_tc_009_validFiftyTraversesHttpAndReachesServiceIntact() throws Exception {
        when(checklistService.importFromTemplate(any(), any())).thenReturn(List.of());

        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateItemIds\":" + uuidArray(50) + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<ImportFromTemplateRequest> request =
                ArgumentCaptor.forClass(ImportFromTemplateRequest.class);
        verify(checklistService).importFromTemplate(request.capture(), eq(UUID.fromString(USER_ID)));
        assertThat(request.getValue().templateItemIds()).hasSize(50).doesNotContainNull();
    }

    @Test
    void uc82_69_tc_009_malformedJsonReturnsNeutralChecklist001BeforeService() throws Exception {
        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateItemIds\":[not-json]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CHECKLIST-001"))
                .andExpect(jsonPath("$.message").value("Invalid checklist request"))
                .andExpect(content().string(not(containsString("JsonParseException"))))
                .andExpect(content().string(not(containsString("not-json"))));
        verifyNoInteractions(checklistService);
    }

    @Test
    void uc82_69_tc_023_zeroFiftyOneAndNullElementsFailBeforeService() throws Exception {
        assertInvalid("{\"templateItemIds\":[]}");
        assertInvalid("{\"templateItemIds\":" + uuidArray(51) + "}");
        assertInvalid("{\"templateItemIds\":[null]}");
        verifyNoInteractions(checklistService);
    }

    @Test
    void uc82_69_sec_003_unavailableItemUsesNeutralChecklist007WithoutUuidOrPolicyLeak()
            throws Exception {
        when(checklistService.importFromTemplate(any(), any())).thenThrow(new BusinessException(
                HttpStatus.NOT_FOUND, "CHECKLIST-007", "Template item not found or unavailable"));

        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CHECKLIST-007"))
                .andExpect(jsonPath("$.message").value("Template item not found or unavailable"))
                .andExpect(content().string(not(containsString("69000000-0000-0000-0000-000000000020"))))
                .andExpect(content().string(not(containsString("APPROVED"))))
                .andExpect(content().string(not(containsString("PREGNANCY"))));
    }

    @Test
    void uc82_69_tc_023_missingCanonicalContextUsesNeutralCnt013() throws Exception {
        when(checklistService.importFromTemplate(any(), any()))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateItemIds\":[\"69000000-0000-0000-0000-000000000020\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CNT-013"))
                .andExpect(jsonPath("$.message").value("Lifecycle content context unavailable"))
                .andExpect(jsonPath("$.path").value(IMPORT_URL))
                .andExpect(jsonPath("$.details")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void uc82_69_sec_004_denialResponseAndLogsOmitRequestSentinels() throws Exception {
        String privateItemId = "69000000-0000-0000-0000-000000000777";
        String privateToken = "PRIVATE-TOKEN-SENTINEL-69";
        String privateEmail = "private-69@example.invalid";
        BusinessException denial = new BusinessException(
                HttpStatus.NOT_FOUND, "CHECKLIST-007", "Template item not found or unavailable");
        denial.initCause(new IllegalStateException(
                "nested denial " + privateItemId,
                new IllegalArgumentException(privateToken + " " + privateEmail)));
        when(checklistService.importFromTemplate(any(), any())).thenThrow(denial);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        LoggerContext loggerContext = logger.getLoggerContext();
        ByteArrayOutputStream encodedLog = new ByteArrayOutputStream();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%level %logger - %msg%n%ex");
        encoder.start();
        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setOutputStream(encodedLog);
        appender.start();
        logger.addAppender(appender);
        try {
            String denialBody = mockMvc.perform(post(IMPORT_URL).with(csrf())
                            .with(user(USER_ID).roles("MOTHER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"templateItemIds\":[\"" + privateItemId + "\"]}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("CHECKLIST-007"))
                    .andReturn().getResponse().getContentAsString();
            String malformedBody = mockMvc.perform(post(IMPORT_URL).with(csrf())
                            .with(user(USER_ID).roles("MOTHER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"templateItemIds\":\"" + privateToken + " "+ privateEmail + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("CHECKLIST-001"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(denialBody + malformedBody)
                    .doesNotContain(privateItemId, privateToken, privateEmail,
                            "APPROVED", "PREGNANCY", "itemText");
            String capturedLogs = encodedLog.toString(StandardCharsets.UTF_8);
            assertThat(capturedLogs)
                    .doesNotContain(privateItemId, privateToken, privateEmail,
                            "APPROVED", "PREGNANCY", "itemText");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            encoder.stop();
        }
    }

    @Test
    void unmatchedGetOnExistingItemPathShapeReturnsNeutral405WithoutMethodMetadata()
            throws Exception {
        String body = mockMvc.perform(get("/api/v1/user-checklist-items/unmapped-route")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Request method not supported"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/user-checklist-items/unmapped-route"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(
                "HttpRequestMethodNotSupportedException", "Request method 'GET'",
                "PUT", "PATCH", "DELETE", "INTERNAL_ERROR");
        verifyNoInteractions(checklistService);
    }

    private void assertInvalid(String body) throws Exception {
        mockMvc.perform(post(IMPORT_URL).with(csrf()).with(user(USER_ID).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CHECKLIST-001"));
    }

    private String validBody() {
        return "{\"journeyId\":\"69000000-0000-0000-0000-000000000010\","
                + "\"templateItemIds\":[\"69000000-0000-0000-0000-000000000020\"]}";
    }

    private String uuidArray(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "\"" + new UUID(0L, index + 1L) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
