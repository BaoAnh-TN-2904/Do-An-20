# KẾ HOẠCH PHÁT TRIỂN APP ANDROID – GỌN PHÒNG

## 1. Thông tin dự án

- **Tên app:** Gọn Phòng
- **Nền tảng:** Android
- **IDE:** Android Studio
- **Ngôn ngữ:** Kotlin
- **UI:** XML Layout + Material Components
- **Kiến trúc đề xuất:** MVVM
- **Mục tiêu:** Chuyển prototype React hiện tại thành ứng dụng Android native nhưng giữ nguyên bố cục, màu sắc, luồng điều hướng và chức năng chính.
- **Kích thước prototype tham chiếu:** khoảng 390 px chiều rộng, tối ưu theo màn hình điện thoại.

> Nguồn thiết kế: file `Layout_ĐoAn20.txt`. Prototype hiện tại mô phỏng ứng dụng quản lý vật dụng với 4 tab: Trang chủ, Vật dụng, Thống kê, Cá nhân; ngoài ra có màn hình Thêm vật dụng và Chi tiết vật dụng. fileciteturn0file0L107-L124

---

# 2. Phân tích layout hiện tại

Prototype có các nhóm giao diện chính:

1. **Trang chủ**
2. **Danh sách vật dụng**
3. **Thống kê**
4. **Cá nhân**
5. **Thêm vật dụng**
6. **Chi tiết vật dụng**
7. **Thông báo dạng popup/dropdown**

Prototype cũng hỗ trợ:
- Tìm kiếm vật dụng.
- Lọc theo trạng thái.
- Chuyển trạng thái `Còn dùng` / `Hỏng`.
- Bật/tắt thiết bị có công tắc.
- Thêm vật dụng.
- Xóa vật dụng.
- Xem thông tin chi tiết.
- Hiển thị thông báo.
- Thống kê theo tình trạng và danh mục.

Các trường dữ liệu của một vật dụng gồm `id`, `name`, `category`, `image`, `status`, `hasSwitch`, `isOn`, `price`, `buyDate`, `pinConfig`, `notes`. fileciteturn0file0L30-L40

---

# 3. Mục tiêu khi chuyển sang Android

## 3.1. Giữ nguyên thiết kế

Ưu tiên giữ:

- Màu xanh chủ đạo `#2563EB`.
- Nền sáng `#F6F8FC`.
- Card nền trắng.
- Bo góc lớn khoảng 12–16dp.
- Font gần với Inter.
- Bottom Navigation 4 mục.
- Các badge trạng thái.
- Switch bật/tắt.
- Khoảng cách và thứ tự thông tin giống prototype.

## 3.2. Chuyển đổi công nghệ

| Prototype React | Android |
|---|---|
| `useState` | ViewModel + StateFlow/LiveData |
| React component | Fragment / View |
| `onClick` | `setOnClickListener` |
| `<input>` | EditText / TextInputLayout |
| `<select>` | AutoCompleteTextView / Spinner |
| `<textarea>` | TextInputEditText nhiều dòng |
| `<button>` | MaterialButton |
| CSS Tailwind | XML + dimens/colors/styles |
| Array `items` | Repository + Room |
| `img` | Drawable / Coil |
| Tab state | Navigation Component |
| Popup | Dialog / BottomSheet |
| File upload | Activity Result API |
| `filteredItems` | ViewModel filter |

---

# 4. Công nghệ Android đề xuất

## 4.1. Thành phần chính

- Kotlin
- Android Studio
- XML Layout
- Material 3
- AndroidX
- Navigation Component
- ViewModel
- StateFlow
- Room Database
- RecyclerView
- ViewBinding
- Coil
- Activity Result API

## 4.2. Nếu chỉ làm prototype trước

Có thể triển khai theo 2 giai đoạn:

### Giai đoạn A – UI Prototype

- Dữ liệu giả bằng Kotlin.
- Chưa cần database.
- Tập trung làm giao diện và điều hướng.
- Kiểm tra giống Figma/prototype.

### Giai đoạn B – App hoàn chỉnh

- Room Database.
- Lưu ảnh.
- CRUD vật dụng.
- Thông báo.
- Thống kê động.
- Có thể mở rộng API/server sau này.

---

# 5. Cấu trúc project Android

Đề xuất:

```text
app/
└── src/main/
    ├── java/com/example/gonphong/
    │   ├── data/
    │   │   ├── local/
    │   │   │   ├── AppDatabase.kt
    │   │   │   ├── ItemDao.kt
    │   │   │   └── ItemEntity.kt
    │   │   └── repository/
    │   │       └── ItemRepository.kt
    │   │
    │   ├── model/
    │   │   └── Item.kt
    │   │
    │   ├── ui/
    │   │   ├── main/
    │   │   │   ├── MainActivity.kt
    │   │   │   └── MainViewModel.kt
    │   │   │
    │   │   ├── home/
    │   │   │   └── HomeFragment.kt
    │   │   │
    │   │   ├── items/
    │   │   │   ├── ItemsFragment.kt
    │   │   │   ├── ItemAdapter.kt
    │   │   │   └── ItemViewModel.kt
    │   │   │
    │   │   ├── additem/
    │   │   │   └── AddItemFragment.kt
    │   │   │
    │   │   ├── detail/
    │   │   │   └── ItemDetailFragment.kt
    │   │   │
    │   │   ├── statistics/
    │   │   │   └── StatisticsFragment.kt
    │   │   │
    │   │   └── profile/
    │   │       └── ProfileFragment.kt
    │   │
    │   └── utils/
    │       ├── Constants.kt
    │       └── Extensions.kt
    │
    └── res/
        ├── drawable/
        ├── mipmap/
        ├── layout/
        ├── menu/
        ├── navigation/
        ├── values/
        │   ├── colors.xml
        │   ├── dimens.xml
        │   ├── strings.xml
        │   └── themes.xml
        └── xml/
```

---

# 6. Thiết kế Navigation

## 6.1. Bottom Navigation

Có 4 tab:

```text
Trang chủ
    │
    ├── Vật dụng
    │      ├── Chi tiết vật dụng
    │      └── Thêm vật dụng
    │
    ├── Thống kê
    │
    └── Cá nhân
```

Prototype sử dụng chính xác 4 mục bottom navigation: `Trang chủ`, `Vật dụng`, `Thống kê`, `Cá nhân`. fileciteturn0file0L1079-L1203

## 6.2. Navigation Graph

Đề xuất:

```text
MainActivity
│
└── NavHostFragment
    │
    ├── HomeFragment
    │
    ├── ItemsFragment
    │      │
    │      ├── ItemDetailFragment
    │      │
    │      └── AddItemFragment
    │
    ├── StatisticsFragment
    │
    └── ProfileFragment
```

---

# 7. Màn hình 01 – Trang chủ

## Thành phần

### Header

- Logo/icon nhà.
- Tên `Gọn Phòng`.
- Nút chuông thông báo.

Prototype dùng header với logo nhà và tên ứng dụng `Gọn Phòng`. fileciteturn0file0L468-L495

### Lời chào

```text
Chào Minh Anh!
Phòng gọn gàng, mọi thứ trong tầm tay.
```

### 3 thẻ thống kê

```text
┌──────────┬──────────┬──────────┐
│    25    │    20    │     5    │
│ Tổng     │ Còn dùng │  Hỏng    │
└──────────┴──────────┴──────────┘
```

Chức năng click:

- Tổng vật dụng → mở danh sách toàn bộ.
- Còn dùng → mở danh sách đã lọc.
- Hỏng → mở danh sách hỏng.

Prototype dùng ba card thống kê tương tác theo đúng luồng này. fileciteturn0file0L543-L609

### Vật dụng gần đây

Mỗi item:

```text
┌────────────────────────────────┐
│ [Ảnh]  Tên vật dụng            │
│        Danh mục       [Status] │
│                       [Switch] │
└────────────────────────────────┘
```

Click item → `ItemDetailFragment`.

### Nút

```text
+ Thêm vật dụng
```

→ `AddItemFragment`.

---

# 8. Màn hình 02 – Danh sách vật dụng

## Header

```text
Danh sách vật dụng
Quản lý và cập nhật tình trạng
```

Nút:

```text
+ Thêm
```

## Search

```text
Tìm kiếm theo tên hoặc danh mục...
```

Tìm theo:

- Tên.
- Danh mục.

Prototype lọc item bằng cả `name` và `category`. fileciteturn0file0L221-L227

## Filter

```text
[Tất cả] [Còn dùng] [Hỏng]
```

## RecyclerView

Mỗi item hiển thị:

- Ảnh.
- Tên.
- Danh mục.
- Trạng thái.
- Switch nếu là thiết bị có thể điều khiển.

## Empty state

Nếu không có dữ liệu:

```text
Không tìm thấy vật dụng phù hợp

Đặt lại bộ lọc
```

---

# 9. Màn hình 03 – Chi tiết vật dụng

Khi click một item:

```text
← Quay lại

┌─────────────────────────┐
│                         │
│          ẢNH            │
│                         │
└─────────────────────────┘

Tên vật dụng
Danh mục: Điện tử

Trạng thái: Còn dùng

Cấu hình chân / Giao tiếp:
GPIO 14, PWM Pin 12

Giá mua: 450.000đ
Ngày mua: 10/05/2024

Ghi chú:
...

Công tắc nguồn       [ON]

[Đổi trạng thái] [Xóa]
```

Prototype có phần ảnh lớn, trạng thái, danh mục, cấu hình GPIO/giao tiếp, giá mua, ngày mua, ghi chú, switch và nút đổi trạng thái/xóa. fileciteturn0file0L967-L1073

---

# 10. Màn hình 04 – Thêm vật dụng

## Header

```text
[←] Thêm vật dụng
```

## Form

### 1. Ảnh

```text
┌─────────────────────────┐
│       📷                │
│   Thêm ảnh vật dụng     │
│ Chọn ảnh từ thư viện    │
└─────────────────────────┘
```

Cho phép:

- Chọn ảnh từ thư viện.
- Preview ảnh.
- Đổi ảnh.

### 2. Tên vật dụng

```text
Tên vật dụng
[Nhập tên vật dụng]
```

### 3. Loại vật dụng

```text
Loại vật dụng
[Chọn loại vật dụng ▼]
```

Options:

- Nội thất
- Điện tử
- Đồ dùng học tập
- Khác

### 4. Cấu hình chân / giao tiếp

Chỉ hiển thị nếu:

```text
Loại = Điện tử
```

Ví dụ:

```text
GPIO 23, ADC1, SPI...
```

Prototype cũng chỉ hiển thị trường này khi chọn `Điện tử`. fileciteturn0file0L336-L356

### 5. Giá mua + Ngày mua

```text
┌──────────────┬──────────────┐
│ Giá mua      │ Ngày mua    │
│ [0đ]         │ [dd/mm/yyyy]│
└──────────────┴──────────────┘
```

### 6. Trạng thái

```text
[Chọn trạng thái ▼]
```

Options:

- Còn dùng
- Hỏng

### 7. Ghi chú

```text
[Thêm ghi chú về vật dụng...]
```

### 8. Button

```text
[✓ Lưu vật dụng]
[    Hủy       ]
```

Prototype hiện có đầy đủ các trường này và nút `Lưu vật dụng` / `Hủy`. fileciteturn0file0L359-L460

---

# 11. Màn hình 05 – Thống kê

## Tiêu đề

```text
Thống kê phòng
Báo cáo đồ đạc và thiết bị sử dụng
```

## Tỉ lệ hoạt động

```text
Tỉ lệ đồ đạc hoạt động                    80%

████████████████████░░░░░

20 còn dùng tốt             5 cần bảo trì/thay thế
```

## Phân loại

```text
Nội thất             14 vật dụng
Thiết bị điện tử      8 vật dụng
Khác                   3 vật dụng
```

## Gợi ý

```text
Gợi ý dọn dẹp & sắp xếp

Bạn có 5 vật dụng hỏng chưa xử lý.
Bạn có thể thanh lý hoặc sửa chữa...
```

Các số liệu trên là dữ liệu mẫu đang được dùng trong prototype, cần chuyển thành dữ liệu động khi kết nối database. fileciteturn0file0L871-L924

---

# 12. Màn hình 06 – Cá nhân

## Profile card

```text
        [MA]

      Minh Anh
Phòng 402 • Căn hộ Harmony

[25 vật dụng] [Đã dọn dẹp hôm nay]
```

## Menu

```text
Cài đặt phòng             >
Thông báo & Nhắc nhở     Đang bật
Sao lưu dữ liệu            >
Đăng xuất
```

Giữ nguyên nội dung và thứ tự chức năng như prototype. fileciteturn0file0L928-L963

---

# 13. Thông báo

Nút chuông trên Header mở danh sách thông báo.

Ví dụ:

```text
Thông báo phòng

● Đèn bàn cần sửa chữa
  Thiết bị đang báo hỏng...
  10 phút trước

● Quạt điện đang mở
  Quạt đang hoạt động...
  1 giờ trước
```

Prototype có hai thông báo mẫu này trong popup. fileciteturn0file0L487-L523

Android nên triển khai bằng:

- MaterialAlertDialog hoặc
- Modal BottomSheet.

Nếu sau này có backend:

- Firebase Cloud Messaging.
- Local notification.

---

# 14. Model dữ liệu

## Item

```kotlin
data class Item(
    val id: String,
    val name: String,
    val category: String,
    val image: String?,
    val status: ItemStatus,
    val hasSwitch: Boolean,
    val isOn: Boolean,
    val price: Long?,
    val buyDate: String?,
    val pinConfig: String?,
    val notes: String?
)
```

## Status

```kotlin
enum class ItemStatus {
    ACTIVE,
    BROKEN
}
```

Mapping giao diện:

```text
ACTIVE → Còn dùng
BROKEN → Hỏng
```

---

# 15. Room Database

## Bảng Item

```text
items
--------------------------------
id
name
category
imageUri
status
hasSwitch
isOn
price
buyDate
pinConfig
notes
createdAt
updatedAt
```

## DAO

Cần có:

```kotlin
getAllItems()

getItemById(id)

insertItem(item)

updateItem(item)

deleteItem(item)

searchItems(query)

getItemsByStatus(status)

getItemsByCategory(category)
```

---

# 16. ViewModel

## ItemViewModel

Quản lý:

```text
Danh sách item
Search query
Filter status
Selected item
Add item
Edit item
Delete item
Toggle switch
Toggle status
```

State đề xuất:

```kotlin
data class ItemUiState(
    val items: List<Item> = emptyList(),
    val searchQuery: String = "",
    val filter: ItemStatus? = null,
    val isLoading: Boolean = false
)
```

---

# 17. Luồng thêm vật dụng

```text
Trang chủ
   │
   ▼
+ Thêm vật dụng
   │
   ▼
AddItemFragment
   │
   ├── Chọn ảnh
   ├── Nhập tên
   ├── Chọn loại
   ├── Nếu điện tử → nhập GPIO/Giao tiếp
   ├── Nhập giá
   ├── Chọn ngày
   ├── Chọn trạng thái
   └── Nhập ghi chú
          │
          ▼
     Lưu vật dụng
          │
          ▼
     Validate dữ liệu
          │
          ▼
       Room DB
          │
          ▼
    Quay về danh sách
```

Prototype hiện tại validate tối thiểu trường tên, sau đó tạo item mới và thêm vào đầu danh sách. fileciteturn0file0L174-L205

---

# 18. Luồng xem chi tiết

```text
Home / Items
      │
      ▼
Click Item
      │
      ▼
ItemDetailFragment
      │
      ├── Đổi trạng thái
      ├── Bật/tắt switch
      └── Xóa
```

---

# 19. Luồng tìm kiếm và lọc

```text
                    ┌── Tất cả
Search ── Filter ───┼── Còn dùng
                    └── Hỏng
```

Logic:

```text
Query rỗng
    ↓
hiển thị theo filter

Query có dữ liệu
    ↓
lọc name/category
    ↓
áp dụng status filter
    ↓
RecyclerView
```

---

# 20. Màu sắc

Tạo `colors.xml`:

```xml
<color name="primary_blue">#2563EB</color>
<color name="background">#F6F8FC</color>
<color name="text_primary">#17243B</color>
<color name="text_secondary">#748096</color>
<color name="border">#E6EBF2</color>

<color name="status_active">#168451</color>
<color name="status_active_bg">#EAF7EF</color>

<color name="status_broken">#D44242</color>
<color name="status_broken_bg">#FFF0F0</color>
```

---

# 21. Dimens

Tạo `dimens.xml` để tránh hard-code kích thước:

```xml
<dimen name="screen_padding">24dp</dimen>
<dimen name="card_radius">16dp</dimen>
<dimen name="small_radius">12dp</dimen>
<dimen name="button_height">48dp</dimen>
<dimen name="input_height">44dp</dimen>
```

---

# 22. Drawable cần tạo

Cần chuẩn bị:

```text
ic_home.xml
ic_notifications.xml
ic_items.xml
ic_statistics.xml
ic_profile.xml
ic_add.xml
ic_arrow_left.xml
ic_arrow_right.xml
ic_camera.xml
ic_calendar.xml
ic_check.xml
ic_alert.xml
ic_status.xml
ic_dropdown.xml
```

Ảnh vật dụng:

```text
desk
fan
lamp
...
```

Prototype hiện có asset cho bàn học, quạt điện và đèn bàn cùng nhiều icon điều hướng/chức năng. fileciteturn0file0L3-L27

---

# 23. RecyclerView Item

Tạo:

```text
item_item.xml
```

Layout:

```text
┌─────────────────────────────────────┐
│ [62x62]  Tên vật dụng               │
│  IMAGE    Danh mục          [Badge] │
│                            [Switch] │
└─────────────────────────────────────┘
```

Adapter:

```kotlin
class ItemAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onStatusClick: (Item) -> Unit,
    private val onSwitchClick: (Item) -> Unit
)
```

---

# 24. AddItem XML

Tạo:

```text
fragment_add_item.xml
```

Cấu trúc:

```text
ScrollView
└── LinearLayout
    ├── Header
    ├── Image Picker
    ├── Tên vật dụng
    ├── Loại vật dụng
    ├── GPIO / Giao tiếp
    ├── Giá mua + Ngày mua
    ├── Trạng thái
    ├── Ghi chú
    ├── Lưu vật dụng
    └── Hủy
```

---

# 25. Home XML

Tạo:

```text
fragment_home.xml
```

Cấu trúc:

```text
NestedScrollView
└── LinearLayout
    ├── Greeting
    ├── Stats Cards
    ├── Recent Items
    └── Add Item Button
```

---

# 26. Items XML

Tạo:

```text
fragment_items.xml
```

Cấu trúc:

```text
LinearLayout
├── Header
├── Search Bar
├── Filter Chips
└── RecyclerView
```

---

# 27. Statistics XML

Tạo:

```text
fragment_statistics.xml
```

Cấu trúc:

```text
NestedScrollView
└── LinearLayout
    ├── Header
    ├── Activity Progress Card
    ├── Category Breakdown Card
    └── Recommendation Card
```

---

# 28. Profile XML

Tạo:

```text
fragment_profile.xml
```

Cấu trúc:

```text
ScrollView
└── LinearLayout
    ├── Profile Card
    └── Settings Card
```

---

# 29. MainActivity

MainActivity chịu trách nhiệm:

- Khởi tạo Navigation.
- Hiển thị BottomNavigation.
- Đồng bộ selected tab.
- Điều hướng giữa các Fragment.

Không nên đưa logic CRUD vào MainActivity.

---

# 30. Kiến trúc MVVM

```text
                UI
                 │
                 ▼
             ViewModel
                 │
                 ▼
             Repository
                 │
          ┌──────┴──────┐
          ▼             ▼
       Room DB       API/Server
```

Lợi ích:

- Dễ bảo trì.
- Dễ test.
- Dễ thay Room bằng API.
- UI không chứa business logic.
- Có thể mở rộng IoT/backend về sau.

---

# 31. Giai đoạn phát triển

## Phase 1 – Khởi tạo

- [ ] Tạo project Android Studio.
- [ ] Kotlin.
- [ ] XML.
- [ ] Material 3.
- [ ] ViewBinding.
- [ ] Navigation Component.
- [ ] Tạo package structure.

## Phase 2 – Theme

- [ ] colors.xml.
- [ ] dimens.xml.
- [ ] strings.xml.
- [ ] themes.xml.
- [ ] Button style.
- [ ] Card style.
- [ ] TextInput style.
- [ ] Bottom Navigation style.

## Phase 3 – Navigation

- [ ] MainActivity.
- [ ] NavHostFragment.
- [ ] Navigation graph.
- [ ] BottomNavigation.
- [ ] 4 Fragment chính.

## Phase 4 – Trang chủ

- [ ] Header.
- [ ] Greeting.
- [ ] Stats cards.
- [ ] Recent items.
- [ ] Add button.
- [ ] Notification button.

## Phase 5 – Danh sách

- [ ] RecyclerView.
- [ ] ItemAdapter.
- [ ] Search.
- [ ] Filter.
- [ ] Empty state.
- [ ] Click item.

## Phase 6 – Chi tiết

- [ ] Image.
- [ ] Status.
- [ ] Category.
- [ ] GPIO.
- [ ] Price.
- [ ] Buy date.
- [ ] Notes.
- [ ] Switch.
- [ ] Delete.
- [ ] Change status.

## Phase 7 – Thêm vật dụng

- [ ] Image picker.
- [ ] Name.
- [ ] Category.
- [ ] Conditional GPIO field.
- [ ] Price.
- [ ] Date picker.
- [ ] Status.
- [ ] Notes.
- [ ] Validation.
- [ ] Save.
- [ ] Cancel.

## Phase 8 – Database

- [ ] Entity.
- [ ] DAO.
- [ ] Room Database.
- [ ] Repository.
- [ ] ViewModel.
- [ ] CRUD.

## Phase 9 – Thống kê

- [ ] Tổng vật dụng.
- [ ] Còn dùng.
- [ ] Hỏng.
- [ ] Tỉ lệ hoạt động.
- [ ] Thống kê category.
- [ ] Gợi ý bảo trì.

## Phase 10 – Cá nhân

- [ ] Profile.
- [ ] Cài đặt phòng.
- [ ] Thông báo.
- [ ] Backup.
- [ ] Đăng xuất.

## Phase 11 – Hoàn thiện UI

- [ ] So sánh Android với prototype.
- [ ] Căn chỉnh padding.
- [ ] Căn chỉnh font.
- [ ] Căn chỉnh màu.
- [ ] Căn chỉnh icon.
- [ ] Kiểm tra dark/light mode.
- [ ] Kiểm tra nhiều kích thước màn hình.

---

# 32. Checklist kiểm thử

## Navigation

- [ ] Trang chủ → Vật dụng.
- [ ] Trang chủ → Thống kê.
- [ ] Trang chủ → Cá nhân.
- [ ] Item → Detail.
- [ ] Detail → Back.
- [ ] Add → Save.
- [ ] Add → Cancel.

## CRUD

- [ ] Thêm item.
- [ ] Xem item.
- [ ] Sửa trạng thái.
- [ ] Toggle switch.
- [ ] Xóa item.

## Search

- [ ] Tìm theo tên.
- [ ] Tìm theo category.
- [ ] Search không có kết quả.
- [ ] Reset filter.

## Form

- [ ] Tên bắt buộc.
- [ ] Category.
- [ ] GPIO chỉ hiện khi Điện tử.
- [ ] Chọn ảnh.
- [ ] Chọn ngày.
- [ ] Chọn trạng thái.
- [ ] Ghi chú.

---

# 33. Dữ liệu mẫu ban đầu

Có thể tạo 5 item mẫu giống prototype:

| ID | Tên | Category | Status | Switch |
|---|---|---|---|---|
| 1 | Bàn học | Nội thất | Còn dùng | Không |
| 2 | Quạt điện | Điện tử | Còn dùng | Có |
| 3 | Đèn bàn | Điện tử | Hỏng | Có |
| 4 | Ghế xoay công thái học | Nội thất | Còn dùng | Không |
| 5 | Máy sấy tóc | Điện tử | Còn dùng | Có |

Các dữ liệu mẫu này được lấy trực tiếp từ `initialItems` trong prototype. fileciteturn0file0L44-L103

---

# 34. Quy tắc khi code Android

1. Không viết toàn bộ app trong `MainActivity`.
2. Không hard-code màu ở từng XML.
3. Không hard-code text ở từng View.
4. Dùng `strings.xml`.
5. Dùng `colors.xml`.
6. Dùng `dimens.xml`.
7. Dùng ViewBinding.
8. Dùng RecyclerView cho danh sách.
9. Dùng ViewModel cho state.
10. Dùng Room cho dữ liệu lâu dài.
11. Tách UI, data và logic.
12. Giữ tên biến dễ hiểu.
13. Không thay đổi luồng UX nếu chưa có lý do.
14. Ưu tiên giống prototype trước, tối ưu code sau.

---

# 35. Thứ tự tạo file thực tế

Nên tạo theo thứ tự:

```text
01. colors.xml
02. dimens.xml
03. strings.xml
04. themes.xml

05. Item.kt
06. ItemEntity.kt
07. ItemDao.kt
08. AppDatabase.kt
09. ItemRepository.kt

10. MainActivity.kt
11. MainViewModel.kt

12. nav_graph.xml

13. fragment_home.xml
14. HomeFragment.kt

15. item_item.xml
16. ItemAdapter.kt
17. fragment_items.xml
18. ItemsFragment.kt

19. fragment_detail.xml
20. ItemDetailFragment.kt

21. fragment_add_item.xml
22. AddItemFragment.kt

23. fragment_statistics.xml
24. StatisticsFragment.kt

25. fragment_profile.xml
26. ProfileFragment.kt
```

---

# 36. Mục tiêu phiên bản 1.0

Phiên bản đầu tiên phải hoàn thành:

```text
✓ 4 tab chính
✓ Hiển thị danh sách vật dụng
✓ Search
✓ Filter
✓ Xem chi tiết
✓ Thêm vật dụng
✓ Chọn ảnh
✓ Đổi trạng thái
✓ Bật/tắt thiết bị
✓ Xóa vật dụng
✓ Thống kê
✓ Profile
✓ Notification UI
✓ Room Database
✓ Navigation
✓ UI gần giống prototype
```

---

# 37. Mục tiêu mở rộng sau phiên bản 1.0

Có thể mở rộng:

```text
Backend API
     │
     ▼
Android App
     │
     ▼
IoT / ESP32
```

Các chức năng tiềm năng:

- Điều khiển thiết bị IoT.
- MQTT.
- Đồng bộ dữ liệu server.
- Đăng nhập.
- Nhiều người dùng.
- Nhiều phòng.
- Phân quyền.
- Push notification.
- Lịch bảo trì.
- Lịch sử bật/tắt thiết bị.
- Dashboard nâng cao.
- Sao lưu cloud.

---

# 38. Tiêu chí hoàn thành

App được xem là đạt khi:

- UI Android tương đồng prototype.
- Tất cả 4 tab hoạt động.
- Navigation không bị lỗi.
- Có thể thêm/xóa/xem vật dụng.
- Search và filter hoạt động.
- Trạng thái cập nhật đúng.
- Switch cập nhật đúng.
- Dữ liệu không mất khi đóng app.
- Form thêm vật dụng hoạt động.
- Thống kê lấy dữ liệu thật từ database.
- Không có crash khi nhập dữ liệu thông thường.
- Có thể chạy ổn định trên thiết bị Android thật.

---

# 39. Kết luận

Prototype hiện tại đã có sẵn một bộ UI/UX tương đối hoàn chỉnh. Vì vậy khi triển khai Android Studio, **không nên thiết kế lại từ đầu** mà nên coi prototype là bản đặc tả giao diện.

Ưu tiên triển khai theo thứ tự:

```text
Prototype
   ↓
Android XML UI
   ↓
Navigation
   ↓
RecyclerView
   ↓
ViewModel
   ↓
Room Database
   ↓
CRUD
   ↓
Search / Filter
   ↓
Statistics
   ↓
Notification
   ↓
Hoàn thiện UI
```

Mục tiêu là tạo một bản Android native có trải nghiệm tương đương prototype trước, sau đó mới mở rộng backend/IoT nếu cần.
