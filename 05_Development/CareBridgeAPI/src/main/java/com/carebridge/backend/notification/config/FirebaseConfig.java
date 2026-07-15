package com.carebridge.backend.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Initializes the Firebase Admin SDK from a base64-encoded service account JSON.
 * Active when EITHER FCM push (carebridge.fcm.enabled) OR direct-chat realtime signaling
 * (carebridge.firebase.firestore.enabled, UC-144 ADR-DCC-004) is enabled — both share the
 * same FirebaseApp and service-account credentials.
 * Using an env-var-carried value (instead of a file path) avoids working-directory-relative
 * path resolution issues between local dev and containerized deployments.
 */
@Configuration
@ConditionalOnExpression("${carebridge.fcm.enabled:false} or ${carebridge.firebase.firestore.enabled:false}")
public class FirebaseConfig {

    @Value("${carebridge.fcm.credentials-base64:}")
    private String credentialsBase64;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        byte[] credentialsJson = Base64.getDecoder().decode(credentialsBase64.trim());
        try (ByteArrayInputStream serviceAccount = new ByteArrayInputStream(credentialsJson)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
