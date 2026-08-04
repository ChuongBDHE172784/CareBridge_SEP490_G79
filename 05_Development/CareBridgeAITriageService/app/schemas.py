from __future__ import annotations

from typing import Literal
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field

RiskLevel = Literal["GREEN", "YELLOW", "RED", "NEED_MORE_INFO"]
TriageStage = Literal["PRECONCEPTION", "PREGNANCY", "POSTPARTUM", "INFANT", "TODDLER"]


class HealthContextItem(BaseModel):
    """CB-TRIAGE-THMC-IMP-001 §8.2 — advisory prior-triage summary (server-populated by
    the Java backend only). Reference material for narrative composition; NEVER an input
    to deterministic risk rules (BR-THMC-004 / BR-SAFETY). @version 1.0"""

    summaryText: str = Field(max_length=500)
    relatedStage: TriageStage
    createdAt: str | None = None   # ISO-8601
    expiresAt: str | None = None   # ISO-8601


class ChildTriageRequest(BaseModel):
    # Legacy direct callers remain pediatric by default. Conversation endpoints
    # reconcile this nested value with the required caller context and reject a
    # contradictory top-level stage instead of silently switching subjects.
    stage: TriageStage = "INFANT"
    babyProfileId: str | None = None
    motherProfileId: str | None = None
    childAgeMonths: int | None = Field(default=None, ge=0, le=216)
    symptomList: list[str] = Field(default_factory=list)
    duration: str | None = None
    temperatureC: float | None = Field(default=None, ge=30, le=45)
    feedingStatus: str | None = None
    breathingStatus: str | None = None
    consciousnessStatus: str | None = None
    vomiting: str | None = None
    diarrhea: str | None = None
    rash: str | None = None
    # Bounded descriptive facts for common, non-diagnostic symptom follow-ups.
    # They do not create risk thresholds; deterministic red rules remain separate.
    painSeverity: str | None = None
    urinarySymptoms: str | None = None
    seizure: bool | None = None
    dehydrationSigns: list[str] = Field(default_factory=list)
    hydrationStatus: str | None = None
    parentFreeText: str | None = None
    # Additive (CB-TRIAGE-THMC-IMP-001): defaults keep every existing client valid.
    healthContext: list[HealthContextItem] = Field(default_factory=list, max_length=5)
    # Additive (CB-TRIAGE-MATQ-IMP-001): trusted, server-derived context auto-bound by the
    # Java backend from the caller's active PREGNANCY journey — never user-supplied, never a
    # risk-rule input (BR-SAFETY). Descriptive-only follow-up fact for PREGNANCY abdominal
    # pain; also never a risk-rule input.
    gestationalWeeks: int | None = Field(default=None, ge=1, le=45)
    abdominalPainPattern: str | None = None


class Citation(BaseModel):
    id: str | None = None
    sourceId: str | None = None
    title: str
    source: str
    organization: str | None = None
    url: str
    domain: str | None = None
    section: str | None = None
    heading: str | None = None
    excerpt: str
    retrievedAt: str
    matchedSymptoms: list[str] = Field(default_factory=list)
    matchedRules: list[str] = Field(default_factory=list)
    sourceVersion: str = "1.0"
    sourceStatus: Literal["DRAFT", "PENDING_REVIEW", "APPROVED", "DEPRECATED", "ARCHIVED"] = "APPROVED"
    retrievalMode: Literal["LOCAL", "REALTIME"] = "LOCAL"
    lastReviewed: str | None = None


class Claim(BaseModel):
    claimId: str
    text: str
    evidenceIds: list[str] = Field(default_factory=list)


class Evidence(BaseModel):
    basis: str = "RULE_ENGINE_AND_OFFICIAL_SOURCES"
    legalSafetyNote: str
    matchedSymptoms: list[str] = Field(default_factory=list)
    matchedOfficialSources: list[str] = Field(default_factory=list)
    unmatchedSymptoms: list[str] = Field(default_factory=list)


class ChildTriageResponse(BaseModel):
    stage: TriageStage = "INFANT"
    riskLevel: RiskLevel
    riskColor: str
    summary: str
    possibleConcern: str
    recommendedAction: str
    emergencyActionRequired: bool
    redFlags: list[str] = Field(default_factory=list)
    matchedRules: list[str] = Field(default_factory=list)
    citations: list[Citation] = Field(default_factory=list)
    claims: list[Claim] = Field(default_factory=list)
    evidence: Evidence
    questions: list[str] = Field(default_factory=list)
    warning: str | None = None
    disclaimer: str
    normalizedSymptoms: list[str] = Field(default_factory=list)
    normalizedSymptomDetails: list["NormalizedSymptom"] = Field(default_factory=list)
    evidenceIds: list[str] = Field(default_factory=list)
    recommendationCode: str
    explainabilityMetrics: "ExplainabilityMetrics"
    graphVersion: str = "1.0"
    ruleSetVersion: str = "1.0"
    ontologyVersion: str = "1.0"
    responseSchemaVersion: str = "2.0"
    fallbackUsed: bool = False
    assistantProvider: Literal["GEMINI", "DETERMINISTIC_FALLBACK"] = "DETERMINISTIC_FALLBACK"
    assistantFallbackUsed: bool = True


class NormalizedSymptom(BaseModel):
    originalTextMasked: str
    normalizedCode: str
    normalizationMethod: Literal["EXACT", "KEYWORD", "STRUCTURED", "GEMINI_STRUCTURED_OUTPUT"]
    normalizationConfidence: float = Field(ge=0, le=1)
    exactMatch: bool = False


class ExplainabilityMetrics(BaseModel):
    ruleMatchQuality: Literal["EXACT", "PARTIAL", "NONE"]
    evidenceCoverage: Literal["FULL", "PARTIAL", "NONE"]
    normalizationConfidence: float = Field(ge=0, le=1)
    matchedSourceCount: int = Field(ge=0)
    retrievalMode: Literal["LOCAL", "REALTIME", "NONE"]


class SourceDocument(BaseModel):
    id: str
    title: str
    organization: str
    url: str
    domain: str
    lastReviewed: str
    topic: str
    ageRange: str
    riskLevels: list[str] = Field(default_factory=list)
    symptoms: list[str] = Field(default_factory=list)
    applicableStages: list[TriageStage] = Field(default_factory=lambda: ["INFANT", "TODDLER"])
    sourceType: str = "official_guideline"
    sourceVersion: str = "1.0"
    sourceStatus: Literal["DRAFT", "PENDING_REVIEW", "APPROVED", "DEPRECATED", "ARCHIVED"] = "DRAFT"
    approvedAt: str | None = None
    approvedBy: str | None = None
    deprecatedAt: str | None = None
    section: str | None = None
    retrievedAt: str | None = None
    retrievedBy: str | None = None
    etag: str | None = None
    lastModified: str | None = None
    body: str


AnswerType = Literal["TEXT", "NUMBER", "BOOLEAN", "SINGLE_CHOICE", "MULTI_CHOICE"]
IntakeFlowStatus = Literal["ASK_MORE", "TRIAGE_COMPLETE"]


class IntakeQuestion(BaseModel):
    questionKey: str
    text: str
    answerType: AnswerType
    options: list[str] = Field(default_factory=list)


class IntakeMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str


IntentSubject = Literal["MOTHER", "CHILD"]
IntentCareGoal = Literal["ASSESS_REPORTED_SYMPTOM", "CLARIFY_SYMPTOM"]


class TriageIntent(BaseModel):
    """Bounded fact-selection hint; never a risk or routing authority."""

    model_config = ConfigDict(extra="forbid")
    subject: IntentSubject
    careGoal: IntentCareGoal
    symptomFamilies: list[str] = Field(default_factory=list, max_length=8)
    knownFacts: list[str] = Field(default_factory=list, max_length=24)
    confidence: float = Field(ge=0, le=1)


class IntakeStartRequest(BaseModel):
    intakeSessionId: str | None = None
    initialText: str | None = None
    stage: TriageStage | None = None
    currentIntake: ChildTriageRequest = Field(default_factory=ChildTriageRequest)
    # Additive (CB-TRIAGE-THMC-IMP-001): defaults keep every existing client valid.
    healthContext: list[HealthContextItem] = Field(default_factory=list, max_length=5)

class IntakeContinueRequest(BaseModel):
    intakeSessionId: str
    stage: TriageStage | None = None
    currentIntake: ChildTriageRequest = Field(default_factory=ChildTriageRequest)
    messages: list[IntakeMessage] = Field(default_factory=list)
    newAnswers: dict[str, object] = Field(default_factory=dict)
    askedQuestionKeys: list[str] = Field(default_factory=list, max_length=36)
    round: int = Field(default=1, ge=1, le=3)

class IntakeFlowResponse(BaseModel):
    status: IntakeFlowStatus
    intakeSessionId: str = Field(default_factory=lambda: str(uuid4()))
    stage: TriageStage = "INFANT"
    mergedIntake: ChildTriageRequest
    normalizedSymptomDetails: list[NormalizedSymptom] = Field(default_factory=list)
    assistantMessage: str | None = None
    questions: list[IntakeQuestion] = Field(default_factory=list)
    round: int
    triageResult: ChildTriageResponse | None = None
    assistantProvider: Literal["GEMINI", "DETERMINISTIC_FALLBACK"] = "DETERMINISTIC_FALLBACK"
    assistantFallbackUsed: bool = True
    conversationSummary: str | None = None
    intent: TriageIntent | None = None
    askedQuestionKeys: list[str] = Field(default_factory=list, max_length=36)


class GeminiNormalizedSymptomItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    code: str
    matchedText: str = Field(max_length=120)
    confidence: float = Field(ge=0, le=1)


class GeminiExtractedFacts(BaseModel):
    model_config = ConfigDict(extra="forbid")

    temperatureC: float | None = Field(default=None, ge=30, le=45)
    duration: str | None = Field(default=None, max_length=80)
    feedingStatus: str | None = Field(default=None, max_length=40)
    breathingStatus: str | None = Field(default=None, max_length=40)


class GeminiNormalizedSymptoms(BaseModel):
    model_config = ConfigDict(extra="forbid")

    normalizedSymptoms: list[GeminiNormalizedSymptomItem] = Field(default_factory=list, max_length=18)
    extractedFacts: GeminiExtractedFacts = Field(default_factory=GeminiExtractedFacts)
    unknownTerms: list[str] = Field(default_factory=list, max_length=12)
    instructionLikeContentDetected: bool = False


class GeminiFollowupQuestion(BaseModel):
    model_config = ConfigDict(extra="forbid")

    questionKey: str
    text: str = Field(min_length=3, max_length=240)
    answerType: AnswerType
    options: list[str] = Field(default_factory=list, max_length=10)


class GeminiFollowupDraft(BaseModel):
    model_config = ConfigDict(extra="forbid")

    assistantMessage: str = Field(min_length=3, max_length=400)
    questions: list[GeminiFollowupQuestion] = Field(default_factory=list, max_length=3)


class GeminiExplanation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    summary: str = Field(min_length=3, max_length=600)
    possibleConcern: str = Field(min_length=3, max_length=500)
    recommendedAction: str = Field(min_length=3, max_length=600)
    evidenceExplanation: str = Field(default="", max_length=600)
    disclaimer: str = Field(default="", max_length=400)


class GeminiConversationSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    summary: str = Field(min_length=3, max_length=500)
