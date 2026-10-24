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

    private val categories = listOf("Nội thất", "Điện tử", "Đồ dùng cá nhân", "Khác")
    private val statuses = listOf("Còn dùng", "Hỏng")
    private val pinTypes = listOf("GPIO", "SPI", "I2C", "UART", "ADC", "PWM", "DAC", "Khác")
    private val imageTypes = listOf("default", "desk", "bed", "fan", "lamp", "cooker", "chair", "hairdryer")
    private var selectedImageTypeIndex = 0

    private var editingItemId: String? = null
    private var editingItem: Item? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingItemId = arguments?.getString("itemId")
    }

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

        // Check if in edit mode (Figma 05_EditItem)
        editingItemId?.let { id ->
            editingItem = viewModel.getItem(id)
            editingItem?.let { item ->
                populateItemForEdit(item)
            }
        }
    }

    private fun populateItemForEdit(item: Item) {
        binding.tvAddTitle.text = getString(R.string.edit_item_title)
        binding.tvImagePrompt.text = getString(R.string.image_picker_edit_prompt)
        binding.btnSaveItem.text = getString(R.string.action_save_changes)

        binding.etItemName.setText(item.name)
        binding.actCategory.setText(item.category, false)
        binding.actStatus.setText(if (item.status == ItemStatus.BROKEN) "Hỏng" else "Còn dùng", false)

        item.price?.let {
            if (it > 0) binding.etPrice.setText(it.toString())
        }

        item.buyDate?.let {
            binding.etBuyDate.setText(it)
        }

        item.notes?.let {
            binding.etNotes.setText(it)
        }

        // Conditional GPIO
        if (item.category.equals("Điện tử", ignoreCase = true)) {
            binding.layoutConditionalGpio.visibility = View.VISIBLE
            item.pinConfig?.let { cfg ->
                val parts = cfg.split(" ", limit = 2)
                if (parts.isNotEmpty()) binding.actPinType.setText(parts[0], false)
                if (parts.size > 1) binding.etPinNumber.setText(parts[1])
            }
        }

        // Image
        val idx = imageTypes.indexOf(item.imageType.lowercase())
        if (idx >= 0) {
            selectedImageTypeIndex = idx
        }
        updateImagePreview()
    }

    private fun setupDropdowns() {
        // Category Dropdown
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actCategory.setAdapter(categoryAdapter)
        if (editingItem == null) {
            binding.actCategory.setText(categories[0], false)
        }

        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            val selected = categories[position]
            // Show GPIO 2-column input only when category is 'Điện tử' (matching Figma)
            if (selected.equals("Điện tử", ignoreCase = true)) {
                binding.layoutConditionalGpio.visibility = View.VISIBLE
                if (binding.actPinType.text.isNullOrBlank()) {
                    binding.actPinType.setText(pinTypes[0], false)
                }
            } else {
                binding.layoutConditionalGpio.visibility = View.GONE
                binding.actPinType.text?.clear()
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
        if (editingItem == null) {
            binding.actStatus.setText(statuses[0], false)
        }

        // Pin Type Dropdown (GPIO, SPI, I2C, UART, ADC, PWM, DAC, Khác)
        val pinTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            pinTypes
        )
        binding.actPinType.setAdapter(pinTypeAdapter)
        if (editingItem == null) {
            binding.actPinType.setText(pinTypes[0], false)
        }
        binding.actPinType.setOnClickListener {
            binding.actPinType.showDropDown()
        }
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
            selectedImageTypeIndex = (selectedImageTypeIndex + 1) % imageTypes.size
            updateImagePreview()
            Toast.makeText(
                requireContext(),
                "Đã chọn ảnh mẫu: ${imageTypes[selectedImageTypeIndex]}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateImagePreview() {
        val currentType = imageTypes[selectedImageTypeIndex]
        val drawableRes = if (currentType == "default") {
            R.drawable.ic_camera
        } else {
            com.example.doan20.data.model.resolveItemDrawable(currentType)
        }
        binding.ivAddPreview.setImageResource(drawableRes)
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

        val category = binding.actCategory.text?.toString()?.trim().orEmpty().ifEmpty { categories[0] }
        val statusText = binding.actStatus.text?.toString()?.trim()
        val status = if (statusText == "Hỏng") ItemStatus.BROKEN else ItemStatus.ACTIVE

        val priceStr = binding.etPrice.text?.toString()?.trim()
        val price = priceStr?.toLongOrNull()

        val buyDate = binding.etBuyDate.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

        // GPIO configuration
        val pinType = binding.actPinType.text?.toString()?.trim().orEmpty()
        val pinNumber = binding.etPinNumber.text?.toString()?.trim().orEmpty()
        val gpioConfig = if (pinType.isNotBlank() || pinNumber.isNotBlank()) {
            "$pinType $pinNumber".trim()
        } else null

        val notes = binding.etNotes.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        val hasSwitch = category.equals("Điện tử", ignoreCase = true)

        var chosenImageType = imageTypes[selectedImageTypeIndex]
        if (chosenImageType == "default") {
            val lowerName = name.lowercase()
            chosenImageType = when {
                lowerName.contains("bàn") -> "desk"
                lowerName.contains("giường") || lowerName.contains("nệm") -> "bed"
                lowerName.contains("quạt") -> "fan"
                lowerName.contains("đèn") -> "lamp"
                lowerName.contains("nồi") || lowerName.contains("cơm") -> "cooker"
                lowerName.contains("ghế") -> "chair"
                lowerName.contains("sấy") -> "hairdryer"
                else -> "default"
            }
        }

        if (editingItemId != null && editingItem != null) {
            // Update existing item (Figma 05_EditItem)
            val updated = editingItem!!.copy(
                name = name,
                category = category,
                imageType = chosenImageType,
                status = status,
                hasSwitch = hasSwitch,
                price = price,
                buyDate = buyDate,
                pinConfig = gpioConfig,
                notes = notes
            )
            viewModel.updateItem(updated)
            Toast.makeText(requireContext(), R.string.msg_item_updated, Toast.LENGTH_SHORT).show()
        } else {
            // Add new item (Figma 04_AddItem)
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
        }

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
