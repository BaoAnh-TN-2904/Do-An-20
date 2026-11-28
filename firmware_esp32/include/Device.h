#pragma once

#include <Arduino.h>
#include <functional>

/**
 * Lớp đại diện cho một thiết bị IoT (Relay + Nút bấm vật lý)
 */
class Device {
public:
    Device(const String& id, int relayPin, int buttonPin = -1, bool activeLow = true);

    // Khởi tạo / giải phóng phần cứng
    void initHardware();
    void releaseHardware();

    // Điều khiển trạng thái
    void setState(bool newState);
    void toggleState();
    bool getState() const { return m_state; }
    void setPendingSync(bool pending) { m_pendingSync = pending; }
    bool isPendingSync() const { return m_pendingSync; }

    // Quản lý chân GPIO
    void setRelayPin(int pin);
    void setButtonPin(int buttonPin);
    int getRelayPin() const { return m_relayPin; }
    int getButtonPin() const { return m_buttonPin; }
    String getId() const { return m_id; }

    // Giám sát nút bấm vật lý kèm debounce
    void checkButton(const std::function<void(Device&)>& onToggled);

    // Kiểm tra tính hợp lệ của chân GPIO xuất Relay trên ESP32
    static bool isValidOutputPin(int pin);

private:
    void applyRelayOutput();

    String m_id;
    int m_relayPin;
    int m_buttonPin;
    bool m_activeLow;
    bool m_state;
    bool m_pendingSync;

    // Biến giám sát chống dội phím (debounce)
    int m_lastReading;
    int m_stableState;
    unsigned long m_lastDebounceTime;
};
