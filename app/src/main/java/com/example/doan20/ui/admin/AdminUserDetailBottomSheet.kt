package com.example.doan20.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.AdminUser
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.data.mqtt.MqttManager
import com.example.doan20.data.repository.AdminRepository
import com.example.doan20.databinding.BottomSheetAdminUserDetailBinding
import com.example.doan20.ui.applyStatusBadge
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminUserDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAdminUserDetailBinding? = null
    private val binding get() = _binding!!

    private var userId: String? = null
    private var currentUser: AdminUser? = null
    private var itemsListener: ListenerRegistration? = null

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

        val targetId = userId.orEmpty()
        val user = AdminRepository.users.find { it.id == targetId }
        if (user != null) {
            currentUser = user
            bindUser(user)
            loadUserItems(targetId)
        } else {
            dismiss()
        }
    }

    private fun bindUser(user: AdminUser) {
        binding.tvSheetUserName.text = user.name
        binding.tvSheetUserEmail.text = user.email
        binding.tvSheetUserCode.text = "@${user.id} · Người dùng"
        binding.tvSheetUserInitials.text = user.initials

        binding.tvSheetDeviceTotal.text = user.deviceCount.toString()
        binding.tvSheetDeviceActive.text = user.activeCount.toString()
        binding.tvSheetDeviceBroken.text = user.brokenCount.toString()

        binding.tvSheetDeviceSectionTitle.text = "Thiết bị của ${user.name}"
    }

    private fun loadUserItems(targetUserId: String) {
        binding.layoutSheetDeviceList.removeAllViews()

        try {
            itemsListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(targetUserId)
                .collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (_binding == null) return@addSnapshotListener
                    if (error != null) {
                        Log.w("AdminUserDetail", "Error loading items: ${error.message}")
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents?.mapNotNull { doc ->
                        val item = doc.toObject(Item::class.java)
                        item?.copy(id = doc.id)
                    } ?: emptyList()

                    populateDevices(targetUserId, items)
                }
        } catch (e: Exception) {
            Log.e("AdminUserDetail", "Failed to listen to items", e)
        }
    }

    private fun populateDevices(targetUserId: String, items: List<Item>) {
        if (_binding == null) return
        binding.layoutSheetDeviceList.removeAllViews()

        // Update stats cards in real time
        val total = items.size
        val active = items.count { it.status == ItemStatus.ACTIVE }
        val broken = items.count { it.status == ItemStatus.BROKEN }
        binding.tvSheetDeviceTotal.text = total.toString()
        binding.tvSheetDeviceActive.text = active.toString()
        binding.tvSheetDeviceBroken.text = broken.toString()

        if (items.isEmpty()) {
            val tvEmpty = TextView(requireContext()).apply {
                text = "Người dùng này chưa thêm thiết bị nào."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            binding.layoutSheetDeviceList.addView(tvEmpty)
            return
        }

        for (item in items) {
            val itemView = layoutInflater.inflate(R.layout.item_item, binding.layoutSheetDeviceList, false)
            val ivImage = itemView.findViewById<ImageView>(R.id.ivItemImage)
            val tvName = itemView.findViewById<TextView>(R.id.tvItemName)
            val tvCategory = itemView.findViewById<TextView>(R.id.tvCategory)
            val tvStatusBadge = itemView.findViewById<TextView>(R.id.tvStatusBadge)
            val switchPower = itemView.findViewById<MaterialSwitch>(R.id.switchPower)

            tvName.text = item.name
            tvCategory.text = "${item.category} · TB-${item.id.takeLast(4)}"

            ivImage.setImageResource(item.getDrawableRes())
            tvStatusBadge.applyStatusBadge(item.status)

            if (item.hasSwitch) {
                switchPower.visibility = View.VISIBLE
                switchPower.trackDecorationDrawable = null
                switchPower.thumbIconDrawable = null
                switchPower.trackTintList = ContextCompat.getColorStateList(requireContext(), R.color.switch_track_selector)
                switchPower.thumbTintList = ContextCompat.getColorStateList(requireContext(), R.color.switch_thumb_selector)
                switchPower.setOnCheckedChangeListener(null)
                switchPower.isChecked = item.isOn
                switchPower.setOnCheckedChangeListener { _, isChecked ->
                    // 1. MQTT with JSON containing GPIO
                    val gpioPin = com.example.doan20.data.repository.ItemRepository.extractGpioPin(item.pinConfig)
                    val payload = org.json.JSONObject().apply {
                        put("id", item.id)
                        put("state", if (isChecked) 1 else 0)
                        if (gpioPin != null) put("gpio", gpioPin)
                    }.toString()
                    MqttManager.publish("doan20/$targetUserId/device/${item.id}/switch", payload)
                    // 2. Firestore
                    try {
                        FirebaseFirestore.getInstance().collection("users")
                            .document(targetUserId)
                            .collection("items")
                            .document(item.id)
                            .update(
                                mapOf(
                                    "isOn" to isChecked,
                                    "on" to isChecked
                                )
                            )
                    } catch (e: Exception) {
                        Log.e("AdminUserDetail", "Failed to update switch", e)
                    }
                }
            } else {
                switchPower.visibility = View.GONE
            }

            itemView.setOnClickListener {
                dismiss()
                findNavController().navigate(
                    R.id.action_admin_to_detail,
                    bundleOf(
                        "itemId" to item.id,
                        "userId" to targetUserId,
                        "ownerName" to (currentUser?.name ?: "Người dùng"),
                        "ownerEmail" to (currentUser?.email ?: "")
                    )
                )
            }

            binding.layoutSheetDeviceList.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        itemsListener?.remove()
        itemsListener = null
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
