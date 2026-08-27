# Project-specific R8/ProGuard rules belong in this file.
# Flutter and plugin consumer rules are supplied by their Android dependencies.

# ---------------------------------------------------------------------------
# Room / WorkManager
# ---------------------------------------------------------------------------
# Bản release crash ngay khi mở app:
#   Unable to get provider androidx.startup.InitializationProvider
#   Caused by: Failed to create an instance of androidx.work.impl.WorkDatabase
#
# WorkManager (kéo vào qua flutter_foreground_task) dựng WorkDatabase bằng
# reflection: Room sinh ra lớp WorkDatabase_Impl lúc biên dịch rồi tra tên lớp
# đó khi chạy. R8 full mode — bật mặc định từ AGP 8 — không thấy chỗ nào gọi
# trực tiếp nên đổi tên hoặc loại bỏ, và phần tra tên thất bại.
#
# Chỉ giữ đúng phần Room cần tìm bằng tên, không giữ cả cây androidx.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepclassmembers class * extends androidx.room.RoomDatabase { public <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Worker được WorkManager khởi tạo qua tên lớp trong dữ liệu công việc đã lưu,
# nên tên lớp và hàm dựng phải giữ nguyên.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# ZEGO (gọi thoại / video)
# ---------------------------------------------------------------------------
# Log bản release trước báo NoSuchMethodException: initApiCalledCallback —
# ZEGO gọi phương thức này qua reflection nên R8 rút gọn làm mất.
-keep class im.zego.** { *; }
-dontwarn im.zego.**
