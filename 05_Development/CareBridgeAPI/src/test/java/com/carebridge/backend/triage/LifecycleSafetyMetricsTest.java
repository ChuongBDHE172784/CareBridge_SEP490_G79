package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics.Boundary;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics.Outcome;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LifecycleSafetyMetricsTest {

    @Test
    void structuredOutcomes_areCountedWithoutIdentifiersTokensRoutesOrHealthContent() {
        Logger logger = (Logger) LoggerFactory.getLogger(LifecycleSafetyMetrics.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            LifecycleSafetyMetrics metrics = new LifecycleSafetyMetrics();
            Arrays.stream(Outcome.values()).forEach(outcome -> {
                metrics.record(Boundary.CONTINUATION, outcome);
                metrics.record(Boundary.PROJECTION, outcome);
            });

            for (Outcome outcome : Outcome.values()) {
                assertThat(metrics.count(Boundary.CONTINUATION, outcome)).isEqualTo(1);
                assertThat(metrics.count(Boundary.PROJECTION, outcome)).isEqualTo(1);
            }
            assertThat(appender.list).hasSize(Outcome.values().length * Boundary.values().length);
            assertThat(appender.list)
                    .allSatisfy(event -> assertThat(event.getFormattedMessage())
                            .startsWith("story67_safety boundary=")
                            .contains(" outcome=", " count=")
                            .doesNotContain("token", "owner", "user", "journey", "route", "symptom"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
