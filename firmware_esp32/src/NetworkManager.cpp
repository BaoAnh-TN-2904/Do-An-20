#include "NetworkManager.h"

NetworkManager::NetworkManager()
    : m_ssid(nullptr),
      m_password(nullptr),
      m_lastReconnectAttempt(0) {}

void NetworkManager::setup(const char* ssid, const char* password) {
    m_ssid = ssid;
    m_password = password;

    Serial.printf("\n[NetworkManager] Đang kết nối WiFi: %s", m_ssid);
    WiFi.mode(WIFI_STA);
    WiFi.begin(m_ssid, m_password);

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 30) {
        delay(300);
        Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n[NetworkManager] WiFi đã kết nối!");
        Serial.printf("[NetworkManager] IP ESP32: %s\n", getIpAddress().c_str());
    } else {
        Serial.println("\n[NetworkManager] Chưa thể kết nối WiFi, sẽ tự động thử lại...");
    }
}

void NetworkManager::loop() {
    if (WiFi.status() != WL_CONNECTED) {
        unsigned long now = millis();
        if (now - m_lastReconnectAttempt > 10000) {
            m_lastReconnectAttempt = now;
            Serial.println("[NetworkManager] Đang thử kết nối lại WiFi...");
            WiFi.disconnect();
            WiFi.reconnect();
        }
    }
}

bool NetworkManager::isConnected() const {
    return WiFi.status() == WL_CONNECTED;
}

String NetworkManager::getIpAddress() const {
    return WiFi.localIP().toString();
}
