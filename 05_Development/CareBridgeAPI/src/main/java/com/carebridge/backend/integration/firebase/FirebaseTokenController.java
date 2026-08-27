package com.carebridge.backend.integration.firebase;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// BR-DCC-013: intentionally takes NO request body/params — the target user id can only
// ever be the caller's own JWT-derived identity, never client-supplied.
@RestController
public class FirebaseTokenController {

    private final IFirebaseAuthBridgeService firebaseAuthBridgeService;
    private final AuditService auditService;
    private final boolean firestoreSignalingEnabled;

    public FirebaseTokenController(
            IFirebaseAuthBridgeService firebaseAuthBridgeService,
            AuditService auditService,
            @Value("${carebridge.firebase.firestore.enabled:false}") boolean firestoreSignalingEnabled) {
        this.firebaseAuthBridgeService = firebaseAuthBridgeService;
        this.auditService = auditService;
        this.firestoreSignalingEnabled = firestoreSignalingEnabled;
    }

    @PostMapping("/api/v1/firebase/custom-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FirebaseCustomTokenResponse>> issueCustomToken(Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        if (!firestoreSignalingEnabled) {
            return ResponseEntity.ok(ApiResponse.success(new FirebaseCustomTokenResponse(false, null)));
        }

        String token = firebaseAuthBridgeService.createCustomToken(currentUserId);
        auditService.log(
                AuditAction.FIREBASE_CUSTOM_TOKEN_ISSUED,
                currentUserId,
                "FIREBASE_CUSTOM_TOKEN",
                currentUserId.toString(),
                Map.of());
        return ResponseEntity.ok(ApiResponse.success(new FirebaseCustomTokenResponse(true, token)));
    }

    public record FirebaseCustomTokenResponse(
            boolean firestoreSignalingEnabled,
            String firebaseCustomToken) {
    }
}
