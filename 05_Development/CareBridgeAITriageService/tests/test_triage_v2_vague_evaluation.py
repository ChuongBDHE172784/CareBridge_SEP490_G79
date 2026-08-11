from __future__ import annotations

from copy import deepcopy
import json
import sys

import pytest

from scripts import run_triage_v2_evaluation as runner


@pytest.fixture(scope="module")
def vague_report() -> dict[str, object]:
    return runner.evaluate_vague_baseline(runner.DEFAULT_VAGUE_CASES)


def test_corpus_has_160_unique_synthetic_pending_cases_and_all_quotas():
    cases = runner.validate_vague_corpus(runner.DEFAULT_VAGUE_CASES)

    assert len(cases) == 160
    assert len({case["id"] for case in cases}) == 160
    assert all(case["synthetic"] is True for case in cases)
    assert {case["clinicalReviewStatus"] for case in cases} == {"PENDING"}
    assert {
        group: sum(case["group"] == group for case in cases)
        for group in runner.VAGUE_GROUP_QUOTAS
    } == runner.VAGUE_GROUP_QUOTAS

    tag_counts = {
        tag: sum(tag in case["tags"] for case in cases)
        for tag in {"NO_DIACRITICS", "TYPO", *runner.VAGUE_CROSS_TAG_MINIMUMS}
    }
    assert tag_counts["NO_DIACRITICS"] + tag_counts["TYPO"] >= 15
    for tag, minimum in runner.VAGUE_CROSS_TAG_MINIMUMS.items():
        assert tag_counts[tag] >= minimum
    assert {case["geminiMode"] for case in cases if case["group"] == "GEMINI_FAILURE"} == (
        runner.GEMINI_FAULT_MODES
    )
    assert {case["knownDefect"] for case in cases if case["knownDefect"]} == (
        runner.KNOWN_DEFECT_CODES
    )


def test_duplicate_ids_never_count_twice_toward_quotas():
    cases = json.loads(runner.DEFAULT_VAGUE_CASES.read_text(encoding="utf-8"))
    cases.append(deepcopy(cases[0]))

    with pytest.raises(runner.CorpusValidationError) as caught:
        runner.validate_vague_cases(cases)

    assert "duplicate id" in str(caught.value)


def test_invalid_review_status_and_actual_fields_are_actionable_validation_errors():
    cases = json.loads(runner.DEFAULT_VAGUE_CASES.read_text(encoding="utf-8"))
    cases[0]["clinicalReviewStatus"] = "APPROVED"
    cases[0]["actualOutcome"] = "RED"

    with pytest.raises(runner.CorpusValidationError) as caught:
        runner.validate_vague_cases(cases)

    message = str(caught.value)
    assert "clinicalReviewStatus must remain PENDING" in message
    assert "actual output fields" in message


def test_invalid_answered_question_shape_is_a_validation_error_not_a_crash():
    cases = json.loads(runner.DEFAULT_VAGUE_CASES.read_text(encoding="utf-8"))
    multi_turn = next(case for case in cases if case["turns"])
    multi_turn["turns"][0]["answeredQuestionIds"] = None

    with pytest.raises(runner.CorpusValidationError) as caught:
        runner.validate_vague_cases(cases)

    assert "answeredQuestionIds must be a string array" in str(caught.value)


def test_submitted_options_are_derived_from_matching_question_answer_provenance_in_question_order():
    turn = {
        "answeredQuestionIds": ["Q_BABY_TEMPERATURE", "Q_GLOBAL_DANGER"],
        "signals": {
            "SEIZURE": {
                "presence": "ABSENT",
                "provenance": "QUESTION_ANSWER",
                "sourceQuestionId": "Q_GLOBAL_DANGER",
                "sourceOptionCode": "DANGER_NONE",
            },
            "FEVER": [
                {
                    "presence": "PRESENT",
                    "provenance": "LLM_EXTRACTED_VALIDATED",
                    "sourceQuestionId": "Q_BABY_TEMPERATURE",
                    "sourceOptionCode": "TEMP_GTE_39",
                },
                {
                    "presence": "PRESENT",
                    "provenance": "QUESTION_ANSWER",
                    "sourceQuestionId": "Q_BABY_TEMPERATURE",
                    "sourceOptionCode": "TEMP_38_TO_39",
                },
            ],
        },
    }

    assert runner._derive_submitted_option_codes(turn, label="case.turns[0]") == [
        "TEMP_38_TO_39",
        "DANGER_NONE",
    ]


def test_missing_canonical_question_answer_option_is_a_clear_corpus_validation_error():
    turn = {
        "answeredQuestionIds": ["Q_GLOBAL_DANGER"],
        "signals": {
            "SEIZURE": {
                "presence": "ABSENT",
                "provenance": "LLM_EXTRACTED_VALIDATED",
                "sourceQuestionId": "Q_GLOBAL_DANGER",
                "sourceOptionCode": "DANGER_NONE",
            },
        },
    }

    with pytest.raises(runner.CorpusValidationError) as caught:
        runner._derive_submitted_option_codes(turn, label="case.turns[0]")

    message = str(caught.value)
    assert "case.turns[0]" in message
    assert "Q_GLOBAL_DANGER" in message
    assert "no QUESTION_ANSWER sourceOptionCode" in message

    cases = json.loads(runner.DEFAULT_VAGUE_CASES.read_text(encoding="utf-8"))
    authored_turn = next(
        turn
        for case in cases
        for turn in case["turns"]
        if turn.get("answeredQuestionIds")
    )
    authored_turn["signals"] = turn["signals"]
    with pytest.raises(runner.CorpusValidationError) as corpus_error:
        runner.validate_vague_cases(cases)
    assert "no QUESTION_ANSWER sourceOptionCode" in str(corpus_error.value)


def test_conflicting_canonical_question_answer_options_are_a_clear_corpus_validation_error():
    turn = {
        "answeredQuestionIds": ["Q_GLOBAL_DANGER"],
        "signals": {
            "SEIZURE": {
                "presence": "ABSENT",
                "provenance": "QUESTION_ANSWER",
                "sourceQuestionId": "Q_GLOBAL_DANGER",
                "sourceOptionCode": "DANGER_NONE",
            },
            "CYANOSIS": {
                "presence": "PRESENT",
                "provenance": "QUESTION_ANSWER",
                "sourceQuestionId": "Q_GLOBAL_DANGER",
                "sourceOptionCode": "DANGER_CYANOSIS",
            },
        },
    }

    with pytest.raises(runner.CorpusValidationError) as caught:
        runner._derive_submitted_option_codes(turn, label="case.turns[0]")

    message = str(caught.value)
    assert "case.turns[0]" in message
    assert "Q_GLOBAL_DANGER" in message
    assert "conflicting QUESTION_ANSWER sourceOptionCodes" in message

    cases = json.loads(runner.DEFAULT_VAGUE_CASES.read_text(encoding="utf-8"))
    authored_turn = next(
        turn
        for case in cases
        for turn in case["turns"]
        if turn.get("answeredQuestionIds")
    )
    authored_turn["signals"] = turn["signals"]
    with pytest.raises(runner.CorpusValidationError) as corpus_error:
        runner.validate_vague_cases(cases)
    assert "conflicting QUESTION_ANSWER sourceOptionCodes" in str(corpus_error.value)


def test_derived_option_must_belong_to_its_source_question():
    turn = {
        "answeredQuestionIds": ["Q_GLOBAL_DANGER"],
        "signals": {
            "SEIZURE": {
                "presence": "ABSENT",
                "provenance": "QUESTION_ANSWER",
                "sourceQuestionId": "Q_GLOBAL_DANGER",
                "sourceOptionCode": "STAGE_PREGNANCY",
            }
        },
    }

    with pytest.raises(runner.CorpusValidationError) as caught:
        runner._derive_submitted_option_codes(turn, label="case.turns[0]")

    assert "does not belong to Q_GLOBAL_DANGER" in str(caught.value)


def test_runner_applies_the_same_numeric_floor_as_the_runtime_api():
    cases = json.loads(runner.DEFAULT_VAGUE_CASES.read_text(encoding="utf-8"))

    for index, case_id in enumerate(("pediatric_018", "pediatric_019")):
        case = next(item for item in cases if item["id"] == case_id)
        result = runner._evaluate_vague_case(case, index)

        assert result["actual"]["finalDisposition"] == "RED", case_id
        assert result["actual"]["finalStage"] == "INFANT_0_12M", case_id


def test_expected_oracle_is_separate_and_byte_unchanged_after_actual_execution(vague_report):
    before = runner.DEFAULT_VAGUE_CASES.read_bytes()
    cases = runner.validate_vague_corpus(runner.DEFAULT_VAGUE_CASES)
    oracle = deepcopy(runner._oracle_projection(cases[0]))
    single_result = runner._evaluate_vague_case(cases[0], 0)
    expected_digest = vague_report["oracle"]["expectationSha256"]

    assert runner.DEFAULT_VAGUE_CASES.read_bytes() == before
    assert runner._oracle_projection(cases[0]) == oracle
    assert single_result["expected"] == oracle
    assert "actual" not in oracle
    assert vague_report["governance"]["expectedAuthoredBeforeActual"] is True
    assert expected_digest == runner._expectation_digest(
        cases
    )
    for result in vague_report["results"]:
        assert set(result) >= {"expected", "actual"}
        assert not any(key.lower().startswith("actual") for key in result["expected"])


def test_fault_modes_use_local_no_network_fixtures_and_fail_closed(vague_report, monkeypatch):
    def forbidden_network_client(*_args, **_kwargs):
        raise AssertionError("network client must not be created")

    monkeypatch.setattr("google.genai.Client", forbidden_network_client)
    cases = runner.validate_vague_corpus(runner.DEFAULT_VAGUE_CASES)
    timeout_case = next(case for case in cases if case["geminiMode"] == "TIMEOUT")
    guarded_result = runner._evaluate_vague_case(timeout_case, 999)
    fault_results = [
        result for result in vague_report["results"]
        if result["group"] == "GEMINI_FAILURE"
    ]

    assert {result["geminiMode"] for result in fault_results} == runner.GEMINI_FAULT_MODES
    assert guarded_result["actual"]["geminiFixture"]["networkClientCreated"] is False
    assert vague_report["metrics"]["networkClientCreatedCount"] == 0
    assert all(result["checks"]["faultNoNetwork"] for result in fault_results)
    for mode in {"TIMEOUT", "429", "5XX"}:
        assert any(
            result["actual"]["geminiFixture"]["callCount"] > 0
            for result in fault_results if result["geminiMode"] == mode
        )
    assert {
        result["geminiMode"]: result["actual"]["geminiFixture"]["observedFailure"]
        for result in fault_results
    } == {
        "OFF": "DISABLED",
        "TIMEOUT": "TIMEOUT",
        "429": "HTTP_429",
        "5XX": "HTTP_503",
    }


def test_multi_turn_state_carries_answers_and_repetition_is_measured(vague_report):
    multi_turn = [result for result in vague_report["results"] if "MULTI_TURN" in result["tags"]]

    assert len(multi_turn) >= 14
    assert all(len(result["actual"]["trajectory"]) >= 2 for result in multi_turn)
    assert any(
        "Q_GLOBAL_DANGER" in turn["answeredQuestionIds"]
        for result in multi_turn
        for turn in result["actual"]["trajectory"][1:]
    )
    assert vague_report["metrics"]["repeatedQuestionRate"] >= 0
    assert all("repeatedQuestion" in result["checks"] for result in multi_turn)


def test_open_short_trajectories_are_not_counted_as_finite_and_red_questions_are_na(vague_report):
    short_open = [
        result for result in vague_report["results"]
        if result["actual"]["turnsExecuted"] < result["expected"]["maxTurns"]
        and result["actual"]["stopConversation"] is False
    ]
    immediate_red = [
        result for result in vague_report["results"]
        if result["actual"]["finalDisposition"] == "RED"
        and not result["actual"]["allQuestions"]
    ]

    assert short_open
    assert all(result["checks"]["finiteTermination"] is None for result in short_open)
    assert vague_report["metrics"]["finiteTerminationEvaluatedCount"] < vague_report["total"]
    assert immediate_red
    assert all(result["checks"]["focusedQuestion"] is None for result in immediate_red)


def test_repeat_detection_covers_same_turn_and_already_answered_questions():
    trajectory = [
        {"questions": ["Q_GLOBAL_DANGER", "Q_GLOBAL_DANGER"], "answeredQuestionIds": []},
        {"questions": ["Q_CLOTS"], "answeredQuestionIds": ["Q_CLOTS"]},
    ]

    assert runner._repeated_questions(trajectory) == {"Q_GLOBAL_DANGER", "Q_CLOTS"}


def test_wrong_context_questions_are_scored_against_oracle_not_engine_prediction():
    trajectory = [{
        "target": "BABY",
        "stage": "INFANT_0_12M",
        "questions": ["Q_BABY_FEEDING"],
    }]

    wrong_entity, wrong_stage = runner._wrong_context_questions(
        trajectory, expected_target="MOTHER", expected_stage="PREGNANCY"
    )

    assert wrong_entity == {"Q_BABY_FEEDING"}
    assert wrong_stage == {"Q_BABY_FEEDING"}


def test_failed_dimensions_are_known_only_when_the_approved_defect_explains_them(vague_report):
    assert vague_report["findings"], "the Phase 1 corpus must expose current limitations"

    for finding in vague_report["findings"]:
        for dimension, classification in finding["classificationByDimension"].items():
            known = finding["knownDefect"]
            if classification == "NEW_FINDING":
                assert not known or dimension not in runner.KNOWN_DEFECT_DIMENSIONS[known]
            else:
                assert classification == known
                assert dimension in runner.KNOWN_DEFECT_DIMENSIONS[known]


def test_markdown_lists_every_failed_case_and_limitations(vague_report):
    markdown = runner.render_vague_markdown(vague_report)

    for finding in vague_report["findings"]:
        assert f"| {finding['caseId']} |" in markdown
    assert "## New discoveries" in markdown
    assert "## Limitations" in markdown
    assert "Clinical review status: **PENDING**" in markdown


def test_baseline_mode_returns_success_even_when_findings_exist(monkeypatch, capsys):
    report = {
        "failed": 3,
        "findings": [{"caseId": "synthetic_failure"}],
    }
    monkeypatch.setattr(runner, "evaluate_vague_baseline", lambda _path: report)
    monkeypatch.setattr(sys, "argv", ["runner", "--mode", "vague-baseline"])

    assert runner.main() == 0
    assert json.loads(capsys.readouterr().out)["failed"] == 3


def test_markdown_output_cannot_overwrite_json_output(monkeypatch, tmp_path):
    same_path = tmp_path / "baseline.md"
    monkeypatch.setattr(
        sys, "argv", ["runner", "--mode", "vague-baseline", "--output", str(same_path)]
    )

    with pytest.raises(SystemExit) as caught:
        runner.main()

    assert caught.value.code == 2
    assert not same_path.exists()


def test_legacy_default_report_remains_green_and_schema_compatible():
    report = runner.evaluate(runner.DEFAULT_CASES)

    assert report["total"] == 14
    assert report["failed"] == 0
    assert set(report) == {"governance", "total", "passed", "failed", "metrics", "coverage", "results"}
    assert report["coverage"]["allRequiredCategoriesCovered"] is True
    assert report["coverage"]["allRequiredMetricFieldsReported"] is True
