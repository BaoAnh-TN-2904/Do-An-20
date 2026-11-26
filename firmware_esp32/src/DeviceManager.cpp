#include "DeviceManager.h"
#include "Config.h"

DeviceManager::DeviceManager() {}

int DeviceManager::getAutoButtonPin(int relayGpio) {
    switch (relayGpio) {
        case 18: return 4;   // Đèn bàn học -> Nút GPIO 4
        case 23: return 5;   // Quạt bàn -> Nút GPIO 5
        case 19: return 16;  // Thiết bị 3 -> Nút GPIO 16
        case 21: return 17;  // Thiết bị 4 -> Nút GPIO 17
        case 22: return 13;  // Thiết bị 5 -> Nút GPIO 13
        case 25: return 14;  // Thiết bị 6 -> Nút GPIO 14
        case 26: return 15;  // Thiết bị 7 -> Nút GPIO 15
        case 27: return 32;  // Thiết bị 8 -> Nút GPIO 32
        default: return 0;   // Dùng nút BOOT (GPIO 0) có sẵn trên bo mạch
    }
}

void DeviceManager::init() {
    // Nạp 2 thiết bị cơ sở mặc định theo dữ liệu mẫu của App
    provisionDevice("item_lamp_01", 18, 4);
    provisionDevice("item_fan_01", 23, 5);

    Serial.printf("[DeviceManager] Đã khởi tạo %d thiết bị cơ sở.\n", (int)m_devices.size());
}

Device* DeviceManager::getDevice(const String& id) {
    for (size_t i = 0; i < m_devices.size(); i++) {
        if (m_devices[i].getId() == id) {
            return &m_devices[i];
        }
    }
    return nullptr;
}

bool DeviceManager::isPinOccupied(int gpio, const String& excludeId) const {
    for (size_t i = 0; i < m_devices.size(); i++) {
        if (m_devices[i].getId() != excludeId && m_devices[i].getRelayPin() == gpio) {
            return true;
        }
    }
    return false;
}

Device* DeviceManager::provisionDevice(const String& id, int gpio, int buttonPin, String* outReason) {
    Device* existing = getDevice(id);
    if (existing != nullptr) {
        if (gpio > 0) {
            if (!Device::isValidOutputPin(gpio)) {
                if (outReason) *outReason = "INVALID_PIN";
                return nullptr;
            }
            if (isPinOccupied(gpio, id)) {
                if (outReason) *outReason = "PIN_OCCUPIED";
                return nullptr;
            }
            if (existing->getRelayPin() != gpio) {
                Serial.printf("[DeviceManager] Cập nhật chân cho '%s': GPIO %d -> GPIO %d\n", id.c_str(), existing->getRelayPin(), gpio);
                existing->setRelayPin(gpio);
                saveDevice(*existing);
            }
        }
        if (outReason) *outReason = "OK";
        return existing;
    }

    // Kiểm tra tính hợp lệ của chân GPIO yêu cầu
    if (!Device::isValidOutputPin(gpio)) {
        Serial.printf("[DeviceManager] LỖI: GPIO %d không phải chân xuất Relay hợp lệ!\n", gpio);
        if (outReason) *outReason = "INVALID_PIN";
        return nullptr;
    }

    // Kiểm tra xem chân này đã bị thiết bị khác chiếm dụng chưa
    if (isPinOccupied(gpio, id)) {
        Serial.printf("[DeviceManager] LỖI: GPIO %d đã bị chiếm bởi thiết bị khác!\n", gpio);
        if (outReason) *outReason = "PIN_OCCUPIED";
        return nullptr;
    }

    // Kiểm tra xem phần cứng ESP32 đã đầy chân / đạt giới hạn thiết bị chưa
    if (m_devices.size() >= MAX_DEVICES) {
        Serial.printf("[DeviceManager] LỖI: Đã đạt giới hạn tối đa %d thiết bị trên ESP32!\n", (int)MAX_DEVICES);
        if (outReason) *outReason = "GPIO_FULL";
        return nullptr;
    }

    // Khởi tạo thiết bị mới
    int assignedBtn = (buttonPin >= 0) ? buttonPin : getAutoButtonPin(gpio);
    Serial.printf("\n[DeviceManager] Cấp phát chân mới: ID='%s', Relay=GPIO %d, Button=GPIO %d\n", id.c_str(), gpio, assignedBtn);

    m_devices.emplace_back(id, gpio, assignedBtn, Config::RELAY_ACTIVE_LOW);
    Device* newDev = &m_devices.back();
    newDev->initHardware();
    saveDevice(*newDev);

    if (outReason) *outReason = "OK";
    return newDev;
}

void DeviceManager::saveDevice(const Device& dev) {
    m_prefs.begin("devices", false);
    m_prefs.putInt(dev.getId().c_str(), dev.getRelayPin());
    String stateKey = dev.getId() + "_st";
    m_prefs.putBool(stateKey.c_str(), dev.getState());
    m_prefs.end();
    Serial.printf("[DeviceManager] Đã lưu '%s' vào bộ nhớ Flash NVS.\n", dev.getId().c_str());
}

void DeviceManager::loop() {
    for (size_t i = 0; i < m_devices.size(); i++) {
        m_devices[i].checkButton(m_onButtonToggled);
    }
}
