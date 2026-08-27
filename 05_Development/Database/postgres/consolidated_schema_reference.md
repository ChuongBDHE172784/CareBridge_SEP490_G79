# DRAFT — Thuyết minh mô hình hợp nhất cơ sở dữ liệu (Phase 2)

> **Trạng thái:** DRAFT/reference-only; chưa được kiểm chứng trên database triển khai, chưa được phê duyệt và không phải đặc tả migration/Flyway.

Tài liệu này mô tả mục đích và phương án nhóm các capability trong bản nháp hợp nhất. Đây không phải danh mục vật lý đầy đủ của database.

### Quy ước đếm và tên

- `carebridge_53.sql` hiện có **53 câu lệnh `CREATE TABLE`**, gồm **52 bảng ứng dụng** và **1 bảng metadata `flyway_schema_history`**.
- Danh sách dưới đây có **50 mục khái niệm được đánh số**; một mục có thể đại diện nhiều tên legacy/logical (ví dụ mục 33), hoặc được lưu trong một bảng vật lý dùng chung (ví dụ `mother_baseline_contexts` trong `audit_events`). Vì vậy không được cộng các mục này để suy ra số bảng vật lý.
- Một số tên trong phần mô tả là tên capability/legacy, không phải tên bảng trong snapshot. Các ánh xạ nổi bật: `baby_daily_logs` → `care_logs`; `health_record_files` không phải bảng vật lý và được thay bằng quan hệ 1:N qua `attachments.health_record_id`; `community_answers` → các hàng `community_content` có `content_type = 'ANSWER'` và `parent_content_id`, còn like/bookmark/mute → `community_interactions`; `archived_consultation_records`, các bảng consultation/chat/call → `archived_records`; các event SOS chi tiết → `safety_events`.
- `expert_reviews`, `refund_records`, `settlement_records` và `consultation_disputes` cũng chỉ là tên logical trong tài liệu này: snapshot không có `CREATE TABLE` tương ứng và không có discriminator/shape constraint đủ rõ để xác lập bảng vật lý đích. Allocation của bốn capability này còn **chưa giải quyết**; không được hiểu là đã được triển khai trong `archived_records` hoặc bảng khác.
- Khi có khác biệt, danh sách `CREATE TABLE` trong `carebridge_53.sql` là inventory vật lý của **chính snapshot nháp này**, không phải bằng chứng rằng schema đó đã được triển khai hoặc phê duyệt. Các migration Flyway canonical vẫn là nguồn triển khai.

---

## 1. PHÂN NHÓM 13 DOMAIN CHỨC NĂNG NGHIỆP VỤ

### Nhóm I: Định danh, Tài khoản & Xác thực (IAM & Auth — 5 bảng)
Bảo vệ định danh người dùng, kiểm soát phiên làm việc và bảo mật quyền riêng tư.

1. **`users`**
   * **Tác dụng:** Lưu tài khoản cốt lõi, thông tin đăng nhập, hồ sơ cá nhân và hồ sơ chuyên môn của chuyên gia y tế (Expert).
   * **Tại sao tồn tại:** Gom toàn bộ thông tin profile (`user_profiles`, `community_profiles`, `persons`, `expert_profiles` cũ) về một bảng duy nhất giúp loại bỏ hoàn toàn các liên kết JOIN phức tạp, tăng tốc độ xử lý xác thực và truy vấn hồ sơ.
2. **`care_subjects`**
   * **Tác dụng:** Quản lý đối tượng y tế nhận chăm sóc (Mẹ bầu, Em bé, hoặc người thân được ủy quyền).
   * **Tại sao tồn tại:** Thay thế bảng `baby_profiles`. Bằng cách chuẩn hóa thành `care_subjects`, hệ thống có thể quản lý đa dạng đối tượng chăm sóc và liên kết trực tiếp em bé như một thực thể người dùng ảo trong hệ thống.
3. **`auth_sessions`**
   * **Tác dụng:** Quản lý phiên đăng nhập thiết bị và trạng thái token (refresh token).
   * **Tại sao tồn tại:** Thay thế cho 3 bảng cũ `user_sessions`, `refresh_tokens`, và `token_blacklist`. Gom về một bảng giúp kiểm soát vòng đời đăng nhập nhất quán, dễ dàng thu hồi phiên (revoke) và ngăn chặn tấn công giả mạo token.
4. **`auth_challenges`**
   * **Tác dụng:** Lưu mã OTP xác thực số điện thoại và các token đặt lại mật khẩu.
   * **Tại sao tồn tại:** Thay thế cho `otp_verifications` và `password_reset_tokens`. Gom các thử thách bảo mật ngắn hạn vào đây để cấu hình thời hạn hết hạn (TTL) và giới hạn số lần thử tập trung.
5. **`data_permissions`**
   * **Tác dụng:** Quản lý quyền chia sẻ dữ liệu y tế dài hạn giữa các tài khoản người dùng độc lập.
   * **Tại sao tồn tại:** Hỗ trợ kiểm soát quyền chia sẻ và giảm nguy cơ truy cập trái phép khi người dùng chia sẻ hồ sơ bệnh án cho bên thứ ba. Mức độ đáp ứng PDPA hoặc nghĩa vụ pháp lý cụ thể cần được legal/DPO đánh giá và xác nhận riêng.

---

### Nhóm II: Hành trình Chăm sóc mẹ bầu (Maternal Care — 2 bảng)
Quản lý chu kỳ thai kỳ và theo dõi các mốc quan trọng của mẹ.

6. **`mother_journeys`**
   * **Tác dụng:** Ghi nhận chu kỳ thai sản hiện tại của mẹ bầu (giai đoạn thụ thai, thai kỳ, hoặc sau sinh).
   * **Tại sao tồn tại:** Là bảng tổng hợp trạng thái (aggregate state) của người mẹ làm trung tâm để các module khác (AI Triage, Bài tập yoga, Lịch khám) truy vấn giai đoạn tương ứng.
7. **`mother_baseline_contexts` (Dạng bảng audit_events)**
   * **Tác dụng:** Lưu thông tin khai báo nền ban đầu (Baseline) của mẹ (mục tiêu thai kỳ, thói quen sinh hoạt).
   * **Tại sao tồn tại:** Được lưu dạng bất biến trong bảng `audit_events` để làm bằng chứng y khoa đầu vào, phục vụ cho AI Triage và RAG đối chiếu sau này mà không bị ghi đè.

---

### Nhóm III: Theo dõi Chăm sóc trẻ nhỏ (Infant Care — 4 bảng)
Quản lý sự phát triển, sinh hoạt hằng ngày và lịch tiêm chủng của em bé.

8. **`baby_daily_logs`**
   * **Tác dụng:** Ghi nhận nhật ký ăn, ngủ, vệ sinh hằng ngày của em bé.
   * **Tại sao tồn tại:** Dữ liệu dạng time-series có tần suất ghi nhận rất cao, cần tách riêng để tránh phình to bảng hồ sơ bé và hỗ trợ vẽ biểu đồ sinh hoạt.
9. **`growth_measurements`**
   * **Tác dụng:** Ghi nhận các chỉ số đo chiều cao, cân nặng, vòng đầu của bé.
   * **Tại sao tồn tại:** Dùng để đối chiếu với biểu đồ tăng trưởng chuẩn của WHO nhằm cảnh báo sớm tình trạng suy dinh dưỡng hoặc béo phì.
10. **`vaccination_records`**
    * **Tác dụng:** Ghi nhận lịch sử tiêm chủng thực tế của bé (ngày tiêm, loại vắc-xin, địa điểm tiêm, hoãn tiêm).
    * **Tại sao tồn tại:** Đảm bảo lưu vết chính xác lịch sử tiêm ngừa cá nhân của từng bé, phục vụ việc kiểm tra của bác sĩ nhi khoa.
11. **`vaccination_schedules`**
    * **Tác dụng:** Master data chứa lịch tiêm chủng chuẩn khuyến nghị từ Bộ Y Tế.
    * **Tại sao tồn tại:** Dùng làm mốc tham chiếu tự động tính toán ngày tiêm dự kiến cho từng bé dựa trên ngày sinh.

---

### Nhóm IV: Phối hợp gia đình (Family Coordination — 3 bảng)
Kết nối người thân vào nhóm chăm sóc và giao việc hằng ngày.

12. **`care_groups`**
    * **Tác dụng:** Định nghĩa nhóm gia đình chăm sóc mẹ và bé (Family Sync).
    * **Tại sao tồn tại:** Đóng vai trò là container liên kết nhiều thành viên và các nhiệm vụ chăm sóc lại với nhau.
13. **`care_group_members`**
    * **Tác dụng:** Danh sách các thành viên trong nhóm, trạng thái lời mời và phân quyền chi tiết.
    * **Tại sao tồn tại:** Chứa cột **`permission_json`** để kiểm soát quyền xem dữ liệu riêng tư (lịch trình, nhật ký, cảnh báo, hồ sơ) của từng người thân theo thiết lập của Mẹ bầu.
14. **`care_tasks`**
    * **Tác dụng:** Quản lý công việc chăm sóc.
    * **Tại sao tồn tại:** Thay thế cho `family_tasks` và `reminders`. Hợp nhất các công việc giao thủ công và các nhắc nhở khám thai/tiêm chủng tự động vào một bảng duy nhất, phân biệt qua cột `task_type` để đồng bộ lịch làm việc của gia đình.

---

### Nhóm V: Hồ sơ sức khỏe & File đính kèm (Health Records & Files — 3 bảng)
Quản lý các file bệnh án, ảnh chụp siêu âm và chia sẻ hồ sơ.

15. **`health_records`**
    * **Tác dụng:** Lưu metadata của hồ sơ bệnh án tự tải lên và các báo cáo tổng hợp sức khỏe (Health Summaries).
    * **Tại sao tồn tại:** Tách biệt metadata nghiệp vụ với file nhạy cảm trong hệ thống lưu trữ để đảm bảo tốc độ tìm kiếm và phân quyền.
16. **`attachments`**
    * **Tác dụng:** Metadata quản lý file vật lý được tải lên Supabase Storage.
    * **Tại sao tồn tại:** Thay thế bảng `uploaded_files`. Lưu tập trung thông tin kích thước file, định dạng MIME, checksum bảo mật để tái sử dụng file ở nhiều module (bệnh án, bằng cấp bác sĩ, ảnh diễn đàn).
17. **`health_record_files` (logical; không có bảng vật lý)**
    * **Tác dụng:** Đại diện capability liên kết hồ sơ sức khỏe với file đính kèm.
    * **Ánh xạ snapshot:** Quan hệ vật lý là **1:N** qua `attachments.health_record_id`: một `health_records` có thể có nhiều `attachments`, còn mỗi attachment tham chiếu tối đa một health record. Snapshot không có bảng nối `health_record_files` và không biểu diễn quan hệ nhiều-nhiều.

---

### Nhóm VI: AI Triage & Trợ lý Triệu chứng (AI Triage — 5 bảng)
Tiếp nhận lời khai triệu chứng, phân loại mức độ rủi ro và quản lý nguồn bằng chứng y học.

18. **`triage_sessions`**
    * **Tác dụng:** Phiên đàm thoại chat chẩn đoán với AI và kết quả phân loại rủi ro (Risk Level: Green, Yellow, Red).
    * **Tại sao tồn tại:** Hợp nhất `intake_sessions` và `structured_intake_data` cũ. Lưu trữ toàn bộ hội thoại và triệu chứng cấu trúc bằng JSONB để AI phân tích khẩn cấp mà không cần JOIN bảng phụ.
19. **`triage_session_evidence`**
    * **Tác dụng:** Lưu vết bằng chứng y học (Citation) được trích dẫn trong từng phiên chat.
    * **Tại sao tồn tại:** Đảm bảo AI chẩn đoán có căn cứ khoa học, giúp người dùng và bác sĩ có thể truy xuất nguồn gốc khuyến nghị y tế.
20. **`health_context_memories`**
    * **Tác dụng:** Bộ nhớ ngữ cảnh y tế ngắn hạn được AI ghi nhớ (vd: bé bị nôn trớ hôm qua).
    * **Tại sao tồn tại:** Giúp AI nắm được bệnh sử gần nhất ở lượt chat sau mà không cần bắt người dùng khai báo lại từ đầu. Thiết kế có TTL (`expires_at`) để tự động xóa theo luật bảo mật thông tin y tế.
21. **`knowledge_sources`**
    * **Tác dụng:** Danh mục website y tế chính thống được phép sử dụng (Allow-list).
    * **Tại sao tồn tại:** Thay thế cho `evidence_sources`. Đây là lá chắn an toàn ngăn chặn AI truy cập nguồn tin rác hoặc chưa kiểm chứng khi chẩn đoán cho mẹ và bé.
22. **`knowledge_source_reviews`**
    * **Tác dụng:** Lịch sử phê duyệt hoặc đình bản các domain y học của Admin.
    * **Tại sao tồn tại:** Đảm bảo quy trình kiểm duyệt nguồn y khoa của nền tảng ứng dụng có thể audit rõ ràng.

---

### Nhóm VII: Trường hợp khẩn cấp & Bản đồ SOS (Emergency & SOS — 4 bảng)
Tự động kích hoạt SOS, gửi SMS cảnh báo cho người thân và chỉ đường đến bệnh viện.

23. **`safety_events`**
    * **Tác dụng:** Lưu trữ các sự kiện khẩn cấp bao gồm: phiên SOS khẩn cấp (`emergency_sessions`), nhật ký gửi cảnh báo gia đình (`family_alert_log`), sự kiện ngã cảm biến (`imu_safety_events`), phiếu bàn giao bản đồ (`emergency_map_handoffs`), và tọa độ GPS (`location_snapshots`).
    * **Tại sao tồn tại:** Hợp nhất toàn bộ các bảng khẩn cấp và sự kiện cảm biến về một bảng duy nhất dùng chung cấu trúc JSONB `payload`. Điều này giúp quản lý trạng thái khẩn cấp cực kỳ tập trung, bảo vệ dữ liệu nhạy cảm (tọa độ GPS) và dễ dàng xử lý escalate.
24. **`emergency_contacts`**
    * **Tác dụng:** Danh bạ liên hệ khẩn cấp của người dùng.
    * **Tại sao tồn tại:** Lưu thông tin số điện thoại người thân để hệ thống tự động gửi SMS cảnh báo ngay lập tức khi phát hiện cú ngã hoặc triage rủi ro mức độ Đỏ (RED).
25. **`care_facilities`**
    * **Tác dụng:** Danh mục bệnh viện, phòng khám, trung tâm y tế phụ sản kèm tọa độ địa lý.
    * **Tại sao tồn tại:** Phục vụ chức năng bản đồ khẩn cấp, cho phép tính toán khoảng cách và chỉ đường đến cơ sở y tế gần nhất cho mẹ bầu trong tình huống SOS.
26. **`safety_configs`**
    * **Tác dụng:** Lưu cài đặt bật/tắt cảm biến phát hiện ngã của người dùng.
    * **Tại sao tồn tại:** Cho phép người dùng cấu hình độ nhạy cảm biến hoặc tắt tính năng phát hiện ngã nếu không sử dụng.

---

### Nhóm VIII: Lịch tập Yoga thai kỳ & AI Posture (Exercise & Posture — 3 bảng)
Catalog bài tập yoga, ghi nhận lịch tập và AI kiểm tra tư thế tập thời gian thực.

27. **`maternal_exercise_sessions`**
    * **Tác dụng:** Nhật ký ghi nhận các buổi tập yoga thai kỳ của mẹ bầu.
    * **Tại sao tồn tại:** Theo dõi tiến độ tập luyện và lưu trữ điểm đánh giá tư thế tập tổng quan của buổi tập.
28. **`care_item_templates`**
    * **Tác dụng:** Lưu danh mục bài tập yoga (`pregnancy_exercises`) và cấu hình model AI posture (`posture_analysis_configs`).
    * **Tại sao tồn tại:** Hợp nhất các bảng cấu hình bài tập dùng chung thành một bảng để Admin dễ dàng cập nhật các bài tập yoga mới mà không cần sửa cấu trúc DB.
29. **`health_observations`**
    * **Tác dụng:** Bảng trung tâm lưu trữ chuỗi thời gian các chỉ số đo đạc sức khỏe mẹ (`maternal_health_metrics`), nhật ký sau sinh (`postpartum_logs`), kết quả khảo sát an toàn trước khi tập (`exercise_safety_checks`) và các sự kiện tư thế sai do AI phát hiện (`posture_feedback_events`).
    * **Tại sao tồn tại:** Gom toàn bộ dữ liệu time-series quan sát sức khỏe về một bảng duy nhất dùng chung cấu trúc JSONB. Giúp tối ưu hóa tài nguyên database, dễ dàng lập chỉ mục (index) theo mốc thời gian và vẽ biểu đồ theo dõi sức khỏe tổng hợp.

---

### Nhóm IX: Diễn đàn Cộng đồng (Community — 6 bảng)
Kênh trao đổi kiến thức giữa các mẹ bầu và bác sĩ.

30. **`community_topics`**
    * **Tác dụng:** Danh mục các chủ đề thảo luận trên diễn đàn (thai kỳ, dinh dưỡng, bỉm sữa).
    * **Tại sao tồn tại:** Dùng để phân loại câu hỏi và cho phép người dùng đăng ký theo dõi chuyên mục quan tâm.
31. **`community_questions`**
    * **Tác dụng:** Lưu các bài đăng câu hỏi của người dùng.
    * **Tại sao tồn tại:** Aggregate chính chứa tiêu đề, nội dung câu hỏi, tuần thai/tuổi bé và các chỉ số thống kê lượt xem/lượt trả lời.
32. **`community_answers` (logical; hợp nhất vào `community_content`)**
    * **Tác dụng:** Các câu trả lời của thành viên hoặc bác sĩ cho câu hỏi.
    * **Ánh xạ snapshot:** Câu trả lời là hàng `community_content` có `content_type = 'ANSWER'`; `parent_content_id` trỏ đến nội dung câu hỏi. Không có bảng vật lý `community_answers` riêng.
33. **`community_question_likes` / `community_answer_likes`**
    * **Tác dụng:** Nhật ký người dùng thích bài viết hoặc câu trả lời.
    * **Tại sao tồn tại:** Đảm bảo tính idempotent (mỗi người chỉ được thích 1 lần) và phục vụ thuật toán đẩy bài viết nổi bật.
34. **`community_bookmarks`**
    * **Tác dụng:** Danh sách các câu hỏi được người dùng lưu lại để đọc sau.
    * **Tại sao tồn tại:** Quản lý danh mục lưu trữ cá nhân hóa của từng người dùng trên diễn đàn.
35. **`question_notification_mutes`**
    * **Tác dụng:** Ghi nhận các bài viết người dùng muốn tắt nhận thông báo khi có bình luận mới.
    * **Tại sao tồn tại:** Cho phép người dùng tùy biến thông báo theo từng bài đăng riêng lẻ.

---

### Nhóm X: Bác sĩ, Bằng cấp & Lịch hẹn (Expert & Verification — 4 bảng)
Quản lý thông tin nghề nghiệp bác sĩ sản/nhi, lịch khám và đánh giá chất lượng.

36. **`expert_availability`**
    * **Tác dụng:** Cấu hình khung giờ rảnh có thể nhận cuộc gọi tư vấn y tế của bác sĩ.
    * **Tại sao tồn tại:** Dùng để hiển thị lịch làm việc cho Mẹ bầu lựa chọn khi đặt lịch tư vấn trực tuyến.
37. **`expert_location_shares`**
    * **Tác dụng:** Chia sẻ vị trí GPS trực tiếp của bác sĩ trong các tình huống tư vấn di động/ khẩn cấp.
    * **Tại sao tồn tại:** Phục vụ tìm kiếm bác sĩ gần nhất trong các tình huống khẩn cấp tại nhà.
38. **`expert_credentials`**
    * **Tác dụng:** Lưu trữ bằng cấp, chứng chỉ hành nghề y tế của bác sĩ.
    * **Tại sao tồn tại:** Một bác sĩ có nhiều bằng cấp khác nhau; cần tách riêng để Admin thực hiện quy trình kiểm duyệt nghiêm ngặt trước khi dán nhãn "Bác sĩ đã xác minh" trên ứng dụng.
39. **`expert_reviews` (logical; physical allocation chưa giải quyết)**
    * **Tác dụng:** Các đánh giá, xếp hạng sao của mẹ bầu dành cho bác sĩ sau buổi tư vấn.
    * **Trạng thái snapshot:** Không có `CREATE TABLE expert_reviews` hoặc mapping/discriminator đủ rõ trong snapshot. Không được coi capability này là đã triển khai cho tới khi có allocation và Test-Spec được duyệt.

---

### Nhóm XI: Kênh chat y tế & Gọi video trực tiếp (Chat & Calls — 3 bảng)
Hệ thống chat thường miễn phí giữa mẹ và bác sĩ y khoa.

40. **`direct_conversations`**
    * **Tác dụng:** Kênh chat thường trực 1-1 lâu dài giữa một người mẹ và một bác sĩ chuyên khoa.
    * **Tại sao tồn tại:** Quản lý container tin nhắn chat thường ngày, độc lập hoàn toàn với các cuộc hẹn tư vấn trả phí.
41. **`direct_messages`**
    * **Tác dụng:** Lưu nội dung tin nhắn chi tiết trong phòng chat thường.
    * **Tại sao tồn tại:** Chứa tin nhắn dạng text/ảnh và `client_msg_id` để chống gửi lặp tin nhắn khi mạng chập chờn.
42. **`conversation_calls`**
    * **Tác dụng:** Nhật ký lịch sử các cuộc gọi thoại/video trực tiếp (thông qua ZegoCloud SDK).
    * **Tại sao tồn tại:** Theo dõi thời lượng cuộc gọi và trạng thái cuộc gọi (nhỡ, kết thúc, từ chối) phục vụ thống kê hoạt động.

---

### Nhóm XII: Tư vấn y tế trả phí & Kế toán Tài chính (Consultation & Finance — 4 bảng)
Đặt lịch khám có phí, giao dịch VNPay, tính hoa hồng platform và quyết toán cho bác sĩ.

43. **`archived_consultation_records`**
    * **Tác dụng:** Lưu trữ toàn bộ thông tin lịch sử giao dịch tư vấn bao gồm: lịch hẹn tư vấn (`consultation_bookings`), buổi tư vấn trực tuyến (`consultation_sessions`), lịch sử thanh toán qua VNPay (`payment_transactions`) và hồ sơ tính hoa hồng platform (`commission_records`).
    * **Tại sao tồn tại:** Thay thế cho 4 bảng cũ của Phase 1. Bằng cách lưu trữ lịch sử giao dịch dưới dạng bản ghi lưu trữ bất biến (archived record), hệ thống đảm bảo tính toàn vẹn của dữ liệu kế toán, không bị ảnh hưởng khi thông tin giá cả hay cấu hình hoa hồng thay đổi, đồng thời giảm tải số lượng bảng hoạt động của database.
44. **`refund_records` (logical; physical allocation chưa giải quyết)**
    * **Tác dụng:** Quản lý các yêu cầu hoàn tiền khi buổi tư vấn bị hủy hoặc gặp tranh chấp.
    * **Trạng thái snapshot:** Không có `CREATE TABLE refund_records` hoặc mapping/discriminator đủ rõ; không được giả định capability này nằm trong `archived_records`.
45. **`settlement_records` (logical; physical allocation chưa giải quyết)**
    * **Tác dụng:** Bản ghi đối soát và quyết toán chuyển khoản thu nhập định kỳ cho bác sĩ.
    * **Trạng thái snapshot:** Không có `CREATE TABLE settlement_records` hoặc mapping/discriminator đủ rõ; không được giả định capability này nằm trong `archived_records`.
46. **`consultation_disputes` (logical; physical allocation chưa giải quyết)**
    * **Tác dụng:** Tiếp nhận khiếu nại của mẹ bầu về chất lượng buổi tư vấn của bác sĩ để Admin xử lý hoàn tiền.
    * **Trạng thái snapshot:** Không có `CREATE TABLE consultation_disputes` hoặc mapping/discriminator đủ rõ; không được giả định capability này nằm trong `archived_records`.

---

### Nhóm XIII: Hạ tầng, Thông báo & Cấu hình hệ thống (Infrastructure & Configurations — 4 bảng)
Quản trị cấu hình toàn hệ thống, outbox thông báo đẩy FCM.

47. **`notification_records`**
    * **Tác dụng:** Hộp thư thông báo của người dùng và hàng đợi bền vững (Durable Outbox) để gửi FCM push notification.
    * **Tại sao tồn tại:** Thay thế bảng `notifications` cũ. Vừa làm hòm thư hiển thị trên app, vừa quản lý trạng thái gửi tin (chờ gửi, thành công, số lần thử gửi lại khi lỗi mạng) để đảm bảo thông báo luôn được phân phát.
48. **`device_tokens`**
    * **Tác dụng:** Lưu trữ token thiết bị Firebase Cloud Messaging (FCM) của người dùng.
    * **Tại sao tồn tại:** Một tài khoản người dùng có thể đăng nhập trên nhiều thiết bị (điện thoại, máy tính bảng); bảng này lưu token của từng thiết bị để gửi thông báo đẩy chính xác.
49. **`system_configurations`**
    * **Tác dụng:** Lưu các thông số vận hành toàn cục của ứng dụng (dung lượng upload tối đa, ngưỡng cảm biến ngã mặc định, tham số AI...).
    * **Tại sao tồn tại:** Giúp Admin thay đổi các thông số cấu hình của hệ thống ngay lập tức mà không cần phải build/deploy lại code backend.
50. **`audit_events`**
    * **Tác dụng:** Bảng lưu vết lịch sử kiểm toán dùng chung (Audit logs, nhật ký chuyển đổi thai kỳ mẹ `mother_journey_events`, log an ninh, và điểm đóng góp bác sĩ `contribution_points`).
    * **Tại sao tồn tại:** Hợp nhất toàn bộ các bảng log riêng lẻ của Phase 1. Sử dụng cột JSONB `payload` để lưu vết bất biến mọi hành động nhạy cảm trong hệ thống, phục vụ công tác thanh tra dữ liệu và bảo mật.
