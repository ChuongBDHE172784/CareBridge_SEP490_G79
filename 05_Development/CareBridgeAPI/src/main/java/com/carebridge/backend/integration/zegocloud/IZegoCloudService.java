package com.carebridge.backend.integration.zegocloud;

/**
 * UC-154 Establish Realtime Communication Session. Stateless token generation only —
 * does NOT read or write any consultation session state (see UC-154 TDS ADR-ZEGO-002,
 * realigned 2026-07-15). Same Token04 format serves both the RTC room (voice/video,
 * UC-145/146) and ZIM in-app-messaging login (chat signaling, UC-144).
 */
public interface IZegoCloudService {

    /**
     * Generates a stateless ZegoCloud Token04 for an already-authorized consultation-session
     * participant. Callers (UC-95 ConsultationSessionService) are responsible for their own
     * authorization checks before calling this.
     *
     * @param sessionId archived consultation session id (= roomId, string form)
     * @param userId    caller's users.user_id, string form
     * @param userName  display name for client UI
     * @return token bundle — never persisted by the caller
     */
    ZegoTokenDto generateToken(String sessionId, String userId, String userName);
}
