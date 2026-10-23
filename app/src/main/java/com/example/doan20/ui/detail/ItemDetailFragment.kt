package com.example.doan20.ui.detail

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()
    private var currentItemId: String? = null
    private var currentItem: Item? = null

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

        // Sửa button -> Navigate to Edit Screen (Figma 05_EditItem)
        binding.btnEdit.setOnClickListener {
            currentItemId?.let { id ->
                findNavController().navigate(
                    R.id.action_detail_to_edit,
                    bundleOf("itemId" to id)
                )
            }
        }

        // Xóa button -> Show custom Figma dialog (Figma 08_DeleteDialog)
        binding.btnDelete.setOnClickListener {
            showCustomDeleteDialog()
        }

        binding.tvDetailStatus.setOnClickListener {
            currentItemId?.let { id ->
                viewModel.toggleStatus(id)
            }
        }
    }

    /**
     * Custom Delete Dialog matching Figma 08_DeleteDialog
     */
    private fun showCustomDeleteDialog() {
        val item = currentItem ?: return
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_delete_confirm)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvMessage = dialog.findViewById<TextView>(R.id.tvDeleteDialogMessage)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnDialogCancel)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btnDialogConfirm)

        tvMessage.text = getString(R.string.dialog_delete_message, item.name)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            viewModel.deleteItem(item.id)
            Toast.makeText(requireContext(), R.string.msg_item_deleted, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        dialog.show()
    }

    private fun observeItem() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    val item = items.find { it.id == currentItemId }
                    if (item != null) {
                        currentItem = item
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
                binding.tvDetailStatus.text = "●  Còn dùng"
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_active_pill)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(context, R.color.status_active))
            }
            ItemStatus.BROKEN -> {
                binding.tvDetailStatus.text = "●  Hỏng"
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_badge_broken_pill)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(context, R.color.status_broken))
            }
        }

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
