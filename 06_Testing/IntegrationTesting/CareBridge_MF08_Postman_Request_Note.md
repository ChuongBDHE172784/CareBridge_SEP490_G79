1_MF08-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/invitations/THAY_TOKEN/accept](http://localhost:8080/api/v1/care-groups/invitations/THAY_TOKEN/accept)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "familyRelationshipRole": "qa_familyrelationshiprole_01",
  "customFamilyRelationshipRole": "qa_customfamilyrelationshiprole_01"
}
```


2_MF08-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations/accept](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations/accept)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "familyRelationshipRole": "qa_familyrelationshiprole_01",
  "customFamilyRelationshipRole": "qa_customfamilyrelationshiprole_01"
}
```


3_MF08-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups](http://localhost:8080/api/v1/care-groups)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "groupName": "qa_groupname_01",
  "description": "qa_description_01",
  "linkedJourneyId": "THAY_JOURNEY_ID",
  "linkedBabyProfileId": "THAY_FILE_ID"
}
```


4_MF08-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/join](http://localhost:8080/api/v1/care-groups/join)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "code": "THAY_OTP",
  "familyRelationshipRole": "qa_familyrelationshiprole_01",
  "customFamilyRelationshipRole": "qa_customfamilyrelationshiprole_01"
}
```


5_MF08-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "channel": "LINK",
  "phone": "0901234567"
}
```


6_MF08-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations/decline](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations/decline)

Body: Không có body.


7_MF08-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations/THAY_TARGET_USER_ID/revoke](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/invitations/THAY_TARGET_USER_ID/revoke)

Body: Không có body.


8_MF08-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/join-requests/THAY_MEMBER_ID/respond](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/join-requests/THAY_MEMBER_ID/respond)

Body: Không có body.


9_MF08-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/leave](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/leave)

Body: Không có body.


10_MF08-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "assigneeMemberId": "qa_assigneememberid_01",
  "title": "qa_title_01",
  "description": "qa_description_01",
  "dueAt": "2026-10-07T09:00:00+07:00",
  "targetSubject": "MOTHER"
}
```


11_MF08-CREATE-009

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID/cancel](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID/cancel)

Body: Không có body.


12_MF08-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID)

Body: Không có body.


13_MF08-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members/THAY_TARGET_USER_ID](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members/THAY_TARGET_USER_ID)

Body: Không có body.


14_MF08-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups](http://localhost:8080/api/v1/care-groups)

Body: Không có body.


15_MF08-PRIVACY-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups](http://localhost:8080/api/v1/care-groups)

Body: Không có body.


16_MF08-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups](http://localhost:8080/api/v1/care-groups)

Body: Không có body.


17_MF08-SEARCH-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/invitations/me](http://localhost:8080/api/v1/care-groups/invitations/me)

Body: Không có body.


18_MF08-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/quick-notes](http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/quick-notes)

Body: Không có body.


19_MF08-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/join-requests](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/join-requests)

Body: Không có body.


20_MF08-SEARCH-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members)

Body: Không có body.


21_MF08-SEARCH-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks)

Body: Không có body.


22_MF08-SEARCH-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/family-alerts](http://localhost:8080/api/v1/family-alerts)

Body: Không có body.


23_MF08-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups](http://localhost:8080/api/v1/care-groups)

Body: Không có body.


24_MF08-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/journey](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/journey)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "journeyId": "THAY_JOURNEY_ID"
}
```


25_MF08-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members/THAY_MEMBER_ID/permissions](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members/THAY_MEMBER_ID/permissions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "calendar": true,
  "logs": true,
  "alerts": true,
  "records": true,
  "checklistView": true,
  "checklistComplete": true,
  "quickNotes": true,
  "quickNoteWeight": true,
  "quickNoteHydration": true,
  "quickNoteEpds": true,
  "quickNoteFetalMovement": true,
  "quickNoteBloodPressure": true,
  "quickNoteBloodGlucose": true
}
```


26_MF08-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "description": "qa_description_01",
  "dueAt": "2026-10-07T09:00:00+07:00",
  "assigneeMemberId": "qa_assigneememberid_01"
}
```


27_MF08-UPDATE-004

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID/status](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID/status)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "status": "qa_status_01"
}
```


28_MF08-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members/THAY_MEMBER_ID/permissions](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/members/THAY_MEMBER_ID/permissions)

Body: Không có body.


29_MF08-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/shared-data](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/shared-data)

Body: Không có body.


30_MF08-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID](http://localhost:8080/api/v1/care-groups/THAY_GROUP_ID/tasks/THAY_TASK_ID)

Body: Không có body.


31_MF08-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/family/dashboard](http://localhost:8080/api/v1/family/dashboard)

Body: Không có body.
