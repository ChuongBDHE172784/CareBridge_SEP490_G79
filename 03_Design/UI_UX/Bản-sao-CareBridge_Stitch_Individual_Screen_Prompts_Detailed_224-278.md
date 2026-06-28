### Prompt 224 — `CB-224` — Unified Search & Filter (Mobile)

**Platform:** Shared Mobile Apps  
**Primary role:** User  
**Functional coverage:** UC-13

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-224`
Platform: Shared Mobile Apps
Primary role: User
Feature: Shared / Cross-platform
Reference screen name: “Unified Search & Filter”
Purpose: Lets users search questions, posts, experts, records, or data within their authorized access scope.
Functional coverage: UC-13

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Thanh tìm kiếm có nút xóa, lịch sử tìm gần đây và trạng thái đang tìm.
2. Tabs kết quả: Cộng đồng · Chuyên gia · Nội dung · Hồ sơ/tệp được cấp quyền; chỉ hiển thị loại dữ liệu role hiện tại được phép xem.
3. Filter chips theo chủ đề, giai đoạn, trạng thái trả lời/xác thực, khoảng cách khi có consent vị trí, và khoảng thời gian khi phù hợp.
4. Danh sách kết quả hiển thị loại nguồn, nhãn xác thực/chuyên gia, mô tả ngắn và CTA mở chi tiết; có empty state và liên kết xóa bộ lọc.

Required interaction and state design: Show a populated query state plus one compact no-result state. Keep sensitive results masked until access is confirmed; prevent global search from exposing other users’ private health data. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 225 — `CB-225` — Unified Search & Filter (Web)

**Platform:** Shared Web Portals  
**Primary role:** Shared Web User  
**Functional coverage:** UC-13

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-225`
Platform: Shared Web Portals
Primary role: Shared Web User
Feature: Shared / Cross-platform
Reference screen name: “Unified Search & Filter”
Purpose: Lets portal users search data that they are authorized to access.
Functional coverage: UC-13

Canvas and navigation: WEB ONLY. Render a reusable signed-in web workspace with a compact top bar and a neutral contextual sidebar; do not use a phone frame or mobile bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Thanh tìm kiếm toàn cục ở đầu workspace, bộ lọc dạng panel gọn và active-filter chips có nút xóa từng điều kiện.
2. Tabs: Cộng đồng · Chuyên gia · Nội dung · Hồ sơ/tệp được cấp quyền; hiển thị số kết quả và giữ role scope rõ ràng.
3. Bảng/danh sách kết quả một cột dễ quét, có type icon, nhãn verified/source, metadata ngắn, CTA xem chi tiết.
4. Trạng thái loading, không có kết quả, lỗi mạng có retry, và quyền truy cập bị từ chối.

Required interaction and state design: Do not mix portal-specific administration data into generic user search; show only allowed result types for the signed-in role. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 226 — `CB-226` — Report Content or Account (Mobile)

**Platform:** Shared Mobile Apps  
**Primary role:** User  
**Functional coverage:** UC-14

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-226`
Platform: Shared Mobile Apps
Primary role: User
Feature: Shared / Cross-platform
Reference screen name: “Report Content or Account”
Purpose: Collects reports about misleading content, disguised advertising, harassment, or unsafe advice.
Functional coverage: UC-14

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Card ngữ cảnh rút gọn của bài viết/câu trả lời/tài khoản đang bị report; không lộ thêm dữ liệu riêng tư ngoài ngữ cảnh cần thiết.
2. Danh sách lý do bắt buộc: Thông tin sai lệch · Quảng cáo trá hình · Công kích/quấy rối · Tư vấn không an toàn · Khác.
3. Ô mô tả bổ sung có giới hạn ký tự; tùy chọn đính kèm bằng chứng chỉ khi scope cho phép.
4. Nút “Gửi báo cáo”, nút hủy, thông báo xác nhận có mã report giả lập và trạng thái không gửi trùng.

Required interaction and state design: Show inline validation for no reason selected and a safe message that urgent danger should use emergency support rather than wait for moderation. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 227 — `CB-227` — Report Content or Account (Web)

**Platform:** Shared Web Portals  
**Primary role:** Shared Web User  
**Functional coverage:** UC-14

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-227`
Platform: Shared Web Portals
Primary role: Shared Web User
Feature: Shared / Cross-platform
Reference screen name: “Report Content or Account”
Purpose: Provides a web dialog for submitting a report about content or an account.
Functional coverage: UC-14

Canvas and navigation: WEB ONLY. Render a reusable signed-in web workspace with a compact top bar and a neutral contextual sidebar; do not use a phone frame or mobile bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Modal/dialog nổi trên trang ngữ cảnh, có tóm tắt đối tượng bị report và nút đóng rõ ràng.
2. Reason selector, optional notes, optional permitted evidence attachment, privacy note, and submit button.
3. Submit loading, success toast/state, disabled duplicate-submit state, and validation message.
4. Không có action trừng phạt trực tiếp cho reporter; chỉ gửi vào moderation queue.

Required interaction and state design: Keep report evidence restricted to authorized moderators; no unnecessary private profile fields. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 228 — `CB-228` — Revoke Data Sharing Permission (Mobile)

**Platform:** Shared Mobile Apps  
**Primary role:** User  
**Functional coverage:** UC-18

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-228`
Platform: Shared Mobile Apps
Primary role: User
Feature: Shared / Cross-platform
Reference screen name: “Revoke Data Sharing Permission”
Purpose: Lets users revoke previously granted access for a family member or expert.
Functional coverage: UC-18

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Recipient card nêu người nhận, mục đích, phạm vi dữ liệu, ngày bắt đầu và thời hạn hiện tại.
2. Danh sách scope đang chia sẻ: lịch, nhật ký, hồ sơ, summary hoặc vị trí; mỗi mục chỉ đọc trong màn xác nhận.
3. Cảnh báo ngắn về tác động: phiên xem đang hoạt động sẽ mất quyền; dữ liệu đã tải hợp lệ trước đó không bị xóa hồi tố.
4. CTA nguy hiểm “Thu hồi quyền”, nút quay lại; confirmation text/checkbox để tránh thao tác nhầm.

Required interaction and state design: Show successful revocation state with timestamp and audit note; block repeated submits. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 229 — `CB-229` — Revoke Data Sharing Permission (Web)

**Platform:** Shared Web Portals  
**Primary role:** Shared Web User  
**Functional coverage:** UC-18

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-229`
Platform: Shared Web Portals
Primary role: Shared Web User
Feature: Shared / Cross-platform
Reference screen name: “Revoke Data Sharing Permission”
Purpose: Provides a web dialog for revoking an existing consent grant.
Functional coverage: UC-18

Canvas and navigation: WEB ONLY. Render a reusable signed-in web workspace with a compact top bar and a neutral contextual sidebar; do not use a phone frame or mobile bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Dialog xác nhận với recipient, purpose, scope, expiry và trạng thái current/expired.
2. Impact note, required confirmation checkbox, revoke CTA, cancel CTA.
3. Audit timestamp preview, loading and success state; an access-denied state when the owner is not the signed-in user.

Required interaction and state design: Do not let a web user revoke another person’s consent or alter historical audit records. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 230 — `CB-230` — Edit Maternal Health Metric (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-26

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-230`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Edit Maternal Health Metric”
Purpose: Edits an incorrectly entered maternal health metric record.
Functional coverage: UC-26

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Header có loại chỉ số và thời điểm bản ghi; selector loại chỉ số chỉ cho các loại đã được cấu hình.
2. Trường giá trị, đơn vị, ngày/giờ đo, nguồn “Tự nhập”, ghi chú tùy chọn và liên kết tới record liên quan nếu có.
3. Nút lưu thay đổi; action xóa/chỉnh sửa history chỉ hiển thị khi user là owner.
4. Hiển thị giá trị trước đó nhỏ gọn để tránh sửa nhầm.

Required interaction and state design: Validate numeric range and required date without diagnosing. Show unsaved-change confirmation when leaving. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 231 — `CB-231` — View Maternal Health Trend (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-27

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-231`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Maternal Health Metric Trend”
Purpose: Shows time-based trends for maternal metrics without providing a diagnosis.
Functional coverage: UC-27

Canvas and navigation: MOBILE ONLY. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination. No desktop browser chrome, sidebar, wide tables, or multi-column dashboard.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Metric selector, range selector 7 ngày/30 ngày/tùy chọn và chips theo nguồn dữ liệu.
2. Biểu đồ đường đơn giản với tooltips giá trị/ngày, label đơn vị và các điểm dữ liệu thiếu.
3. Summary cards chỉ mô tả “tăng/giảm so với kỳ trước”, không dùng ngôn ngữ chẩn đoán.
4. CTA thêm chỉ số và mở chi tiết bản ghi; safe note ngắn “Trao đổi chuyên gia nếu bạn lo lắng”.

Required interaction and state design: Include empty state with CTA add first record and a data-quality/source label. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 232 — `CB-232` — Pregnancy Exercise Library (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-29

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-232`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Pregnancy Exercise Library”
Purpose: Lists approved exercises so the mother can filter and select a suitable activity.
Functional coverage: UC-29

Canvas and navigation: MOBILE ONLY. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination. No desktop browser chrome, sidebar, wide tables, or multi-column dashboard.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Search và filter chips: tam cá nguyệt, thời lượng, mức độ, mục tiêu; chỉ hiển thị bài active/approved.
2. Exercise cards có tên ngắn, thời lượng, mức độ, triệt tiêu nếu không phù hợp giai đoạn, safety badge và CTA “Xem bài tập”.
3. Nút mở safety check trước khi bắt đầu; không tự phát video hoặc camera.
4. Saved/recent section nhỏ gọn, empty state và link xóa filter.

Required interaction and state design: Use movement visuals only as neutral thumbnails; never claim therapy or medical treatment. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 233 — `CB-233` — Edit Baby Profile (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-32

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-233`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Edit Baby Profile”
Purpose: Updates information in a baby profile managed by the mother.
Functional coverage: UC-32

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Avatar/nickname, ngày sinh, giới tính, cân nặng/chiều dài lúc sinh; tất cả dữ liệu owner-only.
2. Option chọn bé đang hoạt động nếu gia đình có nhiều hồ sơ; không xóa bé từ màn này.
3. Nút lưu, cancel/back, validation ngày sinh và numeric birth metrics.
4. Privacy label: dữ liệu chỉ được chia sẻ theo consent hiện tại.

Required interaction and state design: Show unsaved-change confirmation and block editing when user is not the profile owner. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 234 — `CB-234` — Edit Baby Daily Log (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-35

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-234`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Edit Baby Daily Log”
Purpose: Edits or removes an entered feeding, sleep, diaper, or symptom log.
Functional coverage: UC-35

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Log-type selector; fields thay đổi theo loại: thời điểm, lượng/đơn vị, thời lượng, trạng thái, ghi chú.
2. Source label “Tự nhập”; liên kết hồ sơ bé và date/time rõ ràng.
3. Nút lưu; action xóa mở confirmation sheet riêng trong cùng frame.
4. Validation theo log type và cảnh báo khi thời điểm nằm trong tương lai.

Required interaction and state design: Never infer a diagnosis from symptom log values. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 235 — `CB-235` — View Baby Log Summary (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-36

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-235`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Baby Daily Log Summary”
Purpose: Shows a 24-hour or 7-day summary of feeding, sleep, diapers, and symptoms.
Functional coverage: UC-36

Canvas and navigation: MOBILE ONLY. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination. No desktop browser chrome, sidebar, wide tables, or multi-column dashboard.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Switch 24 giờ/7 ngày và baby selector.
2. Cards tổng hợp cho bú, ngủ, tã và triệu chứng; each card has source label, count/duration and open-detail action.
3. Timeline mini chart/list with data gaps and date selector.
4. CTA ghi nhanh nhật ký và empty state khi chưa có dữ liệu.

Required interaction and state design: Use descriptive summaries only; no normal/abnormal medical conclusion. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 236 — `CB-236` — Record Development Milestone (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-37

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-236`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Record Development Milestone”
Purpose: Lets the mother record a baby development milestone with a date and note.
Functional coverage: UC-37

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Milestone selector: lẫy, bò, đi, nói, mọc răng, ăn dặm hoặc “Khác”.
2. Ngày xảy ra, hồ sơ bé, ghi chú tùy chọn, tùy chọn ảnh chỉ khi file permission hợp lệ.
3. Nút lưu mốc; validation ngày và required milestone.
4. Nhãn “Mẹ ghi nhận” để phân biệt dữ liệu tự nhập.

Required interaction and state design: Do not show normative developmental diagnosis; show a neutral suggestion to discuss concerns with a professional. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 237 — `CB-237` — Edit Health Record (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-40

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-237`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Edit Health Record”
Purpose: Edits metadata for a health record entered by the mother.
Functional coverage: UC-40

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. File preview/thumbnail, record title, loại hồ sơ, tags, ngày, đối tượng liên quan mẹ/bé và ghi chú.
2. Hiển thị source “Tự tải lên” hoặc “Tự nhập”, owner và access status.
3. Nút lưu; file binary không bị thay thế vô tình—dùng CTA riêng “Thay tệp” nếu có quyền.
4. Validation required title/type/date and file error state.

Required interaction and state design: Show consent summary when record is currently shared; do not expand recipient data beyond minimum necessary. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 238 — `CB-238` — Delete or Archive Health Record (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-41

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-238`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Delete or Archive Health Record”
Purpose: Confirms the soft deletion or archival of a self-entered health record.
Functional coverage: UC-41

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Record summary với tên, loại, owner, liên kết và trạng thái chia sẻ.
2. Choice rõ ràng: Lưu trữ để ẩn khỏi timeline hoặc Xóa mềm; giải thích ngắn sự khác nhau.
3. Reason optional; CTA destructive requires explicit confirmation.
4. Disabled destructive action when retention/record-link rule blocks deletion, with safe explanation.

Required interaction and state design: Never claim immediate physical deletion; show audit timestamp after action. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 239 — `CB-239` — Generate Health Summary (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-43

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-239`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Generate Health Summary”
Purpose: Selects data to generate a 24-hour, 7-day, or consultation health summary.
Functional coverage: UC-43

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Purpose selector: 24 giờ · 7 ngày · Phiên tư vấn; date range and owner/baby selector.
2. Data-category toggles: chỉ số, nhật ký, vaccine, hồ sơ, nhắc lịch; show item count per category.
3. Preview card with source labels and data limitations; CTA “Tạo tóm tắt”.
4. Option to save locally but no automatic sharing.

Required interaction and state design: Summary must be framed as user-selected information, not a diagnosis; show generation loading and no-data state. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 240 — `CB-240` — Share Health Summary with Expert (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-44

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-240`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Share Health Summary with Expert”
Purpose: Grants time-limited permission to share a selected summary or record with an expert.
Functional coverage: UC-44

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Expert recipient card with verified badge, consultation context and identity.
2. Select summary/records, granular scope chips, purpose selector, expiry date/time and optional revoke reminder.
3. Consent preview states what the expert can see and until when; owner remains Mother.
4. CTA “Cấp quyền chia sẻ”, cancel and confirmation state; current/expired consent shown clearly.

Required interaction and state design: No share without active consultation or explicit consent. Do not expose raw records outside selected scope. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 241 — `CB-241` — Create Medication or Vitamin Reminder (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-46

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-241`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Create Medication or Vitamin Reminder”
Purpose: Creates a reminder based on the user’s existing instruction; it does not prescribe medication.
Functional coverage: UC-46

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Tên thuốc/vitamin tự nhập, đối tượng mẹ/bé, lịch giờ, tần suất, ngày bắt đầu/kết thúc và note “Theo hướng dẫn đã có”.
2. Optional link tới record/đơn đã lưu, but never suggest dose or medical instruction.
3. Reminder notification channel toggle if enabled and primary save CTA.
4. Validation for required name/time/frequency and schedule conflict/duplicate warning.

Required interaction and state design: Use a clear non-prescription label; no dosage recommendation logic. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 242 — `CB-242` — Create Vaccination Reminder (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-47

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-242`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Create Vaccination Reminder”
Purpose: Creates or confirms a baby vaccination reminder from a reference schedule or a mother-entered date.
Functional coverage: UC-47

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Baby selector, vaccine/reminder label, target date, reminder lead time and source: lịch tham khảo hoặc tự nhập.
2. Clinic/location optional field without implying a booking.
3. CTA lưu nhắc; validation for selected baby/date and duplicate reminder warning.
4. Tiny reference/source label and link to vaccination record after completion.

Required interaction and state design: Do not state clinical eligibility or diagnosis; user can verify date with a qualified provider. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 243 — `CB-243` — Update or Snooze Reminder (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-48

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-243`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Update or Snooze Reminder”
Purpose: Lets users edit the time, snooze, complete, or skip a reminder.
Functional coverage: UC-48

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Reminder card with type, owner, due time, recurrence and current status.
2. Action sheet choices: đổi giờ/ngày, hoãn 15 phút/1 giờ/tùy chỉnh, hoàn thành, bỏ qua occurrence.
3. Reason optional for skip and a separate destructive confirmation for deleting recurrence.
4. Show next occurrence preview after a change.

Required interaction and state design: Prevent accidental change to all recurring items; show scope “chỉ lần này” vs “cả chuỗi” where relevant. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 244 — `CB-244` — Edit Expense (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-52

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-244`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Edit Expense”
Purpose: Edits or removes an expense entered by the mother.
Functional coverage: UC-52

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Category selector, amount/currency, date, related journey/baby, merchant/note optional.
2. Source label “Tự nhập”; editable only by owner.
3. Save CTA, delete action with confirmation, and validation for non-negative amount/date.
4. Previous value summary to reduce accidental edits.

Required interaction and state design: No financial advice or insurance claims. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 245 — `CB-245` — Post Community Answer (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** User  
**Functional coverage:** UC-56

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-245`
Platform: Mother Mobile App
Primary role: User
Feature: CareBridge operational feature
Reference screen name: “Post Community Answer”
Purpose: Composes and submits a personal-experience answer on a community question detail.
Functional coverage: UC-56

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Question context card: author display mode, topic, excerpt, moderation/urgency labels, and report action.
2. Answer composer with text area, counter, optional concise experience tags, and mandatory label “Chia sẻ kinh nghiệm”.
3. Visible safe-scope warning: không chẩn đoán, không kê thuốc, không thay thế cấp cứu; block prohibited medical-prescription wording with neutral validation.
4. Primary “Đăng trả lời”, draft/cancel, loading and submitted/pending-moderation result state.

Required interaction and state design: This is the missing dedicated UI contract: do not collapse it into a generic feed card. Expert-only badge must never be shown to ordinary users. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 246 — `CB-246` — Post Anonymously in Community (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-57

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-246`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Use Anonymous Community Display”
Purpose: Sets anonymous display preferences when the mother posts a sensitive question.
Functional coverage: UC-57

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Post preview showing public display vs anonymous display side-by-side without exposing private identity.
2. Anonymous toggle, privacy explanation, topic/stage context remains visible only at allowed granularity.
3. Warning that moderators may still access identity under policy; user controls the community-facing display only.
4. Confirm anonymous setting and continue-to-question-composer CTA.

Required interaction and state design: No false anonymity claim; preserve audit/moderation trace internally but do not show it to community users. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 247 — `CB-247` — View Saved Community Posts (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** User  
**Functional coverage:** UC-58

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-247`
Platform: Mother Mobile App
Primary role: User
Feature: CareBridge operational feature
Reference screen name: “Saved Community Posts”
Purpose: Shows and manages the community posts and questions that the user has bookmarked.
Functional coverage: UC-58

Canvas and navigation: MOBILE ONLY. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination. No desktop browser chrome, sidebar, wide tables, or multi-column dashboard.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Saved posts list with topic, author display type, saved date, answer/verified labels, and CTA mở chi tiết.
2. Remove-bookmark action with undo toast; do not delete the original post.
3. Search/filter by topic and status, plus clear empty state with CTA khám phá cộng đồng.
4. Only private saved list of current user.

Required interaction and state design: Do not expose a user’s saved items to family, experts, moderators, or other community members. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 248 — `CB-248` — Find Nearby Care Facility (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-63

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-248`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Find Nearby Care Facility”
Purpose: Finds a care facility near the current location when support is needed.
Functional coverage: UC-63

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Map/list toggle with location permission state; search radius and facility filters.
2. Facility cards with name, type, distance, ETA, opening/contact availability and data-source note.
3. CTA mở chi tiết, gọi nhanh hoặc chỉ đường; no implied appointment booking.
4. Safe fallback when location is denied: nhập khu vực hoặc xem danh sách không định vị.

Required interaction and state design: Include emergency-care disclaimer for red-flag context; never claim dispatch or guaranteed availability. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 249 — `CB-249` — Quick Call or Navigate (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-64

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-249`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Quick Call or Navigate”
Purpose: Lets users call a hotline or facility, or open navigation from a selected facility.
Functional coverage: UC-64

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Selected facility card with phone, address, distance/ETA and status source.
2. Two primary actions: Gọi ngay and Chỉ đường; include a safe confirmation before placing call/opening external map.
3. Permission/connection state for map, and a fallback copy-address action.
4. Explicit warning that CareBridge is not an emergency dispatch service.

Required interaction and state design: Do not fabricate emergency number or facility capability; use fictional placeholders only. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 250 — `CB-250` — Import Device Data Manually (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-67

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-250`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Import Device Data Manually”
Purpose: Imports or mocks device health data when the user grants permission.
Functional coverage: UC-67

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Source selector fixed to “Nhập thủ công/Mock”; date/time and measurement-type tabs: nhịp tim, giấc ngủ, bước chân, SpO2, huyết áp.
2. Value fields with units and a data-quality/accuracy note; no bulk sensitive data preview.
3. Save CTA, field validation, and source label shown in preview.
4. Link to connected device screen without pretending sync is active.

Required interaction and state design: Never infer diagnosis from imported values; sensitive device data remains owner-controlled. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 251 — `CB-251` — Disconnect Health Device (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-68

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-251`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Disconnect Health Device”
Purpose: Confirms disconnecting a wearable or health platform and stopping future synchronization.
Functional coverage: UC-68

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Connected-device summary: device/platform name, last sync, categories currently syncing and permission status.
2. Clear explanation that future sync stops; previously saved records remain according to retention policy.
3. Destructive CTA “Ngắt kết nối”, cancel, confirmation checkbox and success state.
4. Show reconnect action only after disconnect succeeds.

Required interaction and state design: Do not silently delete historical health records or revoke unrelated permissions. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 252 — `CB-252` — Invite Family Member to Care Group (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-71

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-252`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Invite Family Member to Care Group”
Purpose: Invites a family member to join a care group by phone number, link, or QR code.
Functional coverage: UC-71

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Care-group selector, relationship label optional, invitation method tabs: số điện thoại · link · QR.
2. Recipient contact validation, optional concise message and invitation expiry.
3. Data sharing is not granted here; show explicit next step to configure permissions after acceptance.
4. Send invitation CTA, resend/copy link state and pending-invitation list preview.

Required interaction and state design: Do not expose health data by invitation alone; invitation is separate from consent. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 253 — `CB-253` — Manage Family Permissions (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-72

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-253`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Manage Family Permissions”
Purpose: Sets or updates a member’s access scope for schedules, logs, alerts, and records.
Functional coverage: UC-72

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Member card, relationship, status and last active context.
2. Granular permission toggles: lịch, tasks, nhật ký, cảnh báo, selected health records; each with minimum-necessary description.
3. Purpose, expiry, optional location sharing scope, save and revoke actions.
4. Consent summary plus current vs pending changes; no all-data master toggle by default.

Required interaction and state design: Make expiry and revoke prominent; show access-denied state for scopes user cannot delegate. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 254 — `CB-254` — Assign Care Task to Family Member (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-73

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-254`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Assign Family Care Task”
Purpose: Creates a care task and assigns it to a group member.
Functional coverage: UC-73

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Task title, task category, assignee selector, due date/time, reminder option and concise note.
2. Care-group context shown at top; task visibility limited to intended group.
3. Save CTA, validation for title/assignee/due date, and conflict/overdue warning.
4. Optional link to appointment/reminder but no hidden medical data is attached.

Required interaction and state design: No unsafe instruction content; task status starts “Chưa bắt đầu”. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 255 — `CB-255` — Submit Consultation Dispute or Refund Request (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-78

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-255`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Submit Dispute or Refund Request”
Purpose: Creates a dispute or refund request for an eligible consultation.
Functional coverage: UC-78

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Consultation and payment summary with date, expert, locked price, session status and eligible dispute window.
2. Reason selector: expert absent · phạm vi bị vi phạm · lỗi kỹ thuật · khác; text explanation and permitted evidence upload.
3. Requested outcome selection: review only / refund request, without promising automatic refund.
4. Submit CTA, draft/cancel, duplicate-request prevention and lifecycle state “Đã gửi chờ xử lý”.

Required interaction and state design: Keep auditable status/timestamps. Do not disclose expert private data or payment credentials. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 256 — `CB-256` — Post Community Answer as Expert (Expert App)

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-92

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-256`
Platform: Expert App
Primary role: Verified Expert
Feature: CareBridge operational feature
Reference screen name: “Post Community Answer as Expert”
Purpose: Composes and submits a public answer with an expert badge and safety boundaries.
Functional coverage: UC-92

Canvas and navigation: EXPERT APP — MOBILE ONLY. Use one 390 × 844 portrait phone UI. Use Expert App bottom navigation only for a primary destination: Trang chủ · Yêu cầu · Cộng đồng · Gần đây · Hồ sơ. Do not render browser chrome, a left sidebar, desktop tables, or a 1440 × 1024 layout.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Question context card with topic, stage, urgency and moderation labels.
2. Answer composer, expert identity/badge, specialty label and optional source/reference field.
3. Mandatory safe-scope acknowledgement: no diagnosis, prescription, or emergency replacement; concise escalation wording when appropriate.
4. Post/Save draft actions, validation, pending moderation state and no ability to expose consented health records.

Required interaction and state design: The expert app must remain a 390 × 844 mobile UI; do not render an Expert Web Portal. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Expert App only. Never show desktop sidebar, browser frame, wide table, or 1440 × 1024 layout.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 257 — `CB-257` — Post Community Answer as Expert (Expert Web Portal)

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-92

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-257`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: CareBridge operational feature
Reference screen name: “Post Community Answer as Expert”
Purpose: Composes a community answer within the Expert Web Portal.
Functional coverage: UC-92

Canvas and navigation: EXPERT WEB PORTAL — WEB ONLY. Use Expert Portal sidebar: Tổng quan · Hồ sơ · Lịch làm việc · Tư vấn · Cộng đồng · Doanh thu. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Question detail in a focused workspace with topic, reported/safety labels and prior answers.
2. Rich but concise answer editor, expert badge/specialty shown as sender identity, reference/source field, and safe-scope policy helper.
3. Actions: Lưu nháp · Gửi trả lời; validation and moderation/publish status.
4. No private patient data, diagnosis, prescription, or public display of consented consultation records.

Required interaction and state design: Expert Web Portal only. Do not create a phone frame or mobile bottom navigation. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 258 — `CB-258` — Manage Consultation Session (Expert App)

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-95

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-258`
Platform: Expert App
Primary role: Verified Expert
Feature: CareBridge operational feature
Reference screen name: “Manage Consultation Session”
Purpose: Lets an expert use the app to accept a booking, update participation status, and join an eligible session.
Functional coverage: UC-95

Canvas and navigation: EXPERT APP — MOBILE ONLY. Use one 390 × 844 portrait phone UI. Use Expert App bottom navigation only for a primary destination: Trang chủ · Yêu cầu · Cộng đồng · Gần đây · Hồ sơ. Do not render browser chrome, a left sidebar, desktop tables, or a 1440 × 1024 layout.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Consultation summary with requester, schedule, channel, consent scope, payment/lifecycle status and countdown to start.
2. Role-authorized actions based on state: chấp nhận/từ chối, xác nhận sẵn sàng, vào phiên, đánh dấu hoàn tất; disabled actions explain why.
3. Minimal shared-health-summary preview with “Xem theo consent” CTA; no full record on main action card.
4. Status transition confirmation and network fallback/reconnect state.

Required interaction and state design: Expert App must be 390 × 844 mobile only; preserve session lifecycle and do not use desktop layout. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Expert App only. Never show desktop sidebar, browser frame, wide table, or 1440 × 1024 layout.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 259 — `CB-259` — Manage Consultation Session (Expert Web Portal)

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-95

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-259`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: CareBridge operational feature
Reference screen name: “Manage Consultation Session”
Purpose: Lets an expert manage bookings and consultation participation status in the portal.
Functional coverage: UC-95

Canvas and navigation: EXPERT WEB PORTAL — WEB ONLY. Use Expert Portal sidebar: Tổng quan · Hồ sơ · Lịch làm việc · Tư vấn · Cộng đồng · Doanh thu. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Consultation workspace header with participant, scheduled slot, channel, status, payment/settlement state and consent window.
2. Timeline/audit strip, session actions based on lifecycle, and controlled entry to chat/voice/video room.
3. Protected shared-health section with scope/expiry and access-denied fallback.
4. Actions accept/decline/join/complete only when valid; show no-show or reschedule link when applicable.

Required interaction and state design: Expert Web Portal only. Do not create a phone frame or mobile bottom navigation. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 260 — `CB-260` — Revoke Expert Badge (Admin Web Portal)

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-104

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-260`
Platform: Admin Web Portal
Primary role: System Admin
Feature: CareBridge operational feature
Reference screen name: “Revoke Expert Badge”
Purpose: Suspends or revokes an expert badge when rules are violated or documents expire.
Functional coverage: UC-104

Canvas and navigation: ADMIN WEB PORTAL — WEB ONLY. Use Admin sidebar: Tổng quan · Người dùng · Chuyên gia · Đối tác · Nội dung · An toàn · Cấu hình · Nhật ký. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Expert identity, current badge, verification history, expiry/violation context and linked evidence.
2. Action type selector: tạm khóa / thu hồi; required reason, effective date/time and optional review date.
3. Notification preview, impact on public profile/consultation availability and audit summary.
4. Destructive confirm CTA with explicit acknowledgement; cancel/back and success state.

Required interaction and state design: Do not remove historical verification evidence. Only System Admin may perform this action. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 261 — `CB-261` — Create Staff Account (Admin Web Portal)

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-115

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-261`
Platform: Admin Web Portal
Primary role: System Admin
Feature: CareBridge operational feature
Reference screen name: “Create Staff Account”
Purpose: Creates a Moderator, Content Admin, or System Admin account with controlled permissions.
Functional coverage: UC-115

Canvas and navigation: ADMIN WEB PORTAL — WEB ONLY. Use Admin sidebar: Tổng quan · Người dùng · Chuyên gia · Đối tác · Nội dung · An toàn · Cấu hình · Nhật ký. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Identity fields: full name, work email, phone optional, staff role template, status and invite method.
2. Role cards: Moderator · Content Admin · System Admin, each with concise permission summary.
3. Optional department/note, account activation date and send-invite toggle.
4. Create staff account CTA, duplicate email validation, pending invitation state and no direct plaintext-password field.

Required interaction and state design: Use least-privilege defaults and log creator/time/role assignment. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 262 — `CB-262` — Update Role and Permissions (Admin Web Portal)

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-116

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-262`
Platform: Admin Web Portal
Primary role: System Admin
Feature: CareBridge operational feature
Reference screen name: “Update Role and Permissions”
Purpose: Updates the role, permissions, and access lock status of a staff account.
Functional coverage: UC-116

Canvas and navigation: ADMIN WEB PORTAL — WEB ONLY. Use Admin sidebar: Tổng quan · Người dùng · Chuyên gia · Đối tác · Nội dung · An toàn · Cấu hình · Nhật ký. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Staff account header with active/locked status, current roles, last login and audit link.
2. Role selector plus grouped permission matrix by module; inherited vs direct permissions clearly distinguished.
3. Access state controls lock/unlock, effective time, required change reason and review note.
4. Save changes CTA with impact summary, confirmation, validation and immutable audit event preview.

Required interaction and state design: Prevent self-escalation and removal of the last active system administrator. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 263 — `CB-263` — Approve Partner Service or Sponsored Campaign (Admin Web Portal)

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-124

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-263`
Platform: Admin Web Portal
Primary role: System Admin
Feature: CareBridge operational feature
Reference screen name: “Approve Partner Service or Sponsored Campaign”
Purpose: Reviews and approves a partner service or campaign, applies sponsor or partner labels, and controls medical advertising.
Functional coverage: UC-124

Canvas and navigation: ADMIN WEB PORTAL — WEB ONLY. Use Admin sidebar with Đối tác and Dịch vụ tài trợ as active context. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Partner profile summary, verification status, submitted service/campaign metadata, target audience, period and attached evidence.
2. Policy checklist for safety, disclosure, sponsor label and prohibited medical advertising.
3. Actions: Phê duyệt · Yêu cầu chỉnh sửa · Từ chối; required reasoning for non-approval.
4. Approved display preview with mandatory “Đối tác/Tài trợ” label, effective period and audit status.

Required interaction and state design: No approval without verified partner and required label. Never show private patient data. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 264 — `CB-264` — Remove Partner Content (Admin Web Portal)

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-125

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-264`
Platform: Admin Web Portal
Primary role: System Admin
Feature: CareBridge operational feature
Reference screen name: “Remove Partner Content”
Purpose: Removes a partner service listing or campaign when reported, expired, or policy-violating.
Functional coverage: UC-125

Canvas and navigation: ADMIN WEB PORTAL — WEB ONLY. Use Admin sidebar with Đối tác and Dịch vụ tài trợ as active context. Do not render a phone frame or bottom navigation.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use one 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background and surfaces are white; use #F6F1EC and #F2EAE4 only as subtle warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA or status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables and forms easy to scan, use controls at least 44–48 px, and avoid dense or decorative layouts. This is an operational portal, not a consumer landing page.

Required components and fields:
1. Content summary, partner identity, public status, report/policy reason and expiry information.
2. Removal reason required; action choices hide immediately / end campaign / archive for audit.
3. Impact panel for active referral or public links; notification preview to partner.
4. Destructive confirm CTA, cancel, success state and preserved audit log.

Required interaction and state design: Do not delete historical evidence or modify referral outcomes retrospectively. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. Keep sensitive personal or health data to the minimum necessary; show consent scope, purpose, expiry, and access-denied/expired states where it applies. Preserve auditable lifecycle status for moderation, verification, payment, dispute, role, and partner actions. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete WEB screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep visible copy short and operational; do not add a long explanation or a visible technical screen title.
```

### Prompt 265 — `CB-265` — Confirm Safety Status (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-137

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-265`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Safety Confirmation”
Purpose: Asks the mother to confirm safety after the system detects a suspected event.
Functional coverage: UC-137

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Large countdown, event time and minimal location/context card; do not show raw sensor streams.
2. Three distinct actions: “Tôi ổn” · “Tôi cần hỗ trợ” · “Gọi cấp cứu”.
3. Clear consequence text for each action and auto-alert countdown state.
4. Accessibility: high contrast, large controls, vibration/sound indicator state and retry if network is unavailable.

Required interaction and state design: Do not diagnose fall or injury. Emergency-care disclaimer must be visible; preserve minimal event log. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 266 — `CB-266` — Send Family Emergency Alert (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-138

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-266`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Send Family Emergency Alert”
Purpose: Confirms and tracks delivery of a minimal emergency alert to authorized family members.
Functional coverage: UC-138

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Recipient list with relationship and permission eligibility; user can deselect only authorized recipients.
2. Minimal alert preview: time, general location/location precision scope, and safe status message.
3. Primary send CTA, cancel, delivery progress, partial failure/retry and sent timestamp.
4. Optional call emergency action remains visible but not automatic.

Required interaction and state design: Do not disclose exact location outside consent scope; no diagnosis or emergency dispatch claim. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 267 — `CB-267` — View Safety Event History (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-139

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-267`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Safety Event History”
Purpose: Shows the history of suspected fall or impact events, confirmation results, alert status, and false-positive labels.
Functional coverage: UC-139

Canvas and navigation: MOBILE ONLY. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination. No desktop browser chrome, sidebar, wide tables, or multi-column dashboard.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Timeline list with date/time, event type, confirmation outcome, alert delivery status and quick detail action.
2. Filters by period/outcome and a compact privacy/source explanation.
3. Empty state, no-data state and link to monitoring settings.
4. Each card has “Báo false positive” only when eligible.

Required interaction and state design: Do not show raw IMU data by default; only minimum useful event metadata. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 268 — `CB-268` — Report False Positive (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-140

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-268`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Report False Positive”
Purpose: Marks a safety event as a false positive and submits optional feedback.
Functional coverage: UC-140

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Event summary card with timestamp and prior confirmation outcome.
2. Reason chips: vận động mạnh · rơi điện thoại · đo sai · khác; optional note field.
3. Submit feedback CTA, cancel, loading and success state.
4. No promise that model/rules change immediately.

Required interaction and state design: Store only minimum necessary feedback; do not expose internal model scores. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 269 — `CB-269` — Voice Consultation Session (Expert App)

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-145

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-269`
Platform: Expert App
Primary role: Verified Expert
Feature: CareBridge operational feature
Reference screen name: “Voice Consultation Session”
Purpose: Joins an authenticated voice call within an eligible consultation.
Functional coverage: UC-145

Canvas and navigation: EXPERT APP — MOBILE ONLY. Use one 390 × 844 portrait phone UI. Use Expert App bottom navigation only for a primary destination: Trang chủ · Yêu cầu · Cộng đồng · Gần đây · Hồ sơ. Do not render browser chrome, a left sidebar, desktop tables, or a 1440 × 1024 layout.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Participant/consultation header, connection state, elapsed timer and safe consent-scope indicator.
2. Large call controls: mute, speaker, end call, reconnect; no unrelated browsing controls.
3. Network quality/retry state, privacy notice and end-call confirmation.
4. Link to authorized shared summary only when consent remains active.

Required interaction and state design: Expert App must be 390 × 844 mobile only; no desktop portal or sidebar. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Expert App only. Never show desktop sidebar, browser frame, wide table, or 1440 × 1024 layout.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 270 — `CB-270` — Contact Nearby Expert (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-153

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-270`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Contact Nearby Expert”
Purpose: Selects a contact method for a nearby expert based on availability and consent.
Functional coverage: UC-153

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Selected expert card with verified badge, specialty, distance/ETA, availability and supported channels.
2. Available actions chips: chat · gọi thoại · video · đặt tư vấn; unavailable options are disabled with reason.
3. Consent preview before sending a request, including what location/summary data is shared and expiry.
4. CTA tiếp tục, back and safe fallback to find facility/emergency support.

Required interaction and state design: Do not auto-share exact location or health record. Do not position this as emergency dispatch. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 271 — `CB-271` — Find Nearby Support (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-166

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-271`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Find Nearby Support”
Purpose: Finds care facilities or available experts around a consented location.
Functional coverage: UC-166

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Map/list toggle, location permission state and search radius.
2. Filter chips: Cơ sở chăm sóc · Chuyên gia · specialty · available now; map markers match list items.
3. Result card includes name, type, verified/source label, distance, ETA and CTA mở chi tiết.
4. Safe fallback for denied location: nhập khu vực; no map feature is hidden behind an unexplained error.

Required interaction and state design: Show location precision/expiry and emergency disclaimer when user is in safety flow. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 272 — `CB-272` — Edit My Community Answer (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** User  
**Functional coverage:** UC-200

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-272`
Platform: Mother Mobile App
Primary role: User
Feature: CareBridge operational feature
Reference screen name: “Edit My Community Answer”
Purpose: Lets a user edit a community answer that is not locked by moderation.
Functional coverage: UC-200

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Original question context and current answer content; lock/moderation status shown prominently.
2. Editable response field, personal-experience label and updated-at timestamp preview.
3. Save changes, discard/back and validation for empty/unsafe text.
4. If locked, editor becomes read-only with concise reason and no edit CTA.

Required interaction and state design: Never let a user change author identity, expert badge, moderation history or reaction counts. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 273 — `CB-273` — Complete Reminder (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-213

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-273`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Complete Reminder”
Purpose: Marks a reminder occurrence as complete and shows the next recurrence.
Functional coverage: UC-213

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Reminder summary with owner, due time, recurrence, current state and source.
2. Primary “Đánh dấu hoàn thành”; optional completion time/note only if needed.
3. Next occurrence preview; no accidental completion of the full series.
4. Success state with undo option when safe.

Required interaction and state design: If recurrence exists, clearly scope action to this occurrence unless user explicitly changes series. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 274 — `CB-274` — Skip Reminder (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-214

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-274`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Skip Reminder”
Purpose: Skips a reminder occurrence without deleting its recurrence configuration.
Functional coverage: UC-214

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Reminder summary and current occurrence time.
2. Skip reason optional, explicit scope “Chỉ lần này”, and next occurrence preview.
3. CTA “Bỏ qua lần này”, cancel, confirmation and undo/success state.
4. Separate destructive path for deleting the whole recurrence is not part of this UI.

Required interaction and state design: Do not silently change notification preferences or recurrence rule. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 275 — `CB-275` — Update Family Care Task (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-222

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-275`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Update Family Care Task”
Purpose: Edits an incomplete care group task.
Functional coverage: UC-222

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Task title, assignee, due date/time, reminder, category, note and task status.
2. Care-group context and visibility summary; owner can reassign only within group.
3. Save, cancel and field validation; blocked update when task already completed/locked.
4. Change summary for due-date or assignee edits.

Required interaction and state design: No disclosure of health records beyond task context; preserve activity history. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 276 — `CB-276` — Delete Vaccination Record (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-231

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-276`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Delete Vaccination Record”
Purpose: Soft-deletes a vaccination record entered by the mother.
Functional coverage: UC-231

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Vaccination record summary: baby, vaccine label, date, source, linked reminder and file status.
2. Required confirmation; explain soft-delete and how linked reminder behaves.
3. Destructive CTA, cancel, blocked state when retention/link rule prevents delete.
4. Success state with audit timestamp.

Required interaction and state design: Do not delete provider-issued evidence automatically; do not give medical guidance. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 277 — `CB-277` — Mark Vaccination as Completed (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-232

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-277`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Mark Vaccination as Completed”
Purpose: Marks a planned vaccination as completed and links or creates a vaccination record.
Functional coverage: UC-232

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Planned reminder summary and baby selector.
2. Completion date, location/provider optional, source field and optional evidence upload.
3. Choice to link existing record or create new record; no vaccine eligibility conclusion.
4. Complete CTA, validation and next-reminder update preview.

Required interaction and state design: Clearly label data as user-entered unless supported by an attached record. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```

### Prompt 278 — `CB-278` — Update Growth Measurement (Mobile)

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-235

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-278`
Platform: Mother Mobile App
Primary role: Mother
Feature: CareBridge operational feature
Reference screen name: “Update Growth Measurement”
Purpose: Edits a growth measurement entered by the mother.
Functional coverage: UC-235

Canvas and navigation: MOBILE ONLY. Use a compact contextual app bar with a clear back/close action. Do not render desktop browser chrome, a left sidebar, wide tables, or a 1440 × 1024 layout. Do not show bottom navigation on this form, detail, safety, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use one 390 × 844 portrait phone frame with a 24 px safe margin. Use the CareBridge Warm Claymorphism system: #F6F1EC canvas; floating white/#F2EAE4 cards; 24–32 px card radii; restrained warm shadows; #C98C7B primary actions; #5A463F primary text; #9C857C secondary text; friendly rounded sans-serif. Buttons, chips, badges, and toggles must be pill-shaped and at least 48 px high. Keep copy brief and action-first. Use Lucide-style icons, never emojis in headings. Do not show the technical screen name in the visible app UI.

Required components and fields:
1. Baby selector, measurement date, weight/height/head-circumference fields according to record type, units and source.
2. Previous value, notes and edit history summary.
3. Save, cancel, validation and out-of-range data-quality warning without diagnosis.
4. CTA xem biểu đồ tăng trưởng after save.

Required interaction and state design: Never make a clinical conclusion; show only measurement provenance and trend access. Include a visible primary action, clear back/close navigation, inline field-level validation where there is input, loading during submit/fetch, failure/retry state, and a concise success/empty/no-result state where relevant. Use icon + text for critical statuses; never rely on color alone.

Privacy, safety, and scope: show only role-authorized information. When health records, files, location, care-group context, or consultation data appears, visibly show owner/recipient context, consent scope, purpose, expiry, and an expired/denied safe state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated information when relevant. Never present AI output, trends, exercise feedback, or community content as a diagnosis, prescription, treatment, or emergency replacement. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete MOBILE screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Use natural Vietnamese labels and realistic but fictional placeholder data. Keep the visible UI short; do not add a long explanation, onboarding copy, or a visible technical screen title.
```
