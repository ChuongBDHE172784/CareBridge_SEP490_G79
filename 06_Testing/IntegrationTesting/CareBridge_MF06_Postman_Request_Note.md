1_MF06-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/internal/v2/triage/sessions](http://localhost:8080/api/internal/v2/triage/sessions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "profileId": "THAY_FILE_ID",
  "selectedTarget": "qa_selectedtarget_01",
  "journeyContext": "qa_journeycontext_01",
  "message": "qa_message_01",
  "messageId": "qa_messageid_01",
  "requestId": "qa_requestid_01",
  "consentContext": "qa_consentcontext_01",
  "signals": "qa_signals_01",
  "measurements": "qa_measurements_01"
}
```


2_MF06-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/consent/accept](http://localhost:8080/api/v1/triage/consent/accept)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "policyVersion": "qa_policyversion_01",
  "locale": "qa_locale_01"
}
```


3_MF06-ACTION-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake/continuations/resolve](http://localhost:8080/api/v1/triage/intake/continuations/resolve)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "token": "qa_token_01"
}
```


4_MF06-ACTION-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake/conversation/start](http://localhost:8080/api/v1/triage/intake/conversation/start)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "intakeSessionId": "THAY_SESSION_ID",
  "clientRequestId": "qa_clientrequestid_01",
  "initialText": "qa_initialtext_01",
  "currentIntake": "qa_currentintake_01",
  "stage": "PRECONCEPTION",
  "babyProfileId": "THAY_FILE_ID",
  "motherProfileId": "THAY_FILE_ID",
  "journeyId": "THAY_JOURNEY_ID",
  "originDashboard": "MOTHER_JOURNEY",
  "originReferenceId": "qa_originreferenceid_01"
}
```


5_MF06-ACTION-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/triage/intake/start](http://localhost:8080/triage/intake/start)

Body: Không có body.


6_MF06-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources](http://localhost:8080/admin/api/v1/evidence-sources)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "baseUrl": "https://example.invalid/qa-resource",
  "organization": "qa_organization_01",
  "category": "qa_category_01",
  "applicableStages": "qa_applicablestages_01",
  "notes": "qa_notes_01"
}
```


7_MF06-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/internal/v2/triage/sessions/THAY_SESSION_ID/messages](http://localhost:8080/api/internal/v2/triage/sessions/THAY_SESSION_ID/messages)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "sessionId": "THAY_SESSION_ID",
  "expectedStateVersion": 1,
  "message": "qa_message_01",
  "messageId": "qa_messageid_01",
  "requestId": "qa_requestid_01",
  "answers": [
    "qa_answer_01"
  ],
  "signals": "qa_signals_01",
  "measurements": "qa_measurements_01"
}
```


8_MF06-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/red-flag-rules](http://localhost:8080/api/v1/admin/red-flag-rules)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "keyword": "qa_keyword_01",
  "severity": "GREEN",
  "action": "BLOCK"
}
```


9_MF06-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/rag/answer](http://localhost:8080/api/v1/rag/answer)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "query": "qa_query_01",
  "userStage": "PRE_PREGNANCY",
  "topicId": "qa_topicid_01",
  "maxContextChunks": 1
}
```


10_MF06-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/consent/revoke](http://localhost:8080/api/v1/triage/consent/revoke)

Body: Không có body.


11_MF06-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake](http://localhost:8080/api/v1/triage/intake)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "symptoms": "qa_symptoms_01",
  "childAgeMonths": 1,
  "symptomList": [
    "qa_symptomlist_01"
  ],
  "duration": "qa_duration_01",
  "temperatureC": 1,
  "feedingStatus": "qa_feedingstatus_01",
  "breathingStatus": "qa_breathingstatus_01",
  "consciousnessStatus": "qa_consciousnessstatus_01",
  "painSeverity": "qa_painseverity_01",
  "urinarySymptoms": "qa_urinarysymptoms_01",
  "hydrationStatus": "qa_hydrationstatus_01",
  "vomiting": "qa_vomiting_01",
  "diarrhea": "qa_diarrhea_01",
  "rash": "qa_rash_01",
  "seizure": true,
  "dehydrationSigns": [
    "qa_dehydrationsign_01"
  ],
  "parentFreeText": "qa_parentfreetext_01",
  "babyProfileId": "THAY_FILE_ID",
  "motherProfileId": "THAY_FILE_ID",
  "stage": "PRECONCEPTION",
  "gestationalWeeks": 1,
  "abdominalPainPattern": "qa_abdominalpainpattern_01"
}
```


12_MF06-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake/continuations/acknowledge](http://localhost:8080/api/v1/triage/intake/continuations/acknowledge)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "token": "qa_token_01"
}
```


13_MF06-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake/conversation/continue](http://localhost:8080/api/v1/triage/intake/conversation/continue)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "intakeSessionId": "THAY_SESSION_ID",
  "currentIntake": "qa_currentintake_01",
  "messages": [
    "qa_message_01"
  ],
  "newAnswers": "qa_newanswers_01",
  "round": 1
}
```


14_MF06-CREATE-009

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/internal/triage/v2/turn](http://localhost:8080/internal/triage/v2/turn)

Body: Không có body.


15_MF06-CREATE-010

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/triage/child](http://localhost:8080/triage/child)

Body: Không có body.


16_MF06-CREATE-011

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/triage/intake/continue](http://localhost:8080/triage/intake/continue)

Body: Không có body.


17_MF06-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/internal/v2/triage/sessions/THAY_SESSION_ID](http://localhost:8080/api/internal/v2/triage/sessions/THAY_SESSION_ID)

Body: Không có body.


18_MF06-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/red-flag-rules/THAY_ID](http://localhost:8080/api/v1/admin/red-flag-rules/THAY_ID)

Body: Không có body.


19_MF06-DELETE-003

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/health-memory/THAY_ENTRY_ID](http://localhost:8080/api/v1/triage/health-memory/THAY_ENTRY_ID)

Body: Không có body.


20_MF06-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources](http://localhost:8080/admin/api/v1/evidence-sources)

Body: Không có body.


21_MF06-PRIVACY-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources](http://localhost:8080/admin/api/v1/evidence-sources)

Body: Không có body.


22_MF06-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources](http://localhost:8080/admin/api/v1/evidence-sources)

Body: Không có body.


23_MF06-SEARCH-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/red-flag-rules](http://localhost:8080/api/v1/admin/red-flag-rules)

Body: Không có body.


24_MF06-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/health-memory](http://localhost:8080/api/v1/triage/health-memory)

Body: Không có body.


25_MF06-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake](http://localhost:8080/api/v1/triage/intake)

Body: Không có body.


26_MF06-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources](http://localhost:8080/admin/api/v1/evidence-sources)

Body: Không có body.


27_MF06-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/approve](http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/approve)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "notes": "qa_notes_01"
}
```


28_MF06-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/deprecate](http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/deprecate)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "notes": "qa_notes_01"
}
```


29_MF06-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/reject](http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/reject)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "notes": "qa_notes_01"
}
```


30_MF06-UPDATE-004

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/red-flag-rules/THAY_ID](http://localhost:8080/api/v1/admin/red-flag-rules/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "keyword": "qa_keyword_01",
  "severity": "GREEN",
  "action": "BLOCK",
  "isActive": true
}
```


31_MF06-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/review-log](http://localhost:8080/admin/api/v1/evidence-sources/THAY_ID/review-log)

Body: Không có body.


32_MF06-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/internal/v2/triage/sessions/THAY_SESSION_ID](http://localhost:8080/api/internal/v2/triage/sessions/THAY_SESSION_ID)

Body: Không có body.


33_MF06-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/consent](http://localhost:8080/api/v1/triage/consent)

Body: Không có body.


34_MF06-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/triage/intake/THAY_SESSION_ID](http://localhost:8080/api/v1/triage/intake/THAY_SESSION_ID)

Body: Không có body.


35_MF06-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/health](http://localhost:8080/health)

Body: Không có body.


36_MF06-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/internal/api/v1/triage/evidence-sources/approved](http://localhost:8080/internal/api/v1/triage/evidence-sources/approved)

Body: Không có body.
