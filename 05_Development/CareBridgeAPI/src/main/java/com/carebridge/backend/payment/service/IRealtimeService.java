package com.carebridge.backend.payment.service;

/**
 * Realtime Service Interface.
 * Defines contract for realtime communication session management.
 *
 * ISP: Interface segregation - only realtime-specific methods.
 */
public interface IRealtimeService {

    /**
     * Create a realtime session for a consultation.
     *
     * @param bookingId the booking ID
     * @param providerType the realtime provider (e.g., ZEGO, AGORA)
     * @return session token and room info
     */
    com.carebridge.backend.consultation.dto.response.ConsultationSessionDTO createSession(Long bookingId, String providerType);

    /**
     * Start a realtime session.
     *
     * @param sessionToken the session token
     * @return updated session info
     */
    com.carebridge.backend.consultation.dto.response.ConsultationSessionDTO startSession(String sessionToken);

    /**
     * End a realtime session.
     *
     * @param sessionToken the session token
     * @return true if ended successfully
     */
    boolean endSession(String sessionToken);

    /**
     * Get session info by token.
     *
     * @param sessionToken the session token
     * @return session info
     */
    com.carebridge.backend.consultation.dto.response.ConsultationSessionDTO getSession(String sessionToken);
}
