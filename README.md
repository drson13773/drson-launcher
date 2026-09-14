# Dr Sơn Launcher

Launcher Android cho màn hình ô tô, cấu trúc tương tự CarWebGuru (khai báo HOME/CAR_DOCK launcher,
chạy toàn màn hình ngang), đã tích hợp sẵn wallpaper + bộ icon "Dr Sơn" làm giao diện mặc định.

## Đã có trong bản này
- **Đăng ký làm launcher mặc định** (`HOME`, `CAR_DOCK`) - cài xong bấm nút Home sẽ hỏi chọn app này.
- **Đồng hồ tiếng Việt**: giờ:phút + ngày tháng hiển thị bằng tiếng Việt có dấu (vd:
  "Thứ Hai, 14 tháng 9") ở status bar, không phụ thuộc ngôn ngữ hệ thống của đầu màn hình.
- **Wallpaper**: cố định là **"Dashboard động" (Driving thật)** - xe + đường phối cảnh + skyline
  chuyển động - không còn nút đổi hình nền, đây là giao diện chính thức duy nhất của launcher.
- **Icon tuỳ chỉnh tự động**: các app hệ thống phổ biến tự động hiển thị bằng icon trong bộ
  "Dr Sơn" nếu packageName khớp (xem `data/IconMapping.kt`). App không khớp vẫn hiện icon thật.
- **Đồng hồ giờ + đồng hồ tốc độ luôn hiển thị trên hình nền** (không cần vuốt sang trang nào cả):
  vẽ thẳng vào `ui/DrivingRoadBackground.kt` nên hiện thường trực phía sau Home Screen/Dock, luôn
  cập nhật thời gian thật + tốc độ thật qua GPS (`driving/`). Đã bỏ widget la bàn. 3 cột bên trái
  của lưới Home Screen được chừa trống để icon không đè lên cụm đồng hồ này (xem
  `HOME_GRID_RESERVED_COLUMNS` trong `ui/HomeScreen.kt` nếu muốn đổi số cột chừa).
- **Vuốt từ mép trên cùng màn hình xuống**: bên trái mở **Trung tâm thông báo**, bên phải mở
  **Control Center** - y hệt iPadOS, chạm vào bất kỳ đâu dọc thanh trạng thái trên cùng đều được,
  không cần trúng đúng chữ giờ hay icon.
- **Trung tâm thông báo**: thông báo hệ thống thật qua `NotificationListenerService`
  (`notifications/`), cần cấp quyền "Notification access" 1 lần.
- **Control Center**: Máy bay, Wi-Fi, Data di động, Bluetooth, độ sáng, âm lượng - có logo Dr Sơn
  ở cuối panel. Bluetooth bấm là bật/tắt ngay (chỉ xin quyền hệ thống 1 lần qua hộp thoại). Máy
  bay/Wi-Fi/Data cũng bật/tắt ngay **nếu đã cấp quyền ADB một lần** (xem mục "Bật/tắt Wi-Fi, máy
  bay, Data ngay lập tức" bên dưới) - nếu chưa cấp, app tự mở đúng màn hình Cài đặt tương ứng để
  bật/tắt trong 1 chạm như phiên bản trước.
- **App Switcher** (vuốt lên từ Dock): lịch sử app đã mở qua launcher này, kèm nút mở Recents
  thật của hệ thống qua Accessibility Service (`accessibility/`).
- Toàn bộ 4 phần trên đã đổi màu chủ đạo từ xanh dương sang **vàng đồng trên nền đen**
  (`Color(0xFFC99E5C)` / `0xFFE6C178`), đồng bộ với wallpaper và bộ icon.
- **App Điện thoại riêng** (`ui/dialer/`, `telephony/`): thay thế hoàn toàn bàn phím quay số hệ
  thống bằng giao diện vàng-đen tự vẽ, dùng đúng API `Telecom`/`InCallService` chuẩn của Android
  (không phải giả lập) - có bàn phím quay số, danh bạ, lịch sử cuộc gọi, màn hình đang gọi (nghe/
  từ chối/tắt tiếng/loa ngoài/kết thúc). Hoạt động với CẢ cuộc gọi qua SIM của đầu màn hình LẪN
  cuộc gọi Bluetooth-relay từ điện thoại (đã xác nhận đầu máy của bạn tích hợp đúng chuẩn Telecom).
  Mở app → chạm banner vàng trên cùng để đặt làm "Ứng dụng Điện thoại mặc định" (chỉ cần làm 1 lần).
  App này dùng CHUNG packageName với chính launcher (chỉ khác activity), nên mọi nơi mở app trong
  launcher (Dock, Home, App Switcher, App Drawer) đều mở TRỰC TIẾP đúng activity đã phân giải lúc
  quét danh sách app (`AppItem.activityClassName`, xem `HomeViewModel.launchApp`) thay vì chỉ dựa
  vào packageName - tránh bị mở nhầm về màn hình Home thay vì bàn phím quay số.
- **Lưới vị trí cố định phủ toàn màn hình, đặt được cả app LẪN widget thật + Dock tuỳ chỉnh**:
  Home Screen là lưới 32 ô (8 cột x 4 hàng), tự giãn đều để lấp kín toàn bộ phần nền đang trống
  (không cuộn, không lệ thuộc độ phân giải màn hình xe). Ô trống hiện dấu "+", chạm vào sẽ hỏi
  muốn thêm **"Ứng dụng"** hay **"Widget"**:
  - Chọn **Ứng dụng** → mở danh sách app như trước, chạm 1 app để gán vào ô.
  - Chọn **Widget** → mở danh sách mọi widget mà các app đã cài trên máy cung cấp (đọc qua
    `AppWidgetManager` chuẩn của Android, xem `widget/WidgetHostController.kt`) - chạm 1 widget để
    thêm thật vào ô đó (ví dụ đồng hồ, thời tiết... của app bất kỳ có hỗ trợ). Lần đầu thêm 1
    widget mới có thể hiện 1 hộp thoại hệ thống hỏi "Cho phép ứng dụng thêm widget này?" - đây là
    cơ chế bảo mật chuẩn của Android cho MỌI launcher (Nova, Apex...), không phải lỗi. Mỗi ô chỉ
    chứa đúng 1 widget cỡ bằng đúng ô đó (chưa hỗ trợ kéo giãn 1 widget qua nhiều ô).
  - Ô đã có app: chạm để mở app, **giữ (long-press) để gỡ khỏi ô**.
  - Ô đã có widget: chạm nút **"×"** nhỏ ở góc ô để gỡ (không dùng long-press vì thao tác chạm bên
    trong widget - cuộn, bấm nút... - thường "nuốt" mất long-press).
  Dock có ô cố định, chỉ hiện icon (không hiện tên app, không hỗ trợ widget) cho gọn. Vị trí được
  lưu lại qua `data/HomeLayoutRepository.kt`, không mất khi tắt app. Muốn đổi số cột/hàng, sửa
  `HOME_GRID_COLUMNS` / `HOME_GRID_ROWS` trong file đó. **3 cột đầu tiên bên trái luôn để trống**
  (không gán được app/widget) để nhường chỗ cho cụm đồng hồ giờ/tốc độ luôn hiển thị - đổi số cột
  chừa qua `HOME_GRID_RESERVED_COLUMNS` trong `ui/HomeScreen.kt`.
- **Dock cài sẵn app Điện thoại**: ngay lần đầu mở app (chưa gán gì), ô đầu tiên của Dock tự điền
  sẵn app "Điện thoại" riêng của launcher này (xem mục "App Điện thoại riêng" bên dưới) - đỡ phải
  gán tay. Bấm nút icon Dr Sơn cạnh đó để mở toàn bộ danh sách ứng dụng đã cài
  (`ui/AppDrawerOverlay.kt`) và gán thêm app khác vào các ô trống còn lại của Dock.
- **Thanh điều khiển nhạc thật** (`media/MediaControlRepository.kt`, `ui/NowPlayingBar.kt`): đọc
  bài đang phát từ BẤT KỲ app nhạc nào (**Zing MP3**, Spotify, YouTube Music...) qua
  `MediaSessionManager` chuẩn của Android - không cần SDK riêng của Zing MP3. Nếu nhiều app nhạc
  cùng giữ phiên phát (session) một lúc, tự động ưu tiên hiện đúng app ĐANG PHÁT thay vì app đứng
  im. Có nút Trước/Phát-Tạm dừng/Tiếp. Dùng chung quyền "Notification access" đã xin cho Trung tâm
  thông báo (bắt buộc của nền tảng, không xin thêm quyền nào khác).

## Cách build ra file .apk
**Cách 1 - Android Studio (cần cài Android Studio trên máy tính):**
```bash
cd drson-launcher
./gradlew assembleDebug
```
Hoặc mở bằng Android Studio → Build > Generate Signed Bundle/APK.

**Cách 2 - Để GitHub tự build giúp bạn (không cần cài gì, chỉ cần tài khoản GitHub miễn phí):**
1. Tạo 1 repository mới (riêng tư hoặc công khai đều được) trên github.com.
2. Đẩy (push) toàn bộ thư mục `drson-launcher` này lên repo đó.
3. Vào tab **Actions** của repo → workflow "Build APK" tự chạy (đã cấu hình sẵn tại
   `.github/workflows/build-apk.yml`) → đợi ~3-5 phút.
4. Vào lại lần chạy đó, kéo xuống mục **Artifacts** → tải file `drson-launcher-debug-apk.zip` →
   giải nén ra sẽ có file `app-debug.apk` → copy vào USB cài lên đầu màn hình như bình thường.

(Bản build qua cách 2 là bản **debug** - cài và chạy bình thường trên đầu màn hình, chỉ khác bản
release ở việc chưa ký bằng khoá phát hành chính thức, không ảnh hưởng gì khi dùng cho cá nhân.)

## Cách cài lên đầu màn hình ô tô
1. Copy file `.apk` (tại `app/release/app-release.apk` sau khi build) vào USB.
2. Cài như app thường (bật "Cài đặt từ nguồn không xác định" nếu bị chặn).
3. Bấm nút Home → chọn "Dr Sơn Launcher" → chọn **Always/Luôn luôn**.

## Icon app của chính launcher này
`icon_brand.png` (bộ Dr Sơn) đã được dùng làm icon riêng của app trong `mipmap-anydpi-v26/ic_launcher.xml`.
Đây là bản adaptive icon tối giản (chỉ 1 mật độ) - nếu muốn icon đẹp ở mọi độ phân giải màn hình,
mở Android Studio → New > Image Asset, chọn lại `icon_brand.png` làm nguồn để Studio tự sinh đủ
các mipmap-hdpi/xhdpi/xxhdpi.

## Việc cần làm thêm (nếu muốn hoàn thiện hơn)
- **Quyền runtime** (`WRITE_SETTINGS`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, Notification
  access, Accessibility) cần người dùng cấp thủ công lần đầu - app tự đưa tới đúng màn hình
  Settings khi thiếu quyền, nhưng chưa có màn hình "chào mừng" xin quyền hàng loạt lúc mới cài.
- **Widget chỉ có 1 kích cỡ** (đúng bằng 1 ô lưới) - chưa hỗ trợ kéo giãn 1 widget chiếm nhiều ô
  như Nova Launcher, nên vài widget lớn (lịch, thời tiết chi tiết...) có thể hiện hơi chật.

## Bật/tắt Wi-Fi, máy bay, Data ngay lập tức (không cần mở màn hình Cài đặt)
Từ Android 10 trở lên, Google **chặn cứng** mọi app thường (kể cả launcher) tự bật/tắt trực tiếp
Wi-Fi, chế độ máy bay và Data di động - đây là giới hạn của hệ điều hành, không phải do app viết
thiếu. Cách duy nhất để làm được là cấp cho app 3 quyền hệ thống dưới đây **qua lệnh ADB, một lần
duy nhất** sau khi cài (không có màn hình Cài đặt nào cấp tay được các quyền này):

```bash
adb shell pm grant com.drson.launcher android.permission.NETWORK_SETTINGS
adb shell pm grant com.drson.launcher android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant com.drson.launcher android.permission.MODIFY_PHONE_STATE
```

Cách bật gỡ lỗi USB + chạy các lệnh trên (làm 1 lần duy nhất, y hệt cách nhiều app tự động hoá như
Tasker/MacroDroid vẫn dùng để bật/tắt máy bay hộ người dùng):
1. Vào **Cài đặt > Giới thiệu** trên đầu màn hình, bấm liên tục vào "Số bản dựng" (Build number)
   7 lần để mở khoá **Tuỳ chọn nhà phát triển**.
2. Vào **Cài đặt > Tuỳ chọn nhà phát triển**, bật **Gỡ lỗi USB (USB debugging)**.
3. Cắm dây USB nối đầu màn hình với máy tính, cài `adb` (đi kèm Android SDK Platform-Tools).
4. Chạy `adb devices` để xác nhận đầu màn hình hiện lên, đồng ý hộp thoại "Cho phép gỡ lỗi USB"
   trên màn hình xe nếu có.
5. Chạy 3 lệnh `adb shell pm grant ...` ở trên.
6. Mở lại Control Center trong app - Máy bay/Wi-Fi/Data giờ bật/tắt ngay khi chạm, không mở màn
   hình Cài đặt nữa.

**Lưu ý:**
- `MODIFY_PHONE_STATE` (bật/tắt Data di động) tuỳ hãng chip/firmware đầu màn hình có cho phép hay
  không - nếu cấp quyền rồi mà bấm vẫn mở màn hình Cài đặt Data thì đầu màn hình của bạn không hỗ
  trợ, không phải do thiếu bước nào.
- Nếu gỡ cài đặt rồi cài lại app, cần chạy lại 3 lệnh `adb shell pm grant` ở trên (quyền bị Android
  thu hồi khi gỡ app).
- Không cấp quyền cũng không sao - các nút vẫn hoạt động bình thường, chỉ là sẽ mở màn hình Cài
  đặt tương ứng thay vì bật/tắt ngay tại chỗ.
- Riêng Bluetooth **không cần** làm bước ADB nào - chỉ cần đồng ý hộp thoại xin quyền hệ thống hiện
  ra ở lần chạm đầu tiên là bật/tắt ngay từ lần sau.

## Thêm icon cho app khác
Mở `data/IconMapping.kt`, thêm dòng `"package.name.cua.app" to R.drawable.ten_icon`, và bỏ file
icon PNG tương ứng vào `res/drawable/`. Nói mình biết packageName của app đó, mình vẽ thêm icon
đúng phong cách rồi thêm dòng ánh xạ giúp bạn.
