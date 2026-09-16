package com.carebridge.backend.carejourney;

import com.carebridge.backend.carejourney.controller.BabyDailyLogController;
import com.carebridge.backend.carejourney.controller.BabyLogSummaryController;
import com.carebridge.backend.carejourney.controller.GrowthChartController;
import com.carebridge.backend.carejourney.controller.GrowthMeasurementController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BabyCareControllerAuthorizationContractTest {

    private static final String CAREGIVER_ROLES = "hasAnyRole('MOTHER', 'FAMILY')";
    // ShareBabyGrowthInDirectChat ADR-SBG-002: experts may read the growth chart only.
    private static final String GROWTH_CHART_READER_ROLES = "hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')";

    @Test
    void journalMutationsPermitMotherAndFamilyMemberAtControllerBoundary() {
        assertAuthorization(BabyDailyLogController.class, "addDailyLog", CAREGIVER_ROLES);
        assertAuthorization(BabyDailyLogController.class, "updateLog", CAREGIVER_ROLES);
        assertAuthorization(BabyDailyLogController.class, "deleteLog", CAREGIVER_ROLES);
    }

    @Test
    void journalReadsRequireAuthenticationOrCaregiverRole() {
        assertAuthorization(BabyDailyLogController.class, "getDailyLogDetail", "isAuthenticated()");
        assertAuthorization(BabyLogSummaryController.class, "getSummary", CAREGIVER_ROLES);
    }

    @Test
    void growthEndpointsPermitMotherAndFamilyMemberAtControllerBoundary() {
        Arrays.stream(GrowthMeasurementController.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .forEach(method -> assertThat(preAuthorize(method).value()).isEqualTo(CAREGIVER_ROLES));
    }

    @Test
    void growthChartReadAdditionallyPermitsExpertAtControllerBoundary() {
        assertAuthorization(GrowthChartController.class, "getGrowthChart", GROWTH_CHART_READER_ROLES);
    }

    private void assertAuthorization(Class<?> controller, String methodName, String expression) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        assertThat(preAuthorize(method).value()).isEqualTo(expression);
    }

    private PreAuthorize preAuthorize(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("%s must declare method authorization", method.getName())
                .isNotNull();
        return annotation;
    }
}
