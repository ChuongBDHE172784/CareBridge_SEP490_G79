package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.support.Story69TestFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** RED contracts for UC82-69-RAG-001..005 and SEC-004. */
class RagStageBoundaryTest {

    @Test
    void uc82_69_rag_001_003_policyGateOwnsSafetyAndLifecycleBeforeEveryGenerator() {
        Class<?> policy = Story69TestFactory.loadClass(
                        "com.carebridge.backend.integration.gemini.service.RagPolicyServiceImpl")
                .orElse(null);

        assertThat(policy)
                .as("RAG-001/003: one always-active policy must gate safety and lifecycle")
                .isNotNull();

        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/integration/gemini/service/RagPolicyServiceImpl.java");
        source = source.substring(source.indexOf("generateAnswer"),
                source.indexOf("UserStage mapCanonicalStage"));
        int safety = source.indexOf("safetyFilter.check");
        int lifecycle = source.indexOf("lifecycleContentStageResolver");
        int delegate = source.indexOf("ragService.generateAnswer");

        assertThat(safety).isGreaterThanOrEqualTo(0);
        assertThat(lifecycle).isGreaterThan(safety);
        assertThat(delegate).isGreaterThan(lifecycle);
    }

    @Test
    void uc82_69_rag_002_retrieverHasDedicatedCanonicalStageSignature() {
        Class<?> retriever = Story69TestFactory.loadClass(
                        "com.carebridge.backend.integration.gemini.retriever.RagContextRetriever")
                .orElseThrow();

        Set<Integer> arities = Arrays.stream(retriever.getDeclaredMethods())
                .filter(method -> method.getName().equals("retrieveContext"))
                .map(Method::getParameterCount)
                .collect(Collectors.toSet());

        assertThat(arities)
                .as("RAG-002/004: generic 3-arg and Mother canonical-stage 4-arg retrieval coexist")
                .containsExactlyInAnyOrder(3, 4);

        Method canonical = Arrays.stream(retriever.getDeclaredMethods())
                .filter(method -> method.getName().equals("retrieveContext"))
                .filter(method -> method.getParameterCount() == 4)
                .findFirst()
                .orElseThrow();
        assertThat(canonical.getParameterTypes()[2]).isEqualTo(ContentStage.class);
    }

    @Test
    void uc82_69_rag_003_004_controllerDependsOnlyOnPolicyAndPreservesRoleMatrix() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/integration/gemini/controller/RagController.java");

        assertThat(source.contains("RagPolicyService")
                        && !source.contains("private final RagService ragService"))
                .as("RAG-003/004: controller entry cannot inject a downstream generator")
                .isTrue();
        assertThat(source.contains("'MOTHER'")
                        && source.contains("'FAMILY'")
                        && source.contains("'EXPERT'")
                        && source.contains("'MODERATOR'")
                        && source.contains("'CONTENT_ADMIN'")
                        && source.contains("'SYSTEM_ADMIN'")
                        && !source.contains("'PARTNER'"))
                .as("RAG-004: existing allowed roles remain and PARTNER remains denied")
                .isTrue();
    }

    @Test
    void uc82_69_rag_005_executionContextContainsOnlyMinimumNecessaryStageData() {
        Class<?> context = Story69TestFactory.loadClass(
                        "com.carebridge.backend.integration.gemini.dto.RagExecutionContext")
                .orElse(null);

        assertThat(context).as("RAG-005: minimum-necessary execution context is required").isNotNull();
        assertThat(Arrays.stream(context.getRecordComponents())
                        .map(component -> component.getName())
                        .toList())
                .containsExactly("mother", "canonicalStage", "promptStage")
                .doesNotContain("callerId", "journey", "healthNotes", "body");
    }
}
