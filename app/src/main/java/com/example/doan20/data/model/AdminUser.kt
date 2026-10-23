package com.example.doan20.data.model

data class AdminUser(
    val id: String,
    val name: String,
    val initials: String,
    val email: String,
    val deviceCount: Int,
    val activeCount: Int,
    val brokenCount: Int
)
