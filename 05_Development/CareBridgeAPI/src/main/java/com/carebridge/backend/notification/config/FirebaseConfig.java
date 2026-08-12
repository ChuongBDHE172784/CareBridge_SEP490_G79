package com.carebridge.backend.notification.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;

/**
 * Initializes the Firebase Admin SDK from either a base64-encoded service account JSON or
 * explicitly enabled Application Default Credentials (ADC).
 * Active when EITHER FCM push (carebridge.fcm.enabled) OR direct-chat realtime signaling
 * (carebridge.firebase.firestore.enabled, UC-144 ADR-DCC-004) is enabled — both share the
 * same FirebaseApp and credentials. Base64 remains the default for backwards compatibility;
 * ADC must be explicitly enabled and cannot be combined with the base64 credential.
 */
@Configuration
@ConditionalOnExpression("${carebridge.fcm.enabled:false} or ${carebridge.firebase.firestore.enabled:false} or ${carebridge.firebase.auth.enabled:false} or ${carebridge.firebase.auth-emulator-enabled:false}")
public class FirebaseConfig {

    @Value("${carebridge.fcm.credentials-base64:}")
    private String credentialsBase64;

    @Value("${carebridge.fcm.use-application-default-credentials:false}")
    private boolean useApplicationDefaultCredentials;

    @Value("${carebridge.fcm.service-account-id:}")
    private String serviceAccountId;

    @Value("${carebridge.firebase.auth-emulator-enabled:false}")
    private boolean authEmulatorEnabled;

    @Value("${carebridge.firebase.auth-emulator-host:}")
    private String authEmulatorHost;

    @Value("${carebridge.firebase.project-id:}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        return FirebaseApp.initializeApp(buildFirebaseOptions());
    }

    FirebaseOptions buildFirebaseOptions() throws IOException {
        if (authEmulatorEnabled) {
            validateAuthEmulatorConfiguration();
            return FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.create(
                            new AccessToken("firebase-auth-emulator-owner", new Date(Long.MAX_VALUE))))
                    .setProjectId(projectId.trim())
                    .build();
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder();

        if (useApplicationDefaultCredentials) {
            if (!credentialsBase64.isBlank()) {
                throw new IllegalStateException(
                        "FIREBASE_CREDENTIALS_BASE64 must be empty when Firebase ADC mode is enabled");
            }
            if (projectId.isBlank()) {
                throw new IllegalStateException("FIREBASE_PROJECT_ID is required when Firebase ADC mode is enabled");
            }

            builder.setCredentials(loadApplicationDefaultCredentials())
                    .setProjectId(projectId.trim())
                    // Apache HttpClient consistently handles compressed IAM signBlob responses.
                    .setHttpTransport(new ApacheHttpTransport());
            if (!serviceAccountId.isBlank()) {
                // Enables IAM signBlob for Firebase custom tokens when ADC has no local private key.
                builder.setServiceAccountId(serviceAccountId.trim());
            }
            return builder.build();
        }

        if (credentialsBase64.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_CREDENTIALS_BASE64 is required unless Firebase ADC mode or Auth Emulator is enabled");
        }
        builder.setCredentials(loadBase64Credentials())
                .setHttpTransport(new ApacheHttpTransport());
        return builder.build();
    }

    GoogleCredentials loadApplicationDefaultCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault();
    }

    GoogleCredentials loadBase64Credentials() throws IOException {
        byte[] credentialsJson = Base64.getDecoder().decode(credentialsBase64.trim());
        try (ByteArrayInputStream serviceAccount = new ByteArrayInputStream(credentialsJson)) {
            return GoogleCredentials.fromStream(serviceAccount);
        }
    }

    private void validateAuthEmulatorConfiguration() {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_PROJECT_ID is required when Firebase Auth Emulator is enabled");
        }
        if (authEmulatorHost == null || authEmulatorHost.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_AUTH_EMULATOR_HOST is required when Firebase Auth Emulator is enabled");
        }
        if (authEmulatorHost.contains("://")) {
            throw new IllegalStateException(
                    "FIREBASE_AUTH_EMULATOR_HOST must not include a URL scheme");
        }
    }
}
