1_MF10-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/emergency-alert](http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/emergency-alert)

Body: Không có body.


2_MF10-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/sensor-self-test/complete](http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/sensor-self-test/complete)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "outcome": "qa_outcome_01"
}
```


3_MF10-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/events/sensor-self-test](http://localhost:8080/api/v1/safety/events/sensor-self-test)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "testId": "qa_testid_01",
  "detectedAt": "2026-10-07T09:00:00+07:00",
  "accelerationMagnitude": 1,
  "gyroscopeMagnitude": 1
}
```


4_MF10-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/confirm](http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/confirm)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "note": "qa_note_01"
}
```


5_MF10-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/false-positive](http://localhost:8080/api/v1/safety/events/THAY_EVENT_ID/false-positive)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "note": "qa_note_01"
}
```


6_MF10-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/fall-detection/disable](http://localhost:8080/api/v1/safety/fall-detection/disable)

Body: Không có body.


7_MF10-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/fall-detection/enable](http://localhost:8080/api/v1/safety/fall-detection/enable)

Body: Không có body.


8_MF10-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/imu-data](http://localhost:8080/api/v1/safety/imu-data)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "accelerometerX": 1,
  "accelerometerY": 1,
  "accelerometerZ": 1,
  "gyroscopeX": 1,
  "gyroscopeY": 1,
  "gyroscopeZ": 1,
  "timestamp": "2026-10-07T09:00:00+07:00",
  "signalId": "qa_signalid_01",
  "latitude": 10.7769,
  "longitude": 106.7009
}
```


9_MF10-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/config](http://localhost:8080/api/v1/safety/config)

Body: Không có body.


10_MF10-PRIVACY-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/config](http://localhost:8080/api/v1/safety/config)

Body: Không có body.


11_MF10-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/events](http://localhost:8080/api/v1/safety/events)

Body: Không có body.


12_MF10-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/config](http://localhost:8080/api/v1/safety/config)

Body: Không có body.


13_MF10-UPDATE-001

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/config](http://localhost:8080/api/v1/safety/config)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "fallDetectionEnabled": true,
  "sensitivityLevel": "qa_sensitivitylevel_01",
  "emergencyAutoAlert": true,
  "countdownSeconds": 1,
  "sensorPermissionGranted": true
}
```


14_MF10-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/safety/config](http://localhost:8080/api/v1/safety/config)

Body: Không có body.
