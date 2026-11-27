# Gọn Phòng (Do-An-20)

Ứng dụng Android quản lý vật dụng cá nhân và điều khiển thiết bị phòng thông minh qua **Firebase** và **MQTT**.

---

## 📌 Công Nghệ Sử Dụng

- **Ngôn ngữ & Nền tảng:** Kotlin, Android SDK (Edge-to-Edge UI).
- **Backend & Cơ sở dữ liệu:** Firebase Authentication, Cloud Firestore (Đồng bộ thời gian thực).
- **Giao thức IoT:** MQTT (`broker.hivemq.com`) điều khiển thiết bị từ xa.

---

## 🔑 Tài Khoản Mẫu

| Tài khoản | Username | Password | Quyền (Role) | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `123` | `ADMIN` | Quản lý toàn bộ người dùng, phòng và thiết bị |
| **User** | `user` | `123456` | `USER` | Người dùng cá nhân, tự động đồng bộ dữ liệu mẫu |

---

## 🚀 Tính Năng Chính

- **Quản lý vật dụng:** Thống kê số lượng (Còn dùng / Hỏng), tìm kiếm thông minh, lọc danh mục và thêm/sửa/xóa đồ đạc.
- **Điều khiển thông minh (IoT):** Bật/tắt công tắc thiết bị điện tức thì qua MQTT và đồng bộ trạng thái lên Firebase Firestore.
- **Cảnh báo & Giám sát:** Danh sách cảnh báo đồ dùng hư hỏng, kiểm tra thiết bị đang cấp nguồn và phím tắt nguồn nhanh.
- **Tài khoản & Phân quyền:** Đăng nhập, đăng ký, ẩn/hiện mật khẩu, phân quyền Admin/User và chỉnh sửa thông tin cá nhân.

---

## 🛠️ Cài Đặt & Chạy Ứng Dụng

1. Mở dự án bằng **Android Studio** (hỗ trợ JDK 17+).
2. Đồng bộ Gradle (`Sync Project with Gradle Files`).
3. Chọn thiết bị giả lập (AVD) hoặc điện thoại thật và nhấn **Run** (`Shift + F10`).
