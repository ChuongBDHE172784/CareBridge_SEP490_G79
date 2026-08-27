package com.carebridge.backend.ai.service.impl;

import com.carebridge.backend.ai.entity.StructuredIntakeData;
import com.carebridge.backend.ai.event.StructuredIntakeExtracted;
import com.carebridge.backend.ai.repository.IStructuredIntakeDataRepository;
import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.ai.service.IStructuredIntakeService;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StructuredIntakeService implements IStructuredIntakeService {

    private static final Logger log = LoggerFactory.getLogger(StructuredIntakeService.class);

    // C2 (UC131): no raw symptom text in structured table — constraint prompt only
    private static final String EXTRACTION_CONSTRAINT =
        "[CONSTRAINT: Extract structured medical triage data ONLY. " +
        "Output JSON: {\"symptomList\": [\"...\"], \"durationDays\": N, \"intensity\": \"LOW|MEDIUM|HIGH\", \"emergencyFlag\": true/false}. " +
        "emergencyFlag=true ONLY for life-threatening symptoms. NEVER include patient names, IDs, or PII.] " +
        "Session context: [REDACTED]";

    private final IStructuredIntakeDataRepository structuredIntakeDataRepository;
    private final GeminiExtractionClient geminiExtractionClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void extract(IntakeSessionCompleted event) {
        // C4 (UC131): idempotent — skip if already extracted for this session
        if (structuredIntakeDataRepository.existsBySessionId(event.sessionId())) {
            log.info("Structured intake extraction outcome=ALREADY_EXTRACTED");
            return;
        }

        if (event.riskLevel() == RiskLevel.RED) {
            try {
                StructuredIntakeData data = StructuredIntakeData.builder()
                        .id(event.sessionId())
                        .sessionId(event.sessionId())
                        .symptomList("[]")
                        .durationDays(null)
                        .intensity(null)
                        .emergencyFlag(true)
                        .emergency(true)
                        .extractedAt(Instant.now())
                        .createdBy("SYSTEM")
                        .build();
                StructuredIntakeData saved = structuredIntakeDataRepository.save(data);
                eventPublisher.publishEvent(new StructuredIntakeExtracted(
                        UUID.randomUUID(), event.sessionId(), saved.getId(),
                        true, saved.getExtractedAt()));
                log.info("Structured intake extraction outcome=RED_MARKER_STORED");
            } catch (Exception exception) {
                log.warn("Structured intake RED marker failed reason={}",
                        exception.getClass().getSimpleName());
            }
            return;
        }

        try {
            GeminiExtractionClient.ExtractionResult result =
                    geminiExtractionClient.extractStructuredData(EXTRACTION_CONSTRAINT);

            if (result == null) {
                log.warn("Structured intake extraction outcome=NO_RESULT");
                return;
            }

            // C2 (UC131): no raw symptom text — symptomList stored as structured JSON only
            // C5 (UC131): createdBy="SYSTEM"
            StructuredIntakeData data = StructuredIntakeData.builder()
                    .id(event.sessionId())
                    .sessionId(event.sessionId())
                    .symptomList(result.symptomListJson())
                    .durationDays(result.durationDays())
                    .intensity(result.intensity())
                    .emergencyFlag(result.emergencyFlag())
                    .emergency(false)
                    .extractedAt(Instant.now())
                    .createdBy("SYSTEM")
                    .build();
            StructuredIntakeData saved = structuredIntakeDataRepository.save(data);

            eventPublisher.publishEvent(new StructuredIntakeExtracted(
                    UUID.randomUUID(), event.sessionId(), saved.getId(),
                    result.emergencyFlag(), saved.getExtractedAt()));

            log.info("Structured intake extraction outcome=STORED");

        } catch (Exception e) {
            log.warn("Structured intake extraction failed reason={}", e.getClass().getSimpleName());
        }
    }
}
