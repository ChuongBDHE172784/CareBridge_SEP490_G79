package com.carebridge.backend.security.jwt;

import com.carebridge.backend.common.constants.SecurityConstants;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.security.entity.User;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${carebridge.security.jwt.secret:}")
    private String configuredSecret;

    @Value("${carebridge.security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    private byte[] secretBytes;

    @PostConstruct
    void init() {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured. Set the JWT_SECRET environment variable with a strong secret (at least 32 characters).");
        }
        String trimmedSecret = configuredSecret.trim();
        if (trimmedSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET is too weak. Must be at least 32 characters.");
        }
        secretBytes = trimmedSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateAccessToken(User user) {
        return generateAccessToken(user, null);
    }

    public String generateAccessToken(User user, UUID sessionId) {
        Instant now = Instant.now();
        StringBuilder payload = new StringBuilder("{")
                .append("\"sub\":\"").append(escape(user.getId().toString())).append("\",")
                .append("\"").append(SecurityConstants.CLAIM_PHONE).append("\":\"").append(escape(user.getPhone())).append("\",")
                .append("\"").append(SecurityConstants.CLAIM_ROLE).append("\":\"").append(escape(user.getRole().getAuthority())).append("\",")
                .append("\"").append(SecurityConstants.CLAIM_TOKEN_TYPE).append("\":\"").append(SecurityConstants.ACCESS_TOKEN_TYPE).append("\",")
                .append("\"iat\":").append(now.getEpochSecond()).append(",")
                .append("\"exp\":").append(now.plusMillis(accessTokenExpirationMs).getEpochSecond());
        if (sessionId != null) {
            payload.append(",\"").append(SecurityConstants.CLAIM_SESSION_ID).append("\":\"").append(escape(sessionId.toString())).append("\"");
        }
        payload.append("}");
        return sign(payload.toString());
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = splitToken(token);
            String signatureInput = parts[0] + "." + parts[1];
            String expectedSignature = encode(hmac(signatureInput.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
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
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String encodedHeader = encode(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
            String signatureInput = encodedHeader + "." + encodedPayload;
            String encodedSignature = encode(hmac(signatureInput.getBytes(StandardCharsets.UTF_8)));
            return signatureInput + "." + encodedSignature;
        } catch (Exception ex) {
            throw new AuthenticationException("Could not generate JWT");
        }
    }

    private byte[] hmac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (Exception ex) {
            throw new AuthenticationException("Could not sign JWT");
        }
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
