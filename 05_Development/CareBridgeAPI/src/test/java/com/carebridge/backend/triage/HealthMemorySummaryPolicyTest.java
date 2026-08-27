package com.carebridge.backend.triage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.policy.HealthMemorySummaryPolicy;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.impl.HealthMemoryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.RAW_MARKER;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.SESSION_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.USER_A;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeCompletedSession;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — THMC-TC-04 (CRITICAL, CWE-359, PDPA minimization).
 * Oracle: HealthMemoryEntry.java:32 / BR-THMC-003 / TDS §17 C2.
 */
@ExtendWith(MockitoExtension.class)
class HealthMemorySummaryPolicyTest {

    @Mock private HealthMemoryEntryRepository memoryRepository;
    @Mock private IIntakeSessionRepository sessionRepository;

    @Test
    void thmcTc04_noRawFreeTextInSummaryPayloadOrLogs() {
        HealthMemoryServiceImpl service = new HealthMemoryServiceImpl(
                memoryRepository, sessionRepository,
                new HealthMemorySummaryPolicy(new ObjectMapper()), makeProperties());
        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A))
                .thenReturn(Optional.of(makeCompletedSession()));
        when(memoryRepository.existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1)).thenReturn(false);
        when(memoryRepository.save(any(HealthMemoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger policyLogger = (Logger) LoggerFactory.getLogger(HealthMemorySummaryPolicy.class);
        Logger serviceLogger = (Logger) LoggerFactory.getLogger(HealthMemoryServiceImpl.class);
        policyLogger.addAppender(appender);
        serviceLogger.addAppender(appender);
        HealthMemoryEntry saved;
        try {
            service.writeFromCompletedSession(SESSION_1, USER_A);
            ArgumentCaptor<HealthMemoryEntry> captor = ArgumentCaptor.forClass(HealthMemoryEntry.class);
            verify(memoryRepository).save(captor.capture());
            saved = captor.getValue();
        } finally {
            policyLogger.detachAppender(appender);
            serviceLogger.detachAppender(appender);
        }

        // Minimization: the raw free-text marker (present in symptoms snapshot AND
        // rawAiResponse.parentFreeText) must never be persisted (HealthMemoryEntry.java:32)
        assertThat(saved.getSummaryText()).doesNotContain(RAW_MARKER);
        assertThat(saved.getMemoryPayloadJson()).doesNotContain(RAW_MARKER);
        // Positive control: summary IS built from structured data (not empty/blanked)
        assertThat(saved.getSummaryText()).contains("YELLOW").contains("fever");
        // PDPA log hygiene: no marker, no full summary value in any captured log line
        assertThat(appender.list)
                .noneMatch(event -> event.getFormattedMessage().contains(RAW_MARKER))
                .noneMatch(event -> event.getFormattedMessage().contains(saved.getSummaryText()));
    }
}
