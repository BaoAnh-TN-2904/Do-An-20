package com.example.doan20.data.model

enum class ItemStatus {
    ACTIVE,
    BROKEN;

    val displayName: String
        get() = when (this) {
            ACTIVE -> "Còn dùng"
            BROKEN -> "Hỏng"
        }
}
