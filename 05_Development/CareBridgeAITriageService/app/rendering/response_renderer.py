"""Deterministic template renderer.

Every user-facing sentence comes from a fixed template keyed by
(outcome, reason_code, action_code). No model writes clinical prose, so the output can
never acquire a disease name, a cause, a medication, a dose or a reassurance that was
not reviewed.

The banned-phrase guard below is a belt-and-braces check: if a template is ever edited
to say "bạn an toàn" or "không cần đi khám", rendering fails loudly instead of shipping it.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

TEMPLATE_VERSION = "2.1.0"

#: Phrases no triage output may contain, in any template.
#:
#: The first four are outright reassurances. The last three were added after review: an
#: earlier NEEDS_MORE_INFO template said "Chưa ghi nhận dấu hiệu nguy cơ khẩn cấp" and
#: "đảm bảo an toàn tuyệt đối". Both read as a negative finding, but NEEDS_MORE_INFO means
#: the system could NOT stratify — it is not evidence that anything was ruled out.
BANNED_PHRASES = (
    "bạn an toàn",
    "hoàn toàn an toàn",
    "hoàn toàn khỏe mạnh",
    "không có bệnh",
    "không mắc bệnh",
    "không cần đi khám",
    "không cần khám",
    "chưa ghi nhận dấu hiệu nguy cơ khẩn cấp",
    "an toàn tuyệt đối",
    "chưa phát hiện các dấu hiệu cảnh báo",
)

DISCLAIMER = (
    "Thông tin này chỉ mang tính tham khảo, không phải chẩn đoán và không thay thế "
    "nhân viên y tế."
)


#: Matrix columns 14 ("Lý do hiển thị") and 16 ("Mẫu hành động") could not be transcribed
#: verbatim, so no template below is a clinically signed wording yet. Every rendered
#: response carries this flag until those two columns are captured and reviewed.
CLINICAL_SIGN_OFF = "PROVISIONAL_NOT_CLINICALLY_SIGNED"


@dataclass(frozen=True)
class RenderedResponse:
    template_id: str
    template_version: str
    headline: str
    message: str
    limitations: tuple[str, ...]
    clinical_sign_off: str = CLINICAL_SIGN_OFF


_TEMPLATES: dict[str, tuple[str, str]] = {
    "RED.IMMEDIATE_EMERGENCY_ASSESSMENT": (
        "Dấu hiệu cần được xử trí khẩn cấp",
        "Từ thông tin bạn cung cấp, đây là tình huống cần được nhân viên y tế đánh giá "
        "ngay lập tức. Hãy đến cơ sở y tế gần nhất hoặc gọi cấp cứu ngay.",
    ),
    "RED.IMMEDIATE_SAFETY_SUPPORT": (
        "Bạn cần được hỗ trợ ngay lúc này",
        "Hãy liên hệ ngay với người thân, nhân viên y tế hoặc đường dây hỗ trợ khẩn cấp. "
        "Nếu bạn đang ở một mình và không thấy an toàn, hãy tìm người ở cạnh bạn ngay.",
    ),
    "YELLOW.EARLY_CLINICAL_ASSESSMENT": (
        "Bạn nên được nhân viên y tế đánh giá sớm",
        "Từ thông tin hiện có, bạn nên liên hệ cơ sở y tế để được đánh giá sớm. "
        "Nếu dấu hiệu nặng lên, hãy đi khám ngay.",
    ),
    # NEEDS_MORE_INFO must never read as a negative finding. It means the system could not
    # stratify — not that anything was ruled out. Every variant says so explicitly.
    "NEEDS_MORE_INFO.ASK_CLARIFYING_QUESTIONS": (
        "Chưa đủ thông tin để phân tầng nguy cơ an toàn",
        "Hệ thống chưa có đủ thông tin hoặc bộ quy tắc hiện tại chưa đủ phạm vi để xác định "
        "mức nguy cơ. Kết quả này không có nghĩa rằng triệu chứng là an toàn.",
    ),
    "NEEDS_MORE_INFO.ROUTE_TO_HEALTHCARE_WORKER": (
        "Chưa đủ thông tin để phân tầng nguy cơ an toàn",
        "Hệ thống chưa có đủ thông tin hoặc bộ quy tắc hiện tại chưa đủ phạm vi để xác định "
        "mức nguy cơ. Kết quả này không có nghĩa rằng triệu chứng là an toàn.",
    ),
    "NEEDS_MORE_INFO.SYSTEM_UNAVAILABLE": (
        "Tạm thời chưa thể hoàn tất định hướng",
        "Hệ thống hiện chưa thể hoàn tất kiểm tra an toàn. Kết quả này không có nghĩa rằng triệu chứng là an toàn; vui lòng thử lại hoặc liên hệ nhân viên y tế.",
    ),
    # Green release gate is OFF: no RED/YELLOW rule matched, but the approved rule set does
    # not cover enough danger signs to call this low risk. Same wording as any other
    # NEEDS_MORE_INFO — the user must not read a locked gate as a clean result.
    "NEEDS_MORE_INFO.GREEN_RELEASE_GATE_DISABLED": (
        "Chưa đủ thông tin để phân tầng nguy cơ an toàn",
        "Hệ thống chưa có đủ thông tin hoặc bộ quy tắc hiện tại chưa đủ phạm vi để xác định "
        "mức nguy cơ. Kết quả này không có nghĩa rằng triệu chứng là an toàn.",
    ),
    "NEEDS_MORE_INFO.UNRESOLVED_RED_SIGNAL": (
        "Có dấu hiệu cần được làm rõ hoặc đánh giá sớm",
        "Bạn đã cung cấp một dấu hiệu có thể cần được nhân viên y tế đánh giá. Do thiếu "
        "thông tin quan trọng, hệ thống không thể phân tầng an toàn.",
    ),
    "GREEN.ROUTINE_MONITORING_PATH": (
        "Chưa phát hiện dấu hiệu nguy cơ cao từ thông tin hiện có",
        "Hãy tiếp tục theo dõi và khám định kỳ theo lịch hẹn. Nếu xuất hiện dấu hiệu mới "
        "hoặc dấu hiệu nặng lên, hãy đánh giá lại ngay.",
    ),
    "OUT_OF_SCOPE.ROUTE_OUT_OF_SCOPE": (
        "Nội dung này nằm ngoài phạm vi hỗ trợ",
        "Trợ lý này chỉ hỗ trợ các vấn đề sức khỏe sinh sản: chuẩn bị mang thai, nghi ngờ "
        "mang thai, thai kỳ và sau sinh. Với vấn đề bạn mô tả, hãy liên hệ cơ sở y tế phù hợp.",
    ),
}

_LIMITATIONS = (
    "Đây không phải chẩn đoán bệnh.",
    "Hệ thống không kê đơn và không hướng dẫn điều trị.",
    "Kết quả dựa trên thông tin bạn cung cấp và có thể chưa đầy đủ.",
)


class TemplateNotFoundError(KeyError):
    """Raised when no approved template covers an (outcome, action) pair."""


class BannedPhraseError(ValueError):
    """Raised when a template would ship a reassurance the system must never make."""


def render(outcome: str, action_code: str, reason_codes: tuple[str, ...] = ()) -> RenderedResponse:
    """Render the fixed text for a verdict. Never falls back to free-form prose."""

    # The release-gate case shares outcome+action with the generic route-out template,
    # so the reason code selects the more specific wording.
    if "GREEN_RELEASE_GATE_DISABLED" in reason_codes:
        template_id = "NEEDS_MORE_INFO.GREEN_RELEASE_GATE_DISABLED"
    elif "UNRESOLVED_RED_SIGNAL" in reason_codes and outcome == "NEEDS_MORE_INFO":
        template_id = "NEEDS_MORE_INFO.UNRESOLVED_RED_SIGNAL"
    else:
        template_id = f"{outcome}.{action_code}"

    if template_id not in _TEMPLATES:
        raise TemplateNotFoundError(
            f"no approved template for {template_id}; refusing to generate free text"
        )

    headline, message = _TEMPLATES[template_id]
    _assert_no_banned_phrase(headline, message)
    return RenderedResponse(
        template_id=template_id,
        template_version=TEMPLATE_VERSION,
        headline=headline,
        message=message,
        limitations=_LIMITATIONS,
    )


def _assert_no_banned_phrase(*fragments: str) -> None:
    haystack = " ".join(fragments).lower()
    haystack = re.sub(r"\s+", " ", haystack)
    for phrase in BANNED_PHRASES:
        if phrase in haystack:
            raise BannedPhraseError(f"template contains a forbidden reassurance: {phrase!r}")
