#include "NetworkManager.h"

NetworkManager::NetworkManager()
    : m_ssid(nullptr),
      m_password(nullptr),
      m_lastReconnectAttempt(0),
      m_wasConnected(false) {}

void NetworkManager::setup(const char* ssid, const char* password) {
    m_ssid = ssid;
    m_password = password;

    Serial.printf("\n[NetworkManager] Đang kết nối WiFi: %s", m_ssid);
    WiFi.persistent(false);
    WiFi.mode(WIFI_STA);
    WiFi.setSleep(false);         // Tắt Modem Sleep để tránh rớt gói MQTT từ App
    WiFi.setAutoReconnect(true);  // Tự động nối lại ở tầng WiFi driver
    WiFi.begin(m_ssid, m_password);

    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 30) {
        delay(300);
        Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        m_wasConnected = true;
        Serial.println("\n[NetworkManager] WiFi đã kết nối!");
        Serial.printf("[NetworkManager] IP ESP32: %s | RSSI: %d dBm\n", getIpAddress().c_str(), WiFi.RSSI());
    } else {
        m_wasConnected = false;
        m_lastReconnectAttempt = millis();
        Serial.println("\n[NetworkManager] Chưa thể kết nối WiFi, sẽ tự động thử lại...");
    }
}

void NetworkManager::loop() {
    bool connected = (WiFi.status() == WL_CONNECTED);

    if (connected) {
        if (!m_wasConnected) {
            m_wasConnected = true;
            Serial.printf("\n[NetworkManager] WiFi đã kết nối lại! IP: %s | RSSI: %d dBm\n",
                          getIpAddress().c_str(), WiFi.RSSI());
        }
        return;
    }

    if (m_wasConnected) {
        m_wasConnected = false;
        m_lastReconnectAttempt = millis();
        Serial.println("\n[NetworkManager] Mất kết nối WiFi! Đang chờ tự động kết nối lại...");
        return;
    }

    unsigned long now = millis();
    if (now - m_lastReconnectAttempt >= 15000) {
        m_lastReconnectAttempt = now;
        Serial.println("[NetworkManager] Thử kết nối lại WiFi bằng WiFi.begin()...");
        WiFi.disconnect(false, false);
        WiFi.begin(m_ssid, m_password);
    }
}

bool NetworkManager::isConnected() const {
    return WiFi.status() == WL_CONNECTED;
}

String NetworkManager::getIpAddress() const {
    return WiFi.localIP().toString();
}
