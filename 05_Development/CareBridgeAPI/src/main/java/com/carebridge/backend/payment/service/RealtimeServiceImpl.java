package com.carebridge.backend.payment.service;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.dto.response.ConsultationSessionDTO;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.consultation.entity.ConsultationSession;
import com.carebridge.backend.consultation.repository.ConsultationRepository;
import com.carebridge.backend.consultation.repository.ConsultationSessionRepository;
import com.carebridge.backend.expert.enums.SessionStatus;
import com.carebridge.backend.expert.enums.ConsultationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Realtime Service Implementation.
 * Manages realtime communication sessions for consultations.
 *
 * Implements IRealtimeService for session management.
 * Uses mock provider for Sprint 0.
 *
 * TV4 Use Cases: 3.1.2.7 (Establish Realtime Communication Session)
 */
@Service("realtimeService")
@RequiredArgsConstructor
@Slf4j
public class RealtimeServiceImpl implements IRealtimeService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationSessionRepository sessionRepository;

    @Override
    @Transactional
    public ConsultationSessionDTO createSession(Long bookingId, String providerType) {
        log.info("Creating realtime session for bookingId: {}", bookingId);

        Consultation consultation = consultationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (consultation.getStatus() != ConsultationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed consultations can have realtime sessions");
        }

        // Check if session already exists
        Optional<ConsultationSession> existing = sessionRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            ConsultationSession session = existing.get();
            log.debug("Returning existing session: {}", session.getSessionId());
            return mapToDTO(session);
        }

        // Create new session with mock provider (Sprint 0)
        String sessionToken = "realtime-mock-token-" + UUID.randomUUID();
        String roomId = "room-mock-" + UUID.randomUUID().toString().substring(0, 8);

        ConsultationSession session = ConsultationSession.builder()
                .bookingId(bookingId)
                .communicationRoomId(roomId)
                .sessionToken(sessionToken)
                .providerType("MOCK") // Use providerType param in Sprint 1+
                .sessionStatus(SessionStatus.CREATED)
                .build();

        session = sessionRepository.save(session);
        log.info("Created mock realtime session: sessionId={}, roomId={}", session.getSessionId(), roomId);

        return mapToDTO(session);
    }

    @Override
    @Transactional
    public ConsultationSessionDTO startSession(String sessionToken) {
        log.info("Starting realtime session with token: {}", sessionToken);

        ConsultationSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        session = sessionRepository.save(session);

        log.info("Session started: sessionId={}", session.getSessionId());
        return mapToDTO(session);
    }

    @Override
    @Transactional
    public boolean endSession(String sessionToken) {
        log.info("Ending realtime session with token: {}", sessionToken);

        ConsultationSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setSessionStatus(SessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        sessionRepository.save(session);

        log.info("Session ended: sessionId={}", session.getSessionId());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationSessionDTO getSession(String sessionToken) {
        log.debug("Getting session with token: {}", sessionToken);

        ConsultationSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        return mapToDTO(session);
    }

    /**
     * Map entity to DTO.
     */
    private ConsultationSessionDTO mapToDTO(ConsultationSession session) {
        return ConsultationSessionDTO.builder()
                .sessionId(session.getSessionId())
                .bookingId(session.getBookingId())
                .communicationRoomId(session.getCommunicationRoomId())
                .sessionToken(session.getSessionToken())
                .providerType(session.getProviderType())
                .sessionStatus(session.getSessionStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
