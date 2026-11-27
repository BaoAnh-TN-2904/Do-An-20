package com.example.doan20.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.doan20.R
import com.example.doan20.databinding.ItemItemBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.utils.UiHelper
import com.example.doan20.utils.applyDeviceStatusBadge

class ItemAdapter(
    private var deviceList: List<DeviceModel>,
    private val onItemClick: (DeviceModel) -> Unit,
    private val onStatusClick: (DeviceModel) -> Unit,
    private val onSwitchToggle: (DeviceModel, Boolean) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(val binding: ItemItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val device = deviceList[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.tvItemName.text = device.name
        binding.tvCategory.text = device.getCategoryDisplayName()

        binding.ivItemImage.scaleType = if (device.imageBase64.isBlank()) {
            ImageView.ScaleType.FIT_CENTER
        } else {
            ImageView.ScaleType.CENTER_CROP
        }
        UiHelper.loadBase64Image(binding.ivItemImage, device.imageBase64, R.drawable.app_logo)

        binding.tvStatusBadge.applyDeviceStatusBadge(device)
        binding.layoutBrokenWarning.visibility = if (device.isBroken()) View.VISIBLE else View.GONE

        if (device.isElectronic()) {
            binding.switchPower.visibility = View.VISIBLE
            binding.switchPower.trackDecorationDrawable = null
            binding.switchPower.thumbIconDrawable = null
            binding.switchPower.trackTintList = ContextCompat.getColorStateList(context, R.color.switch_track_selector)
            binding.switchPower.thumbTintList = ContextCompat.getColorStateList(context, R.color.switch_thumb_selector)
            binding.switchPower.setOnCheckedChangeListener(null)
            binding.switchPower.isChecked = device.isOn
            binding.switchPower.setOnCheckedChangeListener { _, isChecked ->
                if (device.isOn != isChecked) {
                    onSwitchToggle(device, isChecked)
                }
            }
        } else {
            binding.switchPower.visibility = View.GONE
            binding.switchPower.setOnCheckedChangeListener(null)
        }

        binding.cardItem.setOnClickListener { onItemClick(device) }
        binding.tvStatusBadge.setOnClickListener { onStatusClick(device) }
    }

    override fun getItemCount(): Int = deviceList.size

    fun updateData(newList: List<DeviceModel>) {
        deviceList = newList
        notifyDataSetChanged()
    }
}
