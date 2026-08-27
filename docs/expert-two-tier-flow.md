# Hai nhóm chuyên gia & thoả thuận hợp tác — `/expert/contract`

> Phân tích nghiệp vụ & thiết kế dữ liệu cho việc tách Chuyên gia Hệ thống (có thoả thuận hợp tác)
> và Chuyên gia Y tế Cộng đồng (tình nguyện): cách phân loại, màn đăng ký, màn xét duyệt của admin,
> cách ký thoả thuận điện tử, và cách lưu bằng chứng đồng thuận.
>
> **Bước trước đó:** xác minh danh tính + bằng cấp — xem [`expert-verification-flow.md`](expert-verification-flow.md).

---

## 1. 🎯 Mục tiêu nghiệp vụ

Phân tách hai nhóm chuyên gia trên UI và trong CSDL, sao cho huy hiệu và quyền ưu tiên hiển thị
phản ánh đúng **mức cam kết** mà mỗi nhóm đã nhận với hệ thống.

| | |
|---|---|
| **Input** | Nguyện vọng của người đăng ký + quyết định của quản trị viên + bản chấp nhận điều khoản |
| **Output** | `users.expert_type ∈ {COMMUNITY, PENDING_CONTRACT, CONTRACTED}` |
| **Nguyên tắc xuyên suốt** | Không ai **tự phong** mình là chuyên gia hợp tác. `CONTRACTED` chỉ đạt được khi đủ hai điều kiện độc lập: admin đã duyệt **và** chuyên gia đã ký. |

### 1.1. Định nghĩa hai nhóm

| | Chuyên gia Hệ thống | Chuyên gia Y tế Cộng đồng |
|---|---|---|
| **Bản chất** | Đã ký thoả thuận hợp tác, **cam kết duy trì lịch trực** | Tư vấn hỗ trợ tình nguyện, phản hồi khi rảnh |
| **Huy hiệu** | Khiên có dấu tích, **đặc**, emerald | Trái tim trong bàn tay, **viền**, teal |
| **Thứ tự hiển thị** | Ưu tiên đầu danh bạ; là tuyến 1 khi điều phối tự động | Sau nhóm hợp tác; là tuyến 2 (fallback) |
| **Lịch rảnh** | **Bắt buộc** — gate onboarding, tối thiểu 10 ca/tuần | Không yêu cầu |
| **Xác minh chuyên môn** | Giống hệt nhau — cả hai đều qua CCCD, đối chiếu khuôn mặt, giấy phép hành nghề, và đều **phải được admin xét duyệt** |

> 💡 **Điều phân biệt hai nhóm là cam kết, không phải tiền.** Ở phiên bản này hệ thống chưa triển
> khai thu/chi. Hệ thống ưu tiên người đã nhận trách nhiệm trực lịch — đây là lập luận đứng vững
> trước hội đồng, khác hẳn với "ưu tiên người trả tiền".

> ⚠️ **Thuật ngữ bắt buộc.** Không dùng *"bác sĩ miễn phí"*, *"bác sĩ free"* ở bất kỳ đâu trong
> mã nguồn, UI hay tài liệu. Danh xưng chuẩn là **"Chuyên gia Y tế Cộng đồng"**.

### 1.2. Huy hiệu — phân biệt bằng hình, không chỉ bằng màu

| | Chuyên gia Hệ thống | Chuyên gia Y tế Cộng đồng |
|---|---|---|
| Icon | `Icons.verified_rounded` (đặc) | `Icons.volunteer_activism` (viền) |
| Màu | `#10B981` emerald | `#0EA5E9` teal |
| Tooltip | "Chuyên gia y tế được xác thực bởi hệ thống" | "Chuyên gia Y tế Cộng đồng — chứng chỉ hành nghề đã được kiểm duyệt" |

Khoảng 8% nam giới bị mù màu đỏ–lục, và slide bảo vệ in đen trắng sẽ làm hai huy hiệu chỉ khác màu
trở thành y hệt nhau. **Khác hình dạng** thì phân biệt được trong mọi điều kiện — và là câu trả lời
sẵn nếu hội đồng hỏi về accessibility.

Chuyên gia cộng đồng **vẫn có huy hiệu**. Để trống sẽ khiến người dùng tưởng họ chưa được xác minh,
trong khi thực tế họ đã qua đúng quy trình kiểm duyệt chứng chỉ như nhóm hợp tác.

---

## 2. 🗄️ Mô hình dữ liệu — một cột, không thêm bảng

### 2.1. `users.expert_type` — máy trạng thái ba giá trị

```sql
-- V7__add_expert_type.sql
ALTER TABLE public."users"
ADD COLUMN IF NOT EXISTS "expert_type" character varying(20);

-- Mọi chuyên gia hiện có về nhóm cộng đồng; admin nâng tay từng người.
UPDATE public."users"
SET "expert_type" = 'COMMUNITY'
WHERE "role" = 'EXPERT' AND "expert_type" IS NULL;
```

CHECK chỉ ràng buộc miền giá trị, **không** ràng theo role:

```sql
CHECK ("expert_type" IS NULL
       OR "expert_type" IN ('COMMUNITY','PENDING_CONTRACT','CONTRACTED'))
```

```java
public enum ExpertType {
    COMMUNITY,          // Chuyên gia Y tế Cộng đồng
    PENDING_CONTRACT,   // đã xin + đã được duyệt, đang chờ ký
    CONTRACTED          // đã ký, đang hiệu lực
}
```

**Vì sao phải có `PENDING_CONTRACT`.** Luồng chốt là ký **sau** khi admin duyệt, nên tồn tại một
trạng thái hợp lệ và có thể kéo dài nhiều ngày: *đã được duyệt chuyên môn, đã được phát hành đề nghị,
chưa bấm ký*. Với enum chỉ hai giá trị, trạng thái đó buộc phải chọn một trong hai câu trả lời sai —
ghi `CONTRACTED` thì huy hiệu bật khi chưa có bản ký nào, ghi `COMMUNITY` thì hệ thống quên mất phải
đẩy họ vào trang ký. Đây không phải trường hợp hiếm: nó là trạng thái **mặc định** của mọi chuyên gia
hợp tác ngay sau khi được duyệt.

**Luật đọc — một dòng, fail-closed:**

```java
public boolean isContracted() {
    return expertType == ExpertType.CONTRACTED;   // NULL / COMMUNITY / PENDING_CONTRACT đều false
}
```

`PENDING_CONTRACT` cư xử **giống hệt** `COMMUNITY` ở mọi đường đọc — không huy hiệu hợp tác, không
ưu tiên sorting. Nó chỉ khác ở đúng một chỗ: `determineNextStep` thấy giá trị này thì đẩy vào
`/expert/contract`.

**Ràng buộc đi kèm:**

- Cột **phải NULLABLE**. Bảng `users` dùng chung cho mọi role — xem cảnh báo có sẵn tại
  [`ExpertProfile.java:69-71`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/entity/ExpertProfile.java#L69-L71):
  khai `NOT NULL` trên entity map vào `users` sẽ làm hỏng schema H2 sinh ra cho **mọi** entity khác cùng bảng.
- Không ràng CHECK theo role. Role được gán ngay khi người dùng chọn vai trò, trước khi hồ sơ chuyên
  gia tồn tại — ràng theo role sẽ làm vỡ luồng đăng ký.

### 2.2. Bằng chứng chấp nhận điều khoản → `audit_events`

Dùng đúng pattern mà [`ContributionPoint.java:22-25`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/entity/ContributionPoint.java#L22-L25)
đã dùng: entity map lên `audit_events` + `@SQLRestriction("event_category = 'EXPERT_CONTRACT'")`.

Bảng [`audit_events`](../05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__baseline_production_schema.sql#L1690-L1715)
đã có sẵn đúng bộ field của một bằng chứng đồng thuận: `actor_user_id`, `ip_address`, `user_agent`,
`checksum`, `occurred_at`, `decision`, `payload` jsonb.

Ba loại row, cùng `event_category`:

| `decision` | Ghi khi nào | Payload |
|---|---|---|
| `REQUESTED` | Chuyên gia chọn hình thức ở bước 2 (ghi qua `AuditService`, action `EXPERT_VERIFICATION`) | `{ requestedType }` |
| `ACCEPTED` | Chuyên gia bấm đồng ý ở trang ký | `{ termsVersion, termsHash, acceptedFullName, contractFileId }` |
| `TERMINATED` | Admin hạ khỏi nhóm hợp tác | `{ reason, terminatedBy }` |

Cột `expert_type` giữ **trạng thái hiện tại**; `audit_events` giữ **lịch sử**. Nhờ vậy vẫn trả lời
được "người này từng xin hợp tác nhưng bị xếp xuống cộng đồng" mà không cần thêm cột thứ hai.

> ❌ **Không dùng module `consent/`.** Nó map vào `data_permissions` với `permission_kind='CONSENT_GRANT'`,
> và `ConsentDataType` chỉ có `HEALTH_RECORD / LOCATION / SENSOR_DATA…` — đó là đồng thuận PDPA về
> **dữ liệu**, đang được `ConsentCheckPolicy` dùng để quyết định quyền truy cập. Nhét thoả thuận hợp
> tác vào đó là làm bẩn logic phân quyền.

### 2.3. Bản PDF thoả thuận → Cloudflare R2

Đi đúng đường mà giấy phép hành nghề đang đi: `IFileService` → bảng `attachments` → `file_id`.

| Tính chất | Chi tiết |
|---|---|
| **Kho** | Bucket riêng `carebridge-private` ([`application.yaml:204-209`](../05_Development/CareBridgeAPI/src/main/resources/application.yaml#L204-L209)) |
| **Giao thức** | S3 API — `R2StorageService` chạy nguyên `software.amazon.awssdk.services.s3.S3Client` / `S3Presigner`. Đổi sang AWS S3 thật chỉ là đổi biến môi trường, không sửa code. |
| **Mã hoá at-rest** | `.serverSideEncryption(ServerSideEncryption.AES256)` — nằm cứng trong `store()`, không phải tuỳ chọn |
| **Truy cập** | Presigned URL, TTL chặn cứng tối đa 15 phút: `Math.max(1, Math.min(ttlMinutes, 15))` |
| **Không có đường public** | `storePublic()` mặc định ném `UnsupportedOperationException`; chỉ `CloudinaryStorageService` override ([`IStorageService.java:13-17`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/service/IStorageService.java#L13-L17)) |

> ⚠️ **Phải là PDF, không phải ảnh.** [`FileServiceImpl.java:191-199`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/service/impl/FileServiceImpl.java#L191-L199)
> định tuyến `FileKind.IMAGE → cloudinary` và **không có đường vòng**. Ảnh thoả thuận sẽ không vào R2.
> Link ký của Cloudinary hiện chưa thực sự hết hạn (comment trong `CloudinaryStorageService` ghi rõ
> *"Enabling true expiry is a follow-up"*) — một link lọt ra ngoài là truy cập được vĩnh viễn.

Vì bản thoả thuận được render server-side (không phải `MultipartFile`), dùng overload:

```java
fileService.uploadPrivateBytes(pdfBytes, expertUserId, "application/pdf",
        "thoa-thuan-hop-tac-" + version + ".pdf", FilePurpose.EXPERT_CONTRACT);
```

Thêm `EXPERT_CONTRACT` vào enum [`FilePurpose`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/enums/FilePurpose.java).

---

## 3. 🔄 Luồng nghiệp vụ End-to-End

### 3.1. Sơ đồ tổng quan

```
Chuyên gia (Flutter/Web)              Backend (Spring Boot)                 Kho
──────────────────────────────────────────────────────────────────────────────────────
[1] Hồ sơ nghề nghiệp     ──POST──▶ ExpertProfileServiceImpl.createProfile()
                                            │           ◀── giống nhau cả 2 nhóm
[2] CHỌN HÌNH THỨC        ──PATCH─▶ expert_type = PENDING_CONTRACT | COMMUNITY
    (2 thẻ lựa chọn)                        │   + audit EXPERT_TYPE_REQUESTED
                                            │   chưa duyệt nên chưa vào danh bạ
                                            ▼
[3] CCCD + selfie         ──POST──▶ ExpertIdentityVerificationService  ──▶ CompreFace
[4] Giấy phép hành nghề   ──POST──▶ ExpertCredentialServiceImpl        ──▶ R2
                                            │           ◀── giống nhau cả 2 nhóm
                                            ▼
Admin (React)  ──GET  /review-cases────▶ một hàng đợi chung, có chip phân nhóm
               ──POST /profiles/{id}/approve──▶ duyệt chuyên môn + chốt nhóm
                                            │
              ┌─────────────────────────────┴──────────────────────────┐
              ▼                                                        ▼
   expert_type = PENDING_CONTRACT                          expert_type = COMMUNITY
   (phát hành đề nghị hợp tác)                             → COMPLETE, vào app luôn
              │
              ▼
[5] TRANG KÝ              ──GET───▶ /expert/contract/offer
                          ──POST──▶ /expert/contract/accept
                                            │ ①sinh PDF ②đẩy R2 ③audit ACCEPTED
                                            │ ④expert_type = CONTRACTED
                                            ▼
[6] Cấu hình lịch rảnh    ──PUT───▶ /expert/availability/batch   (bắt buộc)
                                            ▼
                                        COMPLETE
```

### 3.2. Nguyên tắc: một pipeline, ba chỗ rẽ nhánh

Cả hai nhóm dùng **chung một luồng đăng ký, chung một hàng đợi duyệt, chung một trang chi tiết**.
Không nhân đôi pipeline xác minh — cả hai đều là bác sĩ thật, đều phải nộp CCCD, đối chiếu khuôn mặt
và giấy phép hành nghề y hệt nhau.

Toàn bộ khác biệt gói gọn ở ba chỗ:

| # | Chỗ rẽ | Nội dung |
|---|---|---|
| 1 | Bước 2 khi đăng ký | Hai thẻ lựa chọn hình thức |
| 2 | Khung chi tiết của admin | Khối "Xét duyệt hợp tác" + nút bấm khác nhau |
| 3 | Sau khi duyệt | Nhánh hợp tác có thêm bước ký + bước lịch |

### 3.3. Vì sao ký **sau** khi duyệt, không phải trước

Thoả thuận phải in ra họ tên theo CCCD, **số giấy phép hành nghề**, chuyên khoa, nơi công tác. Ở thời
điểm ngay sau bước hồ sơ, chưa có gì trong số đó được xác minh — chuyên gia sẽ đang ký một văn bản
chứa dữ liệu tự khai. Ngoài đời cũng không ai ký thoả thuận với bác sĩ trước khi kiểm tra chứng chỉ.

Mô hình **đề nghị → chấp nhận** giải quyết cả ba vấn đề cùng lúc:

1. Văn bản in ra **toàn dữ liệu đã được duyệt**, không phải lời tự khai.
2. Admin **không phải thao tác hai lần** — duyệt xong là phát hành đề nghị luôn; việc nâng lên
   `CONTRACTED` xảy ra **tự động** khi chuyên gia ký.
3. Chuyên gia không ký thì **không bị kẹt**: họ ở lại nhóm Cộng đồng và hoạt động bình thường.
   Không có ngõ cụt nào trong luồng.

### 3.4. Máy trạng thái

```
(NULL) ──chọn hình thức + admin duyệt──▶ PENDING_CONTRACT ──admin xếp xuống──▶ COMMUNITY
                                               │
                                               └──chuyên gia ký──▶ CONTRACTED ──admin hạ──▶ COMMUNITY
```

| Trạng thái | Huy hiệu | Vị trí danh bạ | Bước tiếp theo |
|---|---|---|---|
| `NULL` (chưa chọn hình thức) | — | chưa xuất hiện | `/expert/type` |
| `PENDING_CONTRACT`, chưa duyệt | — | chưa xuất hiện | `/expert/identity` → `/expert/credentials` |
| `PENDING_CONTRACT`, đã duyệt | huy hiệu cộng đồng | nhóm cộng đồng | `/expert/contract` |
| `CONTRACTED` | huy hiệu hợp tác | ưu tiên đầu | `/expert-calendar` nếu chưa có lịch |
| `COMMUNITY` | huy hiệu cộng đồng | nhóm cộng đồng | `COMPLETE` |

`PENDING_CONTRACT` do chính chuyên gia ghi khi chọn hình thức, nhưng nó **không cấp đặc quyền
nào** — không huy hiệu hợp tác, không ưu tiên sorting. Bất biến duy nhất cần bảo vệ là
`CONTRACTED`, và trạng thái đó chỉ được ghi bởi `POST /contract/accept`.

### 3.5. Bước điều hướng — `determineNextStep`

Sửa tại [`ExpertIdentityVerificationServiceImpl.java:491-497`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/service/impl/ExpertIdentityVerificationServiceImpl.java#L491-L497):

```java
if (verificationStatus == APPROVED) {
    if (expertType == PENDING_CONTRACT)                  return "CONTRACT";
    if (expertType == CONTRACTED && !hasAvailability)    return "AVAILABILITY";
    return "COMPLETE";
}
```

| `nextStep` | Route mobile | Route web |
|---|---|---|
| `EXPERT_TYPE` | `/expert/type` | bước trong `ExpertOnboardingPage` |
| `CONTRACT` | `/expert/contract` | bước trong `ExpertOnboardingPage` |
| `AVAILABILITY` | `/expert-calendar` *(đã có sẵn)* | `AvailabilityCalendarPage` *(đã có sẵn)* |

> 🚨 **Bẫy mobile.** [`app_router.dart:284-288`](../05_Development/CareBridgeMobileApp/lib/core/routes/app_router.dart#L284-L288)
> có whitelist cứng `isExpertOnboardingRoute`, và guard bên dưới đá mọi expert chưa duyệt về
> `/expert-onboarding`. Thêm route mới mà quên đưa vào whitelist = **redirect loop vô hạn**,
> app treo ở màn loading.

---

## 4. 📝 Màn đăng ký — bước 2: chọn hình thức

Hai thẻ lựa chọn, mỗi thẻ nói thẳng **nghĩa vụ** đi kèm chứ không chỉ mô tả quyền lợi:

> **Chuyên gia Hệ thống**
> Được ưu tiên giới thiệu tới người dùng cần tư vấn.
> *Yêu cầu: ký Thoả thuận hợp tác và duy trì tối thiểu 10 ca rảnh mỗi tuần.*

> **Chuyên gia Y tế Cộng đồng**
> Tư vấn hỗ trợ cộng đồng theo khả năng của bạn.
> *Không cần cam kết lịch cố định, phản hồi khi bạn rảnh.*

Viết nghĩa vụ ngay trên thẻ chọn sẽ lọc bớt người chọn nhóm hợp tác chỉ vì thấy huy hiệu đẹp hơn —
và giảm số ca admin phải xếp xuống nhóm cộng đồng.

Lựa chọn ghi thẳng vào `expert_type` (`PENDING_CONTRACT` hoặc `COMMUNITY`) kèm một audit row
`EXPERT_TYPE_REQUESTED`. Ghi thẳng là an toàn vì cả hai giá trị đó đều **không cấp đặc quyền**;
đổi lại, `determineNextStep` chỉ cần đọc một cột thay vì phải tra bảng lịch sử.

Đã ký rồi thì không tự đổi lại được (`EXPERT-TYPE-LOCKED`) — phải qua admin.

---

## 5. 🛡️ Màn xét duyệt của admin

Giữ nguyên `ExpertVerificationQueuePage`, thêm hai thứ.

**Ở danh sách hàng đợi:** chip cạnh tên cho biết đơn xin nhóm nào, và bộ lọc hiện có (`statusFilter`)
thêm tuỳ chọn lọc theo nhóm.

**Ở khung chi tiết:** phần kiểm tra CCCD / khuôn mặt / giấy phép / đối chiếu Sở Y tế **giữ nguyên cho
cả hai nhóm** — tiêu chuẩn chuyên môn không được phép khác nhau. Chỉ thêm một khối chỉ hiện khi đơn
xin hợp tác:

```
┌─ Xét duyệt hợp tác ────────────────────────────┐
│ Nguyện vọng: Chuyên gia Hệ thống               │
│ Thời hạn thoả thuận: [12 tháng ▾]              │
│ Cam kết lịch tối thiểu: [10] ca/tuần           │
└────────────────────────────────────────────────┘
```

Nút bấm khác nhau:

| Đơn xin hợp tác | Đơn xin cộng đồng |
|---|---|
| **Duyệt & phát hành thoả thuận** → `PENDING_CONTRACT` | **Duyệt** → `COMMUNITY` |
| **Duyệt, xếp nhóm cộng đồng** → `COMMUNITY` | — |
| **Từ chối** | **Từ chối** |

Nút thứ hai ở cột trái là thứ không được thiếu: bác sĩ hợp lệ nhưng hệ thống chưa cần thêm người khoa
đó — vẫn nhận vào, chỉ là ở nhóm cộng đồng. Không có nút này thì admin buộc phải từ chối cả một bác sĩ
tốt.

---

## 6. ✍️ Trang ký thoả thuận

### 6.1. Nội dung văn bản

Đây là **hợp đồng dịch vụ / hợp tác chuyên môn** (quan hệ dân sự), **không phải hợp đồng lao động**.
Chuyên gia không chịu sự quản lý điều hành, không có thời giờ làm việc cố định. Gọi nhầm thành HĐLĐ
sẽ kéo theo nghĩa vụ BHXH và là chỗ hội đồng bắt lỗi ngay.

**Căn cứ pháp lý (ghi ở đầu văn bản):**

| Văn bản | Vai trò |
|---|---|
| **Bộ luật Dân sự 2015** | Nền tảng giao kết, hiệu lực, chấm dứt hợp đồng dịch vụ |
| **Luật Giao dịch điện tử 2023** (20/2023/QH15) | Căn cứ để văn bản ký điện tử có giá trị pháp lý như bản giấy — **quan trọng nhất** |
| **Luật Khám bệnh, chữa bệnh 2023** (15/2023/QH15) | Điều kiện hành nghề, phạm vi hoạt động chuyên môn, khám chữa bệnh từ xa |
| **Nghị định 13/2023/NĐ-CP** | Bảo vệ dữ liệu cá nhân — dữ liệu sức khoẻ là dữ liệu **nhạy cảm** |
| **Thông tư 33/2025/TT-BYT** | Thời hạn lưu trữ hồ sơ tư vấn |
| **Luật Thuế thu nhập cá nhân** | Khấu trừ thuế trên thù lao chi trả |

**11 điều:**

1. Căn cứ pháp lý
2. Thông tin các bên
3. Đối tượng và phạm vi hợp tác — **giới hạn đúng theo phạm vi hoạt động chuyên môn ghi trên chứng chỉ**
4. Quyền và nghĩa vụ của Chuyên gia — bao gồm cam kết duy trì lịch trực
5. Quyền và nghĩa vụ của Hệ thống
6. Thù lao, phương thức thanh toán, khấu trừ thuế
7. Bảo mật thông tin và bảo vệ dữ liệu cá nhân
8. **Trách nhiệm chuyên môn và giới hạn trách nhiệm**
9. Thời hạn, gia hạn, chấm dứt
10. Giải quyết tranh chấp
11. Hiệu lực và xác nhận điện tử

> 💰 **Điều 6 không ghi cứng con số.** Viết *"mức thù lao theo biểu phí do Hệ thống công bố tại từng
> thời điểm"*. Hệ thống hiện chưa triển khai thu/chi; khi bật tính năng thanh toán, bản đã ký vẫn còn
> hiệu lực và **không phải mời toàn bộ chuyên gia ký lại**.

> ⚖️ **Điều 8 là điều quyết định.** Phải viết thẳng: AI chỉ cung cấp thông tin tham khảo và gợi ý;
> mọi quyết định chuyên môn thuộc về Chuyên gia; Chuyên gia chịu trách nhiệm về nội dung tư vấn của
> mình; Hệ thống đóng vai trò nền tảng kết nối và lưu trữ. Đây là ranh giới AI–bác sĩ ở §4 tài liệu
> đặc tả, được viết thành ràng buộc pháp lý — và là câu trả lời mạnh nhất cho câu hỏi *"nếu AI tư vấn
> sai thì ai chịu trách nhiệm"*.

> 📌 Số điều khoản cụ thể của từng luật cần **tra lại trên Thư viện Pháp luật** trước khi đưa vào bản
> chính. Dẫn sai số điều còn tệ hơn không dẫn.

### 6.2. Nội dung trang — mỗi thành phần phục vụ một mục đích pháp lý

1. **Toàn văn render ngay trên trang**, cuộn được — không phải link tải file rồi tick đồng ý.
   Nút đồng ý **disable cho tới khi cuộn hết**: đây là chi tiết làm nên giá trị của click-wrap,
   chứng minh người ký đã có cơ hội đọc.
2. **Checkbox tường minh, không tick sẵn.** Tick sẵn thì bản chấp nhận mất giá trị.
3. **Ô gõ lại họ tên** đúng như trên CCCD đã duyệt — bằng chứng về chủ ý, thay cho chữ ký.
4. **Hiện rõ phiên bản điều khoản** ngay trong khung nhìn, không giấu trong đoạn văn.
5. Ký xong: nút tải PDF, và bản đó **luôn xem lại được** ở trang hồ sơ chuyên gia.

> 📌 **Cách gọi tên trên UI.** Đặt là **"Thoả thuận hợp tác chuyên gia"**. Không dùng chữ
> *"chữ ký điện tử"* — cơ chế ở đây là **click-wrap agreement**, hoàn toàn hợp lệ, nhưng gọi tên kia
> sẽ dẫn tới câu hỏi về chứng thư số và CA.

### 6.3. Endpoint

```java
GET  /api/v1/expert/contract/offer     // chỉ trả khi expert_type = PENDING_CONTRACT
     → { termsVersion, termsHash, renderedHtml, effectiveDate, termMonths }

POST /api/v1/expert/contract/accept
     body { termsVersion, termsHash, acceptedFullName }
     → ①sinh PDF  ②đẩy R2  ③audit ACCEPTED  ④expert_type = CONTRACTED
```

Hai quy tắc không được bỏ:

- **`termsHash` client gửi lên phải khớp bản hiện hành trên server.** Không kiểm thì có kịch bản:
  chuyên gia mở trang → admin sửa điều khoản → chuyên gia tick đồng ý, và hệ thống ghi nhận họ đã
  đồng ý một bản họ chưa từng thấy.
- **IP và User-Agent server tự đọc từ request, không nhận từ client.** Client gửi lên được thì
  bằng chứng vô giá trị.

Template đặt tại `src/main/resources/contracts/expert-contract-v1.0.md`, version là hằng số trong code.
PDF sinh server-side nên cả mobile lẫn web chỉ việc render và POST — logic pháp lý không bị nhân đôi
ở hai client.

### 6.4. Ràng buộc bất đối xứng ở bước duyệt

Admin **được phép xếp xuống**: bác sĩ xin hợp tác nhưng hệ thống chưa cần thêm người khoa đó → duyệt
thành `COMMUNITY`. Chuyện bình thường.

Admin **không được phép đặt thẳng `CONTRACTED`**. Trạng thái đó chỉ đạt được qua hành vi ký của chính
chuyên gia. Chốt cứng trong `approveExpert`, cùng chỗ với các gate identity/credential đã có tại
[`ExpertProfileServiceImpl.java:404-440`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/service/impl/ExpertProfileServiceImpl.java#L404-L440):

```java
if (grantedType == CONTRACTED)
    throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-CONTRACT-REQUIRED",
        "Trạng thái hợp tác chỉ được xác lập khi chuyên gia chấp nhận Thoả thuận");
```

Khi **hạ** từ `CONTRACTED` xuống `COMMUNITY`, trong cùng transaction ghi audit row `TERMINATED`.
Bản ký cũ **giữ nguyên** làm lịch sử, không xoá.

---

## 7. 🔒 Hai lỗ phải vá trước khi lưu thoả thuận

### 7.1. `CONTENT_ADMIN` / `MODERATOR` đang đọc được mọi file

[`FileAccessPolicyImpl.java:36-40`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/policy/FileAccessPolicyImpl.java#L36-L40)
cho `ADMIN_ROLES = {SYSTEM_ADMIN, MODERATOR, CONTENT_ADMIN}` bypass toàn bộ kiểm tra. Với bằng cấp
y khoa thì hợp lý (moderator cần soi). Với thoả thuận hợp tác thì không.

→ Thêm nhánh riêng: `purpose == EXPERT_CONTRACT` ⇒ chỉ owner + `SYSTEM_ADMIN`.

### 7.2. Chuyên gia đang tự xoá được thoả thuận của mình

[`FileDeletePolicyImpl`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/policy/FileDeletePolicyImpl.java)
là *owner-only*, và điều kiện bất biến duy nhất là "đang gắn với health record". Bản thoả thuận do
expert đứng tên owner → expert gọi `deleteFile` là bay mất bằng chứng đồng thuận.

```java
if (file.getPurpose() == FilePurpose.EXPERT_CONTRACT)
    throw new BusinessException(HttpStatus.CONFLICT, "FILE-409",
        "Thoả thuận hợp tác phải được lưu trữ theo quy định và không thể xoá");
```

> Đây chính là chỗ yêu cầu **lưu tối thiểu 10 năm** của Thông tư 33/2025/TT-BYT được thực thi bằng code.

---

## 8. 📋 Ảnh hưởng lên danh bạ chuyên gia

### 8.1. Priority Sorting

Query hiện tại tại [`ExpertProfileRepository.java:113`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/repository/ExpertProfileRepository.java#L113)
không có mệnh đề ưu tiên nào:

```sql
-- Trước
ORDER BY u.rating_avg DESC NULLS LAST, u.user_id ASC

-- Sau
ORDER BY CASE WHEN u.expert_type = 'CONTRACTED' THEN 0 ELSE 1 END,
         u.rating_avg DESC NULLS LAST, u.user_id ASC
```

Sửa ở **cả `@Query` lẫn `countQuery`**.

### 8.2. Huy hiệu trên mobile

[`expert_directory_screen.dart:688`](../05_Development/CareBridgeMobileApp/lib/features/directChat/screens/expert_directory_screen.dart#L688)
đang vẽ `Icons.verified_rounded` với điều kiện `if (isApproved)` — tức **mọi** expert đã duyệt đều có
huy hiệu hợp tác. Đổi thành nhánh hai chiều theo `expertType`, dùng đúng cặp icon/màu/tooltip ở §1.2.

Header ở [dòng 319](../05_Development/CareBridgeMobileApp/lib/features/directChat/screens/expert_directory_screen.dart#L319)
ghi cứng *"100% Đã xác thực chứng chỉ y khoa"* — câu này đúng về kiểm duyệt chứng chỉ nhưng khiến
người đọc hiểu nhầm toàn bộ đều là chuyên gia hợp tác. Cần viết lại.

### 8.3. Dữ liệu demo

[`DevDataSeeder.java:405-414`](../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/dev/DevDataSeeder.java#L405-L414)
seed 6 expert giống nhau. Chia hẳn hai nhóm (ví dụ 3 `CONTRACTED` có lịch rảnh / 3 `COMMUNITY`) để
nhìn thấy được cả hai huy hiệu và priority sorting thực sự hoạt động.

---

## 9. ✅ Trạng thái triển khai

**Backend**

- [x] `V7__add_expert_type.sql` — 1 cột nullable + CHECK 3 giá trị + backfill về `COMMUNITY`
- [x] `expert/experttype/ExpertType.java` (theo pattern `truststatus/`, `verificationstatus/`)
- [x] `ExpertProfile` — thêm field, không khai `nullable`, thêm `isContracted()`
- [x] `ExpertProfileMapper` — sửa **cả** `toResponse` lẫn `toDetailResponse`
- [x] `ExpertProfileResponse` + `ExpertProfileDetailResponse` — thêm `expertType`
- [x] `searchDirectory` — priority sorting ở cả `@Query` và `countQuery`
- [x] `PATCH /profiles/me/expert-type` — chuyên gia chọn hình thức, ghi audit `REQUESTED`
- [x] `approveExpert` — nhận nhóm được duyệt, chặn đặt thẳng `CONTRACTED`
- [x] `PATCH /profiles/{id}/expert-type` — `SYSTEM_ADMIN` hạ nhóm, ghi audit `TERMINATED`
- [x] `ExpertContractAcceptance` entity → `audit_events`, `@SQLRestriction`
- [x] Template `expert-contract-v1.0.md` — 6 căn cứ + 11 điều
- [x] `GET /contract/offer` + `POST /contract/accept` + sinh PDF
- [x] `FilePurpose.EXPERT_CONTRACT`
- [x] `FileAccessPolicyImpl` — chặn `MODERATOR` / `CONTENT_ADMIN`
- [x] `FileDeletePolicyImpl` — chặn xoá thoả thuận
- [x] `determineNextStep` — 2 bước mới
- [x] `DevDataSeeder` — chia hai nhóm

**Mobile**

- [x] `ExpertOnboardingStep` — thêm `expertType`, `contract`, `availability`
- [x] Route `/expert/type`, `/expert/contract` + **đưa vào whitelist `isExpertOnboardingRoute`**
- [x] `ExpertDirectoryItem` — thêm `expertType`
- [x] `expert_directory_screen.dart` — hai huy hiệu theo §1.2 + sửa header

**Web**

- [x] `expertApi.ts` — type `expertType`
- [x] `ExpertOnboardingPage.tsx:32-36` — thêm bước vào mảng `steps`
- [x] `ExpertVerificationQueuePage` — chip phân nhóm, bộ lọc, khối "Xét duyệt hợp tác", 3 nút

---

### 9.1. Ghi chú triển khai

- **Font PDF.** `src/main/resources/fonts/NotoSans-{Regular,Bold}.ttf` (SIL OFL, ~1.1MB) được nhúng
  vào PDF qua `PDType0Font`. 14 font chuẩn của PDF dùng WinAnsiEncoding nên **không có dấu tiếng
  Việt** — không thể bỏ font này đi.
- **Tương thích ngược mobile.** `ExpertOnboardingState.fromJson` chỉ chèn bước chọn hình thức khi
  payload thực sự có key `expertType`. Jackson để mặc định `ALWAYS` nên backend mới luôn gửi key
  (kể cả giá trị null), còn backend cũ thì không — nhờ vậy app cập nhật trước backend vẫn chạy
  đúng thứ tự bước cũ thay vì kẹt ở màn chọn hình thức.
- **Migration đã áp dụng.** V7 đã chạy trên Supabase (PostgreSQL 17.6); 9 hồ sơ EXPERT hiện có được
  backfill về `COMMUNITY`.

---

## 10. ❓ Vấn đề còn mở

**Chưa triển khai thu/chi.** Package `payment/` hiện chỉ có `.gitkeep` — không có entity, service hay
controller nào. Điều 6 của thoả thuận đã dự liệu sẵn cơ chế thù lao nhưng hệ thống chưa thực thi.
Nằm ngoài phạm vi tài liệu này.

**Slot chưa bị tiêu thụ khi đặt lịch — chặn tính năng điều phối tự động.** `accept()` của
`ConsultationRequestServiceImpl` không tạo row `consultation_bookings` và không đặt slot sang `BUSY`;
`AvailabilityStatus.BUSY` không xuất hiện ở bất kỳ đâu trong `src/main/java`. Hệ quả: hai người dùng
đặt được **cùng một ca** với cùng một chuyên gia, và nhánh "giữ slot đã có người đặt" trong
`replaceAvailability` chưa bao giờ chạy. Phải vá trước khi làm thuật toán điều phối tuyến 1 / tuyến 2,
vì nếu không thì hệ thống sẽ liên tục gợi ý những ca đã kín.
