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

            // Item Image
            binding.ivItemImage.setImageResource(item.getDrawableRes())

            // Status Badge & Broken Warning
            binding.tvStatusBadge.applyStatusBadge(item.status)
            binding.layoutBrokenWarning.visibility = if (item.status == ItemStatus.BROKEN) View.VISIBLE else View.GONE

            // Switch Control for electronic items (Figma 01_Dashboard & 02_ItemList)
            if (item.hasSwitch) {
                binding.switchPower.visibility = View.VISIBLE
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
