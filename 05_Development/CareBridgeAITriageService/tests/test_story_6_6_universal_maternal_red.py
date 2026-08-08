import pytest

from app import main as main_module
from app.graph import run_triage
from app.main import continue_intake, start_intake
from app.schemas import ChildTriageRequest, IntakeContinueRequest, IntakeStartRequest


MATERNAL_STAGES = ("PRECONCEPTION", "PREGNANCY")


def maternal_request(stage: str, **overrides) -> ChildTriageRequest:
    data = {
        "stage": stage,
        "symptomList": [],
        "duration": "1 giờ",
        "breathingStatus": "bình thường",
        "consciousnessStatus": "tỉnh táo",
        "seizure": False,
        "parentFreeText": "Tôi hơi mệt",
    }
    data.update(overrides)
    return ChildTriageRequest(**data)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    ("overrides", "rule_suffix"),
    [
        ({"breathingStatus": "khó thở"}, "BREATHING_DISTRESS"),
        ({"breathingStatus": "tím tái"}, "CYANOSIS"),
        ({"seizure": True}, "SEIZURE"),
        ({"consciousnessStatus": "ngất xỉu"}, "ALTERED_CONSCIOUSNESS"),
        ({"parentFreeText": "Tôi đang chảy máu nhiều"}, "HEAVY_BLEEDING"),
        ({"parentFreeText": "Tôi sẽ tự sát ngay bây giờ"}, "SELF_HARM"),
        ({"parentFreeText": "I have difficulty breathing"}, "BREATHING_DISTRESS"),
        ({"parentFreeText": "I am suicidal and may harm myself now"}, "SELF_HARM"),
    ],
)
def test_approved_universal_signs_are_deterministic_red(stage, overrides, rule_suffix):
    response = run_triage(
        maternal_request(stage, **overrides),
        deterministic_only=True,
    )

    assert response.stage == stage
    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True
    assert f"RED_{stage}_{rule_suffix}" in response.matchedRules
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert response.assistantFallbackUsed is True


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "canonical_code",
    ["difficulty_breathing", "cyanosis", "seizure", "lethargy", "difficult_to_wake"],
)
def test_canonical_universal_signs_are_red_for_each_maternal_stage(
    stage, canonical_code
):
    response = run_triage(
        maternal_request(
            stage,
            symptomList=[canonical_code],
            parentFreeText=None,
        ),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_universal_red_precedes_every_gemini_operation(stage, monkeypatch):
    calls: list[str] = []

    class ExplodingGeminiClient:
        reachable = True

        def __getattr__(self, name):
            def fail(**_kwargs):
                calls.append(name)
                raise AssertionError(f"Gemini must not run before deterministic RED: {name}")

            return fail

    monkeypatch.setattr(main_module, "get_gemini_client", ExplodingGeminiClient)

    response = start_intake(IntakeStartRequest(
        currentIntake=maternal_request(
            stage,
            parentFreeText="Tôi khó thở và đang chảy máu nhiều",
        )
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert calls == []


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không khó thở và không tím tái",
        "Tôi không co giật hay ngất xỉu",
        "Tôi không bị chảy máu nhiều",
        "Tôi không muốn tự hại và không có ý định tự sát",
        "I do not have difficulty breathing and I have no cyanosis",
        "I am not suicidal and do not intend self-harm",
        "I don't have difficulty breathing",
        "She doesn't have heavy bleeding",
        "She isn't unconscious",
        "She hasn't had a seizure",
        "I won't kill myself",
        "I don’t have blue lips",
    ],
)
def test_negated_universal_signs_do_not_trigger_red(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không khó thở và chảy máu nhiều",
        "I have no breathing difficulty and heavy bleeding",
    ],
)
def test_affirmed_sign_after_conjunction_is_not_suppressed_by_earlier_negation(
    stage, parent_free_text
):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert f"RED_{stage}_HEAVY_BLEEDING" in response.matchedRules
    assert f"RED_{stage}_BREATHING_DISTRESS" not in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không khó thở và không chảy máu nhiều",
        "I have no breathing difficulty and no heavy bleeding",
    ],
)
def test_each_negated_sign_across_conjunction_remains_non_red(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    ("parent_free_text", "rule_suffix"),
    [
        ("I cannot breathe", "BREATHING_DISTRESS"),
        ("I can't breathe", "BREATHING_DISTRESS"),
        ("My blue lips are getting worse", "CYANOSIS"),
        ("I have altered consciousness", "ALTERED_CONSCIOUSNESS"),
        ("I am difficult to wake", "ALTERED_CONSCIOUSNESS"),
        ("I am soaking pad after pad", "HEAVY_BLEEDING"),
        ("I have a hemorrhage", "HEAVY_BLEEDING"),
        ("I have a haemorrhage", "HEAVY_BLEEDING"),
        ("I want to die", "SELF_HARM"),
    ],
)
def test_python_recognizes_the_java_approved_maternal_synonyms(
    stage, parent_free_text, rule_suffix
):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert f"RED_{stage}_{rule_suffix}" in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "I did not say I cannot breathe",
        "I do not have blue lips",
        "I do not have altered consciousness",
        "I am not difficult to wake",
        "I am not soaking pad after pad",
        "I do not have a haemorrhage",
        "I do not want to die",
        "Patient denies blue lips and denies heavy bleeding",
    ],
)
def test_negated_java_approved_synonyms_do_not_trigger_red(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_java_breathing_difficulty_synonym_has_exact_single_rule(stage):
    response = run_triage(
        maternal_request(stage, parentFreeText="I have breathing difficulty"),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert response.redFlags == ["Khó thở"]
    assert response.matchedRules == [f"RED_{stage}_BREATHING_DISTRESS"]


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_negated_java_breathing_difficulty_synonym_is_not_red(stage):
    response = run_triage(
        maternal_request(stage, parentFreeText="I do not have breathing difficulty"),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không không thở được",
        "I am not unable to breathe",
    ],
)
def test_external_negation_suppresses_an_internal_negative_danger_phrase(
    stage, parent_free_text
):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_java_breathing_difficulty_synonym_precedes_every_gemini_operation(
    stage, monkeypatch
):
    calls: list[str] = []

    class ExplodingGeminiClient:
        reachable = True

        def __getattr__(self, name):
            def fail(**_kwargs):
                calls.append(name)
                raise AssertionError(f"Gemini must not run before deterministic RED: {name}")

            return fail

    monkeypatch.setattr(main_module, "get_gemini_client", ExplodingGeminiClient)

    response = start_intake(IntakeStartRequest(
        currentIntake=maternal_request(stage, parentFreeText="I have breathing difficulty")
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert response.triageResult.matchedRules == [f"RED_{stage}_BREATHING_DISTRESS"]
    assert calls == []


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "I cannot breathe",
        "My lips are blue lips",
        "I have altered consciousness",
        "I am soaking pad after pad",
        "I want to die",
    ],
)
def test_java_approved_synonyms_also_precede_every_gemini_operation(
    stage, parent_free_text, monkeypatch
):
    calls: list[str] = []

    class ExplodingGeminiClient:
        reachable = True

        def __getattr__(self, name):
            def fail(**_kwargs):
                calls.append(name)
                raise AssertionError(f"Gemini must not run before deterministic RED: {name}")

            return fail

    monkeypatch.setattr(main_module, "get_gemini_client", ExplodingGeminiClient)

    response = start_intake(IntakeStartRequest(
        currentIntake=maternal_request(stage, parentFreeText=parent_free_text)
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert calls == []


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_non_danger_maternal_input_asks_stage_appropriate_questions(stage, monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = start_intake(IntakeStartRequest(currentIntake=maternal_request(
        stage,
        duration=None,
        parentFreeText="Tôi hơi mệt",
    )))

    assert response.status == "ASK_MORE"
    assert response.stage == stage
    assert {question.questionKey for question in response.questions} <= {
        "parentFreeText",
        "duration",
    }
    assert all("trẻ" not in question.text.lower() for question in response.questions)
    assert response.triageResult is None


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_complete_non_danger_maternal_input_terminalizes_yellow_at_limit(
    stage, monkeypatch
):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = continue_intake(IntakeContinueRequest(
        intakeSessionId=f"{stage.lower()}-round-limit",
        currentIntake=maternal_request(stage),
        newAnswers={},
        round=3,
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.stage == stage
    assert response.questions == []
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "YELLOW"
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules
    assert f"{stage}_RULES_NEED_CLINICAL_REVIEW" in response.triageResult.matchedRules
    expected_context = {
        "PRECONCEPTION": "trước khi mang thai",
        "PREGNANCY": "trong thai kỳ",
    }[stage]
    assert expected_context in response.triageResult.summary
    assert "trẻ" not in response.triageResult.summary.lower()
    assert "trẻ" not in response.triageResult.recommendedAction.lower()


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_incomplete_non_danger_maternal_input_terminalizes_yellow_at_limit(
    stage, monkeypatch
):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = continue_intake(IntakeContinueRequest(
        intakeSessionId=f"{stage.lower()}-incomplete-round-limit",
        currentIntake=maternal_request(stage, duration=None),
        newAnswers={},
        round=3,
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "YELLOW"
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules


@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Thai giảm cử động",
        "Tôi đau bụng dữ dội",
    ],
)
def test_unsigned_pregnancy_specific_signs_remain_inactive(parent_free_text):
    """These two still have no rule in either engine, so neither may escalate.

    "Đau đầu dữ dội + rối loạn thị giác" used to be pinned here too. It was activated under
    D-030 by porting PREG_RED_002 from the registry — see the test below. The other two are
    not merely unported: the registry declares no rule for them at all, so activating them
    would mean inventing a threshold rather than moving one.
    """

    response = run_triage(
        maternal_request("PREGNANCY", parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "NEED_MORE_INFO"
    assert response.emergencyActionRequired is False
    assert response.matchedRules == ["PREGNANCY_RULES_NEED_CLINICAL_REVIEW"]


@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi đau đầu dữ dội và hoa mắt",
        "Tôi bị phù nề, đau đầu nhiều, mờ mắt",
        "Em nhức đầu dữ dội, nhìn mờ",
    ],
)
def test_pregnancy_headache_with_visual_disturbance_is_red(parent_free_text):
    """PREG_RED_002 ported to V1 under D-030. Registry wording, registry conjunction."""

    response = run_triage(
        maternal_request("PREGNANCY", parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True
    assert "RED_PREGNANCY_NEURO_DANGER" in response.matchedRules


@pytest.mark.parametrize(
    ("stage", "parent_free_text"),
    [
        # One half alone is not the rule. This is what keeps a common complaint from becoming
        # an emergency: headaches and "hoa mắt" are each ordinary in pregnancy.
        ("PREGNANCY", "Tôi bị đau đầu nhiều"),
        ("PREGNANCY", "Tôi bị hoa mắt"),
        ("PREGNANCY", "Tôi hơi mờ mắt khi đứng dậy"),
        ("PREGNANCY", "Tôi không đau đầu và không mờ mắt"),
        # The registry scopes it to pregnancy; the same two signs elsewhere are a different
        # question this rule does not answer.
        ("POSTPARTUM", "Tôi đau đầu dữ dội và mờ mắt"),
        ("PRECONCEPTION", "Tôi đau đầu dữ dội và mờ mắt"),
    ],
)
def test_neuro_danger_needs_both_halves_and_the_right_stage(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert "RED_PREGNANCY_NEURO_DANGER" not in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    ("parent_free_text", "rule_suffix"),
    [
        # Saying the bleeding is worse used to stop the rule firing: the phrase was matched as
        # a literal substring, so any degree word between "máu" and "nhiều" broke it.
        ("Tôi ra máu nhiều", "HEAVY_BLEEDING"),
        ("Tôi ra máu rất nhiều", "HEAVY_BLEEDING"),
        ("Tôi ra máu quá nhiều", "HEAVY_BLEEDING"),
        ("Tôi chảy máu rất nhiều", "HEAVY_BLEEDING"),
        ("Tôi chảy máu cực kỳ nhiều", "HEAVY_BLEEDING"),
        ("Máu ra nhiều lắm", "HEAVY_BLEEDING"),
        ("Tôi bị ra máu ồ ạt", "HEAVY_BLEEDING"),
        ("Tôi ướt đẫm băng vệ sinh trong một giờ", "HEAVY_BLEEDING"),
        ("Tôi rất khó thở", "BREATHING_DISTRESS"),
        # Indirect self-harm wording; only the literal "muốn chết" was recognised before.
        ("Tôi không muốn sống nữa", "SELF_HARM"),
        ("Tôi nghĩ đến chuyện kết liễu đời mình", "SELF_HARM"),
    ],
)
def test_degree_words_inside_a_danger_phrase_stay_red(stage, parent_free_text, rule_suffix):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True
    assert f"RED_{stage}_{rule_suffix}" in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        # Accent-stripped matching merged these onto the danger phrases and fired RED on
        # ordinary sentences. Written WITH diacritics they are the writer's own words:
        "Tôi có giặt quần áo cho bé mỗi ngày",  # "có giặt"  vs "co giật"
        "Tôi đang ngắt sữa cho bé",  # "ngắt"     vs "ngất"
        "Tôi mới tìm hiểu về cách cho bú",  # "mới tìm"  vs "môi tím"
        "Bé bú từ từ, mẹ yên tâm",  # "từ từ"    vs "tự tử"
        "Quần áo của bé ra màu nhiều khi giặt",  # "ra màu"   vs "ra máu"
        "Tôi kê cái tủ sát tường",  # "tủ sát"   vs "tự sát"
        "Phòng thơm ngát mùi sả",  # "ngát"     vs "ngất"
        # A degree word may bridge a phrase, but an arbitrary word may not.
        "Mọi người xung quanh tự nhiên tử tế hẳn lên",  # must not read as "tự tử"
    ],
)
def test_diacritics_keep_ordinary_words_out_of_the_red_rules(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "NEED_MORE_INFO"
    assert response.emergencyActionRequired is False
    assert not [rule for rule in response.matchedRules if rule.startswith("RED_")]


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    ("parent_free_text", "rule_suffix"),
    [
        ("toi ra mau rat nhieu", "HEAVY_BLEEDING"),
        ("toi bi co giat", "SEIZURE"),
        ("toi kho tho", "BREATHING_DISTRESS"),
    ],
)
def test_accent_free_typing_is_still_matched(stage, parent_free_text, rule_suffix):
    """Phones default to accent-free input; that spelling cannot be told apart, so it escalates."""

    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert f"RED_{stage}_{rule_suffix}" in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không ra máu nhiều",
        # The negation sits inside the phrase's own gap, where the preceding-clause scan
        # cannot see it, so the gap must refuse to bridge it.
        "Tôi ra máu không nhiều",
        "Tôi không bị co giật",
    ],
)
def test_negation_still_suppresses_red_after_the_gap_change(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert not [rule for rule in response.matchedRules if rule.startswith("RED_")]
