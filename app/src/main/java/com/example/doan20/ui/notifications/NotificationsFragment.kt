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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.FragmentNotificationsBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNotifications()
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    updateUI(items)
                }
            }
        }
    }

    private fun updateUI(items: List<Item>) {
        val brokenItems = items.filter { it.status == ItemStatus.BROKEN }
        val turnedOnItems = items.filter { it.isOn && it.hasSwitch }

        binding.tvCountBroken.text = brokenItems.size.toString()
        binding.tvCountTurnedOn.text = turnedOnItems.size.toString()

        binding.layoutNotificationsList.removeAllViews()

        if (brokenItems.isEmpty() && turnedOnItems.isEmpty()) {
            binding.layoutEmptyNotificationsTab.visibility = View.VISIBLE
            binding.layoutNotificationsList.visibility = View.GONE
            return
        }

        binding.layoutEmptyNotificationsTab.visibility = View.GONE
        binding.layoutNotificationsList.visibility = View.VISIBLE

        // 1. Broken items alerts
        for (item in brokenItems) {
            val buyDateStr = item.buyDate?.trim().orEmpty()
            val notesStr = item.notes?.trim().orEmpty()
            val timeText = if (buyDateStr.isNotBlank()) "Ngày mua: $buyDateStr" else "Cần bảo trì"
            val descText = if (notesStr.isNotBlank()) notesStr else "Vật dụng này đang ở trạng thái Hỏng. Vui lòng kiểm tra hoặc sửa chữa."

            addCard(
                item = item,
                title = "⚠️ ${item.name} gặp sự cố hỏng",
                badge = "Cảnh báo",
                desc = descText,
                time = timeText,
                isBroken = true
            )
        }

        // 2. Active powered devices
        for (item in turnedOnItems) {
            val pinStr = item.pinConfig?.trim().orEmpty()
            val descText = if (pinStr.isNotBlank()) "Thiết bị đang hoạt động trên chân $pinStr." else "Thiết bị đang được cấp nguồn điện."

            addCard(
                item = item,
                title = "⚡ ${item.name} đang hoạt động",
                badge = "Đang bật",
                desc = descText,
                time = "Trực tuyến",
                isBroken = false
            )
        }
    }

    private fun addCard(
        item: Item,
        title: String,
        badge: String,
        desc: String,
        time: String,
        isBroken: Boolean
    ) {
        val density = resources.displayMetrics.density

        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * density).toInt()
            }
            radius = 16 * density
            strokeWidth = (1 * density).toInt()
            strokeColor = if (isBroken) Color.parseColor("#FECACA") else Color.parseColor("#BFDBFE")
            setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isBroken) R.color.status_broken_bg else R.color.primary_blue_container
                )
            )
            cardElevation = 0f
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Top Row: Title + Badge
        val topRow = RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val badgeId = View.generateViewId()

        val tvTitle = TextView(requireContext()).apply {
            text = title
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isBroken) R.color.status_broken else R.color.primary_blue
                )
            )
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.START_OF, badgeId)
            }
            layoutParams = lp
        }

        val tvBadge = TextView(requireContext()).apply {
            id = badgeId
            text = badge
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isBroken) R.color.status_broken else R.color.primary_blue
                )
            )
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val padH = (8 * density).toInt()
            val padV = (2 * density).toInt()
            setPadding(padH, padV, padH, padV)
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = lp
        }

        topRow.addView(tvTitle)
        topRow.addView(tvBadge)

        // Description
        val tvDesc = TextView(requireContext()).apply {
            text = desc
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            textSize = 13f
            setLineSpacing(2 * density, 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
        }

        // Bottom Action Bar: Time + Quick Action Button
        val bottomBar = RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
        }

        val tvTime = TextView(requireContext()).apply {
            text = time
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            textSize = 12f
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = lp
        }

        val btnAction = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = if (isBroken) "Xem chi tiết" else "Tắt nguồn"
            textSize = 12f
            val btnPadH = (12 * density).toInt()
            val btnPadV = (4 * density).toInt()
            setPadding(btnPadH, btnPadV, btnPadH, btnPadV)
            cornerRadius = (14 * density).toInt()
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = lp

            if (isBroken) {
                strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.status_broken)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.status_broken))
                setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("itemId", item.id)
                    }
                    findNavController().navigate(R.id.nav_detail, bundle)
                }
            } else {
                strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary_blue)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                setOnClickListener {
                    viewModel.setSwitchState(item.id, false)
                }
            }
        }

        bottomBar.addView(tvTime)
        bottomBar.addView(btnAction)

        container.addView(topRow)
        container.addView(tvDesc)
        container.addView(bottomBar)

        card.addView(container)
        binding.layoutNotificationsList.addView(card)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
