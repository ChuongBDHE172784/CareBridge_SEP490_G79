package com.carebridge.backend.security.jwt;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderSecretValidationTest {

    @Test
    void init_WhenActiveKeyIdMissing_ThrowsIllegalStateException() {
        JwtTokenProvider provider = new JwtTokenProvider();

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("JWT_ACTIVE_KEY_ID is not configured");
    }

    @Test
    void init_WhenPrivateKeyMissing_ThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "configuredActiveKeyId", "key-1");
        ReflectionTestUtils.setField(provider, "configuredPublicKeys",
                publicKeyConfig(Map.of("key-1", keyPair.getPublic())));

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("JWT_PRIVATE_KEY is not configured");
    }

    @Test
    void init_WhenPublicKeysMissing_ThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "configuredActiveKeyId", "key-1");
        ReflectionTestUtils.setField(provider, "configuredPrivateKey",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("JWT_PUBLIC_KEYS is not configured");
    }

    @Test
    void init_WhenActiveKidIsAbsent_ThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenProvider provider = configuredProvider(
                "key-1", keyPair, Map.of("other-key", keyPair.getPublic()), false);

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .message().contains("does not contain active kid key-1");
    }

    @Test
    void init_WhenRsaConfigurationIsValid_DoesNotThrow() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenProvider provider = configuredProvider(
                "key-1", keyPair, Map.of("key-1", keyPair.getPublic()), false);

        provider.init();

        assertThat(provider).isNotNull();
    }

    @Test
    @DisplayName("LOGIN-TC-007: RS256 access token has kid and exactly 900-second TTL")
    void generateAccessToken_hasRs256KidAndExactly900SecondTtl() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtTokenProvider provider = configuredProvider(
                "key-2026-07", keyPair, Map.of("key-2026-07", keyPair.getPublic()), true);
        provider.init();
        User user = User.builder()
                .id(UUID.randomUUID())
                .phone("+84912345678")
                .role(Role.MOTHER)
                .build();

        String token = provider.generateAccessToken(user);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        String headerJson = decode(parts[0]);
        String payloadJson = decode(parts[1]);
        assertThat(headerJson).contains("\"alg\":\"RS256\"");
        assertThat(headerJson).contains("\"kid\":\"key-2026-07\"");
        assertThat(extractNumericClaim(payloadJson, "exp")
                - extractNumericClaim(payloadJson, "iat")).isEqualTo(900L);
        assertThat(payloadJson).contains("\"sub\":\"" + user.getId());
        assertThat(payloadJson).contains("\"role\":\"" + Role.MOTHER.getAuthority() + "\"");
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_acceptsPreviousKidDuringRotationAndRejectsAlgorithmConfusion()
            throws Exception {
        KeyPair previous = rsaKeyPair();
        KeyPair active = rsaKeyPair();
        Map<String, PublicKey> rotationSet = new LinkedHashMap<>();
        rotationSet.put("previous", previous.getPublic());
        rotationSet.put("active", active.getPublic());
        JwtTokenProvider previousIssuer = configuredProvider(
                "previous", previous, rotationSet, true);
        JwtTokenProvider activeIssuer = configuredProvider(
                "active", active, rotationSet, true);
        previousIssuer.init();
        activeIssuer.init();
        User user = User.builder().id(UUID.randomUUID()).role(Role.MOTHER).build();

        String previousToken = previousIssuer.generateAccessToken(user);
        String activeToken = activeIssuer.generateAccessToken(user);
        String forgedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\",\"kid\":\"active\"}"
                        .getBytes(StandardCharsets.UTF_8));
        String algorithmConfusionToken = forgedHeader + "."
                + activeToken.split("\\.")[1] + "." + activeToken.split("\\.")[2];

        assertThat(activeIssuer.validateToken(previousToken)).isTrue();
        assertThat(activeIssuer.validateToken(activeToken)).isTrue();
        assertThat(activeIssuer.validateToken(algorithmConfusionToken)).isFalse();
    }

    private static JwtTokenProvider configuredProvider(
            String activeKid,
            KeyPair signingPair,
            Map<String, PublicKey> verificationKeys,
            boolean configureTtl) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "configuredActiveKeyId", activeKid);
        ReflectionTestUtils.setField(provider, "configuredPrivateKey",
                Base64.getEncoder().encodeToString(signingPair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(provider, "configuredPublicKeys",
                publicKeyConfig(verificationKeys));
        if (configureTtl) {
            ReflectionTestUtils.setField(provider, "accessTokenExpirationMs", 900_000L);
        }
        return provider;
    }

    private static String publicKeyConfig(Map<String, PublicKey> keys) {
        return keys.entrySet().stream()
                .map(entry -> entry.getKey() + ":"
                        + Base64.getEncoder().encodeToString(entry.getValue().getEncoded()))
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static long extractNumericClaim(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker) + marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
