# CI/CD Docker cho CareBridge

Pipeline GitLab chạy `validate → test → security → build → publish → deploy` cho Spring Boot backend và React/Vite web. Flutter mobile không tạo Docker image và chưa có job CI vì repo chưa có cấu hình CI mobile phù hợp. Merge Request chỉ validate/test/scan; chỉ default branch mới publish.

## Điều kiện và GitLab Runner

Bật **Settings → General → Visibility, project features, permissions → Container Registry**. Runner dùng Docker executor, Docker Engine và `docker:dind` với `privileged = true`; pipeline bật BuildKit (`DOCKER_BUILDKIT=1`). Đây là phương án tương thích khi runner hiện tại chưa xác nhận hỗ trợ Kaniko/BuildKit rootless. Nếu shared runner không cho privileged, hãy chuyển sang Kaniko/rootless trong thay đổi riêng đã kiểm thử.

Backend test dùng Testcontainers nên cần Docker daemon. Web dùng `npm ci`, lint và build; chưa có script test. Không đưa secret vào Docker build arg hoặc `VITE_*`: Vite đóng gói các biến này vào bundle công khai.

```toml
[[runners]]
  executor = "docker"
  [runners.docker]
    privileged = true
    volumes = ["/cache"]
```

## CI/CD variables

`CI_REGISTRY`, `CI_REGISTRY_IMAGE`, `CI_REGISTRY_USER`, `CI_REGISTRY_PASSWORD` là biến GitLab cung cấp sẵn; không tự in hay lưu mật khẩu Registry. Tạo các biến deploy tại **Settings → CI/CD → Variables**:

| Biến | Phạm vi | Protected | Masked | Ghi chú |
|---|---|---:|---:|---|
| `SSH_PRIVATE_KEY` | staging/prod | Có | Có (File nếu hỗ trợ) | key của deploy user, không phải root |
| `SSH_KNOWN_HOSTS` | staging/prod | Có | Không | output đã kiểm chứng của `ssh-keyscan -H <host>`; ghim host key, không lấy lại trong pipeline |
| `DEPLOY_HOST` | staging/prod | Có | Có nếu chính sách cho phép | host staging |
| `DEPLOY_USER` | staging/prod | Có | Không bắt buộc | deploy account |
| `DEPLOY_PATH` | staging/prod | Có | Không | thư mục chứa compose và `.env` |
| `VITE_API_URL` | build (không phải secret) | Theo môi trường | Không | URL API công khai, ví dụ `https://staging-api.example`; được đóng gói vào web image |

Đặt environment scope `staging`, bảo vệ default branch/tag và giới hạn quyền chạy manual job. Pipeline chỉ tạo deploy job với ref được GitLab đánh dấu protected. Server tự cấu hình quyền pull Registry bằng deploy token read-only lưu trong Docker credential store; không truyền token Registry qua SSH/CI command.

## Chạy local và pipeline

```sh
docker build -t carebridge-backend:local 05_Development/CareBridgeAPI
docker build -t carebridge-web:local 05_Development/CareBridgeWebApp
cd 05_Development/Deployment
cp .env.example .env # điền secret cục bộ, không commit
BACKEND_IMAGE=carebridge-backend:local WEB_IMAGE=carebridge-web:local docker compose -f docker-compose.staging.yml config
BACKEND_IMAGE=carebridge-backend:local WEB_IMAGE=carebridge-web:local docker compose -f docker-compose.staging.yml up -d
```

Push branch và tạo MR để kích hoạt pipeline. Sau merge vào default branch, pipeline push `$CI_REGISTRY_IMAGE/backend:$CI_COMMIT_SHORT_SHA` và image web cùng tag; `latest` chỉ được thêm ở default branch. Protected tag cũng publish SHA nhưng không `latest`. Không có publish job trong MR.

## Báo cáo bảo mật và lỗi thường gặp

Semgrep xuất `semgrep-report.json` dạng GitLab SAST. Trivy filesystem ghi `trivy-fs-report.json`; CRITICAL làm fail pipeline. HIGH chạy advisory `allow_failure`, hiển thị cảnh báo nhưng không chặn mặc định. Trivy scan CRITICAL image tar trước publish, nên image lỗi không được push.

Sửa finding trước. False positive Trivy phải thêm đúng ID vào `.trivyignore`, kèm lý do, ngày review và expiry. Semgrep chỉ thêm rule/exclusion hẹp có comment vào `.semgrep.yml`; không tắt scanner. `Cannot connect to Docker daemon` nghĩa runner thiếu privileged dind; `npm ci` thường là lockfile không khớp.

## Deploy staging và rollback

Staging chỉ manual từ default branch/tag và dùng SHA bất biến, không dùng `latest`:

```sh
export BACKEND_IMAGE=registry.example/group/project/backend:abc1234
export WEB_IMAGE=registry.example/group/project/web:abc1234
docker compose -f docker-compose.staging.yml pull
docker compose -f docker-compose.staging.yml up -d --remove-orphans
```

Hoặc chạy `deploy_staging`. Production không tự deploy. Rollback: chọn SHA của pipeline thành công trước đó, thay hai biến image rồi chạy lại `pull`/`up`; không rebuild hoặc publish lại.

## Checklist production

- [ ] Registry, branch/tag, variables và environment đã protected.
- [ ] Deploy user không phải root; `SSH_KNOWN_HOSTS` đã được xác minh ngoài pipeline và host key rotation có quy trình rõ ràng.
- [ ] `.env` chỉ ở server và secret đã xoay vòng nếu từng lộ.
- [ ] CRITICAL đã sửa; HIGH có owner và hạn xử lý.
- [ ] Backup/restore DB, monitoring, log retention và incident response đã thử.
- [ ] API có health/readiness HTTP thật: hiện tại healthcheck backend chỉ xác minh JVM process, không xác minh DB/request.
- [ ] Kiểm tra GitLab CI Lint trong UI trước merge.
