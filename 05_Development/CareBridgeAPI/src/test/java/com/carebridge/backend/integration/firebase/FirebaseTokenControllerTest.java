package com.carebridge.backend.integration.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.ApiResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class FirebaseTokenControllerTest {

    @Mock private IFirebaseAuthBridgeService firebaseAuthBridgeService;
    @Mock private AuditService auditService;

    // DCC-TC-019 step 1 — no @RequestBody, no @RequestParam, no way to name a different user.
    @Test
    void issueCustomToken_hasNoInputBesidesPrincipal() throws NoSuchMethodException {
        Method method = FirebaseTokenController.class.getMethod("issueCustomToken", Principal.class);
        assertThat(method.getParameterCount()).isEqualTo(1);
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                assertThat(annotation.annotationType().getSimpleName())
                        .as("only Principal is allowed as input")
                        .isNotIn("RequestBody", "RequestParam", "PathVariable");
            }
        }
    }

    @Test
    void issueCustomToken_whenFirestoreDisabled_returnsCapabilityWithoutIssuingToken() {
        UUID userId = UUID.randomUUID();
        Principal principal = () -> userId.toString();
        FirebaseTokenController controller = controller(false);

        ResponseEntity<ApiResponse<FirebaseTokenController.FirebaseCustomTokenResponse>> response =
                controller.issueCustomToken(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().firestoreSignalingEnabled()).isFalse();
        assertThat(response.getBody().getData().firebaseCustomToken()).isNull();
        verifyNoInteractions(firebaseAuthBridgeService, auditService);
    }

    @Test
    void issueCustomToken_whenFirestoreEnabled_usesPrincipalNameAndAuditsIssuedToken() {
        UUID userId = UUID.randomUUID();
        Principal principal = () -> userId.toString();
        FirebaseTokenController controller = controller(true);
        when(firebaseAuthBridgeService.createCustomToken(userId)).thenReturn("tok-abc");

        ResponseEntity<ApiResponse<FirebaseTokenController.FirebaseCustomTokenResponse>> response =
                controller.issueCustomToken(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().firestoreSignalingEnabled()).isTrue();
        assertThat(response.getBody().getData().firebaseCustomToken()).isEqualTo("tok-abc");
        verify(firebaseAuthBridgeService).createCustomToken(userId);
        verify(auditService).log(
                AuditAction.FIREBASE_CUSTOM_TOKEN_ISSUED,
                userId,
                "FIREBASE_CUSTOM_TOKEN",
                userId.toString(),
                Map.of());
    }

    private FirebaseTokenController controller(boolean firestoreSignalingEnabled) {
        return new FirebaseTokenController(
                firebaseAuthBridgeService,
                auditService,
                firestoreSignalingEnabled);
    }
}
