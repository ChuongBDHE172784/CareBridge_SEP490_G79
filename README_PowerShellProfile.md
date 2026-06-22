# PowerShell Profile Setup - LamVH

## Cài đặt PowerShell Profile cho Git Dual Remote

File này hướng dẫn cách cài đặt PowerShell profile với các alias và function hỗn trợ làm việc với 2 remote Git.

---

## 📦 Files đã tạo

1. **`powershell_profile_lamvh.ps1`** - Template PowerShell profile với:
   - Environment variables cho Claude Code
   - Functions: `git-dual-status`, `git-fetch-all`, `git-push-dev-both`, `git-set-email`, `git-check-protected-branches`
   - Aliases: `gds`, `gfa`, `gpdb`, `gse`, `gcheck`
   - Welcome message khi mở terminal

2. **`GIT_RULE_CANHAN_LamVH.md`** - Hướng dẫn chi tiết (đã có rule không merge vào main)

---

## 🚀 Cài đặt nhanh

### Bước 1: Kiểm tra PowerShell Profile hiện tại

```powershell
# Kiểm tra file profile hiện tại
Test-Path $PROFILE
```

Nếu kết quả là `False`, tạo file mới:

```powershell
# Tạo thư mục nếu chưa có
New-Item -Path (Split-Path $PROFILE) -Type Directory -Force

# Tạo file profile rỗng
New-Item -Path $PROFILE -Type File -Force
```

### Bước 2: Thêm nội dung profile vào $PROFILE

Mở file profile:

```powershell
notepad $PROFILE
```

Copy toàn bộ nội dung từ `powershell_profile_lamvh.ps1` và paste vào file `$PROFILE`, sau đó lưu.

### Bước 3: Restart PowerShell

Đóng và mở lại PowerShell (hoặc VS Code terminal) để load profile.

---

## ✅ Kiểm tra cài đặt

Sau khi restart PowerShell, bạn sẽ thấy welcome message màu xanh:

```
==================================
🔧 Git Dual Remote - LamVH loaded
==================================
📧 GitHub: lamgameplayforme@gmail.com
📧 GitLab: lamvhhe186943@fpt.edu.vn

🚨 NHANH MAIN ĐƯỢC BẢO VỆ - Không được merge trực tiếp!

📋 Các lệnh hữu ích:
   gds          - Xem trạng thái tổng hợp (git dual-status)
   gfa          - Fetch từ cả 2 remote (git fetch-all)
   gpdb         - Push dev lên cả 2 remote (git-push-dev-both)
   gse          - Chọn email commit (git-set-email)
   gcheck       - Kiểm tra nhánh có được bảo vệ không
```

---

## 🎯 Các lệnh hữu ích

### Kiểm tra trạng thái tổng hợp
```powershell
gds
```
Hiển thị:
- Git status
- Commits chưa push lên GitHub
- Commits chưa push lên GitLab

### Fetch từ cả 2 remote
```powershell
gfa
```
Tự động fetch từ cả `github` và `gitlab`.

### Push dev lên cả 2 remote
```powershell
gpdb
```
Hỏi xác nhận rồi push nhánh `dev` lên cả 2 remote.

### Chọn email commit
```powershell
gse              # Xem menu chọn
gse -github      # Chọn email GitHub ngay
gse -gitlab      # Chọn email GitLab ngay
```

### Kiểm tra nhánh protected
```powershell
gcheck
```
Kiểm tra xem bạn có đang ở nhánh `main` không. Nếu đang ở main, sẽ cảnh báo và hướng dẫn chuyển sang nhánh khác.

---

## 🔧 Customize thêm (optional)

Nếu bạn muốn tự động check nhánh mỗi khi mở terminal, uncomment dòng này trong profile:

```powershell
# git-check-protected-branches | Out-Null
```

---

## 📝 Notes

- File `powershell_profile_lamvh.ps1` là **template**, bạn copy nội dung vào `$PROFILE`
- `$PROFILE` là đường dẫn đến file PowerShell profile của user hiện tại
- Các function và alias chỉ có sẵn trong PowerShell session sau khi load profile
- Để chỉnh sửa profile sau này: `notepad $PROFILE`
- Để reload profile mà không restart: `. $PROFILE`

---

## 🔍 Troubleshooting

**Lỗi: "The term 'gds' is not recognized"**
- Chưa load profile: Restart PowerShell hoặc chạy `. $PROFILE`
- Profile chưa được tạo: Kiểm tra `Test-Path $PROFILE` và tạo nếu cần

**Lỗi: "git is not recognized"**
- Git chưa được thêm vào PATH. Cài đặt Git từ git-scm.com và chọn "Add to PATH"

**Lỗi: "fatal: 'github' does not appear to be a git repository"**
- Chưa cấu hình 2 remote. Chạy `git remote -v` để kiểm tra
- Nếu chưa có, thêm remote: `git remote add github <url>` và `git remote add gitlab <url>`

---

**Quy tắc quan trọng: KHÔNG BAO GIỜ merge vào nhánh main!**
