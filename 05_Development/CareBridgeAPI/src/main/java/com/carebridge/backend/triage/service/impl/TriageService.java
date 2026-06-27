package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.event.IntakeSessionFailed;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.ITriageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TriageService implements ITriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private static final String CONSTRAINT_BLOCK =
        "[CONSTRAINT: You are a medical triage assistant. You MUST: " +
        "1. ONLY classify as GREEN (low risk), YELLOW (moderate risk), or RED (high risk). " +
        "2. NEVER diagnose any condition. 3. NEVER prescribe medications. " +
        "4. ALWAYS include disclaimer that this is AI guidance only, not medical diagnosis. " +
        "Respond in JSON: {\"riskLevel\": \"GREEN|YELLOW|RED\", \"disclaimer\": \"...\"}] ";

    private final IIntakeSessionRepository intakeSessionRepository;
    private final GeminiTriageClient geminiTriageClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public IntakeSessionResponse runIntake(RunIntakeRequest request, UUID userId) {
        IntakeSession session = IntakeSession.builder()
                .userId(userId)
                .symptoms(request.getSymptoms())
                .babyProfileId(request.getBabyProfileId())
                .status(IntakeStatus.PROCESSING)
                .createdAt(Instant.now())
                .createdBy(userId)
                .build();
        session = intakeSessionRepository.save(session);
        UUID sessionId = session.getId();
        // C3 (UC60): NO symptom text in logs — session ID only
        log.info("Processing intake for session [{}]", sessionId);

        try {
            // Constraint prompt passed to Gemini; symptoms are never logged (PDPA C3)
            String constrainedPrompt = CONSTRAINT_BLOCK + "User symptoms: [REDACTED]";
            GeminiTriageClient.AiTriageResult result = geminiTriageClient.analyzeSymptoms(constrainedPrompt);

            session.setRiskLevel(result.riskLevel());
            session.setDisclaimer(result.disclaimer());
            session.setStatus(IntakeStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            session = intakeSessionRepository.save(session);

            eventPublisher.publishEvent(new IntakeSessionCompleted(
                    UUID.randomUUID(), session.getId(), userId,
                    result.riskLevel(), session.getCompletedAt()));

            log.info("Intake completed for session [{}] riskLevel=[{}]", sessionId, result.riskLevel());

        } catch (Exception e) {
            // C6 (UC60): Gemini timeout/error → FAILED + publish IntakeSessionFailed
            log.warn("Gemini unavailable for session [{}]: {}", sessionId, e.getClass().getSimpleName());
            session.setStatus(IntakeStatus.FAILED);
            session = intakeSessionRepository.save(session);
            eventPublisher.publishEvent(new IntakeSessionFailed(
                    UUID.randomUUID(), session.getId(), userId,
                    "AI service unavailable", Instant.now()));
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE, "TRIAGE-005", "AI service unavailable");
        }

        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResultResponse getResult(UUID sessionId, UUID userId) {
        IntakeSession session = intakeSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-003",
                        "Intake session not found: " + sessionId));

        return TriageResultResponse.builder()
                .sessionId(session.getId())
                .riskLevel(session.getRiskLevel() != null ? session.getRiskLevel().name() : null)
                .disclaimer(session.getDisclaimer())
                .status(session.getStatus().name())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntakeSessionResponse> listSessions(UUID userId) {
        return intakeSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private IntakeSessionResponse toResponse(IntakeSession session) {
        return IntakeSessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus().name())
                .riskLevel(session.getRiskLevel() != null ? session.getRiskLevel().name() : null)
                .disclaimer(session.getDisclaimer())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }
}
