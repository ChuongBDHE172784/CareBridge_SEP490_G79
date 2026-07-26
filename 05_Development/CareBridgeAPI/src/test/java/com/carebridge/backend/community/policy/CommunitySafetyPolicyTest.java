package com.carebridge.backend.community.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.repository.ContentReportRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * CB-MOD-IMP-017 scope 1 — regression pins for the removed domain leak: a user DESCRIBING
 * medical symptoms ("chảy máu nhiều", "khó thở", ...) is a medical-safety signal, never a
 * content violation. CommunitySafetyPolicy therefore must have no moderation-report-creation
 * responsibility at all; violations are detected semantically by the AI moderation scan.
 */
class CommunitySafetyPolicyTest {

    // Scope-1 test: the keyword auto-report hook is gone — a benign symptom description can no
    // longer produce an AUTOMATED ContentReport through this class (compile-level guarantee,
    // pinned here so it cannot be silently re-introduced).
    @Test
    void communitySafetyPolicy_hasNoAutoReportHook() {
        assertThat(Arrays.stream(CommunitySafetyPolicy.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.toLowerCase().contains("report")))
                .isEmpty();
    }

    // The policy must not even hold a handle to moderation-case persistence.
    @Test
    void communitySafetyPolicy_hasNoModerationPersistenceDependency() {
        assertThat(Arrays.stream(CommunitySafetyPolicy.class.getDeclaredFields())
                .map(field -> field.getType().getName()))
                .doesNotContain(ContentReportRepository.class.getName());
    }

    // Medical red-flag evaluation stays in the triage domain (TriageRedFlagPolicy, untouched
    // safety floor) — the community policy no longer references it.
    @Test
    void communitySafetyPolicy_doesNotDependOnTriageRedFlagPolicy() {
        assertThat(Arrays.stream(CommunitySafetyPolicy.class.getDeclaredFields())
                .map(field -> field.getType().getSimpleName()))
                .doesNotContain("TriageRedFlagPolicy");
    }
}
