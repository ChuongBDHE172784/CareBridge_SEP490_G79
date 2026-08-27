package com.carebridge.backend.integration.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// DCC-TC-019 — the uid claim always equals the caller-supplied UUID's string form; there is
// no overload or parameter through which a different target user id could be requested.
@ExtendWith(MockitoExtension.class)
class FirebaseAuthBridgeServiceImplTest {

    @Mock private IFirebaseAuthGateway gateway;
    @InjectMocks private FirebaseAuthBridgeServiceImpl service;

    @Test
    void createCustomToken_usesExactCallerUserIdAsUid() {
        UUID userA = UUID.randomUUID();
        when(gateway.createCustomToken(userA.toString())).thenReturn("token-for-a");

        String token = service.createCustomToken(userA);

        assertThat(token).isEqualTo("token-for-a");
        verify(gateway).createCustomToken(eq(userA.toString()));
    }

    @Test
    void createCustomToken_differentUsers_getDistinctUidClaims() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        when(gateway.createCustomToken(userA.toString())).thenReturn("token-a");
        when(gateway.createCustomToken(userB.toString())).thenReturn("token-b");

        assertThat(service.createCustomToken(userA)).isNotEqualTo(service.createCustomToken(userB));
        verify(gateway).createCustomToken(userA.toString());
        verify(gateway).createCustomToken(userB.toString());
    }

    // Contract check: the interface exposes exactly one method with exactly one UUID
    // parameter — there is no way to pass a different target user id.
    @Test
    void interfaceContract_singleMethodTakesOnlyUuid() throws NoSuchMethodException {
        var method = IFirebaseAuthBridgeService.class.getMethod("createCustomToken", UUID.class);
        assertThat(IFirebaseAuthBridgeService.class.getMethods()).hasSize(1);
        assertThat(method.getParameterCount()).isEqualTo(1);
    }
}
