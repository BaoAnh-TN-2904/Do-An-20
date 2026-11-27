#pragma once

// =============================================================================
// CẤU HÌNH HỆ THỐNG ESP32 (DO-AN-20)
// =============================================================================

namespace Config {
    // 1. Cấu hình WiFi
    constexpr const char* WIFI_SSID       = "TEN_WIFI_CUA_BAN";
    constexpr const char* WIFI_PASSWORD   = "MAT_KHAU_WIFI";

    // 2. Cấu hình MQTT Broker
    constexpr const char* MQTT_BROKER     = "broker.hivemq.com";
    constexpr int         MQTT_PORT       = 1883;

    // 3. Username tài khoản người dùng trên App
    constexpr const char* APP_USERNAME    = "sample_user";

    // 4. Cấu hình Relay
    // true: Module Relay kích mức LOW (Active-LOW, phổ biến nhất)
    // false: Module Relay kích mức HIGH (Active-HIGH, hoặc nối trực tiếp LED)
    constexpr bool RELAY_ACTIVE_LOW       = true;

    // 5. Cấu hình thời gian
    constexpr unsigned long MQTT_RETRY_INTERVAL = 5000; // ms
    constexpr unsigned long BUTTON_DEBOUNCE_MS  = 50;   // ms
}
