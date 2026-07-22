package com.carebridge.backend.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.flywaydb.core.api.MigrationVersion;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.stream.Stream;

/** Test-only support for the opt-in Release 1 database Gate 0 checks. */
final class DatabaseGate0Support {

    static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    static final Path MANIFEST_DIRECTORY = Path.of("target/gate0");

    static final String DUPLICATE_VERSION = "REPOSITORY_DUPLICATE_VERSION";
    static final String MALFORMED_FILENAME = "REPOSITORY_MALFORMED_FILENAME";
    static final String EMPTY_REPOSITORY = "REPOSITORY_MIGRATIONS_EMPTY";
    static final String CANDIDATE_NOT_EMPTY = "CANDIDATE_NOT_EMPTY";
    static final String RETAINED_INBOUND_FK = "RETAINED_INBOUND_FK";
    static final String DATABASE_OBJECT_REFERENCE = "DATABASE_OBJECT_REFERENCE";

    private static final Pattern MIGRATION_PATTERN =
            Pattern.compile("^V(.+)__(.+)\\.sql$");
    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final ObjectMapper JSON = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    static final Set<String> REMOVAL_CANDIDATES = Set.of(
            "commission_config",
            "commission_records",
            "consultation_disputes",
            "consultation_messages",
            "consultation_requests",
            "contribution_attachments",
            "expert_identity_verifications",
            "expert_reviews",
            "expert_verification_documents",
            "impact_assessment_ratings",
            "medical_contributions",
            "partner_expert_links",
            "partner_services",
            "payment_transactions",
            "refund_records",
            "settlement_records",
            "sponsored_campaigns");

    static final Set<String> BLOCKED_OR_DEPENDENT_TABLES = Set.of(
            "consultation_bookings",
            "consultation_price_bands",
            "consultation_sessions",
            "conversation_calls",
            "direct_conversations",
            "direct_messages",
            "expert_consultation_prices",
            "health_summaries",
            "partner_organizations");

    static final Set<String> RETAINED_RELEASE1_GUARDS = Set.of(
            "audit_logs",
            "care_facilities",
            "consent_grants",
            "emergency_sessions",
            "expert_availability",
            "health_records",
            "health_summaries",
            "imu_safety_events",
            "intake_sessions",
            "nearby_support_requests",
            "nearby_support_responses",
            "notification_records",
            "safety_monitoring_config",
            "security_events",
            "structured_intake_data");

    static final Set<String> KNOWN_CLEAN_BOOTSTRAP_ABSENT_CANDIDATES = Set.of(
            "contribution_attachments",
            "expert_identity_verifications",
            "medical_contributions");

    static final Map<String, Integer> EXPECTED_PUBLIC_TABLE_COUNTS = Map.of(
            "repositoryBeforeFinalCleanup", 113,
            "repositoryAfterFinalCleanup", 99,
            "liveAuditBaseline", 127,
            "liveAfterBatch1To5", 121,
            "liveAfterFinalCleanup", 104);

    private DatabaseGate0Support() {
    }

    static RepositoryManifest inspectRepository() {
        List<MigrationFile> migrations = new ArrayList<>();
        List<String> malformed = new ArrayList<>();
        Map<MigrationVersion, List<String>> pathsByVersion = new TreeMap<>();
        List<String> failures = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(MIGRATION_DIRECTORY)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> inspectMigration(path, migrations, malformed, failures));
        } catch (IOException exception) {
            failures.add("REPOSITORY_READ_ERROR:" + exception.getClass().getSimpleName());
        }

        if (migrations.isEmpty()) {
            failures.add(EMPTY_REPOSITORY);
        }

        migrations.sort(Comparator
                .comparing((MigrationFile migration) ->
                        MigrationVersion.fromVersion(migration.version()))
                .thenComparing(MigrationFile::script));

        for (MigrationFile migration : migrations) {
            pathsByVersion.computeIfAbsent(
                            MigrationVersion.fromVersion(migration.version()),
                            ignored -> new ArrayList<>())
                    .add(migration.path());
        }

        Map<String, List<String>> duplicateVersions = new TreeMap<>();
        pathsByVersion.forEach((version, paths) -> {
            if (paths.size() > 1) {
                List<String> sortedPaths = paths.stream().sorted().toList();
                String canonicalVersion = version.toString();
                duplicateVersions.put(canonicalVersion, sortedPaths);
                failures.add(DUPLICATE_VERSION + ":" + canonicalVersion + ":"
                        + String.join(",", sortedPaths));
            }
        });
        malformed.stream().sorted()
                .forEach(path -> failures.add(MALFORMED_FILENAME + ":" + path));

        return new RepositoryManifest(
                Instant.now().toString(),
                List.copyOf(migrations),
                List.copyOf(malformed),
                stableMap(duplicateVersions),
                finalCleanupPolicy(),
                List.copyOf(failures));
    }

    private static FinalCleanupPolicy finalCleanupPolicy() {
        return new FinalCleanupPolicy(
                REMOVAL_CANDIDATES.stream().sorted().toList(),
                BLOCKED_OR_DEPENDENT_TABLES.stream().sorted().toList(),
                RETAINED_RELEASE1_GUARDS.stream().sorted().toList(),
                KNOWN_CLEAN_BOOTSTRAP_ABSENT_CANDIDATES.stream().sorted().toList(),
                stableMap(EXPECTED_PUBLIC_TABLE_COUNTS));
    }

    private static void inspectMigration(
            Path path,
            List<MigrationFile> migrations,
            List<String> malformed,
            List<String> failures) {
        String script = path.getFileName().toString();
        String relativePath = normalizePath(path);
        Matcher matcher = MIGRATION_PATTERN.matcher(script);
        if (!matcher.matches()) {
            malformed.add(relativePath);
            return;
        }
        try {
            String canonicalVersion = canonicalVersion(matcher.group(1));
            migrations.add(new MigrationFile(
                    canonicalVersion,
                    matcher.group(2),
                    script,
                    relativePath,
                    sha256(Files.readAllBytes(path)),
                    flywayChecksum(path)));
        } catch (IllegalArgumentException exception) {
            malformed.add(relativePath);
        } catch (IOException exception) {
            failures.add("REPOSITORY_CHECKSUM_ERROR:" + relativePath + ":"
                    + exception.getClass().getSimpleName());
        }
    }

    static String canonicalVersion(String rawVersion) {
        return MigrationVersion.fromVersion(rawVersion).getVersion();
    }

    private static int flywayChecksum(Path path) throws IOException {
        CRC32 crc32 = new CRC32();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            boolean firstLine = true;
            while (line != null) {
                if (firstLine && !line.isEmpty() && line.charAt(0) == '\ufeff') {
                    line = line.substring(1);
                }
                crc32.update(line.getBytes(StandardCharsets.UTF_8));
                firstLine = false;
                line = reader.readLine();
            }
        }
        return (int) crc32.getValue();
    }

    static ExternalConfig externalConfigFromEnvironment() {
        Map<String, String> environment = System.getenv();
        return new ExternalConfig(
                environment.get("GATE0_DB_URL"),
                environment.get("GATE0_DB_USERNAME"),
                environment.get("GATE0_DB_PASSWORD"),
                defaultIfBlank(environment.get("GATE0_DB_SCHEMA"), "public"),
                defaultIfBlank(environment.get("GATE0_FLYWAY_TABLE"), "flyway_schema_history"),
                defaultIfBlank(environment.get("GATE0_ENVIRONMENT"), "unspecified"));
    }

    static DatabaseManifest auditExternal(ExternalConfig config) {
        RepositoryManifest repository = inspectRepository();
        List<String> failures = new ArrayList<>(repository.gateFailures());
        List<HistoryRow> history = new ArrayList<>();
        Map<String, CandidateState> candidateStates = new TreeMap<>();
        List<ForeignKeyDependency> inboundForeignKeys = new ArrayList<>();
        List<DatabaseObjectReference> objectReferences = new ArrayList<>();
        List<String> liveScriptsMissingFromRepository = new ArrayList<>();
        List<String> repositoryScriptsMissingFromHistory = new ArrayList<>();
        List<String> checksumMismatches = new ArrayList<>();
        Map<String, List<String>> duplicateHistoryVersions = new TreeMap<>();
        String endpointHash = null;
        String databaseName = null;
        String postgresVersion = null;
        String schemaFingerprint = null;
        boolean transactionReadOnly = false;
        boolean rollbackConfirmed = false;

        List<String> missingConfiguration = config.missingRequiredValues();
        if (!missingConfiguration.isEmpty()) {
            missingConfiguration.forEach(name -> failures.add("CONFIG_MISSING:" + name));
            return databaseManifest(
                    config, endpointHash, databaseName, postgresVersion, transactionReadOnly,
                    rollbackConfirmed, repository, history, duplicateHistoryVersions,
                    liveScriptsMissingFromRepository, repositoryScriptsMissingFromHistory,
                    checksumMismatches, schemaFingerprint, candidateStates, inboundForeignKeys,
                    objectReferences, failures);
        }
        if (!SAFE_IDENTIFIER.matcher(config.schema()).matches()) {
            failures.add("CONFIG_INVALID_SCHEMA_IDENTIFIER");
            return databaseManifest(
                    config, endpointHash, databaseName, postgresVersion, transactionReadOnly,
                    rollbackConfirmed, repository, history, duplicateHistoryVersions,
                    liveScriptsMissingFromRepository, repositoryScriptsMissingFromHistory,
                    checksumMismatches, schemaFingerprint, candidateStates, inboundForeignKeys,
                    objectReferences, failures);
        }
        if (!SAFE_IDENTIFIER.matcher(config.historyTable()).matches()) {
            failures.add("CONFIG_INVALID_FLYWAY_TABLE_IDENTIFIER");
            return databaseManifest(
                    config, endpointHash, databaseName, postgresVersion, transactionReadOnly,
                    rollbackConfirmed, repository, history, duplicateHistoryVersions,
                    liveScriptsMissingFromRepository, repositoryScriptsMissingFromHistory,
                    checksumMismatches, schemaFingerprint, candidateStates, inboundForeignKeys,
                    objectReferences, failures);
        }
        if (containsCredentialParameter(config.url())) {
            failures.add("CONFIG_CREDENTIAL_IN_URL");
            return databaseManifest(
                    config, endpointHash, databaseName, postgresVersion, transactionReadOnly,
                    rollbackConfirmed, repository, history, duplicateHistoryVersions,
                    liveScriptsMissingFromRepository, repositoryScriptsMissingFromHistory,
                    checksumMismatches, schemaFingerprint, candidateStates, inboundForeignKeys,
                    objectReferences, failures);
        }
        endpointHash = sha256(normalizedEndpoint(config.url()).getBytes(StandardCharsets.UTF_8));

        Connection connection = null;
        try {
            Properties properties = new Properties();
            properties.setProperty("user", config.username());
            properties.setProperty("password", config.password());
            properties.setProperty("ApplicationName", "CareBridgeDatabaseGate0");
            properties.setProperty("connectTimeout", "10");
            properties.setProperty("socketTimeout", "15");
            connection = DriverManager.getConnection(config.url(), properties);
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            configureReadOnlyTransaction(connection, config.schema());
            transactionReadOnly = isTransactionReadOnly(connection);
            if (!transactionReadOnly) {
                failures.add("TRANSACTION_NOT_READ_ONLY");
            }

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT current_database(), current_setting('server_version')")) {
                result.next();
                databaseName = result.getString(1);
                postgresVersion = result.getString(2);
            }

            history.addAll(readHistory(
                    connection, config.schema(), config.historyTable(), failures));
            reconcileHistory(
                    repository, history, duplicateHistoryVersions,
                    liveScriptsMissingFromRepository, repositoryScriptsMissingFromHistory,
                    checksumMismatches, failures);
            schemaFingerprint = schemaFingerprint(connection, config.schema());
            candidateStates.putAll(readCandidateStates(connection, config.schema(), failures));
            inboundForeignKeys.addAll(readInboundForeignKeys(connection, config.schema(), failures));
            objectReferences.addAll(readObjectReferences(connection, config.schema(), failures));
        } catch (SQLException exception) {
            failures.add("AUDIT_SQL_ERROR:" + safeSqlState(exception));
        } finally {
            if (connection != null) {
                try {
                    connection.rollback();
                    rollbackConfirmed = true;
                } catch (SQLException exception) {
                    failures.add("ROLLBACK_FAILED:" + safeSqlState(exception));
                }
                try {
                    connection.close();
                } catch (SQLException exception) {
                    failures.add("CONNECTION_CLOSE_FAILED:" + safeSqlState(exception));
                }
            }
        }
        if (!rollbackConfirmed) {
            failures.add("ROLLBACK_NOT_CONFIRMED");
        }

        return databaseManifest(
                config, endpointHash, databaseName, postgresVersion, transactionReadOnly,
                rollbackConfirmed, repository, history, duplicateHistoryVersions,
                liveScriptsMissingFromRepository, repositoryScriptsMissingFromHistory,
                checksumMismatches, schemaFingerprint, candidateStates, inboundForeignKeys,
                objectReferences, failures);
    }

    private static DatabaseManifest databaseManifest(
            ExternalConfig config,
            String endpointHash,
            String databaseName,
            String postgresVersion,
            boolean transactionReadOnly,
            boolean rollbackConfirmed,
            RepositoryManifest repository,
            List<HistoryRow> history,
            Map<String, List<String>> duplicateHistoryVersions,
            List<String> liveScriptsMissingFromRepository,
            List<String> repositoryScriptsMissingFromHistory,
            List<String> checksumMismatches,
            String schemaFingerprint,
            Map<String, CandidateState> candidateStates,
            List<ForeignKeyDependency> inboundForeignKeys,
            List<DatabaseObjectReference> objectReferences,
            List<String> failures) {
        List<String> stableFailures = failures.stream().distinct().sorted().toList();
        return new DatabaseManifest(
                Instant.now().toString(),
                stableFailures.isEmpty() ? "passed" : "failed",
                sanitizeLabel(config.environmentLabel()),
                endpointHash,
                databaseName,
                config.schema(),
                postgresVersion,
                transactionReadOnly,
                rollbackConfirmed,
                repository,
                List.copyOf(history),
                stableMap(duplicateHistoryVersions),
                List.copyOf(liveScriptsMissingFromRepository),
                List.copyOf(repositoryScriptsMissingFromHistory),
                List.copyOf(checksumMismatches),
                schemaFingerprint,
                stableMap(candidateStates),
                List.copyOf(inboundForeignKeys),
                List.copyOf(objectReferences),
                stableFailures);
    }

    private static void configureReadOnlyTransaction(Connection connection, String schema)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY");
            statement.execute("SET LOCAL statement_timeout = '5000ms'");
            statement.execute("SET LOCAL lock_timeout = '2000ms'");
            statement.execute("SET LOCAL search_path = " + quoteIdentifier(schema) + ", pg_catalog");
        }
    }

    private static boolean isTransactionReadOnly(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SHOW transaction_read_only")) {
            result.next();
            return "on".equalsIgnoreCase(result.getString(1));
        }
    }

    private static List<HistoryRow> readHistory(
            Connection connection,
            String schema,
            String historyTable,
            List<String> failures) throws SQLException {
        String qualifiedHistory = quoteIdentifier(schema) + "." + quoteIdentifier(historyTable);
        if (!relationExists(connection, schema, historyTable)) {
            failures.add("FLYWAY_HISTORY_MISSING");
            return List.of();
        }

        List<HistoryRow> history = new ArrayList<>();
        String sql = "SELECT installed_rank, version, description, type, script, checksum, "
                + "installed_on, execution_time, success FROM " + qualifiedHistory
                + " ORDER BY installed_rank";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                int installedRank = result.getInt("installed_rank");
                String type = result.getString("type");
                String script = result.getString("script");
                Timestamp installedOn = result.getTimestamp("installed_on");
                if (isBlank(type)) {
                    failures.add("FLYWAY_HISTORY_INVALID_ROW:" + installedRank + ":type");
                }
                if (isBlank(script)) {
                    failures.add("FLYWAY_HISTORY_INVALID_ROW:" + installedRank + ":script");
                }
                if (installedOn == null) {
                    failures.add("FLYWAY_HISTORY_INVALID_ROW:" + installedRank + ":installed_on");
                }
                Integer checksum = (Integer) result.getObject("checksum");
                HistoryRow row = new HistoryRow(
                        installedRank,
                        result.getString("version"),
                        result.getString("description"),
                        type,
                        script,
                        checksum,
                        installedOn == null ? null : installedOn.toInstant().toString(),
                        result.getInt("execution_time"),
                        result.getBoolean("success"));
                history.add(row);
                if (!row.success()) {
                    failures.add("FLYWAY_HISTORY_UNSUCCESSFUL:" + row.installedRank());
                }
            }
        }
        return history;
    }

    private static boolean relationExists(Connection connection, String schema, String relation)
            throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                      FROM pg_class relation_row
                      JOIN pg_namespace namespace_row
                        ON namespace_row.oid = relation_row.relnamespace
                     WHERE namespace_row.nspname = ? AND relation_row.relname = ?
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, relation);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static void reconcileHistory(
            RepositoryManifest repository,
            List<HistoryRow> history,
            Map<String, List<String>> duplicateHistoryVersions,
            List<String> liveScriptsMissingFromRepository,
            List<String> repositoryScriptsMissingFromHistory,
            List<String> checksumMismatches,
            List<String> failures) {
        Map<String, MigrationFile> repositoryByScript = new LinkedHashMap<>();
        repository.migrations().forEach(migration -> repositoryByScript.put(migration.script(), migration));
        Map<MigrationVersion, List<String>> scriptsByHistoryVersion = new TreeMap<>();
        Set<String> liveScripts = new TreeSet<>();

        for (HistoryRow row : history) {
            if (!isVersionedSql(row) || isBlank(row.script())) {
                continue;
            }
            MigrationVersion canonicalHistoryVersion;
            try {
                canonicalHistoryVersion = MigrationVersion.fromVersion(row.version());
            } catch (IllegalArgumentException exception) {
                failures.add("FLYWAY_HISTORY_INVALID_VERSION:" + row.installedRank());
                continue;
            }
            scriptsByHistoryVersion.computeIfAbsent(
                            canonicalHistoryVersion, ignored -> new ArrayList<>())
                    .add(row.script());
            liveScripts.add(row.script());
            MigrationFile repositoryMigration = repositoryByScript.get(row.script());
            if (repositoryMigration == null) {
                liveScriptsMissingFromRepository.add(row.script());
            } else if (row.checksum() == null) {
                failures.add("FLYWAY_CHECKSUM_MISSING:" + row.script());
            } else if (row.checksum().intValue() != repositoryMigration.flywayChecksum()) {
                checksumMismatches.add(row.script());
            }
        }

        scriptsByHistoryVersion.forEach((version, scripts) -> {
            if (scripts.size() > 1) {
                List<String> sortedScripts = scripts.stream().sorted().toList();
                duplicateHistoryVersions.put(version.toString(), sortedScripts);
                failures.add("FLYWAY_HISTORY_DUPLICATE_VERSION:" + version + ":"
                        + String.join(",", sortedScripts));
            }
        });
        repositoryByScript.keySet().stream()
                .filter(script -> !liveScripts.contains(script))
                .sorted()
                .forEach(repositoryScriptsMissingFromHistory::add);

        liveScriptsMissingFromRepository.stream().sorted()
                .forEach(script -> failures.add("LIVE_SCRIPT_MISSING_IN_REPOSITORY:" + script));
        repositoryScriptsMissingFromHistory.stream().sorted()
                .forEach(script -> failures.add("REPOSITORY_SCRIPT_MISSING_IN_HISTORY:" + script));
        checksumMismatches.stream().sorted()
                .forEach(script -> failures.add("FLYWAY_CHECKSUM_MISMATCH:" + script));
    }

    private static boolean isVersionedSql(HistoryRow row) {
        return row.version() != null && "SQL".equalsIgnoreCase(row.type());
    }

    private static String schemaFingerprint(Connection connection, String schema)
            throws SQLException {
        List<String> definitions = new ArrayList<>();

        collectRows(connection, """
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = ? AND table_type = 'BASE TABLE'
                 ORDER BY table_name
                """, schema, "TABLE", 1, definitions);
        collectRows(connection, """
                SELECT table_name, ordinal_position::text, column_name, data_type,
                       udt_schema, udt_name, is_nullable, column_default,
                       character_maximum_length::text, numeric_precision::text,
                       numeric_scale::text, datetime_precision::text,
                       collation_schema, collation_name, domain_schema, domain_name,
                       is_identity, identity_generation, is_generated, generation_expression
                  FROM information_schema.columns
                 WHERE table_schema = ?
                 ORDER BY table_name, ordinal_position
                """, schema, "COLUMN", 20, definitions);
        collectRows(connection, """
                SELECT rel.relname, con.conname, con.contype::text,
                       pg_get_constraintdef(con.oid, true)
                  FROM pg_constraint con
                  JOIN pg_class rel ON rel.oid = con.conrelid
                  JOIN pg_namespace ns ON ns.oid = rel.relnamespace
                 WHERE ns.nspname = ?
                 ORDER BY rel.relname, con.conname
                """, schema, "CONSTRAINT", 4, definitions);
        collectRows(connection, """
                SELECT tablename, indexname, indexdef
                  FROM pg_indexes
                 WHERE schemaname = ?
                 ORDER BY tablename, indexname
                """, schema, "INDEX", 3, definitions);

        return sha256(String.join("\n", definitions).getBytes(StandardCharsets.UTF_8));
    }

    private static void collectRows(
            Connection connection,
            String sql,
            String schema,
            String kind,
            int columnCount,
            List<String> destination) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    StringBuilder row = new StringBuilder().append(kind.length()).append(':').append(kind);
                    for (int index = 1; index <= columnCount; index++) {
                        String value = result.getString(index);
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
    }

    private static Map<String, CandidateState> readCandidateStates(
            Connection connection, String schema, List<String> failures) throws SQLException {
        Map<String, CandidateState> states = new TreeMap<>();
        for (String table : new TreeSet<>(REMOVAL_CANDIDATES)) {
            boolean present = relationExists(connection, schema, table);
            long count = 0;
            if (present) {
                String sql = "SELECT count(*) FROM " + quoteIdentifier(schema) + "." + quoteIdentifier(table);
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(sql)) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            states.put(table, new CandidateState(present, count));
            if (count != 0) {
                failures.add(CANDIDATE_NOT_EMPTY + ":" + table + ":" + count);
            }
        }
        return states;
    }

    private static List<ForeignKeyDependency> readInboundForeignKeys(
            Connection connection, String schema, List<String> failures) throws SQLException {
        List<ForeignKeyDependency> dependencies = new ArrayList<>();
        String sql = """
                SELECT source_ns.nspname, source.relname, target_ns.nspname, target.relname,
                       constraint_row.conname
                  FROM pg_constraint constraint_row
                  JOIN pg_class source ON source.oid = constraint_row.conrelid
                  JOIN pg_namespace source_ns ON source_ns.oid = source.relnamespace
                  JOIN pg_class target ON target.oid = constraint_row.confrelid
                  JOIN pg_namespace target_ns ON target_ns.oid = target.relnamespace
                 WHERE constraint_row.contype = 'f' AND target_ns.nspname = ?
                 ORDER BY source_ns.nspname, source.relname, constraint_row.conname
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String sourceSchema = result.getString(1);
                    String sourceTable = result.getString(2);
                    String targetSchema = result.getString(3);
                    String targetTable = result.getString(4);
                    if (!REMOVAL_CANDIDATES.contains(targetTable)) {
                        continue;
                    }
                    boolean sourceIsCandidate = schema.equals(sourceSchema)
                            && REMOVAL_CANDIDATES.contains(sourceTable);
                    if (!sourceIsCandidate) {
                        ForeignKeyDependency dependency = new ForeignKeyDependency(
                                sourceSchema, sourceTable, targetSchema, targetTable, result.getString(5));
                        dependencies.add(dependency);
                        failures.add(RETAINED_INBOUND_FK + ":" + sourceSchema + "." + sourceTable
                                + ":" + result.getString(5) + ":" + targetTable);
                    }
                }
            }
        }
        return dependencies;
    }

    private static List<DatabaseObjectReference> readObjectReferences(
            Connection connection, String schema, List<String> failures) throws SQLException {
        Set<DatabaseObjectReference> references = new LinkedHashSet<>();
        collectCatalogObjectReferences(connection, schema, references);
        collectFunctionTextReferences(connection, references);

        List<DatabaseObjectReference> sorted = references.stream()
                .sorted(Comparator.comparing(DatabaseObjectReference::candidateTable)
                        .thenComparing(DatabaseObjectReference::objectType)
                        .thenComparing(DatabaseObjectReference::objectName))
                .toList();
        sorted.forEach(reference -> failures.add(DATABASE_OBJECT_REFERENCE + ":"
                + reference.candidateTable() + ":" + reference.objectType() + ":"
                + reference.objectSchema() + "." + reference.objectName()));
        return sorted;
    }

    private static void collectCatalogObjectReferences(
            Connection connection,
            String schema,
            Set<DatabaseObjectReference> destination) throws SQLException {
        String sql = """
                SELECT CASE dependent.relkind
                           WHEN 'v' THEN 'VIEW'
                           WHEN 'm' THEN 'MATERIALIZED_VIEW'
                           ELSE 'RELATION'
                       END,
                       dependent_ns.nspname, dependent.relname, target.relname
                  FROM pg_depend dependency_row
                  JOIN pg_rewrite rewrite_row
                    ON dependency_row.classid = 'pg_rewrite'::regclass
                   AND dependency_row.objid = rewrite_row.oid
                  JOIN pg_class dependent ON dependent.oid = rewrite_row.ev_class
                  JOIN pg_namespace dependent_ns ON dependent_ns.oid = dependent.relnamespace
                  JOIN pg_class target ON target.oid = dependency_row.refobjid
                  JOIN pg_namespace target_ns ON target_ns.oid = target.relnamespace
                 WHERE target_ns.nspname = ?
                   AND dependent.oid <> target.oid
                   AND dependent_ns.nspname NOT IN ('pg_catalog', 'information_schema')
                   AND dependent_ns.nspname NOT LIKE 'pg_toast%'
                UNION ALL
                SELECT CASE procedure_row.prokind WHEN 'p' THEN 'PROCEDURE' ELSE 'FUNCTION' END,
                       procedure_ns.nspname,
                       procedure_row.proname || '(' || pg_get_function_identity_arguments(procedure_row.oid) || ')',
                       target.relname
                  FROM pg_depend dependency_row
                  JOIN pg_proc procedure_row
                    ON dependency_row.classid = 'pg_proc'::regclass
                   AND dependency_row.objid = procedure_row.oid
                  JOIN pg_namespace procedure_ns ON procedure_ns.oid = procedure_row.pronamespace
                  JOIN pg_class target ON target.oid = dependency_row.refobjid
                  JOIN pg_namespace target_ns ON target_ns.oid = target.relnamespace
                 WHERE target_ns.nspname = ?
                   AND procedure_ns.nspname NOT IN ('pg_catalog', 'information_schema')
                   AND procedure_ns.nspname NOT LIKE 'pg_toast%'
                UNION ALL
                SELECT 'TRIGGER', source_ns.nspname, trigger_row.tgname, target.relname
                  FROM pg_depend dependency_row
                  JOIN pg_trigger trigger_row
                    ON dependency_row.classid = 'pg_trigger'::regclass
                   AND dependency_row.objid = trigger_row.oid
                  JOIN pg_class source ON source.oid = trigger_row.tgrelid
                  JOIN pg_namespace source_ns ON source_ns.oid = source.relnamespace
                  JOIN pg_class target ON target.oid = dependency_row.refobjid
                  JOIN pg_namespace target_ns ON target_ns.oid = target.relnamespace
                 WHERE target_ns.nspname = ?
                   AND NOT trigger_row.tgisinternal
                   AND NOT (source_ns.nspname = target_ns.nspname AND source.relname = target.relname)
                 ORDER BY 4, 1, 2, 3
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, schema);
            statement.setString(3, schema);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String candidate = result.getString(4);
                    if (REMOVAL_CANDIDATES.contains(candidate)) {
                        destination.add(new DatabaseObjectReference(
                                result.getString(1), result.getString(2), result.getString(3), candidate));
                    }
                }
            }
        }
    }

    private static void collectFunctionTextReferences(
            Connection connection,
            Set<DatabaseObjectReference> destination) throws SQLException {
        String sql = """
                SELECT namespace_row.nspname,
                       procedure_row.proname || '(' || pg_get_function_identity_arguments(procedure_row.oid) || ')',
                       pg_get_functiondef(procedure_row.oid)
                  FROM pg_proc procedure_row
                  JOIN pg_namespace namespace_row ON namespace_row.oid = procedure_row.pronamespace
                 WHERE procedure_row.prokind IN ('f', 'p')
                   AND namespace_row.nspname NOT IN ('pg_catalog', 'information_schema')
                   AND namespace_row.nspname NOT LIKE 'pg_toast%'
                 ORDER BY namespace_row.nspname, procedure_row.proname, procedure_row.oid
                """;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                String definition = defaultIfBlank(result.getString(3), "").toLowerCase(Locale.ROOT);
                for (String candidate : REMOVAL_CANDIDATES) {
                    if (containsIdentifier(definition, candidate)) {
                        destination.add(new DatabaseObjectReference(
                                "FUNCTION_TEXT", result.getString(1), result.getString(2), candidate));
                    }
                }
            }
        }
    }

    private static boolean containsIdentifier(String definition, String identifier) {
        return Pattern.compile("(?<![a-z0-9_])" + Pattern.quote(identifier) + "(?![a-z0-9_])")
                .matcher(definition)
                .find();
    }

    static Path writeManifest(String filename, Object manifest) throws IOException {
        Files.createDirectories(MANIFEST_DIRECTORY);
        Path path = MANIFEST_DIRECTORY.resolve(sanitizeFilename(filename));
        Path temporary = Files.createTempFile(MANIFEST_DIRECTORY, path.getFileName().toString(), ".tmp");
        try {
            JSON.writeValue(temporary.toFile(), manifest);
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return path;
    }

    static String toJson(Object value) throws IOException {
        return JSON.writeValueAsString(value);
    }

    static String describe(Object manifest) {
        try {
            return toJson(manifest);
        } catch (IOException exception) {
            return manifest.toString();
        }
    }

    private static String quoteIdentifier(String value) {
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe database identifier");
        }
        return '"' + value + '"';
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String sanitizeFilename(String filename) {
        String safe = filename.replaceAll("[^A-Za-z0-9._-]", "-");
        return safe.endsWith(".json") ? safe : safe + ".json";
    }

    private static String sanitizeLabel(String value) {
        return defaultIfBlank(value, "unspecified").replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private static String normalizedEndpoint(String jdbcUrl) {
        String withoutQuery = jdbcUrl.split("[?#]", 2)[0];
        return withoutQuery.replaceFirst("/+$", "");
    }

    private static boolean containsCredentialParameter(String jdbcUrl) {
        String lower = defaultIfBlank(jdbcUrl, "").toLowerCase(Locale.ROOT);
        int authorityStart = lower.indexOf("//");
        if (authorityStart >= 0) {
            int authorityEnd = lower.indexOf('/', authorityStart + 2);
            String authority = authorityEnd < 0
                    ? lower.substring(authorityStart + 2)
                    : lower.substring(authorityStart + 2, authorityEnd);
            if (authority.contains("@")) {
                return true;
            }
        }
        int queryStart = lower.indexOf('?');
        if (queryStart < 0) {
            return false;
        }
        String query = lower.substring(queryStart + 1);
        return Pattern.compile("(?:^|[&;])(user|password)=").matcher(query).find();
    }

    private static <K, V> Map<K, V> stableMap(Map<K, V> source) {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String safeSqlState(SQLException exception) {
        return defaultIfBlank(exception.getSQLState(), "UNKNOWN");
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                result.append(String.format("%02x", current));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    record MigrationFile(
            String version,
            String description,
            String script,
            String path,
            String sha256,
            int flywayChecksum) {
    }

    record RepositoryManifest(
            String generatedAtUtc,
            List<MigrationFile> migrations,
            List<String> malformedFiles,
            Map<String, List<String>> duplicateVersions,
            FinalCleanupPolicy finalCleanupPolicy,
            List<String> gateFailures) {
    }

    record FinalCleanupPolicy(
            List<String> approvedRemovalCandidates,
            List<String> blockedOrDependentTables,
            List<String> retainedRelease1Guards,
            List<String> knownCleanBootstrapAbsentCandidates,
            Map<String, Integer> expectedPublicTableCounts) {
    }

    record ExternalConfig(
            String url,
            String username,
            String password,
            String schema,
            String historyTable,
            String environmentLabel) {

        List<String> missingRequiredValues() {
            List<String> missing = new ArrayList<>();
            if (isBlank(url)) {
                missing.add("GATE0_DB_URL");
            }
            if (isBlank(username)) {
                missing.add("GATE0_DB_USERNAME");
            }
            if (isBlank(password)) {
                missing.add("GATE0_DB_PASSWORD");
            }
            return missing;
        }
    }

    record HistoryRow(
            int installedRank,
            String version,
            String description,
            String type,
            String script,
            Integer checksum,
            String installedOnUtc,
            int executionTimeMs,
            boolean success) {
    }

    record CandidateState(boolean present, long exactRowCount) {
    }

    record ForeignKeyDependency(
            String sourceSchema,
            String sourceTable,
            String targetSchema,
            String targetTable,
            String constraintName) {
    }

    record DatabaseObjectReference(
            String objectType,
            String objectSchema,
            String objectName,
            String candidateTable) {
    }

    record DatabaseManifest(
            String generatedAtUtc,
            String status,
            String environment,
            String endpointSha256,
            String databaseName,
            String schema,
            String postgresVersion,
            boolean transactionReadOnly,
            boolean rollbackConfirmed,
            RepositoryManifest repository,
            List<HistoryRow> flywayHistory,
            Map<String, List<String>> duplicateHistoryVersions,
            List<String> liveScriptsMissingFromRepository,
            List<String> repositoryScriptsMissingFromHistory,
            List<String> checksumMismatches,
            String schemaFingerprintSha256,
            Map<String, CandidateState> candidateTables,
            List<ForeignKeyDependency> retainedInboundForeignKeys,
            List<DatabaseObjectReference> databaseObjectReferences,
            List<String> gateFailures) {
    }
}
