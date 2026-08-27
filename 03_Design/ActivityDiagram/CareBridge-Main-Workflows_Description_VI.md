# Mô tả chi tiết CareBridge Main Workflows Activity Diagram

## 1. Thông tin tài liệu

- Nguồn mô tả: `CareBridge-Main-Workflows.drawio`
- Thư mục: `03_Design/ActivityDiagram/`
- Loại sơ đồ: Activity Diagram / Workflow Diagram
- Số trang workflow: 14
- Mục đích: mô tả các luồng hoạt động nghiệp vụ chính của nền tảng CareBridge từ onboarding, hồ sơ sức khỏe, cộng đồng, tư vấn, AI, khẩn cấp, gia đình, an toàn, bài tập, thiết bị đeo, đối tác, nội dung đến audit/bảo mật.

## 2. Danh sách workflow trong sơ đồ

1. Onboarding, Account & Privacy Setup
2. Mother, Baby & Health Record Workflow
3. Community Q&A, Expert Answer & Moderation Workflow
4. Expert Verification, Booking & Consultation Lifecycle
5. Gemini AI Triage & Safe Action Workflow
6. TrackAsia Emergency Map & Nearby Care Workflow
7. Family Sync, Permission & Care Task Workflow
8. Smart Activity Monitoring & Safety Support Workflow
9. Expert App, TrackAsia Nearby Support & Realtime Contact
10. Pregnancy Exercise & MediaPipe Posture Workflow
11. Smartwatch Data Sync & Health Trend Workflow
12. Partner Clinic & Sponsored Content Governance Workflow
13. Verified Content, Checklist & Gemini RAG Knowledge Workflow
14. Audit, Security Incident & Access Review Workflow

## 3. Quy ước đọc sơ đồ

- Mỗi trang mô tả một workflow độc lập nhưng có liên hệ nghiệp vụ với các workflow khác.
- Các cột/swimlane đại diện cho tác nhân hoặc hệ thống chịu trách nhiệm xử lý một phần của luồng.
- Hình chữ nhật là hoạt động/xử lý.
- Hình thoi là điểm quyết định hoặc rẽ nhánh.
- Mũi tên thể hiện thứ tự xử lý và điều kiện chuyển trạng thái.
- Nhãn trên mũi tên như `Yes`, `No`, `Allowed`, `Denied`, `Pass`, `Fail` mô tả kết quả của bước quyết định.

## 4. Workflow 01: Onboarding, Account & Privacy Setup

### 4.1. Mục tiêu

Workflow này mô tả quá trình người dùng bắt đầu sử dụng CareBridge, đăng ký/đăng nhập, xác thực OTP, chấp nhận điều khoản/quyền riêng tư, chọn vai trò và được chuyển đến dashboard phù hợp.

### 4.2. Tác nhân và hệ thống tham gia

- Guest / User
- Mobile App / Web Portal
- CareBridge Backend
- Family / Expert

### 4.3. Luồng chính

1. Người dùng mở ứng dụng CareBridge hoặc web portal.
2. Hệ thống hiển thị lựa chọn đăng ký hoặc đăng nhập.
3. Người dùng đăng ký bằng email/số điện thoại hoặc đăng nhập.
4. Hệ thống xử lý xác thực OTP hoặc luồng đặt lại mật khẩu khi cần.
5. Nếu OTP hợp lệ, người dùng tiếp tục chấp nhận điều khoản sử dụng, chính sách quyền riêng tư và tuyên bố miễn trừ an toàn y tế.
6. Người dùng cấu hình quyền riêng tư và tùy chọn thông báo.
7. Người dùng chọn vai trò: Mother, Family Member hoặc Expert.
8. Backend tạo tài khoản, phiên đăng nhập và hồ sơ vai trò.
9. Tùy vai trò:
   - Mother nhập giai đoạn thai kỳ, hậu sản hoặc chăm sóc bé.
   - Family Member chấp nhận hoặc từ chối lời mời vào care group.
   - Expert gửi bằng cấp/chứng chỉ và chờ xác minh.
10. Người dùng được chuyển đến dashboard tương ứng với vai trò và trạng thái tài khoản.

### 4.4. Nhánh quyết định và ngoại lệ

- Nếu OTP hết hạn hoặc thất bại, hệ thống cho phép gửi lại OTP hoặc thử lại luồng đặt lại mật khẩu.
- Nếu người dùng từ chối điều khoản/quyền riêng tư, onboarding dừng và tài khoản không được tạo.
- Nếu lời mời family member không hợp lệ, hết hạn hoặc bị từ chối, hệ thống hiển thị đường dẫn thử lại hoặc hỗ trợ.
- Expert sau khi gửi hồ sơ không được kích hoạt đầy đủ ngay mà cần chờ xác minh.

### 4.5. Ý nghĩa nghiệp vụ

Workflow này là cổng vào của toàn hệ thống. Nó bảo đảm người dùng được xác thực, đồng ý với điều khoản và được gán đúng vai trò trước khi truy cập chức năng nhạy cảm như hồ sơ sức khỏe, nhóm chăm sóc, tư vấn hoặc quản trị chuyên gia.

## 5. Workflow 02: Mother, Baby & Health Record Workflow

### 5.1. Mục tiêu

Workflow này mô tả cách Mother quản lý hành trình thai kỳ/hậu sản, hồ sơ bé, log sức khỏe, tệp hồ sơ và việc chia sẻ dữ liệu cho chuyên gia hoặc thành viên gia đình theo consent.

### 5.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App
- CareBridge Backend
- Firebase Storage

### 5.3. Luồng chính

1. Mother tạo hoặc cập nhật hành trình chăm sóc của mẹ.
2. Hệ thống tạo dashboard hành trình, nhắc nhở và checklist.
3. Mother thêm, cập nhật hoặc xóa chỉ số sức khỏe của mẹ.
4. Mother thêm, cập nhật hoặc xóa log hậu sản.
5. Mother tạo, chuyển đổi, cập nhật hoặc lưu trữ hồ sơ bé.
6. Mother thêm, cập nhật hoặc xóa log/mốc phát triển của bé.
7. Mother tải lên, xem hoặc xóa tệp hồ sơ sức khỏe.
8. Firebase Storage lưu/truy xuất tệp và metadata kèm nhãn nguồn.
9. Hệ thống tạo bản tóm tắt sức khỏe được chọn.
10. Nếu Mother muốn chia sẻ dữ liệu, hệ thống kiểm tra consent, phạm vi chia sẻ, thời hạn và người nhận.
11. Mobile App hiển thị timeline, biểu đồ, bản tóm tắt và trạng thái quyền.

### 5.4. Nhánh quyết định và ngoại lệ

- Nếu có consent hợp lệ, dữ liệu được chia sẻ theo đúng phạm vi cho expert/family.
- Nếu consent bị từ chối hoặc hết hạn, dữ liệu sức khỏe được chia sẻ sẽ bị ẩn khỏi expert/family.
- Nếu Mother không chọn chia sẻ, dữ liệu chỉ dùng trong phạm vi cá nhân.

### 5.5. Ý nghĩa nghiệp vụ

Workflow này là lõi dữ liệu sức khỏe của CareBridge. Nó nhấn mạnh việc quản lý hồ sơ mẹ/bé, lưu trữ tệp có metadata, và chỉ chia sẻ dữ liệu khi có consent rõ ràng.

## 6. Workflow 03: Community Q&A, Expert Answer & Moderation Workflow

### 6.1. Mục tiêu

Workflow này mô tả vòng đời nội dung cộng đồng: người dùng xem feed, tạo câu hỏi/bài viết/câu trả lời, hệ thống kiểm tra rủi ro, moderator xử lý nội dung nhạy cảm và expert trả lời câu hỏi phù hợp.

### 6.2. Tác nhân và hệ thống tham gia

- User
- CareBridge System
- Moderator
- Verified Expert

### 6.3. Luồng chính

1. User xem feed, tìm kiếm câu hỏi/chủ đề hoặc theo dõi chủ đề.
2. User xem chi tiết câu hỏi, bookmark hoặc thích câu trả lời.
3. User tạo câu hỏi/bài viết/câu trả lời và chọn public hoặc anonymous.
4. Hệ thống kiểm tra spam, từ khóa không an toàn và nguy cơ red-flag.
5. Nếu nội dung an toàn, câu hỏi được đưa lên feed hoặc vào hàng đợi chuyên gia.
6. Verified Expert trả lời câu hỏi được định tuyến, kèm nhãn expert.
7. Firebase Cloud Messaging gửi trạng thái câu trả lời hoặc kiểm duyệt cho người dùng.
8. User có thể sửa/xóa bài viết hoặc câu trả lời của chính mình nếu được phép.

### 6.4. Nhánh quyết định và ngoại lệ

- Nếu nội dung có rủi ro, hệ thống đưa vào luồng moderator hoặc chuyển sang Gemini triage/emergency support trước khi đề xuất tư vấn trả phí.
- Moderator xem nội dung/báo cáo đang chờ xử lý.
- Moderator có thể approve, hide, reject, warn hoặc suspend.
- Nếu nội dung cần expert, hệ thống định tuyến vào expert queue.

### 6.5. Ý nghĩa nghiệp vụ

Workflow này bảo vệ chất lượng cộng đồng bằng kiểm duyệt và phân loại rủi ro. Các nội dung có dấu hiệu nguy hiểm được ưu tiên xử lý an toàn trước, thay vì để người dùng đi thẳng vào luồng tư vấn hoặc cộng đồng.

## 7. Workflow 04: Expert Verification, Booking & Consultation Lifecycle

### 7.1. Mục tiêu

Workflow này mô tả vòng đời từ xác minh chuyên gia đến đặt lịch tư vấn, thanh toán, chia sẻ tóm tắt sức khỏe, tư vấn realtime, hoàn tất phiên, tranh chấp/hoàn tiền và tính hoa hồng.

### 7.2. Tác nhân và hệ thống tham gia

- Expert
- Admin
- Mother
- Payment Gateway + Backend

### 7.3. Luồng chính

1. Expert gửi hoặc cập nhật hồ sơ, bằng cấp/chứng chỉ và lịch khả dụng.
2. Admin xác minh, từ chối, yêu cầu bổ sung thông tin, gia hạn, thu hồi hoặc đình chỉ.
3. Mother xem hồ sơ/danh bạ expert đã xác minh.
4. Mother đặt lịch, đổi lịch hoặc hủy tư vấn.
5. Expert chấp nhận yêu cầu tư vấn.
6. Payment Gateway + Backend xử lý thanh toán và giữ trạng thái booking.
7. Khi thanh toán được xác nhận, hệ thống kiểm tra consent trước khi chia sẻ tóm tắt sức khỏe.
8. Mother và Expert tư vấn qua Firebase chat hoặc ZegoCloud voice/video.
9. Expert viết consultation summary và cập nhật trạng thái phiên.
10. Hệ thống hoàn tất kết quả: complete, no-show, dispute hoặc refund state.
11. Mother xem summary và đánh giá expert.
12. Nếu có vấn đề, Mother gửi yêu cầu tranh chấp/hoàn tiền.
13. Admin xử lý tranh chấp và phê duyệt/từ chối hoàn tiền.
14. Expert xem doanh thu, hoa hồng và contribution points.

### 7.4. Nhánh quyết định và ngoại lệ

- Nếu expert từ chối hoặc đề xuất thời gian khác, hệ thống thông báo cho Mother.
- Nếu thanh toán thất bại, booking không được xác nhận.
- Nếu consent bị thu hồi hoặc bị từ chối, dữ liệu chia sẻ bị ẩn.
- Hoa hồng chỉ được tính sau kết quả cuối cùng có thể thanh toán; phiên bị hoàn tiền hoặc tranh chấp không được tính.

### 7.5. Ý nghĩa nghiệp vụ

Workflow này liên kết chuyên gia, lịch hẹn, thanh toán, consent, realtime communication và kế toán hoa hồng. Đây là một trong các workflow có nhiều ràng buộc trạng thái nhất của CareBridge.

## 8. Workflow 05: Gemini AI Triage & Safe Action Workflow

### 8.1. Mục tiêu

Workflow này mô tả cách CareBridge sử dụng Gemini AI để hỗ trợ intake, trả lời bằng RAG và đề xuất hành động an toàn theo phân loại Green/Yellow/Red.

### 8.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App
- Gemini AI Service
- CareBridge Rules / Next Action

### 8.3. Luồng chính

1. Mother nhập triệu chứng, mối lo hoặc câu hỏi FAQ.
2. Mother có thể chọn ngữ cảnh từ hồ sơ sức khỏe hoặc smartwatch.
3. Mobile App kiểm tra consent và phạm vi dữ liệu được chọn.
4. Hệ thống gửi intake prompt và ngữ cảnh RAG đã được phê duyệt đến Gemini.
5. Gemini trả về intake có cấu trúc và câu trả lời có hỗ trợ RAG.
6. CareBridge áp dụng red-flag rules và safety rules.
7. Hệ thống phân loại kết quả thành Green, Yellow hoặc Red.
8. Với Green, ứng dụng hiển thị disclaimer và hướng dẫn tự chăm sóc/theo dõi.
9. Với Yellow, ứng dụng gợi ý tìm expert hoặc đặt lịch cho vấn đề không khẩn cấp.
10. Với Red, ứng dụng mở emergency support trước.

### 8.4. Nhánh quyết định và ngoại lệ

- Nếu intake chưa đủ thông tin, hệ thống hỏi câu hỏi bổ sung.
- Nếu consent bị từ chối, người dùng có thể tiếp tục chỉ với triệu chứng hoặc hủy chia sẻ ngữ cảnh.
- Nếu Gemini không khả dụng, hệ thống hiển thị hướng dẫn an toàn fallback.

### 8.5. Ý nghĩa nghiệp vụ

Gemini đóng vai trò hỗ trợ, không thay thế quyết định an toàn của CareBridge. Luồng này đặt safety rules sau kết quả AI để bảo đảm tình huống red-flag được ưu tiên xử lý khẩn cấp.

## 9. Workflow 06: TrackAsia Emergency Map & Nearby Care Workflow

### 9.1. Mục tiêu

Workflow này mô tả luồng hỗ trợ khẩn cấp dựa trên bản đồ TrackAsia, bao gồm xin quyền vị trí, tìm bệnh viện/phòng khám/đối tác/chuyên gia gần nhất, gọi nhanh và gửi cảnh báo cho gia đình.

### 9.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App
- TrackAsia Map Service
- Family / Firebase Cloud Messaging

### 9.3. Luồng chính

1. Mother mở emergency support từ dashboard, Gemini hoặc safety alert.
2. Mobile App yêu cầu quyền truy cập vị trí.
3. Nếu vị trí được cho phép, ứng dụng tìm bệnh viện, phòng khám, partner và expert gần nhất.
4. TrackAsia trả về khoảng cách, tuyến đường và ETA.
5. Mobile App hiển thị tuyến đường nếu có, quick call và lựa chọn hành động an toàn.
6. Mother có thể gọi cơ sở y tế, liên hệ khẩn cấp hoặc expert gần đó.
7. Firebase gửi cảnh báo khẩn cấp cho gia đình nếu tính năng này được bật.

### 9.4. Nhánh quyết định và ngoại lệ

- Nếu không có quyền vị trí, ứng dụng cho phép nhập vị trí thủ công, gọi nhanh và hiển thị hướng dẫn khẩn cấp mặc định.
- Trong emergency mode, hệ thống không hiển thị quảng cáo hoặc sponsored content trước khi người dùng thực hiện hành động an toàn.

### 9.5. Ý nghĩa nghiệp vụ

Workflow này ưu tiên an toàn hơn nội dung thương mại. Quyền vị trí giúp tăng độ chính xác, nhưng hệ thống vẫn phải có phương án thủ công khi người dùng từ chối hoặc không thể cấp quyền.

## 10. Workflow 07: Family Sync, Permission & Care Task Workflow

### 10.1. Mục tiêu

Workflow này mô tả cách Mother tạo care group, mời thành viên gia đình, cấp quyền chia sẻ dữ liệu, giao nhiệm vụ chăm sóc và cập nhật trạng thái thành viên/nhiệm vụ.

### 10.2. Tác nhân và hệ thống tham gia

- Mother
- Family Member
- CareBridge Backend
- Firebase Cloud Messaging

### 10.3. Luồng chính

1. Mother tạo care group.
2. Mother mời Family Member.
3. Firebase gửi thông báo lời mời.
4. Family Member chấp nhận hoặc từ chối lời mời.
5. Nếu chấp nhận, Mother cấu hình phạm vi quyền, mục đích và thời hạn.
6. Backend lưu consent, liên kết gia đình và quyền.
7. Family Member chỉ xem dữ liệu được cấp quyền.
8. Mother giao nhiệm vụ chăm sóc hoặc nhắc nhở chung.
9. Firebase gửi thông báo task/reminder/emergency alert cho gia đình.
10. Family Member xem chi tiết nhiệm vụ và cập nhật trạng thái.
11. Mother có thể cập nhật/hủy nhiệm vụ, thu hồi lời mời hoặc xóa thành viên.
12. Family Member có thể rời group.
13. Backend cập nhật quyền và trạng thái thành viên.

### 10.4. Nhánh quyết định và ngoại lệ

- Nếu lời mời bị từ chối, hệ thống kết thúc luồng tham gia nhóm cho thành viên đó.
- Nếu thành viên rời group, quyền và trạng thái thành viên phải được cập nhật.
- Mọi truy cập dữ liệu của Family Member phải dựa trên permission scope đang còn hiệu lực.

### 10.5. Ý nghĩa nghiệp vụ

Workflow này hỗ trợ chăm sóc cộng tác nhưng vẫn đặt quyền riêng tư ở trung tâm. Gia đình chỉ được xem dữ liệu đúng phạm vi Mother cho phép.

## 11. Workflow 08: Smart Activity Monitoring & Safety Support Workflow

### 11.1. Mục tiêu

Workflow này mô tả chức năng theo dõi an toàn bằng cảm biến thiết bị, phát hiện nghi ngờ té/ngã hoặc va chạm, hỏi xác nhận người dùng và gửi cảnh báo khẩn cấp khi cần.

### 11.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App / On-device IMU
- CareBridge Backend + Firebase Cloud Messaging
- Family Member

### 11.3. Luồng chính

1. Mother cấu hình liên hệ khẩn cấp, countdown và consent cho sensor/location.
2. Hệ thống kiểm tra cấu hình và lưu safety config.
3. Nếu cấu hình đầy đủ, Mother bật fall detection.
4. Mobile App kiểm tra quyền cảm biến.
5. Khi được cho phép, ứng dụng đọc accelerometer/gyroscope trên thiết bị.
6. Ứng dụng phát hiện metadata của sự kiện nghi ngờ té/ngã hoặc va chạm.
7. Ứng dụng hiển thị full-screen safety check với countdown.
8. Nếu Mother phản hồi "I am OK", hệ thống ghi nhận trạng thái an toàn và lý do false-positive nếu có.
9. Nếu Mother cần trợ giúp hoặc không phản hồi, hệ thống gửi emergency alert với dữ liệu tối thiểu và vị trí được phép.
10. Firebase gửi push/in-app alert và trạng thái retry.
11. Family Member xem cảnh báo và liên hệ Mother/caregiver.
12. Mother có thể mở TrackAsia emergency support khi cần.
13. Hệ thống lưu lịch sử safety event và trạng thái gửi thông báo.

### 11.4. Nhánh quyết định và ngoại lệ

- Nếu thiếu liên hệ/cấu hình, hệ thống yêu cầu hoàn thiện trước khi bật giám sát.
- Nếu quyền cảm biến bị từ chối, monitoring không thể bắt đầu.
- Nếu quyền vị trí bị từ chối, alert được gửi không kèm vị trí.
- Nếu gửi thông báo thất bại, Firebase thực hiện retry hoặc trả trạng thái thất bại.

### 11.5. Ý nghĩa nghiệp vụ

Workflow này xử lý tình huống nhạy cảm về an toàn cá nhân. Thiết kế cần giảm false-positive, bảo vệ quyền riêng tư vị trí và vẫn gửi cảnh báo tối thiểu khi người dùng không phản hồi.

## 12. Workflow 09: Expert App, TrackAsia Nearby Support & Realtime Contact

### 12.1. Mục tiêu

Workflow này mô tả cách expert quản lý trạng thái sẵn sàng, chia sẻ vị trí có kiểm soát, nhận yêu cầu hỗ trợ gần đó và liên hệ realtime với Mother qua chat/call/booking.

### 12.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App
- Expert App / Expert
- TrackAsia + ZegoCloud + Firebase + Backend

### 12.3. Luồng chính

1. Expert cập nhật trạng thái khả dụng cho tư vấn.
2. Expert quản lý location visibility gồm phạm vi, bán kính và thời lượng.
3. Expert opt-in chia sẻ vị trí.
4. Hệ thống cập nhật lớp nearby expert.
5. Mother tìm nearby support/experts.
6. Mobile App yêu cầu khoảng cách, tuyến đường và ETA.
7. TrackAsia tính kết quả gần nhất, khoảng cách, ETA và tuyến đường.
8. Ứng dụng hiển thị nearby experts trên bản đồ/danh sách.
9. Mother chọn nearby expert.
10. Mother liên hệ qua Firebase chat, ZegoCloud call hoặc booking.
11. Hệ thống tạo consultation/support request.
12. Expert xem nearby support requests.
13. Expert chấp nhận, từ chối hoặc đề xuất thời gian khác.
14. Nếu remote contact được chấp nhận, hệ thống tạo ZegoCloud voice/video room hoặc Firebase chat thread.
15. Nếu nearby support được chấp nhận và consent vị trí hợp lệ, hệ thống mở TrackAsia route đến vị trí hỗ trợ đã được đồng ý.
16. Hai bên tư vấn qua chat/voice/video.
17. Expert viết summary và cập nhật trạng thái phiên.
18. Firebase gửi thông báo trạng thái/tóm tắt.
19. Mother nhận trạng thái, tham gia phiên hoặc xem summary.

### 12.4. Nhánh quyết định và ngoại lệ

- Nếu Expert từ chối hoặc đề xuất thời gian khác, Mother được thông báo.
- Nếu consent chia sẻ vị trí không hợp lệ, hệ thống ẩn vị trí và chỉ cung cấp remote contact.
- Route/ETA chỉ được hiển thị khi có consent phù hợp.

### 12.5. Ý nghĩa nghiệp vụ

Workflow này mở rộng tư vấn từ đặt lịch thông thường sang hỗ trợ theo vị trí. Vì có dữ liệu vị trí của expert và Mother, consent theo phạm vi/thời lượng là điều kiện bắt buộc.

## 13. Workflow 10: Pregnancy Exercise & MediaPipe Posture Workflow

### 13.1. Mục tiêu

Workflow này mô tả trải nghiệm bài tập thai kỳ, kiểm tra an toàn trước khi tập, phân tích tư thế bằng MediaPipe trên thiết bị và lưu lịch sử phiên tập.

### 13.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App
- MediaPipe Pose Analysis
- Content Admin / Admin

### 13.3. Luồng chính

1. Mother xem danh sách bài tập thai kỳ.
2. Mother mở chi tiết bài tập và hướng dẫn.
3. Ứng dụng hiển thị safety disclaimer và checklist chống chỉ định.
4. Mother hoàn tất pre-exercise safety check.
5. Nếu đạt, Mother bắt đầu exercise session.
6. Mother có thể bật camera để phân tích tư thế.
7. Nếu được phép, MediaPipe trích xuất pose landmarks trên thiết bị.
8. Ứng dụng áp dụng posture rules và safety guardrails.
9. Ứng dụng hiển thị realtime posture feedback.
10. Mother tạm dừng, tiếp tục hoặc hoàn tất phiên tập.
11. Hệ thống lưu kết quả và lịch sử bài tập.
12. Content Admin/Admin quản lý bài tập thai kỳ.
13. Admin quản lý cấu hình phân tích posture.

### 13.4. Nhánh quyết định và ngoại lệ

- Nếu pre-exercise safety check thất bại, không cho bắt đầu và gợi ý hành động an toàn hơn.
- Nếu camera bị từ chối, người dùng vẫn có thể tiếp tục không có phân tích tư thế.
- Nếu camera visibility kém, hệ thống yêu cầu chỉnh vị trí, thử lại hoặc tiếp tục không phân tích.
- Phân tích posture chỉ áp dụng khi cấu hình hỗ trợ.

### 13.5. Ý nghĩa nghiệp vụ

Workflow này kết hợp nội dung đã quản trị với phân tích tại thiết bị. Điểm quan trọng là không ép người dùng chia sẻ camera, đồng thời luôn ưu tiên kiểm tra an toàn trước khi tập.

## 14. Workflow 11: Smartwatch Data Sync & Health Trend Workflow

### 14.1. Mục tiêu

Workflow này mô tả việc kết nối smartwatch/thiết bị đeo hoặc nhập dữ liệu thủ công, lưu dữ liệu có nhãn nguồn, phát hiện dữ liệu thiếu/lỗi/xung đột và dùng dữ liệu cho trend, Gemini hoặc expert summary.

### 14.2. Tác nhân và hệ thống tham gia

- Mother
- Mobile App
- Smartwatch / Wearable Device
- Backend / Firebase Storage

### 14.3. Luồng chính

1. Mother mở tính năng device integration.
2. Ứng dụng giải thích consent, loại dữ liệu, tần suất đồng bộ và giới hạn.
3. Mother chọn kết nối smartwatch hoặc nhập dữ liệu thủ công.
4. Ứng dụng yêu cầu các metric được hỗ trợ.
5. Thiết bị trả về metric khả dụng và trạng thái đồng bộ.
6. Backend/Firebase Storage lưu metric kèm nhãn nguồn và timestamp.
7. Hệ thống phát hiện dữ liệu thiếu, stale hoặc xung đột.
8. Mother xem device trend.
9. Mother chọn metric để đưa vào Gemini intake hoặc expert summary.
10. Mother có thể ngắt kết nối thiết bị hoặc xóa dữ liệu đã nhập.

### 14.4. Nhánh quyết định và ngoại lệ

- Nếu đồng bộ thất bại, hệ thống cho phép retry hoặc hiển thị trạng thái đồng bộ gần nhất.
- Dữ liệu nhập tay cần được phân biệt với dữ liệu từ thiết bị.
- Dữ liệu từ thiết bị cần có nhãn nguồn và timestamp để hỗ trợ truy vết.

### 14.5. Ý nghĩa nghiệp vụ

Workflow này giúp làm giàu ngữ cảnh sức khỏe nhưng không mặc định xem dữ liệu thiết bị là tuyệt đối chính xác. Nhãn nguồn, thời điểm và trạng thái đồng bộ là bắt buộc để diễn giải đúng.

## 15. Workflow 12: Partner Clinic & Sponsored Content Governance Workflow

### 15.1. Mục tiêu

Workflow này mô tả vòng đời quản trị đối tác, phòng khám/dịch vụ và sponsored content/campaign từ lúc gửi đến phê duyệt, xuất bản, yêu cầu sửa đổi hoặc gỡ bỏ.

### 15.2. Tác nhân và hệ thống tham gia

- Partner Representative
- Partner Portal
- Admin
- CareBridge System

### 15.3. Luồng chính

1. Partner Representative gửi hoặc cập nhật hồ sơ đối tác.
2. Partner gửi/cập nhật/rút clinic hoặc service listing.
3. Partner gửi sponsored content hoặc campaign.
4. Partner Portal gửi submission vào review queue.
5. Admin xem xét compliance của profile, service và campaign.
6. Admin quyết định approve, reject, request revision hoặc suspend.
7. Nếu được approve, hệ thống xuất bản service/content kèm sponsor label.
8. Partner xem referral/campaign performance summary.

### 15.4. Nhánh quyết định và ngoại lệ

- Nếu bị reject hoặc cần revision, hệ thống trả trạng thái/yêu cầu sửa cho partner.
- Partner có thể resubmit sau khi chỉnh sửa.
- Nếu bị suspend/remove, hệ thống gỡ nội dung đối tác không an toàn hoặc không tuân thủ.

### 15.5. Ý nghĩa nghiệp vụ

Workflow này bảo đảm nội dung thương mại và đối tác không xuất hiện như nội dung y tế trung lập. Sponsor label và review compliance là điểm then chốt.

## 16. Workflow 13: Verified Content, Checklist & Gemini RAG Knowledge Workflow

### 16.1. Mục tiêu

Workflow này mô tả vòng đời nội dung đã xác minh, từ tạo bài viết/FAQ/checklist/bài tập, review, publish, index vào Gemini RAG, đến report và unpublish.

### 16.2. Tác nhân và hệ thống tham gia

- User
- Content Admin
- Admin
- CareBridge System / Gemini Knowledge Base

### 16.3. Luồng chính

1. Content Admin tạo article, FAQ, checklist hoặc exercise content.
2. Content Admin cập nhật category, version và metadata.
3. Admin review/approve phiên bản khi cần.
4. Nếu được approve, hệ thống publish verified content.
5. Hệ thống index nội dung đã duyệt vào Gemini RAG.
6. User tìm kiếm và xem verified content.
7. User report nội dung outdated hoặc unsafe.
8. Admin/CareBridge review nội dung bị report trước khi quyết định unpublish.
9. Nếu nội dung outdated/unsafe, hệ thống unpublish.
10. Nội dung đã unpublish được de-index khỏi Gemini RAG.

### 16.4. Nhánh quyết định và ngoại lệ

- Nếu nội dung không được approve, nó quay lại draft/revision.
- Nếu có vấn đề sau khi publish, nội dung cần được review trước khi gỡ.
- Nội dung không còn publish không được tiếp tục làm nguồn cho RAG.

### 16.5. Ý nghĩa nghiệp vụ

Workflow này kiểm soát chất lượng nguồn tri thức. Gemini RAG chỉ nên sử dụng nội dung đã xác minh và còn hiệu lực để giảm rủi ro trả lời sai hoặc lỗi thời.

## 17. Workflow 14: Audit, Security Incident & Access Review Workflow

### 17.1. Mục tiêu

Workflow này mô tả cách CareBridge ghi nhận audit log, bảo toàn bằng chứng, hỗ trợ admin rà soát sự kiện bảo mật/truy cập đáng ngờ và ghi nhận kết quả xử lý.

### 17.2. Tác nhân và hệ thống tham gia

- Admin
- CareBridge Backend
- Audit / Security Log
- Affected User / Staff

### 17.3. Luồng chính

1. Backend ghi nhận sự kiện admin, consent, expert access, payment/refund và safety events.
2. Audit / Security Log bảo toàn bằng chứng bất biến và lịch sử truy cập.
3. Admin mở audit logs.
4. Admin lọc theo người dùng, loại sự kiện, tính năng hoặc khoảng thời gian.
5. Admin rà soát security event hoặc suspicious access.
6. Nếu incident được xác nhận, Admin điều tra incident và dữ liệu/hành động bị ảnh hưởng.
7. Khi chính sách yêu cầu, hệ thống/thành viên vận hành thông báo cho user/staff bị ảnh hưởng.
8. Admin ghi nhận kết quả và corrective action.

### 17.4. Nhánh quyết định và ngoại lệ

- Nếu không xác nhận incident, Admin đóng sự kiện với trạng thái reviewed/no incident.
- Operator không được sửa/xóa audit logs.
- Safety logs chỉ lưu metadata, tránh lưu dữ liệu nhạy cảm không cần thiết.

### 17.5. Ý nghĩa nghiệp vụ

Workflow này là nền tảng truy vết và tuân thủ. Với hệ thống có dữ liệu sức khỏe, thanh toán, consent và an toàn, audit log cần bất biến, có khả năng lọc/điều tra và không được chỉnh sửa bởi operator.

## 18. Liên kết giữa các workflow

Các workflow không đứng riêng lẻ mà tạo thành chuỗi nghiệp vụ liên tục:

- Onboarding quyết định vai trò, quyền truy cập ban đầu và dashboard của người dùng.
- Hồ sơ sức khỏe là nguồn dữ liệu cho Gemini triage, expert consultation và family sharing.
- Consent là điều kiện xuyên suốt cho chia sẻ dữ liệu với family, expert, Gemini context và location-based support.
- Community Q&A có thể chuyển sang Gemini triage hoặc expert consultation khi có dấu hiệu rủi ro.
- Expert booking liên kết với VNPay, Firebase/ZegoCloud, health summary, dispute/refund và commission.
- Emergency workflow liên kết với Gemini red-flag, safety monitoring, TrackAsia và family notification.
- Exercise, smartwatch và sensor workflows đều tạo dữ liệu sức khỏe/an toàn có thể xuất hiện trong summary hoặc audit.
- Partner/sponsored content và verified content đều cần governance để tránh nhầm lẫn giữa nội dung được xác minh và nội dung tài trợ.
- Audit/security workflow bao phủ các sự kiện nhạy cảm phát sinh từ hầu hết workflow còn lại.

## 19. Các điểm kiểm soát quan trọng

### 19.1. Consent và quyền riêng tư

CareBridge cần kiểm soát:

- Ai được truy cập dữ liệu.
- Dữ liệu nào được chia sẻ.
- Mục đích chia sẻ.
- Thời hạn hiệu lực.
- Cơ chế thu hồi.
- Lịch sử chia sẻ và truy cập.

### 19.2. An toàn y tế và red-flag

Các workflow liên quan triệu chứng, AI, cộng đồng, tư vấn, bài tập và khẩn cấp đều cần quy tắc an toàn. Với tình huống Red, hệ thống phải ưu tiên emergency support thay vì nội dung tư vấn thông thường hoặc nội dung thương mại.

### 19.3. Tính nhất quán trạng thái

Các trạng thái cần đồng bộ rõ ràng:

- Account status.
- Expert verification status.
- Booking/payment/session status.
- Consent status.
- Task/member status.
- Notification delivery status.
- Safety event status.
- Content approval/publish/index status.
- Incident review status.

### 19.4. Khả năng truy vết

Các thao tác nhạy cảm cần audit:

- Quản trị người dùng/vai trò.
- Truy cập hồ sơ sức khỏe.
- Consent grant/revoke/check.
- Expert access và consultation summary.
- Thanh toán, hoàn tiền, tranh chấp.
- Safety alert và false-positive.
- Nội dung bị report/unpublish.
- Security incident và corrective action.

## 20. Rủi ro cần lưu ý khi triển khai

1. **OTP/onboarding**: cần xử lý OTP hết hạn, retry limit, khóa tạm thời và khôi phục tài khoản.
2. **Consent hết hạn hoặc bị thu hồi**: dữ liệu đã chia sẻ phải bị ẩn ngay khỏi family/expert.
3. **Booking và payment race condition**: booking không được xác nhận nếu thanh toán thất bại hoặc trạng thái chưa chắc chắn.
4. **AI hallucination**: Gemini response phải bị giới hạn bằng RAG đã duyệt, disclaimer và red-flag rules.
5. **Emergency without location**: vẫn phải hỗ trợ gọi nhanh và hướng dẫn an toàn khi vị trí bị từ chối.
6. **False-positive safety alert**: cần lưu lý do, lịch sử và tránh spam cảnh báo cho gia đình.
7. **Thiết bị đeo sai lệch dữ liệu**: cần nhãn nguồn, timestamp, trạng thái stale/conflict.
8. **Sponsored content**: cần label rõ ràng và không được chen trước hành động an toàn trong emergency mode.
9. **RAG de-indexing**: nội dung unpublish phải được loại khỏi kho tri thức AI.
10. **Audit immutability**: operator không được sửa/xóa log; chỉ được thêm outcome/corrective action theo quyền.

## 21. Tóm tắt

`CareBridge-Main-Workflows.drawio` mô tả 14 workflow nghiệp vụ chính của CareBridge. Các workflow thể hiện một nền tảng chăm sóc mẹ và bé có nhiều lớp kiểm soát: xác thực, vai trò, consent, dữ liệu sức khỏe, tư vấn chuyên gia, AI/RAG, cảnh báo an toàn, bản đồ khẩn cấp, đồng bộ gia đình, thiết bị/cảm biến, nội dung đã xác minh, đối tác tài trợ và audit bảo mật.

Điểm nổi bật của sơ đồ là CareBridge không chỉ xử lý chức năng ứng dụng thông thường, mà còn thiết kế nhiều cơ chế bảo vệ người dùng trong bối cảnh dữ liệu nhạy cảm và quyết định liên quan đến sức khỏe/an toàn.
