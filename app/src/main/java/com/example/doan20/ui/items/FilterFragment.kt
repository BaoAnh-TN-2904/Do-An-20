package com.example.doan20.ui.items

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.FragmentFilterBinding
import com.example.doan20.ui.viewmodel.ItemViewModel

/**
 * Full-screen filter view for filtering items by category and status.
 */
class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    private var tempCategory: String? = null
    private var tempStatus: ItemStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()

        // Read current filter state
        tempCategory = viewModel.categoryFilter.value
        tempStatus = viewModel.statusFilter.value

        updateCategoryUI()
        updateStatusUI()
        setupListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        // Back button
        binding.btnFilterBack.setOnClickListener {
            findNavController().popBackStack()
        }

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

        // Apply button
        binding.btnApplyFilter.setOnClickListener {
            viewModel.setCategoryFilter(tempCategory)
            viewModel.setStatusFilter(tempStatus)
            findNavController().popBackStack()
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
}
