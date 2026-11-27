package com.example.doan20.model

data class UserModel(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "user",
    val roomName: String = "Phòng cá nhân",
    var userCode: String = "",
    var deviceCount: Int = 0,
    val createdAt: Any? = null
) {
    fun isAdmin(): Boolean {
        return role.trim().equals("admin", ignoreCase = true)
    }

    /**
     * Lấy 2 chữ cái viết tắt đại diện cho avatar (VD: Minh Anh -> MA)
     */
    fun getInitials(): String {
        val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "ND"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> {
                val firstChar = parts.first().firstOrNull()?.uppercase() ?: "N"
                val lastChar = parts.last().firstOrNull()?.uppercase() ?: "D"
                "$firstChar$lastChar"
            }
        }
    }
}
