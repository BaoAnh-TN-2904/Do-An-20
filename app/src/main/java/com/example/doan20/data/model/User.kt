package com.example.doan20.data.model

enum class UserRole {
    USER,
    ADMIN
}

data class User(
    val email: String,
    val name: String,
    val role: UserRole,
    val roomName: String = "Phòng 402 • Căn hộ Harmony"
)
