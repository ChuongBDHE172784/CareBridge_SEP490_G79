package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Structural guard that prevents legacy write capability from returning. */
class UserChecklistItemRepositoryIntegrationTest {
    @Test
    void compatibilityRepositoryExposesOnlyReadOperations() {
        Set<String> declaredMethods = Arrays.stream(UserChecklistItemRepository.class.getMethods())
                .map(java.lang.reflect.Method::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(declaredMethods).contains("findById", "findByIdAndOwnerUserId", "findByOwnerFiltered");
        assertThat(declaredMethods).noneMatch(name -> name.startsWith("save")
                || name.startsWith("delete") || name.startsWith("insert"));
    }
}
