# Chạy CareBridge bằng Docker trên máy cá nhân

Docker Desktop phải đang chạy. Backend vẫn dùng cấu hình Supabase hiện có trong `05_Development/CareBridgeAPI/.env`; file này không được commit.

## Chuẩn bị

```sh
cp 05_Development/CareBridgeAPI/.env.example 05_Development/CareBridgeAPI/.env
```

Điền các giá trị Supabase cùng `JWT_ACTIVE_KEY_ID`, `JWT_PRIVATE_KEY` (base64 DER PKCS#8) và `JWT_PUBLIC_KEYS` (`kid:base64-DER-SPKI`, phân tách nhiều khóa bằng dấu `;`) vào `.env`. Không đưa private key hoặc secret nào vào `docker-compose.yml` hay Git.

Dev seed mặc định tắt. Chỉ khi chạy backend bằng profile `dev` (không có profile
`prod`) và cần fixture synthetic local, người vận hành mới đặt
`CAREBRIDGE_DEV_SEED_ENABLED=true` đồng thời nạp một
`CAREBRIDGE_DEV_SEED_PASSWORD` riêng, không mặc định, vào `.env` local không commit
hoặc secret source tương đương. Không in/ghi lại giá trị. Password trống hoặc
historical default đã retired sẽ làm backend fail startup khi seed được bật. Không
bật dev seed trên staging/production.

## Chạy ứng dụng

Tại thư mục gốc repository:

```sh
docker compose up --build
```

- Web: http://localhost:5173
- API: http://localhost:8080

Web được build với `VITE_API_URL=http://localhost:8080`; giá trị này đúng khi browser và Docker Desktop cùng chạy trên máy cá nhân.

## Vận hành

```sh
docker compose up -d --build  # chạy nền
docker compose logs -f        # xem log
docker compose ps             # kiểm tra container
docker compose down           # dừng và xóa container/network
docker compose down --rmi local  # dừng và xóa cả image local
```

Sau khi thay đổi backend/web, chạy lại `docker compose up --build`. Không dùng cấu hình này cho staging/production; môi trường đó cần URL API công khai, secrets server-side và cấu hình deploy riêng.
