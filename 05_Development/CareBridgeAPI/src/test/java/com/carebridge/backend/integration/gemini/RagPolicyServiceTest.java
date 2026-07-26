package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.support.Story69TestFactory;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagAudienceContext;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;
import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.carebridge.backend.integration.gemini.filter.RagSafetyFilter;
import com.carebridge.backend.integration.gemini.service.RagPolicyServiceImpl;
import com.carebridge.backend.integration.gemini.service.RagService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

/** RED ordering and canonical-stage contracts for RAG-001..004. */
@ExtendWith(MockitoExtension.class)
class RagPolicyServiceTest {

    @Mock private RagSafetyFilter safetyFilter;
    @Mock private LifecycleContentStageResolver lifecycleContentStageResolver;
    @Mock private RagService ragService;
    @InjectMocks private RagPolicyServiceImpl policy;

    private RagAnswerRequest request(UserStage userStage) {
        return RagAnswerRequest.builder().query("synthetic query").userStage(userStage)
                .maxContextChunks(3).build();
    }

    @Test
    void uc82_69_rag_001_redSafetyReturnsBeforeLifecycleAndGenerator() {
        when(safetyFilter.check("synthetic query"))
                .thenReturn(RagSafetyResult.redFlag("Seek urgent help"));

        RagAnswerResponse response = policy.generateAnswer(request(UserStage.PREGNANCY),
                new RagAudienceContext(UUID.randomUUID(), true));

        assertThat(response.getAnswer()).isEqualTo("Seek urgent help");
        assertThat(response.isFallback()).isTrue();
        verifyNoInteractions(lifecycleContentStageResolver, ragService);
    }

    @Test
    void uc82_69_rag_002_motherIgnoresClientStageAndDelegatesCanonicalStage() {
        UUID motherId = UUID.randomUUID();
        RagAnswerRequest request = request(UserStage.POSTPARTUM);
        RagAnswerResponse downstream = RagAnswerResponse.builder().answer("safe")
                .disclaimer("disclaimer").build();
        when(safetyFilter.check("synthetic query")).thenReturn(RagSafetyResult.safe());
        when(lifecycleContentStageResolver.resolve(motherId)).thenReturn(ContentStage.PREGNANCY);
        when(ragService.generateAnswer(org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any())).thenReturn(downstream);

        assertThat(policy.generateAnswer(request, new RagAudienceContext(motherId, true)))
                .isSameAs(downstream);
        ArgumentCaptor<RagExecutionContext> captor = ArgumentCaptor.forClass(RagExecutionContext.class);
        verify(ragService).generateAnswer(org.mockito.ArgumentMatchers.eq(request), captor.capture());
        assertThat(captor.getValue().mother()).isTrue();
        assertThat(captor.getValue().canonicalStage()).isEqualTo(ContentStage.PREGNANCY);
        assertThat(captor.getValue().promptStage()).isEqualTo(UserStage.PREGNANCY);
    }

    @ParameterizedTest
    @MethodSource("canonicalStageMappings")
    void uc82_69_rag_002_mapsEveryMotherStageAndIgnoresEachClientStage(
            ContentStage canonicalStage, UserStage expectedPromptStage, UserStage clientStage) {
        UUID motherId = UUID.randomUUID();
        RagAnswerRequest request = request(clientStage);
        RagAnswerResponse downstream = RagAnswerResponse.builder().answer("safe")
                .disclaimer("disclaimer").build();
        when(safetyFilter.check("synthetic query")).thenReturn(RagSafetyResult.safe());
        when(lifecycleContentStageResolver.resolve(motherId)).thenReturn(canonicalStage);
        when(ragService.generateAnswer(org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any())).thenReturn(downstream);

        assertThat(policy.generateAnswer(request, new RagAudienceContext(motherId, true)))
                .isSameAs(downstream);

        ArgumentCaptor<RagExecutionContext> captor = ArgumentCaptor.forClass(RagExecutionContext.class);
        verify(ragService).generateAnswer(org.mockito.ArgumentMatchers.eq(request), captor.capture());
        assertThat(captor.getValue())
                .extracting(RagExecutionContext::mother,
                        RagExecutionContext::canonicalStage,
                        RagExecutionContext::promptStage)
                .containsExactly(true, canonicalStage, expectedPromptStage);
        assertThat(captor.getValue().promptStage()).isNotEqualTo(clientStage);
    }

    @Test
    void uc82_69_rag_003_missingMotherLifecycleStopsBeforeEveryDownstreamGenerator() {
        UUID motherId = UUID.randomUUID();
        when(safetyFilter.check("synthetic query")).thenReturn(RagSafetyResult.safe());
        when(lifecycleContentStageResolver.resolve(motherId))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        assertThatThrownBy(() -> policy.generateAnswer(
                request(UserStage.POSTPARTUM), new RagAudienceContext(motherId, true)))
                .isInstanceOfSatisfying(ContentException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("CNT-013");
                    assertThat(error.getHttpStatus().value()).isEqualTo(409);
                });

        verifyNoInteractions(ragService);
    }

    @ParameterizedTest(name = "RAG-004 generic policy role {0} keeps {1}")
    @MethodSource("genericAllowedRoleStages")
    void uc82_69_rag_004_eachAllowedNonMotherRoleSkipsLifecycleAndKeepsRequestStage(
            String role, UserStage requestedStage) {
        UUID callerId = UUID.nameUUIDFromBytes(role.getBytes(StandardCharsets.UTF_8));
        RagAnswerRequest request = request(requestedStage);
        RagAnswerResponse downstream = RagAnswerResponse.builder().answer("safe")
                .disclaimer("disclaimer").build();
        when(safetyFilter.check("synthetic query")).thenReturn(RagSafetyResult.safe());
        when(ragService.generateAnswer(org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any())).thenReturn(downstream);

        assertThat(policy.generateAnswer(request, new RagAudienceContext(callerId, false)))
                .isSameAs(downstream);

        verifyNoInteractions(lifecycleContentStageResolver);
        ArgumentCaptor<RagExecutionContext> execution =
                ArgumentCaptor.forClass(RagExecutionContext.class);
        verify(ragService).generateAnswer(org.mockito.ArgumentMatchers.eq(request),
                execution.capture());
        assertThat(execution.getValue())
                .extracting(RagExecutionContext::mother,
                        RagExecutionContext::canonicalStage,
                        RagExecutionContext::promptStage)
                .containsExactly(false, null, requestedStage);
    }

    @Test
    void uc82_69_rag_001_safetyRedReturnsBeforeLifecycleOrGenerator() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/integration/gemini/service/RagPolicyServiceImpl.java");
        source = source.substring(source.indexOf("generateAnswer"),
                source.indexOf("UserStage mapCanonicalStage"));
        int safety = source.indexOf("safetyFilter.check");
        int redReturn = source.indexOf("isRedFlag");
        int lifecycle = source.indexOf("lifecycleContentStageResolver");
        int delegate = source.indexOf("ragService.generateAnswer");
        assertThat(safety).isGreaterThanOrEqualTo(0);
        assertThat(redReturn).isGreaterThan(safety);
        assertThat(lifecycle).isGreaterThan(redReturn);
        assertThat(delegate).isGreaterThan(lifecycle);
    }

    @Test
    void uc82_69_rag_002_004_policySeparatesMotherCanonicalAndGenericRolePaths() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/integration/gemini/service/RagPolicyServiceImpl.java");
        assertThat(source.contains("context.mother()")
                        && source.contains("mapCanonicalStage")
                        && source.contains("request.getUserStage()"))
                .as("RAG-002/004: Mother overrides userStage while non-Mother retains it")
                .isTrue();
        assertThat(source.contains("PRE_PREGNANCY")
                        && source.contains("PREGNANCY")
                        && source.contains("POSTPARTUM")
                        && source.contains("BABY_CARE"))
                .as("RAG-002: canonical mapping is exhaustive and rejects BABY_CARE")
                .isTrue();
    }

    static Stream<Arguments> canonicalStageMappings() {
        return Stream.of(
                Arguments.of(ContentStage.PRE_PREGNANCY,
                        UserStage.PRE_PREGNANCY, UserStage.POSTPARTUM),
                Arguments.of(ContentStage.PREGNANCY,
                        UserStage.PREGNANCY, UserStage.PRE_PREGNANCY),
                Arguments.of(ContentStage.POSTPARTUM,
                        UserStage.POSTPARTUM, UserStage.PREGNANCY));
    }

    static Stream<Arguments> genericAllowedRoleStages() {
        return Stream.of(
                Arguments.of("FAMILY", UserStage.PRE_PREGNANCY),
                Arguments.of("EXPERT", UserStage.PREGNANCY),
                Arguments.of("MODERATOR", UserStage.POSTPARTUM),
                Arguments.of("CONTENT_ADMIN", UserStage.BABY_CARE),
                Arguments.of("SYSTEM_ADMIN", UserStage.PRE_PREGNANCY));
    }
}
