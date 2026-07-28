Bạn đang làm việc trong repository CareBridge tại:

D:\Do_aN

Branch hiện tại: LamVH1.

Tôi sử dụng Microsoft Edge và muốn nhìn thấy automation test chạy trực tiếp
trước mắt tôi. Bắt buộc mở Microsoft Edge thật ở chế độ visible/headed;
không được âm thầm thay bằng Chrome, in-app browser hoặc headless automation.

==================================================
MỤC TIÊU
==================================================

Audit và hoàn thiện end-to-end toàn bộ Expert Platform của CareBridge trên:

- Backend Spring Boot.
- Expert Web Portal React.
- Admin Web Portal.
- Flutter Mobile.
- PostgreSQL canonical schema.
- PostgreSQL Docker local.
- Cloudinary private/authenticated.
- Cloudflare R2 private.
- CompreFace Docker local.
- Notification và audit.
- Province Open API v2.
- TrackAsia v2.
- Emergency Map.

Phạm vi không chỉ là Expert Registration mà bao gồm toàn bộ vòng đời:

Expert Applicant
→ đăng ký và kích hoạt tài khoản
→ onboarding
→ hồ sơ chuyên môn
→ chọn bệnh viện
→ tải bằng chứng định danh
→ tải tài liệu chuyên môn
→ CompreFace advisory
→ submit verification
→ Admin review
→ approve/reject/resubmit
→ Verified Expert
→ Expert Portal
→ profile
→ availability
→ consultation
→ Expert Q&A
→ contribution/badges
→ notification
→ directory
→ nearby support/location nếu đã có trong codebase
→ renewal/expiry/suspension/revocation nếu đã có trong schema.

Không chỉ review, cleanup hoặc viết báo cáo. Sau audit phải tiếp tục
implementation, compile, test, chạy Docker, chạy Web, chạy Flutter Android và
visible Edge automation.

Không dừng sau cleanup khi vẫn còn phần trong phạm vi có thể hoàn thiện.

==================================================
0. BẮT BUỘC CHẠY /mcp ĐẦU TIÊN
==================================================

Hành động đầu tiên:

/mcp

Hoặc cơ chế MCP/tool discovery tương đương do môi trường Claude Code cung cấp.

Yêu cầu hiển thị khi dùng MCP:

- Khi `/mcp` hoặc MCP browser/DevTools bắt đầu điều khiển giao diện, phải mở
  giao diện visible/headed để tôi nhìn thấy trực tiếp.
- Không chạy âm thầm dưới nền nếu thao tác đó có giao diện người dùng có thể
  hiển thị.
- Mỗi thời điểm chỉ duy trì một cửa sổ Microsoft Edge test visible, trừ khi một
  test bắt buộc phải kiểm tra tương tác đa cửa sổ.
- Ưu tiên tái sử dụng cửa sổ/tab test hiện tại thay vì mở thêm nhiều cửa sổ.
- Sau khi hoàn tất từng test case hoặc test flow, phải đóng tab/cửa sổ giao diện
  vừa dùng và xác nhận cleanup trước khi chuyển sang test tiếp theo.
- Nếu test thất bại hoặc bị timeout, vẫn phải thực hiện teardown và đóng giao
  diện test; không để cửa sổ lỗi tồn tại.
- Không đóng hoặc can thiệp cửa sổ/profile Edge cá nhân của tôi; chỉ đóng đúng
  cửa sổ/tab được automation tạo ra.

Phải kiểm tra:

- code-review-graph MCP.
- Edge DevTools MCP.
- Browser-control MCP/tool có thể điều khiển Microsoft Edge.
- Các MCP được cấu hình trong:

D:\Do_aN\.mcp.json

Không được chỉ đọc `.mcp.json` rồi kết luận MCP hoạt động. Phải thực hiện ít
nhất một tool call read-only để xác nhận kết nối thật.

Với code-review-graph MCP:

- Xác nhận tool callable.
- Lập dependency graph cho Backend, Web và Flutter Expert.
- Tìm route, API wiring, controller, service, repository, entity và table.
- Chạy lại sau implementation để review diff cuối.
- Không tuyên bố đã dùng nếu chỉ dùng grep, rg hoặc đọc source.

Với Edge DevTools MCP:

- Xác nhận kết nối Microsoft Edge.
- Edge phải chạy visible/headed.
- Automation phải chạy trực tiếp trước mắt tôi.
- Không thay bằng Chrome.
- Không thay bằng headless browser.
- Không coi API test hoặc unit test là UI automation.

Nếu Edge DevTools MCP không khả dụng:

- Kiểm tra khả năng điều khiển Edge qua Chrome DevTools Protocol.
- Có thể mở `msedge.exe` bằng remote debugging và profile test riêng.
- Không sửa/xóa profile Edge cá nhân.
- Vẫn phải mở cửa sổ Edge thật.
- Nếu không thể điều khiển Edge, ghi rõ blocker.
- Không âm thầm chuyển browser khác.

Thứ tự bắt buộc:

/mcp
→ xác nhận code-review-graph
→ xác nhận Edge DevTools
→ kiểm tra Git/worktree
→ đọc hai SQL bắt buộc
→ kiểm tra `.env` an toàn
→ kiểm tra Docker PostgreSQL/CompreFace
→ khởi động runtime
→ chạy visible Edge automation baseline
→ audit codebase
→ implementation
→ compile/test/Testcontainers
→ Docker/runtime verification
→ Flutter Android
→ chạy lại visible Edge automation
→ chạy lại code-review-graph
→ báo cáo cuối.

==================================================
1. GIT VÀ WORKTREE
==================================================

Đọc và tuân thủ:

- D:\Do_aN\AGENTS.md
- Các instruction Git dual-remote được AGENTS.md tham chiếu.

Ràng buộc:

- Không sync lại branch dev.
- Không pull lại dev.
- Không checkout dev.
- Không restore stash.
- Không stash pop.
- Không push GitHub.
- Không push GitLab.
- Không commit nếu chưa được yêu cầu.
- Không reset.
- Không force checkout.
- Không ghi đè thay đổi hiện có.
- Không xóa thay đổi của người dùng.
- Kiểm tra `git status` trước khi sửa.
- Bảo toàn file modified/untracked.
- Không làm lộ secret từ `.env`, log, test hoặc screenshot.

==================================================
2. DATABASE VÀ SQL — QUY ĐỊNH TUYỆT ĐỐI
==================================================

Phải đọc đầy đủ và sử dụng đúng hai file:

Assessment SQL read-only:

D:\Do_aN\05_Development\CareBridgeAPI\src\main\resources\db\assessment\story_6_5_legacy_baby_links.sql

Canonical schema:

D:\Do_aN\05_Development\CareBridgeAPI\src\main\resources\db\migration\V1__init_schema.sql

Vai trò:

- `story_6_5_legacy_baby_links.sql` chỉ dùng assessment read-only.
- Không dùng assessment SQL để thay đổi dữ liệu.
- `V1__init_schema.sql` là nguồn canonical.
- Mọi entity, repository, query và relationship phải đối chiếu với schema này.
- Điều chỉnh code theo schema.
- Không sửa schema để hợp thức hóa code sai.
- Audit xem toàn bộ luồng Expert có dùng đúng các bảng canonical hay không.

CẤM TUYỆT ĐỐI:

- Không tự tạo bảng.
- Không tạo migration có `CREATE TABLE`.
- Không tự thêm column.
- Không tạo bảng JSON.
- Không tạo bảng cache.
- Không tạo bảng phụ.
- Không tạo duplicate table.
- Không sửa `V1__init_schema.sql`.
- Không xóa/đổi tên/hợp nhất bảng.
- Không chạy cleanup SQL.
- Không chạy destructive SQL.
- Không tạo entity kéo theo table mới.
- Không dùng `ddl-auto=create` hoặc `create-drop`.
- Không chạy `flyway clean`.
- Không xóa/reset Docker volume database.

Nếu schema chưa đáp ứng:

1. Dừng riêng thay đổi schema đó.
2. Tiếp tục các phần không bị chặn.
3. Trình bày:
   - requirement bị chặn;
   - bảng/cột đã kiểm tra;
   - lý do chưa đáp ứng;
   - đề xuất tối thiểu;
   - migration dự kiến;
   - ảnh hưởng dữ liệu.
4. Chờ tôi chấp thuận.
5. Chỉ sau khi được chấp thuận mới tạo bảng/cột/migration.

Không được hiểu “hoàn thiện” là quyền tự động thay đổi schema.

==================================================
3. POSTGRESQL DOCKER LOCAL
==================================================

PostgreSQL/SQL đã có Docker local trong repository.

Phải:

- Tìm và dùng Docker Compose/config hiện có.
- Không tạo PostgreSQL stack khác nếu stack canonical hoạt động.
- Không dùng H2/mock thay PostgreSQL cho runtime.
- Không xóa volume.
- Không reset database.
- Không chạy cleanup.
- Không làm lộ database credential.
- Không in connection string chứa password.

Trước khi chạy Backend:

1. Kiểm tra Docker Engine.
2. Kiểm tra Compose files.
3. Kiểm tra PostgreSQL container.
4. Kiểm tra port/network.
5. Chờ health thành công.
6. Kiểm tra Backend kết nối database.
7. Xác nhận schema validation.
8. Không tự tạo bảng/cột.

Runtime sử dụng PostgreSQL Docker local.

Integration test sử dụng PostgreSQL Testcontainers để kiểm tra hành vi thật:

- UUID.
- JSON/JSONB đã tồn tại.
- Unique constraint.
- Foreign key.
- Check constraint.
- Transaction.
- Lock/concurrency.
- Native query.
- Idempotency.

Không chỉ chạy H2 rồi tuyên bố integration test pass.

==================================================
4. KIẾN TRÚC PROVINCE VÀ HOSPITAL
==================================================

4.1. Province Open API v2

Kiến trúc:

Province Open API v2
→ CareBridge Backend proxy
→ cache JSON 12–24 giờ
→ Web/Flutter dropdown
→ không nhập toàn bộ vào SQL.

Nguồn:

https://provinces.open-api.vn/api/v2/

Yêu cầu:

- Web/Flutter không gọi API ngoài trực tiếp.
- Backend proxy và chuẩn hóa response.
- Cache bằng memory, HTTP cache hoặc JSON snapshot.
- TTL 12–24 giờ.
- Có fallback JSON/snapshot.
- Không thêm Redis.
- Không tạo bảng cache.
- Không nhập toàn bộ địa giới vào SQL nếu không cần.
- Không gọi API ngoài mỗi lần mở dropdown.
- Dropdown mới:
  Tỉnh/Thành phố → Phường/Xã.
- Không bắt buộc Quận/Huyện trong luồng mới.
- Audit district legacy hiện tại.
- Không trộn địa giới cũ/mới.

API nội bộ dự kiến:

GET /api/v1/master-data/provinces
GET /api/v1/master-data/wards?provinceId={provinceId}

4.2. TrackAsia Search/Autocomplete v2 cho Expert Registration

Kiến trúc:

Expert nhập bệnh viện
→ Web/Flutter gọi Backend
→ Backend gọi TrackAsia Search/Autocomplete v2
→ trả suggestions
→ Expert chọn bệnh viện
→ chỉ lưu bệnh viện được chọn khi submit
→ care_facilities UNVERIFIED
→ Admin xác minh.

Yêu cầu:

- Không import toàn bộ bệnh viện Việt Nam.
- Không lưu toàn bộ TrackAsia response.
- TrackAsia key chỉ ở Backend.
- Không lộ key trên Web/Flutter.
- Dùng endpoint v2.
- `new_admin=true`.
- URL encoding.
- Timeout/error handling.
- Parser đúng response v2.
- Autocomplete có debounce.
- Giới hạn kết quả.
- Không gọi API không kiểm soát trên mỗi keystroke.

Khi Expert submit:

- Tìm `care_facilities` theo `source_type + external_source_id`.
- Nếu tồn tại, tái sử dụng.
- Nếu chưa có, chỉ lưu dữ liệu tối thiểu.
- `verification_status = UNVERIFIED`.
- Không tự verify.
- Không tự approve Expert.
- Upsert idempotent.
- Chống duplicate theo place ID, tên, địa chỉ và tọa độ.

Chỉ dùng field đã tồn tại trong schema:

- facility_id.
- name.
- facility_type.
- facility_level nếu có.
- address.
- administrative reference nếu có.
- latitude.
- longitude.
- phone.
- source_type = TRACKASIA.
- external_source_id.
- verification_status.
- active.
- searchable.

Không được:

- Tạo bảng hospital khác.
- Tạo bảng TrackAsia payload.
- Thêm JSONB column khi chưa được duyệt.
- Lưu toàn bộ hospital JSON trong Expert Profile.
- Chỉ lưu tên bệnh viện text mà không có facility ID.

Expert Profile phải tham chiếu `facility_id/hospital_id` canonical.

4.3. Admin xác minh facility

Luồng:

care_facilities UNVERIFIED
→ Admin review
→ VERIFIED/APPROVED theo enum canonical
→ Admin mới approve Expert khi đủ điều kiện.

Admin thấy:

- Tên.
- Địa chỉ.
- Địa giới.
- Tọa độ.
- Nguồn TRACKASIA.
- External place ID.
- Verification status.
- Expert đang đề xuất facility nếu schema hỗ trợ.

Nếu schema chưa hỗ trợ facility decision:

- Không tạo bảng/cột.
- Báo blocker và đề xuất.
- Chờ tôi phê duyệt.

Nếu không tìm thấy bệnh viện:

- Có lựa chọn “Không tìm thấy cơ sở y tế của tôi”.
- Cho nhập đề xuất nếu schema hiện có hỗ trợ.
- Không tự verify.
- Không tự tạo bảng đề xuất.
- Nếu schema chưa hỗ trợ, báo và xin phép.

4.4. TrackAsia Nearby/Directions v2 cho Emergency Map

Kiến trúc:

GPS mẹ bầu
→ Backend
→ TrackAsia Nearby Search v2
→ nhiều bệnh viện ứng viên
→ Directions/Distance Matrix
→ so sánh duration
→ chọn theo ETA phù hợp nhất
→ marker + polyline + ETA
→ không lưu toàn bộ vào SQL
→ cache memory 1–5 phút.

Yêu cầu:

- Location permission.
- Validate tọa độ.
- API key chỉ ở Backend.
- `type=hospital`.
- `new_admin=true`.
- Radius giới hạn hợp lý.
- Không chỉ lấy một bệnh viện.
- Không chọn chỉ bằng đường chim bay.
- Dùng duration của TrackAsia.
- Không tự tính ETA nếu provider đã trả duration.
- Decode/render polyline đúng.
- Không persist toàn bộ response.
- Không tạo bảng POI/history.
- Không thêm Redis.

Response gồm:

- place ID.
- Tên bệnh viện.
- Địa chỉ.
- Tọa độ.
- Distance.
- Duration/ETA.
- Polyline.
- Nguồn.
- CareBridge verification status nếu đối chiếu được.

Emergency Map không bắt buộc facility phải VERIFIED.

Fallback:

- User từ chối location.
- GPS unavailable.
- TrackAsia timeout.
- Zero results.
- Directions lỗi.
- Polyline lỗi.
- Provider unavailable.

Phải có disclaimer CareBridge không phải dịch vụ điều phối cấp cứu.

4.5. Redis

- Không thêm Redis.
- Không thêm dependency.
- Không thêm container.
- Không thêm configuration.
- Province cache dùng memory/HTTP/JSON.
- Emergency Map cache memory 1–5 phút.
- PostgreSQL chỉ giữ canonical data cần audit/xác minh.

==================================================
5. CLOUDINARY PRIVATE
==================================================

Cloudinary private/authenticated dùng cho:

1. Selfie.
2. CCCD mặt trước.
3. CCCD mặt sau.

Biến Cloudinary đã có trong:

D:\Do_aN\05_Development\CareBridgeAPI\.env

Phải:

- Đọc tên biến đang tồn tại.
- Sử dụng đúng tên biến đó.
- Không tạo bộ tên biến khác.
- Không hard-code credential.
- Không hiển thị giá trị secret.
- Không đưa secret vào source/log/test/screenshot/report.
- Không sửa giá trị `.env` nếu chưa được yêu cầu.
- Chỉ báo tên biến và trạng thái configured/not configured.

Yêu cầu file:

- Private/authenticated delivery.
- Không trả public URL.
- Signed URL có thời hạn.
- Authentication và authorization.
- Expert chỉ xem file của mình.
- Admin đúng quyền mới xem được.
- Upload/view/replace/delete.
- Replace thành công mới xóa asset cũ.
- Replace thất bại không làm mất asset cũ.
- Delete kiểm tra ownership.
- Không orphan asset.
- Không log signed URL hoặc dữ liệu CCCD.

==================================================
6. CLOUDFLARE R2 PRIVATE
==================================================

Cloudflare R2 private dùng cho:

- PDF.
- DOC.
- DOCX.

Biến R2 lấy từ:

D:\Do_aN\05_Development\CareBridgeAPI\.env

Yêu cầu:

- Đây là R2, không gọi R3.
- Không hard-code endpoint/bucket/access key/secret.
- Không hiển thị credential.
- Bucket private.
- Signed view/download có thời hạn.
- Validate extension.
- Validate MIME.
- Validate kích thước.
- Validate ownership.
- Upload/view/replace/delete.
- Replace thành công mới xóa file cũ.
- Replace thất bại không mất file cũ.
- Không orphan object.
- Không lộ signed URL/credential.

==================================================
7. COMPRE­FACE DOCKER LOCAL
==================================================

CompreFace đã có Docker local trong repository.

Tài khoản quản trị CompreFace local được cung cấp để kiểm tra runtime:

- Email: `admin@carebridge.dev`.
- Mật khẩu: `123456789`.
- Chỉ dùng tài khoản này để đăng nhập CompreFace local khi cấu hình/kiểm tra
  Detection API và Verification API.
- Không ghi mật khẩu vào source, `.env`, log, test report, screenshot hoặc
  báo cáo cuối.
- Không dùng tài khoản CompreFace này làm tài khoản đăng nhập CareBridge.

Phải:

- Tìm và dùng Docker Compose hiện có.
- Đọc runbook/config hiện có.
- Không tạo stack CompreFace khác nếu stack hiện tại hoạt động.
- Sử dụng biến CompreFace trong `CareBridgeAPI/.env`.
- Không hard-code URL/API key/threshold.
- Không hiển thị key.
- Không đổi threshold tùy ý.

Kiểm tra:

- Container/services.
- Detection API health.
- Verification API health.
- Backend base URL.
- API key wiring.
- Connect/read timeout.
- Error handling.
- Retry giới hạn.
- Không retry vô hạn.

Luồng:

Selfie
→ so với khuôn mặt trên CCCD mặt trước
→ lưu score + threshold + advisory status + timestamp bằng bảng/cột hiện có.

Quy tắc:

- Chỉ advisory.
- Không auto approve.
- Không auto reject.
- Score thấp không auto reject.
- Service unavailable không làm mất hồ sơ.
- No face/multiple faces chuyển Admin review.
- Không tự tạo bảng/cột CompreFace.
- Admin quyết định cuối cùng.

Admin badge:

- Trùng khớp.
- Không chắc chắn.
- Không trùng khớp.
- Không thể kiểm tra.

Runtime scenario với Docker local:

- Match.
- Low score.
- No face.
- Multiple faces.
- Invalid image.
- Oversized image.
- Timeout.
- Service unavailable.
- Unauthorized API key.
- Resubmission với ảnh mới.

==================================================
8. TOÀN BỘ EXPERT PLATFORM
==================================================

Dùng code-review-graph và source inspection để inventory toàn bộ:

- Expert Applicant.
- Expert Account.
- Expert Profile.
- Expert Verification.
- Expert Identity Verification.
- Expert Credentials/Documents.
- Expert Availability.
- Expert Consultation.
- Expert Q&A.
- Expert Contributions/Badges.
- Expert Directory.
- Expert Notifications.
- Expert Location/Nearby Support.
- Expert Admin Review.
- Expert Trust Status.
- Expert routes/navigation.
- Expert feature gating.

Nếu feature đã có trong code, route, tài liệu hoặc schema nhưng mới làm một phần,
phải hoàn thiện wiring và test trong giới hạn schema hiện tại.

Không tự thêm feature cần schema mới mà chưa được phê duyệt.

==================================================
9. EXPERT ACCOUNT VÀ VERIFICATION LIFECYCLE
==================================================

Audit và hoàn thiện:

Guest
→ Register Expert Applicant
→ OTP/account activation
→ Onboarding
→ Draft profile
→ Province/Ward
→ TrackAsia hospital selection
→ Three identity images
→ Credential documents
→ CompreFace advisory
→ Submit
→ Pending Admin Review
→ Approve/Reject/Resubmit
→ Verified Expert activation
→ Expert Portal
→ Renewal/Expiry/Suspension/Revocation nếu supported.

Mỗi transition cần:

- Trạng thái đầu hợp lệ.
- Trạng thái đích hợp lệ.
- Validation.
- Authorization.
- Idempotency.
- Audit.
- Notification.
- UI loading/error/success.
- Backend enforcement.
- Web route guard.
- Flutter navigation guard.
- Tests.

Không cho phép:

- Applicant tự cấp Expert role.
- Frontend tự quyết định verification status.
- Submit trùng.
- Admin decision trùng.
- Bỏ qua evidence bắt buộc.
- Expert invalid status vẫn dùng official feature.

==================================================
10. EXPERT PROFILE VÀ ONBOARDING
==================================================

Hoàn thiện:

- Create/view/update profile.
- Chuyên khoa.
- Chức danh.
- Kinh nghiệm.
- Phạm vi tư vấn.
- Tỉnh/phường.
- Care facility.
- Verification status.
- Public/private data separation.
- Validation.
- Authorization.
- Audit khi thay đổi dữ liệu quan trọng.

Phân biệt:

- Expert tự khai.
- Admin đã xác minh.
- Public directory data.
- Private verification data.

Nếu thay đổi dữ liệu đã xác minh và business rule yêu cầu review lại, phải cập
nhật trạng thái bằng cơ chế hiện có; không tự tạo column.

==================================================
11. ADMIN EXPERT REVIEW
==================================================

Admin verification queue phải có:

- Search/filter/pagination.
- Pending/resubmit/expired status.
- Applicant detail.
- Facility detail/status.
- Selfie.
- CCCD mặt trước.
- CCCD mặt sau.
- PDF/DOC/DOCX.
- CompreFace score.
- Threshold.
- Advisory badge.
- Submission history.
- Review history.
- Audit context được phép.

Admin actions:

- Verify/reject facility nếu schema hỗ trợ.
- Approve.
- Reject kèm lý do.
- Request resubmission kèm yêu cầu.
- Suspend/revoke/reinstate nếu business rule/schema hỗ trợ.
- Chống concurrent/duplicate decision.

Khi Expert submit:

- Tạo đúng notification cho Admin.
- Tạo audit event.
- Không notification trùng.
- Không duplicate submission.

Sau quyết định:

- Expert nhận notification.
- Status/history/audit được lưu.
- Chỉ approved Expert được cấp quyền.

==================================================
12. CÁC CHỨC NĂNG VERIFIED EXPERT
==================================================

12.1. Expert Dashboard

- Route hoạt động.
- Summary phù hợp.
- Verification status.
- Pending actions.
- Navigation đến feature.
- Loading/error/empty state.

12.2. Availability

Nếu có trong code/schema:

- View/create/update/delete/disable slots.
- Timezone.
- Start/end validation.
- Overlap/duplicate protection.
- Ownership.
- Verified-only access.
- Web/Flutter wiring.
- Tests.

Không tạo bảng availability mới.

12.3. Consultation

Nếu thuộc release/codebase:

- Request list/detail.
- Accept/reject/respond.
- Concurrent handling.
- Consent/data-sharing boundary.
- Chỉ xem dữ liệu được chia sẻ.
- Audit sensitive access.
- Notification.
- Chat/voice/video routes nếu đã có.
- Verified-only gating.
- Web/Flutter wiring.
- Tests.

Không tự mở rộng payment hoặc feature ngoài phạm vi nếu cần schema mới.

12.4. Expert Q&A

Nếu có trong code/schema:

- Question queue.
- Search/filter/pagination.
- Detail.
- Draft/submit/edit theo business rule.
- Verified Expert có thể trả lời câu hỏi trong Community.
- Chỉ câu trả lời do tài khoản Expert đang ở trạng thái verified/approved hợp lệ
  mới hiển thị tích/nhãn “Chuyên gia”.
- Tích/nhãn chuyên gia phải được Backend suy ra từ role và verification status;
  client không được tự gửi hoặc tự quyết định cờ `isExpert`/`verified`.
- Câu trả lời của Expert expired/suspended/revoked không được tiếp tục hiển thị
  như một câu trả lời chuyên gia đang hoạt động; xử lý lịch sử theo business rule
  và schema hiện có.
- Accepted/helpful state nếu supported.
- Authorization.
- Notification/audit.
- Safety boundary.
- Verified-only gating.
- Web/Flutter wiring.
- Tests.

12.5. Contribution/Badges

Nếu có:

- Verified Expert có thể đăng bài/chia sẻ tài liệu trong Community.
- Bài đăng có thể kèm tài liệu theo định dạng, kích thước, MIME type và storage
  lifecycle đã được code/schema hiện tại hỗ trợ.
- Mọi bài đăng do Expert verified/approved tạo phải hiển thị tích/nhãn
  “Chuyên gia” cạnh thông tin tác giả trên Web và Flutter.
- Backend phải trả về trạng thái tác giả chuyên gia từ dữ liệu canonical tại thời
  điểm đọc; frontend không được tin cờ chuyên gia do client gửi lên.
- User thường, applicant chưa duyệt, Expert rejected/expired/suspended/revoked
  không được đăng nội dung với tích/nhãn chuyên gia.
- Upload/view/replace/delete tài liệu Community phải kiểm tra ownership,
  authorization và chống orphan object; không làm lộ signed URL hoặc tài liệu
  private ngoài phạm vi được phép.
- Phân biệt tài liệu chia sẻ trong Community với credential/CCCD/tài liệu xác minh
  chuyên gia; tuyệt đối không công khai evidence xác minh.
- Points/history/badges.
- Backend-calculated only.
- Client không tự gửi điểm tùy ý.
- Idempotent awards.
- Chống duplicate.
- Điểm/đóng góp từ trả lời câu hỏi hoặc đăng tài liệu chỉ được cộng một lần cho
  mỗi hành động hợp lệ.
- Authorization.
- Notification/audit.
- Web/Flutter wiring.
- Tests.

12.6. Expert Directory/Public Profile

- Chỉ hiển thị verified Expert.
- Ẩn rejected/expired/suspended/revoked.
- Search/filter/specialty/facility/pagination.
- Không lộ CCCD/documents/CompreFace/private review.
- Không lộ private contact nếu không được phép.
- Web/Flutter wiring.
- Tests.

12.7. Location/Nearby Support

Nếu có:

- Explicit opt-in.
- Location permission.
- Visibility.
- Share/unshare.
- Expiry nếu supported.
- Không share mặc định.
- Chỉ verified/available Expert.
- Privacy/audit/authorization.
- Không dùng workplace coordinates làm live location.
- Web/Flutter wiring.
- Tests.

12.8. Notifications

Hoàn thiện notification cho:

- Submission received.
- Resubmission requested.
- Rejected.
- Approved.
- Verification expiring/expired nếu supported.
- Suspended/revoked.
- Consultation request.
- Facility verification result nếu cần.

Yêu cầu:

- Đúng recipient.
- Idempotency.
- Không duplicate.
- Không lộ sensitive data.
- Read/unread.
- Deep link đúng.
- Web/Flutter wiring.
- Tests.

==================================================
13. WEB ROUTES VÀ FLUTTER ROUTES
==================================================

Web inventory:

- Expert onboarding.
- Profile.
- Verification status.
- Verification documents.
- Resubmission.
- Dashboard.
- Availability.
- Consultation.
- Q&A.
- Contribution/badges.
- Notifications.
- Nearby support/location.
- Account/security.

Kiểm tra:

- Router registration.
- Lazy import.
- Menu/navigation.
- Route guard.
- Deep link.
- Page refresh.
- Back/forward.
- Loading/empty/error.
- API wiring.
- Unauthorized/forbidden.

Không để screen có source nhưng không có route/navigation.

Flutter inventory:

- Register/login.
- Onboarding.
- Profile.
- Verification.
- Three-image upload.
- Credentials.
- Status/resubmission.
- Dashboard.
- Availability.
- Consultation.
- Q&A.
- Contribution.
- Notifications.
- Nearby support/location.
- Emergency Map.

Kiểm tra:

- Route registration.
- Initial navigation theo role/status.
- Deep link.
- State management.
- API client.
- Token/session.
- Multipart upload.
- Signed file view.
- File picker.
- Camera/gallery nếu có.
- Permission.
- Loading/error/empty/offline.
- Feature gating.

==================================================
14. VISIBLE MICROSOFT EDGE AUTOMATION
==================================================

Sau `/mcp`, Git check, đọc SQL và khởi động Docker/runtime:

- Mở Microsoft Edge visible/headed.
- Điều hướng đến CareBridge Web.
- Chạy automation trực tiếp trước mắt tôi.
- Chỉ mở một cửa sổ Edge automation visible tại một thời điểm.
- Tái sử dụng cửa sổ/tab hiện tại trong cùng một test flow khi có thể.
- Kết thúc mỗi test flow phải đóng toàn bộ tab/cửa sổ do flow đó tạo ra.
- Test fail, exception hoặc timeout cũng bắt buộc chạy teardown và đóng cửa sổ.
- Chỉ sau khi cửa sổ của flow trước đã đóng mới được mở giao diện cho flow kế
  tiếp, tránh tích tụ nhiều cửa sổ gây rối.

Không được:

- Dùng Chrome.
- Dùng headless.
- Chỉ chạy API/unit tests.
- Chỉ chụp screenshot sau test ngầm.
- Chỉ mô tả bằng văn bản.

Automation phải có tốc độ quan sát được:

- Nhập form rõ ràng.
- Mở dropdown.
- Click button.
- Chờ UI/API ổn định.
- Không dùng CCCD/dữ liệu thật.
- Dùng fixture an toàn.

Baseline trước khi sửa:

A. Expert Applicant

1. Login/register.
2. Onboarding.
3. Province dropdown.
4. Ward dropdown.
5. TrackAsia hospital search.
6. Select hospital.
7. Upload selfie.
8. Upload CCCD trước.
9. Upload CCCD sau.
10. Upload PDF/DOC/DOCX.
11. Submit nếu có thể.
12. Ghi UI/API errors.

B. Admin

1. Admin login.
2. Notification.
3. Verification queue.
4. Applicant detail.
5. Ba ảnh.
6. Documents.
7. CompreFace.
8. Facility.
9. Ghi phần thiếu.

C. Emergency Map

1. Test geolocation an toàn.
2. Nearby Search.
3. Candidate hospitals.
4. Marker mẹ bầu.
5. Marker bệnh viện.
6. Polyline.
7. Distance/ETA.
8. Fallback.

Thu thập:

- Console errors.
- Failed network requests.
- HTTP errors.
- Route.
- Screenshot.
- Không xuất token/cookie/secret/signed URL.

Chỉ sau baseline mới sửa business logic.

==================================================
15. AUDIT VÀ IMPLEMENTATION
==================================================

Dùng code-review-graph lập ma trận:

Requirement
→ Web/Flutter Screen
→ API
→ Controller
→ Service
→ Repository
→ Entity
→ Bảng/cột trong V1__init_schema.sql
→ Test.

Phát hiện và sửa:

- Sai bảng.
- Bảng legacy.
- Entity/schema mismatch.
- Query dùng column không tồn tại.
- API thiếu/chưa wiring.
- Route/navigation thiếu.
- Frontend-only gating.
- Backend authorization thiếu.
- Notification/audit thiếu.
- Orphan storage.
- Replace/delete sai lifecycle.
- CompreFace auto reject.
- TrackAsia v1/outdated.
- TrackAsia thiếu key.
- Parser sai v2.
- Province API gọi trực tiếp frontend.
- Import toàn bộ hospital vào SQL.
- Web/Flutter không đồng nhất.

Không dừng ở báo cáo. Tiếp tục implementation trong giới hạn schema.

==================================================
16. TEST, DOCKER VÀ RUNTIME
==================================================

Backend:

- Compile.
- Unit tests.
- PostgreSQL Testcontainers.
- Integration tests.
- Security tests.
- Schema validation.
- Concurrency tests.
- Expert lifecycle tests.
- Cloudinary lifecycle tests.
- R2 lifecycle tests.
- CompreFace Docker integration.
- Notification/audit tests.
- Gating tests.
- Province proxy/cache tests.
- TrackAsia tests.

Docker:

1. PostgreSQL local canonical.
2. CompreFace local canonical.
3. Backend.
4. Web.
5. Health/readiness.
6. Không restart loop.
7. Không log secret.
8. Không thêm Redis.
9. Không xóa volume.
10. Không reset DB.

CompreFace runtime:

- Match.
- Low score.
- No face.
- Multiple faces.
- Timeout.
- Unavailable.
- Unauthorized.
- Resubmission.

Web:

- Install dependencies nếu cần.
- Typecheck.
- Unit/component tests.
- Build.
- Runtime.
- Edge visible automation.

Flutter:

- `flutter pub get`.
- `flutter analyze`.
- `flutter test`.
- Android debug build.
- Android emulator/device.
- Integration test/Appium/ADB nếu khả dụng.

Không dùng Edge responsive mode để tuyên bố Flutter pass.

Nếu emulator/device không khả dụng:

- Hoàn thành analyze/test/build.
- Báo blocker.
- Không tuyên bố Android runtime pass.

==================================================
17. EDGE AUTOMATION SAU IMPLEMENTATION
==================================================

Chạy lại visible Edge automation trước mắt tôi.

Quy tắc quản lý cửa sổ trong toàn bộ các flow dưới đây:

- Chạy tuần tự từng flow với tối đa một cửa sổ Edge test visible.
- Sau mỗi flow, lưu bằng chứng cần thiết rồi đóng tab/cửa sổ automation.
- Xác nhận không còn cửa sổ test dư trước khi bắt đầu flow tiếp theo.
- Không để lại cửa sổ Edge test sau khi toàn bộ automation kết thúc.
- Không đóng cửa sổ Edge cá nhân không do automation tạo.

Flow 1 — Applicant:

Register/Login
→ Onboarding
→ Province/Ward
→ TrackAsia hospital
→ Select facility
→ Upload ba ảnh
→ Upload PDF/DOC/DOCX
→ CompreFace advisory
→ Submit
→ Pending.

Flow 2 — Admin:

Login
→ Notification
→ Queue
→ Applicant detail
→ View ba ảnh
→ View credentials
→ View score/threshold/badge
→ Verify facility
→ Approve.

Flow 3 — Verified Expert:

Login
→ Dashboard
→ Profile
→ Availability
→ Consultation nếu có
→ trả lời câu hỏi Community và kiểm tra tích/nhãn Chuyên gia
→ đăng bài kèm tài liệu Community và kiểm tra tích/nhãn Chuyên gia
→ Contribution/Badges nếu có
→ Notifications
→ Expert-only routes.

Flow 4 — Resubmit:

Admin request resubmission
→ Expert notification
→ Replace evidence
→ Submit lại
→ Admin review lại.

Flow 5 — Reject/Gating:

Admin reject có lý do
→ Expert notification
→ Official features bị chặn
→ Direct URL/API trả forbidden.

Flow 6 — Status:

Approved/expired/suspended/revoked test state an toàn
→ Gating cập nhật
→ Directory ẩn invalid Expert
→ Renewal/reinstatement nếu supported.

Flow 7 — Emergency Map:

GPS test
→ Nearby
→ Directions/Matrix
→ chọn theo ETA
→ marker
→ polyline
→ distance
→ duration
→ disclaimer
→ fallback.

Edge DevTools kiểm tra:

Console:

- Không uncaught exception.
- Không route error.
- Không CORS error.
- Không log secret/token/signed URL.

Network:

- Province request qua Backend.
- Frontend không gọi Province API trực tiếp.
- TrackAsia request qua Backend.
- Frontend không lộ key.
- Upload qua API bảo vệ.
- Unauthorized trả 401/403.

Screenshot checkpoints:

1. Onboarding.
2. Province/Ward.
3. Hospital suggestions.
4. Ba ảnh.
5. Documents.
6. Pending.
7. Admin notification.
8. Admin xem đủ ba ảnh.
9. CompreFace badge.
10. Facility verification.
11. Approve/reject/resubmit.
12. Gating.
13. Expert dashboard.
14. Emergency Map marker/polyline/ETA.

==================================================
18. FINAL REVIEW VÀ DEFINITION OF DONE
==================================================

Chạy code-review-graph lại trên diff cuối.

Review:

- Schema violation.
- Tạo bảng/cột trái phép.
- Security.
- Authorization.
- Privacy.
- Cloudinary/R2 lifecycle.
- Signed URL.
- Orphan object.
- Transaction.
- Route/navigation.
- Feature gating.
- Duplicate notification.
- Audit gaps.
- CompreFace auto-decision.
- Province proxy/cache.
- TrackAsia request/parser/fallback.
- Edge cases.

Sửa finding nghiêm trọng và chạy lại test liên quan.

Không được báo hoàn thành chỉ vì:

- Compile pass.
- Unit test pass.
- Cleanup xong.
- Một màn hình hoạt động.
- Backend xong nhưng Web/Flutter chưa wiring.
- Web xong nhưng Android chưa kiểm tra.
- Headless pass nhưng Edge visible chưa chạy.
- CompreFace mock pass nhưng Docker local chưa chạy.
- H2 pass nhưng PostgreSQL/Testcontainers chưa chạy.

Definition of Done:

- Toàn bộ Expert inventory đã audit.
- Feature trong phạm vi schema hiện tại đã hoàn thiện.
- Backend/Web/Flutter wiring thống nhất.
- PostgreSQL Docker local healthy.
- PostgreSQL Testcontainers pass.
- CompreFace Docker local healthy.
- Edge visible automation chạy trước mắt tôi.
- Flutter Android build/runtime được kiểm tra nếu môi trường cho phép.
- Finding nghiêm trọng đã sửa.
- Không tự tạo bảng/cột.
- Không làm lộ secret.
- Không thêm Redis.

==================================================
19. BÁO CÁO CUỐI
==================================================

Báo cáo phải có:

1. Kết quả `/mcp`.
2. MCP available/unavailable.
3. Bằng chứng code-review-graph.
4. Bằng chứng Edge DevTools.
5. Xác nhận Edge visible/headed.
6. Baseline trước khi sửa.
7. Xác nhận đã đọc hai SQL.
8. Bảng/cột canonical đã dùng.
9. Xác nhận không tạo bảng/cột.
10. Đề xuất schema chờ phê duyệt nếu có.
11. Requirement → implementation → database → test matrix.
12. File đã thay đổi.
13. Backend endpoints.
14. Web routes.
15. Flutter routes.
16. Expert features tìm thấy.
17. Expert features đã hoàn thiện.
18. Expert features bị chặn.
19. Verification state-transition matrix.
20. Feature-gating matrix.
21. Notification matrix.
22. Audit-event matrix.
23. Storage access/ownership matrix.
24. Province proxy/cache implementation.
25. TrackAsia Search/Autocomplete implementation.
26. Cách chỉ lưu hospital được chọn.
27. Facility verification.
28. Emergency Map implementation.
29. Cloudinary `.env` variable names đã dùng, không hiển thị value.
30. R2 `.env` variable names đã dùng, không hiển thị value.
31. PostgreSQL Docker Compose/config đã dùng.
32. PostgreSQL/Testcontainers result.
33. CompreFace Compose/config đã dùng.
34. CompreFace runtime scenarios.
35. Compile/test/build/runtime results.
36. Edge console/network findings.
37. Edge screenshots.
38. Flutter Android evidence.
39. Blockers và risks.
40. Xác nhận:
    - không sync dev;
    - không restore stash;
    - không push;
    - không tạo bảng/cột;
    - không reset database;
    - không xóa Docker volume;
    - không thêm Redis;
    - không dùng Chrome thay Edge;
    - không dùng headless thay visible;
    - chỉ dùng một cửa sổ Edge test visible tại một thời điểm;
    - đã đóng mọi tab/cửa sổ automation sau từng test flow và khi kết thúc;
    - không đóng hoặc can thiệp cửa sổ Edge cá nhân;
    - không dùng Edge thay Flutter Android.

Không được giả mạo kết quả test.

Nếu bị chặn bởi credential, API key, Docker, MCP hoặc emulator:

- Hoàn thành phần không bị chặn.
- Ghi blocker cụ thể.
- Không in secret.
- Không tạo credential giả.
- Không tạo bảng/cột để né blocker.
- Cung cấp bước chính xác để tiếp tục.
