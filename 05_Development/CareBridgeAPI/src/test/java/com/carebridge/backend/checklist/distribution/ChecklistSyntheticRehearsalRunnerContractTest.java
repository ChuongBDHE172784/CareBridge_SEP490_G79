package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChecklistSyntheticRehearsalRunnerContractTest {

    private static final Path SCRIPT = Path.of(
            "..", "Deployment", "database", "Invoke-ChecklistSyntheticRehearsal.ps1");

    @Test
    void runnerPinsPostgres18ToLoopbackAndRejectsRepositoryOwnedRuntimePaths() throws Exception {
        String runner = Files.readString(SCRIPT);

        assertThat(runner)
                .contains("$requiredPostgresMajor = 18")
                .contains("'initdb.exe'")
                .contains("'pg_ctl.exe'")
                .contains("'psql.exe'")
                .contains("listen_addresses = '127.0.0.1'")
                .contains("CHECKLIST_SYNTHETIC_POSTGRES_18_REQUIRED")
                .contains("CHECKLIST_SYNTHETIC_RUNTIME_PATH_INSIDE_REPOSITORY_FORBIDDEN")
                .contains("Assert-PathOutsideRepository $exportRoot")
                .contains("Assert-PathOutsideRepository $script:scratchRoot")
                .doesNotContain("0.0.0.0")
                .doesNotContain("listen_addresses = '*'");
    }

    @Test
    void runnerUsesPasswordFilesWithoutPuttingSecretsInArgumentsOrArtifacts() throws Exception {
        String runner = Files.readString(SCRIPT);

        assertThat(runner)
                .contains("--pwfile=$bootstrapPasswordFile")
                .contains("$env:PGPASSFILE = $script:pgpassFile")
                .contains("Set-ProtectedCredentialFile")
                .contains("Assert-NoSecretInEvidence")
                .contains("CHECKLIST_SYNTHETIC_SECRET_LEAK_DETECTED")
                .contains("Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue")
                .contains("'--no-password'")
                .contains("'--no-psqlrc'")
                .contains("'--set=ON_ERROR_STOP=on'")
                .doesNotContain("--password=")
                .doesNotContain("PGPASSWORD = $databasePassword");
    }

    @Test
    void disposableAndCohortAttestationsHaveTheExactVerifierBindings() throws Exception {
        String runner = Files.readString(SCRIPT);

        assertThat(runner)
                .contains("schemaVersion = 1")
                .contains("host = '127.0.0.1'")
                .contains("user = 'checklist_operations'")
                .contains("dataDirectory = $script:dataDirectory")
                .contains("sentinelPath = $script:sentinelPath")
                .contains("sentinelSha256 = $script:sentinelSha256")
                .contains("evidenceDirectory = $script:internalEvidenceRoot")
                .contains("postgresMajor = $requiredPostgresMajor")
                .contains("psqlSha256 = $script:psqlSha256")
                .contains("postgresSha256 = $script:postgresSha256")
                .contains("initdbSha256 = $script:initdbSha256")
                .contains("pgCtlSha256 = $script:pgCtlSha256")
                .contains("pgDumpSha256 = $script:pgDumpSha256")
                .contains("pgRestoreSha256 = $script:pgRestoreSha256")
                .contains("mode = 'approved-local-control-plane-simulation'")
                .contains("cohortEnabled = $false")
                .contains("datasetFingerprint = $datasetFingerprint")
                .contains("disposableRunId = $script:runId")
                .contains("'-DisposableAttestationPath', $disposableAttestationPath")
                .contains("'-CohortAttestationPath', $cohortAttestationPath")
                .contains("postgresql://checklist_operations@127.0.0.1:$script:port/$Database")
                .contains("'-ReferenceDatasetManifestPath', $ReferenceDatasetManifestPath")
                .doesNotContain("sslmode=disable");
    }

    @Test
    void runnerBindsRawEvidenceAndEveryVerifierPhaseToTheSameDisposableRun() throws Exception {
        String runner = Files.readString(SCRIPT);

        assertThat(runner)
                .contains("raw-flyway.log")
                .contains("raw-lock-samples.json")
                .contains("correction-artifact.json")
                .contains("Get-FileHash -Algorithm SHA256")
                .contains("'CapturePre'")
                .contains("'VerifyPost'")
                .contains("'ProveAbort'")
                .contains("'VerifyRollForward'")
                .contains("Invoke-SyntheticOperator 'today'")
                .contains("Invoke-SyntheticOperator 'reconcile'")
                .contains("Invoke-SyntheticOperator 'expand-challenge' $abortDatabase")
                .contains("Invoke-SyntheticOperator 'expand-challenge' $rollForwardDatabase")
                .contains("$abortDatabase = \"carebridge_chk041_abort_$databaseSuffix\"")
                .contains("$rollForwardDatabase = \"carebridge_chk041_rollforward_$databaseSuffix\"")
                .contains("$independentCorrectedHistory")
                .contains("CHECKLIST_SYNTHETIC_CORRECTED_HISTORY_NOT_INDEPENDENTLY_VERIFIED")
                .contains("New-CohortAttestation")
                .contains("Invoke-RehearsalVerifier")
                .contains("CHECKLIST_SYNTHETIC_REHEARSAL_COMPLETE");
    }

    @Test
    void cleanupRequiresTheExactRunSentinelAndNeverTargetsTheRepository() throws Exception {
        String runner = Files.readString(SCRIPT);

        assertThat(runner)
                .contains("CHECKLIST_SYNTHETIC_CLEANUP_SENTINEL_MISSING")
                .contains("CHECKLIST_SYNTHETIC_CLEANUP_SENTINEL_MISMATCH")
                .contains("$actualSentinelSha256 -ne $script:sentinelSha256")
                .contains("Stop-DisposablePostgres")
                .contains("Remove-SentinelBoundDirectory")
                .contains("-LiteralPath $script:runRoot")
                .doesNotContain("Remove-Item -Recurse -Force $ScratchRoot")
                .doesNotContain("Remove-Item -Recurse -Force $WorkspaceRoot");
    }

    @Test
    void runnerCallsOnlyTheOptInSyntheticOperatorForDataMutation() throws Exception {
        String runner = Files.readString(SCRIPT);

        assertThat(runner)
                .contains("ChecklistSyntheticRehearsalExternalPostgresTest")
                .contains("CAREBRIDGE_CHK041_OPERATOR_ENABLED")
                .contains("Invoke-SyntheticOperator")
                .contains("CHECKLIST_SYNTHETIC_OPERATOR_FAILED")
                .doesNotContain("DROP DATABASE")
                .doesNotContain("DROP SCHEMA")
                .doesNotContain("TRUNCATE TABLE");
    }

    @Test
    void verifierFailsClosedForNonLocalTargetsMissingAttestationsAndUnboundRawEvidence()
            throws Exception {
        String verifier = Files.readString(
                SCRIPT.resolveSibling("Invoke-ChecklistMigrationRehearsal.ps1"));

        assertThat(verifier)
                .contains("CHECKLIST_REHEARSAL_DATABASE_URL_NOT_LOCAL_DISPOSABLE")
                .contains("-not [string]::IsNullOrWhiteSpace($databaseUri.Query)")
                .contains("CHECKLIST_REHEARSAL_LIBPQ_TARGET_ENV_FORBIDDEN")
                .contains("CHECKLIST_REHEARSAL_DATASET_MANIFEST_REQUIRED")
                .contains("CHECKLIST_REHEARSAL_DISPOSABLE_ATTESTATION_REQUIRED")
                .contains("CHECKLIST_REHEARSAL_COHORT_ATTESTATION_REQUIRED")
                .contains("CHECKLIST_REHEARSAL_PGPASSFILE_REQUIRED")
                .contains("CHECKLIST_REHEARSAL_RAW_FLYWAY_EVIDENCE_INVALID")
                .contains("CHECKLIST_REHEARSAL_RAW_LOCK_EVIDENCE_INVALID")
                .contains("CHECKLIST_REHEARSAL_ROLL_FORWARD_THRESHOLD_CHANGE_FORBIDDEN");
    }
}
