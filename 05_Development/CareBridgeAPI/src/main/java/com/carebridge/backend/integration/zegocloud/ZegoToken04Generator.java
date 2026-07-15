package com.carebridge.backend.integration.zegocloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Generates ZegoCloud "Token04" session tokens server-side.
 *
 * <p>Rewritten from the project's prior ZegoCloud integration reference
 * (TokenServerAssistant.generateToken04 / a hand-rolled Base64 clone) to use only
 * JDK-provided primitives (java.util.Base64, javax.crypto) and Jackson (already a Spring
 * Boot dependency) instead of org.json.simple — no new pom.xml dependency required. The
 * wire format (expire[8] + ivLen[2]+iv + contentLen[2]+content, "04" + base64) is
 * unchanged and matches ZegoCloud's official Token04 spec, shared across ZegoCloud's SDK
 * families (RTC + ZIM).
 */
@Component
class ZegoToken04Generator {

    private static final String VERSION_FLAG = "04";
    private static final int IV_LENGTH = 16;
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    String generate(long appId, String userId, String serverSecret, int effectiveSeconds) {
        if (appId <= 0) {
            throw new IllegalArgumentException("illegal appId");
        }
        if (userId == null || userId.isBlank() || userId.length() > 64) {
            throw new IllegalArgumentException("userId must not be blank and no more than 64 characters");
        }
        if (serverSecret == null || serverSecret.length() != 32) {
            throw new IllegalArgumentException("serverSecret must be 32 characters");
        }
        if (effectiveSeconds <= 0) {
            throw new IllegalArgumentException("effectiveSeconds must be > 0");
        }

        try {
            byte[] ivBytes = new byte[IV_LENGTH];
            secureRandom.nextBytes(ivBytes);

            long nowTime = System.currentTimeMillis() / 1000;
            long expireTime = nowTime + effectiveSeconds;

            ObjectNode json = objectMapper.createObjectNode();
            json.put("app_id", appId);
            json.put("user_id", userId);
            json.put("ctime", nowTime);
            json.put("expire", expireTime);
            json.put("nonce", secureRandom.nextInt());
            json.put("payload", "");
            String content = objectMapper.writeValueAsString(json);

            byte[] contentBytes = encrypt(
                    content.getBytes(StandardCharsets.UTF_8),
                    serverSecret.getBytes(StandardCharsets.UTF_8),
                    ivBytes);

            ByteBuffer buffer = ByteBuffer.allocate(8 + 2 + ivBytes.length + 2 + contentBytes.length);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(expireTime);
            buffer.putShort((short) ivBytes.length);
            buffer.put(ivBytes);
            buffer.putShort((short) contentBytes.length);
            buffer.put(contentBytes);

            return VERSION_FLAG + Base64.getEncoder().encodeToString(buffer.array());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ZegoTokenGenerationException("Failed to generate ZegoCloud token", e);
        }
    }

    private byte[] encrypt(byte[] content, byte[] secretKey, byte[] ivBytes) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(content);
    }
}
