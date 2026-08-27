# Hàm vét điều phối chuyên gia — `GET /consultation-requests/matching`

> Mẹ bầu cần tư vấn → hệ thống quét ngang lịch rảnh của toàn bộ chuyên gia và gợi ý danh sách,
> ưu tiên Chuyên gia Hệ thống, hết mới rơi xuống Chuyên gia Y tế Cộng đồng.
>
> **Liên quan:** [`expert-two-tier-flow.md`](expert-two-tier-flow.md) (phân loại hai nhóm).

---

## 1. 🎯 Tôi hiểu yêu cầu như thế nào

| Yêu cầu bro nêu | Cách hiểu của tôi |
|---|---|
| "chuyên gia họ sẽ có khung giờ rảnh" | Các ca 1 tiếng trong `expert_availability`, chuyên gia tự set qua màn Lịch rảnh |
| "ưu tiên chuyên gia hợp đồng trước, cộng đồng sau" | Hai tuyến. Tuyến 2 **chỉ chạy khi tuyến 1 rỗng**, không trộn lẫn |
| "nếu tg rảnh còn 5p... ưu tiên thằng nào nhiều hơn" | Xếp hạng theo **độ dài khối giờ rảnh liền mạch**, dài hơn thì lên trước; khối quá ngắn bị loại thẳng |
| "logic phải đăng ký để chuyên gia chấp nhận thì giữ lại" | Hàm này **chỉ gợi ý**. Vẫn phải `POST /consultation-requests` rồi chuyên gia bấm chấp nhận |
| "chuyên gia không đặt thời gian rảnh mà vẫn book được" | Hai kiểu ứng viên: **THEO CA** và **YÊU CẦU MỞ**. Xem §4 |
| "không được tạo thêm bảng" | Chỉ đọc 3 bảng có sẵn: `expert_availability`, `users`, `expert_consultation_requests` |
| "viết hàm đơn giản, tận dụng class cũ" | 1 service + 1 câu truy vấn thêm vào repository cũ. DTO là record lồng trong service |

---

## 2. ⚠️ Một chi tiết làm đổi cách hiểu "còn 5 phút"

Luật đặt lịch hiện tại ở
[`ConsultationRequestServiceImpl.java:184-185`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/service/impl/ConsultationRequestServiceImpl.java#L184-L185):

```java
boolean exactHourlySlot = start.isAfter(now)
        && end.equals(start.plus(1, ChronoUnit.HOURS));
```

`start.isAfter(now)` nghĩa là **ca đang chạy dở thì không đặt được**. Chuyên gia rảnh 07:00–08:00,
bây giờ là 07:55 → ca đó đã không thể đặt, chứ không phải "đặt được nhưng chỉ còn 5 phút".

Nên luật "còn 5 phút" tôi hiện thực thành: **khối giờ rảnh liền mạch phía trước quá ngắn thì loại**,
chứ không phải đo phần thừa của ca đang chạy. Ngưỡng mặc định **30 phút** (`minMinutes`, sửa được
qua tham số).

Nếu ý bro thực sự là *"cho phép đặt cả ca đang chạy dở, và nếu còn dưới 5 phút thì bỏ"* thì phải sửa
`validatePreferredAvailability` — đó là thay đổi luật đặt lịch, nằm ngoài hàm vét. Bảo tôi nếu muốn.

---

## 3. 🔄 Hàm vét chạy thế nào

```
                    sweep(specialty, limit, windowHours, minMinutes)
                                     │
        ┌────────────────────────────▼────────────────────────────┐
        │ B1. Quét ngang MỌI ca đặt được trong cửa sổ tìm kiếm    │
        │     (1 truy vấn native, mặc định 24 giờ tới)            │
        └────────────────────────────┬────────────────────────────┘
                                     ▼
        ┌─────────────────────────────────────────────────────────┐
        │ B2. Gộp ca liền nhau thành KHỐI, giữ khối dài nhất      │
        │     của mỗi chuyên gia; khối < minMinutes → loại        │
        └────────────────────────────┬────────────────────────────┘
                                     ▼
        ┌─────────────────────────────────────────────────────────┐
        │ B3. Thêm ứng viên YÊU CẦU MỞ: chuyên gia đủ điều kiện   │
        │     mà KHÔNG còn ca tương lai nào (§4)                  │
        └────────────────────────────┬────────────────────────────┘
                                     ▼
        ┌─────────────────────────────────────────────────────────┐
        │ B4. Chia hai rổ theo expert_type                        │
        └────────────────────────────┬────────────────────────────┘
                     ┌───────────────┴───────────────┐
                     ▼                               ▼
        rổ CONTRACTED khác rỗng?              rổ CONTRACTED rỗng
                     │                               │
                     ▼                               ▼
        tier=CONTRACTED, fallback=false   tier=COMMUNITY, fallback=true
                     │                               │
                     └───────────────┬───────────────┘
                                     ▼
        ┌─────────────────────────────────────────────────────────┐
        │ B5. Xếp hạng trong rổ — SLOT luôn trên OPEN:            │
        │     SLOT: ① khối dài hơn ② sớm hơn ③ rating cao hơn     │
        │     OPEN: ① ít yêu cầu đang chờ nhất ② rating cao hơn   │
        └─────────────────────────────────────────────────────────┘
```

### 3.1. Bước 1 — truy vấn quét ngang

Thêm `findBookableSlots` vào `ExpertAvailabilityRepository` (repository cũ, chỉ thêm 1 method).
Bốn điều kiện tài khoản copy nguyên từ truy vấn danh bạ — thiếu một cái là lọt chuyên gia đang bị
đình chỉ vào gợi ý tự động:

```sql
u.verification_status = 'APPROVED'
AND u.trust_status = 'ACTIVE'
AND u.enabled = true AND u.locked = false
AND a.status = 'AVAILABLE'
AND a.start_at > :notBefore AND a.start_at < :notAfter
```

Kèm một mệnh đề quan trọng:

```sql
AND NOT EXISTS (
     SELECT 1 FROM expert_consultation_requests r
     WHERE r.expert_profile_id = a.user_id
       AND r.status IN ('PENDING', 'ACCEPTED')
       AND r.preferred_window_start = a.start_at
       AND r.preferred_window_end   = a.end_at)
```

**Đây là cách tránh trùng lịch mà không phải đụng vào luồng `accept()`.** Hệ thống hiện chưa đặt slot
sang `BUSY` và chưa tạo row `consultation_bookings` khi chuyên gia chấp nhận (xem §10 của
`expert-two-tier-flow.md`). Nếu hàm vét không tự lọc, nó sẽ gợi ý mãi cái ca 09:00 mà ba người đã hỏi.
Mệnh đề trên đọc thẳng bảng yêu cầu nên đúng ngay, không cần sửa luồng cũ và không cần bảng mới.

### 3.2. Bước 2 — gộp khối, đây là chỗ thực thi "ưu tiên thằng nhiều hơn"

Các ca 1 tiếng nối đuôi nhau (`ca trước.end_at == ca sau.start_at`) được gộp làm một khối. Mỗi
chuyên gia giữ lại **khối dài nhất**.

Ví dụ, bây giờ là 08:30:

| Chuyên gia | Ca đã set | Khối liền mạch | Kết quả |
|---|---|---|---|
| BS A | 09:00–10:00 | 60 phút | vào danh sách |
| BS B | 09:00–10:00, 10:00–11:00, 11:00–12:00 | **180 phút** | **xếp trên BS A** |
| BS C | 09:00–09:30 *(nếu có ca lẻ)* | 30 phút | vào danh sách nếu ≥ `minMinutes` |
| BS D | 08:00–09:00 (đang chạy) | 0 phút đặt được | **loại** |

BS B thắng BS A dù cả hai cùng bắt đầu 09:00 — vì mẹ bầu có thể tư vấn dài hơn mà không bị cắt ngang.

### 3.3. Bước 5 — thứ tự xếp hạng ứng viên THEO CA

```java
Comparator
    .comparingLong(Candidate::blockMinutes).reversed()   // ① nhiều giờ rảnh nhất
    .thenComparing(Candidate::slotStart)                 // ② bắt đầu sớm nhất
    .thenComparing(Candidate::ratingAvg, nullsLast(reverseOrder()));  // ③ đánh giá cao nhất
```

> 🔀 **Một lựa chọn tôi tự quyết, bro xem có đúng ý không.** Đặc tả gốc (§3.1 tài liệu Buổi 24) ghi
> *"lịch trống **gần nhất** với thời điểm hiện tại"*, còn bro vừa yêu cầu *"ưu tiên thằng **nhiều
> hơn**"*. Hai cái ngược nhau, tôi để **nhiều giờ trước, sớm sau**. Muốn đảo lại chỉ cần hoán vị hai
> dòng đầu.

> 🪟 **Vì sao phải có cửa sổ tìm kiếm 24 giờ.** Nếu không giới hạn, một chuyên gia rảnh 8 tiếng liền
> vào tuần sau sẽ luôn thắng chuyên gia rảnh 2 tiếng chiều nay — trong khi mẹ bầu đang cần tư vấn
> ngay. `windowHours` sửa được qua tham số.

---

## 4. 📭 Chuyên gia không đặt lịch rảnh vẫn đặt được

### 4.1. Cơ chế đã có sẵn, không phải viết mới

Dòng đầu của `validatePreferredAvailability`:

```java
if (request.getPreferredWindowStart() == null) {
    return;                     // không gửi khung giờ thì không kiểm gì cả
}
```

Nghĩa là hệ thống **đã hỗ trợ sẵn hai kiểu đặt** ngay từ đầu, chỉ là chưa ai dùng kiểu thứ hai:

| | THEO CA (`SLOT`) | YÊU CẦU MỞ (`OPEN`) |
|---|---|---|
| Chuyên gia | Đã set lịch rảnh | **Chưa set ca nào** |
| `preferredWindowStart/End` | Ca cụ thể, đúng 1 giờ | **`null`** |
| Mẹ bầu thấy gì | Lưới giờ, bấm chọn | Ô mô tả + *"chuyên gia sẽ phản hồi trong 48 giờ"* |
| Chuyên gia làm gì | Chấp nhận đúng ca đó | Chấp nhận rồi chủ động hẹn giờ qua chat |

Kiểu `OPEN` hợp với **Chuyên gia Y tế Cộng đồng**: họ tình nguyện, không cam kết lịch cố định, bắt
họ duy trì calendar là cách nhanh nhất để mất tình nguyện viên.

### 4.2. Luật phân loại: có lịch thì tôn trọng lịch

> **Chuyên gia còn bất kỳ ca `AVAILABLE` nào trong tương lai ⇒ chỉ đặt được THEO CA.
> Không còn ca nào ⇒ thành ứng viên YÊU CẦU MỞ.**

Đây là chỗ tôi phải tự quyết, và nó **ảnh hưởng trực tiếp tới luật fallback** nên bro soát kỹ giúp.

Nếu cho mọi chuyên gia đều nhận được yêu cầu mở bất kể lịch, thì Chuyên gia Hệ thống sẽ **không bao
giờ hết chỗ**, và tuyến 2 (cộng đồng) gần như chẳng bao giờ được kích hoạt — trái với đặc tả §3.2
*"trong trường hợp toàn bộ chuyên gia hệ thống đều kín lịch, hệ thống mới chuyển sang đề xuất
Chuyên gia Y tế Cộng đồng"*.

Luật trên giữ được cả hai:

| Tình huống | Kết quả |
|---|---|
| BS hợp đồng, còn ca trống trong 24h | Ứng viên `SLOT`, tuyến 1 |
| BS hợp đồng, còn ca trống nhưng **ngoài** cửa sổ 24h | Không phải ứng viên lần này |
| BS hợp đồng, **kín lịch hoàn toàn** (không còn ca tương lai nào) | Ứng viên `OPEN`, tuyến 1 |
| BS cộng đồng, chưa từng set lịch | Ứng viên `OPEN`, tuyến 2 |

Nói cách khác: chuyên gia đã set lịch là đang **tuyên bố khi nào mình rảnh** — hệ thống tôn trọng
tuyên bố đó và không quấy họ ngoài giờ. Chuyên gia không set lịch thì chưa tuyên bố gì, nên nhận
yêu cầu mở bất cứ lúc nào.

### 4.3. Xếp hạng ứng viên YÊU CẦU MỞ

Không có khối giờ để so, nên đổi khoá xếp hạng sang **tải hiện tại**:

```java
Comparator
    .comparingLong(Candidate::openRequestCount)          // ① ít yêu cầu đang chờ nhất
    .thenComparing(Candidate::ratingAvg, nullsLast(reverseOrder()));  // ② đánh giá cao nhất
```

Dùng `countByExpertProfileIdAndStatus(id, PENDING)` — method **đã có sẵn** trong
`ConsultationRequestRepository`, không phải viết thêm.

Thiếu mệnh đề ①, bác sĩ tình nguyện điểm cao nhất sẽ nhận **toàn bộ** yêu cầu mở, những người khác
không bao giờ được gợi ý — và người nhận hết sẽ bỏ cuộc sau vài ngày.

### 4.4. Thứ tự trong một tuyến

`SLOT` luôn đứng trên `OPEN`. Một khung giờ cụ thể có giá trị hơn hẳn lời hứa *"sẽ phản hồi trong
48 giờ"*, kể cả khi người kia rảnh nhiều hơn.

```
tuyến 1 (CONTRACTED):  [SLOT xếp theo khối dài nhất]  →  [OPEN xếp theo tải thấp nhất]
                                    ↓ cả hai đều rỗng
tuyến 2 (COMMUNITY):   [SLOT xếp theo khối dài nhất]  →  [OPEN xếp theo tải thấp nhất]
```

---

## 5. 🧩 API

```
GET /api/v1/consultation-requests/matching
    ?specialty=SAN_KHOA     (tuỳ chọn, lọc theo chuyên khoa)
    &limit=5                (mặc định 5)
    &windowHours=24         (mặc định 24)
    &minMinutes=30          (mặc định 30 — luật "khối quá ngắn thì bỏ")

@PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
```

Trả về:

```json
{
  "tier": "CONTRACTED",        // hoặc COMMUNITY, hoặc null khi không ai rảnh
  "fallback": false,           // true = tuyến 1 rỗng, đây là nhóm cộng đồng
  "candidates": [
    {
      "kind": "SLOT",          // SLOT = đặt theo ca | OPEN = gửi yêu cầu mở
      "expertProfileId": "…",  "displayName": "BS Đỗ Hải Long",
      "specialty": "Sản khoa", "expertType": "CONTRACTED",
      "availabilityId": "…",
      "slotStart": "2026-08-23T02:00:00Z",   // ca cụ thể để đặt, đúng 1 giờ
      "slotEnd":   "2026-08-23T03:00:00Z",
      "blockStart":"2026-08-23T02:00:00Z",   // khối liền mạch, dùng để xếp hạng
      "blockEnd":  "2026-08-23T05:00:00Z",
      "blockMinutes": 180,
      "startingSoon": true,
      "openRequestCount": 0
    },
    {
      "kind": "OPEN",          // chưa set lịch — client gửi preferredWindow = null
      "expertProfileId": "…",  "displayName": "BS Trần Thị Thu Nga",
      "specialty": "Sản khoa", "expertType": "COMMUNITY",
      "availabilityId": null,  "slotStart": null, "slotEnd": null,
      "blockMinutes": 0,       "openRequestCount": 2
    }
  ]
}
```

`tier` và `fallback` để client hiển thị đúng ngữ cảnh: ở tuyến 2 phải nói rõ *"hiện không còn Chuyên
gia Hệ thống trống lịch"*, và khi người dùng bấm **Không** thì bật disclaimer miễn trừ trách nhiệm
rồi chuyển sang chế độ tự theo dõi (đặc tả §3.3).

`slotStart` / `slotEnd` là thứ client gửi lại nguyên vẹn vào `preferredWindowStart/End` khi tạo yêu
cầu — khớp đúng ràng buộc "đúng một ca AVAILABLE, dài đúng 1 giờ" của luồng cũ.

Với ứng viên `kind = "OPEN"`, client **bỏ trống** `preferredWindowStart/End`. Backend không cần sửa
gì: `validatePreferredAvailability` tự thoát sớm, yêu cầu vào thẳng trạng thái `PENDING` và hết hạn
sau 48 giờ như mọi yêu cầu khác.

---

## 6. 🚧 Ranh giới — hàm này KHÔNG làm gì

- **Không tự đặt lịch.** Chỉ trả danh sách gợi ý. Luồng đăng ký → chuyên gia chấp nhận giữ nguyên
  100%, đúng yêu cầu của bro.
- **Không đổi trạng thái gì.** `@Transactional(readOnly = true)`, thuần đọc.
- **Không tạo bảng.** Ba bảng đọc vào đều đã có.
- **Không sửa luồng `accept()`.** Việc slot chưa bị tiêu thụ khi chấp nhận vẫn là nợ kỹ thuật; hàm vét
  chỉ tự né bằng `NOT EXISTS`. Vá triệt để thì phải tạo `consultation_bookings` + đặt slot `BUSY`
  trong `accept()`, việc đó nên làm riêng.

---

## 7. 📁 Đụng vào những file nào

| File | Thay đổi |
|---|---|
| `consultation/matching/ExpertMatchingService.java` | **file mới duy nhất** — service + 1 enum + 2 record lồng bên trong |
| `expertavailability/repository/ExpertAvailabilityRepository.java` | **+1 method** `findBookableSlots` |
| `expert/repository/ExpertProfileRepository.java` | **+1 method** `findOpenRequestExperts` |
| `consultation/repository/ConsultationRequestRepository.java` | **+1 method** `countPendingByExpert` (gộp đếm, tránh N+1) |
| `consultation/controller/ConsultationRequestController.java` | **+1 endpoint** `GET /matching` |
| 2 file test của controller | **+1 mock** cho dependency mới |

Không thêm entity, không thêm DTO rời, không thêm bảng.

> ⚙️ **Không inject `Clock`.** Ứng dụng không đăng ký bean `Clock` nào, nên service dùng đúng pattern
> hai constructor của `ExpertAvailabilityServiceImpl`: constructor Spring lấy đồng hồ hệ thống, còn
> constructor kia để test tiêm đồng hồ cố định. Inject thẳng `Clock` sẽ làm app chết lúc khởi động.

---

## 8. ✅ Đã kiểm chứng trên dữ liệu thật

Chạy trên Supabase với 2 chuyên gia hợp đồng, 2 cộng đồng:

| Kịch bản | Kết quả |
|---|---|
| Chưa ai set lịch | `tier=CONTRACTED`, cả 2 ứng viên `OPEN` |
| Hoàng: 1 ca 08:00 · Long: 3 ca 09:00–12:00 | **Long (180p) xếp trên Hoàng (60p)** — khối dài thắng bắt đầu sớm |
| Lọc `specialty=Nhi khoa` | Chỉ còn Hoàng |
| Lọc `specialty=Dinh dưỡng` (không chuyên gia hợp đồng nào có) | `tier=COMMUNITY`, `fallback=true` |
| Tạo yêu cầu vào ca 09:00 của Long rồi vét lại | Ca 09:00 biến mất, khối tụt **180p → 120p**, bắt đầu 10:00 |

Test suite backend: `Tests run: 176, Failures: 1, Errors: 1` — đúng bằng 2 lỗi có sẵn từ trước
(`FileServiceImplTest` giới hạn 20MB/100MB và `DirectMessageServiceImplExpertRevokedTest` cần Docker),
không phát sinh lỗi mới.
