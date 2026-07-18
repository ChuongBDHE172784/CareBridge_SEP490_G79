package com.carebridge.backend.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Initializes the Firebase Admin SDK from either:
 * 1. A service account JSON file (firebase-adminsdk.json) in the project root or classpath — preferred
 * 2. A base64-encoded service account JSON via FIREBASE_CREDENTIALS_BASE64 env var — fallback
 * 3. Application Default Credentials (ADC) — opt-in only
 *
 * Active when EITHER FCM push OR direct-chat realtime signaling is enabled.
 */
@Configuration
@ConditionalOnExpression("${carebridge.fcm.enabled:false} or ${carebridge.firebase.firestore.enabled:false}")
public class FirebaseConfig {

    @Value("${carebridge.fcm.credentials-base64:}")
    private String credentialsBase64;

    @Value("${carebridge.fcm.use-application-default-credentials:false}")
    private boolean useApplicationDefaultCredentials;

    @Value("${carebridge.fcm.project-id:}")
    private String projectId;

    @Value("${carebridge.fcm.service-account-id:}")
    private String serviceAccountId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        return FirebaseApp.initializeApp(buildFirebaseOptions());
    }

    FirebaseOptions buildFirebaseOptions() throws IOException {
        FirebaseOptions.Builder builder = FirebaseOptions.builder();

        // Priority 1: Load from firebase-adminsdk.json file (project root or classpath)
        GoogleCredentials fileCreds = loadFromJsonFile();
        if (fileCreds != null) {
            builder.setCredentials(fileCreds)
                   .setHttpTransport(new ApacheHttpTransport());
            System.out.println("[FIREBASE] Loaded credentials from firebase-adminsdk.json file");
            return builder.build();
        }

        // Priority 2: ADC mode
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
                   .setHttpTransport(new ApacheHttpTransport());
            if (!serviceAccountId.isBlank()) {
                builder.setServiceAccountId(serviceAccountId.trim());
            }
            System.out.println("[FIREBASE] Using Application Default Credentials");
            return builder.build();
        }

        // Priority 3: base64 env var
        if (!credentialsBase64.isBlank()) {
            builder.setCredentials(loadBase64Credentials())
                   .setHttpTransport(new ApacheHttpTransport());
            System.out.println("[FIREBASE] Loaded credentials from FIREBASE_CREDENTIALS_BASE64 env var");
            return builder.build();
        }

        throw new IllegalStateException(
            "Firebase credentials not found. Provide firebase-adminsdk.json, FIREBASE_CREDENTIALS_BASE64, or enable ADC mode.");
    }

    /**
     * Try to load from firebase-adminsdk.json in project root first, then classpath.
     */
    private GoogleCredentials loadFromJsonFile() throws IOException {
        // Try project root (e.g. D:\Do_aN\05_Development\CareBridgeAPI\firebase-adminsdk.json)
        File rootFile = new File("firebase-adminsdk.json");
        if (rootFile.exists() && rootFile.isFile() && rootFile.length() > 0) {
            try (FileInputStream fis = new FileInputStream(rootFile)) {
                System.out.println("[FIREBASE] Found firebase-adminsdk.json at: " + rootFile.getAbsolutePath());
                return GoogleCredentials.fromStream(fis);
            }
        }

        // Try classpath
        Resource classpathResource = new ClassPathResource("firebase-adminsdk.json");
        if (classpathResource.exists()) {
            try (InputStream is = classpathResource.getInputStream()) {
                System.out.println("[FIREBASE] Found firebase-adminsdk.json on classpath");
                return GoogleCredentials.fromStream(is);
            }
        }

        return null;
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
}
