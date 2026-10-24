package com.example.doan20.ui

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.doan20.R
import com.example.doan20.data.model.ItemStatus

/**
 * Shared UI styling extensions to avoid code duplication across adapters and screens
 */
fun TextView.applyStatusBadge(status: ItemStatus) {
    val context = this.context
    when (status) {
        ItemStatus.ACTIVE -> {
            text = "● Còn dùng"
            setBackgroundResource(R.drawable.bg_badge_active_pill)
            setTextColor(ContextCompat.getColor(context, R.color.status_active))
        }
        ItemStatus.BROKEN -> {
            text = "● Hỏng"
            setBackgroundResource(R.drawable.bg_badge_broken_pill)
            setTextColor(ContextCompat.getColor(context, R.color.status_broken))
        }
    }
}
