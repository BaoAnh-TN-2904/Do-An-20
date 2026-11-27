#pragma once

#include <Arduino.h>
#include <functional>

/**
 * Lớp đại diện cho một thiết bị IoT (Relay + Nút bấm vật lý)
 */
class Device {
public:
    Device(const String& id, int relayPin, int buttonPin = -1, bool activeLow = true);

    // Khởi tạo phần cứng (pinMode, trạng thái xuất ban đầu)
    void initHardware();

    // Điều khiển trạng thái
    void setState(bool newState);
    void toggleState();
    bool getState() const { return m_state; }

    // Quản lý chân GPIO
    void setRelayPin(int pin);
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

    // Biến giám sát chống dội phím (debounce)
    int m_lastReading;
    int m_stableState;
    unsigned long m_lastDebounceTime;
};
