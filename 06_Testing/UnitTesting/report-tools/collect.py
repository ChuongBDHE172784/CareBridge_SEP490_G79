"""Normalise backend/mobile/web test results into one all-tests.json."""
import glob, json, os, re, xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))          # 06_Testing/UnitTesting/report-tools
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))  # repo root
API = f"{ROOT}/05_Development/CareBridgeAPI"
MOB = f"{ROOT}/05_Development/CareBridgeMobileApp"
WEB = f"{ROOT}/05_Development/CareBridgeWebApp"

INTEG = re.compile(r"(IntegrationTest|PostgresTest|EmbeddedPostgresTest|Testcontainers|SmokeTest)$")
DOCKER = re.compile(r"AbstractPostgresIntegrationTest|AbstractEmbeddedPostgresIntegrationTest|DockerClient|testcontainers", re.I)

def _repo_relative(path: str) -> str:
    """Trim the machine-specific prefix.

    The project path contains Vietnamese diacritics and the toolchains disagree on Unicode
    normalisation (NFC vs NFD), so a plain prefix replace does not match. Cut at the first
    repository-level marker instead.
    """
    marker = "05_Development/"
    index = path.find(marker)
    return path[index:] if index >= 0 else path


rows = []

# ---------- backend ----------
for f in glob.glob(f"{API}/target/surefire-reports/TEST-*.xml"):
    try:
        suite = ET.parse(f).getroot()
    except Exception:
        continue
    fqcn = suite.get("name")
    short = fqcn.rsplit(".", 1)[-1]
    for tc in suite.iter("testcase"):
        status, detail = "passed", ""
        for kind, mapped in (("failure", "failed"), ("error", "failed"), ("skipped", "untested")):
            node = tc.find(kind)
            if node is not None:
                status = mapped
                detail = (node.get("message") or "") + (node.text or "")
                break
        # A class that boots Testcontainers / embedded Postgres is an integration test even
        # when its name does not say so; with Docker off it fails for environmental reasons.
        env_bound = bool(DOCKER.search(detail))
        rows.append({
            "layer": "backend",
            "unit": not bool(INTEG.search(short)) and not env_bound,
            "container": fqcn,
            "short": short,
            "name": tc.get("name") or "",
            "status": status,
            "detail": detail[:400],
        })

# ---------- mobile ----------
suites, tests, groups, errors = {}, {}, {}, {}
mob_path = f"{HERE}/mobile-test.json"
if os.path.exists(mob_path):
    for line in open(mob_path):
        line = line.strip()
        if not line.startswith("{"):
            continue
        try:
            e = json.loads(line)
        except Exception:
            continue
        t = e.get("type")
        if t == "suite":
            suites[e["suite"]["id"]] = e["suite"]["path"]
        elif t == "group":
            groups[e["group"]["id"]] = e["group"].get("name") or ""
        elif t == "testStart":
            tests[e["test"]["id"]] = (
                e["test"]["name"],
                e["test"].get("suiteID"),
                e["test"].get("groupIDs") or [],
            )
        elif t == "error" and e.get("testID") is not None:
            errors.setdefault(e["testID"], []).append(str(e.get("error") or ""))
        elif t == "testDone":
            if e.get("hidden"):
                continue
            nm, sid, gids = tests.get(e["testID"], ("?", None, []))
            path = _repo_relative(suites.get(sid) or "?")
            res = e.get("result")
            group = " > ".join(g for g in (groups.get(i, "") for i in gids) if g)
            # The Dart reporter repeats the group prefix in the test name; keep the leaf only.
            leaf = nm[len(group) + 1:] if group and nm.startswith(group + " ") else nm
            rows.append({
                "layer": "mobile",
                "unit": True,
                "container": path,
                "short": os.path.basename(path),
                "name": nm,
                "group": group,
                "leaf": leaf,
                "status": "passed" if res == "success" else ("untested" if e.get("skipped") else "failed"),
                "detail": " ".join(errors.get(e["testID"], []))[:400],
            })

# ---------- web ----------
web_path = f"{HERE}/web-test.json"
if os.path.exists(web_path):
    d = json.load(open(web_path))
    for tr in d["testResults"]:
        path = _repo_relative(tr["name"])
        for a in tr["assertionResults"]:
            full = a.get("fullName") or a.get("title") or ""
            title = a.get("title") or full
            group = full[: -len(title)].strip() if title and full.endswith(title) else ""
            rows.append({
                "layer": "web",
                "unit": True,
                "container": path,
                "short": os.path.basename(path),
                "name": full,
                "group": group,
                "leaf": title,
                "status": {"passed": "passed", "failed": "failed"}.get(a["status"], "untested"),
                "detail": " ".join(a.get("failureMessages") or [])[:400],
            })

json.dump(rows, open(f"{HERE}/all-tests.json", "w"), ensure_ascii=False)
from collections import Counter
print("rows:", len(rows))
print("by layer:", Counter(r["layer"] for r in rows))
print("unit only:", Counter(r["status"] for r in rows if r["unit"]))
print("integration:", Counter(r["status"] for r in rows if not r["unit"]))
