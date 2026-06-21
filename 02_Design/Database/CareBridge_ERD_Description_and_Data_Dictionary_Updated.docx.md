

**CAREBRIDGE**  
**ERD DESCRIPTION & DATA DICTIONARY**  
*67 entity \- Logical Data Model hoàn thiện*

Cập nhật theo bộ 241 Use Case và Business Logic MF-01 đến MF-21  
Phiên bản: 12/06/2026

# **1\. ERD Description**

Bảng mô tả các entity nghiệp vụ trong ERD. Entity được đặt tên bằng danh từ số nhiều; quan hệ nhiều-nhiều được phân rã bằng entity trung gian; PK/FK và cardinality được đồng bộ với mã PlantUML.

| STT | Entity | Description |
| :---- | :---- | :---- |
| 1 | **roles** | Danh mục vai trò hệ thống dùng cho RBAC như USER, MOTHER, FAMILY\_MEMBER, EXPERT, MODERATOR, CONTENT\_ADMIN, ADMIN và PARTNER. |
| 2 | **users** | Tài khoản gốc của mọi người dùng CareBridge; lưu dữ liệu định danh và trạng thái tài khoản, không trộn với hồ sơ sức khỏe hoặc hồ sơ cộng đồng. |
| 3 | **user\_roles** | Thực thể trung gian giải quyết quan hệ nhiều-nhiều giữa users và roles. |
| 4 | **user\_sessions** | Phiên đăng nhập và refresh token của người dùng để hỗ trợ xem/thu hồi phiên trên thiết bị khác. |
| 5 | **community\_profiles** | Hồ sơ hiển thị công khai trong cộng đồng, tách biệt khỏi hồ sơ định danh và hồ sơ sức khỏe. |
| 6 | **notification\_preferences** | Tùy chọn kênh và loại thông báo theo từng người dùng. |
| 7 | **notifications** | Thông báo cá nhân do hệ thống tạo từ reminder, community, consultation, safety hoặc vận hành. |
| 8 | **data\_permissions** | Quyền chia sẻ dữ liệu do người dùng cấp cho người thân hoặc chuyên gia theo phạm vi, mục đích và thời hạn. |
| 9 | **audit\_logs** | Nhật ký bất biến của hành động quan trọng: đăng nhập, xem hồ sơ, thay đổi quyền, moderation, tư vấn, thanh toán và sự kiện an toàn. |
| 10 | **mother\_journeys** | Hành trình của mẹ theo trạng thái trước mang thai, thai kỳ hoặc sau sinh; một người dùng có thể có nhiều hành trình theo thời gian. |
| 11 | **maternal\_health\_metrics** | Chỉ số sức khỏe mẹ được nhập thủ công hoặc đồng bộ có gắn nhãn nguồn. |
| 12 | **postpartum\_logs** | Nhật ký phục hồi sau sinh về sản dịch, đau, sữa mẹ, giấc ngủ, tâm trạng và triệu chứng. |
| 13 | **baby\_profiles** | Hồ sơ trẻ do mẹ/người chăm sóc tạo và quản lý. |
| 14 | **baby\_daily\_logs** | Nhật ký hằng ngày của bé: bú, ngủ, tã, nhiệt độ, nôn trớ, thuốc theo chỉ định và ghi chú. |
| 15 | **development\_milestones** | Mốc phát triển của trẻ như lẫy, bò, đi, nói, mọc răng hoặc ăn dặm. |
| 16 | **growth\_measurements** | Các lần đo cân nặng, chiều cao và vòng đầu để tạo biểu đồ tăng trưởng. |
| 17 | **vaccination\_records** | Lịch và trạng thái tiêm chủng của trẻ; là dữ liệu theo dõi cá nhân, không thay thế sổ tiêm chính thức. |
| 18 | **health\_records** | Kho hồ sơ sức khỏe cá nhân của mẹ/bé gồm ảnh siêu âm, xét nghiệm, đơn thuốc, phiếu tiêm và kết quả khám, có nhãn nguồn. |
| 19 | **health\_summaries** | Bản tóm tắt dữ liệu sức khỏe theo khoảng thời gian do người dùng tạo để xem hoặc chia sẻ có thời hạn. |
| 20 | **reminders** | Nhắc lịch khám, thuốc/vitamin, tiêm, checklist và việc chăm sóc. |
| 21 | **care\_groups** | Nhóm chăm sóc do mẹ/người quản lý tạo cho một hành trình hoặc một bé. |
| 22 | **care\_group\_members** | Thực thể trung gian giữa care\_groups và users; lưu trạng thái lời mời và quyền thành viên. |
| 23 | **care\_tasks** | Việc chăm sóc được giao cho thành viên nhóm, có hạn và trạng thái thực hiện. |
| 24 | **expenses** | Khoản chi liên quan thai kỳ, sau sinh hoặc chăm sóc trẻ. |
| 25 | **community\_topics** | Danh mục chủ đề cộng đồng dùng để phân loại câu hỏi và nội dung. |
| 26 | **community\_questions** | Câu hỏi do người dùng đăng, có thể ẩn danh hiển thị nhưng luôn truy vết nội bộ. |
| 27 | **community\_answers** | Câu trả lời của người dùng hoặc chuyên gia; câu trả lời chuyên gia gắn badge/phạm vi chuyên môn. |
| 28 | **content\_reports** | Báo cáo nội dung hoặc tài khoản vi phạm do người dùng gửi. |
| 29 | **moderation\_actions** | Hành động kiểm duyệt đối với báo cáo hoặc nội dung: duyệt, ẩn, khóa, cảnh cáo, tạm ngưng. |
| 30 | **content\_items** | Bài viết, FAQ hoặc nội dung giáo dục đã được quản trị nội dung tạo và kiểm duyệt. |
| 31 | **checklist\_templates** | Mẫu checklist chuẩn bị theo giai đoạn được kiểm duyệt. |
| 32 | **checklist\_items** | Các mục con thuộc một checklist template. |
| 33 | **expert\_profiles** | Hồ sơ chuyên môn của bác sĩ/chuyên gia đã đăng ký; chỉ có badge xác thực sau khi admin duyệt. |
| 34 | **expert\_credentials** | Giấy tờ/chứng chỉ chuyên gia được tải lên để xác minh. |
| 35 | **expert\_availability** | Các khoảng thời gian chuyên gia mở để nhận tư vấn, gồm hình thức hỗ trợ và trạng thái slot; giá được quản lý riêng theo phiên bản trong expert\_consultation\_prices. |
| 36 | **expert\_location\_shares** | Vị trí/khu vực hoạt động do chuyên gia chủ động chia sẻ theo consent và thời hạn để hỗ trợ tìm kiếm gần vị trí. |
| 37 | **consultation\_bookings** | Yêu cầu đặt tư vấn giữa người dùng và chuyên gia; khóa bản chụp giá, tỷ lệ hoa hồng và chính sách hủy tại thời điểm xác nhận/thanh toán để không bị ảnh hưởng bởi thay đổi giá sau này. |
| 38 | **consultation\_sessions** | Phiên chat/gọi thoại/video được tạo từ booking hợp lệ. |
| 39 | **consultation\_messages** | Tin nhắn trong phiên tư vấn; nội dung chỉ thuộc về hai bên và được bảo vệ quyền truy cập. |
| 40 | **payment\_transactions** | Giao dịch thanh toán cho booking theo price snapshot, lưu giá gộp, phí cổng, số tiền hoàn, số tiền thực thu và trạng thái giao dịch. |
| 41 | **commission\_records** | Kết quả tính hoa hồng sau khi phiên đủ điều kiện hoàn thành, gồm giá gốc, tỷ lệ/tiền hoa hồng, phí cổng, hoàn tiền, số chuyên gia thực nhận và trạng thái đối soát. |
| 42 | **expert\_reviews** | Đánh giá chuyên gia chỉ được tạo sau booking hợp lệ/hoàn tất. |
| 43 | **contribution\_points** | Lịch sử điểm đóng góp/huy hiệu của người dùng hoặc chuyên gia từ hoạt động cộng đồng. |
| 44 | **consultation\_price\_bands** | Phiên bản khung giá do CareBridge cấu hình theo hình thức, thời lượng và phạm vi chuyên môn; chứa mức tối thiểu, tối đa, tỷ lệ hoa hồng và thời gian hiệu lực. |
| 45 | **expert\_consultation\_prices** | Bảng giá theo phiên bản do từng chuyên gia thiết lập trong khung cho phép; giá mới chỉ áp dụng cho booking tương lai và không hồi tố booking đã khóa giá. |
| 46 | **consultation\_disputes** | Hồ sơ khiếu nại liên quan đến booking tư vấn, lưu lý do, bằng chứng, trạng thái xử lý và quyết định của đội vận hành. |
| 47 | **refund\_records** | Bản ghi hoàn tiền gắn với giao dịch và khiếu nại; lưu số tiền, trạng thái xử lý và mã hoàn tiền của cổng thanh toán. |
| 48 | **settlement\_records** | Bản ghi đối soát cho chuyên gia theo kỳ hoặc theo khoản hoa hồng, lưu giá trị gộp, hoa hồng, phí cổng, hoàn tiền, số thực nhận và trạng thái thanh toán. |
| 49 | **triage\_assessments** | Phiên intake triệu chứng có cấu trúc và kết quả phân loại rủi ro xanh/vàng/đỏ; không phải chẩn đoán. |
| 50 | **triage\_answers** | Câu trả lời từng bước trong một phiên intake triệu chứng. |
| 51 | **partner\_organizations** | Hồ sơ phòng khám, cơ sở y tế, tổ chức xã hội hoặc nhà tài trợ đã đăng ký/được duyệt. |
| 52 | **partner\_expert\_links** | Thực thể trung gian giữa partner\_organizations và expert\_profiles; liên kết chỉ có hiệu lực sau duyệt. |
| 53 | **partner\_services** | Dịch vụ/lịch khám tham khảo do đối tác gửi và admin duyệt để hiển thị công khai. |
| 54 | **sponsored\_campaigns** | Chiến dịch/nội dung tài trợ do đối tác gửi, bắt buộc gắn nhãn và kiểm duyệt. |
| 55 | **care\_facilities** | Danh mục cơ sở y tế dùng trong bản đồ khẩn cấp/tìm kiếm gần nhất; có thể đến từ đối tác hoặc nguồn bản đồ. |
| 56 | **emergency\_events** | Luồng khẩn cấp/tìm hỗ trợ được người dùng hoặc hệ thống mở từ triage, nút khẩn cấp hoặc sự kiện IMU. |
| 57 | **location\_snapshots** | Ảnh chụp vị trí tối thiểu có thời hạn dùng cho emergency, family alert hoặc tìm kiếm lân cận. |
| 58 | **health\_device\_connections** | Kết nối tùy chọn với smartwatch/health platform; lưu token tham chiếu an toàn và trạng thái consent. |
| 59 | **device\_measurements** | Chỉ số từ thiết bị/health platform có nhãn nguồn và độ tin cậy; dùng xem xu hướng, không thay đo lâm sàng. |
| 60 | **safety\_monitoring\_settings** | Cấu hình bật/tắt giám sát IMU, countdown, người nhận cảnh báo và consent vị trí. |
| 61 | **safety\_events** | Sự kiện nghi ngờ ngã/va chạm mạnh được phát hiện từ IMU; chỉ lưu metadata tối thiểu, không khẳng định chấn thương. |
| 62 | **safety\_alerts** | Cảnh báo tối thiểu gửi cho người thân sau khi người dùng yêu cầu trợ giúp hoặc không phản hồi. |
| 63 | **pregnancy\_exercises** | Danh mục bài tập thai kỳ đã được kiểm duyệt, có phạm vi tam cá nguyệt, mức độ khó, thời lượng, hướng dẫn, cảnh báo an toàn và trạng thái phiên bản. |
| 64 | **exercise\_safety\_checks** | Kết quả kiểm tra an toàn trước tập của Mẹ cho một bài tập và hành trình cụ thể; lưu câu trả lời có cấu trúc, red flag và quyết định cho phép hoặc chặn bắt đầu. |
| 65 | **exercise\_sessions** | Phiên luyện tập thai kỳ của Mẹ; lưu thời gian, trạng thái tạm dừng/hoàn thành, mức hoàn thành, điểm tư thế tổng hợp và cảnh báo. |
| 66 | **posture\_analysis\_configs** | Cấu hình rule-based hoặc ML-based dùng để phân tích tư thế cho từng bài tập, gồm phiên bản rule/model, ngưỡng tin cậy và thời gian hiệu lực. |
| 67 | **posture\_feedback\_events** | Các phản hồi tư thế phát sinh trong phiên tập; chỉ lưu kết quả/keypoint tóm tắt cần thiết, không lưu video camera thô. |

# **2\. Data Dictionary**

Quy ước logic: UUID có thể triển khai bằng UUID/UNIQUEIDENTIFIER; TIMESTAMP có thể ánh xạ sang timestamp/datetime2; JSON có thể dùng jsonb hoặc NVARCHAR(MAX) kèm validation. Các CHECK/UNIQUE/FK chi tiết cần được hiện thực ở migration và test tích hợp.

## **Identity & Access**

### **1\. roles**

Danh mục vai trò hệ thống dùng cho RBAC như USER, MOTHER, FAMILY\_MEMBER, EXPERT, MODERATOR, CONTENT\_ADMIN, ADMIN và PARTNER.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | role\_id | Role Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi roles. |
| 2 | role\_code | Role Code | VARCHAR(50) | UNIQUE | Thông tin role code của bản ghi. |
| 3 | role\_name | Role Name | VARCHAR(100) | NULL allowed / business validation | Thông tin role name của bản ghi. |
| 4 | description | Mô tả | VARCHAR(500) | NULL allowed / business validation | Thông tin description của bản ghi. |
| 5 | is\_active | Is Active | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is active. |
| 6 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 7 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **2\. users**

Tài khoản gốc của mọi người dùng CareBridge; lưu dữ liệu định danh và trạng thái tài khoản, không trộn với hồ sơ sức khỏe hoặc hồ sơ cộng đồng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | user\_id | Mã người dùng | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi users. |
| 2 | email | Email | VARCHAR(255) | UNIQUE | Thông tin email của bản ghi. |
| 3 | phone | Phone | VARCHAR(30) | NULL allowed / business validation | Thông tin phone của bản ghi. |
| 4 | password\_hash | Password Hash | VARCHAR(255) | NULL allowed / business validation | Thông tin password hash của bản ghi. |
| 5 | full\_name | Full Name | VARCHAR(150) | NULL allowed / business validation | Thông tin full name của bản ghi. |
| 6 | avatar\_url | Avatar Url | VARCHAR(500) | NULL allowed / business validation | Thông tin avatar url của bản ghi. |
| 7 | account\_status | Account Status | VARCHAR(30) | NULL allowed / business validation | Trạng thái hiện tại của account status. |
| 8 | email\_verified | Email Verified | BOOLEAN | NULL allowed / business validation | Thông tin email verified của bản ghi. |
| 9 | phone\_verified | Phone Verified | BOOLEAN | NULL allowed / business validation | Thông tin phone verified của bản ghi. |
| 10 | last\_login\_at | Last Login At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của last login at. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **3\. user\_roles**

Thực thể trung gian giải quyết quan hệ nhiều-nhiều giữa users và roles.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | user\_role\_id | User Role Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi user\_roles. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | role\_id | Role Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường role\_id. |
| 4 | assigned\_by | Assigned By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường assigned\_by. |
| 5 | assigned\_at | Assigned At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của assigned at. |
| 6 | expires\_at | Expires At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của expires at. |
| 7 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 8 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 9 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **4\. user\_sessions**

Phiên đăng nhập và refresh token của người dùng để hỗ trợ xem/thu hồi phiên trên thiết bị khác.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | session\_id | Session Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi user\_sessions. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | refresh\_token\_hash | Refresh Token Hash | VARCHAR(255) | NULL allowed / business validation | Thông tin refresh token hash của bản ghi. |
| 4 | device\_name | Device Name | VARCHAR(150) | NULL allowed / business validation | Thông tin device name của bản ghi. |
| 5 | ip\_address | Ip Address | VARCHAR(64) | NULL allowed / business validation | Thông tin ip address của bản ghi. |
| 6 | expires\_at | Expires At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của expires at. |
| 7 | revoked\_at | Revoked At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của revoked at. |
| 8 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **5\. community\_profiles**

Hồ sơ hiển thị công khai trong cộng đồng, tách biệt khỏi hồ sơ định danh và hồ sơ sức khỏe.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | community\_profile\_id | Community Profile Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi community\_profiles. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | display\_name | Display Name | VARCHAR(100) | NULL allowed / business validation | Thông tin display name của bản ghi. |
| 4 | public\_avatar\_url | Public Avatar Url | VARCHAR(500) | NULL allowed / business validation | Thông tin public avatar url của bản ghi. |
| 5 | interest\_stage | Interest Stage | VARCHAR(30) | NULL allowed / business validation | Thông tin interest stage của bản ghi. |
| 6 | region | Region | VARCHAR(120) | NULL allowed / business validation | Thông tin region của bản ghi. |
| 7 | bio | Bio | VARCHAR(500) | NULL allowed / business validation | Thông tin bio của bản ghi. |
| 8 | is\_visible | Is Visible | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is visible. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **6\. notification\_preferences**

Tùy chọn kênh và loại thông báo theo từng người dùng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | preference\_id | Preference Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi notification\_preferences. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | notification\_type | Notification Type | VARCHAR(50) | NULL allowed / business validation | Thông tin notification type của bản ghi. |
| 4 | in\_app\_enabled | In App Enabled | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho in app enabled. |
| 5 | push\_enabled | Push Enabled | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho push enabled. |
| 6 | email\_enabled | Email Enabled | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho email enabled. |
| 7 | quiet\_hours\_start | Quiet Hours Start | TIME | NULL allowed / business validation | Thông tin quiet hours start của bản ghi. |
| 8 | quiet\_hours\_end | Quiet Hours End | TIME | NULL allowed / business validation | Thông tin quiet hours end của bản ghi. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **7\. notifications**

Thông báo cá nhân do hệ thống tạo từ reminder, community, consultation, safety hoặc vận hành.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | notification\_id | Notification Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi notifications. |
| 2 | recipient\_user\_id | Recipient User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường recipient\_user\_id. |
| 3 | notification\_type | Notification Type | VARCHAR(50) | NULL allowed / business validation | Thông tin notification type của bản ghi. |
| 4 | title | Tiêu đề | VARCHAR(200) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 5 | body | Body | TEXT | NULL allowed / business validation | Thông tin body của bản ghi. |
| 6 | reference\_type | Reference Type | VARCHAR(50) | NULL allowed / business validation | Thông tin reference type của bản ghi. |
| 7 | reference\_id | Reference Id | UUID | NULL allowed / business validation | Thông tin reference id của bản ghi. |
| 8 | is\_read | Is Read | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is read. |
| 9 | sent\_at | Sent At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của sent at. |
| 10 | delivery\_status | Delivery Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của delivery status. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **8\. data\_permissions**

Quyền chia sẻ dữ liệu do người dùng cấp cho người thân hoặc chuyên gia theo phạm vi, mục đích và thời hạn.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | permission\_id | Permission Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi data\_permissions. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | grantee\_user\_id | Grantee User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường grantee\_user\_id. |
| 4 | scope\_type | Scope Type | VARCHAR(50) | NULL allowed / business validation | Thông tin scope type của bản ghi. |
| 5 | scope\_reference\_id | Scope Reference Id | UUID | NULL allowed / business validation | Thông tin scope reference id của bản ghi. |
| 6 | purpose | Purpose | VARCHAR(255) | NULL allowed / business validation | Thông tin purpose của bản ghi. |
| 7 | granted\_at | Granted At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của granted at. |
| 8 | expires\_at | Expires At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của expires at. |
| 9 | revoked\_at | Revoked At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của revoked at. |
| 10 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **9\. audit\_logs**

Nhật ký bất biến của hành động quan trọng: đăng nhập, xem hồ sơ, thay đổi quyền, moderation, tư vấn, thanh toán và sự kiện an toàn.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | audit\_log\_id | Audit Log Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi audit\_logs. |
| 2 | actor\_user\_id | Actor User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường actor\_user\_id. |
| 3 | action | Action | VARCHAR(100) | NULL allowed / business validation | Thông tin action của bản ghi. |
| 4 | entity\_type | Entity Type | VARCHAR(80) | NULL allowed / business validation | Thông tin entity type của bản ghi. |
| 5 | entity\_id | Entity Id | UUID | NULL allowed / business validation | Thông tin entity id của bản ghi. |
| 6 | old\_value\_json | Old Value Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ old value json. |
| 7 | new\_value\_json | New Value Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ new value json. |
| 8 | ip\_address | Ip Address | VARCHAR(64) | NULL allowed / business validation | Thông tin ip address của bản ghi. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |

## **Care Journey**

### **10\. mother\_journeys**

Hành trình của mẹ theo trạng thái trước mang thai, thai kỳ hoặc sau sinh; một người dùng có thể có nhiều hành trình theo thời gian.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | journey\_id | Mã hành trình | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi mother\_journeys. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | journey\_type | Journey Type | VARCHAR(30) | NULL allowed / business validation | Thông tin journey type của bản ghi. |
| 4 | start\_date | Start Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của start date. |
| 5 | last\_menstrual\_date | Last Menstrual Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của last menstrual date. |
| 6 | estimated\_due\_date | Estimated Due Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của estimated due date. |
| 7 | delivery\_date | Delivery Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của delivery date. |
| 8 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 9 | notes | Notes | TEXT | NULL allowed / business validation | Thông tin notes của bản ghi. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **11\. maternal\_health\_metrics**

Chỉ số sức khỏe mẹ được nhập thủ công hoặc đồng bộ có gắn nhãn nguồn.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | metric\_id | Metric Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi maternal\_health\_metrics. |
| 2 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 3 | metric\_type | Metric Type | VARCHAR(40) | NULL allowed / business validation | Thông tin metric type của bản ghi. |
| 4 | value\_numeric | Value Numeric | DECIMAL(12,3) | NULL allowed / business validation | Thông tin value numeric của bản ghi. |
| 5 | value\_secondary | Value Secondary | DECIMAL(12,3) | NULL allowed / business validation | Thông tin value secondary của bản ghi. |
| 6 | unit | Unit | VARCHAR(30) | NULL allowed / business validation | Thông tin unit của bản ghi. |
| 7 | measured\_at | Measured At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của measured at. |
| 8 | source\_type | Source Type | VARCHAR(30) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 9 | source\_reference\_id | Source Reference Id | UUID | NULL allowed / business validation | Thông tin source reference id của bản ghi. |
| 10 | note | Ghi chú | VARCHAR(500) | NULL allowed / business validation | Thông tin note của bản ghi. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **12\. postpartum\_logs**

Nhật ký phục hồi sau sinh về sản dịch, đau, sữa mẹ, giấc ngủ, tâm trạng và triệu chứng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | postpartum\_log\_id | Postpartum Log Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi postpartum\_logs. |
| 2 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 3 | log\_date | Log Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của log date. |
| 4 | pain\_level | Pain Level | SMALLINT | CHECK | Thông tin pain level của bản ghi. |
| 5 | bleeding\_level | Bleeding Level | VARCHAR(20) | NULL allowed / business validation | Thông tin bleeding level của bản ghi. |
| 6 | mood\_level | Mood Level | SMALLINT | CHECK | Thông tin mood level của bản ghi. |
| 7 | sleep\_hours | Sleep Hours | DECIMAL(4,1) | NULL allowed / business validation | Thông tin sleep hours của bản ghi. |
| 8 | breastfeeding\_note | Breastfeeding Note | TEXT | NULL allowed / business validation | Giá trị tiền tệ của breastfeeding note. |
| 9 | symptom\_note | Symptom Note | TEXT | NULL allowed / business validation | Thông tin symptom note của bản ghi. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Baby Care**

### **13\. baby\_profiles**

Hồ sơ trẻ do mẹ/người chăm sóc tạo và quản lý.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | baby\_id | Baby Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi baby\_profiles. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | related\_journey\_id | Related Journey Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường related\_journey\_id. |
| 4 | nickname | Nickname | VARCHAR(100) | NULL allowed / business validation | Thông tin nickname của bản ghi. |
| 5 | birth\_date | Birth Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của birth date. |
| 6 | sex | Sex | VARCHAR(20) | NULL allowed / business validation | Thông tin sex của bản ghi. |
| 7 | birth\_weight\_kg | Birth Weight Kg | DECIMAL(5,2) | NULL allowed / business validation | Thông tin birth weight kg của bản ghi. |
| 8 | birth\_length\_cm | Birth Length Cm | DECIMAL(5,2) | NULL allowed / business validation | Thông tin birth length cm của bản ghi. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **14\. baby\_daily\_logs**

Nhật ký hằng ngày của bé: bú, ngủ, tã, nhiệt độ, nôn trớ, thuốc theo chỉ định và ghi chú.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | baby\_log\_id | Baby Log Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi baby\_daily\_logs. |
| 2 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 3 | log\_type | Log Type | VARCHAR(30) | NULL allowed / business validation | Thông tin log type của bản ghi. |
| 4 | started\_at | Started At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của started at. |
| 5 | ended\_at | Ended At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của ended at. |
| 6 | quantity | Quantity | DECIMAL(10,2) | NULL allowed / business validation | Thông tin quantity của bản ghi. |
| 7 | unit | Unit | VARCHAR(20) | NULL allowed / business validation | Thông tin unit của bản ghi. |
| 8 | note | Ghi chú | TEXT | NULL allowed / business validation | Thông tin note của bản ghi. |
| 9 | recorded\_by | Recorded By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường recorded\_by. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **15\. development\_milestones**

Mốc phát triển của trẻ như lẫy, bò, đi, nói, mọc răng hoặc ăn dặm.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | milestone\_id | Milestone Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi development\_milestones. |
| 2 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 3 | milestone\_type | Milestone Type | VARCHAR(60) | NULL allowed / business validation | Thông tin milestone type của bản ghi. |
| 4 | achieved\_date | Achieved Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của achieved date. |
| 5 | note | Ghi chú | TEXT | NULL allowed / business validation | Thông tin note của bản ghi. |
| 6 | source\_type | Source Type | VARCHAR(30) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 7 | recorded\_by | Recorded By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường recorded\_by. |
| 8 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 9 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **16\. growth\_measurements**

Các lần đo cân nặng, chiều cao và vòng đầu để tạo biểu đồ tăng trưởng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | growth\_measurement\_id | Growth Measurement Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi growth\_measurements. |
| 2 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 3 | measured\_date | Measured Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của measured date. |
| 4 | weight\_kg | Weight Kg | DECIMAL(5,2) | NULL allowed / business validation | Thông tin weight kg của bản ghi. |
| 5 | height\_cm | Height Cm | DECIMAL(5,2) | NULL allowed / business validation | Thông tin height cm của bản ghi. |
| 6 | head\_circumference\_cm | Head Circumference Cm | DECIMAL(5,2) | NULL allowed / business validation | Thông tin head circumference cm của bản ghi. |
| 7 | source\_type | Source Type | VARCHAR(30) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 8 | note | Ghi chú | VARCHAR(500) | NULL allowed / business validation | Thông tin note của bản ghi. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **17\. vaccination\_records**

Lịch và trạng thái tiêm chủng của trẻ; là dữ liệu theo dõi cá nhân, không thay thế sổ tiêm chính thức.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | vaccination\_record\_id | Vaccination Record Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi vaccination\_records. |
| 2 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 3 | vaccine\_name | Vaccine Name | VARCHAR(200) | NULL allowed / business validation | Thông tin vaccine name của bản ghi. |
| 4 | dose\_number | Dose Number | VARCHAR(30) | NULL allowed / business validation | Thông tin dose number của bản ghi. |
| 5 | scheduled\_date | Scheduled Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của scheduled date. |
| 6 | administered\_date | Administered Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của administered date. |
| 7 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 8 | facility\_name | Facility Name | VARCHAR(255) | NULL allowed / business validation | Thông tin facility name của bản ghi. |
| 9 | proof\_record\_id | Proof Record Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường proof\_record\_id. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Health Records**

### **18\. health\_records**

Kho hồ sơ sức khỏe cá nhân của mẹ/bé gồm ảnh siêu âm, xét nghiệm, đơn thuốc, phiếu tiêm và kết quả khám, có nhãn nguồn.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | health\_record\_id | Health Record Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi health\_records. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 5 | record\_type | Record Type | VARCHAR(50) | NULL allowed / business validation | Thông tin record type của bản ghi. |
| 6 | title | Tiêu đề | VARCHAR(200) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 7 | file\_url | File Url | VARCHAR(500) | NULL allowed / business validation | Thông tin file url của bản ghi. |
| 8 | record\_date | Record Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của record date. |
| 9 | source\_type | Source Type | VARCHAR(30) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 10 | source\_name | Source Name | VARCHAR(255) | NULL allowed / business validation | Thông tin source name của bản ghi. |
| 11 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **19\. health\_summaries**

Bản tóm tắt dữ liệu sức khỏe theo khoảng thời gian do người dùng tạo để xem hoặc chia sẻ có thời hạn.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | summary\_id | Summary Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi health\_summaries. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 5 | summary\_period | Summary Period | VARCHAR(30) | NULL allowed / business validation | Thông tin summary period của bản ghi. |
| 6 | period\_start | Period Start | TIMESTAMP | NULL allowed / business validation | Thông tin period start của bản ghi. |
| 7 | period\_end | Period End | TIMESTAMP | NULL allowed / business validation | Thông tin period end của bản ghi. |
| 8 | summary\_json | Summary Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ summary json. |
| 9 | generated\_by | Generated By | VARCHAR(20) | NULL allowed / business validation | Thông tin generated by của bản ghi. |
| 10 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Care Coordination**

### **20\. reminders**

Nhắc lịch khám, thuốc/vitamin, tiêm, checklist và việc chăm sóc.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | reminder\_id | Reminder Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi reminders. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 5 | reminder\_type | Reminder Type | VARCHAR(40) | NULL allowed / business validation | Thông tin reminder type của bản ghi. |
| 6 | title | Tiêu đề | VARCHAR(200) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 7 | scheduled\_at | Scheduled At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của scheduled at. |
| 8 | recurrence\_rule | Recurrence Rule | VARCHAR(255) | NULL allowed / business validation | Thông tin recurrence rule của bản ghi. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 10 | snoozed\_until | Snoozed Until | TIMESTAMP | NULL allowed / business validation | Thông tin snoozed until của bản ghi. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **21\. care\_groups**

Nhóm chăm sóc do mẹ/người quản lý tạo cho một hành trình hoặc một bé.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | care\_group\_id | Care Group Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi care\_groups. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 5 | group\_name | Group Name | VARCHAR(150) | NULL allowed / business validation | Thông tin group name của bản ghi. |
| 6 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 7 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 8 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **22\. care\_group\_members**

Thực thể trung gian giữa care\_groups và users; lưu trạng thái lời mời và quyền thành viên.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | care\_group\_member\_id | Care Group Member Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi care\_group\_members. |
| 2 | care\_group\_id | Care Group Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường care\_group\_id. |
| 3 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 4 | member\_role | Member Role | VARCHAR(30) | NULL allowed / business validation | Thông tin member role của bản ghi. |
| 5 | invitation\_status | Invitation Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của invitation status. |
| 6 | permission\_json | Permission Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ permission json. |
| 7 | joined\_at | Joined At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của joined at. |
| 8 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 9 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **23\. care\_tasks**

Việc chăm sóc được giao cho thành viên nhóm, có hạn và trạng thái thực hiện.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | care\_task\_id | Care Task Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi care\_tasks. |
| 2 | care\_group\_id | Care Group Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường care\_group\_id. |
| 3 | assigned\_by | Assigned By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường assigned\_by. |
| 4 | assigned\_to | Assigned To | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường assigned\_to. |
| 5 | title | Tiêu đề | VARCHAR(200) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 6 | description | Mô tả | TEXT | NULL allowed / business validation | Thông tin description của bản ghi. |
| 7 | due\_at | Due At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của due at. |
| 8 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 9 | completed\_at | Completed At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của completed at. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **24\. expenses**

Khoản chi liên quan thai kỳ, sau sinh hoặc chăm sóc trẻ.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | expense\_id | Expense Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expenses. |
| 2 | owner\_user\_id | Owner User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường owner\_user\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 5 | category | Category | VARCHAR(40) | NULL allowed / business validation | Thông tin category của bản ghi. |
| 6 | amount | Amount | DECIMAL(15,2) | NULL allowed / business validation | Giá trị tiền tệ của amount. |
| 7 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 8 | expense\_date | Expense Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của expense date. |
| 9 | note | Ghi chú | VARCHAR(500) | NULL allowed / business validation | Thông tin note của bản ghi. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Community & Content**

### **25\. community\_topics**

Danh mục chủ đề cộng đồng dùng để phân loại câu hỏi và nội dung.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | topic\_id | Topic Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi community\_topics. |
| 2 | name | Name | VARCHAR(150) | NULL allowed / business validation | Thông tin name của bản ghi. |
| 3 | slug | Slug | VARCHAR(160) | NULL allowed / business validation | Thông tin slug của bản ghi. |
| 4 | description | Mô tả | VARCHAR(500) | NULL allowed / business validation | Thông tin description của bản ghi. |
| 5 | risk\_level | Risk Level | VARCHAR(20) | NULL allowed / business validation | Thông tin risk level của bản ghi. |
| 6 | is\_active | Is Active | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is active. |
| 7 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 8 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **26\. community\_questions**

Câu hỏi do người dùng đăng, có thể ẩn danh hiển thị nhưng luôn truy vết nội bộ.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | question\_id | Question Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi community\_questions. |
| 2 | author\_user\_id | Author User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường author\_user\_id. |
| 3 | topic\_id | Topic Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường topic\_id. |
| 4 | title | Tiêu đề | VARCHAR(250) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 5 | content | Content | TEXT | NULL allowed / business validation | Thông tin content của bản ghi. |
| 6 | is\_anonymous | Is Anonymous | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is anonymous. |
| 7 | urgency\_level | Urgency Level | VARCHAR(20) | NULL allowed / business validation | Thông tin urgency level của bản ghi. |
| 8 | moderation\_status | Moderation Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của moderation status. |
| 9 | published\_at | Published At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của published at. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **27\. community\_answers**

Câu trả lời của người dùng hoặc chuyên gia; câu trả lời chuyên gia gắn badge/phạm vi chuyên môn.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | answer\_id | Answer Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi community\_answers. |
| 2 | question\_id | Question Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường question\_id. |
| 3 | author\_user\_id | Author User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường author\_user\_id. |
| 4 | content | Content | TEXT | NULL allowed / business validation | Thông tin content của bản ghi. |
| 5 | answer\_type | Answer Type | VARCHAR(20) | NULL allowed / business validation | Thông tin answer type của bản ghi. |
| 6 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 7 | moderation\_status | Moderation Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của moderation status. |
| 8 | helpful\_count | Helpful Count | INT | NULL allowed / business validation | Thông tin helpful count của bản ghi. |
| 9 | published\_at | Published At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của published at. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **28\. content\_reports**

Báo cáo nội dung hoặc tài khoản vi phạm do người dùng gửi.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | report\_id | Report Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi content\_reports. |
| 2 | reporter\_user\_id | Reporter User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường reporter\_user\_id. |
| 3 | target\_type | Target Type | VARCHAR(30) | NULL allowed / business validation | Thông tin target type của bản ghi. |
| 4 | target\_id | Target Id | UUID | NULL allowed / business validation | Thông tin target id của bản ghi. |
| 5 | reason\_code | Reason Code | VARCHAR(40) | NULL allowed / business validation | Thông tin reason code của bản ghi. |
| 6 | description | Mô tả | TEXT | NULL allowed / business validation | Thông tin description của bản ghi. |
| 7 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 8 | assigned\_moderator\_id | Assigned Moderator Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường assigned\_moderator\_id. |
| 9 | resolved\_at | Resolved At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của resolved at. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **29\. moderation\_actions**

Hành động kiểm duyệt đối với báo cáo hoặc nội dung: duyệt, ẩn, khóa, cảnh cáo, tạm ngưng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | moderation\_action\_id | Moderation Action Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi moderation\_actions. |
| 2 | report\_id | Report Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường report\_id. |
| 3 | moderator\_user\_id | Moderator User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường moderator\_user\_id. |
| 4 | target\_type | Target Type | VARCHAR(30) | NULL allowed / business validation | Thông tin target type của bản ghi. |
| 5 | target\_id | Target Id | UUID | NULL allowed / business validation | Thông tin target id của bản ghi. |
| 6 | action\_type | Action Type | VARCHAR(30) | NULL allowed / business validation | Thông tin action type của bản ghi. |
| 7 | reason | Reason | TEXT | NULL allowed / business validation | Thông tin reason của bản ghi. |
| 8 | action\_at | Action At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của action at. |
| 9 | expires\_at | Expires At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của expires at. |

### **30\. content\_items**

Bài viết, FAQ hoặc nội dung giáo dục đã được quản trị nội dung tạo và kiểm duyệt.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | content\_item\_id | Content Item Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi content\_items. |
| 2 | topic\_id | Topic Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường topic\_id. |
| 3 | author\_user\_id | Author User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường author\_user\_id. |
| 4 | content\_type | Content Type | VARCHAR(30) | NULL allowed / business validation | Thông tin content type của bản ghi. |
| 5 | title | Tiêu đề | VARCHAR(250) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 6 | body | Body | TEXT | NULL allowed / business validation | Thông tin body của bản ghi. |
| 7 | source\_label | Source Label | VARCHAR(255) | NULL allowed / business validation | Thông tin source label của bản ghi. |
| 8 | version\_no | Version No | INT | NULL allowed / business validation | Số phiên bản tăng dần để truy vết thay đổi. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 10 | published\_at | Published At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của published at. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **31\. checklist\_templates**

Mẫu checklist chuẩn bị theo giai đoạn được kiểm duyệt.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | checklist\_template\_id | Checklist Template Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi checklist\_templates. |
| 2 | content\_item\_id | Content Item Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường content\_item\_id. |
| 3 | name | Name | VARCHAR(200) | NULL allowed / business validation | Thông tin name của bản ghi. |
| 4 | stage | Stage | VARCHAR(30) | NULL allowed / business validation | Thông tin stage của bản ghi. |
| 5 | version\_no | Version No | INT | NULL allowed / business validation | Số phiên bản tăng dần để truy vết thay đổi. |
| 6 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 7 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 8 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **32\. checklist\_items**

Các mục con thuộc một checklist template.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | checklist\_item\_id | Checklist Item Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi checklist\_items. |
| 2 | checklist\_template\_id | Checklist Template Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường checklist\_template\_id. |
| 3 | item\_order | Item Order | INT | NULL allowed / business validation | Thông tin item order của bản ghi. |
| 4 | item\_text | Item Text | VARCHAR(500) | NULL allowed / business validation | Thông tin item text của bản ghi. |
| 5 | is\_required | Is Required | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is required. |
| 6 | note | Ghi chú | VARCHAR(500) | NULL allowed / business validation | Thông tin note của bản ghi. |
| 7 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 8 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Expert & Consultation**

### **33\. expert\_profiles**

Hồ sơ chuyên môn của bác sĩ/chuyên gia đã đăng ký; chỉ có badge xác thực sau khi admin duyệt.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expert\_profiles. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | specialty | Specialty | VARCHAR(150) | NULL allowed / business validation | Thông tin specialty của bản ghi. |
| 4 | professional\_title | Professional Title | VARCHAR(150) | NULL allowed / business validation | Thông tin professional title của bản ghi. |
| 5 | experience\_years | Experience Years | INT | NULL allowed / business validation | Thông tin experience years của bản ghi. |
| 6 | workplace | Workplace | VARCHAR(255) | NULL allowed / business validation | Thông tin workplace của bản ghi. |
| 7 | consultation\_scope | Consultation Scope | TEXT | NULL allowed / business validation | Thông tin consultation scope của bản ghi. |
| 8 | verification\_status | Verification Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của verification status. |
| 9 | verified\_at | Verified At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của verified at. |
| 10 | verified\_by | Verified By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường verified\_by. |
| 11 | rating\_avg | Rating Avg | DECIMAL(3,2) | NULL allowed / business validation | Thông tin rating avg của bản ghi. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **34\. expert\_credentials**

Giấy tờ/chứng chỉ chuyên gia được tải lên để xác minh.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | credential\_id | Credential Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expert\_credentials. |
| 2 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 3 | credential\_type | Credential Type | VARCHAR(50) | NULL allowed / business validation | Thông tin credential type của bản ghi. |
| 4 | credential\_number | Credential Number | VARCHAR(120) | NULL allowed / business validation | Thông tin credential number của bản ghi. |
| 5 | issuer | Issuer | VARCHAR(255) | NULL allowed / business validation | Thông tin issuer của bản ghi. |
| 6 | issued\_date | Issued Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của issued date. |
| 7 | expiry\_date | Expiry Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của expiry date. |
| 8 | file\_url | File Url | VARCHAR(500) | NULL allowed / business validation | Thông tin file url của bản ghi. |
| 9 | review\_status | Review Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của review status. |
| 10 | review\_note | Review Note | TEXT | NULL allowed / business validation | Thông tin review note của bản ghi. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **35\. expert\_availability**

Các khoảng thời gian chuyên gia mở để nhận tư vấn, gồm hình thức hỗ trợ và trạng thái slot; giá được quản lý riêng theo phiên bản trong expert\_consultation\_prices.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | availability\_id | Availability Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expert\_availability. |
| 2 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 3 | start\_at | Start At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của start at. |
| 4 | end\_at | End At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của end at. |
| 5 | channel\_type | Channel Type | VARCHAR(20) | NULL allowed / business validation | Thông tin channel type của bản ghi. |
| 6 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 7 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 8 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **36\. expert\_location\_shares**

Vị trí/khu vực hoạt động do chuyên gia chủ động chia sẻ theo consent và thời hạn để hỗ trợ tìm kiếm gần vị trí.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | location\_share\_id | Location Share Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expert\_location\_shares. |
| 2 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 3 | latitude | Latitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin latitude của bản ghi. |
| 4 | longitude | Longitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin longitude của bản ghi. |
| 5 | accuracy\_meters | Accuracy Meters | DECIMAL(10,2) | NULL allowed / business validation | Thông tin accuracy meters của bản ghi. |
| 6 | availability\_status | Availability Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của availability status. |
| 7 | shared\_at | Shared At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của shared at. |
| 8 | expires\_at | Expires At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của expires at. |
| 9 | consent\_reference | Consent Reference | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường consent\_reference. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **37\. consultation\_bookings**

Yêu cầu đặt tư vấn giữa người dùng và chuyên gia; khóa bản chụp giá, tỷ lệ hoa hồng và chính sách hủy tại thời điểm xác nhận/thanh toán để không bị ảnh hưởng bởi thay đổi giá sau này.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | booking\_id | Mã booking | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi consultation\_bookings. |
| 2 | requester\_user\_id | Requester User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường requester\_user\_id. |
| 3 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 4 | availability\_id | Availability Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường availability\_id. |
| 5 | expert\_price\_id | Expert Price Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_price\_id. |
| 6 | shared\_summary\_id | Shared Summary Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường shared\_summary\_id. |
| 7 | topic | Topic | VARCHAR(200) | NULL allowed / business validation | Thông tin topic của bản ghi. |
| 8 | channel\_type | Channel Type | VARCHAR(20) | NULL allowed / business validation | Thông tin channel type của bản ghi. |
| 9 | duration\_minutes | Duration Minutes | INT | NULL allowed / business validation | Thông tin duration minutes của bản ghi. |
| 10 | scheduled\_start | Scheduled Start | TIMESTAMP | NULL allowed / business validation | Thông tin scheduled start của bản ghi. |
| 11 | scheduled\_end | Scheduled End | TIMESTAMP | NULL allowed / business validation | Thông tin scheduled end của bản ghi. |
| 12 | price\_snapshot\_amount | Price Snapshot Amount | DECIMAL(15,2) | NULL allowed / business validation | Mức giá của gói tư vấn được khóa cho booking; không thay đổi khi chuyên gia cập nhật giá sau đó. |
| 13 | commission\_rate\_snapshot | Commission Rate Snapshot | DECIMAL(5,4) | CHECK 0..1 | Tỷ lệ hoa hồng có hiệu lực được chụp tại booking/thanh toán. |
| 14 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 15 | cancellation\_policy\_snapshot | Cancellation Policy Snapshot | TEXT | NULL allowed / business validation | Nội dung chính sách hủy/hoàn tiền tại thời điểm booking được khóa giá. |
| 16 | price\_locked\_at | Price Locked At | TIMESTAMP | NULL allowed / business validation | Thời điểm bản chụp giá trở thành bất biến đối với booking. |
| 17 | status | Trạng thái | VARCHAR(25) | NOT NULL | Trạng thái hiện tại của status. |
| 18 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 19 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **38\. consultation\_sessions**

Phiên chat/gọi thoại/video được tạo từ booking hợp lệ.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | session\_id | Session Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi consultation\_sessions. |
| 2 | booking\_id | Mã booking | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường booking\_id. |
| 3 | communication\_room\_id | Communication Room Id | VARCHAR(255) | NULL allowed / business validation | Thông tin communication room id của bản ghi. |
| 4 | started\_at | Started At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của started at. |
| 5 | ended\_at | Ended At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của ended at. |
| 6 | session\_status | Session Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của session status. |
| 7 | expert\_summary | Expert Summary | TEXT | NULL allowed / business validation | Thông tin expert summary của bản ghi. |
| 8 | technical\_log\_json | Technical Log Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ technical log json. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **39\. consultation\_messages**

Tin nhắn trong phiên tư vấn; nội dung chỉ thuộc về hai bên và được bảo vệ quyền truy cập.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | message\_id | Message Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi consultation\_messages. |
| 2 | session\_id | Session Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường session\_id. |
| 3 | sender\_user\_id | Sender User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường sender\_user\_id. |
| 4 | message\_type | Message Type | VARCHAR(20) | NULL allowed / business validation | Thông tin message type của bản ghi. |
| 5 | message\_body | Message Body | TEXT | NULL allowed / business validation | Thông tin message body của bản ghi. |
| 6 | file\_url | File Url | VARCHAR(500) | NULL allowed / business validation | Thông tin file url của bản ghi. |
| 7 | sent\_at | Sent At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của sent at. |
| 8 | read\_at | Read At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của read at. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |

### **40\. payment\_transactions**

Giao dịch thanh toán cho booking theo price snapshot, lưu giá gộp, phí cổng, số tiền hoàn, số tiền thực thu và trạng thái giao dịch.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | payment\_id | Mã giao dịch | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi payment\_transactions. |
| 2 | booking\_id | Mã booking | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường booking\_id. |
| 3 | payer\_user\_id | Payer User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường payer\_user\_id. |
| 4 | gateway\_name | Gateway Name | VARCHAR(50) | NULL allowed / business validation | Thông tin gateway name của bản ghi. |
| 5 | gateway\_transaction\_id | Gateway Transaction Id | VARCHAR(150) | NULL allowed / business validation | Thông tin gateway transaction id của bản ghi. |
| 6 | gross\_amount | Gross Amount | DECIMAL(15,2) | CHECK \>= 0 | Giá trị tiền tệ của gross amount. |
| 7 | gateway\_fee | Gateway Fee | DECIMAL(15,2) | CHECK \>= 0 | Phí cổng thanh toán được ghi nhận cho giao dịch/đối soát. |
| 8 | refund\_amount | Refund Amount | DECIMAL(15,2) | CHECK \>= 0 | Tổng số tiền đã hoặc sẽ hoàn cho người dùng. |
| 9 | net\_paid\_amount | Net Paid Amount | DECIMAL(15,2) | NULL allowed / business validation | Giá trị tiền tệ của net paid amount. |
| 10 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 11 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 12 | paid\_at | Paid At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của paid at. |
| 13 | refunded\_at | Refunded At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của refunded at. |
| 14 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 15 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **41\. commission\_records**

Kết quả tính hoa hồng sau khi phiên đủ điều kiện hoàn thành, gồm giá gốc, tỷ lệ/tiền hoa hồng, phí cổng, hoàn tiền, số chuyên gia thực nhận và trạng thái đối soát.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | commission\_id | Commission Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi commission\_records. |
| 2 | payment\_id | Mã giao dịch | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường payment\_id. |
| 3 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 4 | original\_price | Original Price | DECIMAL(15,2) | CHECK \>= 0 | Giá gốc đã khóa của booking trước các khoản khấu trừ hoặc hoàn tiền. |
| 5 | commission\_rate | Commission Rate | DECIMAL(5,4) | CHECK 0..1 | Tỷ lệ hoa hồng nền tảng áp dụng cho giá tư vấn. |
| 6 | commission\_amount | Commission Amount | DECIMAL(15,2) | CHECK \>= 0 | Số tiền hoa hồng CareBridge được hưởng. |
| 7 | gateway\_fee | Gateway Fee | DECIMAL(15,2) | CHECK \>= 0 | Phí cổng thanh toán được ghi nhận cho giao dịch/đối soát. |
| 8 | refund\_amount | Refund Amount | DECIMAL(15,2) | CHECK \>= 0 | Tổng số tiền đã hoặc sẽ hoàn cho người dùng. |
| 9 | expert\_net\_amount | Expert Net Amount | DECIMAL(15,2) | CHECK \>= 0 | Số tiền chuyên gia thực nhận sau hoa hồng, phí và hoàn tiền. |
| 10 | eligible\_at | Eligible At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của eligible at. |
| 11 | settlement\_status | Settlement Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của settlement status. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **42\. expert\_reviews**

Đánh giá chuyên gia chỉ được tạo sau booking hợp lệ/hoàn tất.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | review\_id | Review Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expert\_reviews. |
| 2 | booking\_id | Mã booking | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường booking\_id. |
| 3 | reviewer\_user\_id | Reviewer User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường reviewer\_user\_id. |
| 4 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 5 | rating | Rating | SMALLINT | CHECK | Thông tin rating của bản ghi. |
| 6 | comment | Comment | TEXT | NULL allowed / business validation | Thông tin comment của bản ghi. |
| 7 | moderation\_status | Moderation Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của moderation status. |
| 8 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 9 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **43\. contribution\_points**

Lịch sử điểm đóng góp/huy hiệu của người dùng hoặc chuyên gia từ hoạt động cộng đồng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | point\_record\_id | Point Record Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi contribution\_points. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | source\_type | Source Type | VARCHAR(40) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 4 | source\_id | Source Id | UUID | NULL allowed / business validation | Thông tin source id của bản ghi. |
| 5 | points | Points | INT | NULL allowed / business validation | Thông tin points của bản ghi. |
| 6 | reason | Reason | VARCHAR(255) | NULL allowed / business validation | Thông tin reason của bản ghi. |
| 7 | recorded\_at | Recorded At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của recorded at. |

### **44\. consultation\_price\_bands**

Phiên bản khung giá do CareBridge cấu hình theo hình thức, thời lượng và phạm vi chuyên môn; chứa mức tối thiểu, tối đa, tỷ lệ hoa hồng và thời gian hiệu lực.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | price\_band\_id | Price Band Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi consultation\_price\_bands. |
| 2 | configured\_by | Configured By | UUID | FK | Người quản trị thực hiện cấu hình. |
| 3 | channel\_type | Channel Type | VARCHAR(20) | NULL allowed / business validation | Thông tin channel type của bản ghi. |
| 4 | duration\_minutes | Duration Minutes | INT | NULL allowed / business validation | Thông tin duration minutes của bản ghi. |
| 5 | specialty\_scope | Specialty Scope | VARCHAR(150) | NULL allowed / business validation | Thông tin specialty scope của bản ghi. |
| 6 | minimum\_price | Minimum Price | DECIMAL(15,2) | CHECK \>= 0 | Mức giá thấp nhất CareBridge cho phép trong khung đang hiệu lực. |
| 7 | maximum\_price | Maximum Price | DECIMAL(15,2) | CHECK \>= 0 | Mức giá cao nhất CareBridge cho phép trong khung đang hiệu lực. |
| 8 | commission\_rate | Commission Rate | DECIMAL(5,4) | CHECK 0..1 | Tỷ lệ hoa hồng nền tảng áp dụng cho giá tư vấn. |
| 9 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 10 | effective\_from | Effective From | TIMESTAMP | NULL allowed / business validation | Thời điểm phiên bản bắt đầu có hiệu lực. |
| 11 | effective\_to | Effective To | TIMESTAMP | NULL allowed / business validation | Thời điểm phiên bản kết thúc hiệu lực; để trống khi còn hiệu lực. |
| 12 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 13 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 14 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **45\. expert\_consultation\_prices**

Bảng giá theo phiên bản do từng chuyên gia thiết lập trong khung cho phép; giá mới chỉ áp dụng cho booking tương lai và không hồi tố booking đã khóa giá.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | expert\_price\_id | Expert Price Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi expert\_consultation\_prices. |
| 2 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 3 | price\_band\_id | Price Band Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường price\_band\_id. |
| 4 | channel\_type | Channel Type | VARCHAR(20) | NULL allowed / business validation | Thông tin channel type của bản ghi. |
| 5 | duration\_minutes | Duration Minutes | INT | NULL allowed / business validation | Thông tin duration minutes của bản ghi. |
| 6 | price\_amount | Price Amount | DECIMAL(15,2) | CHECK \>= 0 | Giá trị tiền tệ của price amount. |
| 7 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 8 | cancellation\_policy | Cancellation Policy | TEXT | NULL allowed / business validation | Chính sách hủy/hoàn tiền hiển thị cho người dùng trước booking. |
| 9 | effective\_from | Effective From | TIMESTAMP | NULL allowed / business validation | Thời điểm phiên bản bắt đầu có hiệu lực. |
| 10 | effective\_to | Effective To | TIMESTAMP | NULL allowed / business validation | Thời điểm phiên bản kết thúc hiệu lực; để trống khi còn hiệu lực. |
| 11 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 12 | version\_no | Version No | INT | NULL allowed / business validation | Số phiên bản tăng dần để truy vết thay đổi. |
| 13 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 14 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **46\. consultation\_disputes**

Hồ sơ khiếu nại liên quan đến booking tư vấn, lưu lý do, bằng chứng, trạng thái xử lý và quyết định của đội vận hành.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | dispute\_id | Dispute Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi consultation\_disputes. |
| 2 | booking\_id | Mã booking | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường booking\_id. |
| 3 | submitted\_by | Submitted By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường submitted\_by. |
| 4 | resolved\_by | Resolved By | UUID | FK | Quản trị viên xử lý khiếu nại. |
| 5 | reason\_code | Reason Code | VARCHAR(50) | NULL allowed / business validation | Thông tin reason code của bản ghi. |
| 6 | description | Mô tả | TEXT | NULL allowed / business validation | Thông tin description của bản ghi. |
| 7 | evidence\_json | Evidence Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ evidence json. |
| 8 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 9 | resolution\_type | Resolution Type | VARCHAR(30) | NULL allowed / business validation | Thông tin resolution type của bản ghi. |
| 10 | resolution\_note | Resolution Note | TEXT | NULL allowed / business validation | Thông tin resolution note của bản ghi. |
| 11 | submitted\_at | Submitted At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của submitted at. |
| 12 | resolved\_at | Resolved At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của resolved at. |
| 13 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 14 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **47\. refund\_records**

Bản ghi hoàn tiền gắn với giao dịch và khiếu nại; lưu số tiền, trạng thái xử lý và mã hoàn tiền của cổng thanh toán.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | refund\_id | Refund Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi refund\_records. |
| 2 | payment\_id | Mã giao dịch | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường payment\_id. |
| 3 | dispute\_id | Dispute Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường dispute\_id. |
| 4 | approved\_by | Approved By | UUID | FK | Quản trị viên phê duyệt hoàn tiền. |
| 5 | refund\_amount | Refund Amount | DECIMAL(15,2) | CHECK \>= 0 | Tổng số tiền đã hoặc sẽ hoàn cho người dùng. |
| 6 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 7 | reason | Reason | VARCHAR(255) | NULL allowed / business validation | Thông tin reason của bản ghi. |
| 8 | gateway\_refund\_id | Gateway Refund Id | VARCHAR(150) | NULL allowed / business validation | Thông tin gateway refund id của bản ghi. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 10 | requested\_at | Requested At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của requested at. |
| 11 | processed\_at | Processed At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của processed at. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **48\. settlement\_records**

Bản ghi đối soát cho chuyên gia theo kỳ hoặc theo khoản hoa hồng, lưu giá trị gộp, hoa hồng, phí cổng, hoàn tiền, số thực nhận và trạng thái thanh toán.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | settlement\_id | Settlement Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi settlement\_records. |
| 2 | commission\_id | Commission Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường commission\_id. |
| 3 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 4 | settlement\_period\_start | Settlement Period Start | DATE | NULL allowed / business validation | Thông tin settlement period start của bản ghi. |
| 5 | settlement\_period\_end | Settlement Period End | DATE | NULL allowed / business validation | Thông tin settlement period end của bản ghi. |
| 6 | gross\_amount | Gross Amount | DECIMAL(15,2) | CHECK \>= 0 | Giá trị tiền tệ của gross amount. |
| 7 | commission\_amount | Commission Amount | DECIMAL(15,2) | CHECK \>= 0 | Số tiền hoa hồng CareBridge được hưởng. |
| 8 | gateway\_fee | Gateway Fee | DECIMAL(15,2) | CHECK \>= 0 | Phí cổng thanh toán được ghi nhận cho giao dịch/đối soát. |
| 9 | refund\_amount | Refund Amount | DECIMAL(15,2) | CHECK \>= 0 | Tổng số tiền đã hoặc sẽ hoàn cho người dùng. |
| 10 | expert\_net\_amount | Expert Net Amount | DECIMAL(15,2) | CHECK \>= 0 | Số tiền chuyên gia thực nhận sau hoa hồng, phí và hoàn tiền. |
| 11 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 12 | settled\_at | Settled At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của settled at. |
| 13 | reference\_code | Reference Code | VARCHAR(100) | NULL allowed / business validation | Thông tin reference code của bản ghi. |
| 14 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 15 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **AI & Safety**

### **49\. triage\_assessments**

Phiên intake triệu chứng có cấu trúc và kết quả phân loại rủi ro xanh/vàng/đỏ; không phải chẩn đoán.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | assessment\_id | Assessment Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi triage\_assessments. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | baby\_id | Baby Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường baby\_id. |
| 5 | symptom\_summary | Symptom Summary | TEXT | NULL allowed / business validation | Thông tin symptom summary của bản ghi. |
| 6 | risk\_level | Risk Level | VARCHAR(10) | NULL allowed / business validation | Thông tin risk level của bản ghi. |
| 7 | recommended\_action | Recommended Action | TEXT | NULL allowed / business validation | Thông tin recommended action của bản ghi. |
| 8 | rule\_version | Rule Version | VARCHAR(50) | NULL allowed / business validation | Thông tin rule version của bản ghi. |
| 9 | disclaimer\_accepted | Disclaimer Accepted | BOOLEAN | NULL allowed / business validation | Thông tin disclaimer accepted của bản ghi. |
| 10 | completed\_at | Completed At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của completed at. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **50\. triage\_answers**

Câu trả lời từng bước trong một phiên intake triệu chứng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | triage\_answer\_id | Triage Answer Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi triage\_answers. |
| 2 | assessment\_id | Assessment Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường assessment\_id. |
| 3 | question\_code | Question Code | VARCHAR(80) | NULL allowed / business validation | Thông tin question code của bản ghi. |
| 4 | question\_text | Question Text | VARCHAR(500) | NULL allowed / business validation | Thông tin question text của bản ghi. |
| 5 | answer\_value | Answer Value | TEXT | NULL allowed / business validation | Thông tin answer value của bản ghi. |
| 6 | answer\_order | Answer Order | INT | NULL allowed / business validation | Thông tin answer order của bản ghi. |
| 7 | red\_flag\_triggered | Red Flag Triggered | BOOLEAN | NULL allowed / business validation | Thông tin red flag triggered của bản ghi. |
| 8 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 9 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Partner & Location**

### **51\. partner\_organizations**

Hồ sơ phòng khám, cơ sở y tế, tổ chức xã hội hoặc nhà tài trợ đã đăng ký/được duyệt.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | partner\_id | Partner Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi partner\_organizations. |
| 2 | representative\_user\_id | Representative User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường representative\_user\_id. |
| 3 | partner\_type | Partner Type | VARCHAR(30) | NULL allowed / business validation | Thông tin partner type của bản ghi. |
| 4 | name | Name | VARCHAR(255) | NULL allowed / business validation | Thông tin name của bản ghi. |
| 5 | license\_number | License Number | VARCHAR(120) | NULL allowed / business validation | Thông tin license number của bản ghi. |
| 6 | address | Address | VARCHAR(500) | NULL allowed / business validation | Thông tin address của bản ghi. |
| 7 | latitude | Latitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin latitude của bản ghi. |
| 8 | longitude | Longitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin longitude của bản ghi. |
| 9 | verification\_status | Verification Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của verification status. |
| 10 | verified\_by | Verified By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường verified\_by. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **52\. partner\_expert\_links**

Thực thể trung gian giữa partner\_organizations và expert\_profiles; liên kết chỉ có hiệu lực sau duyệt.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | partner\_expert\_link\_id | Partner Expert Link Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi partner\_expert\_links. |
| 2 | partner\_id | Partner Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường partner\_id. |
| 3 | expert\_profile\_id | Mã hồ sơ chuyên gia | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường expert\_profile\_id. |
| 4 | relationship\_type | Relationship Type | VARCHAR(40) | NULL allowed / business validation | Thông tin relationship type của bản ghi. |
| 5 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 6 | approved\_by | Approved By | UUID | FK | Quản trị viên phê duyệt hoàn tiền. |
| 7 | start\_date | Start Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của start date. |
| 8 | end\_date | End Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của end date. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **53\. partner\_services**

Dịch vụ/lịch khám tham khảo do đối tác gửi và admin duyệt để hiển thị công khai.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | service\_id | Service Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi partner\_services. |
| 2 | partner\_id | Partner Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường partner\_id. |
| 3 | service\_name | Service Name | VARCHAR(255) | NULL allowed / business validation | Thông tin service name của bản ghi. |
| 4 | description | Mô tả | TEXT | NULL allowed / business validation | Thông tin description của bản ghi. |
| 5 | price\_from | Price From | DECIMAL(15,2) | NULL allowed / business validation | Giá trị tiền tệ của price from. |
| 6 | currency | Đơn vị tiền tệ | CHAR(3) | NULL allowed / business validation | Thông tin currency của bản ghi. |
| 7 | booking\_url | Booking Url | VARCHAR(500) | NULL allowed / business validation | Thông tin booking url của bản ghi. |
| 8 | approval\_status | Approval Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của approval status. |
| 9 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 10 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **54\. sponsored\_campaigns**

Chiến dịch/nội dung tài trợ do đối tác gửi, bắt buộc gắn nhãn và kiểm duyệt.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | campaign\_id | Campaign Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi sponsored\_campaigns. |
| 2 | partner\_id | Partner Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường partner\_id. |
| 3 | title | Tiêu đề | VARCHAR(250) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 4 | description | Mô tả | TEXT | NULL allowed / business validation | Thông tin description của bản ghi. |
| 5 | start\_date | Start Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của start date. |
| 6 | end\_date | End Date | DATE | NULL allowed / business validation | Ngày nghiệp vụ của end date. |
| 7 | sponsor\_label | Sponsor Label | VARCHAR(150) | NULL allowed / business validation | Thông tin sponsor label của bản ghi. |
| 8 | approval\_status | Approval Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của approval status. |
| 9 | reviewed\_by | Reviewed By | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường reviewed\_by. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **55\. care\_facilities**

Danh mục cơ sở y tế dùng trong bản đồ khẩn cấp/tìm kiếm gần nhất; có thể đến từ đối tác hoặc nguồn bản đồ.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | facility\_id | Facility Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi care\_facilities. |
| 2 | partner\_id | Partner Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường partner\_id. |
| 3 | name | Name | VARCHAR(255) | NULL allowed / business validation | Thông tin name của bản ghi. |
| 4 | facility\_type | Facility Type | VARCHAR(40) | NULL allowed / business validation | Thông tin facility type của bản ghi. |
| 5 | address | Address | VARCHAR(500) | NULL allowed / business validation | Thông tin address của bản ghi. |
| 6 | latitude | Latitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin latitude của bản ghi. |
| 7 | longitude | Longitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin longitude của bản ghi. |
| 8 | phone | Phone | VARCHAR(30) | NULL allowed / business validation | Thông tin phone của bản ghi. |
| 9 | opening\_hours\_json | Opening Hours Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ opening hours json. |
| 10 | source\_type | Source Type | VARCHAR(30) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 11 | verification\_status | Verification Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của verification status. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **56\. emergency\_events**

Luồng khẩn cấp/tìm hỗ trợ được người dùng hoặc hệ thống mở từ triage, nút khẩn cấp hoặc sự kiện IMU.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | emergency\_event\_id | Emergency Event Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi emergency\_events. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | source\_type | Source Type | VARCHAR(30) | NULL allowed / business validation | Thông tin source type của bản ghi. |
| 4 | source\_reference\_id | Source Reference Id | UUID | NULL allowed / business validation | Thông tin source reference id của bản ghi. |
| 5 | risk\_level | Risk Level | VARCHAR(10) | NULL allowed / business validation | Thông tin risk level của bản ghi. |
| 6 | action\_type | Action Type | VARCHAR(30) | NULL allowed / business validation | Thông tin action type của bản ghi. |
| 7 | selected\_facility\_id | Selected Facility Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường selected\_facility\_id. |
| 8 | selected\_expert\_id | Selected Expert Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường selected\_expert\_id. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 10 | opened\_at | Opened At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của opened at. |
| 11 | closed\_at | Closed At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của closed at. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **57\. location\_snapshots**

Ảnh chụp vị trí tối thiểu có thời hạn dùng cho emergency, family alert hoặc tìm kiếm lân cận.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | location\_snapshot\_id | Location Snapshot Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi location\_snapshots. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | context\_type | Context Type | VARCHAR(30) | NULL allowed / business validation | Thông tin context type của bản ghi. |
| 4 | context\_id | Context Id | UUID | NULL allowed / business validation | Thông tin context id của bản ghi. |
| 5 | latitude | Latitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin latitude của bản ghi. |
| 6 | longitude | Longitude | DECIMAL(10,7) | NULL allowed / business validation | Thông tin longitude của bản ghi. |
| 7 | accuracy\_meters | Accuracy Meters | DECIMAL(10,2) | NULL allowed / business validation | Thông tin accuracy meters của bản ghi. |
| 8 | captured\_at | Captured At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của captured at. |
| 9 | expires\_at | Expires At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của expires at. |
| 10 | consent\_status | Consent Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của consent status. |

## **Device & Smart Safety**

### **58\. health\_device\_connections**

Kết nối tùy chọn với smartwatch/health platform; lưu token tham chiếu an toàn và trạng thái consent.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | connection\_id | Connection Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi health\_device\_connections. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | provider\_name | Provider Name | VARCHAR(80) | NULL allowed / business validation | Thông tin provider name của bản ghi. |
| 4 | device\_name | Device Name | VARCHAR(150) | NULL allowed / business validation | Thông tin device name của bản ghi. |
| 5 | scopes\_json | Scopes Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ scopes json. |
| 6 | token\_reference | Token Reference | VARCHAR(255) | NULL allowed / business validation | Thông tin token reference của bản ghi. |
| 7 | consent\_granted\_at | Consent Granted At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của consent granted at. |
| 8 | last\_synced\_at | Last Synced At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của last synced at. |
| 9 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **59\. device\_measurements**

Chỉ số từ thiết bị/health platform có nhãn nguồn và độ tin cậy; dùng xem xu hướng, không thay đo lâm sàng.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | device\_measurement\_id | Device Measurement Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi device\_measurements. |
| 2 | connection\_id | Connection Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường connection\_id. |
| 3 | measurement\_type | Measurement Type | VARCHAR(40) | NULL allowed / business validation | Thông tin measurement type của bản ghi. |
| 4 | value\_numeric | Value Numeric | DECIMAL(12,3) | NULL allowed / business validation | Thông tin value numeric của bản ghi. |
| 5 | value\_secondary | Value Secondary | DECIMAL(12,3) | NULL allowed / business validation | Thông tin value secondary của bản ghi. |
| 6 | unit | Unit | VARCHAR(30) | NULL allowed / business validation | Thông tin unit của bản ghi. |
| 7 | measured\_at | Measured At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của measured at. |
| 8 | source\_record\_id | Source Record Id | VARCHAR(150) | NULL allowed / business validation | Thông tin source record id của bản ghi. |
| 9 | quality\_label | Quality Label | VARCHAR(30) | NULL allowed / business validation | Thông tin quality label của bản ghi. |
| 10 | raw\_metadata\_json | Raw Metadata Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ raw metadata json. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **60\. safety\_monitoring\_settings**

Cấu hình bật/tắt giám sát IMU, countdown, người nhận cảnh báo và consent vị trí.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | setting\_id | Setting Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi safety\_monitoring\_settings. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | is\_enabled | Is Enabled | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho is enabled. |
| 4 | countdown\_seconds | Countdown Seconds | INT | NULL allowed / business validation | Thông tin countdown seconds của bản ghi. |
| 5 | location\_sharing\_enabled | Location Sharing Enabled | BOOLEAN | NULL allowed / business validation | Cờ đúng/sai cho location sharing enabled. |
| 6 | emergency\_contact\_user\_id | Emergency Contact User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường emergency\_contact\_user\_id. |
| 7 | monitoring\_schedule\_json | Monitoring Schedule Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ monitoring schedule json. |
| 8 | sensor\_consent\_at | Sensor Consent At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của sensor consent at. |
| 9 | location\_consent\_at | Location Consent At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của location consent at. |
| 10 | disclaimer\_version | Disclaimer Version | VARCHAR(50) | NULL allowed / business validation | Thông tin disclaimer version của bản ghi. |
| 11 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 12 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **61\. safety\_events**

Sự kiện nghi ngờ ngã/va chạm mạnh được phát hiện từ IMU; chỉ lưu metadata tối thiểu, không khẳng định chấn thương.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | safety\_event\_id | Safety Event Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi safety\_events. |
| 2 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 3 | setting\_id | Setting Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường setting\_id. |
| 4 | detected\_at | Detected At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của detected at. |
| 5 | event\_type | Event Type | VARCHAR(30) | NULL allowed / business validation | Thông tin event type của bản ghi. |
| 6 | confidence\_score | Confidence Score | DECIMAL(5,4) | NULL allowed / business validation | Thông tin confidence score của bản ghi. |
| 7 | peak\_acceleration | Peak Acceleration | DECIMAL(10,4) | NULL allowed / business validation | Thông tin peak acceleration của bản ghi. |
| 8 | angular\_velocity | Angular Velocity | DECIMAL(10,4) | NULL allowed / business validation | Thông tin angular velocity của bản ghi. |
| 9 | inactivity\_seconds | Inactivity Seconds | INT | NULL allowed / business validation | Thông tin inactivity seconds của bản ghi. |
| 10 | user\_response | User Response | VARCHAR(30) | NULL allowed / business validation | Thông tin user response của bản ghi. |
| 11 | response\_at | Response At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của response at. |
| 12 | false\_positive\_reason | False Positive Reason | VARCHAR(80) | NULL allowed / business validation | Thông tin false positive reason của bản ghi. |
| 13 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 14 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 15 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **62\. safety\_alerts**

Cảnh báo tối thiểu gửi cho người thân sau khi người dùng yêu cầu trợ giúp hoặc không phản hồi.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | safety\_alert\_id | Safety Alert Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi safety\_alerts. |
| 2 | safety\_event\_id | Safety Event Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường safety\_event\_id. |
| 3 | recipient\_user\_id | Recipient User Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường recipient\_user\_id. |
| 4 | location\_snapshot\_id | Location Snapshot Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường location\_snapshot\_id. |
| 5 | alert\_reason | Alert Reason | VARCHAR(30) | NULL allowed / business validation | Thông tin alert reason của bản ghi. |
| 6 | payload\_json | Payload Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ payload json. |
| 7 | delivery\_status | Delivery Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của delivery status. |
| 8 | sent\_at | Sent At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của sent at. |
| 9 | acknowledged\_at | Acknowledged At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của acknowledged at. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 11 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

## **Pregnancy Exercise & Posture**

### **63\. pregnancy\_exercises**

Danh mục bài tập thai kỳ đã được kiểm duyệt, có phạm vi tam cá nguyệt, mức độ khó, thời lượng, hướng dẫn, cảnh báo an toàn và trạng thái phiên bản.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | exercise\_id | Mã bài tập | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi pregnancy\_exercises. |
| 2 | created\_by | Created By | UUID | FK | Người tạo nội dung hoặc bản ghi. |
| 3 | title | Tiêu đề | VARCHAR(200) | NULL allowed / business validation | Thông tin title của bản ghi. |
| 4 | description | Mô tả | TEXT | NULL allowed / business validation | Thông tin description của bản ghi. |
| 5 | trimester\_scope | Trimester Scope | VARCHAR(30) | NULL allowed / business validation | Thông tin trimester scope của bản ghi. |
| 6 | difficulty\_level | Difficulty Level | VARCHAR(20) | NULL allowed / business validation | Thông tin difficulty level của bản ghi. |
| 7 | duration\_minutes | Duration Minutes | INT | NULL allowed / business validation | Thông tin duration minutes của bản ghi. |
| 8 | instruction\_content | Instruction Content | TEXT | NULL allowed / business validation | Thông tin instruction content của bản ghi. |
| 9 | media\_url | Media Url | VARCHAR(500) | NULL allowed / business validation | Thông tin media url của bản ghi. |
| 10 | safety\_warning | Safety Warning | TEXT | NULL allowed / business validation | Thông tin safety warning của bản ghi. |
| 11 | supports\_posture\_analysis | Supports Posture Analysis | BOOLEAN | NULL allowed / business validation | Thông tin supports posture analysis của bản ghi. |
| 12 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 13 | version\_no | Version No | INT | NULL allowed / business validation | Số phiên bản tăng dần để truy vết thay đổi. |
| 14 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 15 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **64\. exercise\_safety\_checks**

Kết quả kiểm tra an toàn trước tập của Mẹ cho một bài tập và hành trình cụ thể; lưu câu trả lời có cấu trúc, red flag và quyết định cho phép hoặc chặn bắt đầu.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | safety\_check\_id | Safety Check Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi exercise\_safety\_checks. |
| 2 | exercise\_id | Mã bài tập | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường exercise\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 5 | answer\_json | Answer Json | JSON | NULL allowed / business validation | Câu trả lời kiểm tra an toàn được lưu có cấu trúc. |
| 6 | red\_flag\_detected | Red Flag Detected | BOOLEAN | NULL allowed / business validation | Cho biết kiểm tra trước tập phát hiện dấu hiệu cần dừng hoặc chuyển sang hỗ trợ an toàn. |
| 7 | result\_status | Result Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của result status. |
| 8 | blocked\_reason | Blocked Reason | TEXT | NULL allowed / business validation | Thông tin blocked reason của bản ghi. |
| 9 | completed\_at | Completed At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của completed at. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |

### **65\. exercise\_sessions**

Phiên luyện tập thai kỳ của Mẹ; lưu thời gian, trạng thái tạm dừng/hoàn thành, mức hoàn thành, điểm tư thế tổng hợp và cảnh báo.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | exercise\_session\_id | Exercise Session Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi exercise\_sessions. |
| 2 | exercise\_id | Mã bài tập | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường exercise\_id. |
| 3 | journey\_id | Mã hành trình | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường journey\_id. |
| 4 | user\_id | Mã người dùng | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường user\_id. |
| 5 | safety\_check\_id | Safety Check Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường safety\_check\_id. |
| 6 | started\_at | Started At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của started at. |
| 7 | ended\_at | Ended At | TIMESTAMP | NULL allowed / business validation | Thời điểm nghiệp vụ của ended at. |
| 8 | paused\_seconds | Paused Seconds | INT | NULL allowed / business validation | Thông tin paused seconds của bản ghi. |
| 9 | completion\_percent | Completion Percent | DECIMAL(5,2) | NULL allowed / business validation | Thông tin completion percent của bản ghi. |
| 10 | posture\_score | Posture Score | DECIMAL(5,2) | NULL allowed / business validation | Thông tin posture score của bản ghi. |
| 11 | session\_status | Session Status | VARCHAR(20) | NULL allowed / business validation | Trạng thái hiện tại của session status. |
| 12 | warning\_count | Warning Count | INT | NULL allowed / business validation | Thông tin warning count của bản ghi. |
| 13 | summary\_json | Summary Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ summary json. |
| 14 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 15 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **66\. posture\_analysis\_configs**

Cấu hình rule-based hoặc ML-based dùng để phân tích tư thế cho từng bài tập, gồm phiên bản rule/model, ngưỡng tin cậy và thời gian hiệu lực.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | posture\_config\_id | Posture Config Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi posture\_analysis\_configs. |
| 2 | exercise\_id | Mã bài tập | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường exercise\_id. |
| 3 | configured\_by | Configured By | UUID | FK | Người quản trị thực hiện cấu hình. |
| 4 | analysis\_mode | Analysis Mode | VARCHAR(20) | NULL allowed / business validation | Thông tin analysis mode của bản ghi. |
| 5 | rule\_or\_model\_version | Rule Or Model Version | VARCHAR(100) | NULL allowed / business validation | Phiên bản bộ luật hoặc mô hình ML dùng để phân tích tư thế. |
| 6 | confidence\_threshold | Confidence Threshold | DECIMAL(5,4) | CHECK 0..1 | Ngưỡng tin cậy tối thiểu để phát phản hồi tư thế. |
| 7 | feedback\_level | Feedback Level | VARCHAR(20) | NULL allowed / business validation | Giá trị tiền tệ của feedback level. |
| 8 | config\_json | Config Json | JSON | NULL allowed / business validation | Dữ liệu có cấu trúc phục vụ config json. |
| 9 | effective\_from | Effective From | TIMESTAMP | NULL allowed / business validation | Thời điểm phiên bản bắt đầu có hiệu lực. |
| 10 | effective\_to | Effective To | TIMESTAMP | NULL allowed / business validation | Thời điểm phiên bản kết thúc hiệu lực; để trống khi còn hiệu lực. |
| 11 | status | Trạng thái | VARCHAR(20) | NOT NULL | Trạng thái hiện tại của status. |
| 12 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |
| 13 | updated\_at | Thời điểm cập nhật | TIMESTAMP | NULL allowed / business validation | Thời điểm bản ghi được cập nhật gần nhất. |

### **67\. posture\_feedback\_events**

Các phản hồi tư thế phát sinh trong phiên tập; chỉ lưu kết quả/keypoint tóm tắt cần thiết, không lưu video camera thô.

| No | Field | Name | Type | Constraint | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | feedback\_event\_id | Feedback Event Id | UUID | PK, NOT NULL | Khóa chính định danh duy nhất một bản ghi posture\_feedback\_events. |
| 2 | exercise\_session\_id | Exercise Session Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường exercise\_session\_id. |
| 3 | posture\_config\_id | Posture Config Id | UUID | FK | Khóa ngoại liên kết tới thực thể liên quan cho trường posture\_config\_id. |
| 4 | event\_time\_ms | Event Time Ms | BIGINT | NULL allowed / business validation | Thông tin event time ms của bản ghi. |
| 5 | posture\_code | Posture Code | VARCHAR(80) | NULL allowed / business validation | Thông tin posture code của bản ghi. |
| 6 | confidence\_score | Confidence Score | DECIMAL(5,4) | NULL allowed / business validation | Thông tin confidence score của bản ghi. |
| 7 | severity | Severity | VARCHAR(20) | NULL allowed / business validation | Thông tin severity của bản ghi. |
| 8 | feedback\_text | Feedback Text | VARCHAR(500) | NULL allowed / business validation | Giá trị tiền tệ của feedback text. |
| 9 | keypoint\_summary\_json | Keypoint Summary Json | JSON | NULL allowed / business validation | Tóm tắt điểm cơ thể/đặc trưng cần thiết cho phản hồi; không phải video thô. |
| 10 | created\_at | Thời điểm tạo | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo. |

