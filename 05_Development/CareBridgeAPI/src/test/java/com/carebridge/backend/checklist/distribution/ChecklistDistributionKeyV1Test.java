package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** RED contract for the versioned non-null distribution-key algorithm. */
class ChecklistDistributionKeyV1Test {

    @Test
    void instanceKeyMatchesApprovedV1GoldenVectorAndExcludesTargetSubject() throws Exception {
        Class<?> factory = Class.forName("com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory");
        Method method = factory.getMethod(
                "instanceKey", UUID.class, UUID.class, String.class, UUID.class, String.class, UUID.class,
                String.class, String.class);

        String key = (String) method.invoke(null,
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                "NONE",
                "NONE");

        assertThat(key).isEqualTo("fad7bba6cefeb717acaf887b59410cef7184b88706e67cdf828be0240678369d");
    }

    @Test
    void childKeyMatchesApprovedV1GoldenVector() throws Exception {
        Class<?> factory = Class.forName("com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory");
        Method method = factory.getMethod("childKey", UUID.class, UUID.class);

        String key = (String) method.invoke(null,
                ChecklistDistributionTestFactory.INSTANCE_ID,
                ChecklistDistributionTestFactory.ITEM_VERSION_ID);

        assertThat(key).isEqualTo("1d27af4be812a3b80681e37fb69771c6b5471e0b73d49920f4d05635eb0a9ac6");
    }

    @Test
    void userCreatedInstanceAndChildKeysUseExplicitAbsentTokens() throws Exception {
        Class<?> factory = Class.forName("com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory");
        Method instanceMethod = factory.getMethod(
                "userCreatedInstanceKey", UUID.class, String.class, UUID.class, String.class, UUID.class,
                LocalDate.class, LocalDate.class);
        Method childMethod = factory.getMethod("userCreatedChildKey", UUID.class, UUID.class);

        String instanceKey = (String) instanceMethod.invoke(null,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                null,
                null);
        String childKey = (String) childMethod.invoke(null,
                ChecklistDistributionTestFactory.INSTANCE_ID,
                ChecklistDistributionTestFactory.ITEM_VERSION_ID);

        assertThat(instanceKey).matches("[0-9a-f]{64}");
        assertThat(childKey).matches("[0-9a-f]{64}");
    }

    @Test
    void lifecycleScopeKeyAllowsUserCreatedInstancesWithoutTemplateVersion() {
        assertThat(com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory.lifecycleScopeKey(
                null,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID))
                .matches("[0-9a-f]{64}");
    }

    @Test
    void personalKeysUseDeterministicAbsentCareGroupToken() {
        String first = com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory.instanceKey(
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                null,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                "NONE",
                "NONE");
        String replay = com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory.instanceKey(
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                null,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                "NONE",
                "NONE");

        assertThat(first).matches("[0-9a-f]{64}").isEqualTo(replay);
    }

    @Test
    void nullWindowAndLiteralNoneDoNotCollide() throws Exception {
        Class<?> factory = Class.forName("com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory");
        Method method = factory.getMethod(
                "instanceKey", UUID.class, UUID.class, String.class, UUID.class, String.class, UUID.class,
                String.class, String.class);

        String absent = (String) method.invoke(null,
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                null,
                null);
        String explicitNone = (String) method.invoke(null,
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                "NONE",
                "NONE");

        assertThat(absent).isNotEqualTo(explicitNone);
    }

    @Test
    void dateTokensMustBeCanonicalIsoLocalDates() {
        assertThatThrownBy(() -> com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory.instanceKey(
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                "29/07/2026",
                "30/07/2026"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void windowBoundsMustBePairedAndOrdered() {
        assertThatThrownBy(() -> com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory.instanceKey(
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                null,
                "2026-07-30"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory.instanceKey(
                ChecklistDistributionTestFactory.TEMPLATE_VERSION_ID,
                ChecklistDistributionTestFactory.RECIPIENT_ID,
                "MOTHER",
                ChecklistDistributionTestFactory.CARE_GROUP_ID,
                "JOURNEY",
                ChecklistDistributionTestFactory.CONTEXT_ID,
                "2026-07-31",
                "2026-07-30"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
