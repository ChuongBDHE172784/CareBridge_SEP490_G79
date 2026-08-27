package com.carebridge.backend.checklist.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ChecklistRetentionDeploymentRunnerContractTest {

    private static final Path DEPLOYMENT_ROOT = Path.of("..", "Deployment", "database");

    @Test
    void runnerPinsChecksumStopsOnPsqlFailureAndVerifiesAfterCommit() throws Exception {
        String runner = Files.readString(
                DEPLOYMENT_ROOT.resolve("Invoke-ChecklistRetentionFinalizer.ps1"));
        Path finalizer = DEPLOYMENT_ROOT.resolve(
                "finalizers/V20260729150001__finalize_checklist_retention_security.sql");
        String actualChecksum = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(finalizer)));
        var checksumMatcher = Pattern.compile(
                "\\$expectedSha256 = '([0-9a-f]{64})'").matcher(runner);

        assertThat(checksumMatcher.find()).isTrue();
        assertThat(checksumMatcher.group(1)).isEqualTo(actualChecksum);
        assertThat(runner)
                .contains("$finalizerVersion = '20260729150001'")
                .contains("--set=ON_ERROR_STOP=on")
                .contains("if ($LASTEXITCODE -ne 0)")
                .contains("CHECKLIST_RETENTION_FINALIZER_FAILED")
                .contains("CHECKLIST_RETENTION_POST_COMMIT_VERIFICATION_FAILED")
                .contains("VERIFIED:$finalizerVersion")
                .contains("CHECKLIST_RETENTION_FINALIZER_COMPLETE:${finalizerVersion}:$actualSha256");
    }

    @Test
    void runnerUsesHardenedNonInteractivePsqlAndOnlyTheDedicatedCredential() throws Exception {
        String runner = Files.readString(
                DEPLOYMENT_ROOT.resolve("Invoke-ChecklistRetentionFinalizer.ps1"));

        assertThat(runner)
                .contains("$psqlSafetyArguments = @(")
                .contains("'--no-psqlrc'")
                .contains("'--no-password'")
                .contains("'--set=ON_ERROR_STOP=on'")
                .contains("& $PsqlPath @psqlSafetyArguments $databaseArgument '--file' $resolvedFinalizer")
                .contains("& $PsqlPath @psqlSafetyArguments $databaseArgument '--tuples-only'")
                .contains("if ([string]::IsNullOrWhiteSpace($ProvisionerPassword)) {")
                .contains("Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue")
                .contains("$env:PGPASSWORD = $ProvisionerPassword")
                .contains("$env:PGPASSWORD = $previousPassword");
    }

    @Test
    void runnerRejectsOptionInjectionAndUriPasswordsAndUsesExplicitDbName() throws Exception {
        String runner = Files.readString(
                DEPLOYMENT_ROOT.resolve("Invoke-ChecklistRetentionFinalizer.ps1"));
        var databaseUriMatcher = Pattern.compile(
                "\\[Uri\\]::TryCreate\\(\\s*\\$DatabaseUrl,\\s*"
                        + "\\[UriKind\\]::Absolute,\\s*\\[ref\\]\\$databaseUri\\)")
                .matcher(runner);

        assertThat(databaseUriMatcher.find()).isTrue();
        assertThat(runner)
                .contains("$trimmedDatabaseUrl = $DatabaseUrl.TrimStart()")
                .contains("if ($trimmedDatabaseUrl.StartsWith('-')) {")
                .contains("CHECKLIST_RETENTION_PROVISIONER_DB_URL_OPTION_REJECTED")
                .contains("$databaseUri.UserInfo.Contains(':')")
                .contains("CHECKLIST_RETENTION_PROVISIONER_DB_URL_PASSWORD_FORBIDDEN")
                .contains("$databaseArgument = \"--dbname=$DatabaseUrl\"")
                .contains("& $PsqlPath @psqlSafetyArguments $databaseArgument '--file'")
                .contains("& $PsqlPath @psqlSafetyArguments $databaseArgument '--tuples-only'")
                .doesNotContain("& $PsqlPath @psqlSafetyArguments $DatabaseUrl");
    }

    @Test
    void runnerRejectsUriQueryAndLibpqConninfoPasswordsBeforePsql() throws Exception {
        String runner = Files.readString(
                DEPLOYMENT_ROOT.resolve("Invoke-ChecklistRetentionFinalizer.ps1"));

        assertThat(runner)
                .contains("$decodedDatabaseQuery = [Uri]::UnescapeDataString($databaseUri.Query)")
                .contains("(?i)(?:^|[?&;])password(?:\\s*=|[&;]|$)")
                .contains("(?i)(?:^|\\s)password\\s*=")
                .contains("CHECKLIST_RETENTION_PROVISIONER_DB_URL_PASSWORD_FORBIDDEN")
                .contains("$databaseUriIsPostgres")
                .contains("$databaseArgument = \"--dbname=$DatabaseUrl\"");
        assertThat(runner).containsSubsequence(
                "$decodedDatabaseQuery = [Uri]::UnescapeDataString($databaseUri.Query)",
                "CHECKLIST_RETENTION_PROVISIONER_DB_URL_PASSWORD_FORBIDDEN",
                "$databaseArgument = \"--dbname=$DatabaseUrl\"",
                "& $PsqlPath @psqlSafetyArguments $databaseArgument");
    }

    @Test
    void runnerAttestsAndInvokesFullExactCatalogVerifierBeforeComplete() throws Exception {
        String runner = Files.readString(
                DEPLOYMENT_ROOT.resolve("Invoke-ChecklistRetentionFinalizer.ps1"));
        var verifierChecksumMatcher = Pattern.compile(
                "\\$expectedVerifierSha256 = '([0-9a-f]{64})'").matcher(runner);

        assertThat(verifierChecksumMatcher.find()).isTrue();
        assertThat(runner)
                .contains("public.checklist_assert_retention_security()")
                .contains("carebridge_checklist_schema_owner")
                .contains("language_entry.lanname = 'plpgsql'")
                .contains("routine.prokind = 'f'")
                .contains("routine.provolatile = 'v'")
                .contains("routine.proparallel = 'u'")
                .contains("routine.prosecdef = true")
                .contains("routine.proisstrict = false")
                .contains("routine.proleakproof = false")
                .contains("routine.proconfig = ARRAY['search_path=pg_catalog, public']::text[]")
                .contains("encode(sha256(convert_to(pg_get_functiondef(to_regprocedure("
                        + "'public.checklist_assert_retention_security()')), 'UTF8')), 'hex')")
                .contains("$expectedVerifierSha256")
                .contains("CROSS JOIN LATERAL aclexplode")
                .contains("ATTESTED:$finalizerVersion")
                .contains("CHECKLIST_RETENTION_POST_COMMIT_ATTESTATION_FAILED")
                .contains("$verificationSql = \"SELECT public.checklist_assert_retention_security();\"")
                .contains("CHECKLIST_RETENTION_POST_COMMIT_QUERY_FAILED")
                .contains("CHECKLIST_RETENTION_POST_COMMIT_VERIFICATION_FAILED")
                .contains("if (($verification | Out-String).Trim() -ne \"VERIFIED:$finalizerVersion\")");
        assertThat(runner).containsSubsequence(
                "$attestation = & $PsqlPath",
                "CHECKLIST_RETENTION_POST_COMMIT_ATTESTATION_FAILED",
                "$verification = & $PsqlPath",
                "CHECKLIST_RETENTION_POST_COMMIT_VERIFICATION_FAILED",
                "Write-Output \"CHECKLIST_RETENTION_FINALIZER_COMPLETE");
    }

    @Test
    void runnerAttestsTrustedDatabaseAndPublicSchemaOwnersBeforeVerifier() throws Exception {
        String runner = Files.readString(
                DEPLOYMENT_ROOT.resolve("Invoke-ChecklistRetentionFinalizer.ps1"));

        assertThat(runner)
                .contains("FROM pg_catalog.pg_database database_entry")
                .contains("database_entry.datname = current_database()")
                .contains("database_entry.datdba = "
                        + "to_regrole('carebridge_checklist_schema_owner')")
                .contains("FROM pg_catalog.pg_namespace namespace")
                .contains("namespace.nspname = 'public'")
                .contains("namespace.nspowner = to_regrole('pg_database_owner')")
                .contains("FROM pg_catalog.pg_auth_members membership")
                .contains("membership.roleid = "
                        + "to_regrole('carebridge_checklist_schema_owner')")
                .contains("membership.member = "
                        + "to_regrole('carebridge_checklist_schema_owner')");
        assertThat(runner).containsSubsequence(
                "database_entry.datname = current_database()",
                "namespace.nspname = 'public'",
                "FROM pg_catalog.pg_auth_members membership",
                "$attestation = & $PsqlPath",
                "$verification = & $PsqlPath");
    }
}
