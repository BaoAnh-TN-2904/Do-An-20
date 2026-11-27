package com.example.doan20.admin

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan20.R
import com.example.doan20.databinding.ItemAdminDeviceBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.utils.UiHelper

class AdminDeviceAdapter(
    private var deviceList: List<DeviceModel>,
    private val onItemClick: ((DeviceModel) -> Unit)? = null,
    private val onSwitchToggle: ((DeviceModel, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<AdminDeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(val binding: ItemAdminDeviceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding =
            ItemAdminDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        with(holder.binding) {
            tvDeviceName.text = device.name.ifBlank { "Thiết bị #${position + 1}" }
            tvDeviceCategory.text = device.getCategoryDisplayName()

            // Tải ảnh từ Base64 (hỗ trợ cả SVG và JPEG/PNG)
            ivDeviceImage.scaleType = if (device.imageBase64.isBlank()) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
            UiHelper.loadBase64Image(ivDeviceImage, device.imageBase64, R.drawable.app_logo)

            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
            }

            if (device.isBroken()) {
                layoutStatusBadge.setBackgroundResource(R.drawable.bg_admin_badge_broken)
                tvDeviceStatus.text = "Hỏng"
                tvDeviceStatus.setTextColor(Color.parseColor("#DC2626"))
                dotDrawable.setColor(Color.parseColor("#DC2626"))
            } else {
                layoutStatusBadge.setBackgroundResource(R.drawable.bg_admin_badge_active)
                tvDeviceStatus.text = "Hoạt động"
                tvDeviceStatus.setTextColor(Color.parseColor("#16A34A"))
                dotDrawable.setColor(Color.parseColor("#16A34A"))
            }
            viewStatusDot.background = dotDrawable

            if (device.isElectronic()) {
                switchDeviceStatus.visibility = View.VISIBLE
                switchDeviceStatus.setOnCheckedChangeListener(null)
                switchDeviceStatus.isChecked = device.isOn
                switchDeviceStatus.setOnCheckedChangeListener { _, isChecked ->
                    device.isOn = isChecked
                    onSwitchToggle?.invoke(device, isChecked)
                }
            } else {
                switchDeviceStatus.visibility = View.GONE
                switchDeviceStatus.setOnCheckedChangeListener(null)
            }

            root.setOnClickListener {
                onItemClick?.invoke(device)
            }
        }
    }

    override fun getItemCount(): Int = deviceList.size

    fun updateData(newList: List<DeviceModel>) {
        deviceList = newList
        notifyDataSetChanged()
    }
}
