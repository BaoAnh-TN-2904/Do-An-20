package com.example.doan20.model

import com.example.doan20.utils.UiHelper
import com.google.firebase.firestore.DocumentSnapshot

data class DeviceModel(
    var id: String = "",
    val devicedId: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "other",
    val gpioPin: Long = 0L,
    val imageBase64: String = "",
    val note: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "0",
    var status: String = "active", // "active" (Hoạt động), "broken" (Hỏng)
    val createdAt: String = "",
    var isOn: Boolean = false,
    var provisionStatus: String = "NONE", // "NONE", "PENDING", "READY", "FAILED"
    var provisionMessage: String = ""
) {
    fun isProvisionFailed(): Boolean {
        return provisionStatus.equals("FAILED", ignoreCase = true)
    }

    fun isElectronic(): Boolean {
        val key = category.trim().lowercase()
        return key == "electronics" || key == "electronic" || key == "điện tử"
    }

    fun isBroken(): Boolean {
        val s = status.trim().lowercase()
        return s == "broken" || s == "hỏng" || s == "hong"
    }

    fun isActive(): Boolean {
        return !isBroken()
    }

    fun getCategoryDisplayName(): String {
        return when (category.trim().lowercase()) {
            "electronics", "electronic", "điện tử" -> "Điện tử"
            "furniture", "nội thất" -> "Nội thất"
            "personal_items", "đồ dùng cá nhân" -> "Đồ dùng cá nhân"
            "other", "khác" -> "Khác"
            "lighting" -> "Thiết bị chiếu sáng"
            "climate" -> "Điều hòa & Làm mát"
            "security" -> "Thiết bị an ninh"
            "appliance" -> "Thiết bị gia dụng"
            else -> category.ifBlank { "Khác" }
        }
    }

    fun getPriceLong(): Long {
        return purchasePrice.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    }

    fun getFormattedPrice(): String {
        val priceLong = getPriceLong()
        return if (priceLong > 0L) "${UiHelper.formatNumber(priceLong)}đ" else "Chưa cập nhật"
    }

    fun getFormattedDate(): String {
        return UiHelper.parseDateToDisplay(purchaseDate) ?: "Chưa cập nhật"
    }

    /**
     * Chuyển về các trường lưu lên collection "devices" của Firestore (giữ nguyên trạng thái isOn)
     */
    fun toFirestoreMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "category" to toFirestoreCategory(category),
            "createdAt" to createdAt.ifBlank { System.currentTimeMillis().toString() },
            "devicedId" to devicedId.ifBlank { id },
            "gpioPin" to gpioPin,
            "imageBase64" to imageBase64,
            "isOn" to isOn,
            "name" to name,
            "note" to note,
            "purchaseDate" to purchaseDate,
            "purchasePrice" to purchasePrice,
            "status" to status,
            "userId" to userId,
            "provisionStatus" to provisionStatus,
            "provisionMessage" to provisionMessage
        )
    }

    companion object {
        fun toFirestoreCategory(displayOrKey: String): String {
            return when (displayOrKey.trim().lowercase()) {
                "điện tử", "electronics", "electronic" -> "electronics"
                "nội thất", "furniture" -> "furniture"
                "đồ dùng cá nhân", "personal_items" -> "personal_items"
                "khác", "other" -> "other"
                else -> displayOrKey.ifBlank { "other" }
            }
        }

        fun fromFirestore(doc: DocumentSnapshot): DeviceModel {
            val priceStr = when (val raw = doc.get("purchasePrice") ?: doc.get("price")) {
                is Number -> raw.toLong().toString()
                is String -> raw.replace("[^0-9]".toRegex(), "").ifBlank { "0" }
                else -> "0"
            }

            val dateStr = when (val raw = doc.get("purchaseDate") ?: doc.get("buyDate")) {
                is Number -> raw.toLong().let { if (it > 0L) it.toString() else "" }
                is com.google.firebase.Timestamp -> raw.toDate().time.toString()
                is String -> {
                    val trimmed = raw.trim()
                    if (trimmed.all { it.isDigit() }) trimmed
                    else UiHelper.parseDisplayDateToMillis(trimmed).let { if (it > 0L) it.toString() else "" }
                }
                else -> ""
            }

            val createdAtStr = when (val raw = doc.get("createdAt")) {
                is Number -> raw.toLong().toString()
                is String -> raw
                else -> ""
            }

            val rawImage = doc.getString("imageBase64")?.takeIf { it.isNotBlank() }
                ?: doc.getString("imageUrl")?.takeIf { it.isNotBlank() }
                ?: ""
            val cleanImage = if (rawImage.trim().startsWith("http", ignoreCase = true)) "" else rawImage

            val isOnVal = doc.getBoolean("isOn")
                ?: (doc.getString("state")?.equals("ON", ignoreCase = true) == true)

            return DeviceModel(
                id = doc.id,
                devicedId = doc.getString("devicedId") ?: doc.getString("deviceId") ?: doc.id,
                userId = doc.getString("userId") ?: "",
                name = doc.getString("name") ?: "",
                category = doc.getString("category") ?: "other",
                gpioPin = doc.getLong("gpioPin") ?: 0L,
                imageBase64 = cleanImage,
                note = doc.getString("note") ?: doc.getString("notes") ?: "",
                purchaseDate = dateStr,
                purchasePrice = priceStr,
                status = doc.getString("status") ?: "active",
                createdAt = createdAtStr,
                isOn = isOnVal,
                provisionStatus = doc.getString("provisionStatus") ?: "NONE",
                provisionMessage = doc.getString("provisionMessage") ?: ""
            )
        }
    }
}
