```text
You are completing the existing CareBridge Integration Test Google Sheet from verified repository evidence.

OUTPUT CONTRACT — ABSOLUTE:
1. Output ONLY Markdown tables. Do not output narrative paragraphs, introductions, conclusions, rationale, warnings, or commentary.
2. Work TAB-BY-TAB. For every functional tab, print exactly `## [Tab Name]` followed by exactly one Markdown table.
3. Use this EXACT existing 16-column schema, in this exact order and with this exact number of columns:
`Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Evidence | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Note`
4. Do not add, delete, rename, reorder, merge, or split columns. Fill content only.
5. Do not redesign Cover, Test Cases, Test Statistics, or any functional tab. Preserve existing sheet structure, merged cells, formulas, formatting, test rounds, and school-style grouping logic.
6. Keep mandatory template tabs when relevant:
- Cover
- Test Cases
- Test Statistics
- Account, Trust & Access Control
- Mother Care Journey
- Baby Care Journey, Growth & Vaccination
- Community Q&A & Moderation
- Verified Expert Network & Contribution
- AI Nurse Assistant & Risk Triage
- Emergency Map & Nearby Care Support
- Family Sync & Cooperative Care
- Verified Content & Checklist Hub
- Smart Activity Monitoring & Safety Support
7. Start with the highest-confidence functional tabs first, while retaining the final tab names above.
8. Use only code-backed scenarios from the supplied reverse-engineering report and API Evidence catalog. Use `NEEDS_CONFIRMATION` wherever evidence is not safe or complete.
9. Keep wording concise, professional, academic, and suitable for direct copy-paste into Google Sheets.
10. Focus on integration flows across caller/API/service/repository/database/external boundaries. Avoid isolated unit checks.
11. Never invent endpoints, roles, payloads, responses, credentials, test results, dates, testers, or screenshot evidence.
12. Do not mark a case Passed or Failed without actual execution evidence. Preserve existing execution cells unless explicitly instructed otherwise.
13. Do not add Base URL, Endpoint, HTTP Method, Auth, Headers, Params, or Payload columns. Those fields belong only in the separate API Evidence Markdown catalog.
14. Preserve the existing Test Case IDs when updating existing rows. Do not silently renumber cases.

PROJECT REFERENCES:
- Integration Test sheet: https://docs.google.com/spreadsheets/d/1PBfbN8MOitmgmMM6-Ab6mf9YHYGxcbd_8Ci-0QO3HH0/edit?usp=sharing
- Unit Test sheet: https://docs.google.com/spreadsheets/d/19kZiivQ9ka49XEFEc72h-GZovMMK_PN-UaYNB5HFo1g/edit?usp=sharing
- School sample template: https://docs.google.com/spreadsheets/d/11v8a5n-ZDhmM_LVoZOF0iR3sObAVnZSv/edit?gid=450496332#gid=450496332
- Report 1 Project Introduction: https://www.genspark.ai/api/files/s/1v6b2PYw
- Report 3 SRS: https://www.genspark.ai/api/files/s/ze5anBcF

SOURCE PRIORITY:
1. Current local `dev` backend controller/security/DTO/service/repository code.
2. Current web/mobile API callers and screens.
3. Runtime environment/configuration and Postman/OpenAPI artifacts.
4. Local Report 1/Report 3 only as business context; implementation wins on conflict.

FINAL RESPONSE RULE: output Markdown tables only, with no text before the first tab table and no text after the final tab table.
```
