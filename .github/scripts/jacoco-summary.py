
from pathlib import Path
import os
import xml.etree.ElementTree as ET

root = Path.cwd()
reports = sorted(root.glob("**/target/site/jacoco/jacoco.xml"))
reports = [p for p in reports if "jacoco-aggregate" not in p.parts]

def counters(report):
    xml = ET.parse(report).getroot()
    return {
        c.get("type"): (
            int(c.get("covered")),
            int(c.get("missed"))
        )
        for c in xml.findall("counter")
    }

def percentage(value):
    covered, missed = value
    total = covered + missed
    return f"{100 * covered / total:.1f}%" if total else "N/A"

totals = {}
rows = ["## JaCoCo Code Coverage", "",
        "| Module | Instructions | Branches | Lines |",
        "|---|---:|---:|---:|"]

for report in reports:
    module = report.parent.parent.parent.relative_to(root)
    data = counters(report)

    for kind, (covered, missed) in data.items():
        previous = totals.get(kind, (0, 0))
        totals[kind] = (
            previous[0] + covered,
            previous[1] + missed
        )

    def fmt(kind):
        return percentage(data[kind]) if kind in data else "N/A"

    rows.append(
        f"| `{module}` | {fmt('INSTRUCTION')} | "
        f"{fmt('BRANCH')} | {fmt('LINE')} |"
    )

if reports:
    def total(kind):
        return percentage(totals[kind]) if kind in totals else "N/A"

    rows.append(
        f"| **Total** | **{total('INSTRUCTION')}** | "
        f"**{total('BRANCH')}** | **{total('LINE')}** |"
    )
else:
    rows.append("")
    rows.append("No JaCoCo XML reports found.")

summary = "\n".join(rows) + "\n"
print(summary)

step_summary = os.getenv("GITHUB_STEP_SUMMARY")
if step_summary:
    # Append the summary to the GitHub Actions step summary file
    with open(step_summary, "a") as f:
        f.write(summary)
