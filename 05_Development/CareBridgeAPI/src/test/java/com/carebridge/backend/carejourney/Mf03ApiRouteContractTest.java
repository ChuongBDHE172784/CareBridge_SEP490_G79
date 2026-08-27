package com.carebridge.backend.carejourney;

import com.carebridge.backend.carejourney.controller.MilestoneController;
import com.carebridge.backend.vaccination.controller.VaccinationController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class Mf03ApiRouteContractTest {

    @Test
    void milestoneListRoute_isCanonical() {
        assertThat(MilestoneController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/v1/babies/{babyId}/milestones");
        assertThat(Arrays.stream(MilestoneController.class.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(GetMapping.class))).isTrue();
    }

    @Test
    void vaccinationRecordListRoute_isCanonical() {
        assertThat(Arrays.stream(VaccinationController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .flatMap(method -> Arrays.stream(method.getAnnotation(GetMapping.class).value()))
                .toList()).contains("/babies/{babyId}/records");
    }
}
