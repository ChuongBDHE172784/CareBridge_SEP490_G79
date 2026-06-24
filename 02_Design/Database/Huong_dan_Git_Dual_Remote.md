# Hướng dẫn Cấu hình & Sử dụng Git với 2 Remote (GitHub & GitLab) dùng chung SSH Key

Tài liệu này hướng dẫn bạn từ đầu cách tạo SSH Key dùng chung, liên kết với GitHub & GitLab, clone dự án về thư mục **Đồ án** và cách quản lý, thao tác đồng bộ code với cả 2 hệ thống hàng ngày.

---

## PHẦN 1: Cấu hình SSH Key dùng chung cho cả GitHub và GitLab

Thay vì tạo nhiều key phức tạp, bạn có thể tạo duy nhất một SSH Key trên máy tính của mình và đăng ký nó với cả hai tài khoản GitHub và GitLab.

### Bước 1: Tạo SSH Key mới (nếu chưa có)
Nếu trên máy của bạn chưa từng có SSH key, hãy mở Terminal và chạy lệnh sau:
```bash
ssh-keygen -t ed25519 -C "huy412004@gmail.com" -f ~/.ssh/id_ed25519
```
*Lưu ý: Khi hệ thống hỏi nhập mật khẩu (passphrase), bạn chỉ cần nhấn **Enter** 2 lần để bỏ qua.*

### Bước 2: Lấy nội dung Public Key để thêm vào GitHub & GitLab
Chạy lệnh sau trong Terminal để xem nội dung public key của bạn:
```bash
cat ~/.ssh/id_ed25519.pub
```
Hãy bôi đen và sao chép (Copy) toàn bộ chuỗi ký tự hiển thị ra (bắt đầu bằng `ssh-ed25519 ...`).

### Bước 3: Đăng ký Key lên các tài khoản

#### A. Đăng ký trên GitHub (Email: `huy412004@gmail.com`)
1. Truy cập [github.com](https://github.com) và đăng nhập vào tài khoản của bạn.
2. Click vào ảnh đại diện của bạn ở góc trên bên phải -> chọn **Settings**.
3. Chọn mục **SSH and GPG keys** ở menu bên trái.
4. Click vào nút **New SSH key**.
5. Nhập tiêu đề (Title) ví dụ: `Macbook Pro` và dán (Paste) nội dung public key đã copy vào ô **Key**.
6. Nhấn **Add SSH key**.

#### B. Đăng ký trên GitLab (Email: `huyndhe186775@fpt.edu.vn`)
1. Truy cập [gitlab.com](https://gitlab.com) và đăng nhập vào tài khoản của bạn.
2. Click vào ảnh đại diện của bạn ở góc trên bên phải -> chọn **Preferences** (hoặc **Settings**).
3. Chọn mục **SSH Keys** ở menu bên trái.
4. Click **Add new key**.
5. Dán (Paste) nội dung public key vào ô **Key**.
6. Nhập tiêu đề (Title) và nhấn **Add key**.

### Bước 4: Kiểm tra kết nối SSH
Chạy 2 lệnh sau để xác nhận máy tính của bạn đã kết nối thành công tới cả 2 dịch vụ:
```bash
ssh -T git@github.com
ssh -T git@gitlab.com
```
*Nếu hiển thị thông báo chào mừng chứa username của bạn là thành công.*

---

## PHẦN 2: Khởi tạo dự án & Cấu hình 2 Remote song song

Chúng ta sẽ clone dự án từ GitHub về trước làm thư mục gốc, sau đó tích hợp remote của GitLab vào chung thư mục đó.

### Bước 1: Clone dự án từ GitHub về thư mục "Đồ án"
Mở Terminal và chạy lệnh sau để clone code về:
```bash
git clone git@github.com:ChuongBDHE172784/CareBridge_SEP490_G79.git "/Users/huy/Documents/Đồ án/CareBridge_SEP490_G79"
```

### Bước 2: Thiết lập 2 Remote trong thư mục dự án
Di chuyển vào thư mục dự án vừa clone:
```bash
cd "/Users/huy/Documents/Đồ án/CareBridge_SEP490_G79"
```

Đổi tên remote mặc định `origin` (hiện tại đang trỏ tới GitHub) thành `github` cho rõ ràng:
```bash
git remote rename origin github
```

Thêm remote thứ hai trỏ tới GitLab và đặt tên là `gitlab`:
```bash
git remote add gitlab git@gitlab.com:manhnc2/su26_sep490_g79.git
```

Kiểm tra lại danh sách remote bằng lệnh:
```bash
git remote -v
```
Kết quả hiển thị chính xác sẽ là:
```text
github  git@github.com:ChuongBDHE172784/CareBridge_SEP490_G79.git (fetch)
github  git@github.com:ChuongBDHE172784/CareBridge_SEP490_G79.git (push)
gitlab  git@gitlab.com:manhnc2/su26_sep490_g79.git (fetch)
gitlab  git@gitlab.com:manhnc2/su26_sep490_g79.git (push)
```

### Bước 3: Cấu hình Email hoạt động cho thư mục
Vì bạn dùng 2 email khác nhau, trước khi commit code, bạn nên chỉ định email local cho thư mục này. Ví dụ, để mặc định dùng email GitHub:
```bash
git config user.email "huy412004@gmail.com"
```
> [!TIP]
> **Cách chuyển đổi Email khi Commit:**
> Khi bạn muốn commit code và ghi nhận đóng góp dưới danh nghĩa email GitLab, trước khi thực hiện commit hãy chạy:
> ```bash
> git config user.email "huyndhe186775@fpt.edu.vn"
> ```
> Sau khi commit xong, nếu muốn quay lại email GitHub:
> ```bash
> git config user.email "huy412004@gmail.com"
> ```
> Để kiểm tra email hiện tại đang dùng cho thư mục này, gõ: `git config user.email`

---

## PHẦN 3: Hướng dẫn làm việc hàng ngày (Workflow)

Khi làm việc với mô hình 2 remote song song, bạn cần chỉ định rõ tên remote (`github` hoặc `gitlab`) trong các câu lệnh Git của mình.

### 0. Kiểm tra trạng thái dự án (Trạng thái, Nhánh, Email, Commit chưa push)

Trước khi thực hiện bất kỳ thao tác gộp hay đẩy code nào, bạn hãy sử dụng các lệnh kiểm tra sau để nắm rõ trạng thái:

*   **Kiểm tra Email (Tài khoản) hiện tại đang cấu hình:**
    ```bash
    git config user.email
    ```
    *(Kết quả sẽ trả về email bạn đã cấu hình local cho thư mục này, giúp bạn biết commit tiếp theo sẽ thuộc về tài khoản GitHub hay GitLab).*

*   **Kiểm tra Nhánh hiện tại và file thay đổi:**
    ```bash
    git status
    ```
    *(Cho bạn biết: Bạn đang đứng ở nhánh nào; Có file nào bị sửa đổi chưa lưu hay không; Và nhánh của bạn có đang đi trước/sau remote hay không).*

*   **Xem nhanh tên nhánh hiện tại:**
    ```bash
    git branch --show-current
    ```

*   **Kiểm tra các Commit đã tạo ở máy nhưng CHƯA PUSH lên remote:**
    *   Xem các commit chưa push lên **GitHub** (ví dụ so với nhánh `dev`):
        ```bash
        git log github/dev..HEAD --oneline
        ```
    *   Xem các commit chưa push lên **GitLab** (ví dụ so với nhánh `dev`):
        ```bash
        git log gitlab/dev..HEAD --oneline
        ```
    *(Nếu lệnh không trả về dòng nào, nghĩa là tất cả commit local của bạn đã được push lên remote tương ứng thành công).*

### 1. Cập nhật thông tin các nhánh từ cả 2 phía (Fetch)
Trước khi làm việc hoặc chuyển nhánh, hãy đồng bộ danh sách nhánh mới nhất từ cả hai bên:
```bash
git fetch github
git fetch gitlab
```

### 2. Kéo code mới về (Pull)
*   Để cập nhật code mới nhất từ nhánh `main` của **GitHub**:
    ```bash
    git pull github main
    ```
*   Để cập nhật code mới nhất từ nhánh `main` của **GitLab**:
    ```bash
    git pull gitlab main
    ```

### 3. Xem danh sách toàn bộ các nhánh
```bash
git branch -a
```
*(Kết quả sẽ liệt kê các nhánh local của bạn, cùng các nhánh remote dạng `remotes/github/nhanh-abc` và `remotes/gitlab/nhanh-xyz`).*

### 4. Tạo nhánh mới và chuyển nhánh (Checkout)
*   Tạo nhánh local mới bắt đầu từ code của **GitHub**:
    ```bash
    git checkout -b <ten-nhanh-local> github/<ten-nhanh-remote>
    # Ví dụ: git checkout -b feature-abc github/main
    ```
*   Tạo nhánh local mới bắt đầu từ code của **GitLab**:
    ```bash
    git checkout -b <ten-nhanh-local> gitlab/<ten-nhanh-remote>
    # Ví dụ: git checkout -b feature-xyz gitlab/main
    ```

### 5. Đẩy code lên (Push)
*   Đẩy nhánh local của bạn lên **GitHub**:
    ```bash
    git push github <ten-nhanh-local>
    ```
*   Đẩy nhánh local của bạn lên **GitLab**:
    ```bash
    git push gitlab <ten-nhanh-local>
    ```

---

## PHẦN 4: Quy trình làm việc hàng ngày của bạn (Nhánh dev & Nhánh HuyND)

Dưới đây là quy trình chi tiết từng bước khi bạn bắt đầu một ngày làm việc: Code trên nhánh `HuyND`, cập nhật từ nhánh chung `dev` của cả 2 bên, hợp nhất và đẩy ngược lại cho cả đội ngũ.

### Bước 1: Cập nhật nhánh `dev` mới nhất từ cả 2 Remote về máy
Trước khi code, bạn cần đảm bảo nhánh `dev` ở máy của bạn chứa tất cả code mới nhất mà các thành viên khác đã push lên GitHub và GitLab:

```bash
# 1. Chuyển sang nhánh dev local
git checkout dev

# 2. Lấy code mới nhất của dev từ GitHub về
git pull github dev

# 3. Lấy code mới nhất của dev từ GitLab về (gộp chung vào dev local)
git pull gitlab dev
```
*(Nếu lúc này xảy ra xung đột giữa nhánh dev của GitHub và GitLab, Git sẽ báo lỗi. Bạn hãy giải quyết xung đột trực tiếp trên nhánh dev này trước).*

### Bước 2: Cập nhật code mới vào nhánh làm việc cá nhân `HuyND`
```bash
# 1. Chuyển sang nhánh HuyND của bạn
git checkout HuyND

# 2. Gộp code mới từ dev local vừa cập nhật vào nhánh HuyND của bạn
git merge dev
```

### Bước 3: Thực hiện viết code và Commit
Bạn tiến hành code và tạo các thay đổi trên nhánh `HuyND`.
Khi muốn lưu lại (Commit):

```bash
# 1. (Tùy chọn) Chọn email phù hợp với remote bạn muốn ghi nhận commit
# Dùng GitHub email:
git config user.email "huy412004@gmail.com"
# Hoặc dùng GitLab email:
git config user.email "huyndhe186775@fpt.edu.vn"

# 2. Thêm và Commit code
git add .
git commit -m "Mô tả tính năng hoặc lỗi đã sửa"
```

### Bước 4: Hợp nhất (Merge) lại vào `dev` local và xử lý xung đột
Sau khi code xong và test chạy thử ổn định trên máy của bạn:

```bash
# 1. Chuyển về nhánh dev local
git checkout dev

# 2. Gộp nhánh HuyND vào dev local
git merge HuyND
```

> [!IMPORTANT]
> **Xử lý xung đột (Conflict Resolution):**
> * Nếu Git báo **Conflict**, các file bị xung đột sẽ được đánh dấu. Bạn hãy mở các file đó lên trên VS Code, chọn phần code giữ lại (Accept Current, Accept Incoming hoặc giữ cả hai).
> * Sau khi sửa hết xung đột, chạy lệnh lưu lại:
>   ```bash
>   git add .
>   git commit -m "Merge branch HuyND and resolve conflicts"
>   ```

### Bước 5: Đẩy (Push) code sạch lên cả 2 Remote cho cả đội dùng
Khi nhánh `dev` local của bạn đã hợp nhất xong và không còn xung đột:

```bash
# 1. Đẩy nhánh dev lên GitHub
git push github dev

# 2. Đẩy nhánh dev lên GitLab
git push gitlab dev
```

*(Tùy chọn) Bạn cũng nên đẩy cả nhánh cá nhân `HuyND` của mình lên cả 2 remote để lưu trữ dự phòng:*
```bash
git push github HuyND
git push gitlab HuyND
```

---

## PHẦN 5: Các nguyên tắc & Lưu ý quan trọng để hạn chế Xung đột (Conflict) và giữ Git sạch sẽ

Làm việc nhóm trên cả hai nền tảng GitHub và GitLab rất dễ dẫn đến xung đột code nếu không tuân thủ các nguyên tắc cơ bản. Dưới đây là các mẹo thực tế giúp bạn làm việc trơn tru:

### 1. Nguyên tắc "PULL trước, PUSH sau"
Đây là nguyên tắc vàng. Trước khi bạn gõ bất kỳ dòng lệnh `git push` nào, hoặc trước khi bắt đầu code một ngày mới, hãy chạy:
```bash
git checkout dev
git pull github dev
git pull gitlab dev
```
Việc này đảm bảo bạn luôn code trên nền phiên bản mới nhất của dự án, tránh bị lệch xa commit so với các thành viên khác.

### 2. Chia nhỏ Commit (Commit Small & Often)
*   **Tránh:** Code liên tục 2-3 ngày, sửa hàng chục file rồi mới thực hiện 1 commit duy nhất. Nếu có conflict, bạn sẽ phải giải quyết một đống hỗn độn cực kỳ mất thời gian.
*   **Nên:** Làm xong một phần nhỏ của tính năng (ví dụ: code xong giao diện của 1 trang, viết xong 1 hàm API), hãy test chạy thử rồi **Commit ngay**. 
*   Việc chia nhỏ commit giúp bạn dễ dàng "quay xe" (revert) về phiên bản cũ hơn nếu code bị lỗi, và giải quyết conflict (nếu có) cũng rất nhẹ nhàng vì phạm vi thay đổi nhỏ.

### 3. Cách cất tạm code chưa hoàn thiện bằng `git stash`
Khi bạn đang code dở trên nhánh `HuyND` (code chưa chạy được nên chưa muốn commit), nhưng trưởng nhóm yêu cầu bạn chuyển sang nhánh khác gấp để kiểm tra lỗi:
*   **Cất tạm code đang làm dở:**
    ```bash
    git stash
    ```
    *(Git sẽ dọn sạch thư mục làm việc của bạn về trạng thái commit gần nhất, giúp bạn chuyển nhánh thoải mái).*
*   **Lấy lại code đã cất để làm tiếp:**
    Sau khi xong việc, quay lại nhánh `HuyND` và gõ:
    ```bash
    git stash pop
    ```

### 4. Không bao giờ code trực tiếp trên nhánh `dev`
*   Nhánh `dev` là nhánh chung, chỉ dùng để chứa code ổn định đã gộp từ các thành viên.
*   **Mọi công việc code của bạn bắt buộc phải làm trên nhánh cá nhân `HuyND`**. Khi tính năng đã hoàn thiện, bạn mới merge từ `HuyND` vào `dev` ở máy local của mình, giải quyết conflict xong xuôi rồi mới được push `dev` lên remote.

### 5. Dọn dẹp các file rác bằng `.gitignore`
*   Hãy đảm bảo dự án có file `.gitignore` để bỏ qua các file sinh ra trong quá trình chạy code (như thư mục `node_modules`, file cấu hình của VS Code `.vscode`, file cấu hình môi trường `.env`, hoặc file rác hệ điều hành `.DS_Store`).
*   Nếu lỡ tay commit nhầm một file/thư mục rác lên Git, hãy chạy lệnh sau để gỡ tracking mà không xóa file gốc ở máy:
    ```bash
    git rm -r --cached <ten-file-hoac-thu-muc>
    # Ví dụ xóa file .DS_Store khỏi git tracking:
    git rm --cached .DS_Store
    ```

### 6. Viết thông điệp Commit có ý nghĩa (Semantic Commit)
Hãy tập thói quen viết tên commit rõ ràng, có cấu trúc để cả nhóm dễ theo dõi lịch sử code:
*   `feat: <nội dung>` (Thêm tính năng mới) - Ví dụ: `feat: add OTP login`
*   `fix: <nội dung>` (Sửa lỗi) - Ví dụ: `fix: validation form register`
*   `style: <nội dung>` (Sửa CSS, định dạng code không ảnh hưởng logic)
*   `refactor: <nội dung>` (Cải tiến code cũ nhưng không đổi tính năng)


