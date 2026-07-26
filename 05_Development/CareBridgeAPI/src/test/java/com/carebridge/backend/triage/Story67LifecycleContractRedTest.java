package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.carebridge.backend.triage.controller.IntakeController;
import com.carebridge.backend.triage.dto.request.ContinuationTokenRequest;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Story 6.7 ATDD contract tests.
 *
 * <p>These tests are intentionally active and RED until lifecycle binding,
 * restart-safe continuations, and the minimum-data safety projection exist.
 * Reflection keeps the pre-implementation suite compilable while still fixing
 * the public and persistence contracts before production code is authored.
 */
class Story67LifecycleContractRedTest {

    @Test
    void conversationStart_shouldExposeTypedLifecycleOriginWithoutAcceptingReturnUrls() {
        Set<String> properties = beanProperties(StartIntakeConversationRequest.class);

        assertThat(properties)
                .as("AC1: lifecycle-bound conversation start contract")
                .contains("journeyId", "originDashboard", "originReferenceId")
                .doesNotContain("returnUrl", "returnRoute", "deepLink");

        assertThat(Arrays.stream(StartIntakeConversationRequest.class.getDeclaredMethods())
                .anyMatch(method -> method.getAnnotation(JsonAnySetter.class) != null))
                .as("conversation start must actively reject arbitrary route/URL fields")
                .isTrue();
    }

    @Test
    void lifecycleBinding_shouldBePersistedWithOneOpaqueRestartSafeContinuation() {
        Set<String> fields = fieldNames(IntakeSession.class);

        assertThat(fields)
                .as("AC1/AC5: trusted binding and continuation state must survive process death")
                .contains(
                        "journeyId",
                        "originDashboard",
                        "originReferenceId",
                        "continuationToken",
                        "continuationExpiresAt",
                        "continuationAcknowledgedAt");
    }

    @Test
    void conversationResponses_shouldReturnTheStableServerContinuationDescriptor() {
        Set<String> properties = beanProperties(IntakeConversationResponse.class);

        assertThat(properties)
                .as("AC1/AC4: start and idempotent replay return server-issued continuation state")
                .contains("continuationToken", "continuationExpiresAt", "originDashboard", "originReferenceId", "originAction");
    }

    @Test
    void controller_shouldExposeOwnerBoundResolveAndAcknowledgePostEndpoints() {
        Set<String> postPaths = Arrays.stream(IntakeController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(postPaths)
                .as("AC4/AC5: tokens stay in redacted JSON bodies, never URL parameters")
                .contains("/continuations/resolve", "/continuations/acknowledge");
    }

    @Test
    void continuationToken_shouldRemainAStringUntilTheServiceMapsMalformedValuesToNeutral404()
            throws Exception {
        assertThat(ContinuationTokenRequest.class.getDeclaredField("token").getType())
                .as("malformed JSON token values must reach the neutral TRIAGE-014 service boundary")
                .isEqualTo(String.class);
        Class<?> serviceType = requiredClass(
                "com.carebridge.backend.triage.service.ITriageContinuationService");
        assertThat(serviceType.getMethod("resolve", java.util.UUID.class, String.class))
                .isNotNull();
        assertThat(serviceType.getMethod("acknowledge", java.util.UUID.class, String.class))
                .isNotNull();
        var tokenField = ContinuationTokenRequest.class.getDeclaredField("token");
        assertThat(tokenField.getAnnotation(jakarta.validation.constraints.NotBlank.class))
                .as("blank continuation bodies are rejected before service dispatch")
                .isNotNull();
        assertThat(tokenField.getAnnotation(jakarta.validation.constraints.Size.class).max())
                .as("continuation request bodies have a bounded token field")
                .isEqualTo(36);
    }

    @Test
    void legacyOneShotContract_shouldRemainOutsideLifecycleProjectionAndContinuation() {
        assertThat(beanProperties(RunIntakeRequest.class))
                .as("AC1 compatibility boundary for direct/one-shot intake")
                .doesNotContain(
                        "journeyId",
                        "originDashboard",
                        "originReferenceId",
                        "originAction",
                        "continuationToken",
                        "returnUrl");
    }

    @Test
    void projectionEntity_shouldContainOnlyMinimumNecessarySafetyData() {
        Class<?> outcomeType = requiredClass(
                "com.carebridge.backend.journey.entity.LifecycleSafetyOutcome");
        Set<String> fields = fieldNames(outcomeType);

        assertThat(fields)
                .as("AC2: allowlisted durable projection")
                .contains(
                        "id",
                        "ownerUserId",
                        "journeyId",
                        "intakeSessionId",
                        "emergencySessionId",
                        "riskLevel",
                        "stage",
                        "originDashboard",
                        "originReferenceId",
                        "originAction",
                        "occurredAt",
                        "recordedAt")
                .doesNotContain(
                        "symptoms",
                        "freeText",
                        "rawAiResponse",
                        "summary",
                        "recommendation",
                        "citations",
                        "notes",
                        "latitude",
                        "longitude",
                        "continuationToken");
    }

    @Test
    void projectionHandler_shouldConsumeCompletionSynchronouslyInsideTheTerminalTransaction() {
        Class<?> handlerType = requiredClass(
                "com.carebridge.backend.journey.service.IntakeSafetyOutcomeProjectionHandler");
        Method handler = Arrays.stream(handlerType.getDeclaredMethods())
                .filter(method -> Arrays.equals(
                        method.getParameterTypes(), new Class<?>[] {IntakeSessionCompleted.class}))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "AC2: projection handler must consume IntakeSessionCompleted"));

        assertThat(handler.getAnnotation(EventListener.class))
                .as("ordinary @EventListener participates in the publishing transaction")
                .isNotNull();
        assertThat(handler.getAnnotation(TransactionalEventListener.class))
                .as("the no-loss projection boundary must not be AFTER_COMMIT")
                .isNull();
    }

    @Test
    void lifecycleBindingService_shouldCentralizeConsentOwnershipStageAndBabyLinkValidation() {
        Class<?> bindingType = requiredClass(
                "com.carebridge.backend.triage.service.LifecycleIntakeBindingService");
        Set<String> dependencyTypes = Arrays.stream(bindingType.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertThat(dependencyTypes)
                .as("AC1/AC5: do not duplicate weaker ownership, consent, or baby-link policy")
                .contains(
                        "LifecycleConsentValidator",
                        "MotherJourneyRepository",
                        "BabyProfileRepository");
    }

    @Test
    void replayValidation_shouldRejectLegacyToBoundUpgradeWithoutNpe_andIncludeLockedStage()
            throws Exception {
        Class<?> bindingType = requiredClass(
                "com.carebridge.backend.triage.service.LifecycleBinding");
        assertThat(Arrays.stream(bindingType.getRecordComponents())
                .map(component -> component.getName()))
                .as("the locked stage is part of idempotency intent")
                .contains("stage");

        var service = new com.carebridge.backend.triage.service.LifecycleIntakeBindingService(
                null, null, null, Duration.ofDays(7));
        var legacy = IntakeSession.builder().stage(TriageStage.INFANT).build();
        Object[] arguments = Arrays.stream(bindingType.getRecordComponents())
                .map(component -> switch (component.getName()) {
                    case "journeyId", "originReferenceId", "continuationToken" ->
                            java.util.UUID.randomUUID();
                    case "originDashboard" -> OriginDashboard.BABY_PROFILE;
                    case "stage" -> TriageStage.INFANT;
                    case "continuationExpiresAt" -> Instant.now().plus(Duration.ofDays(7));
                    default -> throw new AssertionError("Unexpected binding component");
                })
                .toArray();
        Object requested = bindingType.getDeclaredConstructors()[0].newInstance(arguments);
        Method validateReplay = service.getClass().getMethod(
                "validateReplay", IntakeSession.class, bindingType);
        assertThatThrownBy(() -> validateReplay.invoke(service, legacy, requested))
                .hasRootCauseInstanceOf(com.carebridge.backend.triage.exception.TriageException.class)
                .hasRootCauseMessage("Intake context conflict");
    }

    private static Set<String> beanProperties(Class<?> type) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                    .map(descriptor -> descriptor.getName())
                    .filter(name -> !"class".equals(name))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception exception) {
            throw new AssertionError("Unable to inspect " + type.getName(), exception);
        }
    }

    private static Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Class<?> requiredClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Story 6.7 production contract is not implemented: " + className,
                    exception);
        }
    }
}
