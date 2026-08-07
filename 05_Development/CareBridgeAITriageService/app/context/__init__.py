"""Context model: who the session is about, what they are asking, and which stage applies.

Nothing here decides a triage outcome. It decides whether the engine knows enough about
*whom* it is assessing to be allowed to ask a symptom question at all.
"""

from app.context.intent_resolver import (
    IntentResolution,
    resolve_intent,
)
from app.context.target_entity_resolver import (
    TargetResolution,
    resolve_target_entity,
)
from app.context.enums import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
    load_context_contract,
    map_legacy_stage,
    stages_for_entity,
)

__all__ = [
    "CareStage",
    "IntentResolution",
    "TargetResolution",
    "resolve_intent",
    "resolve_target_entity",
    "ContextResolutionStatus",
    "IntentType",
    "ResolutionSource",
    "TargetEntity",
    "load_context_contract",
    "map_legacy_stage",
    "stages_for_entity",
]
