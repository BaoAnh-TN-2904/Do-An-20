#pragma once

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiClient.h>
#include <ArduinoJson.h>
#include <functional>
#include "Device.h"

/**
 * Cấu trúc thông điệp MQTT nhận được từ App
 */
struct IncomingCommand {
    String deviceId;
    String action;    // "switch" hoặc "provision"
    bool targetState;
    int targetGpio;
};

/**
 * Lớp dịch vụ MQTT quản lý giao tiếp mạng hai chiều với HiveMQ
 */
class MqttService {
public:
    MqttService();

    void setup(const char* broker, int port, const char* topicRoot = "doan20");
    void loop();

    bool isConnected();
    String getDeviceCode() const { return m_deviceCode; }
    void publishState(const Device& dev);
    void clearRetainedState(const String& id);
    void publishAck(const String& id, int gpio, const char* status = "READY", const char* reason = "OK", const char* message = "", int state = 0);

    // Đăng ký callback xử lý lệnh từ App
    void setCommandCallback(const std::function<void(const IncomingCommand&)>& callback) {
        m_onCommandReceived = callback;
    }

    // Đăng ký callback khi MQTT vừa kết nối thành công
    void setOnConnectedCallback(const std::function<void()>& callback) {
        m_onConnected = callback;
    }

private:
    void ensureDeviceCode();
    void connectNonBlocking();
    void handleMqttPayload(char* topic, byte* payload, unsigned int length);

    WiFiClient m_espClient;
    PubSubClient m_mqttClient;

    const char* m_broker;
    int m_port;
    String m_topicRoot;
    String m_deviceCode;
    unsigned long m_lastRetryTime;

    std::function<void(const IncomingCommand&)> m_onCommandReceived;
    std::function<void()> m_onConnected;
};
