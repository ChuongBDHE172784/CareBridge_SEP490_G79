package com.carebridge.backend.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;
import com.carebridge.backend.security.service.impl.FederatedAuthServiceStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FederatedAuthServiceTest {

    private FederatedAuthService service;

    @BeforeEach
    void setUp() {
        service = new FederatedAuthServiceStub();
    }

    @Test
    void newGoogleIdentity_createsCareBridgeAccountAndSession() {
        FederatedAuthResponse response = service.authenticate(request("valid-google-token"));
        assertThat(response.newUser()).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void newPhoneIdentity_normalizesE164AndCreatesAccount() {
        FederatedAuthResponse response = service.authenticate(request("valid-phone-token"));
        assertThat(response.newUser()).isTrue();
        assertThat(response.user().getPhone()).startsWith("+84");
    }

    @Test
    void invalidExpiredOrRevokedToken_isRejectedWithoutSession() {
        FederatedAuthResponse response = service.authenticate(request("invalid-token"));
        assertThat(response).isNull();
    }

    @Test
    void existingUnlinkedContact_isNotAutomaticallyLinked() {
        FederatedAuthResponse response = service.authenticate(request("colliding-contact-token"));
        assertThat(response).isNull();
    }

    @Test
    void repeatedProviderSubject_doesNotCreateDuplicateUser() {
        FederatedAuthResponse first = service.authenticate(request("stable-subject-token"));
        FederatedAuthResponse second = service.authenticate(request("stable-subject-token"));
        assertThat(first.user().getId()).isEqualTo(second.user().getId());
    }

    @Test
    void providerTimeout_failsClosedWithoutMutation() {
        FederatedAuthResponse response = service.authenticate(request("provider-timeout"));
        assertThat(response).isNull();
    }

    @Test
    void lockedDisabledOrSuspendedAccount_cannotCreateSession() {
        FederatedAuthResponse response = service.authenticate(request("blocked-account-token"));
        assertThat(response).isNull();
    }

    @Test
    void rolelessAccount_isRoutedToProfileCompletion() {
        FederatedAuthResponse response = service.authenticate(request("roleless-user-token"));
        assertThat(response.profileCompleted()).isFalse();
        assertThat(response.accessToken()).isNotBlank();
    }

    private FederatedAuthRequest request(String token) {
        return new FederatedAuthRequest(token, "JUnit device");
    }
}
