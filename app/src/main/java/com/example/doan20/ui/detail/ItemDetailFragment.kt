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
import com.example.doan20.ui.applyStatusBadge
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
    private var targetUserId: String? = null
    private var ownerName: String? = null
    private var ownerEmail: String? = null
    private var currentItem: Item? = null
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentItemId = arguments?.getString("itemId")
        targetUserId = arguments?.getString("userId")
        ownerName = arguments?.getString("ownerName")
        ownerEmail = arguments?.getString("ownerEmail")
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
        setupOwnerUI()
        setupListeners()
        observeItem()
    }

    private fun setupOwnerUI() {
        if (targetUserId != null) {
            // Admin · Chi tiết thiết bị
            binding.tvDetailTitle.text = "Chi tiết thiết bị"
            binding.layoutOwnerSection.visibility = View.VISIBLE
            binding.tvOwnerName.text = ownerName ?: "Người dùng"
            binding.tvOwnerEmail.text = ownerEmail ?: ""
            binding.tvOwnerInitials.text = com.example.doan20.data.model.getInitials(ownerName ?: "ND")
        } else {
            // Normal user · Chi tiết vật dụng
            binding.tvDetailTitle.text = "Chi tiết vật dụng"
            binding.layoutOwnerSection.visibility = View.GONE
        }
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
            val item = currentItem ?: return@setOnClickListener
            val nextStatus = if (item.status == ItemStatus.ACTIVE) ItemStatus.BROKEN else ItemStatus.ACTIVE
            if (targetUserId != null) {
                // Update in Firestore for admin
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(targetUserId!!)
                        .collection("items")
                        .document(item.id)
                        .update("status", nextStatus.name)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.toggleStatus(item.id)
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
            if (targetUserId != null) {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(targetUserId!!)
                        .collection("items")
                        .document(item.id)
                        .delete()
                } catch (e: Exception) {
                    // Ignore
                }
            } else {
                viewModel.deleteItem(item.id)
            }
            Toast.makeText(requireContext(), R.string.msg_item_deleted, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        dialog.show()
    }

    private fun observeItem() {
        val userId = targetUserId
        val itemId = currentItemId

        if (userId != null && itemId != null) {
            // Listen to device directly from Firestore for Admin
            firestoreListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("items")
                .document(itemId)
                .addSnapshotListener { snapshot, error ->
                    if (_binding == null) return@addSnapshotListener
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        findNavController().popBackStack()
                        return@addSnapshotListener
                    }
                    val item = snapshot.toObject(Item::class.java)?.copy(id = snapshot.id)
                    if (item != null) {
                        currentItem = item
                        bindItem(item)
                    } else {
                        findNavController().popBackStack()
                    }
                }
        } else {
            // Normal user observation
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.allItems.collect { items ->
                        val item = items.find { it.id == currentItemId }
                        if (item != null) {
                            currentItem = item
                            bindItem(item)
                        } else {
                            findNavController().popBackStack()
                        }
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
        binding.tvDetailStatus.applyStatusBadge(item.status)

        // Item Image
        binding.ivDetailImage.setImageResource(item.getDrawableRes())

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
        firestoreListener?.remove()
        firestoreListener = null
        _binding = null
    }
}
