# Danh Sách Lỗi & Vấn Đề Cần Fix (CareBridge & Gadgetbridge)

Tài liệu này tổng hợp toàn bộ các lỗi (errors), cảnh báo (warnings), và sự cố cấu hình được ghi nhận từ IDE và phân tích hệ thống để bàn giao cho **Codex** xử lý triệt để.

---

## 1. Tổng quan phân nhóm lỗi

| Nhóm | Vị trí / Module | Số lượng | Tính chất |
| :--- | :--- | :--- | :--- |
| **Nhóm 1** | `05_Development/CareBridgeMobileApp/android` | 1 lỗi chặn build | Gradle & Android Gradle Plugin (AGP) version mismatch |
| **Nhóm 2** | `05_Development/Gadgetbridge/app` (Gson Adapters) | 46 lỗi | Thiếu dependencies `Gson` & `errorprone annotations` |
| **Nhóm 3** | `05_Development/Gadgetbridge/app` (DSLV - DragSortListView) | 160+ lỗi | Thiếu Android Framework SDK & AndroidX dependencies |
| **Nhóm 4** | `05_Development/Gadgetbridge/app` (LineageOS SDK / Weather API) | 80+ lỗi | Thiếu AIDL / IPC Interfaces & Android OS Core types |
| **Nhóm 5** | `05_Development/CareBridgeMobileApp` (Dart / Flutter) | 66 issues | Deprecated APIs & Linting issues |

---

## 2. Chi tiết từng nhóm lỗi

### Nhóm 1: Lỗi Gradle Version Mismatch (CareBridgeMobileApp Android)

- **File**: `05_Development/CareBridgeMobileApp/android/build.gradle.kts:29`
- **File**: `05_Development/CareBridgeMobileApp/android/app/build.gradle.kts:1`
- **Thông báo lỗi**:
  ```text
  The supplied phased action failed with an exception.
  A problem occurred configuring root project 'android'.
  Build file '.../CareBridgeMobileApp/android/build.gradle.kts' line: 29
  A problem occurred configuring project ':app'.
  Build file '.../CareBridgeMobileApp/android/app/build.gradle.kts' line: 1
  An exception occurred applying plugin request [id: 'com.android.application']
  Failed to apply plugin 'com.android.internal.version-check'.
  Minimum supported Gradle version is 9.1.0. Current version is 8.9.
  Try updating the 'distributionUrl' property in .../gradle/wrapper/gradle-wrapper.properties to 'gradle-9.1.0-bin.zip'.
  ```
- **Nguyên nhân**: Android Gradle Plugin (AGP) đang dùng phiên bản yêu cầu tối thiểu Gradle 9.1.0, nhưng file `gradle-wrapper.properties` hiện tại đang trỏ về Gradle 8.9.
- **Cách fix cho Codex**:
  1. Mở `05_Development/CareBridgeMobileApp/android/gradle/wrapper/gradle-wrapper.properties`.
  2. Cập nhật `distributionUrl` thành:
     ```properties
     distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
     ```
  3. Hoặc hạ phiên bản AGP trong `android/settings.gradle.kts` / `android/build.gradle.kts` cho tương thích với Gradle 8.9.

---

### Nhóm 2: Lỗi Gson TypeAdapters (`com.google.gson.typeadapters`)

- **File**: `05_Development/Gadgetbridge/app/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java`
- **Chi tiết lỗi**:
  - `Line 21`: `The import com.google.errorprone cannot be resolved`
  - `Line 22`: `The import com.google.gson.Gson cannot be resolved`
  - `Line 23`: `The import com.google.gson.JsonElement cannot be resolved`
  - `Line 24`: `The import com.google.gson.JsonNull cannot be resolved`
  - `Line 25`: `The import com.google.gson.JsonObject cannot be resolved`
  - `Line 26`: `The import com.google.gson.JsonParseException cannot be resolved`
  - `Line 27`: `The import com.google.gson.JsonPrimitive cannot be resolved`
  - `Line 28`: `The import com.google.gson.TypeAdapter cannot be resolved`
  - `Line 29`: `The import com.google.gson.TypeAdapterFactory cannot be resolved`
  - `Line 30`: `The import com.google.gson.reflect cannot be resolved`
  - `Line 31-32`: `The import com.google.gson.stream cannot be resolved`
  - `Line 161`: `TypeAdapterFactory cannot be resolved to a type`
  - `Line 209, 221, 241`: `CanIgnoreReturnValue cannot be resolved to a type`
  - `Line 247, 258, 259, 260, 262, 267, 287, 304`: `TypeAdapter / Gson / TypeToken cannot be resolved to a type`
  - `Line 269, 300`: `JsonReader / JsonWriter cannot be resolved to a type`
  - `Line 270, 271, 331`: `JsonElement cannot be resolved to a type`
  - `Line 279, 289, 306, 319`: `JsonParseException cannot be resolved to a type`
  - `Line 309, 316`: `JsonObject cannot be resolved to a type`
  - `Line 326`: `JsonPrimitive cannot be resolved to a type`
  - `Line 328`: `JsonNull cannot be resolved to a variable`
  - `Line 336`: `The method nullSafe() is undefined for the type new TypeAdapter<R>(){}`
- **Nguyên nhân**: Thiếu thư viện `com.google.code.gson:gson` và `com.google.errorprone:error_prone_annotations` trong classpath / build configuration của Gadgetbridge.

---

### Nhóm 3: Lỗi DSLV (DragSortListView - `com.mobeta.android.dslv`)

Hàng loạt file trong package `com.mobeta.android.dslv` bị thiếu imports và types của Android SDK và AndroidX:

#### 1. `CheckableLinearLayout.java`
- **Imports không resolve được**: `android.content.*`, `android.util.*`, `android.widget.*` (Lines 3-6)
- **Types không resolve được**: `LinearLayout`, `Checkable`, `Context`, `AttributeSet` (Lines 8, 11, 13, 19, 20, 25, 30, 35)
- **Methods không hợp lệ**: `onFinishInflate()`, `getChildAt()`, `isChecked()`, `setChecked()`, `toggle()` (Lines 18, 20, 24, 29, 34)

#### 2. `DragSortController.java`
- **Imports không resolve được**: `android.graphics.*`, `android.view.*`, `android.widget.*` (Lines 3-9)
- **Types không resolve được**: `View`, `GestureDetector`, `ListView`, `OnGestureListener`, `ViewConfiguration`, `MotionEvent`, `Point`, `HapticFeedbackConstants`, `AdapterView`
- **Methods/Inheritance**: Incompatible return types giữa `DragSortListView.FloatViewManager.onCreateFloatView` và `SimpleFloatViewManager.onCreateFloatView` (Line 22); Missing methods `getContext()`, `getHeaderViewsCount()`, `getFooterViewsCount()`, `getCount()`, `getWidth()`, `getFirstVisiblePosition()`, `pointToPosition()`

#### 3. `DragSortCursorAdapter.java`
- **Imports không resolve được**: `android.content.*`, `android.database.*`, `androidx.*`, `android.util.*`, `android.view.*`, `android.widget.*` (Lines 6-12)
- **Types không resolve được**: `CursorAdapter`, `SparseIntArray`, `Context`, `Cursor`, `View`, `ViewGroup`
- **Methods**: `notifyDataSetChanged()`, `getItem()`, `getItemId()`, `getCount()`

#### 4. `DragSortItemView.java` & `DragSortItemViewCheckable.java`
- **Imports không resolve được**: `android.content.*`, `android.view.*`, `android.widget.*`
- **Types & Variables**: `ViewGroup`, `Gravity`, `Context`, `AbsListView`, `MeasureSpec`, `Checkable`
- **Methods**: `setLayoutParams()`, `onLayout()`, `getChildAt()`, `getMeasuredWidth()`, `getMeasuredHeight()`, `onMeasure()`, `setMeasuredDimension()`, `getLayoutParams()`, `isChecked()`, `setChecked()`, `toggle()`

#### 5. `DragSortListPreference.java` & `DragSortListPreferenceFragment.java`
- **Imports không resolve được**: `android.content.*`, `android.util.*`, `androidx.*`, `nodomain.freeyourgadget.gadgetbridge.R`, `com.google.android.*`
- **Types**: `ListPreference`, `TypedArray`, `ArrayAdapter`, `MaterialAlertDialogBuilder`, `DialogPreference`, `Preference`, `NonNull`
- **Methods**: `callChangeListener()`, `setValue()`, `getPersistedString()`, `getEntryValues()`, `getValue()`, `getEntries()`, `persistString()`, `getPreference()`, `setItemChecked()`, `isItemChecked()`, `findPreference()`

#### 6. `DragSortListView.java`
- **Imports không resolve được**: `android.content.*`, `android.database.*`, `android.graphics.*`, `android.os.*`, `android.util.*`, `android.view.*`, `android.widget.*`, `nodomain.freeyourgadget.gadgetbridge.R` (Lines 30-54)
- **Types**: `ListView`, `View`, `Point`, `DataSetObserver`, `MotionEvent`, `Context`, `ListAdapter`, `BaseAdapter`, `WrapperListAdapter`, `Canvas`, `SystemClock`, `SparseBooleanArray`
- **Methods**: `onChanged()`, `onInvalidated()`, `getItemId()`, `getItem()`, `getCount()`, `areAllItemsEnabled()`, `isEnabled()`, `getItemViewType()`, `getViewTypeCount()`, `hasStableIds()`, `isEmpty()`, `getHeaderViewsCount()`, `getFirstVisiblePosition()`, `getPaddingLeft()`, `getPaddingTop()`, `getHeight()`, `onSizeChanged()`, `requestLayout()`
- **TODOs**:
  - Line 1606: `TODO: what if float view is null because we dropped in middle`
  - Line 2877: `TODO: Bluuurgh... switch to SharedPreferences`

#### 7. `ResourceDragSortCursorAdapter.java` & `SimpleDragSortCursorAdapter.java`
- **Types & Methods**: `LayoutInflater`, `Context`, `Cursor`, `View`, `ViewGroup`, `TextView`, `ImageView`, `Uri`, `setViewText()`, `setViewImage()`.
- **Hierarchy error**: `The hierarchy of the type ResourceDragSortCursorAdapter is inconsistent` (Line 35), `The hierarchy of the type SimpleDragSortCursorAdapter is inconsistent` (Line 54).

#### 8. `SimpleFloatViewManager.java`
- **Types & Variables**: `Bitmap`, `ImageView`, `Color`, `ListView`, `ViewGroup`, `Point`
- **Interface Methods**: Missing implementations for `DragSortListView.FloatViewManager.onDragFloatView(View, Point, Point)` and `onDestroyFloatView(View)`.

---

### Nhóm 4: Lỗi LineageOS Framework & Weather Provider (`lineageos.*`)

Các file thuộc LineageOS SDK stubs bị lỗi compile do thiếu Android OS Parcelable, Location, TextUtils, và các AIDL generated interfaces:

#### 1. `lineageos.os.Concierge.java`
- Missing `android.os.Parcel` (Lines 19, 77, 89, 99, 106, 108, 114, 116, 121, 126, 144-149).

#### 2. `lineageos.providers.WeatherContract.java`
- Missing `android.net.Uri` (Lines 19, 34, 37, 39-44).

#### 3. `lineageos.weather.LineageWeatherManager.java`
- Missing `android.annotation.*`, `android.content.*`, `android.location.Location`, `android.os.Handler/IBinder/RemoteException`, `android.util.ArraySet/Log`, `androidx.annotation.RequiresApi/NonNull/SuppressLint`.
- Missing AIDL Types: `ILineageWeatherManager`, `IRequestInfoListener`, `IWeatherServiceProviderChangeListener`.
- Missing methods / callbacks: `onWeatherServiceProviderChanged()`, `onWeatherRequestCompleted()`, `onLookupCityRequestCompleted()`.
- Warnings: Raw type warnings on `Class` (Lines 128, 129).

#### 4. `lineageos.weather.RequestInfo.java`
- Missing: `android.location.Location`, `android.os.Parcel/Parcelable`, `android.text.TextUtils`, `IRequestInfoListener`, `Creator<RequestInfo>`.
- Methods: `newArray()`, `describeContents()`.

#### 5. `lineageos.weather.WeatherInfo.java` & `WeatherLocation.java`
- Missing: `Parcelable`, `Parcel`, `NonNull`, `TextUtils`, `CREATOR`, `Creator<WeatherLocation>`.

#### 6. `lineageos.weatherservice.ServiceRequest.java`
- Missing: `IWeatherProviderServiceClient`, `RemoteException`, `NonNull`.

---

### Nhóm 5: Flutter / Dart Deprecations & Lints (CareBridgeMobileApp)

- **File**: `lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart`
  - Lines 1384, 1387, 1390, 1393: `Statements in an if should be enclosed in a block (curly_braces_in_flow_control_structures)`
- **File**: `lib/features/healthRecords/screens/edit_health_metric_screen.dart`
  - Lines 701, 750, 805: `'value' is deprecated and shouldn't be used. Use initialValue instead (deprecated_member_use)`
  - Lines 797, 852, 922: `Statements in an if should be enclosed in a block (curly_braces_in_flow_control_structures)`
- **File**: `lib/features/healthRecords/screens/health_metric_trend_screen.dart`
  - Line 620: `Unnecessary use of multiple underscores (unnecessary_underscores)`
  - Lines 1363, 1365, 1367, 1746, 1748, 2211, 2214, 2217, 2220, 2223, 2226, 2229, 2232: `curly_braces_in_flow_control_structures`
  - Lines 1756-1771: `Use null-aware marker '?' rather than null check via 'if' (use_null_aware_elements)`
  - Line 2058: `Don't use BuildContext across async gaps (use_build_context_synchronously)`
- **File**: `lib/features/safety/screens/safety_monitoring_screen.dart`
  - Line 1376: `'withOpacity' is deprecated. Use .withValues() instead (deprecated_member_use)`
- **File**: `test/features/healthRecords/add_maternal_health_metric_screen_test.dart`
  - Lines 35, 46: `Unnecessary 'const' keyword (unnecessary_const)`

---

## 3. Checklist Hướng Dẫn Codex Sửa Lỗi

- [ ] **Bước 1 (Gradle)**: Cập nhật `05_Development/CareBridgeMobileApp/android/gradle/wrapper/gradle-wrapper.properties` sang Gradle `9.1.0` hoặc đồng bộ phiên bản Android Gradle Plugin.
- [ ] **Bước 2 (Gadgetbridge Build Setup)**: Đảm bảo Gradle config của `Gadgetbridge/app` khai báo đầy đủ các dependencies:
  - `com.google.code.gson:gson`
  - `com.google.errorprone:error_prone_annotations`
  - `androidx.appcompat:appcompat`, `androidx.preference:preference`, `com.google.android.material:material`
  - Thư mục sinh mã AIDL cho LineageOS SDK (`ILineageWeatherManager.aidl`, `IRequestInfoListener.aidl`, `IWeatherProviderServiceClient.aidl`, v.v.).
- [ ] **Bước 3 (Gadgetbridge Java Fixes)**: Hoàn thiện hoặc sinh stub AIDL interfaces cho `lineageos` và fix kiểu kế thừa trong `DragSortController` & `SimpleFloatViewManager`.
- [ ] **Bước 4 (Dart / Flutter Fixes)**: Chạy `dart fix --apply` trong `05_Development/CareBridgeMobileApp` và bổ sung ngoặc `{}` cho các câu lệnh `if` cùng việc thay thế các API deprecated (`initialValue`, `.withValues()`).
