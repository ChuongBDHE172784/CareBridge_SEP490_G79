package com.carebridge.backend.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Initializes the Firebase Admin SDK from a base64-encoded service account JSON.
 * Only active when carebridge.fcm.enabled=true — see .env.example for setup.
 * Using an env-var-carried value (instead of a file path) avoids working-directory-relative
 * path resolution issues between local dev and containerized deployments.
 */
@Configuration
@ConditionalOnProperty(name = "carebridge.fcm.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${carebridge.fcm.credentials-base64}")
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
