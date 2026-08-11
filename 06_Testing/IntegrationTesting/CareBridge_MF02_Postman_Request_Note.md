1_MF02-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/safety-check](http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/safety-check)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "q1NoDizziness": true,
  "q2NoContractions": true,
  "q3NoBleeding": true,
  "q4HydratedAndFed": true,
  "journeyId": "THAY_JOURNEY_ID",
  "notes": "qa_notes_01"
}
```


2_MF02-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/sessions](http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/sessions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "safetyCheckId": "qa_safetycheckid_01",
  "journeyId": "THAY_JOURNEY_ID"
}
```


3_MF02-ACTION-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journey-onboarding](http://localhost:8080/api/v1/journey-onboarding)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "submissionId": "qa_submissionid_01",
  "lifecycleGoal": "PREPARING_FOR_PREGNANCY",
  "locale": "qa_locale_01",
  "timeZone": "qa_timezone_01",
  "consentAccepted": true,
  "policyVersion": "qa_policyversion_01"
}
```


4_MF02-ACTION-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/notifications/send](http://localhost:8080/api/v1/notifications/send)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "recipientUserId": "THAY_USER_ID",
  "type": "REMINDER",
  "title": "qa_title_01",
  "body": "qa_body_01",
  "referenceId": "qa_referenceid_01",
  "referenceType": "qa_referencetype_01"
}
```


5_MF02-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises](http://localhost:8080/api/v1/admin/exercises)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "description": "qa_description_01",
  "trimesterScope": "FIRST",
  "difficultyLevel": "EASY",
  "durationMinutes": 1,
  "instructionContent": "qa_instructioncontent_01",
  "mediaUrl": "https://example.invalid/qa-resource",
  "safetyWarning": "qa_safetywarning_01",
  "supportsPostureAnalysis": true
}
```


6_MF02-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/posture-configs](http://localhost:8080/api/v1/admin/posture-configs)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "exerciseId": "qa_exerciseid_01",
  "analysisMode": "RULE_BASED",
  "ruleOrModelVersion": "qa_ruleormodelversion_01",
  "confidenceThreshold": 1,
  "feedbackLevel": "SILENT",
  "configJson": "qa_configjson_01"
}
```


7_MF02-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/posture-configs/THAY_EXERCISE_ID/versions](http://localhost:8080/api/v1/admin/posture-configs/THAY_EXERCISE_ID/versions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "analysisMode": "RULE_BASED",
  "ruleOrModelVersion": "qa_ruleormodelversion_01",
  "confidenceThreshold": 1,
  "feedbackLevel": "SILENT",
  "configJson": "qa_configjson_01"
}
```


8_MF02-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/appointments](http://localhost:8080/api/v1/appointments)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reminderType": "APPOINTMENT",
  "title": "qa_title_01",
  "scheduledAt": "2026-10-07T09:00:00+07:00",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-10-07",
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID",
  "notificationOffsetsMinutes": [
    1
  ],
  "timeZone": "qa_timezone_01"
}
```


9_MF02-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/posture-events](http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/posture-events)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "eventTimeMs": 1,
  "keypointSummaryJson": "qa_keypointsummaryjson_01"
}
```


10_MF02-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/files](http://localhost:8080/api/v1/files)

Body: Không có body.


11_MF02-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/files/health-records](http://localhost:8080/api/v1/files/health-records)

Body: Không có body.


12_MF02-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/files/upload/with-purpose](http://localhost:8080/api/v1/files/upload/with-purpose)

Body: Không có body.


13_MF02-CREATE-009

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-records](http://localhost:8080/api/v1/health-records)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "recordType": "ULTRASOUND",
  "title": "qa_title_01",
  "recordDate": "2026-10-07",
  "facilityName": "qa_facilityname_01",
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID",
  "fileIds": [
    "THAY_FILE_ID"
  ]
}
```


14_MF02-CREATE-010

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-summaries](http://localhost:8080/api/v1/health-summaries)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID",
  "24H": "qa_24h_01",
  "summaryPeriod": "qa_summaryperiod_01",
  "periodStart": "2026-10-07",
  "periodEnd": "2026-10-07",
  "summaryJson": "qa_summaryjson_01"
}
```


15_MF02-CREATE-011

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-summaries/share](http://localhost:8080/api/v1/health-summaries/share)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "summaryId": "qa_summaryid_01",
  "bookingId": "qa_bookingid_01"
}
```


16_MF02-CREATE-012

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys](http://localhost:8080/api/v1/journeys)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "journeyType": "PRE_PREGNANCY",
  "startDate": "2026-10-07",
  "lastMenstrualDate": "2026-10-07",
  "estimatedDueDate": "2026-10-07",
  "dateSource": "2026-10-07",
  "dateConfidence": "2026-10-07",
  "changeReason": "qa_changereason_01",
  "effectiveAt": "2026-10-07T09:00:00+07:00",
  "notes": "qa_notes_01"
}
```


17_MF02-CREATE-013

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "metricType": "qa_metrictype_01",
  "valueNumeric": 1,
  "valueSecondary": 1,
  "unit": "qa_unit_01",
  "measuredAt": "2026-10-07T09:00:00+07:00",
  "sourceType": "MANUAL",
  "note": "qa_note_01",
  "context": "qa_context_01",
  "periodStart": "2026-10-07T09:00:00+07:00",
  "periodEnd": "2026-10-07T09:00:00+07:00",
  "definitionVersion": 1
}
```


18_MF02-CREATE-014

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/postpartum-logs](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/postpartum-logs)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "submissionId": "qa_submissionid_01",
  "logDate": "2026-10-07",
  "painLevel": 1,
  "bleedingLevel": "NONE",
  "moodLevel": 1,
  "sleepHours": 1,
  "breastfeedingNote": "qa_breastfeedingnote_01",
  "symptomNote": "qa_symptomnote_01"
}
```


19_MF02-CREATE-015

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/pregnancy-outcomes](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/pregnancy-outcomes)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "submissionId": "qa_submissionid_01",
  "expectedJourneyVersion": 1,
  "outcomeType": "ONGOING",
  "outcomeDate": "2026-10-07",
  "source": "SELF_REPORTED",
  "reason": "qa_reason_01",
  "effectiveAt": "2026-10-07T09:00:00+07:00",
  "correction": true
}
```


20_MF02-CREATE-016

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminder-schedules](http://localhost:8080/api/v1/reminder-schedules)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "times": [
    "qa_time_01"
  ],
  "timeZone": "qa_timezone_01",
  "recurrence": "NONE",
  "startDate": "2026-10-07",
  "endDate": "2026-10-07",
  "active": true
}
```


21_MF02-CREATE-017

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders](http://localhost:8080/api/v1/reminders)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reminderType": "APPOINTMENT",
  "title": "qa_title_01",
  "scheduledAt": "2026-10-07T09:00:00+07:00",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-10-07",
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID",
  "notificationOffsetsMinutes": [
    1
  ],
  "timeZone": "qa_timezone_01"
}
```


22_MF02-CREATE-018

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/medication](http://localhost:8080/api/v1/reminders/medication)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "scheduledAt": "2026-10-07T09:00:00+07:00",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-10-07",
  "journeyId": "THAY_JOURNEY_ID"
}
```


23_MF02-CREATE-019

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/skip](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/skip)

Body: Không có body.


24_MF02-CREATE-020

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/v1/inference/landmarks](http://localhost:8080/v1/inference/landmarks)

Body: Không có body.


25_MF02-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/appointments/THAY_APPOINTMENT_ID](http://localhost:8080/api/v1/appointments/THAY_APPOINTMENT_ID)

Body: Không có body.


26_MF02-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/files/THAY_FILE_ID](http://localhost:8080/api/v1/files/THAY_FILE_ID)

Body: Không có body.


27_MF02-DELETE-003

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-metrics/THAY_METRIC_ID](http://localhost:8080/api/v1/health-metrics/THAY_METRIC_ID)

Body: Không có body.


28_MF02-DELETE-004

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/postpartum-logs/THAY_LOG_ID](http://localhost:8080/api/v1/postpartum-logs/THAY_LOG_ID)

Body: Không có body.


29_MF02-DELETE-005

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminder-schedules/THAY_SCHEDULE_ID](http://localhost:8080/api/v1/reminder-schedules/THAY_SCHEDULE_ID)

Body: Không có body.


30_MF02-DELETE-006

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID)

Body: Không có body.


31_MF02-DELETE-007

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/permanent](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/permanent)

Body: Không có body.


32_MF02-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises](http://localhost:8080/api/v1/admin/exercises)

Body: Không có body.


33_MF02-PRIVACY-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises](http://localhost:8080/api/v1/admin/exercises)

Body: Không có body.


34_MF02-REVOKE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID/disable](http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID/disable)

Body: Không có body.


35_MF02-REVOKE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-records/THAY_ID/archive](http://localhost:8080/api/v1/health-records/THAY_ID/archive)

Body: Không có body.


36_MF02-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises](http://localhost:8080/api/v1/admin/exercises)

Body: Không có body.


37_MF02-SEARCH-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/posture-configs/THAY_EXERCISE_ID](http://localhost:8080/api/v1/admin/posture-configs/THAY_EXERCISE_ID)

Body: Không có body.


38_MF02-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/appointments](http://localhost:8080/api/v1/appointments)

Body: Không có body.


39_MF02-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/appointments](http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/appointments)

Body: Không có body.


40_MF02-SEARCH-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises](http://localhost:8080/api/v1/exercises)

Body: Không có body.


41_MF02-SEARCH-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/sessions/history](http://localhost:8080/api/v1/exercises/sessions/history)

Body: Không có body.


42_MF02-SEARCH-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-summaries](http://localhost:8080/api/v1/health-summaries)

Body: Không có body.


43_MF02-SEARCH-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/history](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/history)

Body: Không có body.


44_MF02-SEARCH-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/postpartum-logs](http://localhost:8080/api/v1/postpartum-logs)

Body: Không có body.


45_MF02-SEARCH-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminder-schedules](http://localhost:8080/api/v1/reminder-schedules)

Body: Không có body.


46_MF02-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises](http://localhost:8080/api/v1/admin/exercises)

Body: Không có body.


47_MF02-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID/activate](http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID/activate)

Body: Không có body.


48_MF02-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/posture-configs/THAY_POSTURE_CONFIG_ID/activate](http://localhost:8080/api/v1/admin/posture-configs/THAY_POSTURE_CONFIG_ID/activate)

Body: Không có body.


49_MF02-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/appointments/THAY_APPOINTMENT_ID](http://localhost:8080/api/v1/appointments/THAY_APPOINTMENT_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "scheduledAt": "2026-10-07T09:00:00+07:00",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-10-07",
  "recurrenceEndDateSet": "2026-10-07",
  "notificationOffsetsMinutes": [
    1
  ],
  "notificationOffsetsMinutesSet": true,
  "timeZone": "qa_timezone_01"
}
```


50_MF02-UPDATE-004

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/complete](http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/complete)

Body: Không có body.


51_MF02-UPDATE-005

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/pause](http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/pause)

Body: Không có body.


52_MF02-UPDATE-006

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/resume](http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/resume)

Body: Không có body.


53_MF02-UPDATE-007

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-records/THAY_ID](http://localhost:8080/api/v1/health-records/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "recordType": "qa_recordtype_01",
  "recordDate": "2026-10-07",
  "sourceType": "qa_sourcetype_01",
  "sourceName": "qa_sourcename_01",
  "fileUrl": "https://example.invalid/qa-resource",
  "babyId": "THAY_BABY_ID",
  "journeyId": "THAY_JOURNEY_ID",
  "fileIds": "qa_fileids_01"
}
```


54_MF02-UPDATE-008

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/postpartum-logs/THAY_LOG_ID](http://localhost:8080/api/v1/postpartum-logs/THAY_LOG_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "logDate": "2026-10-07",
  "painLevel": 1,
  "bleedingLevel": "NONE",
  "moodLevel": 1,
  "sleepHours": 1,
  "breastfeedingNote": "qa_breastfeedingnote_01",
  "symptomNote": "qa_symptomnote_01",
  "breastfeedingNotePresent": true,
  "symptomNotePresent": true
}
```


55_MF02-UPDATE-009

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminder-schedules/THAY_SCHEDULE_ID](http://localhost:8080/api/v1/reminder-schedules/THAY_SCHEDULE_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "times": [
    "qa_time_01"
  ],
  "timeZone": "qa_timezone_01",
  "recurrence": "NONE",
  "startDate": "2026-10-07",
  "endDate": "2026-10-07",
  "active": true,
  "endDateSet": "2026-10-07"
}
```


56_MF02-UPDATE-010

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "scheduledAt": "2026-10-07T09:00:00+07:00",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-10-07",
  "recurrenceEndDateSet": "2026-10-07",
  "notificationOffsetsMinutes": [
    1
  ],
  "notificationOffsetsMinutesSet": true,
  "timeZone": "qa_timezone_01"
}
```


57_MF02-UPDATE-011

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/complete](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/complete)

Body: Không có body.


58_MF02-UPDATE-012

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/enable](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/enable)

Body: Không có body.


59_MF02-UPDATE-013

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/skip](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/skip)

Body: Không có body.


60_MF02-UPDATE-014

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/snooze](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID/snooze)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "snoozedUntil": "2026-10-07T09:00:00+07:00"
}
```


61_MF02-UPDATE-015

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID](http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "description": "qa_description_01",
  "trimesterScope": "FIRST",
  "difficultyLevel": "EASY",
  "durationMinutes": 1,
  "instructionContent": "qa_instructioncontent_01",
  "mediaUrl": "https://example.invalid/qa-resource",
  "safetyWarning": "qa_safetywarning_01",
  "supportsPostureAnalysis": true
}
```


62_MF02-UPDATE-016

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "journeyType": "PRE_PREGNANCY",
  "lastMenstrualDate": "2026-10-07",
  "estimatedDueDate": "2026-10-07",
  "deliveryDate": "2026-10-07",
  "dateSource": "2026-10-07",
  "dateConfidence": "2026-10-07",
  "changeReason": "qa_changereason_01",
  "effectiveAt": "2026-10-07T09:00:00+07:00",
  "notes": "qa_notes_01",
  "status": "qa_status_01"
}
```


63_MF02-UPDATE-017

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics/THAY_METRIC_ID](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics/THAY_METRIC_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "valueNumeric": 1,
  "valueSecondary": 1,
  "unit": "qa_unit_01",
  "measuredAt": "2026-10-07T09:00:00+07:00",
  "note": "qa_note_01",
  "context": "qa_context_01",
  "periodStart": "2026-10-07T09:00:00+07:00",
  "periodEnd": "2026-10-07T09:00:00+07:00"
}
```


64_MF02-UPDATE-018

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/notifications/read-all](http://localhost:8080/api/v1/notifications/read-all)

Body: Không có body.


65_MF02-UPDATE-019

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/notifications/THAY_NOTIFICATION_ID/read](http://localhost:8080/api/v1/notifications/THAY_NOTIFICATION_ID/read)

Body: Không có body.


66_MF02-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID](http://localhost:8080/api/v1/admin/exercises/THAY_EXERCISE_ID)

Body: Không có body.


67_MF02-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/appointments/THAY_APPOINTMENT_ID](http://localhost:8080/api/v1/appointments/THAY_APPOINTMENT_ID)

Body: Không có body.


68_MF02-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/appointments/THAY_APPOINTMENT_ID](http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/appointments/THAY_APPOINTMENT_ID)

Body: Không có body.


69_MF02-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/result](http://localhost:8080/api/v1/exercises/sessions/THAY_SESSION_ID/result)

Body: Không có body.


70_MF02-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID](http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID)

Body: Không có body.


71_MF02-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/posture-config](http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/posture-config)

Body: Không có body.


72_MF02-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/safety-check/latest](http://localhost:8080/api/v1/exercises/THAY_EXERCISE_ID/safety-check/latest)

Body: Không có body.


73_MF02-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/files/THAY_FILE_ID](http://localhost:8080/api/v1/files/THAY_FILE_ID)

Body: Không có body.


74_MF02-VIEW-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-metrics/THAY_METRIC_ID](http://localhost:8080/api/v1/health-metrics/THAY_METRIC_ID)

Body: Không có body.


75_MF02-VIEW-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-records](http://localhost:8080/api/v1/health-records)

Body: Không có body.


76_MF02-VIEW-011

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-records/timeline](http://localhost:8080/api/v1/health-records/timeline)

Body: Không có body.


77_MF02-VIEW-012

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-records/THAY_RECORD_ID](http://localhost:8080/api/v1/health-records/THAY_RECORD_ID)

Body: Không có body.


78_MF02-VIEW-013

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/health-summaries/THAY_SUMMARY_ID](http://localhost:8080/api/v1/health-summaries/THAY_SUMMARY_ID)

Body: Không có body.


79_MF02-VIEW-014

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journey-onboarding/status](http://localhost:8080/api/v1/journey-onboarding/status)

Body: Không có body.


80_MF02-VIEW-015

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/me/dashboard](http://localhost:8080/api/v1/journeys/me/dashboard)

Body: Không có body.


81_MF02-VIEW-016

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics)

Body: Không có body.


82_MF02-VIEW-017

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics/capabilities](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/metrics/capabilities)

Body: Không có body.


83_MF02-VIEW-018

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/timeline](http://localhost:8080/api/v1/journeys/THAY_JOURNEY_ID/timeline)

Body: Không có body.


84_MF02-VIEW-019

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/postpartum-logs/THAY_LOG_ID](http://localhost:8080/api/v1/postpartum-logs/THAY_LOG_ID)

Body: Không có body.


85_MF02-VIEW-020

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminder-schedules/THAY_SCHEDULE_ID](http://localhost:8080/api/v1/reminder-schedules/THAY_SCHEDULE_ID)

Body: Không có body.


86_MF02-VIEW-021

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders](http://localhost:8080/api/v1/reminders)

Body: Không có body.


87_MF02-VIEW-022

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/today](http://localhost:8080/api/v1/reminders/today)

Body: Không có body.


88_MF02-VIEW-023

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID](http://localhost:8080/api/v1/reminders/THAY_REMINDER_ID)

Body: Không có body.


89_MF02-VIEW-024

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/health (exercise-correction sidecar)](http://localhost:8080/health (exercise-correction sidecar))

Body: Không có body.
