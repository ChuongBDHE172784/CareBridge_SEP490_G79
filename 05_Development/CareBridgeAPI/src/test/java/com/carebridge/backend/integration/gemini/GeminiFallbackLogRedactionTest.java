package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.ai.adapter.GeminiExtractionClientAdapter;
import com.carebridge.backend.integration.gemini.builder.GeminiPromptBuilder;
import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import com.carebridge.backend.integration.gemini.retriever.RagContextRetriever;
import com.carebridge.backend.integration.gemini.service.GeminiRagServiceImpl;
import com.carebridge.backend.triage.adapter.GeminiTriageClientAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class GeminiFallbackLogRedactionTest {

    private static final String LEAK_CANARY =
            "prompt=private-health-note token=OV01-GEMINI-SECRET";

    @Test
    void triageAdapterLogsOnlyBoundedCategoryAndExceptionType() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generate(anyString())).thenThrow(new RuntimeException(LEAK_CANARY));

        List<String> messages = capture(GeminiTriageClientAdapter.class,
                () -> new GeminiTriageClientAdapter(client).analyzeSymptoms("synthetic"));

        assertThat(messages).containsExactly(
                "Gemini triage fallback reason=GEMINI_UNAVAILABLE exceptionType=RuntimeException");
        assertThat(messages).noneMatch(message -> message.contains(LEAK_CANARY));
    }

    @Test
    void extractionAdapterLogsOnlyBoundedCategoryAndExceptionType() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generate(anyString())).thenThrow(new IllegalStateException(LEAK_CANARY));

        List<String> messages = capture(GeminiExtractionClientAdapter.class,
                () -> new GeminiExtractionClientAdapter(client, new ObjectMapper())
                        .extractStructuredData("synthetic"));

        assertThat(messages).containsExactly(
                "Gemini extraction fallback reason=GEMINI_UNAVAILABLE exceptionType=IllegalStateException");
        assertThat(messages).noneMatch(message -> message.contains(LEAK_CANARY));
    }

    @Test
    void ragAdapterLogsOnlyBoundedCategoryAndExceptionType() {
        GeminiClient client = mock(GeminiClient.class);
        RagContextRetriever retriever = mock(RagContextRetriever.class);
        when(retriever.retrieveContext("synthetic", null, 5)).thenReturn(List.of());
        when(client.generate(anyString()))
                .thenThrow(new GeminiUnavailableException(LEAK_CANARY));
        GeminiRagServiceImpl service = new GeminiRagServiceImpl(
                retriever, client, new GeminiPromptBuilder());
        RagAnswerRequest request = RagAnswerRequest.builder()
                .query("synthetic")
                .userStage(UserStage.PREGNANCY)
                .build();

        List<String> messages = capture(GeminiRagServiceImpl.class,
                () -> service.generateAnswer(request,
                        new RagExecutionContext(false, null, UserStage.PREGNANCY)));

        assertThat(messages).containsExactly(
                "RagFallbackTriggered reason=GEMINI_UNAVAILABLE "
                        + "exceptionType=GeminiUnavailableException");
        assertThat(messages).noneMatch(message -> message.contains(LEAK_CANARY));
    }

    private List<String> capture(Class<?> loggerType, Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerType);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
