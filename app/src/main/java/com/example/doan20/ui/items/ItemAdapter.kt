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

            // Rich Figma PNG / SVG Item Image
            val drawableRes = when (item.imageType.lowercase()) {
                "desk" -> R.drawable.item_desk
                "bed" -> R.drawable.item_bed
                "fan" -> R.drawable.item_fan
                "lamp" -> R.drawable.item_lamp
                "cooker" -> R.drawable.item_cooker
                "chair" -> R.drawable.ic_item_chair
                "hairdryer" -> R.drawable.ic_item_hairdryer
                else -> {
                    val lowerName = item.name.lowercase()
                    when {
                        lowerName.contains("bàn") -> R.drawable.item_desk
                        lowerName.contains("giường") -> R.drawable.item_bed
                        lowerName.contains("quạt") -> R.drawable.item_fan
                        lowerName.contains("đèn") -> R.drawable.item_lamp
                        lowerName.contains("nồi") -> R.drawable.item_cooker
                        else -> R.drawable.ic_item_default
                    }
                }
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

            // Switch Control for electronic items (Figma 01_Dashboard & 02_ItemList)
            if (item.hasSwitch) {
                binding.switchPower.visibility = View.VISIBLE
                binding.switchPower.isUseMaterialThemeColors = false
                binding.switchPower.trackDecorationDrawable = null
                binding.switchPower.thumbIconDrawable = null
                binding.switchPower.trackTintList = ContextCompat.getColorStateList(context, R.color.switch_track_selector)
                binding.switchPower.thumbTintList = ContextCompat.getColorStateList(context, R.color.switch_thumb_selector)
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
