package com.example.doan20.data.repository

import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object ItemRepository {
    private val initialItems = listOf(
        Item(
            id = "1",
            name = "Bàn học",
            category = "Nội thất",
            imageType = "desk",
            status = ItemStatus.ACTIVE,
            hasSwitch = false,
            isOn = false,
            price = 1200000L,
            buyDate = "15/01/2024",
            pinConfig = null,
            notes = "Bàn học gỗ công nghiệp phong cách tối giản, đặt cạnh cửa sổ."
        ),
        Item(
            id = "2",
            name = "Giường",
            category = "Nội thất",
            imageType = "bed",
            status = ItemStatus.ACTIVE,
            hasSwitch = false,
            isOn = false,
            price = 3500000L,
            buyDate = "10/01/2024",
            pinConfig = null,
            notes = "Giường ngủ khung gỗ sồi tự nhiên kèm nệm lò xo êm ái."
        ),
        Item(
            id = "3",
            name = "Quạt điện",
            category = "Điện tử",
            imageType = "fan",
            status = ItemStatus.ACTIVE,
            hasSwitch = true,
            isOn = true,
            price = 380000L,
            buyDate = "20/03/2024",
            pinConfig = "OUTPUT - GPIO 14",
            notes = "Quạt đứng Senko, điều khiển từ xa qua ESP32 module relay."
        ),
        Item(
            id = "4",
            name = "Đèn bàn",
            category = "Điện tử",
            imageType = "lamp",
            status = ItemStatus.BROKEN,
            hasSwitch = true,
            isOn = false,
            price = 250000L,
            buyDate = "15/09/2025",
            pinConfig = "OUTPUT - GPIO 16",
            notes = "Đèn không sáng, cần kiểm tra bóng và dây nguồn."
        ),
        Item(
            id = "5",
            name = "Nồi cơm điện",
            category = "Điện tử",
            imageType = "cooker",
            status = ItemStatus.ACTIVE,
            hasSwitch = true,
            isOn = false,
            price = 650000L,
            buyDate = "15/04/2024",
            pinConfig = "OUTPUT - GPIO 18",
            notes = "Nồi cơm điện tử mini 1.2L có chế độ giữ ấm tự động."
        )
    )

    private val _items = MutableStateFlow<List<Item>>(initialItems)
    val itemsFlow: StateFlow<List<Item>> = _items.asStateFlow()

    fun getAllItems(): List<Item> = _items.value

    fun getItemById(id: String): Item? {
        return _items.value.find { it.id == id }
    }

    fun addItem(item: Item) {
        val newItem = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        _items.value = listOf(newItem) + _items.value
    }

    fun updateItem(updatedItem: Item) {
        _items.value = _items.value.map { item ->
            if (item.id == updatedItem.id) updatedItem else item
        }
    }

    fun deleteItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun toggleSwitch(id: String) {
        _items.value = _items.value.map { item ->
            if (item.id == id && item.hasSwitch) {
                item.copy(isOn = !item.isOn)
            } else {
                item
            }
        }
    }

    fun setSwitchState(id: String, isOn: Boolean) {
        _items.value = _items.value.map { item ->
            if (item.id == id && item.hasSwitch) {
                item.copy(isOn = isOn)
            } else {
                item
            }
        }
    }

    fun toggleStatus(id: String) {
        _items.value = _items.value.map { item ->
            if (item.id == id) {
                val nextStatus = if (item.status == ItemStatus.ACTIVE) ItemStatus.BROKEN else ItemStatus.ACTIVE
                item.copy(status = nextStatus)
            } else {
                item
            }
        }
    }
}
