from __future__ import annotations

from typing import Literal
from uuid import uuid4

from pydantic import BaseModel, Field

RiskLevel = Literal["GREEN", "YELLOW", "RED", "NEED_MORE_INFO"]


class ChildTriageRequest(BaseModel):
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
    seizure: bool | None = None
    dehydrationSigns: list[str] = Field(default_factory=list)
    parentFreeText: str | None = None


class Citation(BaseModel):
    id: str | None = None
    title: str
    source: str
    organization: str | None = None
    url: str
    domain: str | None = None
    section: str | None = None
    excerpt: str
    retrievedAt: str
    matchedSymptoms: list[str] = Field(default_factory=list)
    matchedRules: list[str] = Field(default_factory=list)
    confidence: float = 0.95
    sourceStatus: Literal["REVIEWED", "PENDING_REVIEW"] = "REVIEWED"
    lastReviewed: str | None = None


class Evidence(BaseModel):
    basis: str = "RULE_ENGINE_AND_OFFICIAL_SOURCES"
    legalSafetyNote: str
    matchedSymptoms: list[str] = Field(default_factory=list)
    matchedOfficialSources: list[str] = Field(default_factory=list)
    unmatchedSymptoms: list[str] = Field(default_factory=list)


class ChildTriageResponse(BaseModel):
    riskLevel: RiskLevel
    riskColor: str
    summary: str
    possibleConcern: str
    recommendedAction: str
    emergencyActionRequired: bool
    redFlags: list[str] = Field(default_factory=list)
    matchedRules: list[str] = Field(default_factory=list)
    citations: list[Citation] = Field(default_factory=list)
    evidence: Evidence
    questions: list[str] = Field(default_factory=list)
    warning: str | None = None
    disclaimer: str


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
    sourceType: str = "official_guideline"
    sourceStatus: Literal["REVIEWED", "PENDING_REVIEW"] = "REVIEWED"
    retrievedAt: str | None = None
    retrievedBy: str | None = None
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


class IntakeStartRequest(BaseModel):
    initialText: str | None = None
    currentIntake: ChildTriageRequest = Field(default_factory=ChildTriageRequest)


class IntakeContinueRequest(BaseModel):
    intakeSessionId: str
    currentIntake: ChildTriageRequest = Field(default_factory=ChildTriageRequest)
    messages: list[IntakeMessage] = Field(default_factory=list)
    newAnswers: dict[str, object] = Field(default_factory=dict)
    round: int = Field(default=1, ge=1, le=3)


class IntakeFlowResponse(BaseModel):
    status: IntakeFlowStatus
    intakeSessionId: str = Field(default_factory=lambda: str(uuid4()))
    mergedIntake: ChildTriageRequest
    assistantMessage: str | None = None
    questions: list[IntakeQuestion] = Field(default_factory=list)
    round: int
    triageResult: ChildTriageResponse | None = None
