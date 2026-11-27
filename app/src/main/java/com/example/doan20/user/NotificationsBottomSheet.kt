package com.example.doan20.user

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.doan20.R
import com.example.doan20.databinding.BottomSheetNotificationsBinding
import com.example.doan20.utils.UiHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCloseNotifications.setOnClickListener { dismiss() }
        populateNotifications()
    }

    private fun populateNotifications() {
        val items = (activity as? MainActivity)?.deviceList ?: emptyList()
        binding.layoutNotificationsContainer.removeAllViews()

        val brokenItems = items.filter { it.isBroken() }
        val turnedOnItems = items.filter { it.isOn && it.isElectronic() }

        if (items.isEmpty() || (brokenItems.isEmpty() && turnedOnItems.isEmpty())) {
            binding.layoutEmptyNotifications.visibility = View.VISIBLE
            binding.layoutNotificationsContainer.visibility = View.GONE
            return
        }

        binding.layoutEmptyNotifications.visibility = View.GONE
        binding.layoutNotificationsContainer.visibility = View.VISIBLE

        for (device in brokenItems) {
            val dateText = UiHelper.parseDateToDisplay(device.purchaseDate)
            val notesStr = device.note.trim()
            addNotificationCard(
                title = "${device.name} gặp sự cố hỏng hóc",
                time = if (!dateText.isNullOrBlank()) "Ngày mua: $dateText" else "Cần bảo trì",
                desc = notesStr.ifBlank { "Vật dụng này đang ở trạng thái Hỏng. Vui lòng kiểm tra hoặc bảo trì." },
                isBroken = true
            )
        }

        for (device in turnedOnItems) {
            addNotificationCard(
                title = "${device.name} đang hoạt động",
                time = "Đang bật",
                desc = if (device.gpioPin > 0) "Thiết bị đang được cấp nguồn điện (GPIO ${device.gpioPin})." else "Thiết bị đang bật nguồn.",
                isBroken = false
            )
        }
    }

    private fun addNotificationCard(title: String, time: String, desc: String, isBroken: Boolean) {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * resources.displayMetrics.density).toInt()
            }
            radius = 12 * resources.displayMetrics.density
            strokeWidth = (1 * resources.displayMetrics.density).toInt()
            strokeColor = if (isBroken) Color.parseColor("#FECACA") else Color.parseColor("#BFDBFE")
            setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isBroken) R.color.stat_broken_bg else R.color.stat_total_bg
                )
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val topRow = RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTitle = TextView(requireContext()).apply {
            text = title
            setTextColor(ContextCompat.getColor(requireContext(), if (isBroken) R.color.status_broken else R.color.primary_blue))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.ALIGN_PARENT_START) }
        }

        val tvTime = TextView(requireContext()).apply {
            text = time
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textSize = 11f
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.ALIGN_PARENT_END) }
        }

        topRow.addView(tvTitle)
        topRow.addView(tvTime)

        val tvDesc = TextView(requireContext()).apply {
            text = desc
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            textSize = 12f
            setLineSpacing(2 * resources.displayMetrics.density, 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * resources.displayMetrics.density).toInt() }
        }

        root.addView(topRow)
        root.addView(tvDesc)
        card.addView(root)
        binding.layoutNotificationsContainer.addView(card)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NotificationsBottomSheet"
    }
}
