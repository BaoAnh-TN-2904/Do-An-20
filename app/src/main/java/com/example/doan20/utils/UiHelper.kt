package com.example.doan20.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Base64
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.caverock.androidsvg.SVG
import com.example.doan20.R
import com.example.doan20.model.DeviceModel
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UiHelper {
    private val base64BitmapCache = object : LruCache<String, Bitmap>(32) {}

    fun applySystemBarPadding(view: View) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialLeft,
                initialTop + systemBars.top,
                initialRight,
                initialBottom + systemBars.bottom
            )
            insets
        }
    }

    fun formatNumber(value: Long): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
        }
        return DecimalFormat("#,###", symbols).format(value)
    }

    fun parseDateToDisplay(rawDate: Any?): String? {
        return when (rawDate) {
            is com.google.firebase.Timestamp ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(rawDate.toDate())
            is Number -> {
                val millis = rawDate.toLong()
                if (millis > 0L) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis)) else null
            }
            is String -> {
                val trimmed = rawDate.trim()
                when {
                    trimmed.isBlank() -> null
                    trimmed.all { it.isDigit() } -> {
                        val millis = trimmed.toLongOrNull() ?: 0L
                        if (millis > 0L) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis)) else null
                    }
                    else -> trimmed
                }
            }
            else -> null
        }
    }

    fun parseDisplayDateToMillis(dateStr: String?): Long {
        val trimmed = dateStr?.trim().orEmpty()
        if (trimmed.isBlank()) return 0L
        if (trimmed.all { it.isDigit() }) {
            return trimmed.toLongOrNull() ?: 0L
        }
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                isLenient = false
            }
            sdf.parse(trimmed)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun loadBase64Image(
        imageView: ImageView,
        raw: String?,
        @DrawableRes fallbackRes: Int = R.drawable.app_logo
    ) {
        imageView.imageTintList = null
        val value = raw?.trim().orEmpty()
        if (value.isBlank() || value.startsWith("http", ignoreCase = true)) {
            imageView.setImageResource(fallbackRes)
            return
        }

        val cacheKey = "${value.length}_${value.hashCode()}"
        base64BitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) {
                imageView.setImageBitmap(cached)
                return
            }
        }

        val cleanBase64 = value.substringAfter("base64,", value).trim()
        val decodedBytes = try {
            Base64.decode(cleanBase64, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }

        if (decodedBytes == null || decodedBytes.isEmpty()) {
            imageView.setImageResource(fallbackRes)
            return
        }

        val isSvg = value.startsWith("data:image/svg+xml", ignoreCase = true) ||
            String(decodedBytes.take(64).toByteArray(), StandardCharsets.UTF_8)
                .trimStart()
                .startsWith("<svg", ignoreCase = true)

        val bitmap = if (isSvg) {
            renderSvgBytesToBitmap(decodedBytes)
        } else {
            try {
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (_: Exception) {
                null
            }
        }

        if (bitmap != null) {
            base64BitmapCache.put(cacheKey, bitmap)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(fallbackRes)
        }
    }

    private fun renderSvgBytesToBitmap(svgBytes: ByteArray): Bitmap? {
        return try {
            val svgText = String(svgBytes, StandardCharsets.UTF_8)
            val svg = SVG.getFromString(svgText)
            val size = 256
            val pad = 28f
            val drawSize = size - pad * 2
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(pad, pad)
            svg.setDocumentWidth(drawSize)
            svg.setDocumentHeight(drawSize)
            svg.renderToCanvas(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}

fun TextView.applyDeviceStatusBadge(device: DeviceModel) {
    val context = this.context
    if (device.isBroken()) {
        text = "● Hỏng"
        setBackgroundResource(R.drawable.bg_badge_broken_pill)
        setTextColor(ContextCompat.getColor(context, R.color.status_broken))
    } else {
        text = "● Hoạt động"
        setBackgroundResource(R.drawable.bg_badge_active_pill)
        setTextColor(ContextCompat.getColor(context, R.color.status_active))
    }
}
