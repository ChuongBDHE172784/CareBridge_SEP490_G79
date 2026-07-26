package com.carebridge.backend.consultation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(MockMvcSecurityBuilderConfig.class)
class ConsultationRequestApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private IZegoCloudService zegoCloudService;
    @MockitoBean private FcmService fcmService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentSameKeyReturnsOne201AndOne200WithOneResource() throws Exception {
        Fixture fixture = seedFixture();
        UUID clientRequestId = UUID.randomUUID();
        String body = requestBody(fixture.expertProfileId(), clientRequestId, "Nutrition");

        Future<MvcResult> first = executor.submit(() -> create(fixture.motherId(), body));
        Future<MvcResult> second = executor.submit(() -> create(fixture.motherId(), body));
        MvcResult firstResult = first.get(15, TimeUnit.SECONDS);
        MvcResult secondResult = second.get(15, TimeUnit.SECONDS);

        assertThat(List.of(
                        firstResult.getResponse().getStatus(),
                        secondResult.getResponse().getStatus()))
                .containsExactlyInAnyOrder(201, 200);
        UUID firstId = responseId(firstResult);
        UUID secondId = responseId(secondResult);
        assertThat(firstId).isEqualTo(secondId);
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isEqualTo(1);
        assertThat(countCreateAudits(firstId)).isEqualTo(1);
    }

    @Test
    void retryAfterTrustLossReturnsHttp200AndSameResourceWithoutNewSideEffects()
            throws Exception {
        Fixture fixture = seedFixture();
        UUID clientRequestId = UUID.randomUUID();
        String body = requestBody(fixture.expertProfileId(), clientRequestId, "Nutrition");

        MvcResult created = create(fixture.motherId(), body);
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        UUID requestId = responseId(created);
        long auditsBefore = countCreateAudits(requestId);
        jdbcTemplate.update(
                "UPDATE professional_profiles SET trust_status='REVOKED' WHERE professional_profile_id=?",
                fixture.expertProfileId());

        MvcResult retry = create(fixture.motherId(), body);

        assertThat(retry.getResponse().getStatus()).isEqualTo(200);
        assertThat(responseId(retry)).isEqualTo(requestId);
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isEqualTo(1);
        assertThat(countCreateAudits(requestId)).isEqualTo(auditsBefore);
    }

    @Test
    void sameKeyWithDifferentPayloadReturnsConreq009WithoutSecondRow() throws Exception {
        Fixture fixture = seedFixture();
        UUID clientRequestId = UUID.randomUUID();
        MvcResult created = create(
                fixture.motherId(),
                requestBody(fixture.expertProfileId(), clientRequestId, "Nutrition"));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);

        MvcResult conflict = create(
                fixture.motherId(),
                requestBody(fixture.expertProfileId(), clientRequestId, "Different topic"));

        assertThat(conflict.getResponse().getStatus()).isEqualTo(409);
        JsonNode error = objectMapper.readTree(conflict.getResponse().getContentAsString());
        assertThat(error.path("error").asText()).isEqualTo("CONREQ-009");
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isEqualTo(1);
    }

    @Test
    void differentKeysForSameMotherAndExpertCreateIndependentPendingRequests()
            throws Exception {
        Fixture fixture = seedFixture();
        UUID firstKey = UUID.randomUUID();
        UUID secondKey = UUID.randomUUID();

        MvcResult first = create(
                fixture.motherId(),
                requestBody(fixture.expertProfileId(), firstKey, "First request"));
        MvcResult second = create(
                fixture.motherId(),
                requestBody(fixture.expertProfileId(), secondKey, "Second request"));

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getStatus()).isEqualTo(201);
        assertThat(responseId(second)).isNotEqualTo(responseId(first));
        assertThat(countRequests(fixture.motherId(), firstKey)).isEqualTo(1);
        assertThat(countRequests(fixture.motherId(), secondKey)).isEqualTo(1);
    }

    @Test
    void assignedPageSizeAboveFiftyIsRejectedByHttpValidation() throws Exception {
        Fixture fixture = seedFixture();

        MvcResult result = mockMvc.perform(get("/api/v1/consultation-requests/assigned")
                        .with(user(fixture.expertUserId().toString()).roles("EXPERT"))
                        .param("size", "51"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.path("error").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void fcmFailureAfterCommitDoesNotRollbackCreatedRequest() throws Exception {
        Fixture fixture = seedFixture();
        jdbcTemplate.update("""
                INSERT INTO device_tokens
                    (id, user_id, token, platform, active, created_at, updated_at)
                VALUES (?, ?, 'fcm-token', 'ANDROID', true, now(), now())
                """, UUID.randomUUID(), fixture.expertUserId());
        CountDownLatch fcmAttempted = new CountDownLatch(1);
        doAnswer(invocation -> {
                    fcmAttempted.countDown();
                    throw new RuntimeException("FCM unavailable");
                })
                .when(fcmService)
                .sendWithRetry(anyString(), anyString(), anyString(), anyMap(), anyInt());
        UUID clientRequestId = UUID.randomUUID();

        MvcResult response = create(
                fixture.motherId(),
                requestBody(fixture.expertProfileId(), clientRequestId, "FCM isolation"));

        assertThat(response.getResponse().getStatus()).isEqualTo(201);
        assertThat(fcmAttempted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(countRequests(fixture.motherId(), clientRequestId)).isEqualTo(1);
    }

    private MvcResult create(UUID motherId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/consultation-requests")
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private UUID responseId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.path("data").path("id").asText());
    }

    private String requestBody(UUID expertProfileId, UUID clientRequestId, String topic)
            throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "clientRequestId", clientRequestId,
                "expertProfileId", expertProfileId,
                "topic", topic,
                "description", "Please advise on feeding."));
    }

    private Fixture seedFixture() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        seedUser(motherId, "API Mother", "MOTHER");
        seedUser(expertUserId, "API Expert", "EXPERT");
        jdbcTemplate.update("""
                INSERT INTO professional_profiles
                    (professional_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        return new Fixture(motherId, expertUserId, expertProfileId);
    }

    private void seedUser(UUID id, String name, String role) {
        CanonicalUserFixture.insertUser(jdbcTemplate, id, name, uniquePhone(), role);
    }

    private int countRequests(UUID motherId, UUID clientRequestId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM expert_consultation_requests
                 WHERE requester_user_id=? AND client_request_id=?
                """,
                Integer.class,
                motherId,
                clientRequestId);
    }

    private long countCreateAudits(UUID requestId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM audit_events
                 WHERE resource_type='CONSULTATION_REQUEST'
                   AND resource_id=?
                   AND event_category='MODERATION_ACTION'
                   AND event_origin='AUDIT_LOG'
                """,
                Long.class,
                requestId);
        return count == null ? 0 : count;
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private record Fixture(UUID motherId, UUID expertUserId, UUID expertProfileId) {
    }
}
