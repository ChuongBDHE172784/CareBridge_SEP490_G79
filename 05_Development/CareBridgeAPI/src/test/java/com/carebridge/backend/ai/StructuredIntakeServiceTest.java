package com.carebridge.backend.ai;

import com.carebridge.backend.ai.entity.StructuredIntakeData;
import com.carebridge.backend.ai.event.StructuredIntakeExtracted;
import com.carebridge.backend.ai.repository.IStructuredIntakeDataRepository;
import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.ai.service.impl.StructuredIntakeService;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructuredIntakeServiceTest {

    @Mock
    private IStructuredIntakeDataRepository structuredIntakeDataRepository;

    @Mock
    private GeminiExtractionClient geminiExtractionClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StructuredIntakeService structuredIntakeService;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000041");

    private IntakeSessionCompleted makeEvent() {
        return new IntakeSessionCompleted(UUID.randomUUID(), SESSION_ID, USER_ID, RiskLevel.GREEN, Instant.now());
    }

    @Test
    void extract_alreadyExtracted_shouldSkip() {
        when(structuredIntakeDataRepository.existsBySessionId(SESSION_ID)).thenReturn(true);

        structuredIntakeService.extract(makeEvent());

        verify(structuredIntakeDataRepository, never()).save(any());
        verifyNoInteractions(geminiExtractionClient);
    }

    @Test
    void extract_portReturnsNull_shouldNotThrowAndShouldNotSave() {
        // regression: geminiExtractionClient.extractStructuredData() returning null must not NPE
        when(structuredIntakeDataRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(geminiExtractionClient.extractStructuredData(any())).thenReturn(null);

        structuredIntakeService.extract(makeEvent());

        verify(structuredIntakeDataRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void extract_validResult_shouldSaveAndPublishEvent() {
        when(structuredIntakeDataRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(geminiExtractionClient.extractStructuredData(any()))
                .thenReturn(new GeminiExtractionClient.ExtractionResult("[\"fever\"]", 2, "LOW", false));
        when(structuredIntakeDataRepository.save(any(StructuredIntakeData.class)))
                .thenAnswer(invocation -> {
                    StructuredIntakeData data = invocation.getArgument(0);
                    data.setId(UUID.randomUUID());
                    return data;
                });

        structuredIntakeService.extract(makeEvent());

        verify(structuredIntakeDataRepository).save(any(StructuredIntakeData.class));
        verify(eventPublisher).publishEvent(any(StructuredIntakeExtracted.class));
    }
}
