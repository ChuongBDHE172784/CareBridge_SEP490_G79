# Mô tả chi tiết CareBridge Context Diagrams

## 1. Thông tin tài liệu

- Nguồn mô tả: `CareBridge_Context_Diagrams.drawio`
- Thư mục: `02_Requirements/ContextDiagram/`
- Số trang sơ đồ: 2
- Trang 1: `Web App Context`
- Trang 2: `Mobile App Context`
- Mục đích: mô tả ranh giới hệ thống CareBridge, các tác nhân/hệ thống bên ngoài và các luồng dữ liệu chính giữa CareBridge với môi trường xung quanh.

## 2. Tổng quan phạm vi hệ thống

CareBridge được mô tả ở mức context diagram, tức là tập trung vào quan hệ giữa hệ thống trung tâm và các bên tương tác bên ngoài, chưa đi sâu vào cấu trúc module nội bộ.

Sơ đồ chia CareBridge thành hai bối cảnh sử dụng chính:

1. **CareBridge Web App**: đại diện cho các cổng quản trị và vận hành gồm Admin Web, Expert Portal và Partner Portal.
2. **CareBridge Mobile App**: đại diện cho ứng dụng di động dùng bởi mẹ, thành viên gia đình và chuyên gia.

Hai bối cảnh đều kết nối với các dịch vụ nền tảng chung như xác thực/trạng thái tài khoản, dịch vụ đồng ý và kiểm soát truy cập, lưu trữ hồ sơ, nhật ký kiểm toán, thông báo đẩy, thanh toán, AI và dịch vụ gọi/chat.

## 3. Quy ước đọc sơ đồ

- Hình trung tâm là hệ thống CareBridge trong từng bối cảnh.
- Các khối xung quanh là tác nhân người dùng hoặc hệ thống/dịch vụ ngoài.
- Mũi tên thể hiện hướng luồng dữ liệu hoặc yêu cầu.
- Nhãn trên mũi tên mô tả loại dữ liệu, yêu cầu, kết quả xử lý hoặc trạng thái được trao đổi.
- Các tương tác có chiều đi và chiều về thường được tách thành hai mũi tên để làm rõ dữ liệu đầu vào và phản hồi.

## 4. Web App Context

### 4.1. Hệ thống trung tâm

**CareBridge Web App** gồm ba nhóm portal chính:

- **Admin Web**: dành cho quản trị hệ thống và vận hành nền tảng.
- **Expert Portal**: dành cho chuyên gia đã xác minh để quản lý hồ sơ, lịch tư vấn và hoạt động tư vấn.
- **Partner Portal**: dành cho đại diện đối tác để quản lý hồ sơ đối tác, dịch vụ và chiến dịch tài trợ.

Web App là điểm điều phối dữ liệu giữa các vai trò vận hành và các dịch vụ bên ngoài như thanh toán, AI, thông báo, gọi video/voice, lưu trữ hồ sơ, kiểm soát truy cập và kiểm toán.

### 4.2. Tác nhân người dùng trong Web App

#### Guest User

Guest User là người dùng chưa đăng nhập hoặc chưa hoàn tất trạng thái tài khoản. Luồng chính:

- Gửi thông tin đăng ký, đăng nhập và OTP vào Web App.
- Nhận trạng thái xác thực và trạng thái tài khoản từ Web App.

Ý nghĩa nghiệp vụ: nhóm này liên quan đến khởi tạo phiên truy cập, xác minh danh tính cơ bản và kiểm soát điều kiện vào hệ thống.

#### Verified Expert

Verified Expert là chuyên gia đã được xác minh. Luồng chính từ chuyên gia vào hệ thống:

- Hồ sơ chuyên gia.
- Tài liệu xác minh.
- Thông tin khả dụng/lịch rảnh.
- Câu trả lời cho câu hỏi.
- Tóm tắt tư vấn.

Luồng từ hệ thống về chuyên gia:

- Hàng đợi câu hỏi.
- Booking tư vấn.
- Tóm tắt được chia sẻ.
- Thu nhập.
- Trạng thái xác minh hoặc gia hạn xác minh.

Ý nghĩa nghiệp vụ: Web App hỗ trợ chuyên gia quản lý năng lực tư vấn, tình trạng xác minh, luồng hỏi đáp và nghĩa vụ sau phiên tư vấn.

#### Community Moderator

Community Moderator phụ trách kiểm duyệt cộng đồng. Luồng từ moderator vào hệ thống:

- Vòng đời xử lý báo cáo.
- Quyết định kiểm duyệt.
- Cảnh báo hoặc đình chỉ.
- Quản trị chủ đề.

Luồng từ hệ thống về moderator:

- Hàng đợi kiểm duyệt.
- Báo cáo.
- Nội dung bị gắn cờ.
- Lịch sử thực thi đối với tài khoản.

Ý nghĩa nghiệp vụ: vai trò này bảo đảm nội dung cộng đồng được xử lý theo quy tắc, có bằng chứng và có lịch sử thực thi.

#### Content Admin

Content Admin quản lý nội dung chính thức của nền tảng. Luồng từ Content Admin vào hệ thống:

- Bài viết.
- FAQ.
- Checklist.
- Bài tập thai kỳ.
- Danh mục và phiên bản nội dung.

Luồng từ hệ thống về Content Admin:

- Trạng thái phê duyệt.
- Lịch sử phiên bản.
- Yêu cầu gỡ xuất bản hoặc chỉnh sửa.
- Phản hồi.

Ý nghĩa nghiệp vụ: Web App hỗ trợ quản trị vòng đời nội dung, từ tạo mới, cập nhật, phê duyệt, xuất bản đến gỡ hoặc yêu cầu sửa đổi.

#### System Admin

System Admin là vai trò quản trị cấp hệ thống. Luồng từ admin vào hệ thống:

- Quản lý người dùng và vai trò.
- Quản lý chuyên gia và đối tác.
- Cấu hình quy tắc AI/red-flag.
- Cấu hình posture.
- Dashboard vận hành.
- Xử lý vòng đời tư vấn: đổi lịch, hủy, hoàn tất, no-show, tranh chấp và hoàn tiền.

Luồng từ hệ thống về admin:

- Báo cáo hệ thống.
- Audit log.
- Sự kiện bảo mật.
- Chỉ số cộng đồng, chuyên gia và đối tác.
- Kết quả hoàn tiền/tranh chấp.
- Báo cáo hoa hồng và đối soát.

Ý nghĩa nghiệp vụ: System Admin là vai trò giám sát toàn nền tảng, có quyền tác động đến cấu hình, vận hành, an toàn, thanh toán và báo cáo.

#### Partner Representative

Partner Representative là đại diện đối tác. Luồng từ đối tác vào hệ thống:

- Hồ sơ đối tác.
- Danh sách dịch vụ.
- Vòng đời chiến dịch hoặc nội dung tài trợ.

Luồng từ hệ thống về đối tác:

- Kết quả phê duyệt, yêu cầu chỉnh sửa hoặc đình chỉ.
- Hiệu quả giới thiệu/referral.
- Hiệu quả chiến dịch.

Ý nghĩa nghiệp vụ: Web App cung cấp kênh để đối tác đăng ký, quản lý dịch vụ và theo dõi hiệu quả hoạt động trong hệ sinh thái CareBridge.

### 4.3. Dịch vụ/hệ thống ngoài trong Web App

#### VNPay Payment Gateway

Web App gửi đến VNPay:

- Yêu cầu thanh toán.
- Yêu cầu hoàn tiền.
- Yêu cầu hoa hồng.
- Yêu cầu đối soát.

VNPay trả về:

- Trạng thái thanh toán.
- Trạng thái hoàn tiền.
- Trạng thái quyết toán/settlement.

Ý nghĩa: VNPay là cổng xử lý giao dịch tài chính; Web App không tự xử lý thanh toán trực tiếp mà gửi yêu cầu và nhận trạng thái từ cổng thanh toán.

#### Firebase Cloud Messaging

Web App gửi đến Firebase Cloud Messaging:

- Yêu cầu gửi push notification.

Firebase trả về:

- Trạng thái gửi thông báo.

Ý nghĩa: FCM đảm nhiệm việc chuyển thông báo đẩy, còn Web App quyết định nội dung, đối tượng và ngữ cảnh thông báo.

#### Gemini AI Service

Web App gửi đến Gemini:

- Prompt phục vụ intake.
- Prompt phục vụ RAG.
- Prompt hỗ trợ red-flag/triage.

Gemini trả về:

- Dữ liệu intake có cấu trúc.
- Câu trả lời RAG.
- Kết quả hỗ trợ phân loại/triage.

Ý nghĩa: Gemini AI Service hỗ trợ xử lý thông tin và tri thức, nhưng kết quả cần được hệ thống CareBridge kiểm soát trong bối cảnh y tế/mẹ và bé, đặc biệt với các tình huống cảnh báo đỏ.

#### ZegoCloud Call Service

Web App gửi đến ZegoCloud:

- Yêu cầu tạo phòng voice/video.

ZegoCloud trả về:

- Trạng thái phiên gọi.

Ý nghĩa: Web App dùng ZegoCloud để hỗ trợ tư vấn trực tuyến qua thoại hoặc video, còn CareBridge quản lý ngữ cảnh nghiệp vụ của phiên tư vấn.

#### Consent & Access Control Service

Web App gửi đến dịch vụ này:

- Kiểm tra consent.
- Phạm vi chia sẻ.
- Thời hạn hiệu lực.
- Thu hồi quyền.

Dịch vụ trả về:

- Quyết định truy cập.
- Lịch sử chia sẻ.

Ý nghĩa: đây là thành phần quan trọng để bảo đảm dữ liệu sức khỏe và dữ liệu cá nhân chỉ được truy cập trong phạm vi đã được cho phép.

#### Firebase Storage

Web App gửi đến Firebase Storage:

- Tải lên, xem hoặc truy cập các tệp sức khỏe và bản tóm tắt được chia sẻ.

Firebase Storage trả về:

- Metadata của tệp.
- Trạng thái truy xuất.
- Nhãn nguồn dữ liệu.

Ý nghĩa: Firebase Storage là nơi lưu trữ tệp/hồ sơ, còn Web App quản lý quyền truy cập, ngữ cảnh nghiệp vụ và cách hiển thị dữ liệu.

#### Audit / Security Log Service

Web App gửi đến dịch vụ log:

- Sự kiện quản trị.
- Sự kiện consent.
- Sự kiện truy cập của chuyên gia.
- Sự kiện thanh toán.
- Sự kiện bảo mật.

Dịch vụ trả về:

- Audit trail.
- Bằng chứng sự cố.
- Lịch sử truy cập.

Ý nghĩa: dịch vụ này cung cấp khả năng truy vết, phục vụ điều tra sự cố, kiểm toán quyền truy cập và quản trị bảo mật.

#### Approved Content / RAG Knowledge Base

Web App gửi đến kho tri thức:

- Nội dung đã được phê duyệt.
- FAQ.
- Checklist.
- Tri thức bài tập.

Kho tri thức trả về:

- Tri thức đã phê duyệt cho RAG.
- Dữ liệu phục vụ khám phá nội dung.

Ý nghĩa: chỉ nội dung đã qua kiểm duyệt/phê duyệt mới được đưa vào nguồn tri thức, giúp giảm rủi ro AI sử dụng nội dung chưa được xác minh.

## 5. Mobile App Context

### 5.1. Hệ thống trung tâm

**CareBridge Mobile App** gồm ba trải nghiệm ứng dụng chính:

- **Mother App**: dành cho mẹ trong thai kỳ hoặc sau sinh.
- **Family App**: dành cho thành viên gia đình tham gia chăm sóc.
- **Expert App**: dành cho chuyên gia tương tác qua tư vấn, chat/call và hỗ trợ theo ngữ cảnh.

Mobile App là điểm tiếp xúc trực tiếp nhất với người dùng cuối, vì vậy sơ đồ Mobile thể hiện nhiều luồng liên quan đến hồ sơ sức khỏe, chăm sóc hằng ngày, cộng đồng, tư vấn, an toàn, thiết bị, cảm biến và quyền riêng tư.

### 5.2. Tác nhân người dùng trong Mobile App

#### Guest User

Guest User tương tác với Mobile App để:

- Đăng ký.
- Đăng nhập.
- Xác thực OTP.
- Đặt lại mật khẩu.

Mobile App trả về:

- Trạng thái xác thực.
- Kết quả onboarding.

Ý nghĩa nghiệp vụ: đây là luồng khởi tạo người dùng trên mobile, từ truy cập ban đầu đến hoàn tất điều kiện sử dụng ứng dụng.

#### User (shared role)

User là vai trò dùng chung cho các chức năng tài khoản cơ bản. Người dùng gửi vào Mobile App:

- Hồ sơ cá nhân.
- Cài đặt quyền riêng tư.
- Tùy chọn thông báo.
- Tìm kiếm/báo cáo.

Mobile App trả về:

- Thông báo.
- Trạng thái tài khoản.
- Kết quả tìm kiếm/báo cáo.

Ý nghĩa nghiệp vụ: vai trò dùng chung gom các thao tác tài khoản phổ biến, tránh lặp lại ở từng vai trò cụ thể.

#### Mother

Mother là tác nhân trung tâm của Mobile App. Dữ liệu mẹ gửi vào hệ thống gồm nhiều nhóm:

- Hồ sơ, hành trình thai kỳ/hậu sản, nhật ký mẹ/bé và hồ sơ sức khỏe.
- Feed cộng đồng/Q&A, reaction, bookmark, báo cáo và tìm kiếm chuyên gia.
- Vòng đời đặt lịch tư vấn, thanh toán, đánh giá, tranh chấp/hoàn tiền.
- Theo dõi an toàn, liên hệ khẩn cấp, phát hiện té/ngã, xác nhận và báo cáo false-positive.
- Nhóm chăm sóc, lời mời, quyền, phân công nhiệm vụ và lịch chia sẻ.
- Phiên bài tập thai kỳ, hồ sơ tiêm chủng, chỉ số tăng trưởng, chi phí và checklist.

Mobile App trả về cho Mother:

- Dashboard hành trình, nhắc nhở, biểu đồ, tiêm chủng/tăng trưởng và gợi ý chi phí/checklist.
- Feed cộng đồng, câu trả lời, thông báo và danh bạ/hồ sơ chuyên gia.
- Trạng thái booking, thông tin phiên tư vấn, tóm tắt và kết quả thanh toán/hoàn tiền.
- Cảnh báo an toàn, workflow khẩn cấp và lịch sử sự kiện an toàn.
- Lịch chia sẻ, trạng thái nhiệm vụ, dữ liệu chia sẻ và trạng thái quyền.
- Kết quả bài tập, lịch tiêm chủng, lịch sử tăng trưởng và tổng hợp chi phí/checklist.

Ý nghĩa nghiệp vụ: Mother là vai trò tiêu thụ và tạo dữ liệu lớn nhất trong Mobile App. Các luồng của mẹ bao phủ toàn bộ hành trình chăm sóc, từ sức khỏe, cộng đồng, tư vấn đến an toàn và phối hợp gia đình.

#### Family Member

Family Member tương tác với Mobile App để:

- Chấp nhận hoặc từ chối lời mời.
- Xem dữ liệu được chia sẻ.
- Cập nhật hoặc rời khỏi nhiệm vụ.

Mobile App gửi cho Family Member:

- Cảnh báo khẩn cấp.
- Lịch/nhiệm vụ/hồ sơ được chia sẻ.
- Cập nhật quyền.

Ý nghĩa nghiệp vụ: thành viên gia đình tham gia hỗ trợ chăm sóc dựa trên dữ liệu được mẹ chia sẻ và quyền truy cập được cấp.

#### Verified Expert

Verified Expert trong Mobile App gửi:

- Lịch khả dụng.
- Phản hồi tư vấn.
- Chat/call/video.
- Trạng thái hiển thị vị trí.

Mobile App gửi cho chuyên gia:

- Yêu cầu tư vấn.
- Tóm tắt được chia sẻ.
- Yêu cầu hỗ trợ gần vị trí.
- Ngữ cảnh tuyến đường/liên hệ.

Ý nghĩa nghiệp vụ: chuyên gia dùng Mobile App để phản hồi nhanh, hỗ trợ tư vấn và tham gia các tình huống cần hỗ trợ theo vị trí hoặc theo hồ sơ được chia sẻ.

### 5.3. Dịch vụ/hệ thống ngoài trong Mobile App

#### VNPay Payment Gateway

Mobile App gửi đến VNPay:

- Yêu cầu thanh toán tư vấn.
- Yêu cầu hoàn tiền.

VNPay trả về:

- Trạng thái giao dịch.
- Trạng thái hoàn tiền.

Ý nghĩa: Mobile App khởi tạo giao dịch trong luồng đặt lịch/tư vấn, nhưng kết quả tài chính được xác nhận bởi VNPay.

#### TrackAsia Map Service

Mobile App gửi đến TrackAsia:

- Vị trí.
- Tìm cơ sở hoặc chuyên gia gần đó.
- Yêu cầu hỗ trợ.
- Tuyến đường và ETA.

TrackAsia trả về:

- Kết quả gần vị trí.
- Khoảng cách.
- Đường đi.
- Thời gian dự kiến.

Ý nghĩa: dịch vụ bản đồ hỗ trợ các tình huống cần định vị, tìm chuyên gia/cơ sở gần nhất và điều hướng.

#### Smartwatch / Wearable Device

Mobile App gửi đến thiết bị đeo:

- Yêu cầu kết nối.
- Đồng bộ.
- Nhập dữ liệu.
- Ngắt kết nối.

Thiết bị trả về:

- Chỉ số thiết bị.
- Trạng thái đồng bộ.
- Nhãn nguồn dữ liệu.
- Nhãn chất lượng dữ liệu.

Ý nghĩa: thiết bị đeo cung cấp dữ liệu sức khỏe hoặc hoạt động bổ sung, nhưng Mobile App cần gắn nhãn nguồn và chất lượng để tránh hiểu sai dữ liệu.

#### Gemini AI Service

Mobile App gửi đến Gemini:

- Prompt intake.
- FAQ.
- Ngữ cảnh sức khỏe/thiết bị được chọn.

Gemini trả về:

- Intake có cấu trúc.
- Câu trả lời RAG.
- Hướng dẫn an toàn.

Ý nghĩa: AI hỗ trợ trả lời và phân loại thông tin, nhưng nên hoạt động trong phạm vi dữ liệu được chọn và tri thức đã phê duyệt.

#### Firebase Cloud Messaging

Mobile App gửi đến FCM:

- Yêu cầu thông báo nhắc nhở.
- Yêu cầu thông báo cộng đồng.
- Yêu cầu thông báo tư vấn.
- Yêu cầu thông báo khẩn cấp.

FCM trả về:

- Trạng thái gửi theo từng loại thông báo.

Ý nghĩa: FCM hỗ trợ gửi thông báo thời gian thực hoặc gần thời gian thực, đặc biệt quan trọng với nhắc lịch và sự kiện an toàn.

#### ZegoCloud + Firebase Realtime

Mobile App gửi đến dịch vụ realtime:

- Yêu cầu tạo phòng voice/video qua ZegoCloud.
- Tin nhắn chat qua Firebase.

Dịch vụ trả về:

- Trạng thái cuộc gọi ZegoCloud.
- Trạng thái chat Firebase.

Ý nghĩa: kết hợp ZegoCloud và Firebase Realtime để hỗ trợ tư vấn đồng bộ qua gọi thoại/video và chat.

#### Phone Motion Sensors

Mobile App gửi đến cảm biến điện thoại:

- Bắt đầu/dừng theo dõi an toàn.
- Kiểm tra nghi ngờ té/ngã hoặc va chạm.

Cảm biến trả về:

- Dữ liệu gia tốc kế.
- Dữ liệu con quay hồi chuyển.
- Metadata sự kiện.

Ý nghĩa: cảm biến điện thoại là nguồn dữ liệu tại thiết bị cho chức năng an toàn, phát hiện té/ngã hoặc va chạm.

#### Consent & Access Control Service

Mobile App gửi đến dịch vụ consent:

- Cấp quyền.
- Thu hồi quyền.
- Kiểm tra consent.
- Phạm vi và thời hạn quyền.

Dịch vụ trả về:

- Quyết định truy cập.
- Trạng thái quyền.
- Lịch sử chia sẻ.

Ý nghĩa: Mobile App sử dụng consent/access control để bảo vệ dữ liệu cá nhân, hồ sơ sức khỏe và dữ liệu chia sẻ cho gia đình/chuyên gia.

#### Firebase Storage

Mobile App gửi đến Firebase Storage:

- Tải lên hồ sơ sức khỏe và tệp.
- Xem hồ sơ/tệp.
- Xóa hồ sơ/tệp.

Firebase Storage trả về:

- Metadata tệp.
- Trạng thái truy xuất.
- Nhãn nguồn.

Ý nghĩa: Firebase Storage giữ các tệp/hồ sơ, còn Mobile App chịu trách nhiệm kiểm soát quyền, hiển thị và quản lý vòng đời dữ liệu.

#### MediaPipe Pose Analysis

Mobile App gửi đến MediaPipe:

- Input camera để nhận diện pose landmarks.

MediaPipe trả về:

- Phản hồi posture.
- Kết quả kiểm tra an toàn.
- Chỉ số phiên tập.

Ý nghĩa: MediaPipe chạy phân tích tư thế từ camera trên thiết bị, phục vụ bài tập thai kỳ hoặc kiểm tra an toàn mà không nhất thiết phải gửi toàn bộ dữ liệu camera ra ngoài.

#### Audit / Safety Event Log Service

Mobile App gửi đến dịch vụ log:

- Sự kiện an toàn.
- Sự kiện consent/audit.
- Lịch sử truy cập.

Dịch vụ trả về:

- Lịch sử an toàn.
- Bằng chứng audit.
- Lịch sử false-positive.

Ý nghĩa: dịch vụ này giúp truy vết sự kiện nhạy cảm, đặc biệt là cảnh báo an toàn, quyền truy cập và các trường hợp báo động sai.

## 6. Nhận xét kiến trúc từ sơ đồ

### 6.1. Phân tách rõ Web và Mobile theo trách nhiệm

Web App tập trung vào vận hành, quản trị, kiểm duyệt, nội dung, đối tác, chuyên gia và báo cáo. Mobile App tập trung vào trải nghiệm người dùng cuối, chăm sóc hằng ngày, tư vấn, an toàn, thiết bị và phối hợp gia đình.

Sự phân tách này giúp giảm chồng chéo quyền hạn: chức năng quản trị nhạy cảm nằm ở Web App, còn Mobile App ưu tiên luồng tương tác nhanh và cá nhân hóa.

### 6.2. Quyền riêng tư và consent là luồng xuyên suốt

Cả Web App và Mobile App đều kết nối với Consent & Access Control Service. Điều này cho thấy dữ liệu sức khỏe, hồ sơ, tệp chia sẻ, tóm tắt tư vấn và dữ liệu gia đình/chuyên gia cần được kiểm soát bằng consent rõ ràng.

Các thông tin quan trọng gồm:

- Phạm vi chia sẻ.
- Thời hạn hiệu lực.
- Quyết định truy cập.
- Lịch sử chia sẻ.
- Thu hồi quyền.

### 6.3. AI được đặt trong vai trò hỗ trợ, không phải nguồn quyết định độc lập

Gemini AI Service nhận prompt và ngữ cảnh được chọn, sau đó trả về intake có cấu trúc, câu trả lời RAG hoặc hỗ trợ triage. Sơ đồ không thể hiện AI là bên tự ra quyết định cuối cùng; CareBridge vẫn là hệ thống trung tâm kiểm soát luồng nghiệp vụ.

Điều này phù hợp với bối cảnh mẹ và bé, nơi các nội dung liên quan sức khỏe, cảnh báo đỏ và an toàn cần được giới hạn bởi tri thức đã phê duyệt và quy trình kiểm soát.

### 6.4. Dữ liệu nhạy cảm có yêu cầu truy vết cao

Sơ đồ có cả Audit / Security Log Service ở Web App và Audit / Safety Event Log Service ở Mobile App. Các luồng log bao phủ:

- Truy cập của chuyên gia.
- Consent.
- Thanh toán.
- Sự kiện bảo mật.
- Sự kiện an toàn.
- False-positive history.
- Bằng chứng sự cố.

Điều này cho thấy hệ thống cần khả năng kiểm toán và điều tra sau sự kiện, đặc biệt với dữ liệu sức khỏe, thanh toán và cảnh báo an toàn.

### 6.5. Mobile App có nhiều nguồn dữ liệu thiết bị

Mobile App nhận dữ liệu từ:

- Smartwatch / Wearable Device.
- Phone Motion Sensors.
- On-device Camera qua MediaPipe.

Các nguồn này cần được gắn nhãn nguồn, chất lượng dữ liệu và trạng thái đồng bộ để hệ thống và người dùng hiểu đúng độ tin cậy của dữ liệu.

### 6.6. Hệ sinh thái nội dung và RAG cần kiểm soát nguồn tri thức

Web App có luồng xuất bản nội dung đã phê duyệt sang Approved Content / RAG Knowledge Base. Kho này cung cấp tri thức cho RAG và khám phá nội dung. Điều này tạo cơ chế kiểm soát chất lượng trước khi nội dung được dùng bởi AI hoặc hiển thị trong nền tảng.

## 7. Rủi ro và điểm cần lưu ý khi đặc tả chi tiết

1. **Quyền truy cập hồ sơ sức khỏe**: cần định nghĩa rõ ai được xem, xem trong bao lâu, xem loại dữ liệu nào và khi nào quyền bị thu hồi.
2. **Luồng tư vấn và thanh toán**: cần đồng bộ trạng thái booking, phiên gọi, thanh toán, hoàn tiền, tranh chấp và báo cáo hoa hồng.
3. **AI/RAG và red-flag**: cần xác định rõ phạm vi dữ liệu gửi sang AI, cơ chế lọc nội dung, fallback khi AI không chắc chắn và quy tắc chuyển tuyến khẩn cấp.
4. **Cảnh báo an toàn**: cần xử lý false-positive, xác nhận của người dùng, thông báo cho gia đình/chuyên gia và lưu audit đầy đủ.
5. **Thiết bị/cảm biến**: cần phân biệt dữ liệu đo được, dữ liệu đồng bộ thành công, dữ liệu lỗi và dữ liệu có độ tin cậy thấp.
6. **Thông báo đẩy**: cần phân loại ưu tiên giữa nhắc nhở thông thường, tư vấn, cộng đồng và khẩn cấp.
7. **Nội dung được phê duyệt**: cần có quy trình versioning, phê duyệt, gỡ xuất bản và cập nhật kho RAG.

## 8. Tóm tắt

`CareBridge_Context_Diagrams.drawio` mô tả CareBridge như một nền tảng gồm hai mặt chính:

- **Web App** phục vụ vận hành, quản trị, chuyên gia, đối tác, kiểm duyệt, nội dung và báo cáo.
- **Mobile App** phục vụ mẹ, gia đình và chuyên gia trong các luồng chăm sóc, cộng đồng, tư vấn, thanh toán, an toàn, thiết bị và quyền riêng tư.

Các dịch vụ ngoài như VNPay, Firebase, Gemini, ZegoCloud, TrackAsia, MediaPipe, thiết bị đeo, cảm biến điện thoại, storage, consent và audit tạo thành hệ sinh thái hỗ trợ. Điểm nổi bật của sơ đồ là nhấn mạnh quyền riêng tư, consent, kiểm toán, dữ liệu sức khỏe, an toàn người dùng và kiểm soát nguồn tri thức cho AI/RAG.
