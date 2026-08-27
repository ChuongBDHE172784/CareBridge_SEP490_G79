# CareBridge AI Evaluation

This non-production module is the single source of truth for CareBridge AI Triage
regression, conversation, legal/privacy, prompt-injection, and evidence evaluation. It
does not contain or modify production triage rules.

The official catalog has 30 cases: 20 declared in `datasets/benchmark_cases.json` and 10
generated from the canonical pediatric RED parity vectors. Clinical labels marked
`PENDING_MEDICAL_REVIEW` are regression fixtures, not clinical validation. They are
excluded from clinical RED denominators.

## Local setup

```powershell
python -m pip install -e ".[dev]"
pytest
carebridge-eval validate datasets/benchmark_cases.json
carebridge-eval run --mode local --output-dir reports/latest
```

The two execution modes are:

- `LOCAL_DETERMINISTIC`: imports the existing Python triage runtime and forces
  `deterministic_only=True`.
- `API_END_TO_END`: calls Spring canonical conversation `start`, `continue`, and
  persisted `GET` endpoints.

The one-shot endpoint is not part of the main benchmark. Default budgets are 20 seconds
per HTTP request and 90 seconds per case, intentionally above Spring's 15-second Python
call timeout.

## API preflight

API evaluation requires all of the following non-production fixtures:

```text
CAREBRIDGE_API_BASE_URL=
CAREBRIDGE_TEST_JWT=
CAREBRIDGE_TEST_BABY_PROFILE_ID=
CAREBRIDGE_TEST_MOTHER_PROFILE_ID=
EVALUATION_REQUEST_TIMEOUT_SECONDS=20
EVALUATION_CASE_BUDGET_SECONDS=90
EVALUATION_MAX_RETRIES=1
```

Preflight rejects production hosts and checks JWT acceptance, profile ownership, schema
readiness, and conversation start. Missing fixtures or unapplied migrations produce
`INFRASTRUCTURE_SKIPPED`. The runner never logs in, generates a token, retries `continue`,
or applies a migration.

## Metrics and reports

The registry contains exactly 21 approved metrics. Execution/review counts such as
`executedCases`, `infrastructureSkippedCases`, and `pendingMedicalReviewCases` live in
`summary`; they are not quality metrics. Zero-denominator rates use `value=null` and
`NOT_EVALUATED`.

Each run emits only:

- `report.json`
- `report.csv`
- `report.html`

Generated reports are ignored because API-mode output may contain minimized
non-production health data.

## Promptfoo and Galileo

Exactly four Promptfoo configs are maintained:

- `pediatric-safety.yaml`
- `legal-safety.yaml`
- `prompt-injection.yaml`
- `rag-citation.yaml`

The first three call the local deterministic runtime without an external model key. The
RAG suite calls the non-production API and is `INFRASTRUCTURE_SKIPPED` unless every API
fixture is present. Promptfoo never acts as clinical ground truth.

Galileo remains optional and disabled by default. Missing official key/project settings
produce `SKIPPED_NO_EXTERNAL_KEY`; Galileo output cannot change deterministic pass/fail.

The MedQA adapter only reads a user-supplied lawful JSONL subset as a knowledge baseline.
No dataset content is bundled and MedQA is not a triage benchmark. Med-PaLM is a research
reference only and is not integrated.

## Docker

```powershell
docker build --build-context ai-service=../../05_Development/CareBridgeAITriageService `
  -t carebridge-ai-evaluator:local .
docker run --rm -v "${PWD}/reports:/reports" carebridge-ai-evaluator:local
```

The image runs as a non-root user, defaults Gemini off, contains no `.env`, JWT, API key,
database, or migration runner.

## Migration status

Flyway remains disabled and triage migrations are present in source only. Reports retain
`SOURCE_ONLY_NOT_CONFIRMED_APPLIED` until a staging/DB owner confirms application. The
evaluator does not change this state.
