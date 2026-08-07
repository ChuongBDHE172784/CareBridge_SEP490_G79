package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Executes the operator readiness script against a real database.
 *
 * <p>The script is the artefact the release owner runs by hand before the R9
 * cutover and the R11b final delta, so "it looked right" is not evidence. This
 * test proves it parses, runs, and classifies the gates correctly — including the
 * two that are currently expected to fail, which is the whole reason those two
 * waves are still open.
 */
class ConsolidationReadinessScriptEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Path scriptPath() {
        // The test runs from CareBridgeAPI; the script lives with the other
        // consolidation runbook SQL under Deployment.
        return Paths.get("..", "Deployment", "database", "consolidation", "04_readiness_check.sql")
                .toAbsolutePath()
                .normalize();
    }

    private List<Map<String, Object>> runScript() throws Exception {
        String sql = Files.readString(scriptPath(), StandardCharsets.UTF_8);

        // Everything up to the trailing SELECTs is setup; execute it as one script,
        // then read the report the same way an operator would.
        int reportSelect = sql.indexOf("SELECT seq, wave, gate");
        assertThat(reportSelect).as("readiness script must end with its report query").isPositive();

        jdbcTemplate.execute(sql.substring(0, reportSelect));
        return jdbcTemplate.queryForList(
                "SELECT seq, wave, gate, expected, actual, result FROM consolidation_readiness ORDER BY seq");
    }

    @Test
    void readinessScriptRunsAndReportsEveryGate() throws Exception {
        List<Map<String, Object>> report = runScript();

        assertThat(report).isNotEmpty();
        assertThat(report).allSatisfy(row ->
                assertThat(row.get("result"))
                        .as("gate %s must be classified", row.get("gate"))
                        .isIn("PASS", "FAIL", "INFO", "SKIPPED"));

        assertThat(report).anySatisfy(row -> assertThat(row.get("wave")).isEqualTo("R11b"));
        assertThat(report).anySatisfy(row -> assertThat(row.get("wave")).isEqualTo("R9"));
    }

    @Test
    void queueGatesPassOnAQuiescedDatabase() throws Exception {
        List<Map<String, Object>> report = runScript();

        // Nothing is running here, so no job may be stuck in PROCESSING, and the
        // expand backfill must already cover every source row.
        // After the persistence contract the source queues no longer exist, so the
        // script reports SKIPPED. Both readings are healthy; a FAIL is not.
        assertThat(resultOf(report, 1)).isIn("PASS", "SKIPPED");
        assertThat(resultOf(report, 2)).isIn("PASS", "SKIPPED");
        assertThat(resultOf(report, 3)).isIn("PASS", "SKIPPED");
        assertThat(resultOf(report, 4)).isIn("PASS", "SKIPPED");
        assertThat(resultOf(report, 5)).isIn("PASS", "SKIPPED");
    }

    @Test
    void checklistBackfillIsNowComplete() throws Exception {
        List<Map<String, Object>> report = runScript();

        // V20260806175000 retired the two demo rows the canonical backfill had left
        // behind, so every legacy checklist row now has a v2 counterpart. This is the
        // gate that made removing the legacy read merge in UserChecklistItemController
        // safe; if it ever regresses, that merge is load-bearing again.
        // SKIPPED once preparation_checklist_items is dropped — the strongest possible
        // outcome, since there is no longer a legacy row that could go unmapped.
        assertThat(resultOf(report, 7))
                .as("R9 unmapped-legacy gate")
                .isIn("PASS", "SKIPPED");

        // No blocking-id row is emitted once the gate passes.
        assertThat(report).noneSatisfy(row ->
                assertThat(row.get("seq")).isEqualTo(8));

        assertThat(resultOf(report, 11)).isEqualTo("PASS");
    }

    @Test
    void verdictIsClearOnAFullyReconciledDatabase() throws Exception {
        runScript();

        String verdict = jdbcTemplate.queryForObject("""
                SELECT CASE
                         WHEN count(*) FILTER (WHERE result = 'FAIL') > 0
                              THEN 'BLOCKED — ' || count(*) FILTER (WHERE result = 'FAIL')::text
                                   || ' gate(s) failing'
                         ELSE 'ALL GATES PASS — R9 cutover and R11b final delta may proceed'
                       END
                  FROM consolidation_readiness
                """, String.class);

        assertThat(verdict).doesNotStartWith("BLOCKED");
    }

    private static String resultOf(List<Map<String, Object>> report, int seq) {
        return report.stream()
                .filter(row -> Integer.valueOf(seq).equals(row.get("seq")))
                .map(row -> String.valueOf(row.get("result")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no readiness row with seq " + seq));
    }
}
