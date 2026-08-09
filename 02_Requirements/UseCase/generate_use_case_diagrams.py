#!/usr/bin/env python3
"""Generate the actor-centric CareBridge use-case diagrams from Report 3.

The output intentionally remains uncompressed Draw.io XML so it is reviewable in Git.
Run this script from any working directory; paths are resolved relative to the script.
"""

from __future__ import annotations

import argparse
import html
import itertools
import math
import os
import re
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
DEFAULT_SOURCE = REPO_ROOT / "02_Requirements" / "SRS" / "Report3_Functional_Specifications.md"
DEFAULT_OUTPUT = SCRIPT_DIR / "CareBridge_Use_Case_Diagrams.drawio"

HEADING_RE = re.compile(r"^#### \*{3}3\.\d+\.\d+ (UC-\d+) (.+?)\*{3}$")
TABLE_NAME_RE = re.compile(r"^\|\s*UC ID and Name\s*\|\s*(UC-\d+)\s+(.+?)\s*\|")
ACTOR_RE = re.compile(
    r"^\|\s*\*\*Primary Actor\*\*\s*\|\s*([^|]+?)\s*\|\s*\*\*Secondary Actors\*\*"
)

BOUNDARY_STYLE = (
    "rounded=0;whiteSpace=wrap;html=1;fillColor=#FFFFFF;fillOpacity=0;"
    "strokeColor=#374151;strokeWidth=2;fontFamily=Arial;fontSize=15;fontStyle=1;"
    "align=left;verticalAlign=top;spacingTop=12;spacingLeft=14;pointerEvents=0;"
    "collapsible=0;container=1;"
)
TITLE_STYLE = (
    "text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;"
    "whiteSpace=wrap;rounded=0;fontFamily=Arial;fontSize=24;fontStyle=1;"
    "fontColor=#111827;"
)
SUBTITLE_STYLE = (
    "text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;"
    "whiteSpace=wrap;rounded=0;fontFamily=Arial;fontSize=11;fontColor=#64748B;"
)
ACTOR_STYLE = (
    "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;"
    "outlineConnect=0;fontFamily=Arial;fontSize=13;fontStyle=1;fontColor=#111827;"
    "strokeColor=#374151;fillColor=#FFFFFF;"
)
EXTERNAL_STYLE = (
    "rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=6 4;"
    "fillColor=#F8FAFC;strokeColor=#64748B;strokeWidth=1.5;fontFamily=Arial;"
    "fontSize=12;fontStyle=1;fontColor=#334155;align=center;verticalAlign=middle;"
)
ASSOCIATION_STYLE = (
    "endArrow=none;startArrow=none;html=1;strokeColor=#64748B;strokeWidth=1.1;"
    "strokeOpacity=58;"
    "rounded=1;exitPerimeter=1;entryPerimeter=1;"
)
GENERALIZATION_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;startArrow=none;"
    "endArrow=block;endFill=0;endSize=14;strokeColor=#334155;strokeWidth=1.6;"
    "exitX=0.5;exitY=0;exitPerimeter=1;entryX=0.5;entryY=1;entryPerimeter=1;"
    "jettySize=auto;orthogonalLoop=1;"
)
INCLUDE_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;dashed=1;dashPattern=6 4;"
    "startArrow=none;endArrow=open;endFill=0;strokeColor=#2563EB;strokeWidth=1.4;"
    "fontFamily=Arial;fontSize=12;fontStyle=2;fontColor=#1D4ED8;"
    "labelBackgroundColor=#FFFFFF;jettySize=auto;orthogonalLoop=1;"
)
EXTEND_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;dashed=1;dashPattern=6 4;"
    "startArrow=none;endArrow=open;endFill=0;strokeColor=#7C3AED;strokeWidth=1.4;"
    "fontFamily=Arial;fontSize=12;fontStyle=2;fontColor=#6D28D9;"
    "labelBackgroundColor=#FFFFFF;jettySize=auto;orthogonalLoop=1;"
)
GROUP_PALETTES = (
    ("#F8FBFF", "#93C5FD", "#1D4ED8"),
    ("#F5FBF5", "#A7D7A9", "#347A3E"),
    ("#FBF7FF", "#C4B5FD", "#6D28D9"),
    ("#FFF9ED", "#F4C97A", "#9A5B13"),
)
UC_PALETTES = (
    ("#EAF4FF", "#4F81BD"),
    ("#EAF7EA", "#82B366"),
    ("#F3E8FF", "#8B5CF6"),
    ("#FFF4D8", "#D6A64B"),
)

RELATIONSHIPS: tuple[tuple[str, str, str], ...] = (
    ("include", "UC-29", "UC-28"),
    ("extend", "UC-18", "UC-17"),
    ("extend", "UC-47", "UC-45"),
    ("extend", "UC-48", "UC-45"),
    ("extend", "UC-50", "UC-49"),
    ("extend", "UC-66", "UC-65"),
    ("extend", "UC-68", "UC-65"),
    ("extend", "UC-49", "UC-68"),
    ("extend", "UC-51", "UC-68"),
    ("extend", "UC-52", "UC-68"),
)

EXPECTED_PAGE_NAMES = (
    "D-01. Use Case Diagram for Guest & Authenticated User",
    "D-02. Use Case Diagram for Mother",
    "D-03. Use Case Diagram for Family & Authorized Family",
    "D-04. Use Case Diagram for Expert & Verified Expert",
    "D-05. Use Case Diagram for CareBridge System Admin",
    "D-06. Use Case Diagram for Moderator",
    "D-07. Use Case Diagram for Content Admin",
)


def ucids(*numbers: int) -> list[str]:
    return [f"UC-{number:02d}" for number in numbers]


def span(start: int, end: int) -> list[str]:
    return ucids(*range(start, end + 1))


def normalize_markdown(text: str) -> str:
    return re.sub(r"\\([&_*#\-\[\]().])", r"\1", text.strip())


def parse_spec(source: Path) -> dict[str, dict[str, object]]:
    lines = source.read_text(encoding="utf-8").splitlines()
    specs: dict[str, dict[str, object]] = {}
    headings: list[tuple[int, str, str]] = []
    seen_heading_ids: set[str] = set()

    for index, line in enumerate(lines):
        match = HEADING_RE.match(line)
        if match:
            uc_id = match.group(1)
            if uc_id in seen_heading_ids:
                raise ValueError(f"Duplicate use-case heading for {uc_id}")
            seen_heading_ids.add(uc_id)
            headings.append((index, uc_id, normalize_markdown(match.group(2))))

    for position, (start, uc_id, heading_title) in enumerate(headings):
        end = headings[position + 1][0] if position + 1 < len(headings) else len(lines)
        canonical_title: str | None = None
        primary_actor: str | None = None
        for line in lines[start:end]:
            name_match = TABLE_NAME_RE.match(line)
            if name_match and name_match.group(1) == uc_id:
                canonical_title = normalize_markdown(name_match.group(2))
            actor_match = ACTOR_RE.match(line)
            if actor_match:
                primary_actor = actor_match.group(1).strip()

        if not canonical_title or not primary_actor:
            raise ValueError(f"Incomplete specification for {uc_id}")

        specs[uc_id] = {
            "title": canonical_title,
            "heading_title": heading_title,
            "primary_actor": primary_actor,
            "actors": tuple(part.strip() for part in primary_actor.split(" / ")),
        }

    expected = {f"UC-{number:02d}" for number in range(1, 92)}
    actual = set(specs)
    if actual != expected:
        missing = sorted(expected - actual)
        extra = sorted(actual - expected)
        raise ValueError(f"Expected UC-01..UC-91; missing={missing}, extra={extra}")

    # UC-74 is inconsistent in the Markdown heading; the table field is canonical.
    if specs["UC-74"]["title"] != "Manage CareBridge System Configuration":
        raise ValueError("UC-74 canonical table name changed; review the diagram naming decision")
    return specs


def page_definitions() -> list[dict[str, object]]:
    return [
        {
            "key": "guest-and-authenticated-user",
            "title": "Use Case Diagram for Guest & Authenticated User",
            "layout": "masonry",
            "columns": 2,
            "generalizations": [
                ("Authenticated User", "User"),
                ("Locked User", "User"),
            ],
            "groups": [
                {"title": "Guest Access", "actor": "Guest", "ids": ucids(1, 2, 4), "column": 0},
                {
                    "title": "Authenticated Account Management",
                    "actor": "Authenticated User",
                    "ids": ucids(3, 5, 6, 7, 8, 9, 19, 42),
                    "column": 1,
                },
                {
                    "title": "Locked Account Appeal",
                    "actor": "Locked User",
                    "ids": ucids(10),
                    "column": 0,
                },
            ],
        },
        {
            "key": "mother",
            "title": "Use Case Diagram for Mother",
            "layout": "grid",
            "columns": 2,
            "generalizations": [
                ("Authenticated User", "User"),
                ("Mother", "Authenticated User"),
            ],
            "groups": [
                {
                    "title": "Maternal Journey",
                    "actor": "Mother",
                    "ids": span(20, 33),
                    "row": 0,
                    "column": 0,
                },
                {
                    "title": "Baby Care & AI",
                    "actor": "Mother",
                    "ids": span(34, 39) + span(45, 47),
                    "row": 0,
                    "column": 1,
                },
                {
                    "title": "Community, Experts & Emergency",
                    "actor": "Mother",
                    "ids": ucids(11, 17, 18, 40, 41, 43, 44) + span(48, 53),
                    "row": 1,
                    "column": 0,
                },
                {
                    "title": "Cooperative Care, Content & Safety",
                    "actor": "Mother",
                    "ids": ucids(54, 55, 56, 58, 59, 60) + span(62, 68),
                    "row": 1,
                    "column": 1,
                },
            ],
            "external_actors": [
                {"name": "Phone Motion Sensors", "target": "UC-65", "group_index": 3},
            ],
        },
        {
            "key": "family-and-authorized-family",
            "title": "Use Case Diagram for Family & Authorized Family",
            "layout": "grid",
            "columns": 2,
            "generalizations": [
                ("Authenticated User", "User"),
                ("Family", "Authenticated User"),
                ("Authorized Family", "Family"),
            ],
            "groups": [
                {
                    "title": "Family Participation",
                    "actor": "Family",
                    "ids": ucids(11, 17, 18, 40, 41, 43, 44, 48, 49, 50, 51, 53, 55, 59, 60, 62),
                    "row": 0,
                    "column": 0,
                },
                {
                    "title": "Authorized Shared Care",
                    "actor": "Authorized Family",
                    "ids": ucids(46, 57, 58, 61, 63),
                    "row": 0,
                    "column": 1,
                },
            ],
        },
        {
            "key": "expert-and-verified-expert",
            "title": "Use Case Diagram for Expert & Verified Expert",
            "layout": "masonry",
            "columns": 2,
            "generalizations": [
                ("Authenticated User", "User"),
                ("Expert Applicant", "User"),
                ("Expert", "Authenticated User"),
                ("Verified Expert", "Expert"),
            ],
            "groups": [
                {
                    "title": "Expert Application",
                    "actor": "Expert Applicant",
                    "ids": ucids(13),
                    "column": 1,
                },
                {
                    "title": "Expert Services",
                    "actor": "Expert",
                    "ids": ucids(11, 14, 15, 16, 17, 18),
                    "column": 0,
                },
                {
                    "title": "Verified Community Contribution",
                    "actor": "Verified Expert",
                    "ids": ucids(12),
                    "column": 1,
                },
            ],
        },
        {
            "key": "carebridge-system-admin",
            "title": "Use Case Diagram for CareBridge System Admin",
            "layout": "grid",
            "columns": 1,
            "generalizations": [
                ("Authenticated User", "User"),
                ("CareBridge System Admin", "Authenticated User"),
            ],
            "groups": [
                {
                    "title": "System Administration",
                    "actor": "CareBridge System Admin",
                    "ids": span(69, 74) + ucids(82, 83, 88, 89, 91),
                    "row": 0,
                    "column": 0,
                },
            ],
        },
        {
            "key": "moderator",
            "title": "Use Case Diagram for Moderator",
            "layout": "grid",
            "columns": 1,
            "generalizations": [
                ("Authenticated User", "User"),
                ("Moderator", "Authenticated User"),
            ],
            "groups": [
                {
                    "title": "Community Moderation",
                    "actor": "Moderator",
                    "ids": span(75, 81),
                    "row": 0,
                    "column": 0,
                },
            ],
        },
        {
            "key": "content-admin",
            "title": "Use Case Diagram for Content Admin",
            "layout": "grid",
            "columns": 1,
            "generalizations": [
                ("Authenticated User", "User"),
                ("Content Admin", "Authenticated User"),
            ],
            "groups": [
                {
                    "title": "Content Administration",
                    "actor": "Content Admin",
                    "ids": span(84, 87) + ucids(89, 90),
                    "row": 0,
                    "column": 0,
                },
            ],
        },
    ]


def validate_page_definitions(pages: list[dict[str, object]], specs: dict[str, dict[str, object]]) -> None:
    if len(pages) != 7:
        raise ValueError(f"Expected 7 pages, got {len(pages)}")
    if len({page["key"] for page in pages}) != len(pages):
        raise ValueError("Page keys must be unique")
    actual_names = tuple(f"D-{index:02d}. {page['title']}" for index, page in enumerate(pages, start=1))
    if actual_names != EXPECTED_PAGE_NAMES:
        raise ValueError(f"Unexpected diagram order or names: {actual_names}")

    covered: set[str] = set()
    for page in pages:
        for group in page["groups"]:  # type: ignore[index]
            actor = group["actor"]
            for uc_id in group["ids"]:
                if uc_id not in specs:
                    raise ValueError(f"Unknown {uc_id} on page {page['key']}")
                if actor not in specs[uc_id]["actors"]:
                    raise ValueError(f"{actor} is not a primary actor for {uc_id}")
                covered.add(uc_id)

        groups: list[dict[str, object]] = page["groups"]  # type: ignore[assignment]
        parent_by_child = dict(page.get("generalizations", []))
        if len(parent_by_child) != len(page.get("generalizations", [])):
            raise ValueError(f"Actor may have only one direct parent on page {page['key']}")
        if "Guest" in parent_by_child or "Phone Motion Sensors" in parent_by_child:
            raise ValueError("Guest and external devices cannot inherit User")
        for actor in {str(group["actor"]) for group in groups} - {"Guest"}:
            current = actor
            visited: set[str] = set()
            while current != "User":
                if current in visited or current not in parent_by_child:
                    raise ValueError(f"{actor} has no acyclic inheritance path to User on {page['key']}")
                visited.add(current)
                current = parent_by_child[current]
        for external in page.get("external_actors", []):
            group_index = int(external["group_index"])
            if group_index < 0 or group_index >= len(groups):
                raise ValueError(f"External actor group index out of range: {external}")
            if external["target"] not in groups[group_index]["ids"]:
                raise ValueError(f"External actor target is not in its declared group: {external}")
            if external["name"] not in specs[external["target"]]["actors"]:
                raise ValueError(f"External actor mismatch: {external}")

    expected = set(specs)
    if covered != expected:
        raise ValueError(f"Page coverage mismatch: missing={sorted(expected-covered)}, extra={sorted(covered-expected)}")


def number(value: float | int) -> str:
    if isinstance(value, int) or float(value).is_integer():
        return str(int(value))
    return f"{value:.2f}".rstrip("0").rstrip(".")


def ceil10(value: float) -> int:
    return int(math.ceil(value / 10.0) * 10)


def measure_text(text: str) -> float:
    """Return a deterministic Arial-13 approximation without platform font dependencies."""

    total = 0.0
    for character in text:
        if character == " ":
            total += 3.6
        elif character in "ilI|!'`.,:;":
            total += 3.5
        elif character in "()[]{}-/\\":
            total += 4.2
        elif character in "MW@%&#":
            total += 9.5
        elif character.isupper() or character.isdigit():
            total += 7.3
        else:
            total += 6.7
    return total


def balanced_lines(text: str, line_count: int) -> tuple[str, ...]:
    words = text.split()
    if not words:
        return ("",)
    line_count = max(1, min(line_count, len(words)))
    if line_count == 1:
        return (" ".join(words),)

    best: tuple[float, float, tuple[str, ...]] | None = None
    for breaks in itertools.combinations(range(1, len(words)), line_count - 1):
        stops = (0, *breaks, len(words))
        lines = tuple(" ".join(words[stops[index] : stops[index + 1]]) for index in range(line_count))
        widths = [measure_text(line) for line in lines]
        score = (max(widths), max(widths) - min(widths), lines)
        if best is None or score < best:
            best = score
    if best is None:
        raise ValueError(f"Unable to wrap text: {text}")
    return best[2]


def use_case_dimensions(uc_id: str, title: str) -> tuple[int, int, tuple[str, ...]]:
    candidates: list[tuple[float, int, int, tuple[str, ...]]] = []
    words = title.split()
    for line_count in range(1, min(4, len(words)) + 1):
        lines = balanced_lines(title, line_count)
        widest = max(measure_text(uc_id), *(measure_text(line) for line in lines))
        width = ceil10(max(180.0, widest + 72.0))
        height = ceil10(max(64.0, 30.0 + 18.0 * (1 + len(lines))))
        aspect = width / height
        if 2.2 <= aspect <= 4.5:
            ratio_penalty = 1.0 + 0.08 * abs(math.log(aspect / 3.0))
            candidates.append((width * height * ratio_penalty, width, height, lines))

    if not candidates:
        lines = balanced_lines(title, min(4, max(1, len(words))))
        widest = max(measure_text(uc_id), *(measure_text(line) for line in lines))
        width = ceil10(max(180.0, widest + 72.0))
        height = ceil10(max(64.0, 30.0 + 18.0 * (1 + len(lines))))
        return width, height, lines
    _, width, height, lines = min(candidates, key=lambda candidate: candidate[0])
    return width, height, lines


def add_vertex(
    root: ET.Element,
    *,
    cell_id: str,
    value: str,
    style: str,
    parent: str,
    x: float,
    y: float,
    width: float,
    height: float,
    extra: dict[str, str] | None = None,
) -> ET.Element:
    attributes = {
        "id": cell_id,
        "value": value,
        "style": style,
        "parent": parent,
        "vertex": "1",
    }
    if extra:
        attributes.update(extra)
    cell = ET.SubElement(root, "mxCell", attributes)
    ET.SubElement(
        cell,
        "mxGeometry",
        {
            "x": number(x),
            "y": number(y),
            "width": number(width),
            "height": number(height),
            "as": "geometry",
        },
    )
    return cell


def add_edge(
    root: ET.Element,
    *,
    cell_id: str,
    source: str,
    target: str,
    style: str = ASSOCIATION_STYLE,
    value: str = "",
    parent: str = "1",
    extra: dict[str, str] | None = None,
    waypoints: Iterable[tuple[float, float]] | None = None,
    label_y: float | None = None,
) -> ET.Element:
    attributes = {
        "id": cell_id,
        "value": value,
        "style": style,
        "parent": parent,
        "source": source,
        "target": target,
        "edge": "1",
    }
    if extra:
        attributes.update(extra)
    cell = ET.SubElement(
        root,
        "mxCell",
        attributes,
    )
    geometry_attributes = {"relative": "1", "as": "geometry"}
    if label_y is not None:
        geometry_attributes.update({"x": "0", "y": number(label_y)})
    geometry = ET.SubElement(cell, "mxGeometry", geometry_attributes)
    point_list = list(waypoints or ())
    if point_list:
        points = ET.SubElement(geometry, "Array", {"as": "points"})
        for x, y in point_list:
            ET.SubElement(points, "mxPoint", {"x": number(x), "y": number(y)})
    return cell


def group_style(index: int) -> str:
    fill, stroke, font = GROUP_PALETTES[index % len(GROUP_PALETTES)]
    return (
        "rounded=1;arcSize=12;whiteSpace=wrap;html=1;"
        f"fillColor={fill};strokeColor={stroke};strokeWidth=1.5;"
        "fontFamily=Arial;fontSize=15;fontStyle=1;"
        f"fontColor={font};align=left;verticalAlign=top;spacingTop=12;spacingLeft=14;"
        "pointerEvents=0;collapsible=0;container=1;"
    )


def use_case_style(index: int) -> str:
    fill, stroke = UC_PALETTES[index % len(UC_PALETTES)]
    return (
        "ellipse;whiteSpace=wrap;html=1;"
        f"fillColor={fill};strokeColor={stroke};strokeWidth=1.5;"
        "fontFamily=Arial;fontSize=13;fontColor=#1F2937;align=center;verticalAlign=middle;"
        "spacing=8;shadow=0;"
    )


def uc_label(uc_id: str, lines: tuple[str, ...]) -> str:
    return f"<b>{html.escape(uc_id)}</b><br/>" + "<br/>".join(html.escape(line) for line in lines)


def group_label(title: str, count: int) -> str:
    noun = "use case" if count == 1 else "use cases"
    return f"<b>{html.escape(title)}</b><br/><font color='#64748B'>{count} {noun}</font>"


def add_page_header(root: ET.Element, prefix: str, title: str, page_width: int) -> None:
    add_vertex(
        root,
        cell_id=f"{prefix}_title",
        value=html.escape(title),
        style=TITLE_STYLE,
        parent="1",
        x=60,
        y=24,
        width=page_width - 120,
        height=42,
    )
    add_vertex(
        root,
        cell_id=f"{prefix}_subtitle",
        value=(
            "CareBridge Release 1 • Source: Report3 Functional Specifications • "
            "Shared use cases may appear on multiple actor pages"
        ),
        style=SUBTITLE_STYLE,
        parent="1",
        x=60,
        y=68,
        width=page_width - 120,
        height=28,
    )


def actor_slug(actor: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", actor.lower()).strip("_")


def measure_groups(
    groups: list[dict[str, object]], specs: dict[str, dict[str, object]]
) -> list[dict[str, object]]:
    measured: list[dict[str, object]] = []
    for group in groups:
        use_cases: list[dict[str, object]] = []
        for uc_id in group["ids"]:
            width, height, lines = use_case_dimensions(uc_id, str(specs[uc_id]["title"]))
            use_cases.append({"id": uc_id, "width": width, "height": height, "lines": lines})

        group_width = ceil10(
            max(
                360.0,
                max(float(use_case["width"]) for use_case in use_cases) + 120.0,
                measure_text(str(group["title"])) + 80.0,
            )
        )
        header_height = ceil10(52.0 + 18.0 * max(1, math.ceil(measure_text(str(group["title"])) / max(1, group_width - 80))))
        content_top = header_height + 20
        uc_gap = 22
        group_height = ceil10(
            content_top
            + sum(float(use_case["height"]) for use_case in use_cases)
            + uc_gap * max(0, len(use_cases) - 1)
            + 30
        )
        measured.append(
            {
                "definition": group,
                "width": group_width,
                "height": group_height,
                "header_height": header_height,
                "content_top": content_top,
                "uc_gap": uc_gap,
                "use_cases": use_cases,
            }
        )
    return measured


def place_groups(page: dict[str, object], measured: list[dict[str, object]]) -> tuple[int, int]:
    columns = int(page.get("columns", 1))
    gap_x = 80
    gap_y = 55
    pad_x = 60
    pad_y = 70
    column_widths = [0] * columns
    for item in measured:
        column = int(item["definition"].get("column", 0))  # type: ignore[union-attr]
        column_widths[column] = max(column_widths[column], int(item["width"]))

    column_x: list[int] = []
    current_x = pad_x
    for width in column_widths:
        column_x.append(current_x)
        current_x += width + gap_x

    if page.get("layout") == "masonry":
        column_y = [pad_y] * columns
        for item in measured:
            definition: dict[str, object] = item["definition"]  # type: ignore[assignment]
            column = int(definition.get("column", 0))
            item["x"] = column_x[column] + (column_widths[column] - int(item["width"])) / 2
            item["y"] = column_y[column]
            column_y[column] += int(item["height"]) + gap_y
        content_bottom = max(column_y) - gap_y
    else:
        rows = max(int(item["definition"].get("row", 0)) for item in measured) + 1  # type: ignore[union-attr]
        row_heights = [0] * rows
        for item in measured:
            row = int(item["definition"].get("row", 0))  # type: ignore[union-attr]
            row_heights[row] = max(row_heights[row], int(item["height"]))
        row_y: list[int] = []
        current_y = pad_y
        for height in row_heights:
            row_y.append(current_y)
            current_y += height + gap_y
        for item in measured:
            definition = item["definition"]  # type: ignore[assignment]
            row = int(definition.get("row", 0))
            column = int(definition.get("column", 0))
            item["x"] = column_x[column] + (column_widths[column] - int(item["width"])) / 2
            item["y"] = row_y[row]
        content_bottom = current_y - gap_y

    boundary_width = current_x - gap_x + pad_x
    boundary_height = content_bottom + pad_y
    return boundary_width, boundary_height


def ancestor_layout(
    page: dict[str, object], boundary_x: float, boundary_width: float
) -> tuple[int, dict[str, tuple[float, float]]]:
    """Place only non-associating ancestor actors above the system boundary."""

    groups: list[dict[str, object]] = page["groups"]  # type: ignore[assignment]
    group_actors = {str(group["actor"]) for group in groups}
    generalizations: list[tuple[str, str]] = page.get("generalizations", [])  # type: ignore[assignment]
    parent_by_child = dict(generalizations)
    ordered_actors: list[str] = []
    for child, parent in generalizations:
        for actor in (parent, child):
            if actor not in ordered_actors:
                ordered_actors.append(actor)

    def level(actor: str) -> int:
        result = 0
        seen: set[str] = set()
        while actor in parent_by_child:
            if actor in seen:
                raise ValueError(f"Actor inheritance cycle on {page['key']}")
            seen.add(actor)
            actor = parent_by_child[actor]
            result += 1
        return result

    ancestor_actors = [actor for actor in ordered_actors if actor not in group_actors]
    center_x = boundary_x + boundary_width / 2 - 62.5
    positions = {actor: (center_x, 110 + level(actor) * 145) for actor in ancestor_actors}
    bottom = max((y + 104 for _, y in positions.values()), default=96)
    return ceil10(bottom + 130), positions


def build_page(
    diagram: ET.Element,
    page: dict[str, object],
    specs: dict[str, dict[str, object]],
    prefix: str,
) -> None:
    groups: list[dict[str, object]] = page["groups"]  # type: ignore[assignment]
    measured = measure_groups(groups, specs)
    boundary_width, boundary_height = place_groups(page, measured)
    boundary_x = 300
    boundary_y, ancestor_positions = ancestor_layout(page, boundary_x, boundary_width)
    right_gutter = 520 if page.get("external_actors") else 270
    page_width = boundary_x + boundary_width + right_gutter
    page_height = boundary_y + boundary_height + 100

    graph = ET.SubElement(
        diagram,
        "mxGraphModel",
        {
            "dx": str(min(page_width, 2400)),
            "dy": str(min(page_height, 1500)),
            "grid": "1",
            "gridSize": "10",
            "guides": "1",
            "tooltips": "1",
            "connect": "1",
            "arrows": "1",
            "fold": "1",
            "page": "1",
            "pageScale": "1",
            "pageWidth": str(page_width),
            "pageHeight": str(page_height),
            "math": "0",
            "shadow": "0",
        },
    )
    root = ET.SubElement(graph, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})
    add_page_header(root, prefix, str(page["title"]), page_width)

    canonical_actor_cells: dict[str, str] = {}
    actor_geometries: dict[str, tuple[float, float, float, float]] = {}
    for actor, (actor_x, actor_y) in ancestor_positions.items():
        actor_id = f"{prefix}_actor_canonical_{actor_slug(actor)}"
        canonical_actor_cells[actor] = actor_id
        actor_geometries[actor_id] = (actor_x, actor_y, 125, 104)
        add_vertex(
            root,
            cell_id=actor_id,
            value=html.escape(actor),
            style=ACTOR_STYLE,
            parent="1",
            x=actor_x,
            y=actor_y,
            width=125,
            height=104,
            extra={"actorName": actor, "actorKind": "canonical"},
        )

    boundary_id = f"{prefix}_boundary"
    add_vertex(
        root,
        cell_id=boundary_id,
        value="CareBridge System Boundary",
        style=BOUNDARY_STYLE,
        parent="1",
        x=boundary_x,
        y=boundary_y,
        width=boundary_width,
        height=boundary_height,
        extra={"cellType": "systemBoundary"},
    )

    uc_cells: dict[str, str] = {}
    uc_geometry: dict[str, tuple[float, float, float, float]] = {}
    uc_group_index: dict[str, int] = {}
    actor_group_occurrences: dict[str, int] = {}
    for group_index, item in enumerate(measured):
        group: dict[str, object] = item["definition"]  # type: ignore[assignment]
        group_id = f"{prefix}_group_{group_index + 1:02d}"
        group_x = float(item["x"])
        group_y = float(item["y"])
        group_width = float(item["width"])
        group_height = float(item["height"])
        add_vertex(
            root,
            cell_id=group_id,
            value=group_label(str(group["title"]), len(group["ids"])),
            style=group_style(group_index),
            parent=boundary_id,
            x=group_x,
            y=group_y,
            width=group_width,
            height=group_height,
            extra={"cellType": "useCaseGroup", "groupIndex": str(group_index)},
        )

        column = int(group.get("column", 0))
        actor = str(group["actor"])
        occurrence = actor_group_occurrences.get(actor, 0)
        actor_x = 90 if column == 0 else boundary_x + boundary_width + 55
        actor_y = boundary_y + group_y + group_height / 2 - 52
        if occurrence == 0:
            association_actor_id = f"{prefix}_actor_canonical_{actor_slug(actor)}"
            canonical_actor_cells[actor] = association_actor_id
            actor_kind = "canonical"
            actor_extra = {
                "actorName": actor,
                "actorKind": actor_kind,
                "groupIndex": str(group_index),
            }
        else:
            association_actor_id = f"{prefix}_actor_alias_{group_index + 1:02d}"
            actor_extra = {
                "actorName": actor,
                "actorKind": "associationAlias",
                "actorAliasOf": actor,
                "groupIndex": str(group_index),
            }
        actor_geometries[association_actor_id] = (actor_x, actor_y, 125, 104)
        add_vertex(
            root,
            cell_id=association_actor_id,
            value=html.escape(actor),
            style=ACTOR_STYLE,
            parent="1",
            x=actor_x,
            y=actor_y,
            width=125,
            height=104,
            extra=actor_extra,
        )
        actor_group_occurrences[actor] = occurrence + 1

        current_y = group_y + float(item["content_top"])
        for use_case in item["use_cases"]:
            uc_id = str(use_case["id"])
            uc_width = float(use_case["width"])
            uc_height = float(use_case["height"])
            uc_x = group_x + (group_width - uc_width) / 2
            cell_id = f"{prefix}_uc_{uc_id[3:]}"
            uc_cells[uc_id] = cell_id
            uc_geometry[uc_id] = (uc_x, current_y, uc_width, uc_height)
            uc_group_index[uc_id] = group_index
            add_vertex(
                root,
                cell_id=cell_id,
                value=uc_label(uc_id, use_case["lines"]),  # type: ignore[arg-type]
                style=use_case_style(group_index),
                parent=boundary_id,
                x=uc_x,
                y=current_y,
                width=uc_width,
                height=uc_height,
                extra={
                    "ucId": uc_id,
                    "ucTitle": str(specs[uc_id]["title"]),
                    "ucLineCount": str(len(use_case["lines"])),
                    "groupId": group_id,
                },
            )
            add_edge(
                root,
                cell_id=f"{prefix}_assoc_{group_index + 1:02d}_{uc_id[3:]}",
                source=association_actor_id,
                target=cell_id,
                extra={"edgeType": "association", "actor": actor, "targetUc": uc_id},
            )
            current_y += uc_height + float(item["uc_gap"])

    left_actor_lane = boundary_x - 35
    right_actor_lane = boundary_x + boundary_width + 35
    perimeter_index = 0
    for child, parent in page.get("generalizations", []):
        source_id = canonical_actor_cells[child]
        target_id = canonical_actor_cells[parent]
        source_x, source_y, source_width, source_height = actor_geometries[source_id]
        target_x, target_y, target_width, target_height = actor_geometries[target_id]

        def actor_side(x: float, width: float) -> str:
            if x + width <= boundary_x:
                return "left"
            if x >= boundary_x + boundary_width:
                return "right"
            return "top"

        source_side = actor_side(source_x, source_width)
        target_side = actor_side(target_x, target_width)
        waypoints: list[tuple[float, float]] = []
        route_kind = "direct"
        if source_side == "top" and target_side == "top":
            side_lane_x = min(source_x, target_x) - 25
            waypoints = [
                (side_lane_x, source_y + source_height / 2),
                (side_lane_x, target_y + target_height / 2),
            ]
            edge_style = (
                GENERALIZATION_STYLE
                + "exitX=0;exitY=0.5;exitPerimeter=1;"
                + "entryX=0;entryY=0.5;entryPerimeter=1;"
            )
            route_kind = "actorSide"
        else:
            lane_y = boundary_y - 50 - perimeter_index * 25
            perimeter_index += 1
            source_center_y = source_y + source_height / 2
            target_center_y = target_y + target_height / 2
            if source_side == "left":
                source_lane_x = left_actor_lane
                exit_style = "exitX=1;exitY=0.5;exitPerimeter=1;"
            elif source_side == "right":
                source_lane_x = right_actor_lane
                exit_style = "exitX=0;exitY=0.5;exitPerimeter=1;"
            else:
                source_lane_x = source_x + source_width / 2
                exit_style = "exitX=0.5;exitY=1;exitPerimeter=1;"
            waypoints.extend([(source_lane_x, source_center_y), (source_lane_x, lane_y)])

            if target_side == "left":
                target_lane_x = left_actor_lane
                entry_style = "entryX=1;entryY=0.5;entryPerimeter=1;"
                if target_lane_x != source_lane_x:
                    waypoints.append((target_lane_x, lane_y))
                waypoints.append((target_lane_x, target_center_y))
            elif target_side == "right":
                target_lane_x = right_actor_lane
                entry_style = "entryX=0;entryY=0.5;entryPerimeter=1;"
                if target_lane_x != source_lane_x:
                    waypoints.append((target_lane_x, lane_y))
                waypoints.append((target_lane_x, target_center_y))
            else:
                if source_side == "right":
                    target_lane_x = target_x + target_width + 25
                    entry_style = "entryX=1;entryY=0.5;entryPerimeter=1;"
                else:
                    target_lane_x = target_x - 25
                    entry_style = "entryX=0;entryY=0.5;entryPerimeter=1;"
                waypoints.extend([(target_lane_x, lane_y), (target_lane_x, target_center_y)])
            edge_style = GENERALIZATION_STYLE + exit_style + entry_style
            route_kind = "outsideBoundaryPerimeter"

        add_edge(
            root,
            cell_id=f"{prefix}_generalization_{actor_slug(child)}_{actor_slug(parent)}",
            source=source_id,
            target=target_id,
            style=edge_style,
            waypoints=waypoints,
            extra={
                "edgeType": "generalization",
                "childActor": child,
                "parentActor": parent,
                "routeKind": route_kind,
                "waypointCount": str(len(waypoints)),
            },
        )

    cross_group_lane = 0
    for relation_type, source_uc, target_uc in RELATIONSHIPS:
        if source_uc not in uc_cells or target_uc not in uc_cells:
            continue
        source_group_index = uc_group_index[source_uc]
        target_group_index = uc_group_index[target_uc]
        source_x, source_y, source_width, source_height = uc_geometry[source_uc]
        target_x, target_y, target_width, target_height = uc_geometry[target_uc]
        source_center_y = boundary_y + source_y + source_height / 2
        target_center_y = boundary_y + target_y + target_height / 2
        waypoints: list[tuple[float, float]] = []
        route_kind = "direct"

        if source_group_index == target_group_index:
            group_item = measured[source_group_index]
            group_definition: dict[str, object] = group_item["definition"]  # type: ignore[assignment]
            group_ids: list[str] = group_definition["ids"]  # type: ignore[assignment]
            if abs(group_ids.index(source_uc) - group_ids.index(target_uc)) > 1:
                column = int(group_definition.get("column", 0))
                if int(page.get("columns", 1)) > 1 and column > 0:
                    lane_x = boundary_x + float(group_item["x"]) - 10
                else:
                    lane_x = boundary_x + float(group_item["x"]) + float(group_item["width"]) + 10
                waypoints = [(lane_x, source_center_y), (lane_x, target_center_y)]
                route_kind = "sameGroupGutter"
        else:
            source_item = measured[source_group_index]
            target_item = measured[target_group_index]
            source_column = int(source_item["definition"].get("column", 0))  # type: ignore[union-attr]
            target_column = int(target_item["definition"].get("column", 0))  # type: ignore[union-attr]
            if source_column != target_column:
                left_item = source_item if source_column < target_column else target_item
                right_item = target_item if source_column < target_column else source_item
                gap_left = boundary_x + float(left_item["x"]) + float(left_item["width"])
                gap_right = boundary_x + float(right_item["x"])
                lane_fraction = (cross_group_lane + 1) / 5.0
                lane_x = gap_left + (gap_right - gap_left) * lane_fraction
                cross_group_lane += 1
            else:
                lane_x = boundary_x + max(
                    float(source_item["x"]) + float(source_item["width"]),
                    float(target_item["x"]) + float(target_item["width"]),
                ) + 15
            waypoints = [(lane_x, source_center_y), (lane_x, target_center_y)]
            route_kind = "crossGroupLane"
        add_edge(
            root,
            cell_id=f"{prefix}_relation_{relation_type}_{source_uc[3:]}_{target_uc[3:]}",
            source=uc_cells[source_uc],
            target=uc_cells[target_uc],
            style=INCLUDE_STYLE if relation_type == "include" else EXTEND_STYLE,
            value=html.escape(f"«{relation_type}»"),
            waypoints=waypoints,
            label_y=-12,
            extra={
                "edgeType": "useCaseRelationship",
                "relationType": relation_type,
                "sourceUc": source_uc,
                "targetUc": target_uc,
                "routeKind": route_kind,
                "waypointCount": str(len(waypoints)),
            },
        )

    for external_index, external in enumerate(page.get("external_actors", []), start=1):
        target_uc = str(external["target"])
        target_x, target_y, _, target_height = uc_geometry[target_uc]
        external_id = f"{prefix}_external_{external_index:02d}"
        external_name = str(external["name"])
        add_vertex(
            root,
            cell_id=external_id,
            value=f"{html.escape(external_name)}<br/><i>External device actor</i>",
            style=EXTERNAL_STYLE,
            parent="1",
            x=boundary_x + boundary_width + 220,
            y=boundary_y + target_y + target_height / 2 - 36,
            width=250,
            height=72,
            extra={"actorName": external_name, "actorKind": "externalDevice"},
        )
        add_edge(
            root,
            cell_id=f"{prefix}_external_assoc_{external_index:02d}",
            source=external_id,
            target=uc_cells[target_uc],
            extra={"edgeType": "association", "actor": external_name, "targetUc": target_uc},
        )


def build_document(specs: dict[str, dict[str, object]], pages: list[dict[str, object]]) -> ET.Element:
    mxfile = ET.Element(
        "mxfile",
        {
            "host": "Electron",
            "agent": "Codex",
            "version": "30.3.11",
            "pages": str(len(pages)),
        },
    )
    for page_index, page in enumerate(pages, start=1):
        prefix = f"d{page_index:02d}"
        diagram = ET.SubElement(
            mxfile,
            "diagram",
            {
                "id": f"uc-{page['key']}",
                "name": f"D-{page_index:02d}. {page['title']}",
            },
        )
        build_page(diagram, page, specs, prefix)
    ET.indent(mxfile, space="  ")
    return mxfile


def cell_geometry(cell: ET.Element) -> tuple[float, float, float, float]:
    geometry = cell.find("mxGeometry")
    if geometry is None:
        raise ValueError(f"Missing geometry on {cell.get('id')}")
    return tuple(float(geometry.get(name, "0")) for name in ("x", "y", "width", "height"))  # type: ignore[return-value]


def rectangles_overlap(
    first: tuple[float, float, float, float], second: tuple[float, float, float, float]
) -> bool:
    first_x, first_y, first_width, first_height = first
    second_x, second_y, second_width, second_height = second
    return (
        first_x < second_x + second_width
        and second_x < first_x + first_width
        and first_y < second_y + second_height
        and second_y < first_y + first_height
    )


def validate_xml(
    mxfile: ET.Element,
    specs: dict[str, dict[str, object]],
    pages: list[dict[str, object]],
) -> dict[str, int]:
    diagrams = mxfile.findall("diagram")
    if int(mxfile.get("pages", "-1")) != len(diagrams) or len(diagrams) != 7:
        raise ValueError("mxfile page count does not match the seven generated diagrams")
    if len({diagram.get("id") for diagram in diagrams}) != len(diagrams):
        raise ValueError("Diagram IDs must be unique")

    expected_diagrams = [
        (f"uc-{page['key']}", f"D-{index:02d}. {page['title']}")
        for index, page in enumerate(pages, start=1)
    ]
    actual_diagrams = [(diagram.get("id"), diagram.get("name")) for diagram in diagrams]
    if actual_diagrams != expected_diagrams:
        raise ValueError(f"Diagram order/name mismatch: {actual_diagrams}")

    expected_associations: set[tuple[str, str, str]] = set()
    expected_generalizations: set[tuple[str, str, str]] = set()
    expected_relations: set[tuple[str, str, str, str]] = set()
    for page in pages:
        page_key = str(page["key"])
        page_uc_ids: set[str] = set()
        for group in page["groups"]:  # type: ignore[index]
            actor = str(group["actor"])
            for uc_id in group["ids"]:
                page_uc_ids.add(uc_id)
                expected_associations.add((page_key, actor, uc_id))
        for external in page.get("external_actors", []):
            expected_associations.add((page_key, str(external["name"]), str(external["target"])))
        for child, parent in page.get("generalizations", []):
            expected_generalizations.add((page_key, child, parent))
        for relation_type, source_uc, target_uc in RELATIONSHIPS:
            if source_uc in page_uc_ids and target_uc in page_uc_ids:
                expected_relations.add((page_key, relation_type, source_uc, target_uc))

    observed: dict[str, str] = {}
    uc_instances = 0
    dimension_pairs: set[tuple[int, int]] = set()
    observed_associations: list[tuple[str, str, str]] = []
    observed_generalizations: list[tuple[str, str, str]] = []
    observed_relations: list[tuple[str, str, str, str]] = []
    for diagram, page in zip(diagrams, pages):
        page_key = str(page["key"])
        graph = diagram.find("mxGraphModel")
        if graph is None:
            raise ValueError(f"Compressed or missing mxGraphModel on {diagram.get('name')}")
        cells = graph.findall("./root/mxCell")
        if any(cell.get("cellType") == "actorHierarchy" for cell in cells):
            raise ValueError(f"Actor hierarchy panels are not allowed on {diagram.get('name')}")
        ids = [cell.get("id") for cell in cells]
        if any(not cell_id for cell_id in ids):
            raise ValueError(f"Every mxCell requires an ID on {diagram.get('name')}")
        if len(ids) != len(set(ids)):
            raise ValueError(f"Duplicate cell IDs on {diagram.get('name')}")
        known = set(ids)
        by_id = {str(cell.get("id")): cell for cell in cells}
        group_cells = {str(cell.get("id")): cell for cell in cells if cell.get("cellType") == "useCaseGroup"}
        boundary_cells = [cell for cell in cells if cell.get("cellType") == "systemBoundary"]
        if len(boundary_cells) != 1:
            raise ValueError(f"Expected one system boundary on {diagram.get('name')}")
        boundary_width = cell_geometry(boundary_cells[0])[2]
        boundary_height = cell_geometry(boundary_cells[0])[3]
        uc_cells_by_group: dict[str, list[ET.Element]] = {}
        association_source_ids: set[str] = set()
        for cell in cells:
            for attribute in ("parent", "source", "target"):
                reference = cell.get(attribute)
                if reference and reference not in known:
                    raise ValueError(
                        f"Unresolved {attribute}={reference} on {diagram.get('name')} / {cell.get('id')}"
                    )
            uc_id = cell.get("ucId")
            if uc_id:
                uc_instances += 1
                title = cell.get("ucTitle", "")
                if uc_id not in specs or title != specs[uc_id]["title"]:
                    raise ValueError(f"UC metadata mismatch for {uc_id}")
                expected_width, expected_height, expected_lines = use_case_dimensions(uc_id, title)
                x, y, width, height = cell_geometry(cell)
                if (width, height) != (expected_width, expected_height):
                    raise ValueError(
                        f"Dynamic UC geometry mismatch for {uc_id}: {(width, height)} != "
                        f"{(expected_width, expected_height)}"
                    )
                if cell.get("ucLineCount") != str(len(expected_lines)):
                    raise ValueError(f"Dynamic line-count metadata mismatch for {uc_id}")
                if cell.get("value") != uc_label(uc_id, expected_lines):
                    raise ValueError(f"Rendered UC label differs from deterministic wrap for {uc_id}")
                dimension_pairs.add((expected_width, expected_height))
                group_id = cell.get("groupId", "")
                if group_id not in group_cells:
                    raise ValueError(f"Unknown group for {uc_id}: {group_id}")
                group_x, group_y, group_width, group_height = cell_geometry(group_cells[group_id])
                if not (
                    x >= group_x
                    and y >= group_y
                    and x + width <= group_x + group_width
                    and y + height <= group_y + group_height
                ):
                    raise ValueError(f"{uc_id} is not contained in {group_id}")
                uc_cells_by_group.setdefault(group_id, []).append(cell)
                observed[uc_id] = title

            edge_type = cell.get("edgeType")
            if edge_type == "association":
                actor = cell.get("actor", "")
                target_uc = cell.get("targetUc", "")
                association_source_ids.add(cell.get("source", ""))
                source_cell = by_id.get(cell.get("source", ""))
                target_cell = by_id.get(cell.get("target", ""))
                if source_cell is None or source_cell.get("actorName") != actor:
                    raise ValueError(f"Association source actor metadata mismatch on {cell.get('id')}")
                if target_cell is None or target_cell.get("ucId") != target_uc:
                    raise ValueError(f"Association target UC metadata mismatch on {cell.get('id')}")
                if actor not in specs[target_uc]["actors"]:
                    raise ValueError(f"{actor} is not a primary actor for {target_uc}")
                observed_associations.append((page_key, actor, target_uc))
            elif edge_type == "generalization":
                child = cell.get("childActor", "")
                parent = cell.get("parentActor", "")
                source_cell = by_id.get(cell.get("source", ""))
                target_cell = by_id.get(cell.get("target", ""))
                if (
                    source_cell is None
                    or target_cell is None
                    or source_cell.get("actorKind") != "canonical"
                    or target_cell.get("actorKind") != "canonical"
                    or source_cell.get("actorName") != child
                    or target_cell.get("actorName") != parent
                ):
                    raise ValueError(f"Generalization endpoints mismatch on {cell.get('id')}")
                observed_generalizations.append((page_key, child, parent))
            elif edge_type == "useCaseRelationship":
                relation_type = cell.get("relationType", "")
                source_uc = cell.get("sourceUc", "")
                target_uc = cell.get("targetUc", "")
                source_cell = by_id.get(cell.get("source", ""))
                target_cell = by_id.get(cell.get("target", ""))
                if (
                    source_cell is None
                    or target_cell is None
                    or source_cell.get("ucId") != source_uc
                    or target_cell.get("ucId") != target_uc
                ):
                    raise ValueError(f"Use-case relationship endpoints mismatch on {cell.get('id')}")
                expected_label = f"«{relation_type}»"
                if cell.get("value") != expected_label:
                    raise ValueError(f"Use-case relationship label mismatch on {cell.get('id')}")
                route_kind = cell.get("routeKind", "")
                geometry = cell.find("mxGeometry")
                points = [] if geometry is None else geometry.findall("./Array[@as='points']/mxPoint")
                if cell.get("waypointCount") != str(len(points)):
                    raise ValueError(f"Use-case relationship waypoint metadata mismatch on {cell.get('id')}")
                if route_kind == "direct" and points:
                    raise ValueError(f"Direct relationship unexpectedly has waypoints on {cell.get('id')}")
                if route_kind in {"sameGroupGutter", "crossGroupLane"} and len(points) != 2:
                    raise ValueError(f"Routed relationship requires two waypoints on {cell.get('id')}")
                if route_kind not in {"direct", "sameGroupGutter", "crossGroupLane"}:
                    raise ValueError(f"Unknown relationship route kind on {cell.get('id')}: {route_kind}")
                observed_relations.append((page_key, relation_type, source_uc, target_uc))

        group_rectangles = [(group_id, cell_geometry(cell)) for group_id, cell in group_cells.items()]
        for group_id, (x, y, width, height) in group_rectangles:
            if x < 0 or y < 0 or x + width > boundary_width or y + height > boundary_height:
                raise ValueError(f"Group {group_id} is outside the system boundary")
        for index, (first_id, first_rect) in enumerate(group_rectangles):
            for second_id, second_rect in group_rectangles[index + 1 :]:
                if rectangles_overlap(first_rect, second_rect):
                    raise ValueError(f"Use-case groups overlap: {first_id}, {second_id}")
        for group_id, group_uc_cells in uc_cells_by_group.items():
            uc_rectangles = [(str(cell.get("ucId")), cell_geometry(cell)) for cell in group_uc_cells]
            for index, (first_id, first_rect) in enumerate(uc_rectangles):
                for second_id, second_rect in uc_rectangles[index + 1 :]:
                    if rectangles_overlap(first_rect, second_rect):
                        raise ValueError(f"Use cases overlap in {group_id}: {first_id}, {second_id}")

        group_actors = {str(group["actor"]) for group in page["groups"]}
        for actor in group_actors:
            canonical_cells = [
                cell
                for cell in cells
                if cell.get("actorKind") == "canonical" and cell.get("actorName") == actor
            ]
            if len(canonical_cells) != 1 or canonical_cells[0].get("id", "") not in association_source_ids:
                raise ValueError(f"Canonical actor {actor} must directly own associations on {diagram.get('name')}")

        canonical_mothers = [
            cell for cell in cells if cell.get("actorKind") == "canonical" and cell.get("actorName") == "Mother"
        ]
        mother_aliases = [cell for cell in cells if cell.get("actorAliasOf") == "Mother"]
        all_aliases = [cell for cell in cells if cell.get("actorKind") == "associationAlias"]
        if page_key == "mother" and (len(canonical_mothers) != 1 or len(mother_aliases) != 3):
            raise ValueError("Mother page requires one canonical Mother and three association aliases")
        if page_key == "mother" and len(all_aliases) != 3:
            raise ValueError("Only the three additional Mother symbols may be association aliases")
        if page_key != "mother" and all_aliases:
            raise ValueError(f"Unexpected duplicate actor symbols on {diagram.get('name')}")

    if set(observed) != set(specs):
        raise ValueError("Generated XML does not cover exactly UC-01 through UC-91")
    if uc_instances != 114:
        raise ValueError(f"Expected 114 UC instances, got {uc_instances}")
    if len(dimension_pairs) < 2:
        raise ValueError("Use cases were not sized dynamically from their content")
    if len(observed_associations) != len(set(observed_associations)):
        raise ValueError("Duplicate primary-actor associations detected")
    if set(observed_associations) != expected_associations or len(observed_associations) != 115:
        raise ValueError("Primary-actor association topology does not match the approved 115 edges")
    if len(observed_generalizations) != len(set(observed_generalizations)):
        raise ValueError("Duplicate actor generalizations detected")
    if set(observed_generalizations) != expected_generalizations:
        raise ValueError("Actor generalization topology differs from the approved hierarchy")
    if any(child in {"Guest", "Phone Motion Sensors"} for _, child, _ in observed_generalizations):
        raise ValueError("Guest and Phone Motion Sensors must not inherit User")
    if len(observed_relations) != len(set(observed_relations)):
        raise ValueError("Duplicate use-case relationship instances detected")
    if set(observed_relations) != expected_relations:
        raise ValueError("Use-case relationship topology differs from the approved catalogue")
    if {(kind, source, target) for _, kind, source, target in observed_relations} != set(RELATIONSHIPS):
        raise ValueError("Generated pages do not realize exactly the ten distinct use-case relationships")
    return {
        "pages": len(diagrams),
        "distinct_use_cases": len(observed),
        "use_case_instances": uc_instances,
        "primary_associations": len(observed_associations),
        "relationship_instances": len(observed_relations),
    }


def write_atomically(
    output: Path,
    mxfile: ET.Element,
    specs: dict[str, dict[str, object]],
    pages: list[dict[str, object]],
) -> dict[str, int]:
    output.parent.mkdir(parents=True, exist_ok=True)
    xml_text = ET.tostring(mxfile, encoding="unicode", short_empty_elements=True) + "\n"
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{output.name}.", suffix=".tmp", dir=output.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(xml_text)
        reparsed = ET.parse(temporary).getroot()
        stats = validate_xml(reparsed, specs, pages)
        os.replace(temporary, output)
        return stats
    finally:
        if temporary.exists():
            temporary.unlink()


def element_signature(element: ET.Element) -> tuple[object, ...]:
    """Return an attribute-order-independent signature of an entire XML subtree."""

    return (
        element.tag,
        tuple(sorted(element.attrib.items())),
        element.text or "",
        element.tail or "",
        tuple(element_signature(child) for child in element),
    )


def document_signature(mxfile: ET.Element) -> tuple[object, ...]:
    """Return a strict signature covering every layout-bearing XML descendant."""

    return element_signature(mxfile)


def validate_existing(
    output: Path,
    specs: dict[str, dict[str, object]],
    pages: list[dict[str, object]],
) -> dict[str, int]:
    actual = ET.parse(output).getroot()
    stats = validate_xml(actual, specs, pages)
    expected = build_document(specs, pages)
    validate_xml(expected, specs, pages)
    repeated = build_document(specs, pages)
    if document_signature(expected) != document_signature(repeated):
        raise ValueError("Canonical Draw.io generation is not deterministic")
    if document_signature(actual) != document_signature(expected):
        raise ValueError(
            "Existing Draw.io topology or layout differs from the canonical generated document; "
            "regenerate it before approval"
        )
    return stats


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate the existing output instead of regenerating it",
    )
    args = parser.parse_args(list(argv) if argv is not None else None)

    source = args.source.resolve()
    output = args.output.resolve()
    if not args.check and source == output:
        parser.error("source and output must refer to different files")

    specs = parse_spec(source)
    pages = page_definitions()
    validate_page_definitions(pages, specs)
    if args.check:
        stats = validate_existing(output, specs, pages)
        action = "validated"
    else:
        document = build_document(specs, pages)
        validate_xml(document, specs, pages)
        repeated = build_document(specs, pages)
        if document_signature(document) != document_signature(repeated):
            raise ValueError("Canonical Draw.io generation is not deterministic")
        stats = write_atomically(output, document, specs, pages)
        action = "generated"

    print(
        f"{action}: {output} | pages={stats['pages']} | "
        f"distinct_use_cases={stats['distinct_use_cases']} | "
        f"use_case_instances={stats['use_case_instances']} | "
        f"primary_associations={stats['primary_associations']} | "
        f"relationship_instances={stats['relationship_instances']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
