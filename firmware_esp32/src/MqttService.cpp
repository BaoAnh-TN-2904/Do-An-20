#include "MqttService.h"
#include <WiFi.h>

MqttService::MqttService()
    : m_mqttClient(m_espClient),
      m_broker(nullptr),
      m_port(1883),
      m_lastRetryTime(0) {}

void MqttService::setup(const char* broker, int port, const char* username) {
    m_broker = broker;
    m_port = port;
    m_username = String(username);

    m_mqttClient.setServer(m_broker, m_port);
    m_mqttClient.setBufferSize(512);

    // Dùng lambda để chuyển tiếp callback vào phương thức của đối tượng
    m_mqttClient.setCallback([this](char* topic, byte* payload, unsigned int length) {
        this->handleMqttPayload(topic, payload, length);
    });
}

bool MqttService::isConnected() {
    return m_mqttClient.connected();
}

void MqttService::connectNonBlocking() {
    if (WiFi.status() != WL_CONNECTED) return;
    if (m_mqttClient.connected()) return;

    unsigned long now = millis();
    if (now - m_lastRetryTime < 5000) return;
    m_lastRetryTime = now;

    String clientId = "ESP32_DoAn20_" + String((uint32_t)ESP.getEfuseMac(), HEX);
    Serial.printf("[MqttService] Đang kết nối %s:%d ... ", m_broker, m_port);

    if (m_mqttClient.connect(clientId.c_str())) {
        Serial.println("Thành công!");

        String switchTopic = String("doan20/") + m_username + "/device/+/switch";
        String provTopic   = String("doan20/") + m_username + "/device/+/provision";

        m_mqttClient.subscribe(switchTopic.c_str(), 1);
        m_mqttClient.subscribe(provTopic.c_str(), 1);

        Serial.printf("[MqttService] Đã Subscribe:\n  -> %s\n  -> %s\n", switchTopic.c_str(), provTopic.c_str());
    } else {
        Serial.printf("Thất bại (rc=%d), thử lại sau.\n", m_mqttClient.state());
    }
}

void MqttService::loop() {
    if (!m_mqttClient.connected()) {
        connectNonBlocking();
    } else {
        m_mqttClient.loop();
    }
}

void MqttService::publishState(const Device& dev) {
    if (!m_mqttClient.connected()) return;

    String topic = String("doan20/") + m_username + "/device/" + dev.getId() + "/state";

    JsonDocument doc;
    doc["id"] = dev.getId();
    doc["state"] = dev.getState() ? 1 : 0;
    doc["gpio"] = dev.getRelayPin();
    doc["source"] = "esp32";

    char buffer[128];
    serializeJson(doc, buffer);

    m_mqttClient.publish(topic.c_str(), buffer, true);
    Serial.printf("[MqttService] Publish State: %s -> %s\n", topic.c_str(), buffer);
}

void MqttService::publishAck(const String& id, int gpio, const char* status, const char* reason, const char* message) {
    if (!m_mqttClient.connected()) return;

    String topic = String("doan20/") + m_username + "/device/" + id + "/ack";

    JsonDocument doc;
    doc["id"] = id;
    doc["status"] = status;
    doc["gpio"] = gpio;
    doc["state"] = 0;
    doc["reason"] = reason;
    doc["message"] = message;

    char buffer[256];
    serializeJson(doc, buffer);

    m_mqttClient.publish(topic.c_str(), buffer);
    Serial.printf("[MqttService] Publish ACK: %s -> %s\n", topic.c_str(), buffer);
}

void MqttService::handleMqttPayload(char* topic, byte* payload, unsigned int length) {
    char payloadStr[256];
    if (length >= sizeof(payloadStr)) length = sizeof(payloadStr) - 1;
    memcpy(payloadStr, payload, length);
    payloadStr[length] = '\0';

    Serial.printf("\n[MqttService] Nhận tin [%s]: %s\n", topic, payloadStr);

    String topicStr = String(topic);
    int firstSlash  = topicStr.indexOf('/');
    int secondSlash = topicStr.indexOf('/', firstSlash + 1);
    int thirdSlash  = topicStr.indexOf('/', secondSlash + 1);
    int fourthSlash = topicStr.indexOf('/', thirdSlash + 1);

    IncomingCommand cmd;
    cmd.targetState = false;
    cmd.targetGpio  = -1;
    cmd.action      = "switch";

    if (thirdSlash > 0 && fourthSlash > thirdSlash) {
        cmd.deviceId = topicStr.substring(thirdSlash + 1, fourthSlash);
        cmd.action   = topicStr.substring(fourthSlash + 1);
    }

    if (payloadStr[0] == '{') {
        JsonDocument doc;
        DeserializationError err = deserializeJson(doc, payloadStr);
        if (!err) {
            if (doc["id"].is<const char*>()) {
                cmd.deviceId = doc["id"].as<String>();
            }
            int st = doc["state"] | 0;
            cmd.targetState = (st == 1);
            cmd.targetGpio  = doc["gpio"] | -1;
        }
    } else {
        cmd.targetState = (strcmp(payloadStr, "1") == 0 || strcasecmp(payloadStr, "ON") == 0);
    }

    if (cmd.deviceId.length() > 0 && m_onCommandReceived) {
        m_onCommandReceived(cmd);
    }
}
