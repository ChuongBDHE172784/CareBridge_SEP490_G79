package com.carebridge.backend.notification.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseConfigTest {

    private static final String PROJECT_ID = "project-d04b488f-17fb-4ae5-b64";
    private static final String SERVICE_ACCOUNT_ID =
            "firebase-adminsdk-fbsvc@project-d04b488f-17fb-4ae5-b64.iam.gserviceaccount.com";

    @Test
    void base64ModeRemainsDefaultAndDoesNotLoadAdc() throws Exception {
        TestFirebaseConfig config = config(false, "encoded-service-account", "", "");

        FirebaseOptions options = config.buildFirebaseOptions();

        assertThat(config.base64Loaded).isTrue();
        assertThat(config.adcLoaded).isFalse();
        assertThat(options.getProjectId()).isNull();
        assertThat(options.getServiceAccountId()).isNull();
    }

    @Test
    void adcModeUsesExplicitProjectAndRemoteSigningServiceAccount() throws Exception {
        TestFirebaseConfig config = config(true, "", PROJECT_ID, "  " + SERVICE_ACCOUNT_ID + "  ");

        FirebaseOptions options = config.buildFirebaseOptions();

        assertThat(config.adcLoaded).isTrue();
        assertThat(config.base64Loaded).isFalse();
        assertThat(options.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(options.getServiceAccountId()).isEqualTo(SERVICE_ACCOUNT_ID);
        assertThat(options.getHttpTransport()).isInstanceOf(ApacheHttpTransport.class);
    }

    @Test
    void adcModeAllowsHostedCredentialsThatCanSignWithoutOverride() throws Exception {
        TestFirebaseConfig config = config(true, "", PROJECT_ID, " ");

        FirebaseOptions options = config.buildFirebaseOptions();

        assertThat(options.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(options.getServiceAccountId()).isNull();
    }

    @Test
    void adcModeRejectsMixedBase64Credentials() {
        TestFirebaseConfig config = config(true, "old-project-secret", PROJECT_ID, SERVICE_ACCOUNT_ID);

        assertThatThrownBy(config::buildFirebaseOptions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIREBASE_CREDENTIALS_BASE64 must be empty");
        assertThat(config.adcLoaded).isFalse();
        assertThat(config.base64Loaded).isFalse();
    }

    @Test
    void adcModeRequiresExplicitProjectId() {
        TestFirebaseConfig config = config(true, "", " ", SERVICE_ACCOUNT_ID);

        assertThatThrownBy(config::buildFirebaseOptions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIREBASE_PROJECT_ID is required");
    }

    @Test
    void base64ModeRejectsMissingCredentialWithActionableMessage() {
        TestFirebaseConfig config = config(false, " ", "", "");

        assertThatThrownBy(config::buildFirebaseOptions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIREBASE_CREDENTIALS_BASE64 is required");
    }

    private TestFirebaseConfig config(
            boolean useAdc,
            String credentialsBase64,
            String projectId,
            String serviceAccountId
    ) {
        TestFirebaseConfig config = new TestFirebaseConfig();
        ReflectionTestUtils.setField(config, "useApplicationDefaultCredentials", useAdc);
        ReflectionTestUtils.setField(config, "credentialsBase64", credentialsBase64);
        ReflectionTestUtils.setField(config, "projectId", projectId);
        ReflectionTestUtils.setField(config, "serviceAccountId", serviceAccountId);
        return config;
    }

    private static final class TestFirebaseConfig extends FirebaseConfig {
        private final GoogleCredentials credentials = GoogleCredentials.create(
                new AccessToken("test-token", new Date(System.currentTimeMillis() + 60_000)));
        private boolean adcLoaded;
        private boolean base64Loaded;

        @Override
        GoogleCredentials loadApplicationDefaultCredentials() throws IOException {
            adcLoaded = true;
            return credentials;
        }

        @Override
        GoogleCredentials loadBase64Credentials() throws IOException {
            base64Loaded = true;
            return credentials;
        }
    }
}
