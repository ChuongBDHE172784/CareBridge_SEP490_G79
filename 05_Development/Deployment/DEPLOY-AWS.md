# CareBridge — deploy API lên AWS EC2 (kiến trúc A record)

Deploy backend lên EC2 và cho Cloudflare proxy thẳng vào public IP của nó.
**Frontend không nằm ở đây** — portal là bundle tĩnh trên GitLab Pages sau
`carebridgevn.site`, và không có gì trong tài liệu này chạm vào nó.

```
Browser ─┬─▶ carebridgevn.site     ─▶ Cloudflare ─▶ GitLab Pages (React tĩnh)
         │
         └─▶ api.carebridgevn.site ─▶ Cloudflare ─▶ EC2:443 ─▶ nginx-edge ─▶ backend:8080
                                       (Proxied)                              │
                                                                              ├─▶ ai-triage:8001
                                                                              ├─▶ exercise-correction:8002
                                                                              └─▶ Supabase / Gemini / Firebase / R2

                                                       (tuỳ chọn) ─▶ compreface-fe:80  Container B
```

Hai nhánh chỉ gặp nhau **trong trình duyệt của người dùng**. GitLab Pages không
kết nối tới Nginx, và Nginx không phục vụ file frontend nào.

Toàn cảnh hệ thống, phiên bản service, danh sách biến môi trường: xem
[SYSTEM-AUDIT.md](SYSTEM-AUDIT.md).

## 0. Chọn file compose

Repo có ba stack API. Dùng đúng một cái.

| File | Đường vào | Dùng khi |
| --- | --- | --- |
| **`docker-compose.aws.yml`** | **A record → EC2:443** | **Tài liệu này** |
| `docker-compose.server.yml` | Cloudflare Tunnel | Khi nâng cấp sau này (mục 9) |
| `docker-compose.production.yml` | Cloudflare Tunnel + image theo digest | Khi CI đã đẩy image lên registry |

## 1. Yêu cầu EC2

| | |
| --- | --- |
| RAM | **4 GB** nếu build tại chỗ (Maven cần ~2 GB), 2 GB nếu chỉ pull image |
| | cộng **2 GB** nếu bật CompreFace |
| Disk | 20 GB trở lên |
| OS | Ubuntu 22.04 / 24.04 |

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER" && newgrp docker
git clone <repo-url> carebridge && cd carebridge
```

## 2. Security Group

Chỉ cần **hai** rule inbound. Không mở 8080, không mở 5432.

| Type | Port | Source | Lý do |
| --- | --- | --- | --- |
| SSH | 22 | **IP nhà bạn**, không phải `0.0.0.0/0` | quản trị |
| HTTPS | 443 | dải IP của Cloudflare (bên dưới) | traffic API |

Không cần mở 80: Cloudflare ở chế độ Full (strict) chỉ gọi origin qua 443, còn
redirect HTTP→HTTPS thì bật **SSL/TLS → Edge Certificates → Always Use HTTPS**
để Cloudflare xử lý ngay ở edge.

Lấy dải IP hiện hành của Cloudflare (đừng chép tay, danh sách có thay đổi):

```bash
curl -s https://www.cloudflare.com/ips-v4
```

Mở 443 cho **từng dải** trong danh sách đó. Nếu để `0.0.0.0/0` thì ai cũng gõ
thẳng được vào IP EC2, mất hết tác dụng của WAF và DDoS protection —
nginx vẫn trả `444` nhưng bạn đã ăn trọn gói tin.

> Nếu thấy việc bảo trì danh sách IP phiền, bật thêm **Cloudflare Authenticated
> Origin Pulls** (SSL/TLS → Origin Server). Khi đó origin yêu cầu client
> certificate mà chỉ Cloudflare có, và bạn không phải theo dõi dải IP nữa.

## 3. Cloudflare Origin Certificate

Bắt buộc. Không có cert hợp lệ ở origin thì Full (strict) không bắt tay được.

1. Cloudflare → **SSL/TLS → Origin Server → Create Certificate**
2. Hostname: `api.carebridgevn.site` (thêm `*.carebridgevn.site` nếu muốn dùng lại)
3. Chọn **RSA 2048**, hạn 15 năm
4. Copy **Origin Certificate** và **Private Key** — key chỉ hiện **một lần**

Trên EC2:

```bash
sudo install -d -m 700 /etc/carebridge/tls
sudo install -m 600 /dev/null /etc/carebridge/tls/origin.pem
sudo install -m 600 /dev/null /etc/carebridge/tls/origin.key
sudo nano /etc/carebridge/tls/origin.pem   # dán Origin Certificate
sudo nano /etc/carebridge/tls/origin.key   # dán Private Key

# nginx-unprivileged chạy dưới UID 101, không đọc được file của root
sudo chown 101:101 /etc/carebridge/tls/origin.pem /etc/carebridge/tls/origin.key
```

Đặt ở `/etc/` chứ không phải `/run/` để cert sống qua reboot.

Sau đó Cloudflare → **SSL/TLS → Overview** → đặt **Full (strict)**.
Đừng dùng **Flexible**: chặng Cloudflare→EC2 sẽ là HTTP trần đi qua Internet
công cộng, với dữ liệu thai sản thì không chấp nhận được.

## 4. Cloudflare DNS

Thêm **một** record:

| Type | Name | Content | Proxy | TTL |
| --- | --- | --- | --- | --- |
| `A` | `api` | Public IPv4 của EC2 | **Proxied** (mây cam) | Auto |

Lấy IP đúng, đừng chép từ trí nhớ — chạy trên chính EC2:

```bash
TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 60")
curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/public-ipv4; echo
```

> Dùng **Elastic IP**. IP public mặc định của EC2 đổi mỗi lần stop/start, và mỗi
> lần đổi là bạn phải sửa lại record này.

**Không đụng vào record của frontend.** `carebridgevn.site` phải giữ nguyên
`A 35.185.44.232` trỏ GitLab Pages. Nếu đổi nó sang IP EC2 là portal chết ngay.

Kiểm tra sau vài phút:

```bash
nslookup api.carebridgevn.site 1.1.1.1
```

Proxied nên kết quả trả về là IP của Cloudflare, **không phải** IP EC2. Đó là
đúng — nếu thấy IP EC2 thật nghĩa là mây vẫn xám, bấm cho nó thành cam.

## 5. Biến môi trường

```bash
cp 05_Development/Deployment/server.env.example 05_Development/Deployment/.env.server
chmod 600 05_Development/Deployment/.env.server
nano 05_Development/Deployment/.env.server
```

Bắt buộc cho stack này:

```dotenv
CAREBRIDGE_PORTAL_HOSTNAME=carebridgevn.site        # origin CORS, không phải hostname stack này phục vụ
CAREBRIDGE_API_HOSTNAME=api.carebridgevn.site
CAREBRIDGE_TLS_CERT_FILE=/etc/carebridge/tls/origin.pem
CAREBRIDGE_TLS_KEY_FILE=/etc/carebridge/tls/origin.key
SPRING_PROFILES_ACTIVE=supabase
```

Bốn chỗ hay sai nhất:

- `SPRING_PROFILES_ACTIVE` **không được** là `local` — profile `local` bật
  `ddl-auto: update`, Hibernate sẽ `ALTER` schema Supabase dùng chung rồi
  crash-loop.
- DB dùng **session pooler cổng 5432**, không phải transaction pooler 6543.
- `AI_TRIAGE_INTERNAL_API_KEY` phải giống hệt ở backend và ai-triage.
- Các worker nền (`REMINDER_SCHEDULE_NOTIFICATION_ENABLED`,
  `APPOINTMENT_NOTIFICATION_ENABLED`, `CAREBRIDGE_RECOMMENDATION_ENABLED`) mặc
  định `false` trong `application.yaml`. Không set `true` thì hệ thống chạy bình
  thường nhưng **không bao giờ gửi nhắc lịch**.

`CLOUDFLARE_TUNNEL_TOKEN_FILE` và các biến `CAREBRIDGE_TUNNEL_*` **không dùng**
ở stack này, cứ để nguyên.

## 6. Deploy

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml up -d --build
```

Lần đầu mất 10–15 phút vì build cả backend lẫn hai service Python.

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml ps
```

Mong đợi `backend`, `ai-triage`, `exercise-correction`, `nginx-edge` đều
`healthy`. Chưa healthy thì **đừng đụng vào Cloudflare**, đọc log trước:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml logs --tail=100 backend
```

## 7. Kiểm tra, từ trong ra ngoài

Kiểm theo thứ tự này. Hỏng ở bước nào thì dừng ở đó, đừng nhảy cóc.

```bash
# 1. Backend sống, từ trong Docker network (8080 không ra Internet)
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml exec nginx-edge \
  wget -qO- http://backend:8080/actuator/health/readiness

# 2. Nginx phục vụ TLS trên chính EC2. --insecure vì cert Origin CA
#    chỉ Cloudflare tin, curl thì không.
curl -kI https://127.0.0.1/api/v1/master-data/ --resolve api.carebridgevn.site:443:127.0.0.1

# 3. Gõ thẳng IP phải KHÔNG ra gì — đây là mong đợi, không phải lỗi
curl -k https://127.0.0.1/ ; echo "exit=$?"    # 52 hoặc 000 = nginx đóng kết nối, đúng

# 4. Từ Internet, qua Cloudflare
curl -I https://api.carebridgevn.site/api/v1/master-data/

# 5. Preflight CORS từ đúng origin của portal
curl -i -X OPTIONS https://api.carebridgevn.site/api/v1/master-data/ \
  -H "Origin: https://carebridgevn.site" \
  -H "Access-Control-Request-Method: GET"
```

Bước 5 phải trả `Access-Control-Allow-Origin: https://carebridgevn.site` và
`Access-Control-Allow-Credentials: true`. Nếu thiếu, kiểm tra
`CAREBRIDGE_PORTAL_HOSTNAME`.

Cuối cùng, kiểm tra bundle trên Pages đã trỏ đúng API chưa — mở
`https://carebridgevn.site`, DevTools → Network, đăng nhập thử và xem request đi
tới `api.carebridgevn.site` chứ không phải `localhost:8080`. Nếu vẫn là
localhost thì chạy lại pipeline GitLab, xem
[SYSTEM-AUDIT.md §7.5](SYSTEM-AUDIT.md).

## 8. CompreFace (Container B, tuỳ chọn)

Nặng khoảng 2 GB RAM và 3 GB image, nên tách riêng để bật/tắt độc lập. Chạy sau
khi stack chính đã lên, vì nó dùng network `carebridge-face-origin` do stack
chính tạo.

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.compreface.yml up -d
```

Setup lần đầu qua SSH tunnel, **không** mở cổng ra Internet:

Không dùng `127.0.0.1:8000` — `compreface-fe` chỉ nằm trên network `internal`,
nên Docker không publish được cổng host cho nó và sẽ không có gì listen. Tunnel
tới IP container:

```bash
# trên EC2
docker inspect carebridge-compreface-compreface-fe-1 \
  --format '{{(index .NetworkSettings.Networks "carebridge-face-origin").IPAddress}}'
```
```bash
# trên máy bạn
ssh -L 8000:<IP>:80 <user>@<ec2-ip>
```

Mở `http://localhost:8000` bằng **cửa sổ ẩn danh** để tránh cookie của
CompreFace local.

Tạo tài khoản admin → tạo application → tạo service **Face detection** và
**Face verification** → copy 2 API key vào `.env.server`:

```dotenv
CAREBRIDGE_COMPREFACE_ENABLED=true
CAREBRIDGE_COMPREFACE_BASE_URL=http://compreface-fe
CAREBRIDGE_COMPREFACE_DETECTION_API_KEY=<detection key>
CAREBRIDGE_COMPREFACE_VERIFICATION_API_KEY=<verification key>
```

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml up -d --force-recreate backend
```

Tắt đi khi không dùng:

```bash
docker compose -f docker-compose.server.compreface.yml down     # giữ dữ liệu
docker compose -f docker-compose.server.compreface.yml down -v   # xoá luôn DB khuôn mặt
```

Nhớ đặt lại `CAREBRIDGE_COMPREFACE_ENABLED=false` rồi recreate backend, không thì
backend gọi vào hư không.

## 9. Nâng cấp lên Cloudflare Tunnel (sau này)

Khi kiến trúc A record đã chạy ổn, chuyển sang Tunnel sẽ bỏ được: cổng 443 mở ra
Internet, việc bảo trì danh sách IP Cloudflare trong Security Group, và cả
Origin Certificate.

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml down

# đặt CLOUDFLARE_TUNNEL_TOKEN_FILE trong .env.server, rồi:
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.server.yml up -d --build
```

Sau đó ở Cloudflare: xoá A record `api`, thêm public hostname
`api.carebridgevn.site` → `HTTP` → `nginx-edge:8080` trong tunnel, và đóng 443
trong Security Group. Xem [DEPLOY-SERVER.md](DEPLOY-SERVER.md).

## 10. Xử lý sự cố

| Triệu chứng | Nguyên nhân |
| --- | --- |
| Cloudflare **521** Web server is down | Security Group chưa mở 443 cho dải IP Cloudflare, hoặc container `nginx-edge` chưa chạy |
| Cloudflare **526** Invalid SSL certificate | Origin Certificate sai/hết hạn, hoặc đang để Full (strict) mà origin chưa có cert |
| Cloudflare **522** Connection timed out | Security Group chặn, hoặc IP trong A record không còn đúng (EC2 đã stop/start) |
| nginx không lên, log `cannot load certificate` | File cert/key chưa chown sang UID 101 |
| `curl` từ trong EC2 báo cert không hợp lệ | Bình thường — Origin CA chỉ Cloudflare tin. Dùng `-k` |
| Gõ thẳng IP EC2 trả về rỗng | **Đúng như thiết kế** — `return 444` |
| Portal gọi API bị CORS | `CAREBRIDGE_PORTAL_HOSTNAME` khác origin thật của trình duyệt |
| Portal gọi vào `localhost:8080` | Bundle Pages build thiếu `VITE_API_URL` — chạy lại pipeline. Lưu ý tên biến là `VITE_API_URL`, **không phải** `VITE_API_BASE_URL` |
| Portal trắng trang, console lỗi Firebase | Thiếu `VITE_FIREBASE_*` ở Settings → CI/CD → Variables |
| Backend crash-loop, log `cannot alter type of a column used by a view or rule` | `SPRING_PROFILES_ACTIVE=local` — đổi sang `supabase` |
| `DATASOURCE_SUPABASE_CONFIGURATION_INCOMPLETE` | Bộ ba `SUPABASE_DB_URL/USERNAME/PASSWORD` thiếu hoặc rỗng |
| Log chỉ thấy IP Cloudflare, không thấy IP người dùng | Dải `set_real_ip_from` trong `edge-aws.conf` đã cũ — cập nhật từ `curl -s https://www.cloudflare.com/ips-v4` |
| `network carebridge-face-origin declared as external, but could not be found` | Chưa `up` stack chính trước Container B |
| Build backend OOM | VM dưới 4 GB RAM — thêm swap, hoặc pull image thay vì build |
| Nhắc lịch / thông báo lịch hẹn không gửi | Worker mặc định `false` — xem mục 5 |
