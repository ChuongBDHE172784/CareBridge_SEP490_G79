"""Run only the fail-closed non-production API preflight."""

from __future__ import annotations

import json

from carebridge_evaluation.config import EvaluationSettings
from carebridge_evaluation.preflight import ApiPreflight


def main() -> int:
    result = ApiPreflight(EvaluationSettings.from_env()).run()
    print(json.dumps({"ready": result.ready, "reason": result.reason, "checks": result.checks}))
    return 0 if result.ready else 2


if __name__ == "__main__":
    raise SystemExit(main())
