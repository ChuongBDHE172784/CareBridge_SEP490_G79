package com.carebridge.backend.integration.firebase;

import java.util.UUID;

/**
 * BR-DCC-013: bridges a CareBridge JWT identity into a Firebase custom token so the
 * client's {@code auth.uid} in RTDB Rules matches {@code careBridgeUserId} exactly.
 * The uid claim always comes from an already-authenticated caller — this method never
 * accepts an arbitrary target user id from a caller-controlled input.
 */
public interface IFirebaseAuthBridgeService {

    String createCustomToken(UUID careBridgeUserId);
}
