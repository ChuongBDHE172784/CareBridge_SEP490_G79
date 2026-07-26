package com.carebridge.backend.content.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.support.Story69TestFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** RED contracts for UC82-69-TC-004/005/007/016/018/019/020. */
class LifecycleContentStageResolverTest {

    private static final String RESOLVER =
            "com.carebridge.backend.content.policy.LifecycleContentStageResolver";
    private static final String RESOLVED_CONTEXT =
            "com.carebridge.backend.content.policy.ResolvedLifecycleContext";

    @Test
    void uc82_69_tc_007_resolverContractExistsForReadAndLockedResolution() {
        Class<?> resolver = Story69TestFactory.loadClass(RESOLVER)
                .orElse(null);

        assertThat(resolver)
                .as("TC-007: a server-side lifecycle resolver must exist before personalized reads")
                .isNotNull();

        Method resolve = Story69TestFactory.method(resolver, "resolve", 1).orElse(null);
        Method resolveForUpdate = Story69TestFactory.method(resolver, "resolveForUpdate", 1)
                .orElse(null);

        assertThat(resolve).as("TC-007: resolve(UUID) is required").isNotNull();
        assertThat(resolve.getParameterTypes()).containsExactly(UUID.class);
        assertThat(resolve.getReturnType()).isEqualTo(ContentStage.class);
        assertThat(resolveForUpdate).as("TC-007: resolveForUpdate(UUID) is required").isNotNull();
        assertThat(resolveForUpdate.getParameterTypes()).containsExactly(UUID.class);
        assertThat(resolveForUpdate.getReturnType().getName()).isEqualTo(RESOLVED_CONTEXT);
    }

    @Test
    void uc82_69_tc_004_016_lifecycleServiceExposesAllThreeFailClosedOperations() {
        Class<?> service = Story69TestFactory.loadClass(
                        "com.carebridge.backend.content.service.ContentService")
                .orElseThrow();

        Set<String> lifecycleMethods = Arrays.stream(service.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("getLifecycle"))
                .collect(Collectors.toSet());

        assertThat(lifecycleMethods)
                .as("TC-004/016: list, checklist and detail must share the canonical lifecycle gate")
                .containsExactlyInAnyOrder(
                        "getLifecycleContents",
                        "getLifecycleChecklists",
                        "getLifecycleContentById");
    }

    @Test
    void uc82_69_tc_005_lifecycleControllerRoutesDeclareNoClientStageParameter() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/content/controller/ContentController.java");

        assertThat(source.contains("/lifecycle")
                        && source.contains("getLifecycleContents")
                        && source.contains("getLifecycleChecklists")
                        && source.contains("getLifecycleContentById"))
                .as("TC-005: lifecycle list/checklist/detail routes must be explicit")
                .isTrue();
        assertThat(source.contains(
                        "getLifecycleContents(\n            @RequestParam(required = false) ContentStage stage")
                        || source.contains(
                        "getLifecycleChecklists(\n            @RequestParam(required = false) ContentStage stage"))
                .as("TC-005: lifecycle routes cannot accept a client-authored stage")
                .isFalse();
    }
}
