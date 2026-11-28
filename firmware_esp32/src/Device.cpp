#include "Device.h"

Device::Device(const String& id, int relayPin, int buttonPin, bool activeLow)
    : m_id(id),
      m_relayPin(relayPin),
      m_buttonPin(buttonPin),
      m_activeLow(activeLow),
      m_state(false),
      m_pendingSync(false),
      m_lastReading(HIGH),
      m_stableState(HIGH),
      m_lastDebounceTime(0) {}

bool Device::isValidOutputPin(int pin) {
    if (pin < 0 || pin > 33) return false;
    // Chân kết nối chip SPI Flash trên ESP32 không được can thiệp
    if (pin >= 6 && pin <= 11) return false;
    return true;
}

void Device::initHardware() {
    if (isValidOutputPin(m_relayPin)) {
        pinMode(m_relayPin, OUTPUT);
        applyRelayOutput();
    }

    if (m_buttonPin >= 0) {
        pinMode(m_buttonPin, INPUT_PULLUP);
        m_lastReading = digitalRead(m_buttonPin);
        m_stableState = m_lastReading;
    }
}

void Device::releaseHardware() {
    if (isValidOutputPin(m_relayPin)) {
        digitalWrite(m_relayPin, m_activeLow ? HIGH : LOW);
        pinMode(m_relayPin, INPUT);
    }
}

void Device::applyRelayOutput() {
    if (!isValidOutputPin(m_relayPin)) return;
    if (m_activeLow) {
        digitalWrite(m_relayPin, m_state ? LOW : HIGH);
    } else {
        digitalWrite(m_relayPin, m_state ? HIGH : LOW);
    }
}

void Device::setState(bool newState) {
    if (m_state != newState) {
        m_state = newState;
        applyRelayOutput();
    }
}

void Device::toggleState() {
    setState(!m_state);
}

void Device::setRelayPin(int pin) {
    if (m_relayPin != pin && isValidOutputPin(pin)) {
        releaseHardware();
        m_relayPin = pin;
        pinMode(m_relayPin, OUTPUT);
        applyRelayOutput();
    }
}

void Device::setButtonPin(int buttonPin) {
    if (m_buttonPin != buttonPin) {
        m_buttonPin = buttonPin;
        if (m_buttonPin >= 0) {
            pinMode(m_buttonPin, INPUT_PULLUP);
            m_lastReading = digitalRead(m_buttonPin);
            m_stableState = m_lastReading;
        }
    }
}

void Device::checkButton(const std::function<void(Device&)>& onToggled) {
    if (m_buttonPin < 0) return;

    int reading = digitalRead(m_buttonPin);
    unsigned long now = millis();

    if (reading != m_lastReading) {
        m_lastDebounceTime = now;
    }

    if ((now - m_lastDebounceTime) > 50) { // 50ms debounce
        if (reading != m_stableState) {
            m_stableState = reading;

            // Nút nhấn xuống mức LOW (INPUT_PULLUP)
            if (m_stableState == LOW) {
                toggleState();
                if (onToggled) {
                    onToggled(*this);
                }
            }
        }
    }

    m_lastReading = reading;
}
