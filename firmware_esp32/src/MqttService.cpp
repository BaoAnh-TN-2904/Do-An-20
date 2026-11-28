#include "MqttService.h"
#include <WiFi.h>

MqttService::MqttService()
    : m_mqttClient(m_espClient),
      m_broker(nullptr),
      m_port(1883),
      m_topicRoot("doan20"),
      m_lastRetryTime(0),
      m_connectedAt(0),
      m_pendingInitialSync(false) {}

void MqttService::ensureDeviceCode() {
    if (m_deviceCode.length() > 0) return;
    String mac = WiFi.macAddress();
    mac.replace(":", "");
    mac.toUpperCase();
    if (mac.length() >= 6 && mac != "000000000000") {
        m_deviceCode = mac.substring(mac.length() - 6);
    } else {
        uint64_t efuse = ESP.getEfuseMac();
        char buf[7];
        snprintf(buf, sizeof(buf), "%02X%02X%02X",
                 (uint8_t)(efuse >> 24),
                 (uint8_t)(efuse >> 32),
                 (uint8_t)(efuse >> 40));
        m_deviceCode = String(buf);
    }
}

void MqttService::setup(const char* broker, int port, const char* topicRoot) {
    m_broker = broker;
    m_port = port;
    m_topicRoot = (topicRoot != nullptr && strlen(topicRoot) > 0) ? String(topicRoot) : String("doan20");

    ensureDeviceCode();

    m_mqttClient.setServer(m_broker, m_port);
    m_mqttClient.setBufferSize(512);
    m_mqttClient.setKeepAlive(10);      // Phát hiện đứt kết nối nhanh hơn (10s)
    m_mqttClient.setSocketTimeout(5);   // Tránh treo socket quá lâu (5s)

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

    ensureDeviceCode();
    String clientId = "ESP32_DoAn20_" + m_deviceCode;
    Serial.printf("[MqttService] Đang kết nối %s:%d ... ", m_broker, m_port);

    // Dùng cleanSession = false để Broker giữ các lệnh QoS 1 từ App khi ESP32 tạm mất mạng
    if (m_mqttClient.connect(clientId.c_str(), nullptr, nullptr, nullptr, 0, false, nullptr, false)) {
        Serial.println("Thành công!");
        Serial.println("=======================================================");
        Serial.printf("  MA ESP32 (6 KY TU CUOI MAC): %s\n", m_deviceCode.c_str());
        Serial.printf("  WiFi IP: %s | Topic: %s/%s/device/...\n",
                      WiFi.localIP().toString().c_str(),
                      m_topicRoot.c_str(),
                      m_deviceCode.c_str());
        Serial.println("=======================================================");

        String switchTopic = m_topicRoot + "/" + m_deviceCode + "/device/+/switch";
        String provTopic   = m_topicRoot + "/" + m_deviceCode + "/device/+/provision";
        String removeTopic = m_topicRoot + "/" + m_deviceCode + "/device/+/remove";

        m_mqttClient.subscribe(switchTopic.c_str(), 1);
        m_mqttClient.subscribe(provTopic.c_str(), 1);
        m_mqttClient.subscribe(removeTopic.c_str(), 1);

        Serial.printf("[MqttService] Đã Subscribe:\n  -> %s\n  -> %s\n  -> %s\n",
                      switchTopic.c_str(), provTopic.c_str(), removeTopic.c_str());

        m_connectedAt = millis();
        m_pendingInitialSync = true;
    } else {
        Serial.printf("Thất bại (rc=%d), thử lại sau.\n", m_mqttClient.state());
    }
}

void MqttService::loop() {
    if (WiFi.status() != WL_CONNECTED) {
        if (m_mqttClient.connected()) {
            Serial.println("[MqttService] WiFi mất kết nối -> đóng socket MQTT cũ.");
            m_mqttClient.disconnect();
        }
        m_pendingInitialSync = false;
        return;
    }

    if (!m_mqttClient.connected()) {
        m_pendingInitialSync = false;
        connectNonBlocking();
    } else {
        m_mqttClient.loop();

        // Đợi ~1.2s sau khi kết nối để nhận hết gói QoS 1/retained từ App trước khi đồng bộ trạng thái
        static bool firstBootSyncDone = false;
        if (m_pendingInitialSync && (millis() - m_connectedAt >= 1200)) {
            m_pendingInitialSync = false;
            bool isFirstBoot = !firstBootSyncDone;
            firstBootSyncDone = true;
            if (m_onConnected) {
                m_onConnected(isFirstBoot);
            }
        }
    }
}

void MqttService::publishState(const Device& dev) {
    if (!m_mqttClient.connected()) return;

    String topic = m_topicRoot + "/" + m_deviceCode + "/device/" + dev.getId() + "/state";

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

void MqttService::clearRetainedState(const String& id) {
    if (!m_mqttClient.connected()) return;
    String topic = m_topicRoot + "/" + m_deviceCode + "/device/" + id + "/state";
    m_mqttClient.publish(topic.c_str(), "", true);
    Serial.printf("[MqttService] Clear Retained State: %s\n", topic.c_str());
}

void MqttService::publishAck(const String& id, int gpio, const char* status, const char* reason, const char* message, int state) {
    if (!m_mqttClient.connected()) return;

    String topic = m_topicRoot + "/" + m_deviceCode + "/device/" + id + "/ack";

    JsonDocument doc;
    doc["id"] = id;
    doc["status"] = status;
    doc["gpio"] = gpio;
    doc["state"] = state;
    doc["reason"] = reason;
    doc["message"] = message;

    char buffer[256];
    serializeJson(doc, buffer);

    bool retainAck = (strcmp(status, "READY") == 0 || strcmp(status, "FAILED") == 0 || strcmp(status, "FULL") == 0);
    m_mqttClient.publish(topic.c_str(), buffer, retainAck);
    Serial.printf("[MqttService] Publish ACK: %s -> %s\n", topic.c_str(), buffer);
}

void MqttService::handleMqttPayload(char* topic, byte* payload, unsigned int length) {
    // Bỏ qua bản tin rỗng (khi App xóa retained message trên Broker)
    if (length == 0) return;

    char payloadStr[512];
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
            if (doc["action"].is<const char*>()) {
                String jsonAction = doc["action"].as<String>();
                if (jsonAction.equalsIgnoreCase("REMOVE")) {
                    cmd.action = "remove";
                }
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
