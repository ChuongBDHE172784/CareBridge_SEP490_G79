package com.carebridge.backend.integration.firebase;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// BR-DCC-013: intentionally takes NO request body/params — the target user id can only
// ever be the caller's own JWT-derived identity, never client-supplied.
@RestController
@RequiredArgsConstructor
public class FirebaseTokenController {

    private final IFirebaseAuthBridgeService firebaseAuthBridgeService;
    private final AuditService auditService;

    @PostMapping("/api/v1/firebase/custom-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FirebaseCustomTokenResponse>> issueCustomToken(Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        String token = firebaseAuthBridgeService.createCustomToken(currentUserId);
        auditService.log(AuditAction.FIREBASE_CUSTOM_TOKEN_ISSUED, currentUserId, "FIREBASE_CUSTOM_TOKEN", currentUserId.toString(), Map.of());
        return ResponseEntity.ok(ApiResponse.success(new FirebaseCustomTokenResponse(token)));
    }

    public record FirebaseCustomTokenResponse(String firebaseCustomToken) {
    }
}
