1_MF09-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/approve](http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/approve)

Body: Không có body.


2_MF09-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates](http://localhost:8080/api/v1/admin/checklist-templates)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "name": "qa_name_01",
  "description": "qa_description_01",
  "templateType": "MANDATORY",
  "recipientRoles": [
    "MOTHER"
  ],
  "stage": "PRE_PREGNANCY",
  "substage": "qa_substage_01",
  "items": [
    "qa_item_01"
  ],
  "displayOrder": 1
}
```


3_MF09-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/archive](http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/archive)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reason": "qa_reason_01"
}
```


4_MF09-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/clone](http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/clone)

Body: Không có body.


5_MF09-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/decision](http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/decision)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "decision": "APPROVE",
  "reason": "qa_reason_01"
}
```


6_MF09-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/activate](http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/activate)

Body: Không có body.


7_MF09-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/clone](http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/clone)

Body: Không có body.


8_MF09-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/review](http://localhost:8080/api/v1/admin/checklist-templates/THAY_LINEAGE_ID/versions/THAY_VERSION_ID/review)

Body: Không có body.


9_MF09-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content](http://localhost:8080/api/v1/admin/content)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "type": "ARTICLE",
  "title": "qa_title_01",
  "body": "qa_body_01",
  "summary": "qa_summary_01",
  "stage": "PRE_PREGNANCY",
  "topicId": "qa_topicid_01",
  "tagIds": [
    "qa_tagid_01"
  ],
  "eligibleFromWeek": 1,
  "eligibleToWeek": 1,
  "recommendationPriority": 1
}
```


10_MF09-CREATE-009

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/THAY_ID/archive](http://localhost:8080/api/v1/admin/content/THAY_ID/archive)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reason": "qa_reason_01"
}
```


11_MF09-CREATE-010

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/THAY_ID/decision](http://localhost:8080/api/v1/admin/content/THAY_ID/decision)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "decision": "APPROVE",
  "reason": "qa_reason_01"
}
```


12_MF09-CREATE-011

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/THAY_ID/unpublish](http://localhost:8080/api/v1/admin/content/THAY_ID/unpublish)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reason": "qa_reason_01"
}
```


13_MF09-CREATE-012

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/checklists/tasks/THAY_TASK_ID/actions](http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/checklists/tasks/THAY_TASK_ID/actions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "action": "COMPLETE",
  "clientRequestId": "qa_clientrequestid_01",
  "reason": "qa_reason_01"
}
```


14_MF09-CREATE-013

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/checklists/sequences/advance](http://localhost:8080/api/v1/checklists/sequences/advance)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "currentInstanceId": "qa_currentinstanceid_01",
  "clientRequestId": "qa_clientrequestid_01"
}
```


15_MF09-CREATE-014

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/checklists/tasks/THAY_TASK_ID/actions](http://localhost:8080/api/v1/checklists/tasks/THAY_TASK_ID/actions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "action": "COMPLETE",
  "clientRequestId": "qa_clientrequestid_01",
  "reason": "qa_reason_01"
}
```


16_MF09-CREATE-015

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/tasks/THAY_TASK_KIND/THAY_TASK_ID/actions](http://localhost:8080/api/v1/tasks/THAY_TASK_KIND/THAY_TASK_ID/actions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "action": "COMPLETE",
  "clientRequestId": "qa_clientrequestid_01",
  "reason": "qa_reason_01"
}
```


17_MF09-CREATE-016

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items](http://localhost:8080/api/v1/user-checklist-items)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID",
  "itemText": "qa_itemtext_01",
  "category": "DELIVERY",
  "itemOrder": 1,
  "targetSubject": "MOTHER",
  "clientTaskId": "qa_clienttaskid_01",
  "careGroupId": "THAY_CARE_GROUP_ID"
}
```


18_MF09-CREATE-017

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items/from-template](http://localhost:8080/api/v1/user-checklist-items/from-template)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "templateId": "THAY_TEMPLATE_ID",
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID"
}
```


19_MF09-CREATE-018

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items/import](http://localhost:8080/api/v1/user-checklist-items/import)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "journeyId": "THAY_JOURNEY_ID",
  "babyId": "THAY_BABY_ID",
  "templateItemIds": [
    "qa_templateitemid_01"
  ]
}
```


20_MF09-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items/THAY_ITEM_ID](http://localhost:8080/api/v1/user-checklist-items/THAY_ITEM_ID)

Body: Không có body.


21_MF09-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates](http://localhost:8080/api/v1/admin/checklist-templates)

Body: Không có body.


22_MF09-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates](http://localhost:8080/api/v1/admin/checklist-templates)

Body: Không có body.


23_MF09-SEARCH-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/versions](http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID/versions)

Body: Không có body.


24_MF09-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/checklists](http://localhost:8080/api/v1/admin/content/checklists)

Body: Không có body.


25_MF09-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/THAY_ID/versions](http://localhost:8080/api/v1/admin/content/THAY_ID/versions)

Body: Không có body.


26_MF09-SEARCH-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/checklists/history](http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/checklists/history)

Body: Không có body.


27_MF09-SEARCH-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/checklists/history](http://localhost:8080/api/v1/checklists/history)

Body: Không có body.


28_MF09-SEARCH-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content/checklists](http://localhost:8080/api/v1/content/checklists)

Body: Không có body.


29_MF09-SEARCH-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content/lifecycle/checklists](http://localhost:8080/api/v1/content/lifecycle/checklists)

Body: Không có body.


30_MF09-SEARCH-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content/search](http://localhost:8080/api/v1/content/search)

Body: Không có body.


31_MF09-SEARCH-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items](http://localhost:8080/api/v1/user-checklist-items)

Body: Không có body.


32_MF09-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates](http://localhost:8080/api/v1/admin/checklist-templates)

Body: Không có body.


33_MF09-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items/THAY_ITEM_ID/toggle](http://localhost:8080/api/v1/user-checklist-items/THAY_ITEM_ID/toggle)

Body: Không có body.


34_MF09-UPDATE-002

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID](http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "name": "qa_name_01",
  "description": "qa_description_01",
  "templateType": "MANDATORY",
  "recipientRoles": [
    "MOTHER"
  ],
  "stage": "PRE_PREGNANCY",
  "substage": "qa_substage_01",
  "status": "DRAFT",
  "items": [
    "qa_item_01"
  ],
  "displayOrder": 1
}
```


35_MF09-UPDATE-003

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/THAY_ID](http://localhost:8080/api/v1/admin/content/THAY_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "title": "qa_title_01",
  "body": "qa_body_01",
  "summary": "qa_summary_01",
  "stage": "PRE_PREGNANCY",
  "topicId": "qa_topicid_01",
  "tagIds": [
    "qa_tagid_01"
  ],
  "eligibleFromWeek": 1,
  "eligibleToWeek": 1,
  "recommendationPriority": 1,
  "status": "DRAFT",
  "sourceLabel": "qa_sourcelabel_01",
  "sources": [
    "qa_source_01"
  ]
}
```


36_MF09-UPDATE-004

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/recommendations/profile](http://localhost:8080/api/v1/recommendations/profile)

Body: Không có body.


37_MF09-UPDATE-005

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/user-checklist-items/THAY_ITEM_ID](http://localhost:8080/api/v1/user-checklist-items/THAY_ITEM_ID)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "itemText": "qa_itemtext_01",
  "category": "DELIVERY",
  "itemOrder": 1
}
```


38_MF09-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID](http://localhost:8080/api/v1/admin/checklist-templates/THAY_ID)

Body: Không có body.


39_MF09-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content](http://localhost:8080/api/v1/admin/content)

Body: Không có body.


40_MF09-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/recommendation-tags](http://localhost:8080/api/v1/admin/content/recommendation-tags)

Body: Không có body.


41_MF09-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/content/THAY_ID](http://localhost:8080/api/v1/admin/content/THAY_ID)

Body: Không có body.


42_MF09-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/checklists/current/tasks](http://localhost:8080/api/v1/care-groups/THAY_CARE_GROUP_ID/checklists/current/tasks)

Body: Không có body.


43_MF09-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/checklists/current/tasks](http://localhost:8080/api/v1/checklists/current/tasks)

Body: Không có body.


44_MF09-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content](http://localhost:8080/api/v1/content)

Body: Không có body.


45_MF09-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content/lifecycle](http://localhost:8080/api/v1/content/lifecycle)

Body: Không có body.


46_MF09-VIEW-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content/lifecycle/THAY_ID](http://localhost:8080/api/v1/content/lifecycle/THAY_ID)

Body: Không có body.


47_MF09-VIEW-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/content/THAY_ID](http://localhost:8080/api/v1/content/THAY_ID)

Body: Không có body.


48_MF09-VIEW-011

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/operations/checklist-e2e/attestation](http://localhost:8080/api/v1/operations/checklist-e2e/attestation)

Body: Không có body.


49_MF09-VIEW-012

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/recommendations/content](http://localhost:8080/api/v1/recommendations/content)

Body: Không có body.


50_MF09-VIEW-013

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/recommendations/profile](http://localhost:8080/api/v1/recommendations/profile)

Body: Không có body.


51_MF09-VIEW-014

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/tasks/today](http://localhost:8080/api/v1/tasks/today)

Body: Không có body.
