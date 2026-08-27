package com.carebridge.backend.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class TestJwtKeyEnvironmentPostProcessorTest {

    private static final String ACTIVE_KEY_ID = "carebridge.security.jwt.active-key-id";
    private static final String PRIVATE_KEY = "carebridge.security.jwt.private-key";
    private static final String PUBLIC_KEYS = "carebridge.security.jwt.public-keys";

    @Test
    void postProcessEnvironment_WhenJwtKeysAreAbsent_GeneratesMatchingRsaKeyPair()
            throws Exception {
        StandardEnvironment environment = new StandardEnvironment();

        new TestJwtKeyEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        String keyId = environment.getRequiredProperty(ACTIVE_KEY_ID);
        PrivateKey privateKey = privateKey(environment.getRequiredProperty(PRIVATE_KEY));
        String publicKeyEntry = environment.getRequiredProperty(PUBLIC_KEYS);
        assertThat(publicKeyEntry).startsWith(keyId + ":");
        PublicKey publicKey = publicKey(publicKeyEntry.substring(keyId.length() + 1));

        byte[] payload = "carebridge-test-jwt-key-check".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payload);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(payload);

        assertThat(verifier.verify(signer.sign())).isTrue();
        assertThat(environment.getPropertySources()
                .contains(TestJwtKeyEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isTrue();
    }

    @Test
    void postProcessEnvironment_WhenAnyJwtKeyPropertyExists_DoesNotMaskPartialConfig() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "explicitJwtConfig", Map.of(ACTIVE_KEY_ID, "configured-key")));

        new TestJwtKeyEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources()
                .contains(TestJwtKeyEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isFalse();
        assertThat(environment.getProperty(PRIVATE_KEY)).isNull();
        assertThat(environment.getProperty(PUBLIC_KEYS)).isNull();
    }

    private static PrivateKey privateKey(String encoded) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(encoded)));
    }

    private static PublicKey publicKey(String encoded) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(encoded)));
    }
}
