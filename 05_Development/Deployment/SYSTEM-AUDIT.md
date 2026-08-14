# CareBridge — Audit hệ thống & kế hoạch deploy AWS

Rà soát ngày **2026-08-08** trên nhánh `LamVH1` (commit `590f50d4`). Tài liệu này
mô tả hệ thống *đang thực sự chạy*, không phải hệ thống theo thiết kế — hai thứ
đó hiện đang lệch nhau và phần lớn tài liệu này nói về chỗ lệch.

Cách rà soát: quét tĩnh toàn bộ compose/Dockerfile/pom/requirements/env,
[code-review-graph](#phụ-lục-a--code-review-graph) cho cấu trúc code, và
[Edge DevTools](#phụ-lục-b--edge-devtools) bắn thẳng vào site production để đối
chiếu với thực tế.

---

## 1. Trạng thái thực tế, đo ngày 2026-08-08

| Hạng mục | Đo được | Kết luận |
| --- | --- | --- |
| `carebridgevn.site` | `A 35.185.44.232`, HTTP 200, redirect `/login` | GitLab Pages **đang chạy** |
| `api.carebridgevn.site` | `NXDOMAIN` từ 8.8.8.8 | **Chưa tồn tại** |
| Nameserver | `desiree.ns.cloudflare.com`, `zahir.ns.cloudflare.com` | Cloudflare đang quản DNS |
| Bundle `index-Ev3ShwTQ.js` (2096 KB) | chứa `http://localhost:8080` ×2 | Portal **không gọi được API** |
| Bundle — chuỗi `api.carebridgevn.site` | không có | xác nhận điều trên |
| Bundle — Firebase key `AIza…` | không có | chat / call / push **không hoạt động** |
| Console log của trang | rỗng | lỗi chỉ xuất hiện khi bấm đăng nhập |

Nói gọn: **frontend đã lên, backend chưa từng được expose.** Portal hiện là một
vỏ tĩnh, mọi request đều trỏ về `localhost` của chính máy người dùng.

Nguyên nhân gốc nằm ở job `pages` trong `.gitlab-ci.yml`: nó build mà không
truyền `VITE_API_URL`, nên `apiClient.ts` rơi vào nhánh fallback

```ts
baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080'
```

Đã sửa trong commit hiện tại (thêm biến + guard). Cần chạy lại pipeline thì
bundle mới có hiệu lực.

---

## 2. Kiến trúc mục tiêu

```
Browser
  ├─ https://carebridgevn.site ─────▶ Cloudflare ─▶ GitLab Pages  (React SPA tĩnh)
  │
  └─ https://api.carebridgevn.site ─▶ Cloudflare ─▶ cloudflared ─▶ nginx-edge
                                                                        │
                                                                        ▼
                                                                  backend:8080
                                            ┌───────────────────────────┼───────────────┐
                                            ▼                           ▼               ▼
                                     ai-triage:8001        exercise-correction:8002   compreface-fe:80
                                            │                                          (Container B)
                                            ▼
                            Supabase · Gemini · Firebase · Cloudinary · R2 · SMTP
```

Hai điểm dễ hiểu sai:

- **GitLab Pages không nói chuyện với Nginx.** Pages chỉ trả HTML/CSS/JS. Sau đó
  React chạy trong browser của người dùng và tự gọi `api.carebridgevn.site`.
  Hai đường hoàn toàn tách rời, gặp nhau ở trình duyệt chứ không ở server.
- **EC2 không mở cổng inbound nào.** `cloudflared` quay ra ngoài (outbound
  QUIC/7844) rồi Cloudflare đẩy request ngược vào. Security group chỉ cần SSH.

### Lệch với `03_Design/Architecture/System Architecture.drawio`

Sơ đồ thiết kế hiện **không có node GitLab Pages**. Nó vẽ:

```
Users/Client Layer ──[HTTPS 443  SPA Assets + /api RESTful API]──▶ Cloudflare
cloudflared ──[HTTP 8080  http://nginx-edge:8080]──▶ nginx-edge
GitLab CI/CD ─▶ Security Scan ─▶ Container Registry ─[Deployment]─▶ AWS
```

tức là SPA và API **cùng đi qua một origin trên AWS**. Đó là kiến trúc cũ. Sơ đồ
cần cập nhật: tách nhánh Pages ra khỏi Cloudflare, và bỏ frontend khỏi khối AWS.

Sơ đồ còn lệch port so với code:

| Thành phần trong sơ đồ | Sơ đồ ghi | Code thực tế |
| --- | --- | --- |
| AI Orchestration & RAG | `8003` | `ai-triage:8001` |
| Posture Analysis | `8001` | `exercise-correction:8002` |
| CompreFace | `8000` | `compreface-fe:80` (8000 là admin, bind loopback) |
| Private Tool API, RBAC + Consent + Audit | `8081` | **chưa tồn tại** |
| AI ─▶ DB qua `ai_rag_readonly` / pgVector | `TCP 5432` | **chưa tồn tại** — `ai-triage` gọi `http://backend:8080`, và không nằm trên network nào thấy được DB |

Hai dòng cuối là tính năng thiết kế nhưng chưa code. Nếu sơ đồ dùng để nộp báo
cáo thì nên đánh dấu rõ là *planned*, tránh bị hỏi "chỗ này chạy chưa".

---

## 3. Danh mục service — runtime, phiên bản, cổng

| Service | Runtime | Framework chính | Cổng | Nguồn image |
| --- | --- | --- | --- | --- |
| `backend` | Java **21** (`eclipse-temurin:21-jre-jammy`) | Spring Boot **4.1.0** | 8080 | build tại chỗ |
| `ai-triage` | Python **3.12-slim** | FastAPI + uvicorn, `google-genai` | 8001 | build tại chỗ |
| `exercise-correction` | Python **3.10.16-slim-bookworm** (ghim theo digest) | FastAPI 0.115.6, scikit-learn 1.1.2, numpy 1.23.3, pandas 1.4.3, scipy 1.9.1 | 8002 | build tại chỗ |
| `nginx-edge` | — | `nginx-unprivileged:1.29-alpine` | 8080 | pull |
| `cloudflared` | — | `cloudflare/cloudflared:2026.7.2` | — (outbound) | pull |
| CompreFace ×5 | — | `exadel/compreface-*:1.2.0` | 80 nội bộ, 8000 admin (loopback) | pull |
| Portal | Node 22 lúc build | React 19.2.6, TypeScript 6.0.2, Vite 8.0.12 | — | **GitLab Pages** |

### Vì sao hai service Python lệch phiên bản

Không phải do quên nâng cấp. `exercise-correction` ghim numpy 1.23.3 /
scipy 1.9.1 / scikit-learn 1.1.2, và Dockerfile cài bằng `--only-binary=:all:`
(chỉ chấp nhận wheel dựng sẵn). Bộ này **không có wheel cho Python 3.12+**, nên
runtime không thể đi theo `ai-triage`.

Quan trọng hơn: `models/*.pkl` là pickle của scikit-learn (`bicep_curl_model.pkl`,
`lunge_err_model.pkl`, `plank_model.pkl`…). Unpickle bằng scikit-learn khác
phiên bản có thể lỗi hoặc — tệ hơn — chạy được nhưng cho kết quả sai. Có
`models/SHA256SUMS` để kiểm tra toàn vẹn.

**Kết luận: đừng nâng Python hay scikit-learn ở service này nếu chưa train lại
model và cập nhật SHA256SUMS.** Phiên bản cũ ở đây là chủ ý, không phải nợ kỹ thuật.

---

## 4. Biến môi trường

`05_Development/Deployment/.env.server` có **69 biến**, khớp đúng 69 biến trong
`server.env.example` — không thiếu, không thừa. Chỉ một biến để trống:

| Biến | Trạng thái | Ảnh hưởng |
| --- | --- | --- |
| `SUPABASE_SERVICE_ROLE_KEY` | rỗng | **Không sao.** `application.yaml:155` khai báo `${SUPABASE_SERVICE_ROLE_KEY:}` (mặc định rỗng) và không có code Java nào đọc nó. |

### Nhóm biến và mức độ bắt buộc

| Nhóm | Biến tiêu biểu | Thiếu thì sao |
| --- | --- | --- |
| Hostname | `CAREBRIDGE_PORTAL_HOSTNAME`, `CAREBRIDGE_API_HOSTNAME` | Compose **từ chối khởi động** (`:?`) |
| Tunnel | `CLOUDFLARE_TUNNEL_TOKEN_FILE` | Compose từ chối khởi động |
| Spring profile | `SPRING_PROFILES_ACTIVE=supabase` | Để `local` → Hibernate `ddl-auto: update` **ALTER schema Supabase dùng chung** rồi crash-loop |
| Database | `SUPABASE_DB_URL/USERNAME/PASSWORD` | Bộ ba nguyên tử. Thiếu một → `DATASOURCE_CONFIGURATION_INCOMPLETE` |
| JWT | `JWT_ACTIVE_KEY_ID`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEYS` | Không đăng nhập được |
| Firebase | `FIREBASE_CREDENTIALS_BASE64` | Mất chat, call signaling, push |
| Worker nền | `REMINDER_SCHEDULE_NOTIFICATION_ENABLED`, `APPOINTMENT_NOTIFICATION_ENABLED`, `CAREBRIDGE_RECOMMENDATION_ENABLED` | Mặc định `false` trong `application.yaml` → deploy im lặng **không bao giờ gửi nhắc lịch**. Phải set `true` tường minh. |
| AI triage | `AI_TRIAGE_INTERNAL_API_KEY` | Phải **giống hệt** ở backend và ai-triage, lệch → evidence registry fail-closed, không nguồn y khoa nào được duyệt |
| CompreFace | `CAREBRIDGE_COMPREFACE_*` | Chỉ tên có tiền tố `CAREBRIDGE_` mới override được literal trong `application.yaml`. Đặt `COMPREFACE_*` trần **không có tác dụng**. |

### Biến cần thêm trên GitLab (chưa có)

Job `pages` build trên runner, không có `.env`. Phải khai ở
**Settings → CI/CD → Variables**:

```
VITE_FIREBASE_API_KEY
VITE_FIREBASE_AUTH_DOMAIN
VITE_FIREBASE_PROJECT_ID
VITE_FIREBASE_APP_ID
VITE_FIREBASE_STORAGE_BUCKET
VITE_FIREBASE_MESSAGING_SENDER_ID
```

Đây là public client config chứ không phải secret — chúng nằm sẵn trong mọi
bundle web đã deploy. Nhưng repo có quy ước không hardcode, nên để ở CI variable.
`VITE_API_URL` đã hardcode trong job vì nó là hằng số hạ tầng, không phải bí mật.

---

## 5. Hai Container

Chia đôi vì CompreFace nặng (~2 GB RAM, ~3 GB image, 5 container, PostgreSQL
riêng) và không phải lúc nào cũng cần.

### Container A — `docker-compose.server.yml`

`ai-triage` · `exercise-correction` · `backend` · `nginx-edge` · `cloudflared`

Đã **bỏ service `web`** so với bản trước, vì portal do GitLab Pages phục vụ. Giữ
lại nó nghĩa là nuôi một bản copy thứ hai của frontend, luôn cũ hơn Pages, tốn
RAM và gây nhầm lẫn khi debug. `CAREBRIDGE_PORTAL_HOSTNAME` vẫn cần — nó là
origin trong danh sách CORS, không phải hostname stack này phục vụ.

Xác nhận **không service nào publish cổng ra host**.

### Container B — `docker-compose.server.compreface.yml`

`compreface-postgres-db` · `compreface-core` · `compreface-api` ·
`compreface-admin` · `compreface-fe`

Bật / tắt độc lập:

```bash
# bật  (Container A phải chạy trước — nó tạo network carebridge-face-origin)
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.compreface.yml up -d

# tắt, giữ dữ liệu khuôn mặt
docker compose -f docker-compose.server.compreface.yml down

# tắt và xoá sạch DB khuôn mặt
docker compose -f docker-compose.server.compreface.yml down -v
```

Tắt Container B thì đặt `CAREBRIDGE_COMPREFACE_ENABLED=false` rồi
`up -d --force-recreate backend`, nếu không backend sẽ gọi vào hư không.

`compreface-fe` **không publish được cổng nào**, dù file compose từng khai báo
`127.0.0.1:8000:80`. Cả hai network của nó đều `internal: true`, và Docker không
publish cổng host cho container không có network routable — binding được ghi vào
`HostConfig.PortBindings` nhưng không có gì listen, không lỗi ở log nào. Vào UI
admin bằng cách tunnel thẳng tới IP container:

```bash
# trên EC2
docker inspect carebridge-compreface-compreface-fe-1 \
  --format '{{(index .NetworkSettings.Networks "carebridge-face-origin").IPAddress}}'
# trên máy bạn, mở bằng cửa sổ ẩn danh
ssh -L 8000:<IP>:80 <user>@<ec2-ip>
```

Nghĩa là toàn hệ thống chỉ mở đúng **một** cổng ra ngoài: `443` của `nginx-edge`.

---

## 6. Vì sao chia network như vậy

| Network | `internal` | Thành viên | Lý do |
| --- | --- | --- | --- |
| `tunnel` | không | cloudflared, nginx-edge | cloudflared cần ra Internet dựng tunnel |
| `backend-origin` | **có** | nginx-edge, backend | tunnel chỉ chạm được nginx-edge |
| `ai-origin` | **có** | backend, ai-triage | ai-triage không nhận request từ ngoài |
| `exercise-origin` | **có** | backend, exercise-correction | sidecar chỉ suy luận trên model đóng sẵn, không cần mạng |
| `carebridge-face-origin` | **có** | backend, compreface-fe | cầu nối A ↔ B, CompreFace không public |
| `compreface` | **có** | 5 container CompreFace | model nằm sẵn trong image |
| `service-egress` | không | backend, ai-triage | **chỉ hai service này** gọi được Supabase / Gemini / Firebase / R2 |

`exercise-correction` còn bị siết thêm: `read_only: true`, `cap_drop: ALL`,
`no-new-privileges`, `pids_limit: 128`, giới hạn RAM/CPU. Nó xử lý dữ liệu người
dùng gửi lên nên bị đối xử như code không đáng tin.

IP tĩnh trên `tunnel`: cloudflared `.2`, nginx-edge `.3`. **Phải ghim cả hai** —
nginx-edge được tạo trước, IPAM cấp động bắt đầu từ `.2`, để mặc định sẽ đụng IP
của cloudflared và cloudflared không khởi động được.

`nginx-edge` chỉ nhận request từ IP của cloudflared (`allow` + `deny all`), chặn
`/actuator/`, xoá `CF-Connecting-IP` client gửi lên trước khi tự set lại, và trả
`444` cho mọi hostname lạ.

---

## 7. Việc cần làm, theo thứ tự

Đánh số theo mức chặn. Làm từ trên xuống.

### 7.1 — Đổi mật khẩu AWS và Cloudflare  ⚠️ làm ngay

Mật khẩu tài khoản AWS đã bị dán vào một cuộc hội thoại chat. Coi như đã lộ.

1. Đổi mật khẩu AWS, bật MFA cho root.
2. Tạo IAM user riêng cho việc hàng ngày, **ngừng dùng root**.
3. Kiểm tra CloudTrail xem có đăng nhập lạ không.
4. Làm tương tự với Cloudflare, và xoay `CLOUDFLARE_TUNNEL_TOKEN` nếu nghi ngờ.

### 7.2 — Xoay toàn bộ credential trong `.env.server`  ⚠️

Hai file `carebridge-*.standalone.yml` từng được commit ở `b32a6a9d` (tác giả
`ntphuong9824`, 2026-08-05) và **hiện vẫn nằm trên `gitlab/dev` và `github/dev`**
— đã kiểm chứng bằng `git ls-remote` + `git cat-file` ngày 2026-08-08.

Chúng chứa giá trị thật đã render sẵn: mật khẩu Supabase, JWT private key,
Firebase service-account key, Cloudinary secret, R2 key, mật khẩu SMTP, Zego
secret, Gemini key.

`.gitignore` giờ đã chặn `*.standalone.yml`, nhưng **history cũ không tự sạch**.
Cần thống nhất với cả team:

- Xoay tất cả credential nêu trên (bắt buộc, không có đường tắt).
- Cân nhắc `git filter-repo` để xoá khỏi history + force-push. Việc này viết lại
  lịch sử nhánh `dev`, cả team phải re-clone — phải báo trước.

### 7.3 — Tạo DNS record cho `api.carebridgevn.site`  🔴 đang chặn

Hiện là `NXDOMAIN`. Trong Cloudflare → tunnel đang dùng → **Public hostname**:

| Hostname | Service |
| --- | --- |
| `api.carebridgevn.site` | `HTTP` → `nginx-edge:8080` |

Cloudflare tự tạo CNAME proxied trỏ về `<tunnel-id>.cfargotunnel.com`.

**Không thêm `carebridgevn.site` vào tunnel.** Hostname đó phải giữ nguyên
`A 35.185.44.232` trỏ về Pages. Thêm vào tunnel là cướp mất traffic của Pages,
và nginx-edge sẽ trả `444` vì không còn server block cho nó.

### 7.4 — Deploy Container A lên EC2  🔴 đang chặn

Yêu cầu: **4 GB RAM** nếu build tại chỗ (Maven ~2 GB), 2 GB nếu chỉ pull image.
Cộng 2 GB nếu bật CompreFace. Security group **không mở cổng nào ngoài SSH**.

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER" && newgrp docker

git clone <repo-url> carebridge && cd carebridge

sudo install -d -m 700 /run/carebridge/secrets
sudo install -m 600 /dev/null /run/carebridge/secrets/cloudflare-tunnel-token
sudo nano /run/carebridge/secrets/cloudflare-tunnel-token   # chỉ chuỗi eyJ..., không xuống dòng

cp 05_Development/Deployment/server.env.example 05_Development/Deployment/.env.server
chmod 600 05_Development/Deployment/.env.server
nano 05_Development/Deployment/.env.server                  # điền credential ĐÃ XOAY ở 7.2

docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml up -d --build
```

> `/run` bị xoá khi reboot. Muốn token sống qua reboot thì để ở
> `/etc/carebridge/secrets/` và sửa `CLOUDFLARE_TUNNEL_TOKEN_FILE`.

Kiểm tra — không có cổng nào publish nên phải gọi từ trong network Docker:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml ps

docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml exec nginx-edge \
  wget -qO- http://backend:8080/actuator/health/readiness
```

Mong đợi `backend`, `ai-triage`, `exercise-correction`, `nginx-edge` đều
`healthy`, `cloudflared` đang chạy.

### 7.5 — Chạy lại pipeline để build lại bundle Pages  🔴 đang chặn

`VITE_API_URL` đã được thêm vào job `pages`. Thêm nốt 6 biến `VITE_FIREBASE_*`
ở mục 4, rồi chạy lại pipeline. Kiểm chứng bằng cách tải bundle mới về và tìm
chuỗi — xem [Phụ lục B](#phụ-lục-b--edge-devtools).

### 7.6 — Thu hẹp job `pages` về một nhánh

Job đang chạy trên 7 nhánh (`HuyND`, `ChuongBD`, `PhuongNT`, `LamVH1`, `BachNQ1`,
`dev`, `main`) nhưng GitLab Pages chỉ có **một** site cho cả project. Nhánh nào
pipeline xong sau cùng sẽ đè lên. Nghĩa là `carebridgevn.site` hiện phục vụ bản
build của bất kỳ ai push cuối cùng. Nên giới hạn về `dev` hoặc `main`.

### 7.7 — Cập nhật sơ đồ kiến trúc

Theo mục 2. Việc tài liệu, không chặn deploy, nhưng cần xong trước khi nộp.

### 7.8 — Bật CompreFace (tuỳ chọn, làm sau cùng)

Xem `DEPLOY-SERVER.md` mục 6.

---

## 8. Bảng tra lỗi

| Triệu chứng | Nguyên nhân |
| --- | --- |
| Portal mở được, đăng nhập không phản hồi, console báo `localhost:8080` | Bundle Pages build thiếu `VITE_API_URL` — chạy lại pipeline |
| Portal trắng trang, console lỗi Firebase | Thiếu `VITE_FIREBASE_*` trong CI/CD Variables |
| Gọi API bị chặn CORS | `CAREBRIDGE_PORTAL_HOSTNAME` khác origin thật của trình duyệt |
| `api.carebridgevn.site` không phân giải | Chưa thêm public hostname vào tunnel (7.3) |
| Cloudflare trả 502 | nginx-edge chưa healthy, hoặc public hostname trỏ sai `nginx-edge:8080` |
| Cloudflare trả 1033 | cloudflared chưa kết nối — xem log `cloudflared` |
| `carebridgevn.site` bỗng trả lỗi sau khi cấu hình tunnel | Đã lỡ thêm hostname đó vào tunnel — gỡ ra, để nguyên A record về Pages |
| `Set CAREBRIDGE_PORTAL_HOSTNAME` khi `up` | Thiếu `--env-file`, hoặc biến còn trống |
| Backend crash-loop, log `cannot alter type of a column used by a view or rule` | `SPRING_PROFILES_ACTIVE=local` — đổi sang `supabase` |
| `DATASOURCE_CONFIGURATION_INCOMPLETE` | Bộ ba `SUPABASE_DB_*` thiếu hoặc rỗng |
| cloudflared `Address already in use` | `CAREBRIDGE_NGINX_EDGE_IP` trùng `CAREBRIDGE_CLOUDFLARED_IP` |
| `network carebridge-face-origin declared as external, but could not be found` | Chưa `up` Container A trước Container B |
| Build backend OOM trên VM | Dưới 4 GB RAM — thêm swap, hoặc pull image thay vì build |
| Nhắc lịch / thông báo lịch hẹn không gửi | Worker mặc định `false` — xem mục 4 |
| Triage luôn trả RULE_BASED | `AI_TRIAGE_INTERNAL_API_KEY` lệch giữa backend và ai-triage |

---

## Phụ lục A — code-review-graph

`get_architecture_overview_tool`, graph 41 827 node / 301 810 cạnh / 13 469 file:

| Community | Kích thước | Cohesion | Ngôn ngữ chính |
| --- | --- | --- | --- |
| `service-service` | 12 569 | 0.166 | java |
| `src-error` | 8 610 | 0.198 | typescript |
| `screens-state` | 5 327 | 0.313 | dart |
| `tests-red` | 395 | 0.387 | python |
| `carebridge-evaluation-status` | 163 | 0.165 | python |
| `app-registry` | 65 | 0.365 | python |

Ba khối lớn nhất tách bạch theo đúng ranh giới triển khai — Java backend,
TypeScript portal, Dart mobile — và gần như không có cạnh nối trực tiếp giữa
chúng. Đó là dấu hiệu tốt: chúng chỉ giao tiếp qua HTTP, nên tách frontend sang
GitLab Pages không cắt vào một phụ thuộc code nào.

Cảnh báo duy nhất: **52 cạnh `CALLS` giữa `service-service` và `fixtures-order`**
— code production gọi vào cụm fixture của test. Không ảnh hưởng deploy, nhưng
đáng xem lại khi có thời gian.

`service-service` cohesion 0.166 với 12 569 node là thấp, gợi ý tầng service
Java đang gom nhiều thứ ít liên quan. Cũng là việc để sau.

## Phụ lục B — Edge DevTools

Cách kiểm chứng bundle production thay vì đoán. Chạy lại sau mỗi lần deploy Pages:

```js
// mcp__edge-devtools__new_page  →  https://carebridgevn.site
// rồi evaluate_script:
async () => {
  const src = [...document.querySelectorAll('script[src]')].map(s => s.src);
  const t = await (await fetch(src.find(s => s.includes('/assets/')))).text();
  return {
    bundleKB: Math.round(t.length / 1024),
    localhost:  !!t.match(/https?:\/\/localhost:8080/),
    apiHost:    !!t.match(/https:\/\/api\.carebridgevn\.site/),
    firebaseKey:/AIza[0-9A-Za-z_\-]{20,}/.test(t),
  };
}
```

Sau khi làm xong 7.3 và 7.5, kết quả đúng phải là
`localhost: false, apiHost: true, firebaseKey: true`.

Đo ngày 2026-08-08: `localhost: true, apiHost: false, firebaseKey: false` — cả
ba đều sai.
