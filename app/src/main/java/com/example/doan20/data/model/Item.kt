package com.example.doan20.data.model

import java.io.Serializable

data class Item(
    val id: String,
    val name: String,
    val category: String,
    val imageType: String = "default",
    val imageUri: String? = null,
    val status: ItemStatus = ItemStatus.ACTIVE,
    val hasSwitch: Boolean = false,
    val isOn: Boolean = false,
    val price: Long? = null,
    val buyDate: String? = null,
    val pinConfig: String? = null,
    val notes: String? = null
) : Serializable
