[CmdletBinding()]
param(
    [string]$PsqlPath = $env:CAREBRIDGE_PSQL_PATH,
    [string]$DatabaseUrl = $env:CAREBRIDGE_RETENTION_PROVISIONER_DB_URL,
    [string]$ProvisionerPassword = $env:CAREBRIDGE_RETENTION_PROVISIONER_DB_PASSWORD,
    [string]$FinalizerPath = (Join-Path $PSScriptRoot 'finalizers\V20260729150001__finalize_checklist_retention_security.sql')
)

$ErrorActionPreference = 'Stop'
$finalizerVersion = '20260729150001'
$expectedSha256 = '4e1e2e14b42c4321bef8d876d333c7346c934ba5037ac6da9438e66f5a9917de'
$expectedVerifierSha256 = '82acbc14c2af861dcfc543597ef58937f77b2bd26d5b1c4cd36730816f949cf1'
$psqlSafetyArguments = @(
    '--no-psqlrc'
    '--no-password'
    '--set=ON_ERROR_STOP=on'
)

if ([string]::IsNullOrWhiteSpace($PsqlPath) -or -not (Test-Path -LiteralPath $PsqlPath)) {
    throw 'CHECKLIST_RETENTION_PSQL_REQUIRED'
}
if ([string]::IsNullOrWhiteSpace($DatabaseUrl)) {
    throw 'CHECKLIST_RETENTION_PROVISIONER_DB_URL_REQUIRED'
}
$trimmedDatabaseUrl = $DatabaseUrl.TrimStart()
if ($trimmedDatabaseUrl.StartsWith('-')) {
    throw 'CHECKLIST_RETENTION_PROVISIONER_DB_URL_OPTION_REJECTED'
}
$databaseUri = $null
$databaseUriIsPostgres = [Uri]::TryCreate(
        $DatabaseUrl,
        [UriKind]::Absolute,
        [ref]$databaseUri) -and
    ($databaseUri.Scheme -ieq 'postgres' -or $databaseUri.Scheme -ieq 'postgresql')
if ($databaseUriIsPostgres) {
    if ($databaseUri.UserInfo.Contains(':')) {
        throw 'CHECKLIST_RETENTION_PROVISIONER_DB_URL_PASSWORD_FORBIDDEN'
    }
    $decodedDatabaseQuery = [Uri]::UnescapeDataString($databaseUri.Query)
    if ($decodedDatabaseQuery -match '(?i)(?:^|[?&;])password(?:\s*=|[&;]|$)') {
        throw 'CHECKLIST_RETENTION_PROVISIONER_DB_URL_PASSWORD_FORBIDDEN'
    }
}
elseif ($DatabaseUrl -match '(?i)(?:^|\s)password\s*=') {
    throw 'CHECKLIST_RETENTION_PROVISIONER_DB_URL_PASSWORD_FORBIDDEN'
}
$databaseArgument = "--dbname=$DatabaseUrl"

$resolvedFinalizer = (Resolve-Path -LiteralPath $FinalizerPath).Path
$actualSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedFinalizer).Hash.ToLowerInvariant()
if ($actualSha256 -ne $expectedSha256) {
    throw "CHECKLIST_RETENTION_FINALIZER_CHECKSUM_MISMATCH:$actualSha256"
}

$previousPassword = $env:PGPASSWORD
try {
    if ([string]::IsNullOrWhiteSpace($ProvisionerPassword)) {
        # Never inherit an unrelated generic libpq password. With --no-password,
        # authentication must use an explicitly configured noninteractive source.
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $ProvisionerPassword
    }

    & $PsqlPath @psqlSafetyArguments $databaseArgument '--file' $resolvedFinalizer
    if ($LASTEXITCODE -ne 0) {
        throw "CHECKLIST_RETENTION_FINALIZER_FAILED:$LASTEXITCODE"
    }

    $attestationSql = @"
SELECT CASE WHEN
    EXISTS (
        SELECT 1
        FROM pg_catalog.pg_database database_entry
        WHERE database_entry.datname = current_database()
          AND database_entry.datdba = to_regrole('carebridge_checklist_schema_owner'))
    AND EXISTS (
        SELECT 1
        FROM pg_catalog.pg_namespace namespace
        WHERE namespace.nspname = 'public'
          AND namespace.nspowner = to_regrole('pg_database_owner'))
    AND NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_auth_members membership
        WHERE membership.roleid = to_regrole('carebridge_checklist_schema_owner')
           OR membership.member = to_regrole('carebridge_checklist_schema_owner'))
    AND EXISTS (
        SELECT 1
        FROM pg_catalog.pg_proc routine
        JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
        JOIN pg_catalog.pg_language language_entry
          ON language_entry.oid = routine.prolang
        WHERE routine.oid = to_regprocedure(
              'public.checklist_assert_retention_security()')
          AND owner_role.rolname = 'carebridge_checklist_schema_owner'
          AND owner_role.rolcanlogin = false
          AND owner_role.rolsuper = false
          AND owner_role.rolcreatedb = false
          AND owner_role.rolcreaterole = false
          AND owner_role.rolinherit = false
          AND owner_role.rolreplication = false
          AND owner_role.rolbypassrls = false
          AND language_entry.lanname = 'plpgsql'
          AND routine.prokind = 'f'
          AND routine.pronargs = 0
          AND routine.prorettype = 'pg_catalog.text'::regtype
          AND routine.provolatile = 'v'
          AND routine.proparallel = 'u'
          AND routine.prosecdef = true
          AND routine.proisstrict = false
          AND routine.proleakproof = false
          AND routine.proconfig = ARRAY['search_path=pg_catalog, public']::text[]
          AND encode(sha256(convert_to(pg_get_functiondef(to_regprocedure('public.checklist_assert_retention_security()')), 'UTF8')), 'hex')
              = '$expectedVerifierSha256'
          AND has_function_privilege(
              'checklist_operations', routine.oid, 'EXECUTE')
          AND NOT has_function_privilege(
              'carebridge_application', routine.oid, 'EXECUTE')
          AND (SELECT count(*)
               FROM pg_catalog.pg_proc acl_routine
               CROSS JOIN LATERAL aclexplode(COALESCE(
                   acl_routine.proacl, acldefault('f', acl_routine.proowner))) acl
               WHERE acl_routine.oid = routine.oid
                 AND acl.privilege_type = 'EXECUTE'
                 AND acl.is_grantable = false
                 AND acl.grantee IN (
                     routine.proowner, to_regrole('checklist_operations'))) = 2
          AND NOT EXISTS (
              SELECT 1
              FROM pg_catalog.pg_proc acl_routine
              CROSS JOIN LATERAL aclexplode(COALESCE(
                  acl_routine.proacl, acldefault('f', acl_routine.proowner))) acl
              WHERE acl_routine.oid = routine.oid
                AND (acl.privilege_type <> 'EXECUTE'
                     OR acl.is_grantable = true
                     OR acl.grantee NOT IN (
                         routine.proowner, to_regrole('checklist_operations')))))
THEN 'ATTESTED:$finalizerVersion' ELSE 'INVALID' END;
"@
    $attestation = & $PsqlPath @psqlSafetyArguments $databaseArgument '--tuples-only' '--no-align' '--command' $attestationSql
    if ($LASTEXITCODE -ne 0) {
        throw "CHECKLIST_RETENTION_POST_COMMIT_ATTESTATION_QUERY_FAILED:$LASTEXITCODE"
    }
    if (($attestation | Out-String).Trim() -ne "ATTESTED:$finalizerVersion") {
        throw "CHECKLIST_RETENTION_POST_COMMIT_ATTESTATION_FAILED:$attestation"
    }

    $verificationSql = "SELECT public.checklist_assert_retention_security();"
    $verification = & $PsqlPath @psqlSafetyArguments $databaseArgument '--tuples-only' '--no-align' '--command' $verificationSql
    if ($LASTEXITCODE -ne 0) {
        throw "CHECKLIST_RETENTION_POST_COMMIT_QUERY_FAILED:$LASTEXITCODE"
    }
    if (($verification | Out-String).Trim() -ne "VERIFIED:$finalizerVersion") {
        throw "CHECKLIST_RETENTION_POST_COMMIT_VERIFICATION_FAILED:$verification"
    }
}
finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPassword
    }
}

Write-Output "CHECKLIST_RETENTION_FINALIZER_COMPLETE:${finalizerVersion}:$actualSha256"
