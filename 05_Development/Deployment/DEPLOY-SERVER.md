# CareBridge — deploy lên server (AWS EC2 / VM)

Tài liệu này triển khai đúng Runtime Architecture đã thiết kế: frontend và API
đi qua Cloudflare, cloudflared quay ra ngoài từ EC2 nên **không mở cổng inbound
nào**, CompreFace là Container B nằm trong private network.

```
Internet ─▶ Cloudflare ─▶ cloudflared ─▶ nginx-edge ─┬─▶ web:8080      (SPA assets)
                                                     └─▶ backend:8080  (/api)

backend ─▶ ai-triage:8001            FastAPI + LangGraph, AI triage & RAG
backend ─▶ exercise-correction:8002  MediaPipe posture analysis
backend ─▶ compreface-fe:80          Face enrollment & verification (Container B)
backend ─▶ Supabase / Gemini / Firebase / Cloudinary / R2 / SMTP
```

## 1. Hai file compose

| File | Nội dung |
| --- | --- |
| `docker-compose.server.yml` | cloudflared, nginx-edge, backend, ai-triage, exercise-correction, web |
| `docker-compose.server.compreface.yml` | Container B: CompreFace + PostgreSQL riêng |

Kèm theo: `05_Development/Deployment/nginx/edge-server.conf` (nginx-edge cần để
định tuyến 2 hostname) và `05_Development/Deployment/server.env.example` (mẫu
biến môi trường).

### Bản standalone để gửi tay

Hai file trên cố tình để trống giá trị sau `${...}` và đọc secret từ
`.env.server`, vì chúng được commit. Khi cần gửi file chạy ngay mà không kèm
`.env.server` và file token, render bản nhét sẵn giá trị:

```powershell
python 05_Development/Deployment/render-standalone-compose.py
```

Sinh ra `carebridge-server.standalone.yml` và `carebridge-compreface.standalone.yml`
ở thư mục gốc, mỗi file tự chạy độc lập:

```bash
docker compose -f carebridge-server.standalone.yml up -d --build
```

Hai file này chứa mật khẩu DB, JWT private key và Firebase service-account key ở
dạng chữ thường — `.gitignore` đã chặn qua pattern `*.standalone.yml`. Chuyển
bằng `scp` rồi `chmod 600`, đừng gửi qua chat hay Google Doc. Sửa template hoặc
`.env.server` thì render lại, đừng sửa tay file standalone.

Không service nào publish cổng ra ngoài. Cổng duy nhất được mở là CompreFace
admin, bind vào `127.0.0.1` của VM để vào bằng SSH tunnel.

## 2. Chuẩn bị Cloudflare (làm một lần)

1. Dashboard → **Networking → Tunnels → Create a tunnel**, chọn **Docker**.
2. Copy phần token `eyJ...` phía sau `--token`. **Không** chạy lệnh Docker mà
   Cloudflare hiển thị — file compose sẽ chạy connector.
3. Trong tunnel vừa tạo, thêm 2 **Public hostname**, cả hai trỏ về cùng service:

   | Hostname | Service |
   | --- | --- |
   | `carebridgevn.site` | `HTTP` → `nginx-edge:8080` |
   | `api.carebridgevn.site` | `HTTP` → `nginx-edge:8080` |

   nginx-edge phân biệt hai hostname bằng `server_name`, nên cùng một origin.

4. Nếu vẫn giữ frontend trên GitLab Pages theo sơ đồ CI/CD: **bỏ** hostname
   `carebridgevn.site` ở bước 3 và để DNS trỏ về Pages như cũ. Service `web`
   trong compose vẫn chạy nhưng không ai vào được — hoặc xóa hẳn service đó.

## 3. Chuẩn bị server

Yêu cầu tối thiểu: **4 GB RAM** nếu build ngay trên VM (Maven cần ~2 GB),
**2 GB** nếu chỉ pull image. Cộng thêm **2 GB** nếu bật CompreFace.
Security group **không cần** mở port nào ngoài SSH.

```bash
# Docker Engine + Compose plugin (Ubuntu)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER" && newgrp docker

git clone <repo-url> carebridge && cd carebridge

# Token tunnel: chỉ chứa chuỗi eyJ..., không xuống dòng, không dấu nháy
sudo install -d -m 700 /run/carebridge/secrets
sudo install -m 600 /dev/null /run/carebridge/secrets/cloudflare-tunnel-token
sudo nano /run/carebridge/secrets/cloudflare-tunnel-token

cp 05_Development/Deployment/server.env.example 05_Development/Deployment/.env.server
chmod 600 05_Development/Deployment/.env.server
nano 05_Development/Deployment/.env.server
```

> `/run` bị xóa khi reboot. Nếu muốn token sống qua reboot, đặt ở
> `/etc/carebridge/secrets/` rồi sửa `CLOUDFLARE_TUNNEL_TOKEN_FILE`.

Điền `.env.server` theo chú thích trong file. Ba chỗ hay sai nhất:

- `SPRING_PROFILES_ACTIVE` **không được** là `local` — profile `local` bật
  `ddl-auto: update`, Hibernate sẽ `ALTER` schema Supabase dùng chung rồi
  crash-loop.
- DB dùng **session pooler cổng 5432**, không phải transaction pooler 6543.
- `AI_TRIAGE_INTERNAL_API_KEY` phải giống nhau ở cả backend và ai-triage.

## 4. Deploy

**Cách A — build ngay trên server** (không cần registry, lần đầu 10–15 phút):

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml up -d --build
```

**Cách B — pull image từ GitLab Container Registry** (theo sơ đồ CI/CD):
bỏ comment `BACKEND_IMAGE` / `WEB_IMAGE` / `AI_IMAGE` trong `.env.server`, trỏ
tới digest `@sha256:...`, rồi:

```bash
docker login registry.gitlab.com
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml pull
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml up -d
```

Dùng digest chứ đừng dùng tag `latest`: có script
[validate-production-images.sh](validate-production-images.sh) để CI chặn tag
mutable.

Kiểm tra:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml ps
```

Mong đợi `backend`, `web`, `ai-triage`, `nginx-edge` đều `healthy` và
`cloudflared` đang chạy. Chưa healthy thì đừng đụng vào Cloudflare, đọc log
service đó trước:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml logs --tail=100 backend
```

Vì không có cổng nào publish, muốn test từ trong VM thì gọi qua network Docker:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml exec nginx-edge \
  wget -qO- http://backend:8080/actuator/health/readiness
```

## 5. Cập nhật phiên bản

```bash
git pull
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml up -d --build
docker image prune -f
```

Đổi hostname hoặc Firebase project thì **phải build lại `web`** — Vite nhúng
cứng các giá trị `VITE_*` vào bundle lúc build, không đọc lúc chạy.

## 6. CompreFace (Container B)

Chạy sau khi stack chính đã lên, vì nó dùng network `carebridge-face-origin` do
stack chính tạo.

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.compreface.yml up -d
```

Setup lần đầu — mở UI qua SSH tunnel, **không** mở cổng ra internet:

```bash
# chạy trên máy của bạn
ssh -L 8000:127.0.0.1:8000 <user>@<vm-ip>
# rồi mở http://localhost:8000 trên trình duyệt
```

1. Tạo tài khoản admin, tạo một application.
2. Tạo service **Face detection** và **Face verification**, copy 2 API key.
3. Điền vào `.env.server`:
   ```dotenv
   CAREBRIDGE_COMPREFACE_ENABLED=true
   CAREBRIDGE_COMPREFACE_BASE_URL=http://compreface-fe
   CAREBRIDGE_COMPREFACE_DETECTION_API_KEY=<detection key>
   CAREBRIDGE_COMPREFACE_VERIFICATION_API_KEY=<verification key>
   ```
4. Nạp lại backend:
   ```bash
   docker compose --env-file 05_Development/Deployment/.env.server \
     -f docker-compose.server.yml up -d --force-recreate backend
   ```

Tắt: `docker compose -f docker-compose.server.compreface.yml down`
(thêm `-v` để xóa cả DB khuôn mặt).

## 7. Vì sao chia mạng như vậy

| Network | `internal` | Ai ở trong | Lý do |
| --- | --- | --- | --- |
| `tunnel` | không | cloudflared, nginx-edge | cloudflared cần ra Internet để dựng tunnel |
| `portal-origin` | có | nginx-edge, web | web chỉ phục vụ file tĩnh, không cần ra ngoài |
| `backend-origin` | có | nginx-edge, backend | tunnel chỉ chạm được nginx-edge |
| `ai-origin` | có | backend, ai-triage | ai-triage không nhận request từ ngoài |
| `exercise-origin` | có | backend, exercise-correction | sidecar chỉ suy luận trên model đóng sẵn, không cần mạng |
| `carebridge-face-origin` | có | backend, compreface-fe | CompreFace không public |
| `service-egress` | không | backend, ai-triage | chỉ 2 service này được gọi Supabase/Gemini/Firebase/R2 |

`nginx-edge` chỉ nhận request từ IP tĩnh của cloudflared (`allow` + `deny all`),
chặn `/actuator/`, xóa `CF-Connecting-IP` do client gửi trước khi tự set lại, và
trả `444` cho mọi hostname lạ — nên gõ thẳng IP của EC2 sẽ không ra gì.

IP tĩnh: cloudflared `.2`, nginx-edge `.3`. Phải pin cả hai — nginx-edge được
tạo trước, mà IPAM cấp động bắt đầu từ `.2`, để mặc định sẽ đụng IP tĩnh của
cloudflared và cloudflared không khởi động được.

## 8. Xử lý sự cố

| Triệu chứng | Nguyên nhân |
| --- | --- |
| `Set CAREBRIDGE_PORTAL_HOSTNAME` khi `up` | Thiếu `--env-file`, hoặc biến còn trống trong `.env.server` |
| `env file ... .env.server not found` | Chưa copy từ `server.env.example` |
| Backend crash-loop, log có `cannot alter type of a column used by a view or rule` | `SPRING_PROFILES_ACTIVE=local` — đổi sang `supabase` |
| Backend dừng với `DATASOURCE_*_CONFIGURATION_INCOMPLETE` | Bộ ba `SUPABASE_DB_*` thiếu biến hoặc để trống |
| cloudflared báo `Address already in use` | `CAREBRIDGE_NGINX_EDGE_IP` và `CAREBRIDGE_CLOUDFLARED_IP` trùng nhau |
| Cloudflare báo 502 | nginx-edge chưa healthy, hoặc public hostname trỏ sai `nginx-edge:8080` |
| Portal mở được nhưng gọi API bị CORS | `CAREBRIDGE_PORTAL_HOSTNAME` khác origin thật của trình duyệt |
| Portal trắng trang, console lỗi Firebase | Build `web` thiếu build arg `VITE_FIREBASE_*` — sửa `.env.server` rồi build lại |
| `network carebridge-face-origin declared as external, but could not be found` | Chưa `up` stack chính trước khi chạy CompreFace |
| Build backend bị OOM trên VM | VM dưới 4 GB RAM — thêm swap hoặc chuyển sang Cách B (pull image) |
