# Hướng dẫn dựng EC2 cho CareBridge API — từ số 0

Dành cho người chưa từng dùng EC2. Làm tuần tự từ mục 1 đến mục 11. Mỗi mục có
lệnh copy-paste được và giải thích ngắn *vì sao* cần bước đó.

Sau khi xong: `https://api.carebridgevn.site` trả dữ liệu, và portal trên
`carebridgevn.site` gọi được API.

**Không có bước nào ở đây chạm vào frontend.** Portal vẫn nằm trên GitLab Pages.

Tài liệu tham chiếu sâu hơn: [DEPLOY-AWS.md](DEPLOY-AWS.md) ·
[SYSTEM-AUDIT.md](SYSTEM-AUDIT.md)

---

## 0. Trước khi bắt đầu — đổi mật khẩu AWS

Mật khẩu AWS của bạn đã bị dán vào một cuộc hội thoại chat, coi như đã lộ.

1. Đăng nhập AWS → góc phải trên → **Security credentials**
2. Đổi mật khẩu
3. Bật **MFA** cho tài khoản root (dùng Google Authenticator hoặc Microsoft Authenticator)
4. Vào **CloudTrail → Event history**, xem có đăng nhập lạ không

Làm xong mới sang mục 1.

---

## 1. Tạo EC2 instance

AWS Console → tìm **EC2** → **Instances** → **Launch instances**

Điền theo bảng. Mục nào không nói tới thì để mặc định.

| Trường | Chọn | Vì sao |
| --- | --- | --- |
| **Region** (góc phải trên) | **Asia Pacific (Tokyo) ap-northeast-1** | Supabase của bạn ở Tokyo. Cùng region → mỗi truy vấn DB nhanh hơn ~70ms |
| Name | `carebridge-api` | |
| **AMI** | **Ubuntu Server 24.04 LTS** (64-bit x86) | |
| **Instance type** | **m7i-flex.large** (2 vCPU, 8 GiB RAM) | 8 GiB chạy được cả CompreFace nên không phải nâng/hạ instance. `t3.micro` chỉ 1 GiB → Maven build sẽ bị OOM |
| **Key pair** | Create new key pair → tên `carebridge-key` → loại **RSA** → định dạng **.pem** → Create | File `.pem` tải về **chỉ một lần**, mất là không vào được máy |
| **Network settings** | Bấm **Edit** | xem mục 2 |
| **Storage** | đổi 8 GB → **30 GB** gp3 | Images ~4 GB + build cache. 8 GB sẽ hết đĩa |

Chưa bấm Launch. Làm mục 2 trước.

> ### Chi phí — đọc kỹ phần này
>
> `m7i-flex.large` ở Tokyo là **0.12369 USD/giờ** (Linux, giá hiển thị trong
> console lúc chọn instance). Nhãn *"Free tier eligible"* **không** nghĩa là miễn
> phí vô hạn: AWS Free Plan kiểu mới cấp **credit** (thường $100–200) rồi trừ
> dần, chứ không miễn phí theo loại instance như `t2.micro` ngày trước.
>
> | Cách dùng | Tiền/tháng |
> | --- | --- |
> | 24/7 | **~$90** — hết $100 credit trong ~5 tuần |
> | 8 giờ/ngày (dev + demo) | ~$30 |
> | 3 giờ/ngày | ~$11 |
>
> **Stop instance khi không dùng.** EC2 tính theo giây và chỉ khi đang chạy.
> Stop không mất dữ liệu. Xem credit còn lại ở **Billing and Cost Management →
> Free tier**, và đặt **Budget alert** ở mức $20 để không bị bất ngờ.
>
> Elastic IP (mục 3) bị tính ~$3.6/tháng kể cả khi instance đang Stop — AWS thu
> phí mọi địa chỉ IPv4 public từ 2024. Vẫn nên dùng, vì không có nó thì IP đổi
> mỗi lần Start và bạn phải sửa DNS liên tục.
>
> `m7i-flex` là dòng **burst CPU**: sustained khoảng 40% của 2 vCPU, burst được
> lên 100%. API lưu lượng vừa thì thoải mái; chỉ có Maven build là chậm hơn
> dòng t3 vài phút.

---

## 2. Security Group

Trong phần **Network settings** vừa bấm Edit:

- Security group name: `carebridge-api-sg`
- Xoá hết rule mặc định, tạo lại đúng **2 rule** dưới đây

**Rule 1 — SSH để bạn vào máy**

| Type | Port | Source |
| --- | --- | --- |
| SSH | 22 | **My IP** |

Chọn **My IP** chứ không phải `Anywhere`. Để `0.0.0.0/0` là cả Internet gõ cửa
SSH của bạn.

**Rule 2 — HTTPS cho API**

| Type | Port | Source |
| --- | --- | --- |
| HTTPS | 443 | tạm để **Anywhere-IPv4** `0.0.0.0/0` |

Tạm thời để `Anywhere` cho dễ debug lần đầu. **Mục 10 sẽ siết lại** chỉ còn dải
IP của Cloudflare — đừng bỏ qua bước đó.

**Tuyệt đối không mở:**

| Cổng | Vì sao không |
| --- | --- |
| `8080` | Spring Boot. Nginx đã reverse proxy, mở ra là bỏ qua toàn bộ lớp bảo vệ |
| `5432` | PostgreSQL |
| `8000` | CompreFace admin. Vào bằng SSH tunnel, xem mục 11 |

Xong thì bấm **Launch instance**.

---

## 3. Elastic IP — để IP không đổi

Mặc định IP public của EC2 **đổi mỗi lần Stop/Start**. Mỗi lần đổi là DNS sai và
API chết. Elastic IP giữ IP cố định.

EC2 → **Elastic IPs** (menu trái) → **Allocate Elastic IP address** → Allocate

Rồi chọn IP vừa tạo → **Actions → Associate Elastic IP address**:
- Resource type: **Instance**
- Instance: `carebridge-api`
- Associate

**Ghi lại IP này.** Mục 9 sẽ cần.

> Elastic IP miễn phí khi đang gắn vào instance **đang chạy**. Nếu bạn Stop
> instance mà vẫn giữ IP thì AWS tính phí nhỏ (~$3.6/tháng).

---

## 4. Vào máy bằng SSH (từ Windows)

Windows 11 có sẵn OpenSSH, không cần cài PuTTY. Mở **PowerShell** trên máy bạn.

Giả sử file key ở `C:\Users\admin\Downloads\carebridge-key.pem`:

```powershell
# Windows bắt buộc phải siết quyền file key, không thì SSH từ chối dùng nó
icacls "C:\Users\admin\Downloads\carebridge-key.pem" /inheritance:r
icacls "C:\Users\admin\Downloads\carebridge-key.pem" /grant:r "$($env:USERNAME):(R)"
```

Chuyển key vào chỗ ổn định hơn Downloads:

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\.ssh"
Move-Item "C:\Users\admin\Downloads\carebridge-key.pem" "$env:USERPROFILE\.ssh\carebridge-key.pem"
```

Vào máy (thay `<ELASTIC-IP>` bằng IP ở mục 3):

```powershell
ssh -i "$env:USERPROFILE\.ssh\carebridge-key.pem" ubuntu@<ELASTIC-IP>
```

Lần đầu nó hỏi `Are you sure you want to continue connecting?` → gõ `yes`.

Thành công thì dấu nhắc đổi thành `ubuntu@ip-xxx:~$`. **Từ đây trở đi, mọi lệnh
là chạy trên EC2, không phải trên máy bạn.**

<details>
<summary>Không vào được?</summary>

| Lỗi | Nguyên nhân |
| --- | --- |
| `Connection timed out` | Security Group chưa mở 22, hoặc IP nhà bạn đã đổi — vào Console sửa lại rule SSH thành My IP |
| `Permission denied (publickey)` | Sai user (phải là `ubuntu` với Ubuntu AMI, không phải `ec2-user`), hoặc sai file key |
| `UNPROTECTED PRIVATE KEY FILE` | Chưa chạy 2 lệnh `icacls` ở trên |

</details>

---

## 5. Cài Docker

Chạy trên EC2:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
```

Rồi **thoát và vào lại SSH** để quyền `docker` có hiệu lực:

```bash
exit
```
```powershell
ssh -i "$env:USERPROFILE\.ssh\carebridge-key.pem" ubuntu@<ELASTIC-IP>
```

Kiểm tra:

```bash
docker --version && docker compose version
docker run --rm hello-world
```

### Thêm swap 2 GB

RAM 4 GB vừa đủ để build, nhưng Maven hay chạm trần. Swap là lưới an toàn:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

Dòng `Swap:` phải hiện `2.0Gi`.

---

## 6. Lấy source code về EC2

Repo là private nên cần token đọc. **Đừng** dùng mật khẩu GitLab.

Trên **máy bạn**, mở GitLab trong browser:

1. Vào project `SU26_SEP490_G79`
2. **Settings → Repository → Deploy tokens → Add token**
3. Name: `ec2-deploy`, Scopes: tick **`read_repository`** (chỉ cái này)
4. **Create deploy token**
5. Copy **username** và **token** — token chỉ hiện một lần

Trên **EC2**:

```bash
git clone https://gitlab.com/manhnc2/su26_sep490_g79.git carebridge
```

Nó sẽ hỏi:
- `Username for 'https://gitlab.com':` → dán **username** của deploy token
- `Password for ...:` → dán **token** (gõ vào sẽ không hiện gì, cứ dán rồi Enter)

```bash
cd carebridge
git checkout dev          # hoặc nhánh bạn muốn deploy
ls docker-compose.aws.yml # phải thấy file này
```

> Nếu `docker-compose.aws.yml` không tồn tại nghĩa là commit chứa nó chưa được
> push lên `dev`. Quay lại máy bạn push trước.

---

## 7. Chứng chỉ TLS từ Cloudflare

Cloudflare cần bắt tay HTTPS với EC2. Chứng chỉ này miễn phí, hạn 15 năm, và chỉ
Cloudflare tin — đúng mục đích.

Trên **máy bạn**, vào Cloudflare dashboard:

1. Chọn domain `carebridgevn.site`
2. **SSL/TLS → Origin Server → Create Certificate**
3. Private key type: **RSA (2048)**
4. Hostnames: `api.carebridgevn.site` (thêm `*.carebridgevn.site` nếu muốn dùng lại sau)
5. Certificate Validity: **15 years** → **Create**
6. Giữ cửa sổ này mở — có 2 ô: **Origin Certificate** và **Private Key**

Trên **EC2**:

```bash
sudo install -d -m 700 /etc/carebridge/tls
sudo nano /etc/carebridge/tls/origin.pem
```

Dán toàn bộ ô **Origin Certificate** (gồm cả 2 dòng `-----BEGIN CERTIFICATE-----`
và `-----END CERTIFICATE-----`). Lưu bằng `Ctrl+O` → Enter → `Ctrl+X`.

```bash
sudo nano /etc/carebridge/tls/origin.key
```

Dán ô **Private Key**. Lưu và thoát như trên.

Bây giờ phần quan trọng nhất, thiếu là nginx không khởi động được:

```bash
sudo chmod 600 /etc/carebridge/tls/origin.pem /etc/carebridge/tls/origin.key
sudo chown 101:101 /etc/carebridge/tls/origin.pem /etc/carebridge/tls/origin.key
ls -l /etc/carebridge/tls/
```

Phải thấy owner là `101 101`. Lý do: image nginx chạy dưới user không phải root
(UID 101), nên không đọc được file thuộc root.

Cuối cùng, trên Cloudflare → **SSL/TLS → Overview** → đặt **Full (strict)**.

> Đừng chọn **Flexible**. Chặng Cloudflare→EC2 sẽ là HTTP trần đi qua Internet
> công cộng — không chấp nhận được với dữ liệu thai sản.

---

## 8. Điền biến môi trường

```bash
cd ~/carebridge
cp 05_Development/Deployment/server.env.example 05_Development/Deployment/.env.server
chmod 600 05_Development/Deployment/.env.server
nano 05_Development/Deployment/.env.server
```

File có 70 dòng biến. Điền theo chú thích trong file. Những dòng **bắt buộc**:

```dotenv
CAREBRIDGE_PORTAL_HOSTNAME=carebridgevn.site
CAREBRIDGE_API_HOSTNAME=api.carebridgevn.site
CAREBRIDGE_TLS_CERT_FILE=/etc/carebridge/tls/origin.pem
CAREBRIDGE_TLS_KEY_FILE=/etc/carebridge/tls/origin.key
CAREBRIDGE_HTTPS_PORT=443
SPRING_PROFILES_ACTIVE=supabase

# Ghim RAM — bắt buộc, xem giải thích ngay dưới
BACKEND_HEAP=1536m
BACKEND_METASPACE=256m
BACKEND_MEM_LIMIT=2560m
AI_MEM_LIMIT=1g

# CompreFace tắt cho tới hôm bảo vệ (mục 11)
CAREBRIDGE_COMPREFACE_ENABLED=false
```

> ### Vì sao phải ghim RAM của backend
>
> Image backend đặt `-XX:MaxRAMPercentage=75.0` — một **tỉ lệ tương đối**. Không
> chặn gì thì JVM tính 75% trên RAM của cả máy. Đo thực tế trên máy 8 GiB:
>
> | | `MaxHeapSize` |
> | --- | --- |
> | Mặc định của image | **5816 MiB** |
> | Ghim `-Xmx1536m` như trên | **1536 MiB** |
>
> 5816 MiB đủ để backend một mình chiếm gần hết RAM và làm chết ai-triage,
> exercise-correction, nginx. Lỗi này **chỉ hiện ra lúc tải cao** nên cực khó
> truy nguyên — nạn nhân là container khác, không phải backend.
>
> Ghim hai tầng:
>
> - `BACKEND_HEAP` → `-Xmx`, trần tuyệt đối ở tầng JVM. Chạy máy nào cũng như nhau.
> - `BACKEND_METASPACE` → metaspace nằm **ngoài** heap. Spring Boot nạp rất nhiều
>   class; không chặn thì RSS vượt `mem_limit` và container bị `OOMKilled` trong
>   khi heap vẫn còn chỗ trống.
> - `BACKEND_MEM_LIMIT` → chốt cuối ở tầng cgroup, bao trọn heap + metaspace +
>   code cache + stack của ~200 thread + direct buffer.
>
> `-Xms` để 256m (trong compose) nên lúc rảnh heap không giữ RAM vô ích, chỉ nở
> dần khi thật cần. Nâng `BACKEND_HEAP` thì nâng cả `BACKEND_MEM_LIMIT` theo.

Còn lại là credential: Supabase, JWT, Firebase, Cloudinary, R2, mail, Gemini,
Zego. Lấy từ file `.env` trên máy bạn (`05_Development/CareBridgeAPI/.env`).

**Chuyển bằng `scp`, đừng copy qua chat hay Google Doc.** Trên máy bạn:

```powershell
scp -i "$env:USERPROFILE\.ssh\carebridge-key.pem" `
  D:\Do_aN\05_Development\CareBridgeAPI\.env `
  ubuntu@<ELASTIC-IP>:/home/ubuntu/api.env
```

Rồi trên EC2 mở cả hai file cạnh nhau mà đối chiếu, hoặc dùng nó làm nền rồi bổ
sung. Xong thì **xoá file tạm**:

```bash
shred -u /home/ubuntu/api.env
```

### Bốn chỗ hay sai nhất

| Biến | Sai thế nào | Hậu quả |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | để `local` | Hibernate `ALTER` schema Supabase dùng chung → crash-loop, hỏng DB của cả nhóm |
| `SUPABASE_DB_URL` | dùng cổng `6543` | Transaction pooler không giữ session → Flyway không lấy được lock |
| `AI_TRIAGE_INTERNAL_API_KEY` | khác nhau giữa backend và ai-triage | Evidence registry fail-closed, không nguồn y khoa nào được duyệt |
| `REMINDER_SCHEDULE_NOTIFICATION_ENABLED` và các worker | không set | Hệ thống chạy bình thường nhưng **không bao giờ gửi nhắc lịch** |

> ⚠️ **Xoay credential trước khi dùng.** Hai file `carebridge-*.standalone.yml`
> từng bị commit lên `gitlab/dev` và `github/dev` với giá trị thật. Toàn bộ mật
> khẩu DB, JWT key, Firebase key trong đó phải coi như đã lộ.

---

## 9. Chạy stack và tạo DNS

```bash
cd ~/carebridge
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml up -d --build
```

Lần đầu **10–15 phút** (build backend Java + 2 service Python). Cứ để chạy.

Xong thì kiểm tra:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml ps
```

Phải thấy `backend`, `ai-triage`, `exercise-correction`, `nginx-edge` đều
**healthy**. Thứ tự lên: `backend` healthy trước, rồi `ai-triage` và `nginx-edge`
mới khởi động — đó là đúng, không phải lỗi.

Chưa healthy thì **đừng tạo DNS**, đọc log trước:

```bash
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml logs --tail=50 backend
```

### Test ngay trên EC2, trước khi mở ra Internet

```bash
curl -kI https://127.0.0.1/api/v1/master-data/provinces \
  --resolve api.carebridgevn.site:443:127.0.0.1
```

Phải trả `HTTP/2 200`. `-k` là vì chứng chỉ Origin CA chỉ Cloudflare tin, curl
không tin — bình thường.

**Chỉ khi lệnh trên trả 200 mới sang bước DNS.**

### Tạo bản ghi DNS

Cloudflare → domain `carebridgevn.site` → **DNS → Records → Add record**:

| Type | Name | IPv4 address | Proxy status | TTL |
| --- | --- | --- | --- | --- |
| `A` | `api` | Elastic IP ở mục 3 | **Proxied** (mây **cam**) | Auto |

Mây phải là **cam**, không phải xám. Xám nghĩa là lộ IP EC2 thẳng ra Internet.

**Đừng sửa bản ghi `carebridgevn.site`** — nó đang trỏ `35.185.44.232` về GitLab
Pages. Sửa nó là portal chết ngay.

Đợi 1–2 phút rồi test **từ máy bạn**:

```powershell
nslookup api.carebridgevn.site 1.1.1.1
curl.exe -I https://api.carebridgevn.site/api/v1/master-data/provinces
```

`nslookup` trả về IP của Cloudflare (không phải IP EC2) là **đúng** — đó là tác
dụng của Proxied.

---

## 10. Siết Security Group lại

Giờ API đã chạy, đóng cửa lại. Nếu để `443` mở cho `0.0.0.0/0` thì ai cũng gõ
thẳng IP EC2 được, mất hết tác dụng WAF và chống DDoS của Cloudflare.

Lấy danh sách dải IP hiện hành (đừng chép tay, danh sách có thay đổi):

```bash
curl -s https://www.cloudflare.com/ips-v4
```

Trong AWS Console → EC2 → Security Groups → `carebridge-api-sg` → **Edit inbound
rules**:

1. Xoá rule `HTTPS 443 từ 0.0.0.0/0`
2. Thêm một rule `HTTPS 443` cho **từng dải** trong danh sách trên (khoảng 15 dòng)
3. Giữ nguyên rule SSH 22

Test lại `curl -I https://api.carebridgevn.site/...` từ máy bạn — vẫn phải `200`.

---

## 11. Sau khi API chạy

### Rebuild bundle frontend

Bundle trên Pages hiện vẫn trỏ `localhost:8080`. Cần chạy lại pipeline GitLab:

1. Push commit chứa fix `.gitlab-ci.yml` lên `dev`
2. Thêm 6 biến `VITE_FIREBASE_*` ở **Settings → CI/CD → Variables** (không có
   chúng thì chat, gọi video và push notification sẽ hỏng)
3. Đợi pipeline xong, mở `https://carebridgevn.site` thử đăng nhập

### Quy trình bật CompreFace hôm bảo vệ đồ án

`m7i-flex.large` 8 GiB **không cần nâng cấp gì** — chỉ bật thêm stack CompreFace
rồi tắt lại khi xong. Nhưng RAM sẽ khá sát trần, nên phải siết JVM của CompreFace.

Nên **tập dượt trước một lần** vài ngày trước buổi bảo vệ. Lần đầu phải tải ~3 GB
image và setup admin UI, mất 15–20 phút — đừng làm lần đầu vào đúng hôm đó.

**Bước 1 — tính lại ngân sách RAM**

```
m7i-flex.large           8192 MiB
  OS Ubuntu              -  400
  backend (limit)        - 2560
  ai-triage (limit)      - 1024
  exercise (limit)       -  512
  nginx                  -   20
  ────────────────────────────────
  còn cho CompreFace       3676 MiB
```

CompreFace để mặc định cần ~4520 MiB (`compreface-api` `-Xmx1500m` → RSS ~2000,
`compreface-admin` `-Xmx1g` → ~1300, `compreface-core` uWSGI 2 process với model
ML ~1000, postgres ~200, fe ~20). **Vượt trần.** Nên siết xuống:

> **Đo thực tế sau khi siết** (chạy trên chính EC2 này): `compreface-core`
> 1.52 GiB, `api` 425 MiB, `admin` 357 MiB, `postgres` 51 MiB, `fe` 7 MiB —
> **tổng ~2.36 GiB**, thấp hơn ước tính 3.42 GiB khá nhiều. Với cả hai stack
> chạy cùng lúc, RAM dùng là **4.4 GiB / 7.6 GiB**, còn dư 3.2 GiB.

```bash
nano 05_Development/Deployment/.env.server
```
```dotenv
COMPREFACE_API_JAVA_OPTS=-Xmx1g
COMPREFACE_ADMIN_JAVA_OPTS=-Xmx512m
COMPREFACE_UWSGI_PROCESSES=1

# ai-triage đo thực tế chỉ ~108 MiB, hạ trần để nhường chỗ cho CompreFace
AI_MEM_LIMIT=512m
```

Sau khi hạ `AI_MEM_LIMIT`, phần dành cho CompreFace thành **4188 MiB** còn nó cần
~3420 MiB → dư ~768 MiB. Thoải mái. Swap 2 GB ở mục 5 lo phần peak.

Nhớ recreate `ai-triage` để trần mới có hiệu lực:

```bash
docker compose $EF -f docker-compose.aws.yml up -d --force-recreate ai-triage
```

**Bước 2 — chạy stack chính, rồi mới tới CompreFace**

```bash
export EF="--env-file 05_Development/Deployment/.env.server"
docker compose $EF -f docker-compose.aws.yml up -d

# CompreFace phải chạy SAU, vì nó dùng network carebridge-face-origin
# do stack chính tạo ra
docker compose $EF -f docker-compose.server.compreface.yml up -d
```

Lần đầu tải **~7.9 GB** image, mất 5–10 phút. Riêng `compreface-core` đã 6.03 GB
vì chứa model nhận diện. Dọn chỗ trước nếu đĩa chật:

```bash
docker builder prune -f
```

**Bước 3 — setup CompreFace (chỉ làm một lần)**

Mở UI qua SSH tunnel. **Không** tunnel tới `127.0.0.1:8000` — sẽ không bao giờ
kết nối được. Cả hai network của `compreface-fe` đều `internal: true`, mà Docker
không publish được cổng host cho container không có network routable nào: nó ghi
nhận binding nhưng **không có gì listen**, và không log lỗi ở đâu cả.

Host vẫn gọi thẳng IP container được, nên tunnel tới đó. Trên **EC2** lấy IP:

```bash
docker inspect carebridge-compreface-compreface-fe-1 \
  --format '{{(index .NetworkSettings.Networks "carebridge-face-origin").IPAddress}}'
```

Rồi trên **máy bạn** (thay `<IP>` bằng kết quả trên):

```powershell
ssh -i "$env:USERPROFILE\.ssh\carebridge-key.pem" -L 8000:<IP>:80 ubuntu@<ELASTIC-IP>
```

Giữ cửa sổ đó mở và **đừng gõ gì vào nó** — nó đang giữ đường hầm. Mở
`http://localhost:8000` trong **cửa sổ ẩn danh** (`Ctrl+Shift+N`).

> Phải là cửa sổ ẩn danh nếu bạn từng chạy CompreFace local cũng ở
> `localhost:8000`. Trình duyệt coi đó là cùng một origin và gửi cookie cũ lên;
> instance mới có DB trống nên từ chối token đó, admin log ghi
> `JdbcTokenStore: Failed to find access token`, và UI kẹt ở màn hình
> *"CompreFace is starting..."* với `Admin node: loading` vĩnh viễn — trông
> giống hệt lỗi hạ tầng nhưng thật ra chỉ là cookie.

1. Tạo tài khoản admin — tài khoản này của riêng CompreFace, không liên quan
   Supabase hay user CareBridge
2. Tạo một application
3. Trong application tạo 2 service: một type **Detection**, một type **Verify**.
   Tên tuỳ ý, **type mới là thứ quyết định**
4. Copy 2 API key

Key do CompreFace sinh ra khi bạn tạo service, gắn với DB của chính instance
này. Key của instance khác **không dùng được** — không thể chuẩn bị trước.

**Bước 4 — nối CompreFace vào backend**

```bash
nano 05_Development/Deployment/.env.server
```
```dotenv
CAREBRIDGE_COMPREFACE_ENABLED=true
CAREBRIDGE_COMPREFACE_BASE_URL=http://compreface-fe
CAREBRIDGE_COMPREFACE_DETECTION_API_KEY=<detection key>
CAREBRIDGE_COMPREFACE_VERIFICATION_API_KEY=<verification key>
```
```bash
docker compose $EF -f docker-compose.aws.yml up -d --force-recreate backend
docker stats --no-stream    # kiểm tra tổng RAM còn dư
```

**Bước 5 — sau khi bảo vệ xong, tắt CompreFace đi**

```bash
# $EF là bắt buộc kể cả khi down: file compose có ${COMPREFACE_POSTGRES_PASSWORD:?}
# nên thiếu --env-file là Compose từ chối, không tắt được gì cả.
# giữ DB khuôn mặt lại cho lần sau, KHÔNG dùng -v
docker compose $EF -f docker-compose.server.compreface.yml down

nano 05_Development/Deployment/.env.server
#   CAREBRIDGE_COMPREFACE_ENABLED=false

docker compose $EF -f docker-compose.aws.yml up -d --force-recreate backend
```

Không cần đổi instance type — 8 GiB vẫn dùng bình thường cho stack chính. Muốn
tiết kiệm tiền thì **Stop instance** lúc không dùng, xem bảng chi phí ở mục 1.

> ⚠️ Phải đặt lại `CAREBRIDGE_COMPREFACE_ENABLED=false` và recreate backend.
> Nếu quên, backend vẫn gọi `http://compreface-fe` trong khi container đã tắt →
> tính năng verify chuyên gia treo chờ timeout.
>
> ⚠️ **Đừng bao giờ dùng `down -v`** với stack CompreFace — cờ đó xoá volume
> `compreface_postgres_data`, tức là mất toàn bộ khuôn mặt đã đăng ký và phải
> làm lại từ bước 4.

### Cập nhật code mới

```bash
cd ~/carebridge && git pull
docker compose --env-file 05_Development/Deployment/.env.server \
  -f docker-compose.aws.yml up -d --build
docker image prune -f
```

### Lệnh hay dùng

```bash
cd ~/carebridge
export EF="--env-file 05_Development/Deployment/.env.server -f docker-compose.aws.yml"

docker compose $EF ps                          # trạng thái
docker compose $EF logs -f backend             # theo dõi log
docker compose $EF restart backend             # khởi động lại 1 service
docker compose $EF down                        # dừng (KHÔNG dùng -v, sẽ xoá volume)
docker stats --no-stream                       # xem RAM/CPU
free -h && df -h                               # RAM và đĩa còn bao nhiêu
```

---

## 12. Bảng tra lỗi

| Triệu chứng | Nguyên nhân |
| --- | --- |
| Cloudflare **521** Web server is down | Security Group chưa mở 443, hoặc `nginx-edge` chưa chạy |
| Cloudflare **522** Connection timed out | Security Group chặn, hoặc IP trong A record không còn đúng (chưa dùng Elastic IP?) |
| Cloudflare **526** Invalid SSL certificate | Chứng chỉ origin sai, hoặc để Full (strict) mà EC2 chưa có cert |
| nginx không lên, log `cannot load certificate` | Chưa `sudo chown 101:101` cho cert và key (mục 7) |
| `curl` trên EC2 báo cert không hợp lệ | Bình thường, dùng `-k` |
| Gõ thẳng IP EC2 không ra gì | **Đúng thiết kế** — nginx `return 444` |
| Backend crash-loop, log `cannot alter type of a column used by a view` | `SPRING_PROFILES_ACTIVE=local` → đổi sang `supabase` |
| `DATASOURCE_SUPABASE_CONFIGURATION_INCOMPLETE` | Bộ ba `SUPABASE_DB_*` thiếu hoặc rỗng |
| Build backend bị kill giữa chừng | Hết RAM — kiểm tra swap ở mục 5 |
| `no space left on device` | Đĩa 8 GB thay vì 30 GB. `docker system prune -a` chữa tạm |
| Portal vẫn báo "Không thể kết nối đến máy chủ" | Bundle Pages chưa rebuild — mục 11 |
| Portal trắng trang, console lỗi Firebase | Thiếu `VITE_FIREBASE_*` trong CI/CD Variables |
| `ai-triage` unhealthy | Kiểm tra `backend` healthy chưa. ai-triage cần backend làm evidence registry |
| Container bị kill ngẫu nhiên lúc tải cao, `docker inspect` thấy `OOMKilled: true` | Thiếu `BACKEND_MEM_LIMIT` — JVM đòi heap 75% RAM cả máy. Xem mục 8 |
| Bật CompreFace xong cả hệ thống chậm/treo, `docker stats` gần 8 GiB | Chưa siết `COMPREFACE_API_JAVA_OPTS` / `COMPREFACE_ADMIN_JAVA_OPTS`, xem mục 11 bước 1 |
| Tắt CompreFace rồi mà verify chuyên gia treo | Quên đặt `CAREBRIDGE_COMPREFACE_ENABLED=false` và recreate backend |
