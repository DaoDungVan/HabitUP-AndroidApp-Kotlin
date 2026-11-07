# HabitUp — Theo dõi thói quen

Dự án cuối khoá "Lập Trình Android với Kotlin & Jetpack Compose 2026". App theo dõi thói quen hằng ngày: tạo thói quen theo lịch tuỳ chọn (mỗi ngày hoặc một số thứ trong tuần), đánh dấu hoàn thành, xem chuỗi ngày liên tiếp (streak) và nhận nhắc nhở.

## Kiến trúc

```
UI (Compose, Navigation type-safe)
   │  collectAsStateWithLifecycle
ViewModel (StateFlow, Hilt)
   │
Repository (interface) ── HabitRepositoryImpl / AuthRepositoryImpl
   │
Firebase: Auth (Google Sign-In qua Credential Manager) + Firestore (offline persistence mặc định)
   │
users/{uid}/habits/{habitId}          — thói quen
users/{uid}/completions/{habitId_date} — đã hoàn thành ngày nào (id cố định, idempotent)
```

- **MVVM + Hilt**: mọi ViewModel chỉ biết `HabitRepository`/`AuthRepository` (interface) — không biết Firestore tồn tại, nên test được bằng repository giả.
- **Dữ liệu từ xa + offline**: Firestore bật persistence mặc định trên Android — ngắt mạng vẫn đọc/ghi được, đồng bộ lại khi có mạng (đáp ứng yêu cầu "remote + offline" của đề bài).
- **Đăng nhập**: Firebase Auth + Google Sign-In qua Credential Manager (tái dùng đúng pattern đã học ở Lab 2).
- **WorkManager + notification**: `ReminderWorker` (periodic ~1h, HiltWorker) kiểm tra các thói quen chưa hoàn thành hôm nay và đã qua giờ nhắc, hiện notification có nút "Đã làm" (BroadcastReceiver + goAsync).
- **Streak**: logic thuần (`StreakCalculator`, không phụ thuộc Android) — đếm theo đúng lịch riêng của từng thói quen, không phải mọi ngày lịch.

## Vì sao khác với app To-do đã học

| | To-do | HabitUp |
|---|---|---|
| Một mục sau khi xong | Biến mất | Lặp lại ngày hôm sau |
| Dữ liệu | 1 bảng phẳng | 2 bảng quan hệ (Habit + nhiều Completion theo ngày) |
| Logic cốt lõi | Không có | Tính streak theo lịch riêng từng habit |
| Thống kê | Không có | Streak hiện tại/kỷ lục, tỉ lệ hoàn thành, lịch sử 30 ngày |

## ⚠️ BẮT BUỘC trước khi chạy thật

Project hiện dùng `app/google-services.json` **giả** (chỉ để build code). Cần làm:

### 1. Tạo Firebase project
1. [Firebase Console](https://console.firebase.google.com) → tạo project mới.
2. **Add app** → Android → package `com.example.habitup`.
3. **SHA-1 cần đăng ký** (Project settings → Your apps → Add fingerprint) — thêm **CẢ HAI**:
   - Debug: `0E:F4:73:06:1F:48:2C:64:6F:35:F1:7B:6A:80:85:2D:23:39:8C:C1`
   - Release: `24:02:28:DE:51:47:18:D3:DD:B1:D2:44:52:1F:6C:43:AA:71:C8:AF`
4. Tải `google-services.json` thật, ghi đè `app/google-services.json`.

### 2. Bật Google Sign-In
Authentication → Sign-in method → bật **Google** → chọn email hỗ trợ.

### 3. Bật Firestore
Firestore Database → Create database → **Production mode**. Sau đó deploy `firestore.rules` (ở gốc repo) qua tab Rules trên Console (dán đè) hoặc Firebase CLI (`firebase deploy --only firestore:rules`).

### 4. Keystore release (đã có sẵn, không cần tạo lại)
`keystore/habitup-release.jks` + `keystore.properties` đã tạo sẵn cục bộ (gitignore, không có trên GitHub). Nếu clone máy khác, tự tạo lại bằng `keytool -genkeypair` rồi điền `keystore.properties` theo mẫu trong `app/build.gradle.kts`.

## Chạy & kiểm thử

```bash
./gradlew assembleDebug          # build debug
./gradlew testDebugUnitTest      # 10 unit test (5 StreakCalculator, 3 TodayViewModel, 2 SettingsViewModel)
./gradlew connectedDebugAndroidTest  # 2 Compose UI test (cần emulator/thiết bị đang chạy)
./gradlew assembleRelease        # build release đã ký + R8 (cần keystore.properties)
```

Mở project trong Android Studio, chạy trên emulator có Google Play (để đăng nhập Google). Android 13+ sẽ xin quyền `POST_NOTIFICATIONS`.

## Cấu trúc package

```
data/
  model/Habit.kt, Completion.kt
  streak/StreakCalculator.kt      — logic thuần, test riêng
  auth/AuthRepository(Impl).kt
  HabitRepository(Impl).kt
di/FirebaseModule.kt, RepositoryModule.kt
notification/HabitNotifier.kt
work/ReminderWorker.kt, ReminderScheduler.kt
receiver/MarkDoneReceiver.kt
ui/
  navigation/ — Routes, AppNavHost, AuthGateViewModel
  splash/, login/, today/, form/ (Thêm/Sửa dùng chung), detail/, settings/
```

5 màn hình chức năng thật: **Đăng nhập, Hôm nay, Thêm/Sửa thói quen, Chi tiết (streak), Cài đặt** (+ Splash làm cổng chờ).

## Ghi chú kỹ thuật

- **Id completion cố định** `"{habitId}_{date}"`: đánh dấu hai lần trong ngày là ghi đè, không tạo bản ghi trùng; đồng thời giúp Security Rules kiểm tra được `id == habitId + '_' + date`.
- **Không dùng composite index**: mọi truy vấn Firestore chỉ lọc một trường (`archived`, `habitId`, hoặc `date`); sắp xếp/tính streak làm ở client để khỏi cần tạo index thủ công trên Console.
- **Token Firebase tự làm mới** phát lại `currentUser` cùng uid — `AppNavHost` chỉ tự chuyển sang "Hôm nay" khi đang ở Splash/Login, tránh bị bật khỏi màn Chi tiết/Cài đặt đang xem.
- **WorkManager không hẹn giờ chính xác** (periodic tối thiểu 15 phút, dao động) — giờ nhắc của từng habit chỉ là "từ giờ X trở đi", không phải đúng giờ tuyệt đối; cần chính xác phải dùng `AlarmManager` (ngoài phạm vi).
- **Hai `hilt-compiler`**: thiếu `ksp(libs.androidx.hilt.compiler)` thì `ReminderWorker` build được nhưng không bao giờ chạy — lỗi "Could not instantiate ReminderWorker" trong Logcat.

## Sản phẩm nộp

- [ ] Link GitHub repo
- [ ] `app-release.apk` đã ký (R8 bật)
- [ ] Video demo 3–5 phút
- [ ] Slide ≤10 trang
- [ ] README (file này)
