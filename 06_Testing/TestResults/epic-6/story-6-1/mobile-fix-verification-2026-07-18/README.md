# MF-01 Story 6.1 — Mobile fix verification

Ngày thực thi: 2026-07-18  
Thiết bị: Samsung SM-N986N, Android 13  
APK: debug, `API_BASE_URL=http://127.0.0.1:8080`

## Bằng chứng

| File | Kết quả xác minh |
| --- | --- |
| `01-empty-routing-fixed.png` / `.xml` | Mother đã có role nhưng chưa có canonical journey được chuyển tới màn hình chọn giai đoạn (`MF01-MOB-001`). |
| `02-preg-history-fixed.png` / `.xml` | Tab Hành trình hiển thị transition history theo thứ tự mới nhất trước, provenance và reason label nghiệp vụ (`MF01-MOB-011`). |
| `03-back-accessibility-fixed.png` / `.xml` | Nút quay lại trong Journey Setup có nhãn accessibility `Quay lại` (`MF01-MOB-015`). |

## Automated gates

- Targeted Story 6.1 mobile gap tests: 10/10 pass.
- Full Flutter suite: 182/182 pass.
- Targeted Flutter analysis: no issues.
- Debug APK build and install: pass.

Đây là fix verification, không thay thế việc rerun đầy đủ 16 ca manual trong guide.
