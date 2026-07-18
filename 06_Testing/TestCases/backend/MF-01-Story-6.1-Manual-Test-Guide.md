# Hướng dẫn kiểm thử thủ công MF-01 / Story 6.1

## Vòng đời Người mẹ Chuẩn hoá và Lịch sử Chuyển trạng thái

| Trường | Giá trị |
| --- | --- |
| Story | Epic 6 — Story 6.1 |
| Phạm vi | Vòng đời Người mẹ chuẩn hoá, nguồn gốc dữ liệu, lịch sử chuyển trạng thái, phân quyền và đồng thời |
| Giao diện chính | REST API |
| Client đề xuất | Postman |
| URL API cơ sở | `http://localhost:8080` |
| Phân loại dữ liệu kiểm thử | Chỉ sử dụng dữ liệu giả lập (Synthetic only) |
| Phiên bản hướng dẫn | 1.1 |
| Ngày | 2026-07-18 |
| Trạng thái Story | `done`; review gate sạch, Journey suite `45/45 PASS` |

## 1. Bản ghi Đợt kiểm thử (Test Run Record)

Hoàn thành bảng này trước khi thực hiện kiểm thử.

| Trường | Nhập thông tin của Tester |
| --- | --- |
| Tester | `[nhập]` |
| Ngày kiểm thử | `[nhập]` |
| Commit / build | `[nhập]` |
| Môi trường | `[local / isolated test / staging]` |
| API URL | `[nhập]` |
| Cơ sở dữ liệu | `[chỉ nhập alias cơ sở dữ liệu — không nhập credential]` |
| Kết quả tổng thể | `[PASS / FAIL / BLOCKED]` |
| Liên kết lỗi (Defect links) | `[nhập]` |

Không dán mật khẩu, access token, refresh token, credential cơ sở dữ liệu hoặc dữ liệu sức khỏe thực tế vào file này, ảnh chụp màn hình hoặc báo cáo lỗi.

## 2. Phạm vi Xác minh của Hướng dẫn này

Hướng dẫn này thực hiện xác minh thủ công các nội dung sau:

- Một vòng đời sản khoa chuẩn hoá đang hoạt động (active canonical maternal lifecycle) cho mỗi Người mẹ;
- Các giai đoạn vòng đời được hỗ trợ: `PRE_PREGNANCY`, `PREGNANCY`, và `POSTPARTUM`;
- Yêu cầu nguồn gốc ngày tháng (date provenance);
- Lịch sử chuyển trạng thái chỉ được ghi thêm (append-only transition history);
- Các chuyển đổi giai đoạn hợp lệ và không hợp lệ;
- Quyền sở hữu và kiểm soát vai trò (ownership and role enforcement);
- Phản hồi lịch sử tối thiểu-cần thiết (minimum-necessary history responses);
- Hành vi vòng đời kết thúc (terminal lifecycle behavior);
- Migration cơ sở dữ liệu và kiểm soát tính duy nhất (uniqueness controls).

API history hỗ trợ phân trang bằng `page`/`size`, trả metadata `totalElements`/`totalPages` và sắp xếp mới nhất trước. Client phải tiếp tục tải cho đến trang cuối; không được giả định history chỉ có tối đa 20 bản ghi.

## 3. Các Giới hạn Quan trọng

Các bài kiểm thử sau đây chạy tin cậy hơn ở dạng tự động hóa (automation) so với việc sử dụng API client bên ngoài:

- Rollback sau khi cố ý chèn lỗi bảng chuyển trạng thái (injected transition-table failure);
- Tranh chấp khóa lạc quan chính xác (optimistic-lock race) sau khi hai transaction cùng đọc một phiên bản entity;
- Hủy bỏ migration khi đối mặt với các dòng dữ liệu cũ bị trùng lặp cố ý.

Ghi lại các trường hợp này là `AUTOMATED EVIDENCE` trừ khi bạn có một cơ sở dữ liệu thử nghiệm cách ly lỗi đã được phê duyệt. Phạm vi tự động hóa chuẩn xác của chúng nằm ở:

- `JourneyCanonicalLifecycleIntegrationTest`
- `JourneyCanonicalLifecycleServiceTest`
- `JourneyCanonicalLifecycleControllerTest`

Không cố ý chèn lỗi cơ sở dữ liệu hoặc tạo các bản ghi giống dữ liệu thực vào cơ sở dữ liệu dùng chung hoặc môi trường staging.

## 4. Điều kiện Tiên quyết

1. Đã cài đặt Java 21 và backend CareBridge đang sẵn sàng.
2. Cơ sở dữ liệu kiểm thử PostgreSQL đang chạy.
3. Hai migration Story 6.1 đã được thực thi:

   - `V20260718090000__canonical_mother_lifecycle_history.sql`
   - `V20260718091000__enforce_mother_journey_transition_immutability.sql`
4. API có thể kết nối tại `{{baseUrl}}`.
5. Chuẩn bị ba tài khoản giả lập (synthetic):

   - `MOTHER_A`: Người mẹ chưa có vòng đời chuẩn hoá nào đang hoạt động.
   - `MOTHER_B`: Một Người mẹ khác chưa có vòng đời chuẩn hoá nào đang hoạt động.
   - `EXPERT_A`: Tài khoản Chuyên gia (Expert).
6. Sử dụng cơ sở dữ liệu dùng một lần (disposable database) hoặc các tài khoản được tạo riêng cho đợt kiểm thử này. Các tài khoản dev-seed `mother3@carebridge.dev` và `mother4@carebridge.dev` đã có luồng hành trình (journeys) nên không phù hợp cho trường hợp tạo mới đầu tiên (happy path).
7. Nếu sử dụng thông tin đăng nhập dev-seed, chỉ bật chúng trong môi trường local/test. Không bao giờ bật dev seeding trên môi trường production.

### 4.1 Cảnh báo về Migration

File `application.yaml` mặc định của repository hiện tại đang tắt Flyway. Việc chỉ khởi chạy Spring Boot không chứng minh rằng migration của Story 6.1 đã được chạy.

Đối với cơ sở dữ liệu local dùng một lần, hãy bật Flyway rõ ràng trong môi trường test trước khi khởi động:

```powershell
$env:SPRING_FLYWAY_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Đối với các môi trường dùng chung, hãy tuân theo quy trình triển khai đã được phê duyệt thay vì tự thay đổi cấu hình migration khi chạy.

### 4.2 Xác minh Migration (Chỉ đọc)

Chạy các truy vấn chỉ đọc này trước khi kiểm thử API:

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('20260718090000', '20260718091000')
ORDER BY version;

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'mother_journeys'
  AND column_name IN ('version', 'date_source', 'date_confidence')
ORDER BY column_name;

SELECT indexname
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname = 'uq_mother_journeys_one_canonical_active';

SELECT to_regclass('public.mother_journey_transitions') AS transition_table;

SELECT trigger_name
FROM information_schema.triggers
WHERE event_object_schema = 'public'
  AND event_object_table = 'mother_journey_transitions'
  AND trigger_name = 'trg_mother_journey_transitions_append_only';
```

Kết quả kỳ vọng:

- Flyway báo cáo cả hai migration thành công.
- Cả 3 cột mới đều tồn tại.
- Partial unique index tồn tại.
- `transition_table` là `public.mother_journey_transitions`.
- Trigger `trg_mother_journey_transitions_append_only` tồn tại.

Nếu thiếu bất kỳ mục nào, dừng kiểm thử và đánh dấu đợt chạy là `BLOCKED`.

## 5. Môi trường Postman (Postman Environment)

Tạo các biến sau:

| Biến | Giá trị ban đầu |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |
| `motherAEmail` | email giả lập của Mother A |
| `motherAPassword` | chỉ lưu trong môi trường Postman local |
| `motherAToken` | để trống |
| `motherAUserId` | để trống |
| `motherBEmail` | email giả lập của Mother B |
| `motherBPassword` | chỉ lưu trong môi trường Postman local |
| `motherBToken` | để trống |
| `motherBUserId` | để trống |
| `expertEmail` | email giả lập của Expert |
| `expertPassword` | chỉ lưu trong môi trường Postman local |
| `expertToken` | để trống |
| `journeyAId` | để trống |
| `journeyAVersion` | để trống |

Không export môi trường có chứa mật khẩu hoặc token.

### 5.1 Lấy Token trong môi trường Local/Test

Request:

```http
POST {{baseUrl}}/api/v1/auth/login-direct
Content-Type: application/json
```

Body cho Mother A:

```json
{
  "email": "{{motherAEmail}}",
  "password": "{{motherAPassword}}"
}
```

Sử dụng các request tương tự cho Mother B và Expert A.

Đối với request của Mother A, script Postman sau đây sẽ lưu token và user ID:

```javascript
const body = pm.response.json();
pm.environment.set("motherAToken", body.data.accessToken);
pm.environment.set("motherAUserId", body.data.user.id);
```

Sử dụng tên biến tương ứng cho Mother B và Expert A.

`login-direct` là endpoint phục vụ dev/test. Hãy sử dụng luồng xác thực chuẩn được phê duyệt bên ngoài môi trường local/test.

## 6. Header Request Chung

Request đã xác thực của Mother A:

```http
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

Cấu trúc response thành công chuẩn (standard success envelope):

```json
{
  "success": true,
  "data": {},
  "message": "optional message",
  "timestamp": "2026-07-18T00:00:00Z"
}
```

Cấu trúc response lỗi nghiệp vụ chuẩn (standard business-error envelope):

```json
{
  "success": false,
  "status": 409,
  "error": "JOURNEY-015",
  "message": "An active mother lifecycle already exists",
  "path": "/api/v1/journeys",
  "timestamp": "2026-07-18T00:00:00Z"
}
```

## 7. Tóm tắt Kết quả (Result Summary)

| ID | Kịch bản | Độ ưu tiên | Kết quả | Bằng chứng / Mã lỗi |
| --- | --- | --- | --- | --- |
| MF01-6.1-MAN-001 | Sẵn sàng cho Migration | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-002 | Request chưa xác thực bị từ chối | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-003 | Thay đổi ngày không có nguồn gốc bị từ chối | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-004 | Vòng đời chuẩn hoá đầu tiên và lịch sử CREATED | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-005 | Vòng đời đang hoạt động bị trùng lặp bị từ chối | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-006 | Cập nhật giai đoạn/ngày hợp lệ được ghi vào lịch sử | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-007 | Chuyển đổi không hợp lệ không gây ra tác dụng phụ | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-008 | Quyền sở hữu chéo tài khoản bị từ chối | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-009 | Vai trò Chuyên gia (Expert) bị từ chối | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-010 | Payload lịch sử đạt mức tối thiểu-cần thiết | P1 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-011 | Quy tắc hoàn thành và kết thúc vòng đời | P1 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-012 | Dashboard sử dụng vòng đời chuẩn hoá | P1 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-013 | Tạo đồng thời tạo ra duy nhất 1 kết quả thành công | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-014 | History phân trang đầy đủ và giữ provenance | P1 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-015 | No-op, notes-only và status không hỗ trợ | P1 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-016 | Giới hạn thời gian hiệu lực do client cung cấp | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-017 | User chưa gán role được onboarding thành MOTHER | P0 | `[ ]` | `[nhập]` |
| MF01-6.1-MAN-018 | Legacy BABY_CARE vẫn đọc được trên dashboard | P1 | `[ ]` | `[nhập]` |
| MF01-6.1-AUTO-001 | Rollback nguyên tử khi ghi lịch sử thất bại | P0 | `AUTOMATED EVIDENCE` | `[nhập bằng chứng build]` |
| MF01-6.1-AUTO-002 | Xung đột lạc quan ngăn chặn ghi đè mất dữ liệu | P0 | `AUTOMATED EVIDENCE` | `[nhập bằng chứng build]` |
| MF01-6.1-AUTO-003 | UPDATE/DELETE transition bị chặn ở repository và PostgreSQL | P0 | `AUTOMATED EVIDENCE` | `[nhập bằng chứng build]` |
| MF01-6.1-AUTO-004 | Audit JOURNEY_CREATED/UPDATED cùng transaction nghiệp vụ | P0 | `AUTOMATED EVIDENCE` | `[nhập bằng chứng build]` |

Sử dụng:

- `PASS` khi tất cả kết quả kỳ vọng được ghi nhận.
- `FAIL` khi hành vi khác biệt hoặc dữ liệu thay đổi ngoài dự kiến.
- `BLOCKED` khi môi trường hoặc dữ liệu kiểm thử ngăn cản việc thực thi.
- `AUTOMATED EVIDENCE` chỉ dành cho các trường hợp không thể xác định/chèn lỗi cố ý đã được chỉ định rõ.

## 8. Chi tiết Kịch bản Kiểm thử (Detailed Test Cases)

### MF01-6.1-MAN-001 — Sẵn sàng cho Migration

**Điều kiện tiên quyết:** Quyền truy cập cơ sở dữ liệu chỉ đọc (read-only).

**Các bước thực hiện:**

1. Chạy tất cả các truy vấn ở Mục 4.2.
2. Xác nhận không có Người mẹ nào hiện tại có nhiều hơn một vòng đời chuẩn hoá đang hoạt động:

   ```sql
   SELECT owner_user_id, count(*) AS active_canonical_count
   FROM public.mother_journeys
   WHERE status = 'ACTIVE'
     AND journey_type IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM')
   GROUP BY owner_user_id
   HAVING count(*) > 1;
   ```

**Kết quả kỳ vọng:**

- Các đối tượng migration tồn tại.
- Truy vấn kiểm tra trùng lặp trả về 0 dòng.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-002 — Request chưa xác thực bị từ chối

**Các bước thực hiện:**

1. Xóa header `Authorization`.
2. Gửi request:

   ```http
   POST {{baseUrl}}/api/v1/journeys
   Content-Type: application/json
   ```

   ```json
   {
     "journeyType": "PRE_PREGNANCY",
     "startDate": "2026-07-18"
   }
   ```
3. Lặp lại với:

   ```http
   GET {{baseUrl}}/api/v1/journeys/00000000-0000-0000-0000-000000000001/history
   ```

**Kết quả kỳ vọng:**

- Cả hai request đều trả về HTTP `401`.
- Không có dòng journey hay history nào được tạo.
- Response 401 rỗng là chấp nhận được vì quá trình xác thực đã từ chối trước khi thực thi controller.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-003 — Thay đổi ngày không có nguồn gốc bị từ chối

**Điều kiện tiên quyết:** Mother A không có vòng đời chuẩn hoá nào đang hoạt động.

**Request:**

```http
POST {{baseUrl}}/api/v1/journeys
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "journeyType": "PRE_PREGNANCY",
  "startDate": "2026-07-18",
  "lastMenstrualDate": "2026-06-01",
  "changeReason": "MF01 manual test — missing provenance"
}
```

**Kết quả kỳ vọng:**

- HTTP `400`.
- `error` là `JOURNEY-018`.
- Không có journey hoặc transition hiện tại nào được tạo cho Mother A.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-004 — Vòng đời chuẩn hoá đầu tiên và lịch sử CREATED

**Điều kiện tiên quyết:** Mother A vẫn chưa có vòng đời chuẩn hoá nào đang hoạt động.

**Request:**

```http
POST {{baseUrl}}/api/v1/journeys
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "journeyType": "PRE_PREGNANCY",
  "startDate": "2026-07-18",
  "changeReason": "MF01 manual test — initial lifecycle",
  "effectiveAt": "2026-07-18T02:00:00Z",
  "notes": "Synthetic manual test record"
}
```

**Kết quả tạo mới kỳ vọng:**

- HTTP `201`.
- `success` là `true`.
- `data.journeyType` là `PRE_PREGNANCY`.
- `data.status` là `ACTIVE`.
- `data.version` là `0`.
- `data.id` là một UUID.

Lưu các biến:

```javascript
const body = pm.response.json();
pm.environment.set("journeyAId", body.data.id);
pm.environment.set("journeyAVersion", body.data.version);
```

Sau đó gửi request:

```http
GET {{baseUrl}}/api/v1/journeys/{{journeyAId}}/history
Authorization: Bearer {{motherAToken}}
```

**Kết quả lịch sử kỳ vọng:**

- HTTP `200`.
- `data` chứa chính xác một bản ghi cho journey mới tạo này.
- Bản ghi có `eventType: CREATED`.
- `toStage` là `PRE_PREGNANCY`.
- `changedFields` chứa `journeyType`, `startDate`, và `status`.
- `source` là `UNKNOWN`.
- `reason` khớp với lý do đã gửi.
- `journeyVersion` là `0`.
- Phản hồi lịch sử không làm lộ `actorUserId`, dữ liệu thô `changesJson`, ghi chú (notes), dữ liệu liên hệ, hoặc token.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-005 — Vòng đời đang hoạt động bị trùng lặp bị từ chối

**Request:**

```http
POST {{baseUrl}}/api/v1/journeys
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "journeyType": "POSTPARTUM",
  "startDate": "2026-07-18",
  "changeReason": "MF01 manual test — duplicate attempt"
}
```

**Kết quả kỳ vọng:**

- HTTP `409`.
- `error` là `JOURNEY-015`.
- Mother A vẫn chỉ có duy nhất một vòng đời chuẩn hoá đang hoạt động.
- Lịch sử của `{{journeyAId}}` vẫn chứa đúng một bản ghi.

Xác minh chỉ đọc (tùy chọn):

```sql
SELECT count(*)
FROM public.mother_journeys
WHERE owner_user_id = '<MOTHER_A_USER_ID>'
  AND status = 'ACTIVE'
  AND journey_type IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM');
```

Số lượng kỳ vọng: `1`.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-006 — Cập nhật giai đoạn/ngày hợp lệ được ghi vào lịch sử

Chuyển đổi giai đoạn duy nhất hiện được cho phép là từ `PRE_PREGNANCY` sang `PREGNANCY`.

**Request:**

```http
PUT {{baseUrl}}/api/v1/journeys/{{journeyAId}}
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "journeyType": "PREGNANCY",
  "lastMenstrualDate": "2026-06-01",
  "dateSource": "CLINICIAN_CONFIRMED",
  "dateConfidence": "CONFIRMED",
  "changeReason": "MF01 manual test — pregnancy confirmed",
  "effectiveAt": "2026-07-18T03:00:00Z"
}
```

**Kết quả cập nhật kỳ vọng:**

- HTTP `200`.
- `data.journeyType` là `PREGNANCY`.
- `data.lastMenstrualDate` là `2026-06-01`.
- `data.estimatedDueDate` là `2027-03-08` (LMP cộng thêm 280 ngày).
- `data.dateSource` là `CLINICIAN_CONFIRMED`.
- `data.dateConfidence` là `CONFIRMED`.
- `data.version` là `1`.

Gửi request xem lịch sử một lần nữa.

**Lịch sử kỳ vọng:**

- Trả về 2 bản ghi, mới nhất xếp trước.
- Bản ghi mới nhất có `eventType: STAGE_CHANGED`.
- `fromStage` là `PRE_PREGNANCY`.
- `toStage` là `PREGNANCY`.
- `changedFields` bao gồm `journeyType`, `lastMenstrualDate`, và `estimatedDueDate`.
- `source` và `confidence` khớp với request.
- `journeyVersion` là `1`.
- Bản ghi `CREATED` trước đó vẫn giữ nguyên.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-007 — Chuyển đổi không hợp lệ không gây ra tác dụng phụ

**Request:**

```http
PUT {{baseUrl}}/api/v1/journeys/{{journeyAId}}
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "journeyType": "PRE_PREGNANCY",
  "changeReason": "MF01 manual test — invalid reverse transition"
}
```

**Kết quả kỳ vọng:**

- HTTP `409`.
- `error` là `JOURNEY-016`.
- Giai đoạn hiện tại vẫn là `PREGNANCY`.
- Version vẫn là `1`.
- Lịch sử vẫn giữ nguyên ở mức 2 bản ghi.

Đồng thời xác minh việc tạo chuẩn hoá từ chối loại chỉ thuộc về legacy. Trước khi Mother B có vòng đời, gửi request tạo dưới danh nghĩa Mother B với:

```json
{
  "journeyType": "BABY_CARE",
  "startDate": "2026-07-18"
}
```

Kỳ vọng: HTTP `409`, `JOURNEY-016`, và không có journey nào được tạo cho Mother B.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-008 — Quyền sở hữu chéo tài khoản bị từ chối

Sử dụng token của Mother B với journey ID của Mother A.

**Các Request:**

```http
GET {{baseUrl}}/api/v1/journeys/{{journeyAId}}/history
Authorization: Bearer {{motherBToken}}
```

```http
PUT {{baseUrl}}/api/v1/journeys/{{journeyAId}}
Authorization: Bearer {{motherBToken}}
Content-Type: application/json
```

```json
{
  "notes": "Unauthorized update attempt"
}
```

**Kết quả kỳ vọng:**

- Cả hai request đều trả về HTTP `403`.
- Lỗi nghiệp vụ là `JOURNEY-011`.
- Response không chứa dữ liệu journey hoặc history của Mother A.
- Dòng dữ liệu hiện tại, phiên bản và lịch sử của Mother A giữ nguyên không đổi.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-009 — Vai trò Chuyên gia (Expert) bị từ chối

Sử dụng `{{expertToken}}`.

1. Cố gắng tạo một journey.
2. Cố gắng cập nhật `{{journeyAId}}`.
3. Cố gắng đọc `{{journeyAId}}/history`.

**Kết quả kỳ vọng:**

- Tất cả request đều trả về HTTP `403`.
- Việc tạo có thể trả về `JOURNEY-003` vì service kiểm tra vai trò Mother.
- Việc cập nhật và đọc lịch sử có thể trả về lỗi `ACCESS_DENIED` từ tầng bảo mật (security layer).
- Không có dữ liệu hiện tại hoặc dữ liệu lịch sử nào bị thay đổi.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-010 — Payload lịch sử đạt mức tối thiểu-cần thiết

Yêu cầu lấy lịch sử của Mother A và kiểm tra từng đối tượng.

**Các trường được phép:**

- `transitionId`
- `eventType`
- `fromStage`
- `toStage`
- `changedFields`
- `source`
- `confidence`
- `reason`
- `effectiveAt`
- `recordedAt`
- `journeyVersion`

**Kết quả kỳ vọng:**

- Không tồn tại bất kỳ key nào khác.
- Không trả về `actorUserId`, `ownerUserId`, email, số điện thoại, JWT, ghi chú (notes), dữ liệu sức khỏe thô, hoặc `changesJson` thô.
- Response không chứa giá trị nào từ ghi chú `Synthetic manual test record`.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-011 — Quy tắc hoàn thành và kết thúc vòng đời

Đầu tiên thử hoàn thành mà không cung cấp ngày sinh (delivery date):

```http
PUT {{baseUrl}}/api/v1/journeys/{{journeyAId}}
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "status": "COMPLETED",
  "changeReason": "MF01 manual test — invalid completion"
}
```

Kỳ vọng: HTTP `400`, `JOURNEY-013`, không thay đổi phiên bản hay lịch sử.

Sau đó hoàn thành với thông tin nguồn gốc dữ liệu:

```json
{
  "status": "COMPLETED",
  "deliveryDate": "2027-03-08",
  "dateSource": "CLINICIAN_CONFIRMED",
  "dateConfidence": "CONFIRMED",
  "changeReason": "MF01 manual test — lifecycle completed",
  "effectiveAt": "2027-03-08T02:00:00Z"
}
```

Kỳ vọng:

- HTTP `200`.
- Status là `COMPLETED`.
- Version tăng lên.
- Bản ghi lịch sử mới nhất có `eventType: STATUS_CHANGED`.
- `changedFields` bao gồm `deliveryDate` và `status`.

Cuối cùng, lặp lại bất kỳ cập nhật nào.

Kỳ vọng:

- HTTP `400`.
- `error` là `JOURNEY-012`.
- Không có thêm bất kỳ thay đổi nào đối với dữ liệu hiện tại hoặc lịch sử.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-012 — Dashboard sử dụng vòng đời chuẩn hoá

Thực hiện kịch bản này trước kịch bản MF01-6.1-MAN-011, hoặc sử dụng một Mother khác có vòng đời chuẩn hoá đang hoạt động.

Request:

```http
GET {{baseUrl}}/api/v1/journeys/me/dashboard
Authorization: Bearer {{motherAToken}}
```

**Kết quả kỳ vọng:**

- HTTP `200`.
- Giai đoạn trên Dashboard phản ánh đúng vòng đời chuẩn hoá đang hoạt động.
- Dòng dữ liệu `BABY_CARE` thuộc legacy không được chọn làm vòng đời người mẹ.

Nếu không có dữ liệu mẫu (fixture) legacy nào được duyệt, ghi lại phần loại trừ `BABY_CARE` là `BLOCKED — fixture unavailable`; không tự chèn dòng dữ liệu cũ vào cơ sở dữ liệu dùng chung.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

### MF01-6.1-MAN-013 — Tạo đồng thời tạo ra duy nhất 1 kết quả thành công

**Môi trường:** Chỉ dành cho cơ sở dữ liệu kiểm thử local/dùng một lần (disposable).

**Điều kiện tiên quyết:** Sử dụng một tài khoản Mother mới hoàn toàn chưa có vòng đời chuẩn hoá nào.

Chuẩn bị hai POST request hợp lệ với cùng một token:

- Request A: `PRE_PREGNANCY`
- Request B: `POSTPARTUM`

Gửi hai request này đồng thời bằng cách sử dụng 2 tiến trình Postman Runner hoặc 2 cửa sổ terminal.

Ví dụ lệnh Bash:

```bash
curl -s -o create-a.json -w "%{http_code}" \
  -H "Authorization: Bearer ${FRESH_MOTHER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"journeyType":"PRE_PREGNANCY","startDate":"2026-07-18"}' \
  "${BASE_URL}/api/v1/journeys" &

curl -s -o create-b.json -w "%{http_code}" \
  -H "Authorization: Bearer ${FRESH_MOTHER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"journeyType":"POSTPARTUM","startDate":"2026-07-18"}' \
  "${BASE_URL}/api/v1/journeys" &

wait
```

**Kết quả kỳ vọng:**

- Chính xác một request trả về `201`.
- Chính xác một request trả về `409` với `JOURNEY-015`.
- Cơ sở dữ liệu chứa duy nhất 1 dòng chuẩn hoá đang hoạt động cho Người mẹ.
- Journey thành công chỉ có 1 chuyển đổi `CREATED`.

Nếu các request vô tình bị gửi tuần tự, cặp status code tương tự vẫn chấp nhận được cho việc bảo vệ trùng lặp, nhưng hãy ghi rõ rằng cuộc đua đồng thời thực sự (true race) chưa được chứng minh.

**Kết quả thực tế / Bằng chứng:** `[nhập]`

## 9. Kiểm tra Tính Nhất quán Cơ sở Dữ liệu (Chỉ đọc)

Thay thế các giữ chỗ (placeholder) bằng ID kiểm thử. Không bao giờ đưa credential vào lịch sử SQL hoặc bằng chứng.

```sql
SELECT
    journey_id,
    owner_user_id,
    journey_type,
    status,
    version,
    date_source,
    date_confidence
FROM public.mother_journeys
WHERE owner_user_id = '<MOTHER_A_USER_ID>';
```

```sql
SELECT
    transition_id,
    journey_id,
    event_type,
    from_stage,
    to_stage,
    source,
    confidence,
    reason,
    journey_version,
    effective_at,
    recorded_at
FROM public.mother_journey_transitions
WHERE journey_id = '<JOURNEY_A_ID>'
ORDER BY recorded_at ASC;
```

Bất biến kỳ vọng (Expected invariants):

- không quá một dòng chuẩn hoá hiện tại đang hoạt động cho mỗi owner;
- một chuyển đổi trên mỗi cặp `(journey_id, journey_version)`;
- phiên bản chuyển đổi tăng theo phiên bản dòng dữ liệu hiện tại;
- request bị từ chối không thêm chuyển đổi nào;
- các chuyển đổi đã tồn tại không bao giờ bị cập nhật hoặc xóa.

### 9.1 Regression bổ sung sau Code Review

Các mục này bổ sung cho 13 ca chi tiết ở Mục 8 và phải được đưa vào lần chạy regression mới.

#### MF01-6.1-MAN-014 — History phân trang đầy đủ và giữ provenance

1. Dùng fixture synthetic có số transition lớn hơn `size` (ví dụ 21 event với `size=10`).
2. Gọi `GET /api/v1/journeys/{{journeyAId}}/history?page=0&size=10`, sau đó gọi lần lượt đến `totalPages - 1`.
3. Ghép các page theo thứ tự trả về và kiểm tra không trùng/mất event.

Kỳ vọng: `totalElements` và `totalPages` đúng; mọi page mới nhất trước; thay đổi ngày/provenance dùng shape `{previous,new}` và chỉ chứa field allow-list.

#### MF01-6.1-MAN-015 — No-op, notes-only và status không hỗ trợ

1. Gửi PUT không làm thay đổi field nghiệp vụ; kỳ vọng HTTP `400`, `JOURNEY-020`, không tăng version/history.
2. Gửi PUT chỉ thay đổi `notes`; kỳ vọng thành công, tăng version một lần và thêm `DETAILS_CHANGED` mà không lộ notes trong history response tối thiểu.
3. Gửi status ngoài allow-list; kỳ vọng HTTP `400`, `JOURNEY-021`, không có side effect.

#### MF01-6.1-MAN-016 — Giới hạn `effectiveAt`

1. Gửi một update có `effectiveAt` backdated hợp lệ; kỳ vọng thành công và `recordedAt` vẫn là thời điểm server ghi nhận.
2. Gửi `effectiveAt` trong phạm vi server time cộng tối đa 5 phút; kỳ vọng được chấp nhận.
3. Gửi `effectiveAt` lớn hơn server time cộng 5 phút; kỳ vọng HTTP `400`, `JOURNEY-019`, không tăng version/history.

Ghi lại server time dùng để đối chiếu nhằm tránh false failure do lệch đồng hồ client.

#### MF01-6.1-MAN-017 — Onboarding user chưa gán role

1. Dùng user synthetic có role `null`/chưa gán và chưa có journey.
2. Tạo canonical lifecycle đầu tiên bằng request hợp lệ.
3. Đọc lại user và journey.

Kỳ vọng: request thành công; user được gán `MOTHER` cùng transaction; tài khoản có role khác như `EXPERT` vẫn bị từ chối và không có journey/audit dang dở.

#### MF01-6.1-MAN-018 — Legacy BABY_CARE vẫn đọc được

Chỉ dùng fixture legacy được phê duyệt; không tạo BABY_CARE mới qua canonical API. Gọi dashboard của owner có một `BABY_CARE ACTIVE` legacy và không có maternal canonical active.

Kỳ vọng: dashboard vẫn đọc được legacy row; create canonical `BABY_CARE` mới vẫn bị từ chối; dữ liệu owner khác không xuất hiện.

### 9.2 Bằng chứng tự động hóa bắt buộc

Không chạy UPDATE/DELETE trực tiếp trên transition table ở shared/staging. Liên kết kết quả `JourneyCanonicalLifecycleIntegrationTest` xác nhận:

- PostgreSQL trigger chặn cả UPDATE và DELETE trên `mother_journey_transitions`;
- repository/entity không cung cấp đường mutation hợp lệ;
- audit `JOURNEY_CREATED`/`JOURNEY_UPDATED` được persist nguyên tử cùng thay đổi journey;
- rollback và optimistic-lock race không để current/history/audit lệch nhau.

## 10. Mẫu Báo cáo Lỗi (Defect Reporting Template)

```text
Tiêu đề: [Story 6.1][Manual][Case ID] Mô tả ngắn gọn sự cố ghi nhận được

Môi trường:
Build / commit:
Mã kịch bản (Case ID):
Vai trò tài khoản (không ghi email/sđt):
Journey ID (chỉ trong môi trường test):

Điều kiện tiên quyết:
Các bước thực hiện:
Kỳ vọng:
Thực tế:
HTTP status:
Mã lỗi (Error code):

Ảnh hưởng tính nhất quán CSDL:
Khả năng tái lập:
Bằng chứng:

Lưu ý bảo mật:
- Đã ẩn token/credential: Có/Không
- Có dữ liệu cá nhân hoặc dữ liệu sức khỏe thực tế: Có/Không
```

Bất kỳ hành vi vượt quyền sở hữu (ownership bypass), tồn tại nhiều vòng đời chuẩn hoá đang hoạt động, mất lịch sử, hoặc bất đồng bộ giữa dữ liệu hiện tại và lịch sử đều là lỗi ngăn chặn phát hành (release-blocking defect).

## 11. Cổng Hoàn thành (Completion Gate)

Trạng thái Story ngày 2026-07-18: `done`. Automated backend Story/contract suite `45/45 PASS`; code review sạch. Full backend baseline còn lỗi ngoài Journey đã được waive riêng và không được dùng để waive bất kỳ failure nào của Story 6.1.

Đợt kiểm thử thủ công vượt qua (PASS) khi:

- tất cả các kịch bản P0 có thể thực thi đều PASS;
- không tồn tại việc vượt quyền sở hữu hoặc vượt vai trò;
- tồn tại chính xác một vòng đời chuẩn hoá đang hoạt động cho mỗi Người mẹ được kiểm thử;
- mọi thay đổi thành công đều có bản ghi lịch sử tương ứng theo kỳ vọng;
- các request bị từ chối không để lại tác dụng phụ;
- lịch sử chỉ chứa các trường trong danh sách được phép (allow-list);
- các trường hợp kiểm thử đồng thời / chèn lỗi không thể thực thi thủ công được liên kết với bằng chứng tự động hóa đã PASS;
- tất cả token và giá trị ngữ cảnh sức khỏe giả lập đều đã được ẩn khỏi bằng chứng.

Một lần chạy mới không được kết luận PASS chỉ dựa trên manual result. Phê duyệt DPO/quyền riêng tư, preflight cơ sở dữ liệu đích, code review, độ phủ kiểm thử và cổng regression vẫn là các yêu cầu phát hành riêng biệt.

## 12. Tài liệu Tham khảo

- `04_Implement/UC22 - Canonical Mother Lifecycle and Transition History/UC22 - Canonical Mother Lifecycle and Transition History_TDS.md`
- `04_Implement/UC22 - Canonical Mother Lifecycle and Transition History/UC22 - Canonical Mother Lifecycle and Transition History_Test-Spec.md`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260718090000__canonical_mother_lifecycle_history.sql`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260718091000__enforce_mother_journey_transition_immutability.sql`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/controller/JourneyController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/service/impl/JourneyTransitionServiceImpl.java`
- `06_Testing/TestResults/epic-6/story-6-1/code-review-2026-07-18.md`
- `06_Testing/TestResults/epic-6/story-6-1/backend-coverage-2026-07-18.md`
- `06_Testing/TestResults/epic-6/story-6-1/backend-baseline-waiver-2026-07-18.md`
