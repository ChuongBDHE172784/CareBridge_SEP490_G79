package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.event.IntakeSessionFailed;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TriageService implements ITriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final IIntakeSessionRepository intakeSessionRepository;
    private final ChildTriageAiClient childTriageAiClient;
    private final TriageGraphService triageGraphService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public IntakeSessionResponse runIntake(RunIntakeRequest request, UUID userId) {
        IntakeSession session = IntakeSession.builder()
                .userId(userId)
                .symptoms(snapshotRequest(request))
                .babyProfileId(request.getBabyProfileId())
                .status(IntakeStatus.PROCESSING)
                .createdAt(Instant.now())
                .createdBy(userId)
                .build();
        session = intakeSessionRepository.save(session);
        UUID sessionId = session.getId();
        log.info("Processing intake for session [{}]", sessionId);

        try {
            String aiResponse = triageWithAiServiceOrFallback(request, sessionId);
            JsonNode result = objectMapper.readTree(aiResponse);
            String triageStatus = result.path("status").asText(null);
            String riskLevel = result.path("riskLevel").isMissingNode() || result.path("riskLevel").isNull()
                    ? null
                    : result.path("riskLevel").asText();
            if (triageStatus == null) {
                triageStatus = "NEED_MORE_INFO".equals(riskLevel) ? "NEED_MORE_INFO" : "COMPLETED";
            }

            session.setRawAiResponse(aiResponse);
            session.setRiskLevel(isPersistableRiskLevel(riskLevel) ? RiskLevel.valueOf(riskLevel) : null);
            session.setDisclaimer(result.path("disclaimer").asText(null));
            session.setStatus("NEED_MORE_INFO".equals(triageStatus)
                    ? IntakeStatus.NEED_MORE_INFO
                    : IntakeStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            session = intakeSessionRepository.save(session);

            if (session.getStatus() == IntakeStatus.COMPLETED && session.getRiskLevel() != null) {
                eventPublisher.publishEvent(new IntakeSessionCompleted(
                        UUID.randomUUID(), session.getId(), userId,
                        session.getRiskLevel(), session.getCompletedAt()));
            }

            log.info("Intake completed for session [{}] status=[{}] riskLevel=[{}]",
                    sessionId, session.getStatus(), session.getRiskLevel());
        } catch (Exception e) {
            log.warn("Triage graph failed for session [{}]: {}", sessionId, e.getClass().getSimpleName());
            session.setStatus(IntakeStatus.FAILED);
            intakeSessionRepository.save(session);
            eventPublisher.publishEvent(new IntakeSessionFailed(
                    UUID.randomUUID(), session.getId(), userId,
                    "Triage processing failed", Instant.now()));
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE, "TRIAGE-005", "Triage processing failed");
        }

        return toResponse(session);
    }

    private String triageWithAiServiceOrFallback(RunIntakeRequest request, UUID sessionId) throws JsonProcessingException {
        try {
            return childTriageAiClient.triageChild(request);
        } catch (Exception e) {
            log.warn("AI triage service unavailable for session [{}], falling back to Java rule engine: {}",
                    sessionId, e.getClass().getSimpleName());
            ChildTriageResult graphResult = triageGraphService.run(request);
            return objectMapper.writeValueAsString(graphResult);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResultResponse getResult(UUID sessionId, UUID userId) {
        IntakeSession session = intakeSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-003",
                        "Intake session not found: " + sessionId));

        return TriageResultResponse.builder()
                .sessionId(session.getId())
                .triageStatus(readText(session, "status", session.getStatus().name()))
                .riskLevel(readText(session, "riskLevel"))
                .riskColor(readText(session, "riskColor"))
                .summary(readText(session, "summary"))
                .possibleConcern(readText(session, "possibleConcern"))
                .recommendedAction(readText(session, "recommendedAction"))
                .emergencyActionRequired(readBoolean(session, "emergencyActionRequired"))
                .redFlags(readStringList(session, "redFlags"))
                .matchedRules(readStringList(session, "matchedRules"))
                .citations(readObjectList(session, "citations"))
                .evidence(readObject(session, "evidence"))
                .disclaimer(readText(session, "disclaimer", session.getDisclaimer()))
                .questions(readStringList(session, "questions"))
                .warning(readText(session, "warning"))
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

    private String snapshotRequest(RunIntakeRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return request.getSymptoms() != null ? request.getSymptoms() : "";
        }
    }

    private JsonNode rawResult(IntakeSession session) {
        if (session.getRawAiResponse() == null || session.getRawAiResponse().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(session.getRawAiResponse());
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private String readText(IntakeSession session, String field) {
        return readText(session, field, null);
    }

    private String readText(IntakeSession session, String field, String fallback) {
        JsonNode node = rawResult(session).path(field);
        if (node.isMissingNode() || node.isNull()) {
            if ("riskLevel".equals(field) && session.getRiskLevel() != null) {
                return session.getRiskLevel().name();
            }
            return fallback;
        }
        return node.asText();
    }

    private boolean isPersistableRiskLevel(String riskLevel) {
        return "GREEN".equals(riskLevel) || "YELLOW".equals(riskLevel) || "RED".equals(riskLevel);
    }

    private Boolean readBoolean(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        return node.isMissingNode() || node.isNull() ? Boolean.FALSE : node.asBoolean();
    }

    private List<String> readStringList(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private List<Map<String, Object>> readObjectList(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
    }

    private Map<String, Object> readObject(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isObject()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }
}
