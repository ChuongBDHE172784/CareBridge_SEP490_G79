"""V2's danger floor: what it must catch without Gemini, and what it must not invent.

Both halves matter equally. The floor exists because a self-harm disclosure produced only a
generic clarification when extraction was unavailable; it earns its place only for as long as
it does not also turn ordinary sentences into emergencies.
"""

from __future__ import annotations

import json
import pathlib
import uuid

import pytest

import app.triage.api as api
from app.rules.registry import get_registry
from app.triage.deterministic_signals import detect_danger_signals, merge_as_floor


CORPUS = json.loads(
    (pathlib.Path(__file__).parent / "data" / "vietnamese_intake_corpus.json").read_text(
        encoding="utf-8"
    )
)
#: The corpus speaks the V1 wire vocabulary; V2 takes the entity-split stage names.
_STAGE = {
    "POSTPARTUM": ("POSTPARTUM_MOTHER", {"postpartumDay": 10}, "MOTHER"),
    "PREGNANCY": ("PREGNANCY", {"gestationalWeek": 28}, "MOTHER"),
    "PRECONCEPTION": ("PRECONCEPTION", {}, "MOTHER"),
}


def _turn(message: str, stage: str):
    care_stage, extra, target = _STAGE[stage]
    payload = {
        "sessionId": str(uuid.uuid4()),
        "stateVersion": 0,
        "expectedStateVersion": 0,
        "requestId": "request_" + uuid.uuid4().hex[:16],
        "messageId": "message_" + uuid.uuid4().hex[:16],
        "latestUserMessage": message,
        "selectedTarget": target,
        "journeyContext": {"stage": care_stage, **extra},
        # Empty: this is a turn where extraction produced nothing.
        "signals": {},
        "measurements": {},
        "expectedRulesetHash": get_registry().ruleset_sha256,
    }
    return api.execute_turn(api.TriageTurnRequest.model_validate(payload)).state


@pytest.mark.parametrize(
    ("message", "code"),
    [
        # The case this module was written for: previously only Q_CLARIFY_INTENT.
        ("Tôi không muốn sống nữa", "SELF_HARM_IDEATION"),
        ("Tôi muốn chết", "SELF_HARM_IDEATION"),
        ("Tôi bị co giật", "SEIZURE"),
        ("Tôi khó thở dữ dội", "SEVERE_BREATHING_DIFFICULTY"),
        ("Môi tôi tím tái", "CYANOSIS"),
        ("Bé nhà em li bì khó đánh thức", "ALTERED_CONSCIOUSNESS"),
        # Accent-free typing must reach the same floor.
        ("toi bi co giat", "SEIZURE"),
    ],
)
def test_danger_phrase_becomes_a_signal(message, code):
    assert code in detect_danger_signals(message)


@pytest.mark.parametrize(
    "message",
    [case["text"] for case in CORPUS["cases"] if case["expect"] == "NOT_RED"],
)
def test_ordinary_sentences_produce_no_danger_signal(message):
    assert detect_danger_signals(message) == {}


def test_floor_never_overwrites_a_richer_observation():
    """An explicit ABSENT from a question answer outranks a phrase match, not the reverse."""

    answered = {
        "SEIZURE": {
            "presence": "ABSENT",
            "provenance": "QUESTION_ANSWER",
            "sourceQuestionId": "Q_GLOBAL_DANGER",
        }
    }
    merged = merge_as_floor(answered, detect_danger_signals("Tôi bị co giật"))

    assert merged["SEIZURE"]["presence"] == "ABSENT"
    assert merged["SEIZURE"]["provenance"] == "QUESTION_ANSWER"


def test_floor_fills_only_what_is_missing():
    merged = merge_as_floor(
        {"SEIZURE": {"presence": "ABSENT"}},
        detect_danger_signals("Tôi bị co giật và khó thở dữ dội"),
    )

    assert merged["SEIZURE"]["presence"] == "ABSENT"
    assert merged["SEVERE_BREATHING_DIFFICULTY"]["presence"] == "PRESENT"


@pytest.mark.parametrize("message", ["", "   ", None, 123])
def test_non_text_input_asserts_nothing(message):
    assert detect_danger_signals(message) == {}


def test_self_harm_disclosure_reaches_safety_support_without_gemini(monkeypatch):
    """End to end: the turn that used to answer with a clarification question."""

    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    state = _turn("Tôi không muốn sống nữa", "POSTPARTUM")

    assert state.get("triageOutcome") == "RED"
    assert state.get("requiredAction") == "IMMEDIATE_SAFETY_SUPPORT"
    assert "SAFETY_RISK_SELF_OR_INFANT_HARM" in (state.get("reasonCodes") or [])


@pytest.mark.parametrize(
    "case",
    [c for c in CORPUS["cases"] if c["expect"] == "NOT_RED"],
    ids=lambda c: c["text"][:38],
)
def test_ordinary_sentences_do_not_reach_red_in_v2(case, monkeypatch):
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    state = _turn(case["text"], case["stage"])

    assert state.get("triageOutcome") != "RED", (
        f"{case['text']!r} escalated on {state.get('reasonCodes')}"
    )


# --------------------------------------------------------------- lexicon additions 2026-08-15


_TRUSTED_PREGNANCY = {"stage": "PREGNANCY", "stage_source": "EXPLICIT_SELECTED_PROFILE"}


@pytest.mark.parametrize(
    "message",
    [
        # Từ Dũ's own wording for postpartum haemorrhage, added under D-029/D-031.
        "Chảy máu lượng lớn từ âm đạo",
        "Em đang bầu, chảy máu lượng lớn không dứt",
        # Từ Dũ's quantified definition of heavy bleeding in pregnancy, added 2026-08-15.
        "Em phải thay băng vệ sinh chỉ sau 1-2 giờ",
        # Từ Dũ words the postpartum warning as "Nếu sản dịch ra nhiều (1 giờ mà ướt đẫm băng vệ
        # sinh) thì nên đi khám lại ngay."
        # (https://www.tudu.com.vn/vn/y-hoc-thuong-thuc/suc-khoe-sau-sanh-sau-mo/
        #  nhung-van-de-thuong-gap-o-giai-doan-hau-san/, truy cập 2026-08-15).
        # No phrase was added for it: "ướt đẫm băng" already covers "ướt đẫm băng vệ sinh", the
        # word boundary falling after "băng". Pinned so that coverage is not lost by accident.
        "Em đang bầu, 1 giờ mà ướt đẫm băng vệ sinh",
        "sản dịch ra nhiều ướt đẫm băng vệ sinh",
    ],
)
def test_official_heavy_bleeding_wording_reaches_the_floor(message):
    assert "HEAVY_VAGINAL_BLEEDING" in detect_danger_signals(message, **_TRUSTED_PREGNANCY)


@pytest.mark.parametrize(
    "message",
    [
        # A smaller volume is not the danger sign, and the negated form is not a report.
        "Chảy máu lượng nhỏ thôi",
        "chảy máu lượng nhỏ",
        "Em không chảy máu lượng lớn",
        "Bé bị chảy máu cam lượng ít",
        # A different construction, not this phrase.
        "băng đẫm máu",
        # Changing a pad is ordinary; only the frequency Từ Dũ names is the danger sign. The
        # sourced phrase is kept long precisely so these two stay out.
        "Em thay băng vệ sinh 2 lần một ngày như bình thường",
        "Bao lâu thì nên thay băng vệ sinh?",
        # The same Từ Dũ page carries both instructions, which is what makes it a good anchor:
        # "thay băng mỗi 3 – 4 giờ 1 lần" is hygiene, "trong vòng 1 giờ thấm ướt 2 – 3 miếng BVS
        # dày" is the warning. Only the second is a danger sign, and routine pad-changing at any
        # interval must stay out of the floor.
        # (https://www.tudu.com.vn/vn/y-hoc-thuong-thuc/suc-khoe-sau-sanh-sau-mo/
        #  cham-soc-ve-sinh-dinh-duong-van-dong-hau-phau-mo-lay-thai/, truy cập 2026-08-15)
        "Thay băng vệ sinh mỗi 4 tiếng cho sạch",
        "Con phải thay băng 3-4 giờ một lần",
        "thay băng thường xuyên",
    ],
)
def test_heavy_bleeding_wording_does_not_over_match(message):
    assert detect_danger_signals(message, **_TRUSTED_PREGNANCY) == {}


def test_bleeding_in_pregnancy_matches_however_the_writer_labels_it():
    """How the writer explains the bleeding does not change what the phrase reports.

    "chảy máu lượng lớn kinh nguyệt trong mang thai tuần 20" was proposed as a false positive
    and is not one. Bệnh viện Từ Dũ lists vaginal bleeding in pregnancy as a danger sign in its
    own right — "Ra máu âm đạo trong thai kỳ thì bạn cần đến ngay cơ sở y tế…vì có thể chảy máu
    nhiều gây nguy hiểm đến tính mạng mẹ và con"
    (https://www.tudu.com.vn/vn/y-hoc-thuong-thuc/suc-khoe-thai-ky/nhung-dau-hieu-nguy-hiem-trong-thai-ky/,
    truy cập 2026-08-15) — so a user calling it a period does not make it safe. Ruled by the
    clinician on the project, 2026-08-15; the matcher does not get to decide this.
    """

    assert "HEAVY_VAGINAL_BLEEDING" in detect_danger_signals(
        "chảy máu lượng lớn kinh nguyệt trong mang thai tuần 20", **_TRUSTED_PREGNANCY
    )


# TODO chờ D-032 (mở nhóm nhi khoa cho sàn cụm từ). Cho tới khi có quyết định, "thở rút lõm"
# (CHEST_INDRAWING, pediatric_011) và "nôn liên tục" (PERSISTENT_VOMITING, pediatric_016) không
# được thêm: hai signal đó có rule đọc nhưng _STAGE_PHRASE_SIGNALS chưa nối dây cho giai đoạn
# nhi khoa, nên thêm cụm từ vào cũng không chạy.


@pytest.mark.parametrize(
    "message",
    [
        # The corpus case this fixed: a count between "ướt đẫm" and "băng" used to stop the match,
        # so describing the bleeding more precisely made the rule stop firing.
        "Em đang bầu, máu ra ướt đẫm hai miếng băng chỉ trong một lúc.",
        "ướt đẫm 3 miếng băng",
        "ướt đẫm ba miếng băng vệ sinh",
        "ướt đẫm nhiều băng",
        "thấm ướt hai băng",
        "ướt đẫm băng",
    ],
)
def test_a_counted_pad_still_matches_the_soaking_phrase(message):
    assert "HEAVY_VAGINAL_BLEEDING" in detect_danger_signals(message, **_TRUSTED_PREGNANCY)


@pytest.mark.parametrize(
    "message",
    [
        # The quantity gap is allowed only inside the pad-soaking phrases. Applied to every
        # phrase it made "co hai giật mình" read as a seizure, which is why it is scoped.
        "co hai giật mình",
        "Em giặt hai cái băng cho bé",
        "Em mua năm miếng băng dán",
        "Tôi tự nhiên thấy vài cái tử tế",
        "Bé bú từ từ hai lần",
        # A count with nothing countable after it does not complete "ướt đẫm băng".
        "ướt đẫm một",
        # Words that are neither an intensifier nor a count still break the phrase.
        "đau đầu nhẹ vừa dữ dội",
    ],
)
def test_a_count_cannot_bridge_two_unrelated_words(message):
    assert detect_danger_signals(message, **_TRUSTED_PREGNANCY) == {}


@pytest.mark.parametrize(
    "message",
    [
        # Refused on 2026-08-15 for want of a source that meets D-029/D-031. Pinned so the
        # refusal is visible if someone adds them later without recording a source.
        "Em bầu 31 tuần, đầu đau dữ dội",
        "nhìn mọi thứ nhòe đi",
        "máu chảy ào ra và không giảm",
    ],
)
def test_wording_refused_for_want_of_a_source_stays_out(message):
    assert detect_danger_signals(message, **_TRUSTED_PREGNANCY) == {}
