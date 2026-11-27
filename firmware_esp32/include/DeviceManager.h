#pragma once

#include <vector>
#include <Preferences.h>
#include "Device.h"

/**
 * Lớp quản lý toàn bộ thiết bị trong phòng, lưu trữ NVS Flash
 * và cấp phát chân GPIO động (Dynamic GPIO Provisioning)
 */
class DeviceManager {
public:
    DeviceManager();

    // Khởi tạo các thiết bị từ bộ nhớ NVS Flash và cấu hình ban đầu
    void init();

    // Giới hạn tối đa số thiết bị relay khả dụng trên phần cứng ESP32
    static constexpr size_t MAX_DEVICES = 16;

    // Tìm hoặc tự động khởi tạo chân GPIO cho thiết bị mới (trả về lý do lỗi nếu thất bại)
    Device* provisionDevice(const String& id, int gpio, int buttonPin = -1, String* outReason = nullptr);

    // Xóa thiết bị và giải phóng chân GPIO
    bool removeDevice(const String& id);

    // Kiểm tra xem chân GPIO đã bị thiết bị khác chiếm dụng chưa
    bool isPinOccupied(int gpio, const String& excludeId) const;

    // Lấy thiết bị theo ID
    Device* getDevice(const String& id);

    // Danh sách toàn bộ thiết bị hiện có
    const std::vector<Device>& getDevices() const { return m_devices; }

    // Đăng ký callback khi có nút vật lý bị nhấn
    void setOnButtonToggledCallback(const std::function<void(Device&)>& callback) {
        m_onButtonToggled = callback;
    }

    // Vòng lặp giám sát nút bấm của tất cả thiết bị
    void loop();

    // Lưu toàn bộ danh sách thiết bị vào bộ nhớ Flash NVS
    void saveAllDevices();

    // Tự động ghép cặp chân nút bấm theo chân Relay (-1 nếu không có nút riêng)
    static int getAutoButtonPin(int relayGpio);

private:
    std::vector<Device> m_devices;
    Preferences m_prefs;
    std::function<void(Device&)> m_onButtonToggled;
};
