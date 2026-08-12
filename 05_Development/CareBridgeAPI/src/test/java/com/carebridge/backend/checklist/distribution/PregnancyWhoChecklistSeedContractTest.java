package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Contract evidence for the detailed Markdown-only pregnancy checklist import. */
class PregnancyWhoChecklistSeedContractTest {

    private static final String SOURCE_NAME = "Checklist giai đoạn đang mai thai.md";
    private static final Path SEED = Path.of(
            "src/main/resources/db/migration/V20260812100000__seed_pregnancy_who_checklist_content.sql");
    private static final Pattern PLAN = Pattern.compile("^### Plan (\\d+).*$");
    private static final Pattern SECTION = Pattern.compile(
            "^- \\[ \\] \\*\\*(Chung|Theo dõi định kỳ hàng tuần).*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ANCESTOR = Pattern.compile(
            "^\\s+- \\[ \\] \\*\\*(.+)\\*\\*:\\s*$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern LEAF = Pattern.compile(
            "^(\\s+)- \\[ \\] (.+)$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern SQL_ITEM = Pattern.compile(
            "\\('f820[^']+'::uuid,\\s*'f810[^']+'::uuid,\\s*(\\d+),\\s*'([^']+)',"
                    + "\\s*(\\d+),\\s*'([^']+)',\\s*(NULL|'[^']*'),\\s*'((?:[^']|'')*)'\\),?",
            Pattern.UNICODE_CHARACTER_CLASS);

    @Test
    void detailedMarkdownMapsBijectionToTargetlessCadenceItems() throws Exception {
        String sourceRaw = Files.readString(resolveSource(), StandardCharsets.UTF_8);
        String source = normalize(sourceRaw);
        String sql = read(SEED);
        List<SourceLeaf> sourceLeaves = parseSource(source);
        Map<String, SeedItem> seedItems = parseSeedItems(sql);

        assertThat(sha256(sourceRaw)).isEqualTo(
                "d68edc9f3d2d595876f8b1f9d3332e6fcfa55986535b52d8ae01bd25faafe133");
        assertThat(sourceLeaves).hasSize(62);
        assertThat(seedItems).hasSize(62);
        assertThat(sourceLeaves.stream().map(SourceLeaf::locator).distinct()).hasSize(62);
        assertThat(seedItems.keySet()).containsExactlyInAnyOrderElementsOf(
                sourceLeaves.stream().map(SourceLeaf::locator).toList());

        for (SourceLeaf sourceLeaf : sourceLeaves) {
            SeedItem seedItem = seedItems.get(sourceLeaf.locator());
            assertThat(seedItem.plan()).isEqualTo(sourceLeaf.plan());
            assertThat(seedItem.section()).isEqualTo(sourceLeaf.section());
            assertThat(seedItem.ancestor()).isEqualTo(sourceLeaf.ancestor());
            assertThat(seedItem.text()).isEqualTo(sourceLeaf.renderedText());
        }
        assertThat(sql).contains(
                "'sourceArtifactSha256', 'D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133'");
        assertThat(sql).contains(
                "'importBatchId', 'PREGNANCY_WHO_20260812_V1'",
                "'importCorrelationId', '8a2dfd42-8ce0-5de6-9d30-0fe3e50210c5'",
                "'sourceRelationship', 'PRODUCT_AUTHORED_WHO_INFORMED'",
                "'translationProvenance', 'NOT_A_VERBATIM_TRANSLATION'",
                "'cadenceReviewStatus', 'PENDING'",
                "'cadenceReviewerUserId', NULL",
                "'copyReviewerUserId', NULL",
                "'contentOwnerApprovedAt', NULL",
                "'copyReviewedAt', NULL",
                "'sourceVersionOrPublicationDate', NULL",
                "'sourceUrl', NULL",
                "'priorityNarrative', CASE WHEN r.plan_no = 1",
                "'priorityNarrativeMode', CASE WHEN r.plan_no = 1");

        assertThat(count(sourceLeaves, 1, "COMMON")).isEqualTo(5);
        assertThat(count(sourceLeaves, 1, "WEEKLY")).isEqualTo(3);
        assertThat(count(sourceLeaves, 2, "COMMON")).isEqualTo(2);
        assertThat(count(sourceLeaves, 2, "WEEKLY")).isEqualTo(4);
        assertThat(count(sourceLeaves, 3, "COMMON")).isEqualTo(4);
        assertThat(count(sourceLeaves, 3, "WEEKLY")).isEqualTo(4);
        assertThat(count(sourceLeaves, 4, "COMMON")).isEqualTo(4);
        assertThat(count(sourceLeaves, 4, "WEEKLY")).isEqualTo(5);
        assertThat(count(sourceLeaves, 5, "COMMON")).isEqualTo(1);
        assertThat(count(sourceLeaves, 5, "WEEKLY")).isEqualTo(5);
        assertThat(count(sourceLeaves, 6, "COMMON")).isEqualTo(3);
        assertThat(count(sourceLeaves, 6, "WEEKLY")).isEqualTo(5);
        assertThat(count(sourceLeaves, 7, "COMMON")).isEqualTo(4);
        assertThat(count(sourceLeaves, 7, "WEEKLY")).isEqualTo(5);
        assertThat(count(sourceLeaves, 8, "COMMON")).isEqualTo(3);
        assertThat(count(sourceLeaves, 8, "WEEKLY")).isEqualTo(5);
    }

    @Test
    void seedPinsManifestAndCadenceBoundariesWithoutCreatingPregnancyDailyRoot() throws Exception {
        String source = normalize(Files.readString(resolveSource(), StandardCharsets.UTF_8));
        String sql = read(SEED);
        List<SourceLeaf> leaves = parseSource(source);
        String manifestHash = sha256(canonicalManifest(leaves));

        assertThat(sql).contains(
                "'sourceArtifactSha256', 'D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133'",
                "'renderedManifestSchema', 'CHECKLIST_COPY_MANIFEST_V1'",
                "'renderedManifestHash', '" + manifestHash + "'",
                "'normalizerId', 'PREGNANCY_CHECKLIST_NORMALIZER_V1'",
                "'originalLeafSha256'",
                "'renderedTextSha256'",
                "'sourceSliceEncoding', 'UTF8_NFC_LF_NO_EOL'");

        String rootData = sql.substring(sql.indexOf("WITH root_data"), sql.indexOf("), root_payload"));
        assertThat(rootData).contains(
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 0, 19, 5",
                "'EACH_WEEK', 'FIXED_OFFSET', 0, 19, 3",
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 20, 24, 2",
                "'EACH_WEEK', 'FIXED_OFFSET', 20, 24, 4",
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 25, 28, 4",
                "'EACH_WEEK', 'FIXED_OFFSET', 25, 28, 4",
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 29, 32, 4",
                "'EACH_WEEK', 'FIXED_OFFSET', 29, 32, 5",
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 33, 34, 1",
                "'EACH_WEEK', 'FIXED_OFFSET', 33, 34, 5",
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 35, 36, 3",
                "'EACH_WEEK', 'FIXED_OFFSET', 35, 36, 5",
                "'ONCE_PER_WINDOW', 'FIXED_OFFSET', 37, 38, 4",
                "'EACH_WEEK', 'FIXED_OFFSET', 37, 38, 5",
                "'ONCE_PER_WINDOW', 'STAGE_EXIT', 39, 2147483647, 3",
                "'EACH_WEEK', 'STAGE_EXIT', 39, 2147483647, 5");
        assertThat(rootData).doesNotContain("'DAILY'");
        assertThat(rootData).doesNotContain("0, 11");
        assertThat(sql).contains(
                "target_subject, is_required, checklist_contract_version",
                "NULL, NULL, 2,");
        assertThat(sql).contains("root_count <> 16", "item_count <> 62", "daily_count <> 0");
    }

    @Test
    void forwardMigrationKeepsPendingPregnancyCopyNonDistributableAtDatabaseBoundary() throws Exception {
        String migration = read(Path.of(
                "src/main/resources/db/migration/V20260812130000__gate_pregnancy_v2_provenance_activation.sql"));
        assertThat(migration).contains(
                "checklist_pregnancy_v2_provenance_activation_ck",
                "checklist_metadata_jsonb ->> 'schema' = 'CHECKLIST_METADATA_V1'",
                "checklist_metadata_jsonb ->> 'provenanceStatus' = 'SIGNED_OFF'",
                "distribution_enabled = false",
                "content_status <> 'APPROVED'",
                "VALIDATE CONSTRAINT checklist_pregnancy_v2_provenance_activation_ck");
    }

    private static int count(List<SourceLeaf> leaves, int plan, String section) {
        return (int) leaves.stream()
                .filter(leaf -> leaf.plan() == plan && leaf.section().equals(section))
                .count();
    }

    private static List<SourceLeaf> parseSource(String source) {
        int plan = 0;
        String section = null;
        String ancestor = null;
        Map<String, Integer> topOrder = new HashMap<>();
        Map<String, Integer> nestedOrder = new HashMap<>();
        List<SourceLeaf> leaves = new ArrayList<>();
        for (String line : source.split("\\n", -1)) {
            Matcher planMatcher = PLAN.matcher(line);
            if (planMatcher.matches()) {
                plan = Integer.parseInt(planMatcher.group(1));
                section = null;
                ancestor = null;
                continue;
            }
            Matcher sectionMatcher = SECTION.matcher(line);
            if (sectionMatcher.matches()) {
                section = sectionMatcher.group(1).equals("Chung") ? "COMMON" : "WEEKLY";
                ancestor = null;
                topOrder.put(plan + "/" + section, 0);
                continue;
            }
            Matcher ancestorMatcher = ANCESTOR.matcher(line);
            if (ancestorMatcher.matches()) {
                ancestor = ancestorMatcher.group(1);
                nestedOrder.put(plan + "/" + section + "/" + ancestor, 0);
                continue;
            }
            Matcher leafMatcher = LEAF.matcher(line);
            if (!leafMatcher.matches() || section == null) {
                continue;
            }
            String text = leafMatcher.group(2).replace("**", "");
            String key = plan + "/" + section;
            String locator;
            String rendered;
            if (ancestor != null) {
                String nestedKey = key + "/" + ancestor;
                int order = nestedOrder.merge(nestedKey, 1, Integer::sum);
                locator = String.format("P%02d/%s/RH_NEGATIVE/%02d", plan, section, order);
                rendered = ancestor + ": " + text;
            } else {
                int order = topOrder.merge(key, 1, Integer::sum);
                locator = String.format("P%02d/%s/%02d", plan, section, order);
                rendered = text;
            }
            String normalizedSourceSlice = leafMatcher.group(1) + "- [ ] " + leafMatcher.group(2);
            leaves.add(new SourceLeaf(plan, section, locator, ancestor, rendered, normalizedSourceSlice));
        }
        return leaves;
    }

    private static Map<String, SeedItem> parseSeedItems(String sql) {
        Map<String, SeedItem> items = new LinkedHashMap<>();
        Matcher matcher = SQL_ITEM.matcher(sql);
        while (matcher.find()) {
            String ancestor = matcher.group(5);
            if (!"NULL".equals(ancestor)) {
                ancestor = ancestor.substring(1, ancestor.length() - 1);
            } else {
                ancestor = null;
            }
            items.put(matcher.group(4), new SeedItem(
                    Integer.parseInt(matcher.group(1)), matcher.group(2), matcher.group(4),
                    ancestor, matcher.group(6).replace("''", "'")));
        }
        return items;
    }

    private static String canonicalManifest(List<SourceLeaf> leaves) {
        List<SourceLeaf> sorted = leaves.stream()
                .sorted(Comparator.comparing(SourceLeaf::locator))
                .toList();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            SourceLeaf leaf = sorted.get(i);
            json.append("{\"leafLocator\":\"").append(leaf.locator())
                    .append("\",\"renderedTextHash\":\"")
                    .append(sha256(leaf.renderedText())).append("\"}");
        }
        return json.append(']').toString();
    }

    private static String read(Path path) throws Exception {
        return normalize(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static String normalize(String content) {
        return content
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static Path resolveSource() {
        Path cwdRelative = Path.of("08_References", SOURCE_NAME);
        if (Files.exists(cwdRelative)) {
            return cwdRelative;
        }
        return Path.of("../../08_References", SOURCE_NAME);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte valueByte : digest) {
                result.append(String.format("%02x", valueByte));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record SourceLeaf(
            int plan, String section, String locator, String ancestor, String renderedText,
            String normalizedSourceSlice) {
    }

    private record SeedItem(int plan, String section, String locator, String ancestor, String text) {
    }
}
