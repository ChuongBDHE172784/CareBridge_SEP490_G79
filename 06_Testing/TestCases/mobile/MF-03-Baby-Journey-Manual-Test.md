# MF-03 Hành trình của bé — Kiểm thử thủ công trên ứng dụng di động

## Thông tin lần kiểm thử

| Trường | Giá trị |
|---|---|
| Bản dựng / mã commit | `[điền]` |
| Ngày kiểm thử | `[điền]` |
| Người kiểm thử | `[điền]` |
| Môi trường | `[local / staging]` |
| Thiết bị Android / phiên bản OS | `[điền]` |
| URL máy chủ API | `[điền]` |
| Kết quả tổng thể | `[ĐẠT / KHÔNG ĐẠT / BỊ CHẶN]` |
| Liên kết lỗi | `[điền]` |

## Phạm vi

Kiểm tra luồng ứng dụng di động chính thức của MF-03:

`Trang chủ → Hành trình → Bé → MotherJourneyScreen → BabyProfileDetailScreen`

Đường dẫn tạm thời `/baby-care-hub` không phải điểm bắt đầu của bộ kiểm thử này.

Các miền chức năng được kiểm tra:

- Nhật ký
- Tăng trưởng
- Cột mốc
- Tiêm chủng
- Chuyển đổi bé và duy trì đúng danh tính bé
- Phân quyền, khôi phục lỗi, khả năng tiếp cận và nội dung an toàn

## Điều kiện tiên quyết

1. Máy chủ API và cơ sở dữ liệu đang hoạt động.
2. Ứng dụng di động đã được cài trên thiết bị Android thật.
3. Lệnh `flutter devices` hiển thị thiết bị Android.
4. Nếu máy chủ API chạy trên máy tính, thực hiện:

   ```powershell
   adb reverse tcp:8080 tcp:8080
   ```

5. Có tài khoản Mẹ dùng để test và tài khoản này quản lý hai bé khác nhau.
6. Có một bé thuộc tài khoản khác để kiểm tra phân quyền.
7. Không ghi mật khẩu, access token, refresh token hoặc dữ liệu cá nhân vào file hay ảnh chụp màn hình.

## Dữ liệu kiểm thử

| Bí danh | Dữ liệu yêu cầu |
|---|---|
| `MOTHER_1` | Tài khoản Mẹ quản lý Bé A và Bé B |
| `BABY_A` | Tên và ID khác biệt; dữ liệu có sẵn là tùy chọn |
| `BABY_B` | Tên và ID khác biệt; là bé được chọn cho các ca tạo/cập nhật dữ liệu |
| `FOREIGN_BABY` | Bé thuộc tài khoản khác; lưu ID ở nơi riêng tư |
| `JOURNAL_MARKER` | `MF03-JOURNAL-<timestamp>` |
| `MILESTONE_MARKER` | Cột mốc/ngày test chưa tồn tại ở Bé B |
| `VACCINATION_MARKER` | Vắc-xin/ngày test chưa tồn tại ở Bé B |

## Quy tắc đánh giá kết quả

- **ĐẠT:** Quan sát được toàn bộ kết quả mong đợi và đã đính kèm bằng chứng.
- **KHÔNG ĐẠT:** Ứng dụng trả kết quả sai, bị dừng đột ngột, rò rỉ dữ liệu hoặc phát sinh thay đổi ngoài ý muốn.
- **BỊ CHẶN:** Không thể thực hiện do môi trường hoặc dữ liệu. Phải ghi rõ nguyên nhân, không được chuyển thành ĐẠT.
- Mọi trường hợp hiển thị hoặc thay đổi dữ liệu nhầm giữa các bé đều là lỗi chặn phát hành.

## Bảng tổng hợp

| ID | Kịch bản | Ưu tiên | Kết quả | Bằng chứng / Lỗi |
|---|---|---|---|---|
| MF03-MAN-001 | Mở Hành trình chính thức và kiểm tra bốn nhóm tương tác | P0 | `[ ]` | `[điền]` |
| MF03-MAN-002 | Chuyển Bé A → Bé B mà không còn dữ liệu cũ | P0 | `[ ]` | `[điền]` |
| MF03-MAN-003 | Thêm Nhật ký cho Bé B đang được chọn | P0 | `[ ]` | `[điền]` |
| MF03-MAN-004 | Thêm và mở chi tiết Cột mốc của Bé B | P0 | `[ ]` | `[điền]` |
| MF03-MAN-005 | Thêm và mở chi tiết Tiêm chủng của Bé B | P0 | `[ ]` | `[điền]` |
| MF03-MAN-006 | Từ chối truy cập tài nguyên của bé thuộc tài khoản khác | P0 | `[ ]` | `[điền]` |
| MF03-MAN-007 | Mở lịch sử và chi tiết Tăng trưởng | P1 | `[ ]` | `[điền]` |
| MF03-MAN-008 | Trạng thái trống của Tăng trưởng | P1 | `[ ]` | `[điền]` |
| MF03-MAN-009 | Hủy tạo Nhật ký | P1 | `[ ]` | `[điền]` |
| MF03-MAN-010 | Hủy tạo Cột mốc | P1 | `[ ]` | `[điền]` |
| MF03-MAN-011 | Hủy tạo bản ghi Tiêm chủng | P1 | `[ ]` | `[điền]` |
| MF03-MAN-012 | Duy trì Bé B trên mọi đường quay lại | P1 | `[ ]` | `[điền]` |
| MF03-MAN-013 | Chuyển bé nhanh và loại bỏ phản hồi cũ | P1 | `[ ]` | `[điền]` |
| MF03-MAN-014 | Lỗi API và thử lại | P1 | `[ ]` | `[điền]` |
| MF03-MAN-015 | Mất mạng và khôi phục kết nối | P1 | `[ ]` | `[điền]` |
| MF03-MAN-016 | Phiên đăng nhập hết hạn bảo vệ dữ liệu của bé | P1 | `[ ]` | `[điền]` |
| MF03-MAN-017 | Tài khoản Mẹ chưa có bé hiển thị trạng thái trống an toàn | P1 | `[ ]` | `[điền]` |
| MF03-MAN-018 | Khả năng tiếp cận và phóng to chữ | P1 | `[ ]` | `[điền]` |
| MF03-MAN-019 | Chạy nền/khôi phục và xoay màn hình | P2 | `[ ]` | `[điền]` |
| MF03-MAN-020 | Nội dung sức khỏe trung lập, không chẩn đoán | P2 | `[ ]` | `[điền]` |

## Chi tiết ca kiểm thử

### MF03-MAN-001 — Hành trình chính thức và bốn nhóm tương tác

**Ưu tiên:** P0  
**Điều kiện:** Đăng nhập bằng `MOTHER_1`; Bé A và Bé B đã tồn tại.

**Các bước:**

1. Mở ứng dụng từ Trang chủ.
2. Chọn `Hành trình` trên thanh điều hướng chính.
3. Chọn phần `Bé`.
4. Ghi lại tên bé đang được hiển thị.
5. Kiểm tra các tương tác Nhật ký, Tăng trưởng, Cột mốc và Tiêm chủng.

**Kết quả mong đợi:**

- Màn hình Hành trình của bé chính thức được mở, không cần đi qua hub tạm.
- Tên thật của bé đang hoạt động được hiển thị.
- Cả bốn nhóm tương tác đều nhìn thấy, đọc được và có thể nhấn.
- Không hiển thị danh tính bé giả hoặc bản ghi dựng sẵn.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-002 — Chuyển từ Bé A sang Bé B

**Ưu tiên:** P0

**Các bước:**

1. Bắt đầu tại Bé A trong Hành trình chính thức.
2. Mở bộ chọn bé.
3. Chọn Bé B.
4. Chờ toàn bộ thẻ và danh sách tải xong.
5. Đối chiếu tên hồ sơ và các bản ghi hiển thị với dữ liệu đã chuẩn bị của Bé B.

**Kết quả mong đợi:**

- Bé B trở thành bé đang hoạt động.
- Nhật ký, Tăng trưởng, Cột mốc và Tiêm chủng chỉ chứa dữ liệu của Bé B.
- Không còn dữ liệu Bé A sau khi tải hoàn tất.
- Sau mọi trạng thái tải/chớp màn hình, bé cuối cùng vẫn phải là Bé B.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-003 — Thêm Nhật ký cho Bé B

**Ưu tiên:** P0

**Các bước:**

1. Chọn Bé B.
2. Mở Nhật ký/tổng kết nhật ký trong ngày.
3. Thêm một bản ghi dùng `JOURNAL_MARKER` hoặc thời gian/giá trị có thể nhận biết duy nhất.
4. Lưu bản ghi.
5. Quay lại Hành trình của bé.
6. Mở lại Nhật ký và làm mới.
7. Chuyển sang Bé A và xác nhận bản ghi mới không xuất hiện.

**Kết quả mong đợi:**

- Bản ghi được lưu cho Bé B.
- Quay lại Hành trình vẫn giữ Bé B đang hoạt động.
- Bản ghi vẫn tồn tại sau khi tải lại.
- Bản ghi không xuất hiện ở Bé A.
- Nhấn nút nhiều lần không tạo bản ghi trùng.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-004 — Thêm và mở Cột mốc của Bé B

**Ưu tiên:** P0

**Các bước:**

1. Chọn Bé B.
2. Mở phần Cột mốc và chọn Thêm.
3. Nhập `MILESTONE_MARKER`, ngày đạt được và ghi chú tùy chọn.
4. Lưu.
5. Xác nhận danh sách được làm mới, sau đó mở chi tiết cột mốc vừa tạo.
6. Quay lại Hành trình và mở lại cột mốc.

**Kết quả mong đợi:**

- Cột mốc chỉ được tạo một lần cho Bé B.
- Danh sách được làm mới mà không đổi bé đang hoạt động.
- Chi tiết hiển thị đúng loại/ngày/ghi chú đã lưu và đúng ngữ cảnh Bé B.
- Không có tên bé, ngày, ghi chú hoặc hình ảnh cố định trong mã thay thế dữ liệu thật.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-005 — Thêm và mở Tiêm chủng của Bé B

**Ưu tiên:** P0

**Các bước:**

1. Chọn Bé B.
2. Mở Tiêm chủng và chọn Thêm.
3. Nhập `VACCINATION_MARKER` cùng dữ liệu test hợp lệ.
4. Lưu.
5. Xác nhận danh sách được làm mới, sau đó mở chi tiết bản ghi vừa tạo.
6. Quay lại Hành trình và mở lại bản ghi.

**Kết quả mong đợi:**

- Bản ghi tiêm chủng chỉ được tạo một lần cho Bé B.
- Chi tiết hiển thị bản ghi đã lưu thực tế và đúng ngữ cảnh Bé B.
- Khi quay lại, Bé B vẫn được chọn.
- Không hiển thị danh tính bé giả hoặc bản ghi tiêm chủng không liên quan.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-006 — Từ chối tài nguyên của bé thuộc tài khoản khác

**Ưu tiên:** P0  
**Điều kiện:** Có phương thức liên kết sâu/công cụ hỗ trợ kiểm thử đã được phê duyệt và `FOREIGN_BABY` thuộc tài khoản khác.

**Các bước:**

1. Đăng nhập bằng `MOTHER_1`.
2. Dùng liên kết sâu hoặc công cụ hỗ trợ kiểm thử đã phê duyệt để thử mở tài nguyên Nhật ký, Tăng trưởng, Cột mốc hoặc Tiêm chủng của `FOREIGN_BABY`.
3. Lặp lại với ít nhất một đường đọc và một đường thay đổi dữ liệu.
4. Kiểm tra UI và log API được phép xem, không sao chép token.

**Kết quả mong đợi:**

- Truy cập bị từ chối hoặc được biểu diễn là không tìm thấy.
- Không hiển thị tên, giá trị, ghi chú, số đo hoặc thông tin vắc-xin của bé ngoài phạm vi.
- Không tạo, sửa hoặc xóa bản ghi ngoài phạm vi.
- Ứng dụng vẫn sử dụng được và hiển thị lỗi trung lập.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-007 — Lịch sử và chi tiết Tăng trưởng

**Ưu tiên:** P1  
**Điều kiện:** Bé B có ít nhất một số đo tăng trưởng.

**Các bước:**

1. Chọn Bé B.
2. Mở lịch sử Tăng trưởng.
3. Đối chiếu giá trị trong danh sách với dữ liệu đã chuẩn bị.
4. Mở chi tiết một số đo.
5. Quay lại lịch sử rồi quay lại Hành trình.

**Kết quả mong đợi:**

- Lịch sử và chi tiết chỉ chứa số đo của Bé B.
- Chiều cao, cân nặng, ngày và đơn vị khớp dữ liệu lưu trữ.
- Điều hướng quay lại vẫn giữ Bé B.
- UI không đưa ra chẩn đoán từ số đo.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-008 — Trạng thái trống của Tăng trưởng

**Ưu tiên:** P1  
**Điều kiện:** Dùng một bé chưa có số đo tăng trưởng.

**Các bước:**

1. Mở lịch sử Tăng trưởng của bé.
2. Quan sát trạng thái trống.
3. Quay lại Hành trình.

**Kết quả mong đợi:**

- Trạng thái trống rõ ràng và trung lập.
- Không hiển thị số đo giả.
- Màn hình không crash và điều hướng quay lại hoạt động.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-009 — Hủy tạo Nhật ký

**Ưu tiên:** P1

**Các bước:**

1. Chọn Bé B và mở Nhật ký.
2. Bắt đầu thêm một bản ghi có thể nhận biết duy nhất.
3. Hủy hoặc quay lại mà không lưu.
4. Tải lại Nhật ký.

**Kết quả mong đợi:** Không có bản ghi mới; Bé B vẫn được chọn.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-010 — Hủy tạo Cột mốc

**Ưu tiên:** P1

**Các bước:**

1. Chọn Bé B và mở Thêm Cột mốc.
2. Nhập dữ liệu test.
3. Hủy hoặc quay lại mà không lưu.
4. Tải lại danh sách cột mốc.

**Kết quả mong đợi:** Không tạo cột mốc; Bé B vẫn được chọn.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-011 — Hủy tạo Tiêm chủng

**Ưu tiên:** P1

**Các bước:**

1. Chọn Bé B và mở Thêm Tiêm chủng.
2. Nhập dữ liệu test.
3. Hủy hoặc quay lại mà không lưu.
4. Tải lại danh sách tiêm chủng.

**Kết quả mong đợi:** Không tạo bản ghi tiêm chủng; Bé B vẫn được chọn.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-012 — Duy trì Bé B trên mọi đường quay lại

**Ưu tiên:** P1

**Các bước:**

1. Chọn Bé B.
2. Mở và quay lại từ Nhật ký.
3. Mở và quay lại từ Tăng trưởng.
4. Mở và quay lại từ chi tiết/thêm Cột mốc.
5. Mở và quay lại từ chi tiết/thêm Tiêm chủng.

**Kết quả mong đợi:** Bé B vẫn hoạt động sau mỗi lần quay lại; không đường nào âm thầm chuyển về Bé A.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-013 — Chuyển bé nhanh và phản hồi cũ

**Ưu tiên:** P1

**Các bước:**

1. Trên kết nối chậm hoặc đã giới hạn tốc độ, chuyển nhanh Bé A → Bé B → Bé A → Bé B.
2. Dừng tại Bé B và chờ mọi yêu cầu hoàn tất.
3. Kiểm tra toàn bộ thẻ và danh sách đang hiển thị.

**Kết quả mong đợi:** UI cuối cùng chỉ hiển thị Bé B; phản hồi đến muộn của Bé A không ghi đè hồ sơ, bản ghi hoặc trạng thái lỗi.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-014 — Lỗi API và thử lại

**Ưu tiên:** P1

**Các bước:**

1. Chọn Bé B.
2. Dừng máy chủ API hoặc dùng phương thức mô phỏng lỗi được phê duyệt.
3. Mở/làm mới một màn hình chức năng.
4. Quan sát trạng thái lỗi.
5. Khôi phục máy chủ API và chọn Thử lại hoặc mở lại màn hình.

**Kết quả mong đợi:**

- Hiển thị trạng thái lỗi/trống an toàn; không crash hoặc lộ stack trace.
- Không hiển thị dữ liệu cũ của bé khác.
- Sau khi dịch vụ được khôi phục, dữ liệu Bé B được tải lại.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-015 — Mất mạng và khôi phục kết nối

**Ưu tiên:** P1

**Các bước:**

1. Mở Hành trình của Bé B khi đang có mạng.
2. Tắt kết nối mạng.
3. Thử mở hoặc làm mới Nhật ký/Tăng trưởng.
4. Khôi phục kết nối.
5. Thử lại hoặc mở lại màn hình.

**Kết quả mong đợi:** Ứng dụng vẫn phản hồi, thông báo lỗi an toàn và tải lại dữ liệu Bé B sau khi kết nối lại, không lộ dữ liệu trong bộ nhớ đệm của Bé A.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-016 — Phiên đăng nhập hết hạn

**Ưu tiên:** P1  
**Điều kiện:** Có phương thức đã phê duyệt để làm hết hạn/thu hồi phiên test.

**Các bước:**

1. Mở Hành trình của Bé B.
2. Làm hết hạn hoặc thu hồi phiên hiện tại.
3. Làm mới màn hình được bảo vệ hoặc thử tạo/cập nhật dữ liệu.

**Kết quả mong đợi:** Không cho phép truy cập hay thay đổi dữ liệu; ứng dụng yêu cầu xác thực hoặc hiển thị lỗi phân quyền an toàn, không lộ dữ liệu nhạy cảm/ngoài phạm vi trong bộ nhớ đệm.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-017 — Tài khoản Mẹ chưa có bé

**Ưu tiên:** P1  
**Điều kiện:** Đăng nhập bằng tài khoản Mẹ chưa có hồ sơ bé.

**Các bước:**

1. Đi tới Hành trình → Bé.
2. Quan sát hướng dẫn và các hành động hiện có.

**Kết quả mong đợi:** Hiển thị trạng thái trống an toàn, có hướng dẫn; không hiển thị bé giả hoặc dữ liệu của người khác; ứng dụng không crash.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-018 — Khả năng tiếp cận và phóng to chữ

**Ưu tiên:** P1

**Các bước:**

1. Bật Android TalkBack.
2. Duyệt qua bộ chọn bé và bốn nhóm tương tác chăm sóc.
3. Xác nhận các thành phần điều khiển có nhãn rõ nghĩa và thứ tự duyệt hợp lý.
4. Đặt cỡ chữ hệ thống ở mức lớn nhất được hỗ trợ.
5. Lặp lại Hành trình chính thức và mở một màn hình chức năng.

**Kết quả mong đợi:**

- Danh tính bé và các hành động được đọc rõ nghĩa.
- Không có thành phần điều khiển quan trọng bị thiếu nhãn hoặc không thể truy cập.
- Chữ không che hành động bắt buộc hoặc chồng lấn đến mức không sử dụng được.
- Vùng nhấn và độ tương phản vẫn sử dụng được.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-019 — Chạy nền/khôi phục và xoay màn hình

**Ưu tiên:** P2

**Các bước:**

1. Chọn Bé B và mở một màn hình chức năng.
2. Đưa ứng dụng xuống nền ít nhất 30 giây rồi mở lại.
3. Nếu bản dựng hỗ trợ xoay màn hình, xoay rồi đưa về hướng ban đầu.
4. Quay lại Hành trình của bé.

**Kết quả mong đợi:** Không crash, không tạo thay đổi trùng và không tự chuyển về Bé A; trạng thái hiện tại được giữ hoặc làm mới an toàn.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF03-MAN-020 — Nội dung sức khỏe trung lập

**Ưu tiên:** P2

**Các bước:**

1. Kiểm tra nội dung hiển thị trong các thẻ, trạng thái trống và chi tiết của Tăng trưởng, Cột mốc và Tiêm chủng.
2. Ghi nhận mọi nội dung khẳng định chẩn đoán, kết luận chắc chắn hoặc cảnh báo không có đánh giá của chuyên gia.

**Kết quả mong đợi:** Nội dung chỉ mô tả quan sát và hành động; không chẩn đoán, không bảo đảm trạng thái phát triển và không đưa ra kết luận y khoa thiếu căn cứ.

**Kết quả thực tế / bằng chứng:** `[điền]`

## Ma trận truy vết tiêu chí chấp nhận

| Tiêu chí chấp nhận | Test thủ công | Bằng chứng tự động |
|---|---|---|
| Bé đang hoạt động thấy bốn nhóm tương tác đúng phạm vi | MF03-MAN-001, 002, 012 | `mf03_canonical_journey_test.dart` |
| Sau khi lưu thành công, dữ liệu được làm mới mà không mất bé đang chọn | MF03-MAN-003, 004, 005, 012 | Kiểm thử widget chính thức và kiểm thử hợp đồng |
| Luồng API-backed chính thức dùng danh tính bé thật | MF03-MAN-001–007, 013–016 | `integration_test/mf03_hub_e2e_test.dart` |
| Không phụ thuộc hub tương thích | MF03-MAN-001 | Tài liệu rà soát tái sử dụng màn hình và kiểm thử tích hợp đã chuyển đường dẫn |

## Xác nhận kết quả

| Vai trò | Họ tên | Quyết định | Ngày | Ghi chú |
|---|---|---|---|---|
| Người kiểm thử | `[điền]` | `[ĐẠT / KHÔNG ĐẠT / BỊ CHẶN]` | `[điền]` | `[điền]` |
| QA Lead | `[điền]` | `[PHÊ DUYỆT / TỪ CHỐI]` | `[điền]` | `[điền]` |
| Product / Tech | `[điền]` | `[PHÊ DUYỆT / TỪ CHỐI]` | `[điền]` | `[điền]` |

Yêu cầu vượt cổng chất lượng:

- P0: đạt 100%.
- P1: đạt tối thiểu 95% và không có lỗi bảo mật/toàn vẹn dữ liệu.
- Không còn lỗi nghiêm trọng chưa xử lý.
- Đính kèm bằng chứng API-backed mới trên thiết bị Android thật.
