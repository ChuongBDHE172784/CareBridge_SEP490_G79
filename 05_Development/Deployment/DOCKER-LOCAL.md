# CareBridge — chạy toàn hệ thống bằng Docker (localhost)

Tài liệu này mô tả stack Docker chạy trên `localhost`, không dùng Cloudflare
Tunnel. Bản có tunnel + edge Nginx nằm ở `docker-compose.yml` và
[EDGE_TOPOLOGY.md](EDGE_TOPOLOGY.md); chỉ dùng bản đó khi cần kiểm chứng đúng
đường đi production.

## 1. Thành phần hệ thống

| Thư mục | Vai trò | Runtime | Chạy trong Docker? |
| --- | --- | --- | --- |
| `05_Development/CareBridgeAPI` | Backend REST, Spring Boot 4.1.0 | Java 21 | ✅ `backend` |
| `05_Development/CareBridgeWebApp` | Portal React 19 + Vite 8 | Node 22 | ✅ `web` (dev server) |
| `05_Development/CareBridgeAITriageService` | AI Triage FastAPI + LangGraph | Python 3.12 | ✅ `ai-triage` |
| `05_Development/MachineLearning` | Sidecar sửa tư thế MediaPipe | Python | ⬜ tùy chọn, overlay riêng |
| `05_Development/CareBridgeMobileApp` | App Flutter | Dart/Flutter | ⬜ chạy ngoài Docker |
| CompreFace (ảnh Exadel) | Nhận diện khuôn mặt cho verify chuyên gia | — | ✅ stack riêng |

Database **không** nằm trong stack: backend nối thẳng Supabase PostgreSQL qua
pooler. Có sẵn một service `postgres` tùy chọn nếu muốn chạy DB nội bộ.

## 2. Yêu cầu môi trường

### Bắt buộc cài trên máy

| Công cụ | Phiên bản | Dùng cho |
| --- | --- | --- |
| Docker Desktop | 24+ (đang dùng: 29.6.1, Compose v5.3.0) | Toàn bộ stack |
| RAM cấp cho Docker | ≥ 4 GB; ≥ 8 GB nếu bật CompreFace | |
| Git | bất kỳ | |

### Chỉ cần khi chạy trực tiếp không qua Docker

| Công cụ | Phiên bản yêu cầu | Ghi chú |
| --- | --- | --- |
| JDK | **21** (Temurin) | `<java.version>21</java.version>` trong `pom.xml` |
| Maven | dùng `./mvnw` kèm repo | |
| Node.js | **≥ 22.12** | Vite 8 không chạy trên Node < 20.19 |
| Python | **3.10 – 3.13** | Code dùng cú pháp `X | None`; Python 3.14 chưa có wheel cho một số dependency — đây là lý do triage service được đóng gói ở image 3.12 |
| Flutter | Dart SDK `>=3.10.0 <4.0.0` → Flutter **3.38+** | Máy bạn: Flutter 3.44.0 ✅ |

Máy bạn hiện có: Java 21.0.10 ✅, Node v24.11.1 ✅, Flutter 3.44.0 ✅,
Python 3.14.3 ⚠️ (chỉ ảnh hưởng khi chạy triage service ngoài Docker).

## 3. File `.env` cần có

| File | Bắt buộc | Nội dung |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/.env` | ✅ | DB tuple, Supabase, Firebase, Cloudinary, R2, JWT, ZEGO, Gemini, mail |
| `05_Development/CareBridgeAITriageService/.env` | ⬜ (thiếu thì chạy chế độ deterministic) | `GEMINI_*`, `AI_TRIAGE_INTERNAL_API_KEY` |
| `05_Development/CareBridgeWebApp/.env` | ✅ | `VITE_FIREBASE_*` |

Tất cả đều bị `.gitignore` chặn. Tạo file triage từ file API bằng lệnh:

```powershell
$api = Get-Content 05_Development\CareBridgeAPI\.env
$api | Select-String '^(GEMINI_API_KEY|GEMINI_MODEL|GEMINI_ENABLED|AI_TRIAGE_INTERNAL_API_KEY)=' |
  Set-Content 05_Development\CareBridgeAITriageService\.env -Encoding utf8
```

`AI_TRIAGE_INTERNAL_API_KEY` phải **giống nhau** ở hai file, nếu không evidence
registry fail closed và mọi nguồn y khoa bị coi là chưa duyệt.

### Biến của DB

Backend nhận datasource theo **bộ ba nguyên tử**, đọc bởi
`RuntimeDatasourceEnvironmentPostProcessor`. Chọn một trong hai bộ:

- `CAREBRIDGE_DB_URL` + `CAREBRIDGE_DB_USERNAME` + `CAREBRIDGE_DB_PASSWORD` (ưu tiên)
- `SUPABASE_DB_URL` + `SUPABASE_DB_USERNAME` + `SUPABASE_DB_PASSWORD` (dự phòng)

Điền thiếu một biến trong bộ nào là app **dừng khởi động** — đó là thiết kế cố
ý, không phải lỗi.

⚠️ **Profile phải khớp với loại database:**

| DB | `SPRING_PROFILES_ACTIVE` | Hệ quả |
| --- | --- | --- |
| Supabase (dùng chung cả nhóm) | `supabase` (hoặc bất kỳ tên nào ≠ `local`) | `ddl-auto: validate` — Hibernate không sửa schema |
| PostgreSQL nội bộ | `local` | `ddl-auto: update` — Hibernate tự sửa schema |

Đặt `local` mà trỏ vào Supabase thì Hibernate sẽ `ALTER` các cột đang được view
tham chiếu, Postgres trả `cannot alter type of a column used by a view or rule`,
pooler đóng connection và backend **crash-loop**.

Cổng pooler: dùng **5432** (session pooler). Cổng 6543 là transaction pooler,
không giữ session state nên Flyway không lấy được advisory lock.

### Biến CompreFace bị bỏ qua

`application.yaml` ghi cứng `carebridge.compreface.enabled/base-url/api-key`,
nên `COMPREFACE_ENABLED` và `COMPREFACE_BASE_URL` trong `.env` **không có tác
dụng**. Muốn đổi phải dùng tên biến theo relaxed binding của Spring:

```dotenv
CAREBRIDGE_COMPREFACE_ENABLED=true
CAREBRIDGE_COMPREFACE_BASE_URL=http://host.docker.internal:8000
```

`docker-compose.dev.yml` đã set sẵn hai biến này (mặc định tắt CompreFace).

## 4. Chạy stack chính

```powershell
docker compose -f docker-compose.dev.yml up -d --build
docker compose -f docker-compose.dev.yml ps
```

| Service | URL trên máy | Ghi chú |
| --- | --- | --- |
| `web` | http://localhost:5173 | Vite dev server, hot reload |
| `backend` | http://localhost:8080 | `/actuator/health/readiness` để kiểm tra |
| `ai-triage` | http://localhost:8001/health | |

Lần build đầu mất 5–15 phút (Maven tải dependency). Xem log một service:

```powershell
docker compose -f docker-compose.dev.yml logs -f backend
```

Dừng: `docker compose -f docker-compose.dev.yml down`
(thêm `-v` nếu muốn xóa luôn `node_modules` trong volume).

### Biến điều chỉnh

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `CAREBRIDGE_BIND_ADDRESS` | `127.0.0.1` | Đặt `0.0.0.0` để máy khác trong LAN (điện thoại thật) truy cập được |
| `CAREBRIDGE_COMPREFACE_ENABLED` | `false` | Bật khi stack CompreFace đã chạy |
| `NODE_IMAGE` | `node:22.17.1-alpine3.22` | |

### DB nội bộ thay cho Supabase

```powershell
docker compose -f docker-compose.dev.yml --profile localdb up -d
```

rồi đổi trong `05_Development/CareBridgeAPI/.env`:

```dotenv
CAREBRIDGE_DB_URL=jdbc:postgresql://postgres:5432/carebridge
CAREBRIDGE_DB_USERNAME=carebridge
CAREBRIDGE_DB_PASSWORD=carebridge
```

(`postgres` là DNS trong network Docker; từ máy host là `localhost:5433`.)

## 5. Chạy CompreFace (tách rời)

Đây là stack độc lập: 5 container, ~3 GB image, ~2 GB RAM. Vì vậy nó nằm ở file
riêng và không nối chung network — backend gọi qua cổng host.

```powershell
docker compose --env-file 05_Development\CareBridgeAPI\.env `
  -f docker-compose.compreface.yml up -d
```

Lần đầu (chỉ làm một lần):

1. Mở http://localhost:8000, tạo tài khoản admin.
2. Tạo application, rồi tạo 2 service:
   - **Face detection** → lấy API key → `COMPREFACE_DETECTION_API_KEY`
   - **Face verification** → lấy API key → `COMPREFACE_VERIFICATION_API_KEY`
3. Ghi 2 key vào `05_Development/CareBridgeAPI/.env`, thêm:
   ```dotenv
   CAREBRIDGE_COMPREFACE_ENABLED=true
   CAREBRIDGE_COMPREFACE_BASE_URL=http://host.docker.internal:8000
   CAREBRIDGE_COMPREFACE_DETECTION_API_KEY=<detection key>
   CAREBRIDGE_COMPREFACE_VERIFICATION_API_KEY=<verification key>
   ```
4. `docker compose -f docker-compose.dev.yml up -d --force-recreate backend`

Dừng: `docker compose -f docker-compose.compreface.yml down`
(thêm `-v` để xóa DB khuôn mặt).

> `05_Development/Deployment/docker-compose.compreFace.yml` là file khác: overlay
> chồng lên stack staging. Không chạy đồng thời hai file, cả hai cùng chiếm cổng 8000.

## 6. Mobile app

Flutter chạy ngoài Docker. `api_client.dart` tự chọn base URL:

- Android emulator → `http://10.0.2.2:8080` (khớp sẵn với binding `127.0.0.1`)
- iOS simulator → `http://localhost:8080`
- Máy thật: chạy stack với `CAREBRIDGE_BIND_ADDRESS=0.0.0.0` rồi
  `flutter run --dart-define=API_BASE_URL=http://<IP-LAN>:8080`

## 7. Xử lý sự cố

| Triệu chứng | Nguyên nhân |
| --- | --- |
| Backend dừng ngay với `DATASOURCE_*_CONFIGURATION_INCOMPLETE` | Bộ ba DB thiếu biến hoặc có giá trị rỗng |
| Backend crash-loop, log có `cannot alter type of a column used by a view or rule` | `SPRING_PROFILES_ACTIVE=local` nhưng DB là Supabase — đổi sang `supabase` |
| Backend log lỗi gọi `localhost:8000`/`localhost:8001` | Đang chạy image cũ; container không có sidecar ở `localhost` |
| Web sửa code mà không reload | Watcher bỏ sót event qua bind mount Windows — thêm `server: { watch: { usePolling: true } }` vào `vite.config.ts`. Biến `CHOKIDAR_USEPOLLING` **không** được Vite đọc |
| `npm ci` chạy lại mỗi lần khởi động | Bình thường; volume `web_node_modules` giữ cache nên lần sau nhanh |
| CompreFace `compreface-api` restart liên tục | Docker Desktop thiếu RAM, cần ≥ 8 GB |
| Port 8080/5173/8000 bị chiếm | Tắt tiến trình chạy trực tiếp (`mvnw spring-boot:run`, `npm run dev`) trước |
