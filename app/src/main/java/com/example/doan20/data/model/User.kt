package com.example.doan20.data.model

enum class UserRole {
    USER,
    ADMIN
}

data class User(
    val username: String = "",
    val name: String = "",
    val role: UserRole = UserRole.USER,
    val roomName: String = "Phòng 402 • Căn hộ Harmony",
    val email: String = "",
    val uid: String = ""
) {
    val initials: String get() = getInitials(name)
}

fun getInitials(name: String): String {
    val words = name.trim().split(" ")
    val result = if (words.size >= 2) {
        "${words.first().take(1)}${words.last().take(1)}".uppercase()
    } else {
        name.take(2).uppercase()
    }
    return if (result.isBlank()) "ND" else result
}
