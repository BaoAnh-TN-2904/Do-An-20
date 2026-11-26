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

    void setup(const char* broker, int port, const char* username);
    void loop();

    bool isConnected();
    void publishState(const Device& dev);
    void publishAck(const String& id, int gpio, const char* status = "READY", const char* reason = "OK", const char* message = "");

    // Đăng ký callback xử lý lệnh từ App
    void setCommandCallback(const std::function<void(const IncomingCommand&)>& callback) {
        m_onCommandReceived = callback;
    }

private:
    void connectNonBlocking();
    void handleMqttPayload(char* topic, byte* payload, unsigned int length);

    WiFiClient m_espClient;
    PubSubClient m_mqttClient;

    const char* m_broker;
    int m_port;
    String m_username;
    unsigned long m_lastRetryTime;

    std::function<void(const IncomingCommand&)> m_onCommandReceived;
};
