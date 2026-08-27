"""Fail-closed API preflight for non-production evaluation."""

from __future__ import annotations

import ipaddress
from dataclasses import dataclass, field
from typing import Any
from urllib.parse import urlparse

import requests

from .config import EvaluationSettings

PRODUCTION_HOSTS = {"api.carebridge.vn", "carebridge.vn", "www.carebridge.vn"}
NON_PRODUCTION_SUFFIXES = (".test", ".local", ".dev", ".staging")


@dataclass(frozen=True)
class PreflightResult:
    ready: bool
    reason: str
    checks: dict[str, bool] = field(default_factory=dict)


def is_explicit_non_production_url(value: str) -> bool:
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        return False
    host = parsed.hostname.lower()
    if host in PRODUCTION_HOSTS:
        return False
    if host in {"localhost", "127.0.0.1", "::1"} or host.endswith(NON_PRODUCTION_SUFFIXES):
        return True
    try:
        return ipaddress.ip_address(host).is_private
    except ValueError:
        return False


class ApiPreflight:
    def __init__(self, settings: EvaluationSettings, session: requests.Session | None = None) -> None:
        self.settings = settings
        self.session = session or requests.Session()

    def run(self) -> PreflightResult:
        checks: dict[str, bool] = {
            "baseUrlConfigured": bool(self.settings.api_base_url),
            "jwtConfigured": bool(self.settings.test_jwt),
        }
        if not self.settings.api_base_url or not self.settings.test_jwt:
            missing = [name for name, ok in checks.items() if not ok]
            return PreflightResult(False, f"Missing non-production evaluation configuration: {', '.join(missing)}", checks)
        checks["nonProductionHost"] = is_explicit_non_production_url(self.settings.api_base_url)
        if not checks["nonProductionHost"]:
            return PreflightResult(False, "CAREBRIDGE_API_BASE_URL is not an explicit non-production host", checks)

        try:
            profile = self._get("/api/v1/profile")
        except requests.RequestException as exc:
            return PreflightResult(False, f"API/JWT preflight failed: {type(exc).__name__}", checks)
        checks["apiReachable"] = True
        checks["jwtAccepted"] = profile.status_code == 200
        if not checks["jwtAccepted"]:
            return PreflightResult(False, f"JWT was not accepted (HTTP {profile.status_code})", checks)

        checks["motherFixtureConfigured"] = bool(self.settings.mother_profile_id)
        checks["babyFixtureConfigured"] = bool(self.settings.baby_profile_id)
        if not self.settings.baby_profile_id or not self.settings.mother_profile_id:
            return PreflightResult(
                False,
                "Both CAREBRIDGE_TEST_BABY_PROFILE_ID and CAREBRIDGE_TEST_MOTHER_PROFILE_ID are required",
                checks,
            )
        try:
            if self.settings.baby_profile_id:
                baby = self._get(f"/api/v1/babies/{self.settings.baby_profile_id}")
                checks["babyFixtureOwned"] = baby.status_code == 200
                if not checks["babyFixtureOwned"]:
                    return PreflightResult(False, "Baby profile fixture is missing or not owned by the JWT user", checks)
            if self.settings.mother_profile_id:
                dashboard = self._get("/api/v1/journeys/me/dashboard")
                checks["motherFixtureOwned"] = (
                    dashboard.status_code == 200 and self.settings.mother_profile_id in dashboard.text
                )
                if not checks["motherFixtureOwned"]:
                    return PreflightResult(False, "Mother profile fixture is missing or not owned by the JWT user", checks)

            probe_stage = "INFANT"
            profile_key = "babyProfileId"
            profile_id = self.settings.baby_profile_id
            probe = self._post(
                "/api/v1/triage/intake/conversation/start",
                {
                    "clientRequestId": "evaluation_preflight_v1",
                    "initialText": "evaluation preflight",
                    "stage": probe_stage,
                    profile_key: profile_id,
                    "currentIntake": {
                        "stage": probe_stage,
                        profile_key: profile_id,
                        "symptomList": ["evaluation_unknown"],
                    },
                },
            )
        except requests.RequestException as exc:
            return PreflightResult(False, f"Fixture/schema preflight failed: {type(exc).__name__}", checks)
        checks["conversationStartReachable"] = probe.status_code == 200
        checks["requiredSchemaReady"] = probe.status_code == 200
        if probe.status_code != 200:
            return PreflightResult(
                False,
                f"Conversation start/schema preflight failed (HTTP {probe.status_code}); migrations may not be applied",
                checks,
            )
        return PreflightResult(True, "Non-production API, JWT, fixtures, schema, and conversation start are ready", checks)

    def _headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.settings.test_jwt}", "Content-Type": "application/json"}

    def _get(self, path: str) -> requests.Response:
        return self.session.get(
            f"{self.settings.api_base_url.rstrip('/')}{path}",
            headers=self._headers(),
            timeout=self.settings.request_timeout_seconds,
        )

    def _post(self, path: str, payload: dict[str, Any]) -> requests.Response:
        return self.session.post(
            f"{self.settings.api_base_url.rstrip('/')}{path}",
            headers=self._headers(),
            json=payload,
            timeout=self.settings.request_timeout_seconds,
        )
