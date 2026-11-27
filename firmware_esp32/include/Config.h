#pragma once

// =============================================================================
// CẤU HÌNH HỆ THỐNG ESP32 (DO-AN-20)
// =============================================================================

namespace Config {
    // 1. Cấu hình WiFi
    constexpr const char* WIFI_SSID       = "ESP32_NCKH";
    constexpr const char* WIFI_PASSWORD   = "giaky123";

    // 2. Cấu hình MQTT Broker
    constexpr const char* MQTT_BROKER     = "broker.hivemq.com";
    constexpr int         MQTT_PORT       = 1883;

    // 3. Topic Prefix mặc định (ESP32 tự lấy 6 ký tự cuối MAC làm định danh)
    //    Ví dụ: MAC D4:E9:F4:E2:F3:98 -> Mã: E2F398 -> Topic: doan20/E2F398/device/...
    //    Không cần sửa code khi đổi tài khoản App!
    constexpr const char* TOPIC_ROOT      = "doan20";

    // 4. Cấu hình Relay / LED
    // true: Module Relay kích mức LOW (Active-LOW)
    // false: Module Relay kích mức HIGH (Active-HIGH, hoặc nối trực tiếp LED)
    constexpr bool RELAY_ACTIVE_LOW       = false;

    // 5. Cấu hình thời gian
    constexpr unsigned long MQTT_RETRY_INTERVAL = 5000; // ms
    constexpr unsigned long BUTTON_DEBOUNCE_MS  = 50;   // ms
}
