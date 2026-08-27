package com.carebridge.backend.integration.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.carebridge.backend.directchat.exception.DirectChatException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FirebaseAuthGatewayImpl implements IFirebaseAuthGateway {

    private final Optional<FirebaseApp> firebaseApp;

    public FirebaseAuthGatewayImpl(Optional<FirebaseApp> firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    public String createCustomToken(String uid) {
        FirebaseApp app = firebaseApp.orElseThrow(DirectChatException::firebaseUnavailable);
        try {
            return FirebaseAuth.getInstance(app).createCustomToken(uid);
        } catch (com.google.firebase.auth.FirebaseAuthException ex) {
            throw new RuntimeException("Failed to create Firebase custom token", ex);
        }
    }
}
