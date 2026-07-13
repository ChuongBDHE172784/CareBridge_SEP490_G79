# Chạy CareBridge bằng Docker trên máy cá nhân

Docker Desktop phải đang chạy. Backend vẫn dùng cấu hình Supabase hiện có trong `05_Development/CareBridgeAPI/.env`; file này không được commit.

## Chuẩn bị

```sh
cp 05_Development/CareBridgeAPI/.env.example 05_Development/CareBridgeAPI/.env
```

Điền các giá trị Supabase và `JWT_SECRET` vào `.env`. Không đưa bất kỳ secret nào vào `docker-compose.yml`.

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
