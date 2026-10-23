package com.example.doan20.data.repository

import com.example.doan20.data.model.AdminUser

object AdminRepository {
    val users = listOf(
        AdminUser(
            id = "ND-001",
            name = "Minh Anh",
            initials = "MA",
            email = "minhanh@gmail.com",
            deviceCount = 25,
            activeCount = 20,
            brokenCount = 5
        ),
        AdminUser(
            id = "ND-002",
            name = "Hoàng Nam",
            initials = "HN",
            email = "hoangnam@gmail.com",
            deviceCount = 18,
            activeCount = 15,
            brokenCount = 3
        ),
        AdminUser(
            id = "ND-003",
            name = "Thu Hà",
            initials = "TH",
            email = "thuha@gmail.com",
            deviceCount = 12,
            activeCount = 10,
            brokenCount = 2
        ),
        AdminUser(
            id = "ND-004",
            name = "Đức Huy",
            initials = "DH",
            email = "duchuy@gmail.com",
            deviceCount = 8,
            activeCount = 6,
            brokenCount = 2
        )
    )

    const val TOTAL_DEVICES = 63
    const val TOTAL_ACTIVE = 51
    const val TOTAL_BROKEN = 12
}
