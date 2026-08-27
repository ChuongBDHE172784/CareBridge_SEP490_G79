package com.carebridge.backend.security.jwt;

import com.carebridge.backend.common.constants.SecurityConstants;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.security.entity.User;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String RSA_SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int MINIMUM_RSA_BITS = 2048;

    @Value("${carebridge.security.jwt.active-key-id:}")
    private String configuredActiveKeyId;

    @Value("${carebridge.security.jwt.private-key:}")
    private String configuredPrivateKey;

    @Value("${carebridge.security.jwt.public-keys:}")
    private String configuredPublicKeys;

    @Value("${carebridge.security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    private String activeKeyId;
    private PrivateKey privateKey;
    private Map<String, PublicKey> publicKeys;

    @PostConstruct
    void init() {
        if (configuredActiveKeyId == null || configuredActiveKeyId.isBlank()) {
            throw new IllegalStateException("JWT_ACTIVE_KEY_ID is not configured");
        }
        if (configuredPrivateKey == null || configuredPrivateKey.isBlank()) {
            throw new IllegalStateException("JWT_PRIVATE_KEY is not configured");
        }
        if (configuredPublicKeys == null || configuredPublicKeys.isBlank()) {
            throw new IllegalStateException("JWT_PUBLIC_KEYS is not configured");
        }

        activeKeyId = configuredActiveKeyId.trim();
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(
                    decodeKeyMaterial(configuredPrivateKey)));
            if (!(privateKey instanceof RSAPrivateKey rsaPrivateKey)
                    || rsaPrivateKey.getModulus().bitLength() < MINIMUM_RSA_BITS) {
                throw new IllegalStateException("JWT RSA private key must be at least 2048 bits");
            }

            Map<String, PublicKey> parsedPublicKeys = new LinkedHashMap<>();
            for (String entry : configuredPublicKeys.split(";")) {
                if (entry.isBlank()) {
                    continue;
                }
                int separator = entry.indexOf(':');
                if (separator <= 0 || separator == entry.length() - 1) {
                    throw new IllegalStateException(
                            "JWT_PUBLIC_KEYS must use kid:base64Der entries separated by semicolons");
                }
                String keyId = entry.substring(0, separator).trim();
                if (parsedPublicKeys.containsKey(keyId)) {
                    throw new IllegalStateException("JWT_PUBLIC_KEYS contains duplicate kid " + keyId);
                }
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(
                        decodeKeyMaterial(entry.substring(separator + 1))));
                if (!(publicKey instanceof RSAPublicKey rsaPublicKey)
                        || rsaPublicKey.getModulus().bitLength() < MINIMUM_RSA_BITS) {
                    throw new IllegalStateException("JWT RSA public key must be at least 2048 bits");
                }
                parsedPublicKeys.put(keyId, publicKey);
            }
            if (!parsedPublicKeys.containsKey(activeKeyId)) {
                throw new IllegalStateException(
                        "JWT_PUBLIC_KEYS does not contain active kid " + activeKeyId);
            }
            publicKeys = Map.copyOf(parsedPublicKeys);

            byte[] probe = "carebridge-jwt-key-pair-check".getBytes(StandardCharsets.UTF_8);
            if (!verifyRsa(publicKeys.get(activeKeyId), probe, signRsa(probe))) {
                throw new IllegalStateException(
                        "JWT private key does not match the active public key");
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("JWT RSA key configuration is invalid", exception);
        }
    }

    public String generateAccessToken(User user) {
        return generateAccessToken(user, null);
    }

    public String generateAccessToken(User user, UUID sessionId) {
        Instant now = Instant.now();
        StringBuilder payload = new StringBuilder("{")
                .append("\"sub\":\"").append(escape(user.getId().toString())).append("\",")
                .append("\"").append(SecurityConstants.CLAIM_PHONE).append("\":\"").append(escape(user.getPhone())).append("\",");
        if (user.getRole() != null) {
            payload.append("\"").append(SecurityConstants.CLAIM_ROLE).append("\":\"")
                    .append(escape(user.getRole().getAuthority())).append("\",");
        }
        payload.append("\"").append(SecurityConstants.CLAIM_TOKEN_TYPE).append("\":\"").append(SecurityConstants.ACCESS_TOKEN_TYPE).append("\",")
                .append("\"iat\":").append(now.getEpochSecond()).append(",")
                .append("\"exp\":").append(now.plusMillis(accessTokenExpirationMs).getEpochSecond());
        if (sessionId != null) {
            payload.append(",\"").append(SecurityConstants.CLAIM_SESSION_ID).append("\":\"").append(escape(sessionId.toString())).append("\"");
        }
        payload.append("}");
        return sign(payload.toString());
    }

    public String generateAppealToken(User user) {
        if (user.getLockEpisodeId() == null) {
            throw new AuthenticationException("Lock episode is missing");
        }
        Instant now = Instant.now();
        String payload = "{"
                + "\"sub\":\"" + escape(user.getId().toString()) + "\","
                + "\"" + SecurityConstants.CLAIM_TOKEN_TYPE + "\":\""
                + SecurityConstants.APPEAL_TOKEN_TYPE + "\","
                + "\"" + SecurityConstants.CLAIM_LOCK_EPISODE_ID + "\":\""
                + escape(user.getLockEpisodeId().toString()) + "\","
                + "\"iat\":" + now.getEpochSecond() + ","
                + "\"exp\":" + now.plusSeconds(10 * 60).getEpochSecond()
                + "}";
        return sign(payload);
    }

    public AppealTokenClaims validateAppealToken(String token) {
        try {
            String[] parts = splitToken(token);
            String header = decode(parts[0]);
            if (!"RS256".equals(getJsonValue(header, "alg"))) return null;
            PublicKey verificationKey = publicKeys.get(getJsonValue(header, "kid"));
            if (verificationKey == null) return null;
            String signatureInput = parts[0] + "." + parts[1];
            if (!verifyRsa(verificationKey, signatureInput.getBytes(StandardCharsets.UTF_8),
                    Base64.getUrlDecoder().decode(parts[2]))) return null;
            String claims = decode(parts[1]);
            if (!SecurityConstants.APPEAL_TOKEN_TYPE.equals(
                    getJsonValue(claims, SecurityConstants.CLAIM_TOKEN_TYPE))) return null;
            String expiresAt = getJsonValue(claims, "exp");
            if (expiresAt == null || Long.parseLong(expiresAt) <= Instant.now().getEpochSecond()) return null;
            return new AppealTokenClaims(
                    UUID.fromString(getJsonValue(claims, "sub")),
                    UUID.fromString(getJsonValue(claims, SecurityConstants.CLAIM_LOCK_EPISODE_ID)));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public record AppealTokenClaims(UUID userId, UUID lockEpisodeId) {}

    public boolean validateToken(String token) {
        try {
            String[] parts = splitToken(token);
            String header = decode(parts[0]);
            if (!"RS256".equals(getJsonValue(header, "alg"))) {
                return false;
            }
            String keyId = getJsonValue(header, "kid");
            PublicKey verificationKey = publicKeys.get(keyId);
            if (verificationKey == null) {
                return false;
            }
            String signatureInput = parts[0] + "." + parts[1];
            if (!verifyRsa(
                    verificationKey,
                    signatureInput.getBytes(StandardCharsets.UTF_8),
                    Base64.getUrlDecoder().decode(parts[2]))) {
                return false;
            }
            String claims = decode(parts[1]);
            String expiresAt = getJsonValue(claims, "exp");
            return expiresAt != null
                    && Long.parseLong(expiresAt) > Instant.now().getEpochSecond()
                    && SecurityConstants.ACCESS_TOKEN_TYPE.equals(
                            getJsonValue(claims, SecurityConstants.CLAIM_TOKEN_TYPE));
        } catch (Exception ex) {
            return false;
        }
    }

    public String getSubject(String token) {
        String subject = getJsonValue(decode(splitToken(token)[1]), "sub");
        if (subject == null) {
            throw new AuthenticationException("JWT subject is missing");
        }
        return subject;
    }

    public UUID getSessionId(String token) {
        String sid = getJsonValue(decode(splitToken(token)[1]), SecurityConstants.CLAIM_SESSION_ID);
        if (sid == null) {
            return null;
        }
        try {
            return UUID.fromString(sid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<SimpleGrantedAuthority> getAuthorities(String token) {
        String role = getJsonValue(decode(splitToken(token)[1]), SecurityConstants.CLAIM_ROLE);
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(role));
    }

    private String sign(String payload) {
        try {
            String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\""
                    + escape(activeKeyId) + "\"}";
            String encodedHeader = encode(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
            String signatureInput = encodedHeader + "." + encodedPayload;
            String encodedSignature = encode(signRsa(
                    signatureInput.getBytes(StandardCharsets.UTF_8)));
            return signatureInput + "." + encodedSignature;
        } catch (Exception ex) {
            throw new AuthenticationException("Could not generate JWT");
        }
    }

    private byte[] signRsa(byte[] payload) {
        try {
            Signature signature = Signature.getInstance(RSA_SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(payload);
            return signature.sign();
        } catch (Exception ex) {
            throw new AuthenticationException("Could not sign JWT");
        }
    }

    private boolean verifyRsa(PublicKey key, byte[] payload, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance(RSA_SIGNATURE_ALGORITHM);
            signature.initVerify(key);
            signature.update(payload);
            return signature.verify(signatureBytes);
        } catch (Exception exception) {
            return false;
        }
    }

    private byte[] decodeKeyMaterial(String value) {
        String normalized = value
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private String[] splitToken(String token) {
        if (token == null) {
            throw new AuthenticationException("JWT is missing");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationException("Invalid JWT");
        }
        return parts;
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String decode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private String getJsonValue(String json, String key) {
        String marker = "\"" + key + "\":";
        int markerIndex = json.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        int valueStart = markerIndex + marker.length();
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            return valueEnd < 0 ? null : unescape(json.substring(valueStart + 1, valueEnd));
        }
        int valueEnd = valueStart;
        while (valueEnd < json.length() && "0123456789".indexOf(json.charAt(valueEnd)) >= 0) {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
