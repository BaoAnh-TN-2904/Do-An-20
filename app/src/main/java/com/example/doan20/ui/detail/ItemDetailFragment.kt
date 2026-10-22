package com.example.doan20.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.doan20.databinding.FragmentDetailBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()
    private var currentItemId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentItemId = arguments?.getString("itemId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeItem()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEdit.setOnClickListener {
            // Toggle item status or edit
            currentItemId?.let { id ->
                viewModel.toggleStatus(id)
                Toast.makeText(requireContext(), "Đã cập nhật trạng thái vật dụng", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }

        binding.tvDetailStatus.setOnClickListener {
            currentItemId?.let { id ->
                viewModel.toggleStatus(id)
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.dialog_confirm_delete) { _, _ ->
                currentItemId?.let { id ->
                    viewModel.deleteItem(id)
                    Toast.makeText(requireContext(), R.string.msg_item_deleted, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun observeItem() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    val item = items.find { it.id == currentItemId }
                    if (item != null) {
                        bindItem(item)
                    } else {
                        // Item was deleted
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun bindItem(item: Item) {
        val context = requireContext()

        // Name & Category
        binding.tvDetailName.text = item.name
        binding.tvDetailCategory.text = item.category

        // Status Badge with dot matching Figma 03_ItemDetail
        when (item.status) {
            ItemStatus.ACTIVE -> {
                binding.tvDetailStatus.text = "● Còn dùng"
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_active_pill)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(context, R.color.status_active))
            }
            ItemStatus.BROKEN -> {
                binding.tvDetailStatus.text = "● Hỏng"
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_broken_pill)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(context, R.color.status_broken))
            }
        }

        // Image
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
        binding.ivDetailImage.setImageResource(drawableRes)

        // Price
        if (item.price != null && item.price > 0) {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            binding.tvDetailPrice.text = "${formatter.format(item.price)}đ"
        } else {
            binding.tvDetailPrice.text = "250.000đ"
        }

        // Buy Date
        binding.tvDetailDate.text = item.buyDate ?: "15/09/2025"

        // Notes
        binding.tvDetailNotes.text = item.notes?.ifBlank {
            "Đèn không sáng, cần kiểm tra bóng và dây nguồn."
        } ?: "Đèn không sáng, cần kiểm tra bóng và dây nguồn."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
