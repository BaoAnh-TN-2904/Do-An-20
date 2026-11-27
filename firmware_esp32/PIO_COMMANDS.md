# Các lệnh PlatformIO (PIO) cơ bản (chạy trong thư mục `firmware_esp32`)

## Kiểm tra cổng COM kết nối ESP32
```powershell
pio device list
```

## Build (biên dịch kiểm tra lỗi)
```powershell
pio run
```

## Build và Upload (tự động chọn cổng)
```powershell
pio run -t upload
```

## Build và Upload (chỉ định cổng COM)
```powershell
pio run -t upload --upload-port COM4
```

## Mở Serial Monitor
```powershell
pio device monitor -p COM4 -b 115200
```

## Upload và mở Serial Monitor
```powershell
pio run -t upload --upload-port COM4 -t monitor
```

## Xóa file build cũ (Clean)
```powershell
pio run -t clean
```

## Xóa toàn bộ Flash (Erase)
```powershell
pio run -t erase --upload-port COM4
```
