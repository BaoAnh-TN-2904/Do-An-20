#pragma once

#include <Arduino.h>
#include <WiFi.h>

/**
 * Lớp quản lý kết nối WiFi cho ESP32
 */
class NetworkManager {
public:
    NetworkManager();

    void setup(const char* ssid, const char* password);
    void loop();

    bool isConnected() const;
    String getIpAddress() const;

private:
    const char* m_ssid;
    const char* m_password;
    unsigned long m_lastReconnectAttempt;
};
