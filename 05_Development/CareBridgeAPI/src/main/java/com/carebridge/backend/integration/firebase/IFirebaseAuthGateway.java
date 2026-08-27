package com.carebridge.backend.integration.firebase;

/** Thin boundary around {@code FirebaseAuth.getInstance(app).createCustomToken(uid)} — same rationale as {@link IFirebaseRealtimeGateway}. */
public interface IFirebaseAuthGateway {

    String createCustomToken(String uid);
}
