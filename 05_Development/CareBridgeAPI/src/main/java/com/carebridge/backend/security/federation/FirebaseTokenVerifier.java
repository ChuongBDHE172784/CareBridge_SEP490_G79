package com.carebridge.backend.security.federation;

public interface FirebaseTokenVerifier {

    VerifiedFederatedIdentity verify(String idToken);
}
