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
import java.text.NumberFormat
import java.util.Locale

class ItemAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onStatusClick: (Item) -> Unit,
    private val onSwitchToggle: (Item, Boolean) -> Unit
) : ListAdapter<Item, ItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

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

    inner class ItemViewHolder(
        private val binding: ItemItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val context = binding.root.context

            // Title & Category
            binding.tvItemName.text = item.name
            binding.tvCategory.text = item.category

            // Image mapping
            val drawableRes = when (item.imageType.lowercase()) {
                "desk" -> R.drawable.ic_item_desk
                "bed" -> R.drawable.ic_item_bed
                "fan" -> R.drawable.ic_item_fan
                "lamp" -> R.drawable.ic_item_lamp
                "chair" -> R.drawable.ic_item_chair
                "cooker" -> R.drawable.ic_item_cooker
                "hairdryer" -> R.drawable.ic_item_hairdryer
                else -> R.drawable.ic_item_default
            }
            binding.ivItemImage.setImageResource(drawableRes)

            // Status Badge with dot matching Figma
            when (item.status) {
                ItemStatus.ACTIVE -> {
                    binding.tvStatusBadge.text = "● Còn dùng"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active_pill)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_active))
                }
                ItemStatus.BROKEN -> {
                    binding.tvStatusBadge.text = "● Hỏng"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_broken_pill)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_broken))
                }
            }

            // Switch Control
            if (item.hasSwitch) {
                binding.switchPower.visibility = View.VISIBLE
                binding.switchPower.setOnCheckedChangeListener(null)
                binding.switchPower.isChecked = item.isOn
                binding.switchPower.setOnCheckedChangeListener { _, isChecked ->
                    onSwitchToggle(item, isChecked)
                }
            } else {
                binding.switchPower.visibility = View.GONE
            }

            // Click Listeners
            binding.cardItem.setOnClickListener {
                onItemClick(item)
            }

            binding.tvStatusBadge.setOnClickListener {
                onStatusClick(item)
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
    }
}
