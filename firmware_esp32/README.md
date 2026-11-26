# Firmware ESP32 - Cấu trúc Hướng Đối Tượng (OOP Architecture)

Mã nguồn firmware cho ESP32 trong dự án **Gọn Phòng (Do-An-20)** đã được thiết kế lại hoàn chỉnh theo mô hình **Hướng đối tượng (OOP)**, giúp phân tách rõ ràng trách nhiệm giữa các lớp (Clean Architecture), dễ đọc, dễ mở rộng và bảo trì.

---

## 1. Cấu trúc cây thư mục OOP

```
firmware_esp32/
├── platformio.ini           # Cấu hình PlatformIO (board, thư viện, tốc độ baud)
├── include/                 # Khai báo Header (.h)
│   ├── Config.h             # Hằng số cấu hình: WiFi, MQTT Broker, Username, thời gian
│   ├── Device.h             # Lớp Device: Thực thể thiết bị (Relay, nút bấm, trạng thái, debounce)
│   ├── DeviceManager.h      # Lớp DeviceManager: Quản lý danh sách thiết bị, NVS Flash, tạo chân động
│   ├── NetworkManager.h     # Lớp NetworkManager: Quản lý kết nối & tự động phục hồi WiFi
│   └── MqttService.h        # Lớp MqttService: Đóng gói PubSubClient, giao tiếp MQTT & JSON
├── src/                     # Cài đặt mã nguồn (.cpp)
│   ├── Device.cpp           # Logic chi tiết điều khiển phần cứng của từng Device
│   ├── DeviceManager.cpp    # Quản lý bộ nhớ Flash NVS, ánh xạ chân động & giám sát nút
│   ├── NetworkManager.cpp   # Xử lý kết nối WiFi non-blocking
│   ├── MqttService.cpp      # Xử lý giải mã lệnh JSON từ App, gửi ACK & State
│   └── main.cpp             # Điểm khởi chạy (Orchestrator kết nối các controller với nhau)
└── README.md                # Tài liệu hướng dẫn
```

---

## 2. Chi tiết các lớp (Classes & Responsibilities)

| Lớp (Class) | Trách nhiệm chính |
| :--- | :--- |
| **`Config`** | Chứa namespace các hằng số WiFi SSID, Pass, MQTT Broker, Port, Username. |
| **`Device`** | Đại diện cho 1 thiết bị thực tế: quản lý chân Relay, chân Nút bấm, trạng thái Bật/Tắt, xử lý chống dội phím (Debounce 50ms). |
| **`DeviceManager`** | Quản lý danh sách các thiết bị (`std::vector<Device>`), lưu cấu hình vào Flash NVS (`Preferences`), tự động cấp phát chân GPIO khi App thêm vật dụng mới (`provisionDevice`). |
| **`NetworkManager`** | Quản lý kết nối WiFi, tự động kết nối lại khi mất mạng (non-blocking, không làm treo hệ thống). |
| **`MqttService`** | Đóng gói kết nối MQTT, giải mã gói tin JSON từ App, phát thông báo ACK khi khởi tạo chân và publish trạng thái khi thay đổi. |
| **`main.cpp`** | Kết nối sự kiện giữa `DeviceManager` và `MqttService` bằng Callbacks (Lambda). Cực kỳ ngắn gọn và dễ hiểu (~60 dòng). |

---

## 3. Sơ đồ nối dây phần cứng (Wiring Diagram)

| Thiết bị | Chân trên Module | Chân nối trên ESP32 | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Relay 1 (Đèn bàn)** | IN1 / Signal | **GPIO 18** | Đèn mặc định |
| **Nút bấm 1 (Đèn bàn)** | Chân 1 (Chân kia nối GND) | **GPIO 4** | Nút bấm thủ công tại phòng |
| **Relay 2 (Quạt bàn)** | IN2 / Signal | **GPIO 23** | Quạt mặc định |
| **Nút bấm 2 (Quạt bàn)** | Chân 1 (Chân kia nối GND) | **GPIO 5** | Nút bấm thủ công tại phòng |
| **Vật dụng mới thêm** | INx / Signal | **GPIO bất kỳ (ví dụ 19, 21...)** | Khởi tạo động từ App Android |
| **Nút bấm cho vật dụng mới** | Chân 1 (Chân kia nối GND) | **GPIO 16, 17, 13...** | Ghép cặp tự động hoặc bấm nút BOOT (GPIO 0) |

---

## 4. Cách nạp code và sử dụng

1. Mở thư mục `firmware_esp32/` bằng **VS Code** (đã cài extension PlatformIO).
2. Mở file `include/Config.h`, chỉnh sửa:
   - `WIFI_SSID`: Tên WiFi của bạn.
   - `WIFI_PASSWORD`: Mật khẩu WiFi.
   - `APP_USERNAME`: Tên đăng nhập trên App (mặc định là `"sample_user"`).
3. Cắm cáp USB nối ESP32 với máy tính.
4. Bấm biểu tượng mũi tên (`→`) ở thanh trạng thái dưới cùng để Upload code.
5. Bấm biểu tượng phích cắm để mở Serial Monitor xem log ở tốc độ `115200` baud.
