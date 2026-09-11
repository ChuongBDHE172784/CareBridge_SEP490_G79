package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
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

        // record() emits log.info, but CI pins com.carebridge.backend to WARN through
        // SPRING_APPLICATION_JSON and surefire reuses one JVM, so a Spring context booted
        // by an earlier class silences this one and hasSize() below sees an empty list.
        // INFO rather than DEBUG: the size assertion is exact, so a stray debug event from
        // this logger would turn a pass into a count mismatch. getLevel() is null while the
        // logger inherits, and restoring that null hands inheritance back to later tests.
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
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
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
