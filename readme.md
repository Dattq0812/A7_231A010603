# Lab A7 - Xin quyền truy cập (Runtime Permissions) 🛡️

Dự án này là một ứng dụng Android minh họa cách quản lý và xin quyền truy cập các tài nguyên nhạy cảm trên thiết bị theo chuẩn Runtime Permissions được giới thiệu từ Android 6.0, kết hợp với các API mới nhất của Android 13+.

## 🌟 Các tính năng cốt lõi

Ứng dụng thực hiện việc xin quyền theo **Mẫu chuẩn 3 nhánh** được Google khuyến nghị:
1. **Đã cấp quyền:** Sử dụng tài nguyên ngay lập tức.
2. **Từ chối lần 1 (Cần giải thích):** Hiển thị hộp thoại (AlertDialog) giải thích lý do app cần quyền này trước khi hỏi lại.
3. **Từ chối vĩnh viễn (Bị chặn):** Hướng dẫn và điều hướng người dùng mở màn hình Cài đặt ứng dụng để cấp quyền thủ công.

### Chức năng chính:
* **Chụp ảnh xem trước (CAMERA):** Xin quyền `CAMERA`, mở giao diện chụp ảnh mini bằng `ActivityResultContracts.TakePicturePreview()` và hiển thị ảnh Bitmap thu nhỏ lên màn hình.
* **Gửi thông báo (POST_NOTIFICATIONS):** Tạo `NotificationChannel` và xin quyền gửi thông báo cục bộ. Có xử lý tính tương thích ngược (tự động bỏ qua việc xin quyền đối với Android 12 trở xuống).

## 🚀 Các tính năng nâng cao (NC1 & NC2)

Ứng dụng đã được tích hợp thêm hai bài nâng cao để khai thác tối đa sức mạnh của Activity Result API và Content Provider:

### NC1: Xin nhiều quyền cùng lúc
* Sử dụng `ActivityResultContracts.RequestMultiplePermissions()` để gộp yêu cầu cấp quyền CAMERA và POST_NOTIFICATIONS vào chung một lần gọi.
* Hệ thống sẽ trả về một `Map<String, Boolean>`. Ứng dụng tiến hành bóc tách kết quả để đánh giá trạng thái và hiển thị thông báo (`Toast`) độc lập cho từng quyền một cách linh hoạt.

### NC2: Đọc danh bạ thiết bị (READ_CONTACTS)
* Bổ sung luồng xin quyền `READ_CONTACTS`.
* Sử dụng `ContentResolver` và `ContactsContract` để truy vấn cơ sở dữ liệu danh bạ nội bộ của thiết bị.
* Lọc, trích xuất tên (`DISPLAY_NAME`) và số điện thoại (`NUMBER`) của 5 liên hệ đầu tiên.
* Dữ liệu trả về được định dạng thành chuỗi và hiển thị trực quan thông qua một `AlertDialog`.