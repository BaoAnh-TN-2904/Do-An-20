package com.example.doan20.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.doan20.R
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.ItemItemBinding
import com.example.doan20.ui.applyStatusBadge

class ItemAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onStatusClick: (Item) -> Unit,
    private val onSwitchToggle: (Item, Boolean) -> Unit
) : ListAdapter<Item, ItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    companion object {
        private const val PAYLOAD_SWITCH = "PAYLOAD_SWITCH"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_SWITCH)) {
            holder.bindSwitchOnly(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ItemViewHolder(
        private val binding: ItemItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: Item? = null

        init {
            binding.cardItem.setOnClickListener {
                currentItem?.let { onItemClick(it) }
            }

            binding.tvStatusBadge.setOnClickListener {
                currentItem?.let { onStatusClick(it) }
            }

            // Only trigger callback when the user physically presses / toggles the switch
            binding.switchPower.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    currentItem?.let { item ->
                        if (item.isOn != isChecked) {
                            onSwitchToggle(item, isChecked)
                        }
                    }
                }
            }
        }

        fun bind(item: Item) {
            currentItem = item
            val context = binding.root.context

            // Title & Category
            binding.tvItemName.text = item.name
            binding.tvCategory.text = item.category

            // Item Image
            binding.ivItemImage.setImageResource(item.getDrawableRes())

            // Status Badge & Broken Warning
            binding.tvStatusBadge.applyStatusBadge(item.status)
            binding.layoutBrokenWarning.visibility = if (item.status == ItemStatus.BROKEN) View.VISIBLE else View.GONE

            // Power switch control for electronic items
            if (item.hasSwitch) {
                binding.switchPower.visibility = View.VISIBLE
                binding.switchPower.trackDecorationDrawable = null
                binding.switchPower.thumbIconDrawable = null
                binding.switchPower.trackTintList = ContextCompat.getColorStateList(context, R.color.switch_track_selector)
                binding.switchPower.thumbTintList = ContextCompat.getColorStateList(context, R.color.switch_thumb_selector)

                // Only set if different to prevent interrupting running animation
                if (binding.switchPower.isChecked != item.isOn) {
                    binding.switchPower.isChecked = item.isOn
                }
            } else {
                binding.switchPower.visibility = View.GONE
            }
        }

        fun bindSwitchOnly(item: Item) {
            currentItem = item
            if (item.hasSwitch) {
                if (binding.switchPower.isChecked != item.isOn) {
                    binding.switchPower.isChecked = item.isOn
                }
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Item, newItem: Item): Any? {
            return if (oldItem.id == newItem.id && oldItem.copy(isOn = newItem.isOn) == newItem) {
                PAYLOAD_SWITCH
            } else {
                null
            }
        }
    }
}
