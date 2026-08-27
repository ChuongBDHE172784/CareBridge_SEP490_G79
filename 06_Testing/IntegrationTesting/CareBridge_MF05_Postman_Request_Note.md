1_MF05-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials](http://localhost:8080/api/v1/expert/credentials)

Body: Không có body.


2_MF05-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/identity](http://localhost:8080/api/v1/expert/identity)

Body: Không có body.


3_MF05-ACTION-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID/approve](http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID/approve)

Body: Không có body.


4_MF05-ACTION-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID/reject](http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID/reject)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reason": "qa_reason_01"
}
```


5_MF05-ACTION-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/verify-face](http://localhost:8080/api/v1/expert/verify-face)

Body: Không có body.


6_MF05-CREATE-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/availability](http://localhost:8080/api/v1/expert/availability)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "startAt": "2026-10-07T09:00:00+07:00",
  "endAt": "2026-10-07T09:00:00+07:00",
  "channelType": "qa_channeltype_01",
  "status": "qa_status_01"
}
```


7_MF05-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/location/share](http://localhost:8080/api/v1/expert/location/share)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "accuracyMeters": 1,
  "availabilityStatus": "qa_availabilitystatus_01",
  "expiresAt": "2026-10-07T09:00:00+07:00",
  "consentReference": "qa_consentreference_01"
}
```


8_MF05-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles](http://localhost:8080/api/v1/expert/profiles)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "specialtyId": "qa_specialtyid_01",
  "hospitalId": "qa_hospitalid_01",
  "trackAsiaName": "qa_trackasianame_01",
  "trackAsiaAddress": "qa_trackasiaaddress_01",
  "trackAsiaLat": 1,
  "trackAsiaLng": 1,
  "specialty": "qa_specialty_01",
  "professionalTitle": "qa_professionaltitle_01",
  "experienceYears": 1,
  "workplace": "qa_workplace_01",
  "consultationScope": "qa_consultationscope_01",
  "ratingAvg": 1,
  "consultationFeeVnd": 1
}
```


9_MF05-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/me/renew](http://localhost:8080/api/v1/expert/profiles/me/renew)

Body: Không có body.


10_MF05-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/availability/THAY_ID](http://localhost:8080/api/v1/expert/availability/THAY_ID)

Body: Không có body.


11_MF05-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID](http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID)

Body: Không có body.


12_MF05-DELETE-003

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/location/share](http://localhost:8080/api/v1/expert/location/share)

Body: Không có body.


13_MF05-FORBIDDEN-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/credential-issuers](http://localhost:8080/api/v1/credential-issuers)

Body: Không có body.


14_MF05-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/credential-issuers](http://localhost:8080/api/v1/credential-issuers)

Body: Không có body.


15_MF05-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/online-status](http://localhost:8080/api/v1/expert/online-status)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "online": true
}
```


16_MF05-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/me](http://localhost:8080/api/v1/expert/profiles/me)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "specialtyId": "qa_specialtyid_01",
  "hospitalId": "qa_hospitalid_01",
  "trackAsiaName": "qa_trackasianame_01",
  "trackAsiaAddress": "qa_trackasiaaddress_01",
  "trackAsiaLat": 1,
  "trackAsiaLng": 1,
  "specialty": "qa_specialty_01",
  "professionalTitle": "qa_professionaltitle_01",
  "experienceYears": 1,
  "workplace": "qa_workplace_01",
  "consultationScope": "qa_consultationscope_01",
  "ratingAvg": 1,
  "consultationFeeVnd": 1
}
```


17_MF05-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID/trust](http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID/trust)

Body: Không có body.


18_MF05-UPDATE-004

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID/review](http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID/review)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reviewStatus": "PENDING",
  "reviewNote": "qa_reviewnote_01"
}
```


19_MF05-UPDATE-005

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/identity/THAY_ATTEMPT_ID/review](http://localhost:8080/api/v1/expert/identity/THAY_ATTEMPT_ID/review)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "reviewStatus": "PENDING_REVIEW",
  "reason": "qa_reason_01"
}
```


20_MF05-VIEW-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/credential-issuers](http://localhost:8080/api/v1/credential-issuers)

Body: Không có body.


21_MF05-VIEW-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/admin/profiles](http://localhost:8080/api/v1/expert/admin/profiles)

Body: Không có body.


22_MF05-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/availability/me](http://localhost:8080/api/v1/expert/availability/me)

Body: Không có body.


23_MF05-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/contribution-points](http://localhost:8080/api/v1/expert/contribution-points)

Body: Không có body.


24_MF05-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/contribution-points/breakdown](http://localhost:8080/api/v1/expert/contribution-points/breakdown)

Body: Không có body.


25_MF05-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/contribution-points/total](http://localhost:8080/api/v1/expert/contribution-points/total)

Body: Không có body.


26_MF05-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/me](http://localhost:8080/api/v1/expert/credentials/me)

Body: Không có body.


27_MF05-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/pending](http://localhost:8080/api/v1/expert/credentials/pending)

Body: Không có body.


28_MF05-VIEW-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID](http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID)

Body: Không có body.


29_MF05-VIEW-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID/file](http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID/file)

Body: Không có body.


30_MF05-VIEW-011

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID/preview](http://localhost:8080/api/v1/expert/credentials/THAY_CREDENTIAL_ID/preview)

Body: Không có body.


31_MF05-VIEW-012

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/directory](http://localhost:8080/api/v1/expert/directory)

Body: Không có body.


32_MF05-VIEW-013

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/identity/files/THAY_FILE_ID/url](http://localhost:8080/api/v1/expert/identity/files/THAY_FILE_ID/url)

Body: Không có body.


33_MF05-VIEW-014

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/identity/pending](http://localhost:8080/api/v1/expert/identity/pending)

Body: Không có body.


34_MF05-VIEW-015

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/onboarding](http://localhost:8080/api/v1/expert/onboarding)

Body: Không có body.


35_MF05-VIEW-016

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/me](http://localhost:8080/api/v1/expert/profiles/me)

Body: Không có body.


36_MF05-VIEW-017

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/me/verification-status](http://localhost:8080/api/v1/expert/profiles/me/verification-status)

Body: Không có body.


37_MF05-VIEW-018

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID](http://localhost:8080/api/v1/expert/profiles/THAY_EXPERT_PROFILE_ID)

Body: Không có body.


38_MF05-VIEW-019

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/review-cases](http://localhost:8080/api/v1/expert/review-cases)

Body: Không có body.


39_MF05-VIEW-020

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/review-cases/THAY_EXPERT_PROFILE_ID](http://localhost:8080/api/v1/expert/review-cases/THAY_EXPERT_PROFILE_ID)

Body: Không có body.


40_MF05-VIEW-021

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/expert/verified](http://localhost:8080/api/v1/expert/verified)

Body: Không có body.
