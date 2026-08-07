# Wave 13 — `growth_measurements` → `health_observations`: chốt mapping

Đóng **prerequisite 4** của V3 §3.12. Quyết định ở đây được rút ra từ dữ liệu thật trên
production ngày 2026-08-07 (8 dòng growth, 513 observation), không phải từ giả định.

Prerequisite 1–3 đã xong ở `V20260807130000`. Prerequisite 5 (chuyển service + frontend)
nằm ở tài liệu này mục 6.

---

## 1. Ràng buộc đã kiểm chứng, chi phối mọi quyết định bên dưới

| Sự thật | Nguồn |
|---|---|
| `health_observations.observation_type` **không** có CHECK, **không** có FK sang definitions | `pg_constraint` |
| `observation_type` chỉ khớp `metric_code` ở 3/7 giá trị đang dùng | dữ liệu production |
| `health_metric_definitions` unique theo `(metric_code, version)` | `health_metric_definitions_code_version_uk` |
| `growth_measurements.care_subject_id = baby_id` ở **cả 8/8** dòng | dữ liệu production |
| Cả 8 care subject đều có `subject_type = 'BABY'` | dữ liệu production |
| `growth.source_type` ∈ {HOME, CLINIC, HOME_SCALE} | dữ liệu production |
| `health_observations.source_type` ∈ {MANUAL, POSTURE_ANALYSIS, EXERCISE_SAFETY} | dữ liệu production |

---

## 2. Quyết định 1 — `observation_type` dùng tên riêng cho trẻ, không dùng lại `WEIGHT`

```
BABY_WEIGHT   BABY_HEIGHT   BABY_HEAD_CIRCUMFERENCE
```

**Không** dùng `WEIGHT`/`HEIGHT` rồi để `subject_type` phân biệt.

Lý do quan trọng nhất là **an toàn dữ liệu**, không phải thẩm mỹ: `WEIGHT` hiện đã tồn tại
cho MOTHER (3 dòng). Nếu cân nặng trẻ cũng mang type `WEIGHT`, một truy vấn quên lọc
`subject_type` sẽ trộn 3.4 kg của trẻ với 60 kg của mẹ trên cùng một biểu đồ — sai lệch âm
thầm, không có exception nào bật lên. Tên riêng làm cho lỗi đó không thể xảy ra.

Lý do thứ hai: definitions unique theo `(metric_code, version)`. Muốn có định nghĩa cho cân
nặng trẻ mà vẫn giữ `metric_code = 'WEIGHT'` thì phải sửa unique key của một bảng đang phục
vụ các metric khác — thay đổi rộng hơn nhiều so với việc đặt tên riêng.

Việc `observation_type` vốn là từ vựng tự do (`POSTURE_FEEDBACK`, `EXERCISE_SAFETY_CHECK`…
đều không có definition) khiến lựa chọn này nhất quán với cách bảng đang được dùng.

---

## 3. Quyết định 2 — hai cột `source_type` là hai từ vựng khác nhau, không gộp

`growth.source_type` trả lời **"đo ở đâu / bằng gì"** (HOME, CLINIC, HOME_SCALE).
`health_observations.source_type` trả lời **"hệ nào sinh ra dữ liệu"** (MANUAL,
POSTURE_ANALYSIS, EXERCISE_SAFETY). Gộp chúng làm mất một trong hai nghĩa.

Vì vậy:

- `health_observations.source_type` = `'MANUAL'` — mọi dòng growth đều do người dùng nhập.
- Giá trị gốc giữ nguyên trong `context_jsonb`:

```json
{ "measurementSetting": "HOME", "note": "..." }
```

`context_jsonb` tồn tại đúng cho mục đích này và đã có CHECK bắt buộc là object.

---

## 4. Bảng mapping đầy đủ

Một dòng growth sinh **tối đa ba** observation. Chỉ sinh dòng cho phép đo khác `NULL`.

| `growth_measurements` | `health_observations` | Ghi chú |
|---|---|---|
| `growth_measurement_id` | `measurement_group_id` | cùng UUID, giữ session là một aggregate |
| `care_subject_id` | `care_subject_id` | |
| `baby_id` | *(bỏ)* | trùng `care_subject_id` ở 8/8 dòng |
| — | `subject_type` = `'BABY'` | |
| `measured_date` (date) | `observed_at` | date lúc **00:00 `Asia/Ho_Chi_Minh'`** |
| `weight_kg` | `observation_type='BABY_WEIGHT'`, `value_numeric`, `unit='kg'` | |
| `height_cm` | `observation_type='BABY_HEIGHT'`, `value_numeric`, `unit='cm'` | |
| `head_circumference_cm` | `observation_type='BABY_HEAD_CIRCUMFERENCE'`, `value_numeric`, `unit='cm'` | |
| `source_type` | `context_jsonb->>'measurementSetting'` | xem mục 3 |
| — | `source_type` = `'MANUAL'` | |
| `note` | `context_jsonb->>'note'` | lặp lại trên cả 3 dòng cùng group |
| `created_at`, `updated_at`, `deleted_at` | y nguyên | |
| — | `legacy_source` = `'growth_measurements'` | |
| — | `legacy_id` = `'<growth_measurement_id>:<TYPE>'` | V3 §3.12, khoá idempotent |

`measured_date` là `date`; đổi ngược lấy `(observed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date`.

`note` bị lặp trên cả ba dòng là **có chủ ý**: nó thuộc về buổi đo, và
`measurement_group_id` là thứ giữ chúng là một. Chuẩn hoá `note` ra bảng riêng sẽ dựng lại
đúng cái quan hệ mà wave này đang xoá bỏ.

### Bẫy: `HealthObservation.note` **không** phải chỗ để ghi chú

Entity `HealthObservation` có field tên `note`, nhưng nó map vào cột `text_value`. Cái tên
gây hiểu nhầm — kiểm dữ liệu production cho thấy `text_value` chứa **nội dung văn bản của
chính phép đo**, không phải chú thích của người dùng:

| observation_type | dòng | có text_value | mẫu |
|---|---|---|---|
| `POSTURE_FEEDBACK` | 344 | 344 | nội dung phản hồi tư thế |
| `BLOOD_PRESSURE_SYSTOLIC` | 1 | 1 | `115/75 mmHg` |
| mọi loại còn lại | — | 0 | — |

Ghi `note` của buổi đo vào `text_value` sẽ trộn hai khái niệm và làm hỏng ý nghĩa của cột với
các metric khác. Store phải dùng `context_jsonb`, **không** dùng `HealthObservation.note`.

---

## 5. Quyết định 3 — update và delete thao tác theo **group**, không theo dòng

- **Update một buổi đo**: upsert theo `(measurement_group_id, observation_type)`. Phép đo bị
  xoá khỏi buổi đo (giá trị chuyển thành `NULL`) thì dòng tương ứng bị **soft-delete**, không
  hard-delete — để `legacy_id` vẫn giữ chỗ và backfill chạy lại không tái sinh nó.
- **Xoá một buổi đo**: set `deleted_at` cho **mọi** dòng cùng `measurement_group_id`.
- **Đọc**: gom theo `measurement_group_id`, luôn kèm `deleted_at IS NULL`.

> **Bẫy phải nhớ:** `deleted_at` là cột chết cho tới khi read path lọc nó. Hiện **chưa** query
> nào loại `deleted_at IS NOT NULL`. Bộ lọc phải vào cùng release với backfill, nếu không
> những buổi đo đã xoá sẽ sống lại.

---

## 6. Quyết định 4 — chuyển thẳng code, **không** làm compatibility view

V3 §3.12 cho hai lựa chọn. Chọn chuyển thẳng.

Bề mặt thật đã đếm:

| Thành phần | Quy mô |
|---|---|
| `GrowthServiceImpl` | 296 dòng |
| `GrowthMeasurementRepository` | 4 method |
| `GrowthMeasurement` entity | 78 dòng |
| `AppointmentPreparationServiceImpl`, `BabyCareOverviewServiceImpl`, `BabyCareTimelineServiceImpl` | mỗi cái gọi **đúng 1** method |
| Web `BabyCareHubPage.tsx` | 1 trang |

Compatibility view sẽ phải là **updatable view kèm INSTEAD OF trigger** vì
`GrowthServiceImpl` có add/update/delete — nhiều máy móc hơn chính lượng code nó thay thế.
View chỉ đáng làm khi không thể chuyển hết consumer trong một release; ở đây chuyển được.

---

## 7. Thứ tự triển khai

1. **Expand** — seed 3 definition BABY, backfill 8 dòng hiện có (idempotent qua
   `legacy_source`/`legacy_id`). `growth_measurements` **không** bị đụng.
2. **Cutover** — service và 3 consumer đọc/ghi `health_observations`; read path thêm bộ lọc
   `deleted_at IS NULL`; frontend đổi theo.
3. **Observe** — `growth_measurements` đóng băng, đối chiếu số liệu.
4. **Contract** — chạy lại backfill idempotent để hứng dòng ghi trễ giữa bước 1 và 2, rồi mới
   drop `growth_measurements`.

Bước 4 tồn tại vì có một khe hở thật: dòng nào được ghi vào `growth_measurements` sau backfill
nhưng trước khi deploy bước 2 sẽ chưa có mặt trong `health_observations`. Backfill idempotent
chạy lại ngay trước khi drop sẽ hứng chúng — đó là lý do khoá `(legacy_source, legacy_id)`
phải tồn tại.
