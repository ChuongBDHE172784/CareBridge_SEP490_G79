---
name: git-dual-remote-personal-rule-lamvh
description: Cấu hình Git Dual Remote cá nhân cho LamVH - email GitHub và GitLab + PowerShell profile
metadata:
  type: user
---

**Tài khoản Commit:**
- GitHub: `lamgameplayforme@gmail.com`
- GitLab: `lamvhhe186943@fpt.edu.vn`

**Files đã tạo:**
1. `GIT_RULE_CANHAN_LamVH.md` - Hướng dẫn chi tiết cá nhân (QUAN TRỌNG: KHÔNG merge vào main)
2. `setup-git-local.ps1` - Script PowerShell để setup nhanh
3. `powershell_profile_lamvh.ps1` - Template PowerShell profile với alias/functions
4. `README_PowerShellProfile.md` - Hướng dẫn cài đặt profile

**Quy trình làm việc:**
- Nhánh cá nhân: `LamVH`
- Nhánh chung: `dev`
- Remote: `github` và `gitlab`
- Luôn pull từ cả 2 remote trước khi code
- Chọn email phù hợp trước khi commit

**QUY TẮC NGHIÊM NGẶT (MUST NOT - Violation = Critical):**
- ❌ **KHÔNG BAO GIỜ merge vào nhánh `main`** - chỉ merge vào `dev`
- Nhánh `main` là production, chỉ trưởng nhóm được phép merge `dev` → `main`
- Mọi code mới phải đi qua `dev` trước khi lên `main`
- Nếu ai yêu cầu merge vào main, từ chối và hướng dẫn đưa code vào dev trước

**Claude Code (Codex) Config:**
- Environment variables: `GIT_DUAL_REMOTE=true`, `GIT_EMAIL_GITHUB`, `GIT_EMAIL_GITLAB`
- Git aliases/functions:
  - `git dual-status` (gds): Xem trạng thái tổng hợp + commits chưa push
  - `git fetch-all` (gfa): Fetch từ cả 2 remote
  - `git push-dev-both` (gpdb): Push dev lên cả 2 remote (có xác nhận)
  - `git-set-email` (gse): Chọn email commit nhanh
  - `git-check-protected-branches` (gcheck): Cảnh báo nếu đang ở nhánh main
- PowerShell profile với welcome message và auto-check optional

**Cài đặt PowerShell Profile:**
```powershell
# Copy template vào $PROFILE
notepad $PROFILE  # Paste nội dung từ powershell_profile_lamvh.ps1
# Restart PowerShell
```

**Cách sử dụng:**
```powershell
.\setup-git-local.ps1  # Setup nhanh cấu hình email và check remotes
gds                    # Xem trạng thái tổng hợp
gcheck                 # Kiểm tra có đang ở main không
gfa                    # Fetch cả 2 remote
# ... code và commit ...
gpdb                   # Push dev lên cả 2 remote (chỉ sau khi merge xong và test kỹ)
```

**Lưu ý:** Các file này đã được thêm vào `.gitignore`:
- `*GIT_RULE_CANHAN*.md`
- `*setup-git*.ps1`
- `*powershell_profile*.ps1`

→ Tất cả đều **local only**, KHÔNG COMMIT lên git.

