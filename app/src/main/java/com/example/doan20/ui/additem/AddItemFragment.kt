package com.example.doan20.ui.additem

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.databinding.FragmentAddItemBinding
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        setupWindowInsets()
        setupDropdowns()
        setupDatePicker()
        setupImageSelector()
        setupListeners()

        // Populate fields when editing an existing item
        editingItemId?.let { id ->
            editingItem = viewModel.getItem(id)
            editingItem?.let { item ->
                populateItemForEdit(item)
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
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
            // Show GPIO configuration inputs only for electronic items
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

        val savedItem = if (editingItemId != null && editingItem != null) {
            // Update existing item
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
            updated
        } else {
            // Create new item
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
            newItem
        }

        val gpioPin = com.example.doan20.data.repository.ItemRepository.extractGpioPin(savedItem.pinConfig)
        if (savedItem.hasSwitch && gpioPin != null) {
            showEsp32ProvisionDialog(savedItem, gpioPin)
        } else {
            findNavController().popBackStack()
        }
    }

    /**
     * Popup dialog kiểm tra và xác nhận ESP32 đã khởi tạo thành công chân GPIO
     */
    private fun showEsp32ProvisionDialog(item: Item, gpioPin: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_esp32_provision)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        val pbLoading = dialog.findViewById<ProgressBar>(R.id.pbProvisionLoading)
        val ivSuccess = dialog.findViewById<ImageView>(R.id.ivProvisionSuccess)
        val ivWarning = dialog.findViewById<ImageView>(R.id.ivProvisionWarning)
        val ivError = dialog.findViewById<ImageView>(R.id.ivProvisionError)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvProvisionTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvProvisionMessage)
        val btnAction = dialog.findViewById<MaterialButton>(R.id.btnProvisionAction)

        tvMessage.text = "Đang gửi yêu cầu khởi tạo chân GPIO $gpioPin tới vi điều khiển ESP32..."
        btnAction.text = "Đóng"
        btnAction.setOnClickListener {
            dialog.dismiss()
            findNavController().popBackStack()
        }

        dialog.show()

        // 1. Gửi lệnh provision sang MQTT
        com.example.doan20.data.repository.ItemRepository.sendProvisionRequest(item, gpioPin)

        // 2. Chờ ACK từ ESP32 qua flow
        var isAckReceived = false
        val ackJob = viewLifecycleOwner.lifecycleScope.launch {
            com.example.doan20.data.repository.ItemRepository.provisionAck.collect { (id, payload) ->
                if (id == item.id) {
                    isAckReceived = true
                    pbLoading.visibility = View.GONE

                    var isSuccess = true
                    var failMessage = ""
                    try {
                        val json = org.json.JSONObject(payload)
                        val status = json.optString("status", "READY")
                        val reason = json.optString("reason", "")
                        val msg = json.optString("message", "")
                        if (status.equals("FAILED", ignoreCase = true) || status.equals("FULL", ignoreCase = true)) {
                            isSuccess = false
                            failMessage = if (msg.isNotBlank()) msg else when (reason) {
                                "GPIO_FULL" -> "ESP32 đã sử dụng hết các chân GPIO vật lý khả dụng!"
                                "PIN_OCCUPIED" -> "Chân GPIO $gpioPin đã được vật dụng khác sử dụng trên ESP32!"
                                "INVALID_PIN" -> "Chân GPIO $gpioPin không hợp lệ để làm cổng điều khiển!"
                                else -> "Không thể khởi tạo chân GPIO $gpioPin trên ESP32."
                            }
                        }
                    } catch (e: Exception) {
                        if (payload.contains("FAILED") || payload.contains("FULL")) {
                            isSuccess = false
                            failMessage = "ESP32 không thể khởi tạo chân GPIO $gpioPin."
                        }
                    }

                    if (isSuccess) {
                        ivWarning.visibility = View.GONE
                        ivError.visibility = View.GONE
                        ivSuccess.visibility = View.VISIBLE
                        tvTitle.text = "ESP32 đã sẵn sàng!"
                        tvMessage.text = "Chân GPIO $gpioPin đã được vi điều khiển ESP32 khởi tạo thành công làm cổng điều khiển (Relay)!"
                        btnAction.text = "Hoàn tất"

                        // Tự động đóng sau 1.8 giây hoặc người dùng bấm Hoàn tất
                        delay(1800)
                        if (dialog.isShowing) {
                            dialog.dismiss()
                            findNavController().popBackStack()
                        }
                    } else {
                        // Thất bại do hết chân GPIO hoặc chân đã bị chiếm
                        ivSuccess.visibility = View.GONE
                        ivWarning.visibility = View.GONE
                        ivError.visibility = View.VISIBLE
                        tvTitle.text = "Khởi tạo thất bại: Chân GPIO không khả dụng"
                        tvMessage.text = "$failMessage\n\nVui lòng chọn một chân GPIO khác còn trống."
                        btnAction.text = "Chọn chân khác"
                        btnAction.setOnClickListener {
                            dialog.dismiss()
                            binding.etPinNumber.requestFocus()
                        }
                    }
                }
            }
        }

        // 3. Timeout sau 4.5 giây nếu ESP32 đang tắt nguồn hoặc không có mạng
        viewLifecycleOwner.lifecycleScope.launch {
            delay(4500)
            if (!isAckReceived && dialog.isShowing) {
                ackJob.cancel()
                pbLoading.visibility = View.GONE
                ivSuccess.visibility = View.GONE
                ivWarning.visibility = View.VISIBLE
                tvTitle.text = "Chưa nhận phản hồi từ ESP32"
                tvMessage.text = "Vật dụng đã được lưu vào hệ thống. Vi điều khiển ESP32 sẽ tự động nhận diện và cấu hình chân GPIO $gpioPin ngay khi online."
                btnAction.text = "Tôi đã hiểu"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
