from carebridge_evaluation.sanitization import minimize_response


def test_response_minimization_redacts_nested_health_text_and_identifiers():
    minimized = minimize_response({
        "intakeSessionId": "session-id",
        "mergedIntake": {
            "parentFreeText": "name and phone",
            "babyProfileId": "baby-id",
            "symptomList": ["cough"],
        },
        "triageResult": {"riskLevel": "YELLOW"},
    })

    assert minimized["intakeSessionId"] == "[REDACTED]"
    assert minimized["mergedIntake"]["parentFreeText"] == "[REDACTED]"
    assert minimized["mergedIntake"]["babyProfileId"] == "[REDACTED]"
    assert minimized["mergedIntake"]["symptomList"] == ["cough"]
