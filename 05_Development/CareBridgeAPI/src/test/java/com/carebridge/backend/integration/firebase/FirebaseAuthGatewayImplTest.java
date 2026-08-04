package com.carebridge.backend.integration.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.directchat.exception.DirectChatException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FirebaseAuthGatewayImplTest {

    @Test
    void createCustomToken_withoutFirebaseApp_keepsDcc012Fallback() {
        FirebaseAuthGatewayImpl gateway = new FirebaseAuthGatewayImpl(Optional.empty());

        assertThatThrownBy(() -> gateway.createCustomToken("carebridge-user-id"))
                .isInstanceOfSatisfying(DirectChatException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("DCC-012");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }
}
