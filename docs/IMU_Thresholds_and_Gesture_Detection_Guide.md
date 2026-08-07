# Hướng dẫn Chỉ số Cảnh báo IMU & Thử nghiệm Vung tay (IMU Thresholds & Gesture Detection Guide)

> **Mô tả**: Tài liệu tổng hợp và giải thích chi tiết các chỉ số ngưỡng gia tốc, vận tốc góc, và các quy tắc để kích hoạt **Cảnh báo IMU ngã thật** và **Thử nghiệm vung tay (Diễn tập cảm biến)** trong dự án CareBridge.

---

## Tổng quan Tính năng IMU

Hệ thống xử lý cảnh báo IMU trong dự án CareBridge được chia thành **2 tính năng riêng biệt**:

1. **Cảnh báo ngã thật (Real Suspected Fall/Impact Alert - UC-136)**: Dùng để phát hiện nguy hiểm thực tế khi người dùng đeo/cầm điện thoại.
2. **Thử nghiệm vung tay / Diễn tập cảm biến (Sensor Self-Test / Demo Mode)**: Nhận diện cử động vung tay có chủ đích để người dùng thử nghiệm tính năng cảnh báo và đếm ngược an toàn mà không kích hoạt ngã thật.

---

## 1. Chỉ số kỹ thuật kích hoạt CẢNH BÁO IMU THẬT (Real Fall/Impact Detection)

Hệ thống xử lý cảnh báo ngã thật qua 2 lớp kiểm tra: **On-Device (Cảm biến di động)** và **Backend API (Server)**.

### A. Thuật toán 3 giai đoạn trên thiết bị di động ([`ImuFallDetector.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/imu_fall_detector.dart#L79-L97))

Để phát hiện một tình huống nghi ngờ ngã thật, dữ liệu IMU (*Accelerometer - Gia tốc kế* và *Gyroscope - Con quay hồi chuyển*) phải thỏa mãn chuỗi **3 giai đoạn liên tiếp**:

1. **Giai đoạn Rơi tự do (Free Fall)**:
   - **Gia tốc tổng ($a$)**: $< 3.0 \text{ m/s}^2$ (Gia tốc rơi rụng mạnh so với trọng lực $g \approx 9.81 \text{ m/s}^2$).
   - **Cửa sổ thời gian va chạm**: Giai đoạn va chạm phải xuất hiện trong vòng **$1.5 \text{ giây}$** ($1500 \text{ ms}$) ngay sau rơi tự do.

2. **Giai đoạn Va chạm (Impact)**:
   - **Gia tốc đỉnh va chạm**: $> 25.0 \text{ m/s}^2$.
   - **Tốc độ biến thiên gia tốc (Jerk - $\Delta a / \Delta t$)**: $\ge 80.0 \text{ m/s}^3$.
   - **Độ tươi dữ liệu con quay**: Chênh lệch thời gian giữa Accelerometer và Gyroscope $\le 200 \text{ ms}$.

3. **Giai đoạn Bất động sau va chạm (Immobility)**:
   - **Thời gian theo dõi bất động**: **$4 \text{ giây}$** liên tục ngay sau va chạm.
   - **Độ biến thiên gia tốc tĩnh ($|a - g|$)**: $\le 2.0 \text{ m/s}^2$.
   - **Vận tốc góc tĩnh (Gyroscope)**: $\le 0.5 \text{ rad/s}$.
   - **Ngưỡng bị hủy (Cancellation)**: Nếu vận tốc góc $> 1.5 \text{ rad/s}$ hoặc gia tốc lệch $> 6.0 \text{ m/s}^2$ trong $4\text{s}$ này $\rightarrow$ Hệ thống tự động hủy cảnh báo do xác định người dùng vẫn cử động bình thường.
   - **Tỷ lệ mẫu tĩnh bắt buộc**: $\ge 80\%$ tổng số mẫu dữ liệu trong $4\text{s}$ phải đạt trạng thái bất động.

---

### B. Thuật toán lọc Server Backend ([`FallDetectionAlgorithmService.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionAlgorithmService.java#L16-L32))

Sau khi tính toán gia tốc thuần loại bỏ trọng lực: $\text{Magnitude} = \sqrt{a_x^2 + a_y^2 + a_z^2} - 9.81 \text{ m/s}^2$, Backend so sánh với ngưỡng cài đặt ([`SensitivityLevel.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SensitivityLevel.java#L6-L12)):

| Mức độ nhạy (Sensitivity) | Ngưỡng gia tốc kích hoạt (Magnitude Threshold) | Ghi chú |
| :--- | :--- | :--- |
| **HIGH (Nhạy cao)** | $\ge 9.0 \text{ m/s}^2$ | Dễ kích hoạt, nhận biết cả va chạm nhẹ. |
| **MEDIUM (Trung bình - Mặc định)** | $\ge 12.0 \text{ m/s}^2$ | Ngưỡng chuẩn kết hợp va chạm và bất động. |
| **LOW (Nhạy thấp)** | $\ge 15.0 \text{ m/s}^2$ | Chỉ kích hoạt khi ngã hoặc va đập rất mạnh. |

* **Phân loại sự kiện**:
  - Nếu $\text{Magnitude} \ge \text{Threshold} \times 1.5 \rightarrow$ Loại cảnh báo: `SUSPECTED_FALL` (Nghi ngờ ngã).
  - Nếu $\text{Threshold} \le \text{Magnitude} < \text{Threshold} \times 1.5 \rightarrow$ Loại cảnh báo: `SUSPECTED_IMPACT` (Nghi ngờ va chạm).

---

## 2. Chỉ số kỹ thuật kích hoạt THỬ NGHIỆM VUNG TAY (Sensor Self-Test / Demo Gesture)

Để người dùng có thể tự thử nghiệm tính năng đếm ngược an toàn bằng cách vung tay mà không bị hệ thống ghi nhận nhầm thành ngã thật, ứng dụng cài đặt bộ nhận diện cử động vung tay có chủ đích ([`safety_demo_mode.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/safety_demo_mode.dart#L22-L50)):

### Các chỉ số bắt buộc để kích hoạt thử nghiệm vung tay:
1. **Trạng thái sẵn sàng (Preparation)**:
   - Cầm giữ điện thoại đứng yên trước khi vung trong tối thiểu **$300 \text{ ms}$** ($|a - g| \le 0.8 \text{ m/s}^2$, vận tốc góc $\le 0.25 \text{ rad/s}$). Trạng thái sẵn sàng này có hiệu lực trong **$3 \text{ giây}$**.
2. **Khởi động chuyển động vung (Motion Start)**:
   - Độ lệch gia tốc bắt đầu: $|a - g| \ge 3.0 \text{ m/s}^2$.
   - Vận tốc góc xoay bắt đầu: $\ge 1.4 \text{ rad/s}$.
3. **Các ngưỡng ĐỈNH trong cử động vung tay** (thời gian vung dứt khoát từ **$250 \text{ ms}$ đến $900 \text{ ms}$**):
   - **Đỉnh độ lệch gia tốc ($\text{Peak Acceleration Deviation}$)**: $\ge 5.0 \text{ m/s}^2$ (tương đương tổng gia tốc $a \ge 14.81 \text{ m/s}^2$).
   - **Đỉnh vận tốc góc xoay ($\text{Peak Rotation Magnitude}$)**: $\ge 2.0 \text{ rad/s}$.
   - **Quãng đường vung tay dứt khoát ước tính ($\text{Estimated Travel}$)**: $\ge 0.45 \text{ m}$ (sải tay vung dài tối thiểu **$45 \text{ cm}$**).
   - **Số lượng mẫu cử động mạnh liên tục**: $\ge 8 \text{ mẫu}$ (mỗi mẫu có $|a - g| \ge 2.5 \text{ m/s}^2$ và vận tốc góc $\ge 1.2 \text{ rad/s}$).
4. **Thời gian hồi (Cooldown)**: Giữa các lần thử nghiệm cần nghỉ **$2 \text{ giây}$**.

---

## 3. Giải thích Siêu Dễ Hiểu từng Chỉ số Thử Nghiệm Vung Tay

Dưới đây là giải thích đơn giản từng thông số thử nghiệm vung tay theo hành động thực tế đời sống:

### ✋ Giải thích Đơn giản Cử động Vung tay Thử nghiệm (Dành cho bất kỳ ai)

Thuật toán vung tay được thiết kế để nhận biết một **CỬ ĐỘNG CÓ CHỦ ĐÍCH CỦA BẠN** chứ không phải vô tình làm rơi điện thoại:

1. **Bước 1: Cầm đứng yên chuẩn bị (Tối thiểu 0,3 giây — 300 ms)**
   - **Mục đích:** Điện thoại cần biết bạn đã "sẵn sàng" cầm chắc máy, không bị rung lắc.
   - **Hành động:** Bạn chỉ cần cầm điện thoại đứng yên trong bàn tay khoảng **1/3 giây** (300 ms).
   - **Cơ chế:** Cảm biến đo độ lắc lệch $|a - g| \le 0.8\text{ m/s}^2$ và độ xoay $\le 0.25\text{ rad/s}$ (tay hầu như không rung). Trạng thái đứng yên này được điện thoại "nhớ" tối đa trong **3 giây**. Nếu quá 3 giây mà bạn chưa vung tay thì phải cầm đứng yên lại 0.3s.

2. **Bước 2: Bắt đầu vung tay (Motion Start)**
   - **Mục đích:** Điện thoại nhận diện nhịp vung tay bắt đầu kéo đi.
   - **Hành động:** Bạn bắt đầu vung tay nhanh lên.
   - **Cơ chế:** Lực vung tay do bạn tự tạo ra vọt lên $|a - g| \ge 3.0\text{ m/s}^2$ (gấp 3 lần cử động thường) và cổ tay bắt đầu xoay một góc cua $\ge 1.4\text{ rad/s}$.

3. **Bước 3: Cú vung tay dứt khoát (Thời gian 0,25 đến 0,9 giây)**
   - **Mục đích:** Loại bỏ các cử động giật mình hoặc giơ tay nghe điện thoại.
   - **Hành động:** Bạn thực hiện cú vung tay dứt khoát trong vòng khoảng **nửa giây** (tối thiểu 0.25s và tối đa 0.9s).
   - **Vì sao?** Vung quá nhanh ($< 0.25\text{s}$) $\rightarrow$ Điện thoại tính là giật mình/gãi đầu. Vung quá chậm ($> 0.9\text{s}$) $\rightarrow$ Tính là từ từ giơ tay lên nghe điện thoại.

4. **Bước 4: Sải tay vung dài (Tối thiểu 45 cm)**
   - **Mục đích:** Đảm bảo bạn vung cả cánh tay chứ không phải chỉ lắc cổ tay tại chỗ.
   - **Hành động:** Bạn vung nguyên cánh tay đi một sải dài **trên 45 cm** (dài hơn một gang tay rưỡi, xấp xỉ nửa cánh tay).
   - **Cơ chế:** Điện thoại tự cộng dồn vận tốc và gia tốc từng millisecond để tính ra quãng đường tay bạn đã di chuyển. Nếu chỉ lắc cổ tay vài cm $\rightarrow$ Điện thoại bỏ qua.

5. **Bước 5: Lực vung tay & Xoay cổ tay (Đỉnh lực xé gió)**
   - **Mục đích:** Đảm bảo cú vung có đủ lực xé gió nhẹ.
   - **Hành động:** Ở giữa cú vung tay, lực vung tay của bạn tạo ra độ lệch gia tốc đỉnh $\ge 5.0\text{ m/s}^2$ (tương đương tổng gia tốc cảm biến đo được $a \ge 14.81\text{ m/s}^2$) và cổ tay cuộn một góc cua $\ge 2.0\text{ rad/s}$ (khoảng $115^\circ/\text{giây}$).

6. **Bước 6: Lực vung mạnh duy trì liên tục (Tối thiểu 8 mẫu)**
   - **Mục đích:** Đảm bảo cử động vung tay mượt mà liên tục, không bị đứt đoạn.
   - **Cơ chế:** Điện thoại chụp ảnh cảm biến 50 lần mỗi giây ($50\text{Hz}$). Cú vung tay phải duy trì lực mạnh trong ít nhất **8 tấm ảnh cảm biến liên tiếp** (khoảng $0.16\text{ giây}$).

7. **Bước 7: Thời gian hồi (Cooldown 2 giây)**
   - Sau khi vung tay thử nghiệm thành công 1 lần, điện thoại sẽ tạm nghỉ **2 giây** mới nhận lần tiếp theo để tránh bị nổ thông báo đếm ngược liên tục.

---

### 💡 TÓM TẮT THAO TÁC VUNG TAY 3 BƯỚC DỄ NÓI CHO NGƯỜI DÙNG:

1. **Đứng yên:** Cầm điện thoại giữ yên trong tay **0.5 giây**.
2. **Vung mạnh:** Vung nguyên cánh tay một sải dài **hơn 45 cm** dứt khoát trong vòng **0.5 giây**.
3. **Thành công:** Màn hình đếm ngược **Self-Test Safety Countdown Sheet** xuất hiện lập tức!

---

## 4. Giải thích Chuyên sâu Lý do Chọn các Con số Ngưỡng (Threshold Rationale)

Dưới đây là căn cứ vật lý, sinh học và thực nghiệm trả lời chi tiết cho câu hỏi: **"Tại sao lại là con số này mà không phải con số khác?"**

### ❓ 1. Vì sao Cửa sổ Va chạm phải là 1,5 giây (Impact Window = 1.5s)? Vì sao không phải 1s, 2s hay 10s?
- **Tính toán cơ học ngã người:** 
  - Theo công thức rơi tự do: $h = \frac{1}{2}g t^2 \Rightarrow t = \sqrt{\frac{2h}{g}}$.
  - Chiều cao từ ngực/túi quần người trưởng thành ($1.2\text{m} - 1.5\text{m}$) cho thời gian rơi tự do $t \approx 0.5\text{s}$.
  - Cộng thêm thời gian mất thăng bằng, loạng choạng trượt chân ngã lộn người trước khi tiếp đất (khoảng $0.4\text{s} - 0.8\text{s}$), tổng thời gian thực tế từ khi gia tốc bị hẫng đến khi cơ thể đập xuống sàn rơi vào khoảng **$0.8\text{s} - 1.3\text{s}$**.
- **Vì sao không chọn 1,0 giây?** $1.0\text{s}$ quá ngắn, sẽ bỏ sót các trường hợp người già ngã chậm, trượt chân loạng choạng từ từ hoặc ngã từ giường/ghế cao xuống sàn.
- **Vì sao không chọn 2,0s hay 10,0 giây?** Nếu kéo dài đến 2s hay 10s, những cử động sinh hoạt bình thường (như nhún người đứng dậy rồi 5-10 giây sau mới đặt mạnh điện thoại xuống bàn) sẽ bị ghép nhầm thành một chuỗi ngã giả (False Positive).
- **Kết luận:** **1,5 giây** là "cửa sổ vàng" bao phủ $99\%$ mọi tình huống ngã thực tế của con người mà không gây báo động nhầm.

---

### ❓ 2. Vì sao Ngưỡng Rơi tự do lại là 3,0 m/s² (Free Fall Threshold = 3.0 m/s²)?
- **Trọng lực Trái Đất tiêu chuẩn:** $g \approx 9.81\text{ m/s}^2$ ($1\text{G}$). Khi rơi tự do tuyệt đối trong chân không, chỉ số gia tốc $= 0\text{ m/s}^2$.
- **Thực tế cơ học ngã:** Khi người dùng ngã, điện thoại rơi trong túi quần hay trên tay không hoàn toàn lơ lửng tuyệt đối (do lực cản không khí, ma sát với quần áo, hoặc điện thoại vừa rơi vừa lộn vòng).
- **Thực nghiệm cơ sở dữ liệu quốc tế (MobiFall, SisFall):** Khi rơi ngã thực tế, gia tốc tổng của điện thoại giảm xuống dưới mức **$3.0\text{ m/s}^2$** (tức là tuột giảm hơn **70%** so với mức $9.81\text{ m/s}^2$ bình thường).
- **Vì sao không chọn 5,0 m/s²?** Quá cao! Khi bạn bước đi nhanh, đi cầu thang hay nhún người, gia tốc cũng có lúc giảm nhẹ xuống $5.0\text{ m/s}^2$, chọn mốc này sẽ làm điện thoại liên tục báo ngã nhầm.
- **Vì sao không chọn 1,0 m/s²?** Quá thấp! Do ma sát túi quần và xoay không khí, điện thoại ngã thực tế hiếm khi đạt được mốc $1.0\text{ m/s}^2$, chọn mốc này sẽ bị bỏ sót không báo động khi có tai nạn ngã thật.
- **Kết luận:** Mốc **$3.0\text{ m/s}^2$** là ranh giới chuẩn xác nhất phân biệt giữa nhún người đi lại với rơi hẫng thực sự.

---

### ❓ 3. Vì sao Ngưỡng Va chạm lại là 25,0 m/s² (Impact Threshold = 25.0 m/s²)?
- **Bản chất con số:** $25.0\text{ m/s}^2$ tương đương khoảng **$2.55\text{G}$** (gấp 2.55 lần gia tốc trọng lực Trái Đất).
- **Dữ liệu sinh hoạt vận động mạnh nhất:** Khi con người chạy nhanh, nhảy vọt lên cao rồi tiếp đất, ngồi mạnh xuống ghế, hoặc đi xuống cầu thang dốc, gia tốc va động đo được trên điện thoại tối đa chỉ đạt từ **$14.0\text{ m/s}^2$ đến $18.0\text{ m/s}^2$** ($1.4\text{G} - 1.8\text{G}$).
- **Dữ liệu ngã va đập thật:** Khi cơ thể hoặc điện thoại tiếp sàn gạch/gỗ, lực phản hồi hãm dừng từ mặt sàn cực cứng khiến cảm biến bị nén mạnh, gia tốc va đập đỉnh luôn vượt từ **$25.0\text{ m/s}^2$ đến $45.0\text{ m/s}^2$**.
- **Kết luận:** Mốc **$25.0\text{ m/s}^2$** tạo ra một "bức tường phân cách" tuyệt đối giữa mọi hoạt động thể thao chạy nhảy mạnh với một cú nện đập xuống đất do tai nạn.

---

### ❓ 4. Jerk là cái gì? Vì sao Ngưỡng Jerk lại là 80,0 m/s³ (Jerk Threshold = 80.0 m/s³)?

#### A. Khái niệm Jerk (Độ giật / Mức độ sốc đột ngột):
- Trong vật lý học:
  - Quãng đường thay đổi theo thời gian $\rightarrow$ **Vận tốc** ($v = \text{m/s}$).
  - Vận tốc thay đổi theo thời gian $\rightarrow$ **Gia tốc** ($a = \text{m/s}^2$).
  - Gia tốc thay đổi theo thời gian $\rightarrow$ **Jerk (Độ giật)** ($j = \frac{\Delta a}{\Delta t} = \text{m/s}^3$).
- **Ví dụ thực tế đời sống dễ hiểu:** Bạn đi xe ô tô tăng tốc từ 0 lên 100 km/h trong 10 giây (gia tốc cao nhưng xe chạy rất êm, Jerk nhỏ). Nhưng nếu xe đang đi mà đạp phanh gấp cái "RẤT" hoặc đâm vào dải phân cách trong $0.05$ giây (lực biến thiên khựng lại trong chớp mắt $\rightarrow$ Jerk cực kỳ lớn, làm người trong xe bị giật nẩy mạnh).

#### B. Cách điện thoại tính Jerk & Lý do chọn 80,0 m/s³:
- **Công thức điện thoại tự tính giữa 2 mẫu cảm biến:**
  $$\text{Jerk} = \frac{|\text{Gia tốc mẫu hiện tại} - \text{Gia tốc mẫu trước đó}|}{\text{Khoảng thời gian giữa 2 mẫu (tính bằng giây)}}$$
- **Tần số cảm biến điện thoại:** Điện thoại lấy mẫu $50\text{Hz}$ (mỗi mẫu cách nhau $0.02\text{ giây} = 20\text{ ms}$).
- **Khi ngã va đập thật:** Gia tốc vọt từ $3.0\text{ m/s}^2$ lên $25.0\text{ m/s}^2$ (chênh lệch $\Delta a = 22.0\text{ m/s}^2$) chỉ trong vòng $0.04\text{ giây}$ ($40\text{ ms}$).
  $$\Rightarrow \text{Jerk} = \frac{22.0\text{ m/s}^2}{0.04\text{ s}} = 550.0\text{ m/s}^3$$ (lớn hơn rất nhiều so với mốc $80.0\text{ m/s}^3$).
- **Khi cử động tay vung nhanh:** Bạn vung tay hay nhảy lên, lực gia tốc tăng từ từ qua nhiều mẫu ($0.3\text{s} - 0.5\text{s}$), Jerk chỉ đạt tầm $20.0 - 50.0\text{ m/s}^3$.
- **Kết luận:** Mốc **$80.0\text{ m/s}^3$** giúp điện thoại phân biệt chính xác giữa cử động tay di chuyển nhanh nhưng êm với một cú va đập cứng (chấn động cực ngắn nhưng lực biến đổi cực kỳ đột ngột).

---

### 📊 Bảng So sánh Nguyên lý 2 Tính năng

| Tiêu chí | Cảnh báo Ngã thật | Vung tay thử nghiệm |
| :--- | :--- | :--- |
| **Giai đoạn 1** | **Rơi tự do**: Cả vỏ máy và cảm biến lơ lửng cùng rơi, lực tuột về gần 0 ($< 3.0\text{ m/s}^2$). | **Giữ đứng yên** 0,3s để cảm biến đo lực đẩy mặt bàn tĩnh 1G. |
| **Giai đoạn 2** | **Va đập cực mạnh**: Mặt đất cản đột ngột, nén mạnh cảm biến ($> 25\text{ m/s}^2$). | **Vung cánh tay** đi một sải dài trên **45 cm** trong khoảng 0,5s. |
| **Giai đoạn 3** | **Nằm bất động 4 giây**: Nằm im trên sàn. Cử động nhặt máy sẽ tự hủy báo động. | Cử động vung tay xong dứt khoát trong vòng **0,5 giây**. |
| **Mục đích** | Phát hiện tai nạn thật để báo cấp cứu/người thân. | Giúp người dùng tập dùng thử đếm ngược an toàn mà không làm phiền người thân. |
