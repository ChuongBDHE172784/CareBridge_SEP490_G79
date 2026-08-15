"""Emergency hotlines and the fixed copy shown alongside them.

Every number here was checked against an official page on 2026-08-15 and carries its source in
the record. The whitelist exists so a number can never reach a user without having been through
that check: the service asserts every registry entry against it before rendering, so adding a
hotline in one place and forgetting the other fails loudly rather than quietly publishing an
unverified number to someone in an emergency.

Deliberately four entries. A fifth — the Từ Dũ switchboard, a suicide-support line, any
1800/+84 number — stays out until it has the same provenance.
"""

from __future__ import annotations

HOTLINE_REGISTRY: list[dict[str, str]] = [
    {
        "name": "Trung tâm Cấp cứu 115",
        "number": "115",
        "scope": "toàn quốc",
        "hours": "24/7",
        "cost": "miễn phí",
        "sourceUrl": (
            "https://benhviennhitrunguong.gov.vn/"
            "5-buoc-cap-cuu-tre-duoi-nuoc-dung-cach-va-nhung-sai-lam-can-tranh.html"
        ),
        "verifiedDate": "2026-08-15",
    },
    {
        "name": "Đường dây nóng Bộ Y tế",
        "number": "1900-9095",
        "scope": "toàn quốc",
        "hours": "trong giờ hành chính",
        "cost": "miễn phí",
        "sourceUrl": (
            "https://baochinhphu.vn/"
            "phan-hoi-cua-bo-y-te-ve-duong-day-nong-1900-9095-102205966.htm"
        ),
        "verifiedDate": "2026-08-15",
    },
    {
        "name": "Tổng đài Quốc gia bảo vệ trẻ em",
        "number": "111",
        "scope": "toàn quốc",
        "hours": "24/7",
        "cost": "miễn phí",
        "sourceUrl": "https://www.unicef.org/vietnam/child-protection",
        "verifiedDate": "2026-08-15",
    },
    {
        "name": "Bệnh viện Nhi Trung ương — Khoa Khám bệnh",
        "number": "0989.132.099",
        "scope": "Hà Nội (tuyến cuối toàn quốc)",
        "hours": "giờ hành chính",
        "cost": "theo bảng giá BYT",
        "sourceUrl": "https://benhviennhitrunguong.gov.vn/khoa-kham-benh-chuyen-khoa.html",
        "verifiedDate": "2026-08-15",
    },
]

#: The only numbers this service may ever render. Checked against the registry at render time.
HOTLINE_NUMBERS_WHITELIST = frozenset({"115", "1900-9095", "111", "0989.132.099"})

DISCLAIMER_BASE = (
    "Hệ thống cung cấp thông tin tham khảo. Trong tình huống cấp cứu thật sự, "
    "vui lòng gọi 115 ngay hoặc đến cơ sở y tế gần nhất. Không tự ý di chuyển "
    "xa nếu triệu chứng nghiêm trọng."
)
DISCLAIMER_NO_CONSENT_SUFFIX = (
    " Bạn đã chọn không chia sẻ vị trí, danh sách dưới đây chỉ mang tính tham "
    "khảo tổng quát không sắp xếp theo khoảng cách."
)
DISCLAIMER_EMPTY_FACILITIES_SUFFIX = (
    " Hiện chưa có dữ liệu bệnh viện đã xác minh trong hệ thống. "
    "Vui lòng gọi 115 ngay để được hỗ trợ."
)
