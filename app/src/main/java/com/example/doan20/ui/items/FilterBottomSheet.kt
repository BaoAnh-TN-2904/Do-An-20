package com.example.doan20.ui.items

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.BottomSheetFilterBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    private var tempCategory: String? = null
    private var tempStatus: ItemStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read current filter state
        tempCategory = viewModel.categoryFilter.value
        tempStatus = viewModel.statusFilter.value

        updateCategoryUI()
        updateStatusUI()
        setupListeners()
    }

    private fun setupListeners() {
        // Category selection rows
        binding.rowCategoryAll.setOnClickListener {
            tempCategory = null
            updateCategoryUI()
        }
        binding.rowCategoryFurniture.setOnClickListener {
            tempCategory = "Nội thất"
            updateCategoryUI()
        }
        binding.rowCategoryElectronics.setOnClickListener {
            tempCategory = "Điện tử"
            updateCategoryUI()
        }
        binding.rowCategoryPersonal.setOnClickListener {
            tempCategory = "Đồ dùng cá nhân"
            updateCategoryUI()
        }
        binding.rowCategoryOther.setOnClickListener {
            tempCategory = "Khác"
            updateCategoryUI()
        }

        // Status selection buttons
        binding.btnStatusAll.setOnClickListener {
            tempStatus = null
            updateStatusUI()
        }
        binding.btnStatusActive.setOnClickListener {
            tempStatus = ItemStatus.ACTIVE
            updateStatusUI()
        }
        binding.btnStatusBroken.setOnClickListener {
            tempStatus = ItemStatus.BROKEN
            updateStatusUI()
        }

        // Apply
        binding.btnApplyFilter.setOnClickListener {
            viewModel.setCategoryFilter(tempCategory)
            viewModel.setStatusFilter(tempStatus)
            dismiss()
        }

        // Cancel
        binding.btnCancelFilter.setOnClickListener {
            dismiss()
        }
    }

    private fun updateCategoryUI() {
        binding.rbCatAll.isChecked = tempCategory == null
        binding.rbCatFurniture.isChecked = tempCategory.equals("Nội thất", ignoreCase = true)
        binding.rbCatElectronics.isChecked = tempCategory.equals("Điện tử", ignoreCase = true)
        binding.rbCatPersonal.isChecked = tempCategory.equals("Đồ dùng cá nhân", ignoreCase = true)
        binding.rbCatOther.isChecked = tempCategory.equals("Khác", ignoreCase = true)
    }

    private fun updateStatusUI() {
        binding.btnStatusAll.isSelected = tempStatus == null
        binding.btnStatusActive.isSelected = tempStatus == ItemStatus.ACTIVE
        binding.btnStatusBroken.isSelected = tempStatus == ItemStatus.BROKEN
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheet"
        fun newInstance() = FilterBottomSheet()
    }
}
