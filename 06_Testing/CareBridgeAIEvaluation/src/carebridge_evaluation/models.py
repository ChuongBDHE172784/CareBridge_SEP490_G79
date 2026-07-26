"""Stable schemas shared by datasets, adapters, metrics, and reports."""

from __future__ import annotations

from datetime import UTC, datetime
from enum import StrEnum
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field, model_validator


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", populate_by_name=True)


class BenchmarkCategory(StrEnum):
    PEDIATRIC_RED = "PEDIATRIC_RED"
    NEED_MORE_INFO = "NEED_MORE_INFO"
    PRECONCEPTION = "PRECONCEPTION"
    PREGNANCY = "PREGNANCY"
    POSTPARTUM_MOTHER = "POSTPARTUM_MOTHER"
    AGE_BAND_CHILD_CARE = "AGE_BAND_CHILD_CARE"
    LEGAL_PRIVACY_SAFETY = "LEGAL_PRIVACY_SAFETY"
    PROMPT_INJECTION = "PROMPT_INJECTION"
    RAG_CITATION = "RAG_CITATION"


class TriageStage(StrEnum):
    PRECONCEPTION = "PRECONCEPTION"
    PREGNANCY = "PREGNANCY"
    POSTPARTUM = "POSTPARTUM"
    INFANT = "INFANT"
    TODDLER = "TODDLER"


class JourneyPhase(StrEnum):
    PRECONCEPTION = "PRECONCEPTION"
    PREGNANCY_WEEK_BASED = "PREGNANCY_WEEK_BASED"
    CHILDBIRTH = "CHILDBIRTH"
    POSTPARTUM_MOTHER = "POSTPARTUM_MOTHER"
    NEWBORN_0_28_DAYS = "NEWBORN_0_28_DAYS"
    INFANT_1_6_MONTHS = "INFANT_1_6_MONTHS"
    INFANT_6_12_MONTHS = "INFANT_6_12_MONTHS"
    TODDLER_12_24_MONTHS = "TODDLER_12_24_MONTHS"


class LegalSubcategory(StrEnum):
    LEGAL = "LEGAL"
    MEDICAL_SAFETY = "MEDICAL_SAFETY"
    PRIVACY = "PRIVACY"
    CONSENT = "CONSENT"


class DatasetType(StrEnum):
    TRIAGE_BENCHMARK = "TRIAGE_BENCHMARK"
    KNOWLEDGE_BASELINE = "KNOWLEDGE_BASELINE"


class ReviewStatus(StrEnum):
    CONFIRMED_REVIEWED = "CONFIRMED_REVIEWED"
    PENDING_MEDICAL_REVIEW = "PENDING_MEDICAL_REVIEW"
    NOT_APPLICABLE = "NOT_APPLICABLE"


class ExpectedExecutionStatus(StrEnum):
    EXECUTE = "EXECUTE"
    KNOWN_SCOPE_GAP = "KNOWN_SCOPE_GAP"
    UNSUPPORTED_SCOPE = "UNSUPPORTED_SCOPE"


class ExecutionMode(StrEnum):
    LOCAL_DETERMINISTIC = "LOCAL_DETERMINISTIC"
    API_END_TO_END = "API_END_TO_END"


class ExecutionStatus(StrEnum):
    PASSED = "PASSED"
    FAILED = "FAILED"
    INFRASTRUCTURE_SKIPPED = "INFRASTRUCTURE_SKIPPED"
    KNOWN_SCOPE_GAP = "KNOWN_SCOPE_GAP"
    UNSUPPORTED_SCOPE = "UNSUPPORTED_SCOPE"
    SKIPPED_NO_EXTERNAL_KEY = "SKIPPED_NO_EXTERNAL_KEY"
    NOT_RUN = "NOT_RUN"


class MetricStatus(StrEnum):
    PASSED = "PASSED"
    FAILED = "FAILED"
    MEASURED = "MEASURED"
    NOT_EVALUATED = "NOT_EVALUATED"


class ConversationTurn(StrictModel):
    answers: dict[str, Any] = Field(default_factory=dict)
    expectedQuestionKeys: list[str] = Field(default_factory=list)


class SourceReference(StrictModel):
    organization: str
    title: str
    url: str

    @model_validator(mode="after")
    def require_https_deep_link(self) -> SourceReference:
        from urllib.parse import parse_qs, urlparse

        parsed = urlparse(self.url)
        path = parsed.path.rstrip("/").lower()
        query_identifies_document = any(
            key.lower() in {"docid", "itemid"}
            for key in parse_qs(parsed.query)
        )
        if (
            parsed.scheme != "https"
            or not parsed.hostname
            or (path in {"", "/vi", "/en"} and not query_identifies_document)
        ):
            raise ValueError("Source references must be verified HTTPS deep links")
        return self


class CaseExpectation(StrictModel):
    riskLevel: str | None = None
    conversationStatus: str | None = None
    emergencyActionRequired: bool | None = None
    recommendationCode: str | None = None
    matchedRules: list[str] = Field(default_factory=list)
    requiredQuestionKeys: list[str] = Field(default_factory=list)
    requiredStage: TriageStage | None = None
    citationRequired: bool = False
    claimEvidenceRequired: bool = False
    professionalConsultationReminderRequired: bool = False
    promptInjectionMustBeContained: bool = False
    forbiddenOutputTerms: list[str] = Field(default_factory=list)
    expectedHttpStatus: int | None = None
    expectedErrorCode: str | None = None
    hardcodedRegistryFallbackForbidden: bool = False


class BenchmarkCase(StrictModel):
    id: str = Field(pattern=r"^[A-Z0-9][A-Z0-9_-]{2,127}$")
    category: BenchmarkCategory
    stage: TriageStage | None = None
    journeyPhase: JourneyPhase | None = None
    subcategory: LegalSubcategory | None = None
    datasetType: DatasetType = DatasetType.TRIAGE_BENCHMARK
    reviewStatus: ReviewStatus
    expectedExecutionStatus: ExpectedExecutionStatus = ExpectedExecutionStatus.EXECUTE
    supportedModes: list[ExecutionMode] = Field(
        default_factory=lambda: [ExecutionMode.LOCAL_DETERMINISTIC, ExecutionMode.API_END_TO_END]
    )
    requiresProfile: bool = False
    input: dict[str, Any]
    turns: list[ConversationTurn] = Field(default_factory=list)
    expected: CaseExpectation
    forbiddenBehaviors: list[str]
    requiredDisclaimer: bool
    sourceReferences: list[SourceReference]
    notes: str
    tags: list[str] = Field(default_factory=list)
    canonicalVectorName: str | None = None

    @model_validator(mode="after")
    def validate_category_metadata(self) -> BenchmarkCase:
        if self.category == BenchmarkCategory.AGE_BAND_CHILD_CARE and self.journeyPhase is None:
            raise ValueError("AGE_BAND_CHILD_CARE cases require journeyPhase")
        if self.category == BenchmarkCategory.LEGAL_PRIVACY_SAFETY and self.subcategory is None:
            raise ValueError("LEGAL_PRIVACY_SAFETY cases require subcategory")
        if self.category == BenchmarkCategory.POSTPARTUM_MOTHER:
            if self.expectedExecutionStatus == ExpectedExecutionStatus.EXECUTE:
                if self.stage != TriageStage.POSTPARTUM:
                    raise ValueError("Executable POSTPARTUM_MOTHER cases require the explicit POSTPARTUM stage")
            elif self.expectedExecutionStatus not in {
                ExpectedExecutionStatus.KNOWN_SCOPE_GAP,
                ExpectedExecutionStatus.UNSUPPORTED_SCOPE,
            }:
                raise ValueError("POSTPARTUM_MOTHER must execute at POSTPARTUM or declare a scope gap")
        if self.stage is not None and self.input.get("stage") != self.stage.value:
            raise ValueError("Every staged benchmark input must send the stage explicitly")
        return self


class BenchmarkDataset(StrictModel):
    schemaVersion: str = Field(pattern=r"^2\.0$")
    name: str
    canonicalParitySource: str | None = None
    canonicalParitySha256: str | None = Field(default=None, pattern=r"^[a-fA-F0-9]{64}$")
    cases: list[BenchmarkCase] = Field(min_length=1)

    @model_validator(mode="after")
    def unique_case_ids(self) -> BenchmarkDataset:
        ids = [case.id for case in self.cases]
        if len(ids) != len(set(ids)):
            raise ValueError("Benchmark case IDs must be unique")
        return self


class AssertionResult(StrictModel):
    assertionId: str
    passed: bool
    reason: str | None = None


class CaseResult(StrictModel):
    runId: str
    caseId: str
    category: BenchmarkCategory
    stage: TriageStage | None = None
    journeyPhase: JourneyPhase | None = None
    executionMode: ExecutionMode
    executionStatus: ExecutionStatus
    reviewStatus: ReviewStatus
    expectedRisk: str | None = None
    actualRisk: str | None = None
    passed: bool | None = None
    failureReasons: list[str] = Field(default_factory=list)
    latencyMs: float | None = None
    fallbackUsed: bool | None = None
    assistantProvider: str | None = None
    conversationStatus: str | None = None
    questionsByRound: list[list[str]] = Field(default_factory=list)
    disclaimerPassed: bool | None = None
    professionalConsultationReminderPassed: bool | None = None
    diagnosisViolation: bool | None = None
    prescriptionViolation: bool | None = None
    dosageViolation: bool | None = None
    guaranteedOutcomeViolation: bool | None = None
    emergencyDelayViolation: bool | None = None
    promptInjectionPassed: bool | None = None
    citationApprovedPassed: bool | None = None
    citationDeepLinkPassed: bool | None = None
    citationDomainMatchPassed: bool | None = None
    claimEvidenceMappingPassed: bool | None = None
    evidenceStageMatchPassed: bool | None = None
    hardcodedRegistryFallbackViolation: bool | None = None
    requiredQuestionCoverage: float | None = None
    stageRoutingPassed: bool | None = None
    response: dict[str, Any] | None = None
    assertions: list[AssertionResult] = Field(default_factory=list)


class MetricResult(StrictModel):
    metricId: str
    displayName: str
    description: str
    numerator: float | None
    denominator: float | None
    formula: str
    exclusions: list[str] = Field(default_factory=list)
    value: float | None
    target: float | None
    targetComparator: str | None = None
    status: MetricStatus
    reason: str | None = None

    @model_validator(mode="after")
    def denominator_zero_is_not_evaluated(self) -> MetricResult:
        if self.denominator == 0 and (self.value is not None or self.status != MetricStatus.NOT_EVALUATED):
            raise ValueError("Zero-denominator metrics must be null and NOT_EVALUATED")
        if self.status == MetricStatus.NOT_EVALUATED and not self.reason:
            raise ValueError("NOT_EVALUATED metrics require a reason")
        return self


class RunSummary(StrictModel):
    executedCases: int = 0
    infrastructureSkippedCases: int = 0
    pendingMedicalReviewCases: int = 0
    confirmedReviewedCases: int = 0
    notApplicableCases: int = 0
    knownScopeGapCases: int = 0
    notRunCases: int = 0


class EvaluationRun(StrictModel):
    schemaVersion: str = "2.0"
    runId: str = Field(default_factory=lambda: str(uuid4()))
    generatedAt: datetime = Field(default_factory=lambda: datetime.now(UTC))
    clinicalValidation: str = "NOT_CLINICALLY_VALIDATED"
    results: list[CaseResult] = Field(default_factory=list)
    metrics: list[MetricResult] = Field(default_factory=list)
    summary: RunSummary = Field(default_factory=RunSummary)
    auxiliaryMetrics: dict[str, Any] = Field(default_factory=dict)
    migrationStatus: str = "SOURCE_ONLY_NOT_CONFIRMED_APPLIED"
