package com.carebridge.backend.security.jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Supplies one in-memory RSA key pair per test JVM when no JWT key set is configured. */
public final class TestJwtKeyEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "carebridgeEphemeralTestJwtKeys";
    private static final String ACTIVE_KEY_ID_PROPERTY =
            "carebridge.security.jwt.active-key-id";
    private static final String PRIVATE_KEY_PROPERTY =
            "carebridge.security.jwt.private-key";
    private static final String PUBLIC_KEYS_PROPERTY =
            "carebridge.security.jwt.public-keys";
    private static final Map<String, Object> EPHEMERAL_KEY_PROPERTIES = generateKeyProperties();

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (hasText(environment.getProperty(ACTIVE_KEY_ID_PROPERTY))
                || hasText(environment.getProperty(PRIVATE_KEY_PROPERTY))
                || hasText(environment.getProperty(PUBLIC_KEYS_PROPERTY))) {
            return;
        }
        environment.getPropertySources().addLast(
                new MapPropertySource(PROPERTY_SOURCE_NAME, EPHEMERAL_KEY_PROPERTIES));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static Map<String, Object> generateKeyProperties() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            String keyId = "ephemeral-test-key";
            return Map.of(
                    ACTIVE_KEY_ID_PROPERTY, keyId,
                    PRIVATE_KEY_PROPERTY,
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                    PUBLIC_KEYS_PROPERTY,
                    keyId + ":" + Base64.getEncoder()
                            .encodeToString(keyPair.getPublic().getEncoded()));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate ephemeral test JWT keys", exception);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
