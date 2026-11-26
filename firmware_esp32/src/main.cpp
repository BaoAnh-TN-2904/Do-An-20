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

    // 3. Khởi tạo MQTT Service
    mqttService.setup(Config::MQTT_BROKER, Config::MQTT_PORT, Config::APP_USERNAME);

    // 4. Đăng ký sự kiện: Khi người dùng BẤM NÚT VẬT LÝ tại phòng
    deviceManager.setOnButtonToggledCallback([](Device& dev) {
        Serial.printf("\n[SỰ KIỆN] Nút vật lý nhấn -> '%s' (GPIO %d) = %s\n",
                      dev.getId().c_str(), dev.getRelayPin(), dev.getState() ? "BẬT" : "TẮT");
        deviceManager.saveDevice(dev);
        mqttService.publishState(dev); // Gửi ngay lên App để gạt switch
    });

    // 5. Đăng ký sự kiện: Khi NHẬN LỆNH TỪ APP ANDROID gửi xuống
    mqttService.setCommandCallback([](const IncomingCommand& cmd) {
        if (cmd.action == "provision") {
            // Popup kiểm tra khởi tạo chân từ App
            Serial.printf("[LỆNH PROVISION] Khởi tạo chân '%s' trên GPIO %d\n", cmd.deviceId.c_str(), cmd.targetGpio);
            String failReason = "";
            Device* dev = deviceManager.provisionDevice(cmd.deviceId, cmd.targetGpio, -1, &failReason);
            if (dev != nullptr) {
                mqttService.publishAck(cmd.deviceId, dev->getRelayPin(), "READY", "OK", "Chân GPIO đã được khởi tạo thành công");
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
        } else {
            // Lệnh Bật/Tắt bình thường (switch)
            Device* dev = deviceManager.provisionDevice(cmd.deviceId, cmd.targetGpio);
            if (dev != nullptr) {
                dev->setState(cmd.targetState);
                deviceManager.saveDevice(*dev);
                mqttService.publishState(*dev);
                Serial.printf("[LỆNH SWITCH] '%s' trên GPIO %d -> %s\n",
                              dev->getId().c_str(), dev->getRelayPin(), dev->getState() ? "BẬT" : "TẮT");
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
