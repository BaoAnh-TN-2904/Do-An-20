package com.example.doan20.ui.additem

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.FragmentAddItemBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import java.util.Calendar
import java.util.UUID

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    private val categories = listOf("Nội thất", "Điện tử", "Đồ dùng học tập", "Khác")
    private val statuses = listOf("Còn dùng", "Hỏng")
    private val imageTypes = listOf("default", "desk", "bed", "fan", "lamp", "chair", "cooker", "hairdryer")
    private var selectedImageTypeIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupDatePicker()
        setupImageSelector()
        setupListeners()
    }

    private fun setupDropdowns() {
        // Category Dropdown
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actCategory.setAdapter(categoryAdapter)
        binding.actCategory.setText(categories[0], false)

        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            val selected = categories[position]
            // Show GPIO 2-column input only when category is 'Điện tử' (matching Figma)
            if (selected.equals("Điện tử", ignoreCase = true)) {
                binding.layoutConditionalGpio.visibility = View.VISIBLE
            } else {
                binding.layoutConditionalGpio.visibility = View.GONE
                binding.etPinType.text?.clear()
                binding.etPinNumber.text?.clear()
            }
        }

        // Status Dropdown
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statuses
        )
        binding.actStatus.setAdapter(statusAdapter)
        binding.actStatus.setText(statuses[0], false)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            binding.etBuyDate.setText(formattedDate)
        }

        binding.etBuyDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.tilBuyDate.setEndIconOnClickListener {
            binding.etBuyDate.performClick()
        }
    }

    private fun setupImageSelector() {
        binding.btnSelectImage.setOnClickListener {
            // Cycle through available sample illustrations
            selectedImageTypeIndex = (selectedImageTypeIndex + 1) % imageTypes.size
            val currentType = imageTypes[selectedImageTypeIndex]
            val drawableRes = when (currentType) {
                "desk" -> R.drawable.ic_item_desk
                "bed" -> R.drawable.ic_item_bed
                "fan" -> R.drawable.ic_item_fan
                "lamp" -> R.drawable.ic_item_lamp
                "chair" -> R.drawable.ic_item_chair
                "cooker" -> R.drawable.ic_item_cooker
                "hairdryer" -> R.drawable.ic_item_hairdryer
                else -> R.drawable.ic_item_default
            }
            binding.ivAddPreview.setImageResource(drawableRes)
            Toast.makeText(requireContext(), "Đã chọn biểu trưng: $currentType", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnAddBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.etItemName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrBlank()) {
                    binding.tilItemName.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSaveItem.setOnClickListener {
            saveItem()
        }
    }

    private fun saveItem() {
        val name = binding.etItemName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            binding.tilItemName.error = getString(R.string.error_name_required)
            binding.etItemName.requestFocus()
            return
        }

        val category = binding.actCategory.text?.toString()?.trim().ifEmpty { categories[0] }
        val statusText = binding.actStatus.text?.toString()?.trim()
        val status = if (statusText == "Hỏng") ItemStatus.BROKEN else ItemStatus.ACTIVE

        val priceStr = binding.etPrice.text?.toString()?.trim()
        val price = priceStr?.toLongOrNull()

        val buyDate = binding.etBuyDate.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        
        // GPIO configuration
        val pinType = binding.etPinType.text?.toString()?.trim().orEmpty()
        val pinNumber = binding.etPinNumber.text?.toString()?.trim().orEmpty()
        val gpioConfig = if (pinType.isNotBlank() || pinNumber.isNotBlank()) {
            "$pinType $pinNumber".trim()
        } else null

        val notes = binding.etNotes.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        val hasSwitch = category.equals("Điện tử", ignoreCase = true)

        // Determine image type based on name or category if default
        var chosenImageType = imageTypes[selectedImageTypeIndex]
        if (chosenImageType == "default") {
            val lowerName = name.lowercase()
            chosenImageType = when {
                lowerName.contains("bàn") -> "desk"
                lowerName.contains("giường") || lowerName.contains("nệm") -> "bed"
                lowerName.contains("quạt") -> "fan"
                lowerName.contains("đèn") -> "lamp"
                lowerName.contains("ghế") -> "chair"
                lowerName.contains("nồi") || lowerName.contains("cơm") -> "cooker"
                lowerName.contains("sấy") -> "hairdryer"
                else -> "default"
            }
        }

        val newItem = Item(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            imageType = chosenImageType,
            status = status,
            hasSwitch = hasSwitch,
            isOn = false,
            price = price,
            buyDate = buyDate,
            pinConfig = gpioConfig,
            notes = notes
        )

        viewModel.addItem(newItem)
        Toast.makeText(requireContext(), R.string.msg_item_saved, Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
