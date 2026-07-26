package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import db.callback.Postgresql18CanonicalCleanupCallback;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class Postgresql18CanonicalBaselineIntegrationTest {

    private static final Path MIGRATION_DIRECTORY =
            Path.of("src/main/resources/db/migration");
    private static final String BASELINE_SCRIPT =
            "B20260724111500__canonical_70_table_baseline.sql";
    private static final String LEGACY_CUTOVER_SCRIPT =
            "V20260724111500__remove_legacy_expert_profile_columns.sql";
    private static final String POST_BASELINE_COMPATIBILITY_SCRIPT =
            "V20260724120000__bootstrap_post_baseline_compatibility_bridges.sql";
    private static final Set<String> REFERENCE_TABLES = Set.of(
            "administrative_areas",
            "care_facilities",
            "care_item_templates",
            "community_topics",
            "knowledge_sources",
            "red_flag_rules",
            "specialties",
            "vaccination_schedules");
    private static final Pattern INSERT_TARGET =
            Pattern.compile("(?m)^INSERT INTO public\\.([a-z_]+) ");

    @Container
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine");

    @TempDir
    Path temporaryDirectory;

    @Test
    void baselineMatchesHistoricalChainAndSupportsEveryFlywayLifecycle() throws Exception {
        Path baseline = MIGRATION_DIRECTORY.resolve(BASELINE_SCRIPT);
        assertThat(baseline).as("canonical baseline migration").isRegularFile();
        assertBaselineContainsOnlyPortableCanonicalContent(baseline);

        /*
         * The deployed V-chain received the legacy-column removal out of order:
         * V20260724214200 completed the expert-profile cutover first, and the
         * later-arriving V20260724111500 then became a harmless IF EXISTS no-op.
         * Replaying every V script in version order would manufacture an
         * impossible history (the 24111500 script removes columns required by
         * 24214200).  Build the historical location in that same two-step order.
         */
        Path historicalLocation = copyMigrations(
                "historical", false, Set.of(LEGACY_CUTOVER_SCRIPT));
        Path baselineOnlyLocation = copyMigrations("baseline-only", true);

        String historicalUrl = createDatabase("carebridge_historical_chain");
        String baselineOnlyUrl = createDatabase("carebridge_baseline_only");
        String normalLocationUrl = createDatabase("carebridge_normal_location");

        long historicalMigrationCount;
        try (Stream<Path> paths = Files.walk(historicalLocation)) {
            historicalMigrationCount = paths.filter(Files::isRegularFile).count();
        }

        Flyway historicalFlyway = flyway(historicalUrl, filesystemLocation(historicalLocation));
        var historicalResult = historicalFlyway.migrate();
        assertThat(historicalResult.success).isTrue();
        assertThat(historicalResult.migrationsExecuted).isEqualTo(historicalMigrationCount);
        assertCanonicalTableCount(historicalUrl);
        DatabaseState historicalBeforeLateCutover = databaseState(historicalUrl);

        Files.copy(
                MIGRATION_DIRECTORY.resolve(LEGACY_CUTOVER_SCRIPT),
                historicalLocation.resolve(LEGACY_CUTOVER_SCRIPT),
                StandardCopyOption.REPLACE_EXISTING);
        var lateCutoverResult =
                flyway(historicalUrl, filesystemLocation(historicalLocation)).migrate();
        assertThat(lateCutoverResult.success).isTrue();
        assertThat(lateCutoverResult.migrationsExecuted).isOne();
        assertThat(successfulMigrationScripts(historicalUrl))
                .last().isEqualTo(LEGACY_CUTOVER_SCRIPT);
        DatabaseState historicalAfterLateCutover = databaseState(historicalUrl);
        assertThat(historicalAfterLateCutover.catalog())
                .as("late out-of-order legacy cutover must not alter canonical catalog")
                .isEqualTo(historicalBeforeLateCutover.catalog());
        assertThat(historicalAfterLateCutover.tableRowCounts())
                .as("late out-of-order legacy cutover must not alter row counts")
                .isEqualTo(historicalBeforeLateCutover.tableRowCounts());
        assertThat(historicalAfterLateCutover.referenceData())
                .as("late out-of-order legacy cutover must not alter reference data")
                .isEqualTo(historicalBeforeLateCutover.referenceData());

        Flyway baselineOnlyFlyway = flyway(baselineOnlyUrl, filesystemLocation(baselineOnlyLocation));
        var baselineResult = baselineOnlyFlyway.migrate();
        assertThat(baselineResult.success).isTrue();
        assertThat(baselineResult.migrationsExecuted).isOne();
        assertThat(successfulMigrationScripts(baselineOnlyUrl)).containsExactly(BASELINE_SCRIPT);
        assertCanonicalBaselineTableCount(baselineOnlyUrl);
        /*
         * B20260724111500 is an immutable 70-table cut-off, not the latest schema.
         * Later V migrations legitimately expand administrative-area reference data
         * and add the Release-1 extension tables.  The normal-location assertions
         * below prove that baseline + V chain converges to the historical V chain.
         */

        var repeatResult = baselineOnlyFlyway.migrate();
        assertThat(repeatResult.success).isTrue();
        assertThat(repeatResult.migrationsExecuted).isZero();

        Flyway normalLocationFlyway = flyway(normalLocationUrl, "classpath:db/migration");
        var normalLocationResult = normalLocationFlyway.migrate();
        assertThat(normalLocationResult.success).isTrue();
        assertThat(normalLocationResult.migrationsExecuted).isGreaterThan(1);
        assertThat(successfulMigrationScripts(normalLocationUrl))
                .contains(BASELINE_SCRIPT, POST_BASELINE_COMPATIBILITY_SCRIPT)
                .doesNotContain(LEGACY_CUTOVER_SCRIPT);
        assertCanonicalTableCount(normalLocationUrl);
        assertThat(catalogSnapshot(normalLocationUrl))
                .containsExactlyElementsOf(catalogSnapshot(historicalUrl));
        assertThat(referenceDataSnapshot(normalLocationUrl))
                .isEqualTo(referenceDataSnapshot(historicalUrl));
        validateJpaMappings(normalLocationUrl);

        var existingChainResult = flyway(historicalUrl, "classpath:db/migration").migrate();
        assertThat(existingChainResult.success).isTrue();
        assertThat(existingChainResult.migrationsExecuted).isZero();
        DatabaseState historicalAfterBaselineAttempt = databaseState(historicalUrl);
        assertThat(historicalAfterBaselineAttempt.catalog())
                .as("an existing versioned chain must ignore the baseline completely")
                .isEqualTo(historicalAfterLateCutover.catalog());
        assertThat(historicalAfterBaselineAttempt.tableRowCounts())
                .isEqualTo(historicalAfterLateCutover.tableRowCounts());
        assertThat(historicalAfterBaselineAttempt.referenceData())
                .isEqualTo(historicalAfterLateCutover.referenceData());
        assertThat(historicalAfterBaselineAttempt.flywayHistory())
                .isEqualTo(historicalAfterLateCutover.flywayHistory());
    }

    private void assertBaselineContainsOnlyPortableCanonicalContent(Path baseline) throws IOException {
        String sql = Files.readString(baseline);
        assertThat(sql)
                .doesNotContain("\\restrict", "\\unrestrict", "transaction_timeout")
                .doesNotContainPattern("(?m)^SET ")
                .doesNotContain("flyway_schema_history")
                .doesNotContain("CREATE TABLE public.users_old")
                .doesNotContain("INSERT INTO public.users ")
                .doesNotContain("INSERT INTO public.persons ")
                .doesNotContain("INSERT INTO public.auth_sessions ");

        Matcher matcher = INSERT_TARGET.matcher(sql);
        Set<String> insertTargets = new java.util.TreeSet<>();
        while (matcher.find()) {
            insertTargets.add(matcher.group(1));
        }
        assertThat(insertTargets).isSubsetOf(REFERENCE_TABLES);
    }

    private Path copyMigrations(String directoryName, boolean baselineOnly) throws IOException {
        return copyMigrations(directoryName, baselineOnly, Set.of());
    }

    private Path copyMigrations(
            String directoryName, boolean baselineOnly, Set<String> excludedScripts)
            throws IOException {
        Path destination = temporaryDirectory.resolve(directoryName);
        Files.createDirectories(destination);
        try (Stream<Path> paths = Files.walk(MIGRATION_DIRECTORY)) {
            for (Path source : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .filter(path -> !excludedScripts.contains(path.getFileName().toString()))
                    .filter(path -> baselineOnly
                            ? path.getFileName().toString().equals(BASELINE_SCRIPT)
                            : path.getFileName().toString().startsWith("V"))
                    .toList()) {
                Files.copy(source, destination.resolve(source.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return destination;
    }

    private Flyway flyway(String url, String location) {
        return Flyway.configure()
                .dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations(location)
                .callbacks(new Postgresql18CanonicalCleanupCallback())
                .outOfOrder(true)
                .load();
    }

    private static String filesystemLocation(Path directory) {
        return "filesystem:" + directory.toAbsolutePath().normalize();
    }

    private String createDatabase(String databaseName) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute("CREATE DATABASE " + quoteIdentifier(databaseName));
        }
        return replaceDatabaseName(postgres.getJdbcUrl(), databaseName);
    }

    private static String replaceDatabaseName(String jdbcUrl, String databaseName) {
        int queryStart = jdbcUrl.indexOf('?');
        String suffix = queryStart < 0 ? "" : jdbcUrl.substring(queryStart);
        String withoutQuery = queryStart < 0 ? jdbcUrl : jdbcUrl.substring(0, queryStart);
        int finalSlash = withoutQuery.lastIndexOf('/');
        return withoutQuery.substring(0, finalSlash + 1) + databaseName + suffix;
    }

    private static String quoteIdentifier(String identifier) {
        if (!identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe database identifier");
        }
        return '"' + identifier + '"';
    }

    private void assertCanonicalTableCount(String url) throws SQLException {
        try (Connection connection = connection(url);
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT count(*) FILTER (WHERE table_name <> 'flyway_schema_history'),
                               count(*) FILTER (WHERE table_name = 'flyway_schema_history'),
                               count(*)
                          FROM information_schema.tables
                         WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                        """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(72);
            assertThat(result.getInt(2)).isOne();
            assertThat(result.getInt(3)).isEqualTo(73);
        }
    }

    private void assertCanonicalBaselineTableCount(String url) throws SQLException {
        try (Connection connection = connection(url);
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT count(*) FILTER (WHERE table_name <> 'flyway_schema_history'),
                               count(*) FILTER (WHERE table_name = 'flyway_schema_history'),
                               count(*)
                          FROM information_schema.tables
                         WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                        """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(69);
            assertThat(result.getInt(2)).isOne();
            assertThat(result.getInt(3)).isEqualTo(70);
        }
    }

    private List<String> successfulMigrationScripts(String url) throws SQLException {
        return queryRows(url, """
                SELECT script
                  FROM flyway_schema_history
                 WHERE success AND version IS NOT NULL
                 ORDER BY installed_rank
                """);
    }

    private DatabaseState databaseState(String url) throws SQLException {
        return new DatabaseState(
                catalogSnapshot(url),
                queryRows(url, """
                        SELECT table_name || ':' || exact_count
                          FROM (
                              SELECT tablename AS table_name,
                                     (xpath('/row/c/text()', query_to_xml(
                                         format('select count(*) as c from %I.%I', schemaname, tablename),
                                         false, true, '')))[1]::text AS exact_count
                               FROM pg_tables
                               WHERE schemaname = 'public'
                                 AND tablename <> 'flyway_schema_history'
                          ) counts
                         ORDER BY table_name
                        """),
                queryRows(url, """
                        SELECT installed_rank::text || '|' || coalesce(version, '') || '|' ||
                               type || '|' || script || '|' || coalesce(checksum::text, '') || '|' || success
                          FROM flyway_schema_history
                         ORDER BY installed_rank
                        """),
                referenceDataSnapshot(url));
    }

    private List<String> catalogSnapshot(String url) throws SQLException {
        List<String> snapshot = new ArrayList<>();
        collectRows(url, "TABLE", """
                SELECT relation.relkind::text, relation.relname, access_method.amname,
                       coalesce(array_to_string(relation.reloptions, ','), '')
                  FROM pg_class relation
                  JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                  LEFT JOIN pg_am access_method ON access_method.oid = relation.relam
                 WHERE namespace.nspname = 'public'
                   AND relation.relkind IN ('r', 'p')
                   AND relation.relname <> 'flyway_schema_history'
                 ORDER BY relation.relname
                """, snapshot);
        collectRows(url, "COLUMN", """
                SELECT table_name, column_name, data_type,
                       udt_schema, udt_name, is_nullable, column_default,
                       character_maximum_length::text, numeric_precision::text,
                       numeric_scale::text, datetime_precision::text,
                       collation_schema, collation_name, domain_schema, domain_name,
                       is_identity, identity_generation, is_generated, generation_expression
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                 ORDER BY table_name, column_name
                """, snapshot);
        collectRows(url, "CONSTRAINT", """
                SELECT relation.relname, constraint_row.conname, constraint_row.contype::text,
                       pg_get_constraintdef(constraint_row.oid, true)
                  FROM pg_constraint constraint_row
                  JOIN pg_class relation ON relation.oid = constraint_row.conrelid
                  JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                 WHERE namespace.nspname = 'public'
                   AND relation.relname <> 'flyway_schema_history'
                 ORDER BY relation.relname, constraint_row.conname
                """, snapshot);
        collectRows(url, "INDEX", """
                SELECT table_row.relname, index_row.relname, pg_get_indexdef(index_row.oid)
                  FROM pg_index index_catalog
                  JOIN pg_class index_row ON index_row.oid = index_catalog.indexrelid
                  JOIN pg_class table_row ON table_row.oid = index_catalog.indrelid
                  JOIN pg_namespace namespace ON namespace.oid = table_row.relnamespace
                 WHERE namespace.nspname = 'public'
                   AND table_row.relname <> 'flyway_schema_history'
                 ORDER BY table_row.relname, index_row.relname
                """, snapshot);
        collectRows(url, "SEQUENCE", """
                SELECT sequence_row.relname, sequence_catalog.seqstart::text,
                       sequence_catalog.seqincrement::text, sequence_catalog.seqmin::text,
                       sequence_catalog.seqmax::text, sequence_catalog.seqcache::text,
                       sequence_catalog.seqcycle::text
                  FROM pg_sequence sequence_catalog
                  JOIN pg_class sequence_row ON sequence_row.oid = sequence_catalog.seqrelid
                  JOIN pg_namespace namespace ON namespace.oid = sequence_row.relnamespace
                 WHERE namespace.nspname = 'public'
                 ORDER BY sequence_row.relname
                """, snapshot);
        collectRows(url, "FUNCTION", """
                SELECT procedure_row.proname,
                       pg_get_function_identity_arguments(procedure_row.oid),
                       pg_get_functiondef(procedure_row.oid)
                  FROM pg_proc procedure_row
                  JOIN pg_namespace namespace ON namespace.oid = procedure_row.pronamespace
                 WHERE namespace.nspname = 'public'
                 ORDER BY procedure_row.proname, procedure_row.oid
                """, snapshot);
        collectRows(url, "TRIGGER", """
                SELECT relation.relname, trigger_row.tgname,
                       pg_get_triggerdef(trigger_row.oid, true), trigger_row.tgenabled::text
                  FROM pg_trigger trigger_row
                  JOIN pg_class relation ON relation.oid = trigger_row.tgrelid
                  JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                 WHERE namespace.nspname = 'public' AND NOT trigger_row.tgisinternal
                 ORDER BY relation.relname, trigger_row.tgname
                """, snapshot);
        return List.copyOf(snapshot);
    }

    private Map<String, List<String>> referenceDataSnapshot(String url) throws SQLException {
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("administrative_areas", """
                SELECT jsonb_build_object('area_type', area.area_type, 'code', area.code,
                           'name', area.name, 'legacy_code', area.legacy_code,
                           'parent_code', parent.code)::text
                  FROM administrative_areas area
                  LEFT JOIN administrative_areas parent
                    ON parent.administrative_area_id = area.parent_area_id
                 ORDER BY 1
                """);
        queries.put("care_facilities", """
                SELECT (to_jsonb(facility) - ARRAY[
                           'facility_id', 'partner_id', 'province_id', 'district_id',
                           'administrative_area_id', 'created_at', 'updated_at'
                       ] || jsonb_build_object('administrative_area_code', area.code))::text
                  FROM care_facilities facility
                  LEFT JOIN administrative_areas area
                    ON area.administrative_area_id = facility.administrative_area_id
                 ORDER BY 1
                """);
        queries.put("care_item_templates", """
                SELECT (to_jsonb(template) - ARRAY[
                           'template_id', 'parent_template_id', 'created_at', 'updated_at',
                           'created_by', 'configured_by'
                       ])::text
                  FROM care_item_templates template
                 ORDER BY 1
                """);
        queries.put("community_topics", """
                SELECT (to_jsonb(topic) - ARRAY[
                           'id', 'parent_id', 'created_at', 'updated_at', 'created_by'
                       ] || jsonb_build_object('parent_slug', parent.slug))::text
                  FROM community_topics topic
                  LEFT JOIN community_topics parent ON parent.id = topic.parent_id
                 ORDER BY 1
                """);
        queries.put("knowledge_sources", """
                SELECT (to_jsonb(source_row) - ARRAY[
                           'knowledge_source_id', 'added_by', 'reviewed_by', 'reviewed_at',
                           'created_at', 'updated_at'
                       ])::text
                  FROM knowledge_sources source_row
                 ORDER BY 1
                """);
        queries.put("red_flag_rules", """
                SELECT (to_jsonb(rule_row) - ARRAY[
                           'id', 'created_by', 'updated_by', 'created_at', 'updated_at'
                       ])::text
                  FROM red_flag_rules rule_row
                 ORDER BY 1
                """);
        queries.put("specialties", """
                SELECT (to_jsonb(specialty) - ARRAY['specialty_id', 'created_at'])::text
                  FROM specialties specialty
                 ORDER BY 1
                """);
        queries.put("vaccination_schedules", """
                SELECT (to_jsonb(schedule) - ARRAY[
                           'vaccination_schedule_id', 'created_at', 'schedule_version'
                       ])::text
                  FROM vaccination_schedules schedule
                 ORDER BY 1
                """);

        Map<String, List<String>> snapshot = new TreeMap<>();
        for (Map.Entry<String, String> entry : queries.entrySet()) {
            snapshot.put(entry.getKey(), queryRows(url, entry.getValue()));
        }
        return java.util.Collections.unmodifiableMap(snapshot);
    }

    private void collectRows(String url, String kind, String sql, List<String> destination)
            throws SQLException {
        try (Connection connection = connection(url);
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            int columns = result.getMetaData().getColumnCount();
            while (result.next()) {
                StringBuilder row = new StringBuilder(kind);
                for (int index = 1; index <= columns; index++) {
                    String value = result.getString(index);
                    if (value != null && ("CONSTRAINT".equals(kind) || "INDEX".equals(kind))) {
                        value = normalizeDeparsedSql(value);
                    }
                    row.append('|');
                    if (value == null) {
                        row.append("-1:");
                    } else {
                        row.append(value.length()).append(':').append(value);
                    }
                }
                destination.add(row.toString());
            }
        }
    }

    private static String normalizeDeparsedSql(String value) {
        return value
                .replace("::character varying", "")
                .replace("::text[]", "")
                .replace("::text", "")
                .replaceAll("\\('([^']*)'\\)", "'$1'")
                .replaceAll("ANY \\(\\(ARRAY\\[([^]]*)]\\)\\)", "ANY (ARRAY[$1])")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> queryRows(String url, String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection connection = connection(url);
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(result.getString(1));
            }
        }
        return List.copyOf(rows);
    }

    private Connection connection(String url) throws SQLException {
        return DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
    }

    private void validateJpaMappings(String url) {
        var dataSource = new DriverManagerDataSource(
                url, postgres.getUsername(), postgres.getPassword());
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.carebridge.backend");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        var properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        factory.setJpaProperties(properties);

        try {
            factory.afterPropertiesSet();
            assertThat(factory.getObject().isOpen()).isTrue();
        } finally {
            factory.destroy();
        }
    }

    private record DatabaseState(
            List<String> catalog,
            List<String> tableRowCounts,
            List<String> flywayHistory,
            Map<String, List<String>> referenceData) {
    }
}
