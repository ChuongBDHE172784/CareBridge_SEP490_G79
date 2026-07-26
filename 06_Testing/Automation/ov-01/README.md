# OV-01 Deterministic Quality-Gate Runner

`OV01-AUTO-002` binds the 15 required `OV01-E2E-001..015` scenario IDs to the strongest existing Backend/PostgreSQL, Mobile, Web and AI tests. It does not create a Function or use-case identity.

The registry is a closed contract: exactly 15 scenario identities, the immutable ordered 17-gate Release catalog, and nine artifact identities. Every scenario has at least one executable selector that must be observed in the parsed output of its mapped gate before the scenario can pass.

## Release run

Create a UTF-8 text file containing every explicit Story 6.10-owned path, one repository-relative path per line. The runner unions that list with Git's complete tracked/untracked dirty set, sorts it ordinally, hashes every present file with SHA-256, represents deleted files as `<MISSING>`, and binds the result to the approved baseline HEAD.

Run the complete catalog into a new evidence directory:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/Invoke-Ov01QualityGate.ps1 `
  -ScenarioId ALL `
  -RunMode Release `
  -BaselineHead 6514f376496a51a55bbd5da257ea8e034a4428ae `
  -TouchedPathsFile D:/tmp/ov01-touched-paths.txt `
  -ManualEvidenceSummaryPath _bmad-output/test-artifacts/story-6-10/android-final-<timestamp>/manual-run-summary.json `
  -CheckpointKeyPath D:/tmp/ov01-release-<same-timestamp>/checkpoint-key.json `
  -OutputDirectory _bmad-output/test-artifacts/story-6-10/official-run-<timestamp>
```

If the Windows job host terminates after one or more gates, resume the same directory with every binding argument unchanged and add `-Resume`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/Invoke-Ov01QualityGate.ps1 `
  -ScenarioId ALL `
  -RunMode Release `
  -Resume `
  -BaselineHead 6514f376496a51a55bbd5da257ea8e034a4428ae `
  -TouchedPathsFile D:/tmp/ov01-touched-paths.txt `
  -ManualEvidenceSummaryPath _bmad-output/test-artifacts/story-6-10/android-final-<timestamp>/manual-run-summary.json `
  -CheckpointKeyPath D:/tmp/ov01-release-<same-timestamp>/checkpoint-key.json `
  -OutputDirectory _bmad-output/test-artifacts/story-6-10/official-run-<same-timestamp>
```

Release writes `release-checkpoint.json` atomically before the first gate, before starting each gate, and after every completed gate. A fresh Release requires a new absolute `-CheckpointKeyPath` outside both repository and evidence directory; the runner creates one random 256-bit HMAC key, stores it only as a Windows DPAPI `CurrentUser` blob with inheritance disabled and a current-user-only ACL, and never writes key bytes or the protected blob into evidence. Resume must use that exact existing path and Windows user. Missing, changed, copied, relocated, wrong-user, wrong-volume, or wrong-key state fails closed and never causes silent key regeneration. The external state directory and evidence directory must be on the same volume (`D:/tmp` and this repository are the canonical layout), because authenticated checkpoint replacement uses same-volume `MoveFileEx(REPLACE_EXISTING | WRITE_THROUGH)`. Every staging filename contains the public key ID plus a fresh 32-hex GUID. A host crash may leave an exact `.tmp-<32hex>` or legacy `.bak-<32hex>` file in the external state directory; resume inventories its non-secret name, size, and SHA-256 but never deletes, moves, rewrites, trusts, or imports it. The authenticated main checkpoint remains the sole resume authority, and the next write uses a fresh GUID. No `.tmp` or `.bak` checkpoint file is created inside evidence.

The checkpoint and final report retain only the public attestation: HMAC scheme, `DPAPI-CurrentUser`, key ID, key fingerprint, canonical-path fingerprint, and key-file hash. Preserve the external key file with the sealed bundle's operational records; do not copy it into the repository or evidence directory and do not disclose its DPAPI blob. The threat boundary protects against coherent rewriting of checkpoint, logs, results, and every unkeyed digest by a process that can alter repository/evidence files but cannot read/unprotect the external current-user key. It does not claim protection after compromise of that Windows user account or DPAPI profile.

The `activeGate` marker records the canonical incomplete next gate, attempt number, exact log path, attempt-specific pytest runtime prefix, and canonical build-artifact IDs. Resume accepts only an ordered prefix of the exact 17-gate catalog and replays the checkpoint's source identity, manual-evidence binding, registry-bound command/toolchain/runtime configuration, retained-log totals, Surefire selectors/skips, forbidden outcomes, selector matches, gate status, result digests, logs, artifacts, and complete output-file set before skipping completed gates. If a Windows host terminates before or during a gate, only that marker's canonical log/runtime/build surfaces may be staged; arbitrary extras fail closed, and pytest resumes with a fresh attempt prefix while retaining the prior partial runtime evidence. A missing, foreign, reordered, duplicated, stale, mutated, or extra checkpoint member fails closed. `Diagnostic -Resume` is forbidden. The checkpoint is retained in the final sealed evidence set.

`Release` requires the literal `-ScenarioId ALL` selection and the immutable exact 17-gate catalog; a mutable registry cannot add, replace, or remove a release gate even when a scenario references it. Any subset is diagnostic-only. It also requires a sealed schema-v2 manual summary covering each `OV01-MAN-001..034` exactly once with `PASS`, in-bundle evidence paths, exact device identity, UTC build/install/execute/completion chronology, a zero-finding leak scan, and byte-identical candidate/installed APK hashes. Every row must retain a non-empty `.txt`/`.log` ADB transcript in both `adbTranscriptPaths` and `evidencePaths`; an oracle with `requiresApiDb=true` must also bind substantive `apiDbEvidencePaths`. The runner independently rescans every retained text/log/JSON/XML/Markdown/CSV/YAML file instead of trusting the declared leak result. After the 17 automated gates run, the manual APK hash must equal the APK produced by the official build gate; manual evidence is a release precondition and does not add an eighteenth platform gate. Each reported gate log and build/input artifact is replayed against its contained filesystem path, byte length, and SHA-256; missing, escaping, forged, or extra identities fail closed. The full source identity is recomputed after all gates and artifact builds, excluding only the current evidence output directory, and any HEAD, registry, composite, file-state, or file-hash drift makes the run ineligible. Release mode accepts only the canonical `ov01-scenario-registry.json`; its repository-relative path and SHA-256 are included in the composite source identity and copied into every artifact record. The touched-Dart format gate suppresses analytics without changing Dart configuration, executes only ordinal-sorted changed/present Dart files, and records `NO_TOUCHED_DART_FILES` when there are none.

A minimal schema-v2 scenario row is:

```json
{
  "id": "OV01-MAN-001",
  "status": "PASS",
  "actualResult": "Observed the expected mother-lifecycle state.",
  "executedUtc": "2026-07-26T08:10:00Z",
  "apkSha256": "<64 lowercase hex>",
  "device": { "serial": "<serial>", "androidVersion": "15", "apiLevel": "35", "buildFingerprint": "<fingerprint>" },
  "oracle": { "type": "state", "expected": "<expected>", "observed": "<observed>", "verdict": "PASS", "requiresApiDb": false },
  "defectRefs": [],
  "evidencePaths": ["<bundle>/man001-proof.png", "<bundle>/man001-adb.txt"],
  "adbTranscriptPaths": ["<bundle>/man001-adb.txt"],
  "apiDbEvidencePaths": []
}
```

The summary root uses `schemaVersion: 2` and must include `status`, `candidateBuiltUtc`, `installedUtc`, `completedUtc`, `candidateApkSha256`, `installedApkSha256`, `evidenceManifestPath`, the exact `requiredScenarioIds`, all 34 `scenarioResults`, `device`, `leakScan`, and `multiCaseArtifacts`. Evidence is scenario-specific by filename. A file intentionally shared across rows is valid only when one `multiCaseArtifacts` entry declares its path and the exact complete scenario-ID set (at least two); undeclared, partial, duplicate, or additional reuse is rejected.

A focused or synthetic run must use diagnostic mode:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/Invoke-Ov01QualityGate.ps1 `
  -ScenarioId OV01-E2E-001 `
  -RunMode Diagnostic `
  -RegistryPath <diagnostic-registry.json> `
  -BaselineHead <approved-head> `
  -TouchedPathsFile <touched-paths.txt> `
  -OutputDirectory <new-output-directory>
```

Diagnostic success is recorded as `diagnosticStatus=PASS`, `overallStatus=NOT_APPLICABLE`, and `releaseEligible=false`; it can never emit release `PASS`.

The runner refuses empty or subset Release selections, unknown/duplicate/replaced registry or result identities, non-empty output directories, stale HEADs, zero-test PASS, pytest `failed/error/skipped/xfailed/xpassed/deselected` outcomes, pending/fixme/disabled markers, missing or unexpected artifacts, missing selector evidence, false-green scenario/report states, leaked high-confidence secrets/contact data, and artifact/source/registry-identity mismatches. Output includes exact commands, exit codes, parsed totals, durations, sanitized logs, validated absolute toolchain paths/hashes/version output, whitelisted environment identity, source/artifact hashes, validator results, and a closed-set evidence manifest. Windows `.bat`/`.cmd` tools are invoked as the final `cmd.exe /d /v:off /s /c` command with target and arguments supplied through reserved environment slots. This deliberately avoids `CALL`'s second expansion pass, quotes every slot, and preserves individual spaces, quotes, trailing backslashes, and CMD metacharacters as data. CR/LF/NUL values and the inherently ambiguous combination of an embedded quote with a CMD separator are rejected before process creation. Canonical Flutter CLI gates do not execute `flutter.bat`: the resolver derives `bin/cache/dart-sdk/bin/dart.exe` and `bin/cache/flutter_tools.snapshot` from the configured `flutter.bat` root, validates and hashes all three files, then invokes the snapshot directly with the original `test`, `analyze`, or `build` arguments. The report binds the configured launcher, Dart executable, snapshot, version output, and SHA-256 identities as one Flutter toolchain. Every canonical gate declares a 1-3600 second timeout; timeout kills the spawned process tree, records `timedOut`, `childTreeTerminated`, and exit `124`, forces the gate to `FAIL`, and still lets the run write and seal rejected evidence. Backend full and Flutter full/build gates use 1800-second budgets. The backend clean-package gate uses a 2400-second budget after the final Story 6.10 source snapshot measured 2096 seconds on the release host; the prior 1800-second host terminated before Maven's clean test-inclusive package could emit its JAR despite zero product-test failures. Focused and lighter gates use 300-1200 seconds. Maven wrapper fallback is accepted only after locating and executing an actual Apache Maven 3.9.16 binary; the package artifact is the exact `target/backend-0.0.1-SNAPSHOT.jar`.

## Independent validation

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/Test-Ov01Evidence.ps1 -Mode Registry
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/Test-Ov01Evidence.ps1 -Mode Report -ReportPath <run>/run-report.json -CheckpointKeyPath D:/tmp/ov01-release-<timestamp>/checkpoint-key.json
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/Test-Ov01Evidence.ps1 -Mode Seal -EvidenceDirectory <run>
powershell -NoProfile -ExecutionPolicy Bypass -File 06_Testing/Automation/ov-01/tests/Runner.SelfTest.ps1
```

Do not modify a sealed evidence directory. Any later file mutation, addition or removal makes `Seal` validation fail. Generate a new run instead.
