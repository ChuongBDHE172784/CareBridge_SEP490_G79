package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.support.Story69TestFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/** RED bean/signature contracts for RAG-003/004. */
class RagImplementationContractTest {

    @Test
    void uc82_69_rag_003_004_downstreamGeneratorHasOnlyTwoArgumentExecutionContract() {
        Class<?> service = Story69TestFactory.loadClass(
                        "com.carebridge.backend.integration.gemini.service.RagService")
                .orElseThrow();
        Method generate = Arrays.stream(service.getDeclaredMethods())
                .filter(method -> method.getName().equals("generateAnswer"))
                .findFirst()
                .orElseThrow();
        assertThat(generate.getParameterCount()).isEqualTo(2);
        assertThat(generate.getParameterTypes()[1].getName())
                .isEqualTo("com.carebridge.backend.integration.gemini.dto.RagExecutionContext");
    }

    @Test
    void uc82_69_rag_003_004_profileSelectionCannotBypassPolicy() {
        for (String implementation : new String[]{"GeminiRagServiceImpl", "FallbackRagServiceImpl"}) {
            Class<?> type = Story69TestFactory.loadClass(
                            "com.carebridge.backend.integration.gemini.service." + implementation)
                    .orElseThrow();
            Profile profile = type.getAnnotation(Profile.class);
            assertThat(profile).as("%s must be excluded from the test profile", implementation).isNotNull();
            assertThat(profile.value()).containsExactly("!test");
        }
    }
}
