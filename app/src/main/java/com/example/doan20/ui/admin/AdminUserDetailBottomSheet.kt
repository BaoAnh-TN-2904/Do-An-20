package com.example.doan20.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.AdminUser
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.data.repository.AdminRepository
import com.example.doan20.databinding.BottomSheetAdminUserDetailBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch

class AdminUserDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAdminUserDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString(ARG_USER_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAdminUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = AdminRepository.users.find { it.id == userId } ?: AdminRepository.users[0]
        bindUser(user)
    }

    private fun bindUser(user: AdminUser) {
        binding.tvSheetUserName.text = user.name
        binding.tvSheetUserEmail.text = user.email
        binding.tvSheetUserCode.text = "${user.id} · Người dùng"
        binding.tvSheetUserInitials.text = user.initials

        binding.tvSheetDeviceTotal.text = user.deviceCount.toString()
        binding.tvSheetDeviceActive.text = user.activeCount.toString()
        binding.tvSheetDeviceBroken.text = user.brokenCount.toString()

        binding.tvSheetDeviceSectionTitle.text = "Thiết bị của ${user.name}"

        // Populate sample device cards for this user
        val items = viewModel.allItems.value
        binding.layoutSheetDeviceList.removeAllViews()

        val displayItems = items.take(4)
        for (item in displayItems) {
            val itemView = layoutInflater.inflate(R.layout.item_item, binding.layoutSheetDeviceList, false)
            val ivImage = itemView.findViewById<ImageView>(R.id.ivItemImage)
            val tvName = itemView.findViewById<TextView>(R.id.tvItemName)
            val tvCategory = itemView.findViewById<TextView>(R.id.tvCategory)
            val tvStatusBadge = itemView.findViewById<TextView>(R.id.tvStatusBadge)
            val switchPower = itemView.findViewById<MaterialSwitch>(R.id.switchPower)

            tvName.text = item.name
            tvCategory.text = "${item.category} · TB-00${item.id}"

            val drawableRes = when (item.imageType.lowercase()) {
                "desk" -> R.drawable.item_desk
                "bed" -> R.drawable.item_bed
                "fan" -> R.drawable.item_fan
                "lamp" -> R.drawable.item_lamp
                "cooker" -> R.drawable.item_cooker
                else -> R.drawable.ic_item_default
            }
            ivImage.setImageResource(drawableRes)

            when (item.status) {
                ItemStatus.ACTIVE -> {
                    tvStatusBadge.text = "● Còn dùng"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active_pill)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_active))
                }
                ItemStatus.BROKEN -> {
                    tvStatusBadge.text = "● Hỏng"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_broken_pill)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_broken))
                }
            }

            if (item.hasSwitch) {
                switchPower.visibility = View.VISIBLE
                switchPower.isUseMaterialThemeColors = false
                switchPower.trackDecorationDrawable = null
                switchPower.thumbIconDrawable = null
                switchPower.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.switch_track_selector)
                switchPower.thumbTintList = ContextCompat.getColorStateList(requireContext(), R.color.switch_thumb_selector)
                switchPower.setOnCheckedChangeListener(null)
                switchPower.isChecked = item.isOn
                switchPower.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.setSwitchState(item.id, isChecked)
                }
            } else {
                switchPower.visibility = View.GONE
            }

            itemView.setOnClickListener {
                dismiss()
                findNavController().navigate(
                    R.id.action_admin_to_detail,
                    bundleOf("itemId" to item.id)
                )
            }

            binding.layoutSheetDeviceList.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AdminUserDetailBottomSheet"
        private const val ARG_USER_ID = "arg_user_id"

        fun newInstance(userId: String): AdminUserDetailBottomSheet {
            return AdminUserDetailBottomSheet().apply {
                arguments = bundleOf(ARG_USER_ID to userId)
            }
        }
    }
}
