package com.example.doan20.ui.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.doan20.R
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.BottomSheetNotificationsBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

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
        binding.btnCloseNotifications.setOnClickListener {
            dismiss()
        }
        populateNotifications()
    }

    private fun populateNotifications() {
        val items = viewModel.allItems.value
        binding.layoutNotificationsContainer.removeAllViews()

        // Filter broken items and active powered-on devices
        val brokenItems = items.filter { it.status == ItemStatus.BROKEN }
        val turnedOnItems = items.filter { it.isOn && it.hasSwitch }

        if (items.isEmpty() || (brokenItems.isEmpty() && turnedOnItems.isEmpty())) {
            binding.layoutEmptyNotifications.visibility = View.VISIBLE
            binding.layoutNotificationsContainer.visibility = View.GONE
            return
        }

        binding.layoutEmptyNotifications.visibility = View.GONE
        binding.layoutNotificationsContainer.visibility = View.VISIBLE

        // Add broken item cards
        for (item in brokenItems) {
            val buyDateStr = item.buyDate?.trim().orEmpty()
            val notesStr = item.notes?.trim().orEmpty()
            addNotificationCard(
                title = "${item.name} gặp sự cố hỏng hóc",
                time = if (buyDateStr.isNotBlank()) "Ngày mua: $buyDateStr" else "Cần bảo trì",
                desc = if (notesStr.isNotBlank()) notesStr else "Vật dụng này đang ở trạng thái Hỏng. Vui lòng kiểm tra hoặc bảo trì.",
                isBroken = true
            )
        }

        // Add active/turned-on item cards
        for (item in turnedOnItems) {
            addNotificationCard(
                title = "${item.name} đang hoạt động",
                time = "Đang bật",
                desc = if (item.pinConfig != null) "Thiết bị đang được cấp nguồn điện (${item.pinConfig})." else "Thiết bị đang bật nguồn.",
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
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
            }
            layoutParams = lp
        }

        val tvTime = TextView(requireContext()).apply {
            text = time
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textSize = 11f
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
            }
            layoutParams = lp
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
            ).apply {
                topMargin = (4 * resources.displayMetrics.density).toInt()
            }
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
        fun newInstance() = NotificationsBottomSheet()
    }
}
