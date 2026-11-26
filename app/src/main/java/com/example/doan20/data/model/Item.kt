package com.example.doan20.data.model

import com.example.doan20.R
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Item(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val imageType: String = "default",
    val imageUri: String? = null,
    val status: ItemStatus = ItemStatus.ACTIVE,
    @get:PropertyName("hasSwitch")
    val hasSwitch: Boolean = false,
    @get:PropertyName("isOn")
    val isOn: Boolean = false,
    val price: Long? = null,
    val buyDate: String? = null,
    val pinConfig: String? = null,
    val notes: String? = null
) : Serializable {
    fun getDrawableRes(): Int = resolveItemDrawable(imageType, name)
}

fun resolveItemDrawable(imageType: String, name: String = ""): Int {
    return when (imageType.lowercase()) {
        "desk" -> R.drawable.item_desk
        "bed" -> R.drawable.item_bed
        "fan" -> R.drawable.item_fan
        "lamp" -> R.drawable.item_lamp
        "cooker" -> R.drawable.item_cooker
        "chair" -> R.drawable.ic_item_chair
        "hairdryer" -> R.drawable.ic_item_hairdryer
        else -> {
            val lower = name.lowercase()
            when {
                lower.contains("bàn") -> R.drawable.item_desk
                lower.contains("giường") || lower.contains("nệm") -> R.drawable.item_bed
                lower.contains("quạt") -> R.drawable.item_fan
                lower.contains("đèn") -> R.drawable.item_lamp
                lower.contains("nồi") || lower.contains("cơm") -> R.drawable.item_cooker
                lower.contains("ghế") -> R.drawable.ic_item_chair
                lower.contains("sấy") -> R.drawable.ic_item_hairdryer
                else -> R.drawable.ic_item_default
            }
        }
    }
}
