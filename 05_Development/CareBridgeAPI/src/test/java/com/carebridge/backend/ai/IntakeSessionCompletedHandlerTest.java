package com.carebridge.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

import com.carebridge.backend.ai.service.IStructuredIntakeService;
import com.carebridge.backend.ai.service.IntakeSessionCompletedHandler;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class IntakeSessionCompletedHandlerTest {

    @Mock
    private IStructuredIntakeService structuredIntakeService;

    @InjectMocks
    private IntakeSessionCompletedHandler handler;

    @Test
    void completionSideWork_shouldRunOnlyAfterAuthoritativeTransactionCommits() throws Exception {
        var method = IntakeSessionCompletedHandler.class
                .getMethod("onIntakeSessionCompleted", IntakeSessionCompleted.class);

        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isFalse();
    }

    @Test
    void isolatedStructuredPersistenceFailure_shouldNotEscapeAfterCommitHandler() {
        IntakeSessionCompleted event = new IntakeSessionCompleted(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), RiskLevel.RED, Instant.now());
        doThrow(new IllegalStateException("synthetic isolated persistence failure"))
                .when(structuredIntakeService).extract(event);

        assertThatCode(() -> handler.onIntakeSessionCompleted(event)).doesNotThrowAnyException();
    }
}
