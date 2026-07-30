package com.carebridge.backend.directchat.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class ConversationCallControllerContractTest {

    private static final String PARTICIPANT_ROLES = "hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')";

    @Test
    void joinCredentials_acceptsOnlyPathIdsAndAuthenticatedPrincipal() throws Exception {
        Method method = ConversationCallController.class.getDeclaredMethod(
                "issueJoinCredentials", UUID.class, UUID.class, Principal.class);

        assertThat(method.getAnnotation(PostMapping.class)).isNotNull();
        assertThat(Arrays.stream(method.getParameters())
                        .map(parameter -> parameter.getType().getName()))
                .containsExactly(UUID.class.getName(), UUID.class.getName(), Principal.class.getName());
    }

    @Test
    void callDetail_isExposedAsParticipantGet() throws Exception {
        Method method = ConversationCallController.class.getDeclaredMethod(
                "getCall", UUID.class, UUID.class, Principal.class);

        assertThat(method.getAnnotation(GetMapping.class)).isNotNull();
    }

    @Test
    void activeCalls_hasDedicatedAuthenticatedGetEndpoint() throws Exception {
        Class<?> controller =
                Class.forName("com.carebridge.backend.directchat.controller.ActiveConversationCallController");
        Method method = controller.getDeclaredMethod("listActiveCalls", Principal.class);

        assertThat(method.getAnnotation(GetMapping.class)).isNotNull();
    }

    @Test
    void participantEndpoints_allowFamilyRole() {
        assertParticipantRoles(DirectConversationController.class);
        assertParticipantRoles(DirectMessageController.class);
        assertParticipantRoles(ConversationCallController.class);
        assertParticipantRoles(ActiveConversationCallController.class);
    }

    private void assertParticipantRoles(Class<?> controller) {
        Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .forEach(method -> assertThat(method.getAnnotation(PreAuthorize.class).value())
                        .as("%s.%s role gate", controller.getSimpleName(), method.getName())
                        .isEqualTo(PARTICIPANT_ROLES));
    }
}
