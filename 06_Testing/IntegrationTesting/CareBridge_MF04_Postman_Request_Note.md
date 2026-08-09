1_MF04-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/assessments/THAY_ASSESSMENT_ID/feedback](http://localhost:8080/api/v1/admin/moderation/assessments/THAY_ASSESSMENT_ID/feedback)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "verdict": "AGREE",
  "note": "qa_note_01"
}
```


2_MF04-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/resolve](http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/resolve)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "outcome": "DISMISS",
  "reason": "qa_reason_01",
  "expiresAt": "2026-10-07T09:00:00+07:00"
}
```


3_MF04-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/policies](http://localhost:8080/api/v1/admin/ai-moderation/policies)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "policyCode": "qa_policycode_01",
  "name": "qa_name_01",
  "detectionGuidance": "qa_detectionguidance_01",
  "violationCategory": "SPAM_ADVERTISING",
  "reportCategory": "INACCURATE_INFORMATION",
  "severity": "LOW",
  "applicableTargetTypes": [
    "QUESTION"
  ],
  "confidenceThreshold": 1,
  "active": true,
  "referenceLinks": [
    "qa_referencelink_01"
  ],
  "referenceFiles": [
    "qa_referencefile_01"
  ]
}
```


4_MF04-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/rescan](http://localhost:8080/api/v1/admin/ai-moderation/rescan)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "targetType": "QUESTION",
  "targetId": "qa_targetid_01"
}
```


5_MF04-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/test](http://localhost:8080/api/v1/admin/ai-moderation/test)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "targetType": "QUESTION",
  "sampleText": "qa_sampletext_01"
}
```


6_MF04-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/account-actions](http://localhost:8080/api/v1/admin/moderation/account-actions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "targetUserId": "THAY_USER_ID",
  "actionType": "APPROVE",
  "reason": "qa_reason_01",
  "expiresAt": "2026-10-07T09:00:00+07:00"
}
```


7_MF04-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/actions](http://localhost:8080/api/v1/admin/moderation/actions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "targetId": "qa_targetid_01",
  "targetType": "QUESTION",
  "actionType": "APPROVE",
  "reason": "qa_reason_01"
}
```


8_MF04-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/actions/THAY_ACTION_ID/undo](http://localhost:8080/api/v1/admin/moderation/actions/THAY_ACTION_ID/undo)

Body: Không có body.


9_MF04-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/claim](http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/claim)

Body: Không có body.


10_MF04-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/release](http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/release)

Body: Không có body.


11_MF04-CREATE-009

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/answers/THAY_ANSWER_ID/like](http://localhost:8080/api/v1/community/answers/THAY_ANSWER_ID/like)

Body: Không có body.


12_MF04-CREATE-010

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions](http://localhost:8080/api/v1/community/questions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "topicId": "qa_topicid_01",
  "title": "qa_title_01",
  "body": "qa_body_01",
  "stage": "PRE_PREGNANCY",
  "pregnancyWeek": 1,
  "babyAgeMonths": 1,
  "urgency": "LOW",
  "isAnonymous": true
}
```


13_MF04-CREATE-011

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/answers](http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/answers)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "body": "qa_body_01",
  "isPersonalExperience": true,
  "experienceTag": "qa_experiencetag_01"
}
```


14_MF04-CREATE-012

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/bookmark](http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/bookmark)

Body: Không có body.


15_MF04-CREATE-013

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/like](http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/like)

Body: Không có body.


16_MF04-CREATE-014

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/topics](http://localhost:8080/api/v1/community/topics)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "name": "qa_name_01",
  "description": "qa_description_01",
  "icon": "qa_icon_01",
  "type": "TOPIC",
  "parentId": "qa_parentid_01",
  "sortOrder": 1
}
```


17_MF04-CREATE-015

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/topics/THAY_ID/follow](http://localhost:8080/api/v1/community/topics/THAY_ID/follow)

Body: Không có body.


18_MF04-CREATE-016

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/reports](http://localhost:8080/api/v1/reports)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "targetType": "QUESTION",
  "targetId": "qa_targetid_01",
  "category": "INACCURATE_INFORMATION",
  "description": "qa_description_01"
}
```


19_MF04-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_ID](http://localhost:8080/api/v1/community/questions/THAY_ID)

Body: Không có body.


20_MF04-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/answers/THAY_ID](http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/answers/THAY_ID)

Body: Không có body.


21_MF04-DELETE-003

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/topics/THAY_ID](http://localhost:8080/api/v1/community/topics/THAY_ID)

Body: Không có body.


22_MF04-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/policies](http://localhost:8080/api/v1/admin/ai-moderation/policies)

Body: Không có body.


23_MF04-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/policies](http://localhost:8080/api/v1/admin/ai-moderation/policies)

Body: Không có body.


24_MF04-SEARCH-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/account-history](http://localhost:8080/api/v1/admin/moderation/account-history)

Body: Không có body.


25_MF04-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/escalations](http://localhost:8080/api/v1/admin/moderation/escalations)

Body: Không có body.


26_MF04-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/history](http://localhost:8080/api/v1/admin/moderation/history)

Body: Không có body.


27_MF04-SEARCH-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/pending-content](http://localhost:8080/api/v1/admin/moderation/pending-content)

Body: Không có body.


28_MF04-SEARCH-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/queue](http://localhost:8080/api/v1/admin/moderation/queue)

Body: Không có body.


29_MF04-SEARCH-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/feed](http://localhost:8080/api/v1/community/feed)

Body: Không có body.


30_MF04-SEARCH-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions](http://localhost:8080/api/v1/community/questions)

Body: Không có body.


31_MF04-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/policies](http://localhost:8080/api/v1/admin/ai-moderation/policies)

Body: Không có body.


32_MF04-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/policies/THAY_ID/status](http://localhost:8080/api/v1/admin/ai-moderation/policies/THAY_ID/status)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "active": true
}
```


33_MF04-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_ID](http://localhost:8080/api/v1/community/questions/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "topicId": "qa_topicid_01",
  "title": "qa_title_01",
  "body": "qa_body_01",
  "stage": "PRE_PREGNANCY",
  "pregnancyWeek": 1,
  "babyAgeMonths": 1,
  "isAnonymous": true,
  "urgency": "LOW"
}
```


34_MF04-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/answers/THAY_ID](http://localhost:8080/api/v1/community/questions/THAY_QUESTION_ID/answers/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "body": "qa_body_01",
  "isPersonalExperience": true,
  "experienceTag": "qa_experiencetag_01"
}
```


35_MF04-UPDATE-004

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/topics/THAY_ID](http://localhost:8080/api/v1/community/topics/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "name": "qa_name_01",
  "description": "qa_description_01",
  "icon": "qa_icon_01",
  "type": "TOPIC",
  "parentId": "qa_parentid_01",
  "isHidden": true,
  "sortOrder": 1
}
```


36_MF04-UPDATE-005

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/policies/THAY_ID](http://localhost:8080/api/v1/admin/ai-moderation/policies/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "name": "qa_name_01",
  "detectionGuidance": "qa_detectionguidance_01",
  "violationCategory": "SPAM_ADVERTISING",
  "reportCategory": "INACCURATE_INFORMATION",
  "severity": "LOW",
  "applicableTargetTypes": [
    "QUESTION"
  ],
  "confidenceThreshold": 1,
  "active": true,
  "referenceLinks": [
    "qa_referencelink_01"
  ],
  "referenceFiles": [
    "qa_referencefile_01"
  ]
}
```


37_MF04-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/ai-moderation/status](http://localhost:8080/api/v1/admin/ai-moderation/status)

Body: Không có body.


38_MF04-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/account-history/THAY_TARGET_USER_ID](http://localhost:8080/api/v1/admin/moderation/account-history/THAY_TARGET_USER_ID)

Body: Không có body.


39_MF04-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/community-content](http://localhost:8080/api/v1/admin/moderation/community-content)

Body: Không có body.


40_MF04-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/content/THAY_TARGET_TYPE/THAY_TARGET_ID](http://localhost:8080/api/v1/admin/moderation/content/THAY_TARGET_TYPE/THAY_TARGET_ID)

Body: Không có body.


41_MF04-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/assessment](http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/assessment)

Body: Không có body.


42_MF04-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/related](http://localhost:8080/api/v1/admin/moderation/reports/THAY_REPORT_ID/related)

Body: Không có body.


43_MF04-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/me/bookmarks](http://localhost:8080/api/v1/community/me/bookmarks)

Body: Không có body.


44_MF04-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/mine](http://localhost:8080/api/v1/community/questions/mine)

Body: Không có body.


45_MF04-VIEW-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/questions/THAY_ID](http://localhost:8080/api/v1/community/questions/THAY_ID)

Body: Không có body.


46_MF04-VIEW-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/community/topics](http://localhost:8080/api/v1/community/topics)

Body: Không có body.


47_MF04-VIEW-011

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/moderator/community/dashboard](http://localhost:8080/api/v1/moderator/community/dashboard)

Body: Không có body.
