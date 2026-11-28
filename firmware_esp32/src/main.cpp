#include <Arduino.h>
#include "Config.h"
#include "DeviceManager.h"
#include "NetworkManager.h"
#include "MqttService.h"

// =============================================================================
// KHỞI TẠO CÁC ĐỐI TƯỢNG (OOP CONTROLLERS)
// =============================================================================
DeviceManager   deviceManager;
NetworkManager  networkManager;
MqttService     mqttService;

void setup() {
    Serial.begin(115200);
    delay(500);

    Serial.println("\n=======================================================");
    Serial.println("  HỆ THỐNG GỌN PHÒNG - ESP32 IOT OOP ARCHITECTURE      ");
    Serial.println("=======================================================");

    // 1. Khởi tạo danh sách thiết bị và chân phần cứng
    deviceManager.init();

    // 2. Kết nối WiFi
    networkManager.setup(Config::WIFI_SSID, Config::WIFI_PASSWORD);

    // 3. Khởi tạo MQTT Service (tự động dùng mã MAC/Chip ID của ESP32)
    mqttService.setup(Config::MQTT_BROKER, Config::MQTT_PORT, Config::TOPIC_ROOT);

    // 4. Đăng ký sự kiện: Sau khi MQTT kết nối & nhận hết gói chờ -> chỉ gửi trạng thái các thiết bị cần đồng bộ
    mqttService.setOnConnectedCallback([](bool isFirstBoot) {
        auto& devices = const_cast<std::vector<Device>&>(deviceManager.getDevices());
        for (auto& dev : devices) {
            if (isFirstBoot || dev.isPendingSync()) {
                mqttService.publishState(dev);
                dev.setPendingSync(false);
            }
        }
    });

    // 5. Đăng ký sự kiện: Khi người dùng BẤM NÚT VẬT LÝ tại phòng
    deviceManager.setOnButtonToggledCallback([](Device& dev) {
        Serial.printf("\n[SỰ KIỆN] Nút vật lý nhấn -> '%s' (GPIO %d) = %s\n",
                      dev.getId().c_str(), dev.getRelayPin(), dev.getState() ? "BẬT" : "TẮT");
        deviceManager.saveAllDevices();
        if (mqttService.isConnected()) {
            dev.setPendingSync(false);
            mqttService.publishState(dev); // Gửi ngay lên App để gạt switch
        } else {
            dev.setPendingSync(true); // Đánh dấu để gửi lên App ngay khi có mạng lại
        }
    });

    // 6. Đăng ký sự kiện: Khi NHẬN LỆNH TỪ APP ANDROID gửi xuống
    mqttService.setCommandCallback([](const IncomingCommand& cmd) {
        if (cmd.action == "provision") {
            // Popup kiểm tra khởi tạo chân từ App
            Serial.printf("[LỆNH PROVISION] Khởi tạo chân '%s' trên GPIO %d\n", cmd.deviceId.c_str(), cmd.targetGpio);
            String failReason = "";
            Device* dev = deviceManager.provisionDevice(cmd.deviceId, cmd.targetGpio, -1, &failReason);
            if (dev != nullptr) {
                mqttService.publishAck(
                    cmd.deviceId,
                    dev->getRelayPin(),
                    "READY",
                    "OK",
                    "Chân GPIO đã được khởi tạo thành công",
                    dev->getState() ? 1 : 0
                );
                mqttService.publishState(*dev);
            } else {
                const char* msg = "Không thể khởi tạo chân GPIO";
                if (failReason == "GPIO_FULL") {
                    msg = "ESP32 đã hết chân GPIO vật lý khả dụng!";
                } else if (failReason == "PIN_OCCUPIED") {
                    msg = "Chân GPIO này đã được sử dụng bởi vật dụng khác!";
                } else if (failReason == "INVALID_PIN") {
                    msg = "Chân GPIO không hợp lệ để làm cổng xuất Relay!";
                }
                mqttService.publishAck(cmd.deviceId, cmd.targetGpio, "FAILED", failReason.c_str(), msg);
            }
        } else if (cmd.action == "remove") {
            Serial.printf("[LỆNH REMOVE] Xóa thiết bị '%s'\n", cmd.deviceId.c_str());
            deviceManager.removeDevice(cmd.deviceId);
            mqttService.clearRetainedState(cmd.deviceId);
            mqttService.publishAck(cmd.deviceId, cmd.targetGpio, "REMOVED", "OK", "Đã xóa thiết bị và giải phóng chân GPIO");
        } else {
            // Lệnh Bật/Tắt bình thường (switch)
            Device* dev = deviceManager.provisionDevice(cmd.deviceId, cmd.targetGpio);
            if (dev != nullptr) {
                dev->setState(cmd.targetState);
                dev->setPendingSync(false);
                deviceManager.saveAllDevices();
                mqttService.publishState(*dev);
                Serial.printf("[LỆNH SWITCH] '%s' trên GPIO %d -> %s\n",
                              dev->getId().c_str(), dev->getRelayPin(), dev->getState() ? "BẬT" : "TẮT");
            } else {
                Serial.printf("[LỆNH SWITCH] Bỏ qua '%s' (GPIO %d không hợp lệ hoặc bị chiếm)\n",
                              cmd.deviceId.c_str(), cmd.targetGpio);
            }
        }
    });
}

void loop() {
    // 1. Duy trì kết nối mạng WiFi
    networkManager.loop();

    // 2. Duy trì kết nối và nhận/gửi MQTT
    mqttService.loop();

    // 3. Giám sát tất cả các nút nhấn vật lý
    deviceManager.loop();
}
