1_MF07-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/facilities/THAY_FACILITY_ID/verify](http://localhost:8080/api/v1/admin/facilities/THAY_FACILITY_ID/verify)

Body: Không có body.


2_MF07-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/emergency/sessions](http://localhost:8080/api/v1/emergency/sessions)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "triggerSource": "qa_triggersource_01",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```


3_MF07-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/location/snapshots](http://localhost:8080/api/v1/location/snapshots)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "accuracyMeters": 1,
  "contextType": "qa_contexttype_01",
  "contextId": "qa_contextid_01",
  "expiresAt": "2026-10-07T09:00:00+07:00"
}
```


4_MF07-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/emergency/handoff](http://localhost:8080/api/v1/map/emergency/handoff)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "triageHandoffId": "qa_triagehandoffid_01",
  "riskLevel": "qa_risklevel_01",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009,
  "symptomSummary": "qa_symptomsummary_01",
  "selectedFacilityId": "THAY_FACILITY_ID"
}
```


5_MF07-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/route](http://localhost:8080/api/v1/map/route)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "fromLat": 1,
  "fromLng": 1,
  "toLat": 1,
  "toLng": 1,
  "transportMode": "qa_transportmode_01"
}
```


6_MF07-FORBIDDEN-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/facilities/THAY_FACILITY_ID/verify](http://localhost:8080/api/v1/admin/facilities/THAY_FACILITY_ID/verify)

Body: Không có body.


7_MF07-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/nearby-facilities](http://localhost:8080/api/v1/map/nearby-facilities)

Body: Không có body.


8_MF07-UNAUTHORIZED-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/facilities/THAY_FACILITY_ID/verify](http://localhost:8080/api/v1/admin/facilities/THAY_FACILITY_ID/verify)

Body: Không có body.


9_MF07-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/emergency/sessions/THAY_ID/resolve](http://localhost:8080/api/v1/emergency/sessions/THAY_ID/resolve)

Body: Không có body.


10_MF07-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/facilities/pending](http://localhost:8080/api/v1/admin/facilities/pending)

Body: Không có body.


11_MF07-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/emergency/sessions/active](http://localhost:8080/api/v1/emergency/sessions/active)

Body: Không có body.


12_MF07-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/emergency/sessions/THAY_ID/alert](http://localhost:8080/api/v1/emergency/sessions/THAY_ID/alert)

Body: Không có body.


13_MF07-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/location/snapshots/me](http://localhost:8080/api/v1/location/snapshots/me)

Body: Không có body.


14_MF07-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/emergency/me](http://localhost:8080/api/v1/map/emergency/me)

Body: Không có body.


15_MF07-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/emergency/THAY_HANDOFF_ID](http://localhost:8080/api/v1/map/emergency/THAY_HANDOFF_ID)

Body: Không có body.


16_MF07-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/facilities](http://localhost:8080/api/v1/map/facilities)

Body: Không có body.


17_MF07-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/map/facilities/THAY_ID](http://localhost:8080/api/v1/map/facilities/THAY_ID)

Body: Không có body.
