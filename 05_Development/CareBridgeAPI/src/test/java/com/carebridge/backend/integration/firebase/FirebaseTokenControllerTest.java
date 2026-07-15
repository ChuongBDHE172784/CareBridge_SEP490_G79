package com.carebridge.backend.integration.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class FirebaseTokenControllerTest {

    @Mock private IFirebaseAuthBridgeService firebaseAuthBridgeService;
    @Mock private AuditService auditService;
    @InjectMocks private FirebaseTokenController controller;

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
    void issueCustomToken_usesPrincipalNameAsUserId() {
        UUID userId = UUID.randomUUID();
        Principal principal = () -> userId.toString();
        when(firebaseAuthBridgeService.createCustomToken(userId)).thenReturn("tok-abc");

        ResponseEntity<?> response = controller.issueCustomToken(principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(firebaseAuthBridgeService).createCustomToken(eq(userId));
        verify(auditService).log(eq(AuditAction.FIREBASE_CUSTOM_TOKEN_ISSUED), eq(userId), any(), any(), any());
    }
}
