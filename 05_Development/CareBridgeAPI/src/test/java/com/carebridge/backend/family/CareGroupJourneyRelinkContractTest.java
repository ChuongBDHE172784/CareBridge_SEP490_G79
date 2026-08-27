package com.carebridge.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.family.controller.CareGroupController;
import com.carebridge.backend.family.service.ICareGroupService;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;

/** RED contract for the owner-only care-group journey relink workflow. */
class CareGroupJourneyRelinkContractTest {

    private static final String REQUEST =
            "com.carebridge.backend.family.dto.RelinkCareGroupJourneyRequest";
    private static final String RESPONSE =
            "com.carebridge.backend.family.dto.RelinkCareGroupJourneyResponse";

    @Test
    void requestAndResponseDtosExposeOnlyCanonicalRelinkFields() throws Exception {
        Class<?> request = Class.forName(REQUEST);
        assertThat(request.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .containsExactly("journeyId");
        assertThat(request.getDeclaredField("journeyId").getType()).isEqualTo(UUID.class);
        assertThat(request.getDeclaredField("journeyId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(request.getMethod("getJourneyId").getReturnType()).isEqualTo(UUID.class);

        Class<?> response = Class.forName(RESPONSE);
        assertGetter(response, "getGroupId", UUID.class);
        assertGetter(response, "getPreviousJourneyId", UUID.class);
        assertGetter(response, "getJourneyId", UUID.class);
        assertGetter(response, "getRelinkedAt", Instant.class);
        assertGetter(response, "getCorrelationId", UUID.class);
        assertThat(response.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder(
                        "groupId", "previousJourneyId", "journeyId", "relinkedAt", "correlationId");
    }

    @Test
    void controllerPublishesOwnerOnlyPatchAndTypedResponse() throws Exception {
        Class<?> request = Class.forName(REQUEST);
        Method endpoint = CareGroupController.class.getMethod(
                "relinkJourney", UUID.class, request, Principal.class);
        PatchMapping patch = endpoint.getAnnotation(PatchMapping.class);
        assertThat(patch).isNotNull();
        assertThat(patch.value()).containsExactly("/{groupId}/journey");
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('MOTHER')");
        assertThat(endpoint.getReturnType()).isEqualTo(ResponseEntity.class);
        assertThat(endpoint.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
        assertThat(endpoint.getGenericReturnType().getTypeName()).contains(RESPONSE);
    }

    @Test
    void serviceBoundaryRequiresGroupJourneyAndAuthenticatedOwnerIds() throws Exception {
        Class<?> response = Class.forName(RESPONSE);
        Method service = ICareGroupService.class.getMethod(
                "relinkJourney", UUID.class, UUID.class, UUID.class);
        assertThat(service.getReturnType()).isEqualTo(response);
    }

    @Test
    void relinkHasDedicatedTypedAuditAction() {
        assertThat(AuditAction.valueOf("CARE_GROUP_CONTEXT_RELINKED").name())
                .isEqualTo("CARE_GROUP_CONTEXT_RELINKED");
    }

    private static void assertGetter(Class<?> type, String name, Class<?> returnType) throws Exception {
        assertThat(type.getMethod(name).getReturnType()).isEqualTo(returnType);
    }
}
