"""Canonical clinical rule registry and the deterministic evaluator built on it.

The rule engine is the ONLY component allowed to decide a triage outcome. No LLM output
reaches it: it reads structured signals and context fields exclusively.
"""

from app.rules.condition import Presence, Tri
from app.rules.evaluator import RuleEvaluation, evaluate
from app.rules.registry import (
    RegistryIntegrityError,
    RuleRegistry,
    get_registry,
    load_registry,
    reset_registry_cache,
)

__all__ = [
    "Presence",
    "RegistryIntegrityError",
    "RuleEvaluation",
    "RuleRegistry",
    "Tri",
    "evaluate",
    "get_registry",
    "load_registry",
    "reset_registry_cache",
]
