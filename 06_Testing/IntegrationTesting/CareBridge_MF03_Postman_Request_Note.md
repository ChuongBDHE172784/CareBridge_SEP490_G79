1_MF03-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/completions](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/completions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "vaccineName": "qa_vaccinename_01",
  "doseNumber": 1,
  "administeredDate": "2026-10-07",
  "facilityName": "qa_facilityname_01",
  "proofRecordId": "THAY_RECORD_ID"
}
```


2_MF03-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies](http://localhost:8080/api/v1/babies)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "nickname": "qa_nickname_01",
  "birthDate": "2026-10-07",
  "gender": "MALE",
  "birthWeightKg": "2026-10-07",
  "birthLengthCm": "2026-10-07"
}
```


3_MF03-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/archive](http://localhost:8080/api/v1/babies/THAY_BABY_ID/archive)

Body: Không có body.


4_MF03-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs](http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "logType": "qa_logtype_01",
  "startedAt": "2026-10-07T09:00:00+07:00",
  "endedAt": "2026-10-07T09:00:00+07:00",
  "quantity": 1,
  "unit": "qa_unit_01",
  "note": "qa_note_01"
}
```


5_MF03-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements](http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "measuredDate": "2026-10-07",
  "weightKg": 1,
  "heightCm": 1,
  "headCircumferenceCm": 1,
  "sourceType": "qa_sourcetype_01",
  "note": "qa_note_01"
}
```


6_MF03-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones](http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "milestoneType": "qa_milestonetype_01",
  "achievedDate": "2026-10-07",
  "note": "qa_note_01",
  "sourceType": "qa_sourcetype_01"
}
```


7_MF03-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/vaccination](http://localhost:8080/api/v1/reminders/vaccination)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "babyId": "THAY_BABY_ID",
  "title": "qa_title_01",
  "scheduledAt": "2026-10-07T09:00:00+07:00",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-10-07",
  "journeyId": "THAY_JOURNEY_ID"
}
```


8_MF03-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/postponements](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/postponements)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "vaccineName": "qa_vaccinename_01",
  "doseNumber": 1,
  "newScheduledDate": "2026-10-07",
  "reason": "qa_reason_01"
}
```


9_MF03-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "vaccineName": "qa_vaccinename_01",
  "doseNumber": 1,
  "administeredDate": "2026-10-07",
  "facilityName": "qa_facilityname_01",
  "proofRecordId": "THAY_RECORD_ID"
}
```


10_MF03-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/THAY_LOG_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/THAY_LOG_ID)

Body: Không có body.


11_MF03-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements/THAY_GROWTH_MEASUREMENT_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements/THAY_GROWTH_MEASUREMENT_ID)

Body: Không có body.


12_MF03-DELETE-003

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones/THAY_MILESTONE_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones/THAY_MILESTONE_ID)

Body: Không có body.


13_MF03-DELETE-004

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records/THAY_RECORD_ID](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records/THAY_RECORD_ID)

Body: Không có body.


14_MF03-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies](http://localhost:8080/api/v1/babies)

Body: Không có body.


15_MF03-PRIVACY-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies](http://localhost:8080/api/v1/babies)

Body: Không có body.


16_MF03-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies](http://localhost:8080/api/v1/babies)

Body: Không có body.


17_MF03-SEARCH-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements](http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements)

Body: Không có body.


18_MF03-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones](http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones)

Body: Không có body.


19_MF03-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records)

Body: Không có body.


20_MF03-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies](http://localhost:8080/api/v1/babies)

Body: Không có body.


21_MF03-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/active](http://localhost:8080/api/v1/babies/THAY_BABY_ID/active)

Body: Không có body.


22_MF03-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements/THAY_GROWTH_MEASUREMENT_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-measurements/THAY_GROWTH_MEASUREMENT_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "measuredDate": "2026-10-07",
  "weightKg": 1,
  "heightCm": 1,
  "headCircumferenceCm": 1,
  "sourceType": "qa_sourcetype_01",
  "note": "qa_note_01"
}
```


23_MF03-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones/THAY_MILESTONE_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/milestones/THAY_MILESTONE_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "achievedDate": "2026-10-07",
  "note": "qa_note_01",
  "status": "qa_status_01"
}
```


24_MF03-UPDATE-004

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records/THAY_RECORD_ID](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/records/THAY_RECORD_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "vaccineName": "qa_vaccinename_01",
  "doseNumber": 1,
  "administeredDate": "2026-10-07",
  "facilityName": "qa_facilityname_01",
  "proofRecordId": "THAY_RECORD_ID",
  "clearProof": true,
  "unknownFields": "qa_unknownfields_01"
}
```


25_MF03-UPDATE-005

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "nickname": "qa_nickname_01",
  "birthDate": "2026-10-07",
  "gender": "MALE",
  "birthWeightKg": "2026-10-07",
  "birthLengthCm": "2026-10-07"
}
```


26_MF03-UPDATE-006

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/THAY_LOG_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/THAY_LOG_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "startedAt": "2026-10-07T09:00:00+07:00",
  "endedAt": "2026-10-07T09:00:00+07:00",
  "quantity": 1,
  "unit": "qa_unit_01",
  "note": "qa_note_01"
}
```


27_MF03-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID)

Body: Không có body.


28_MF03-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/appointment-preparation-summary](http://localhost:8080/api/v1/babies/THAY_BABY_ID/appointment-preparation-summary)

Body: Không có body.


29_MF03-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/care-overview](http://localhost:8080/api/v1/babies/THAY_BABY_ID/care-overview)

Body: Không có body.


30_MF03-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/care-timeline](http://localhost:8080/api/v1/babies/THAY_BABY_ID/care-timeline)

Body: Không có body.


31_MF03-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs](http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs)

Body: Không có body.


32_MF03-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/summary](http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/summary)

Body: Không có body.


33_MF03-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/THAY_LOG_ID](http://localhost:8080/api/v1/babies/THAY_BABY_ID/daily-logs/THAY_LOG_ID)

Body: Không có body.


34_MF03-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-chart](http://localhost:8080/api/v1/babies/THAY_BABY_ID/growth-chart)

Body: Không có body.


35_MF03-VIEW-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reminders/vaccination/suggestions](http://localhost:8080/api/v1/reminders/vaccination/suggestions)

Body: Không có body.


36_MF03-VIEW-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/schedule](http://localhost:8080/api/v1/vaccination/babies/THAY_BABY_ID/schedule)

Body: Không có body.
