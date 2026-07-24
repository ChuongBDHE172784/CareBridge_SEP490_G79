package com.carebridge.backend.content.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.support.Story69TestFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** RED service contracts for TC-004/006/016/018/020. */
class LifecycleContentServiceTest {

    @Test
    void uc82_69_tc_004_018_020_lifecycleOperationsReturnTypedStageEnvelopes() {
        Class<?> service = Story69TestFactory.loadClass(
                        "com.carebridge.backend.content.service.ContentService")
                .orElseThrow();

        for (String name : new String[]{
                "getLifecycleContents", "getLifecycleChecklists", "getLifecycleContentById"}) {
            Method method = Arrays.stream(service.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(name))
                    .findFirst()
                    .orElse(null);
            assertThat(method).as("Story 6.9 service operation %s must exist", name).isNotNull();
            assertThat(method.getReturnType().getName())
                    .as("%s must expose the canonical stage envelope", name)
                    .isEqualTo("com.carebridge.backend.content.dto.response.LifecycleContentEnvelope");
        }
    }

    @Test
    void uc82_69_tc_006_016_contextResolutionPrecedesAnyLifecycleDataLookup() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/content/service/ContentServiceImpl.java");
        source = source.substring(source.indexOf("getLifecycleContents"));
        int resolver = source.indexOf("lifecycleContentStageResolver.resolve");
        int lifecycleQuery = source.indexOf("contentRepository.findByFilters");

        assertThat(resolver)
                .as("TC-016: absent lifecycle must fail before content/checklist repositories")
                .isGreaterThanOrEqualTo(0);
        assertThat(lifecycleQuery).isGreaterThan(resolver);
        assertThat(source.contains("CNT-013") || source.contains("lifecycleContentStageResolver"))
                .isTrue();
    }
}
