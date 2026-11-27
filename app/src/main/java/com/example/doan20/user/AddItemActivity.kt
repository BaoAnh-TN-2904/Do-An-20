package com.example.doan20.user

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.doan20.R
import com.example.doan20.databinding.ActivityAddItemBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.utils.MqttManager
import com.example.doan20.utils.UiHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import java.util.UUID

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val db = FirebaseFirestore.getInstance()

    private val categories = listOf("Nội thất", "Điện tử", "Đồ dùng cá nhân", "Khác")
    private val statuses = listOf("Hoạt động", "Hỏng")

    private var editingDeviceId: String? = null
    private var targetUserId: String = ""
    private var targetEsp32Id: String = ""
    private var editingDevice: DeviceModel? = null

    private var selectedImageBase64: String = ""
    private var pendingCameraUri: Uri? = null
    private var isFormattingPrice = false
    private var activeAckTopic: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Vui lòng cấp quyền Camera để chụp ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        editingDeviceId = intent.getStringExtra("DEVICE_ID")?.takeIf { it.isNotBlank() }
        targetUserId = intent.getStringExtra("USER_ID").orEmpty()
            .ifBlank { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
        targetEsp32Id = MqttManager.normalizeEsp32Id(intent.getStringExtra("ESP32_ID"))

        if (targetEsp32Id.isBlank() && targetUserId.isNotBlank()) {
            db.collection("users").document(targetUserId).get()
                .addOnSuccessListener { doc ->
                    targetEsp32Id = MqttManager.normalizeEsp32Id(doc.getString("esp32Id"))
                }
        }

        savedInstanceState?.getString("pendingCameraUri")?.let {
            pendingCameraUri = Uri.parse(it)
        }

        setupDropdowns()
        setupDatePicker()
        setupPriceFormatter()
        setupImageSelector()
        setupListeners()

        // Nếu là chế độ Sửa thì tải dữ liệu thiết bị từ Firestore
        editingDeviceId?.let { id ->
            binding.btnSaveItem.isEnabled = false
            db.collection("devices").document(id).get()
                .addOnSuccessListener { doc ->
                    binding.btnSaveItem.isEnabled = true
                    if (doc.exists()) {
                        val device = DeviceModel.fromFirestore(doc)
                        editingDevice = device
                        populateForEdit(device)
                    }
                }
                .addOnFailureListener {
                    binding.btnSaveItem.isEnabled = true
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingCameraUri?.let { outState.putString("pendingCameraUri", it.toString()) }
    }

    private fun setupDropdowns() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actCategory.setAdapter(categoryAdapter)
        if (editingDeviceId == null) {
            binding.actCategory.setText(categories[0], false)
        }

        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            val selected = categories[position]
            if (selected.equals("Điện tử", ignoreCase = true)) {
                binding.layoutConditionalGpio.visibility = View.VISIBLE
            } else {
                binding.layoutConditionalGpio.visibility = View.GONE
                binding.etPinNumber.text?.clear()
            }
        }

        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actStatus.setAdapter(statusAdapter)
        if (editingDeviceId == null) {
            binding.actStatus.setText(statuses[0], false)
        }
    }

    private fun populateForEdit(device: DeviceModel) {
        binding.tvAddTitle.text = getString(R.string.edit_item_title)
        binding.btnSaveItem.text = getString(R.string.action_save_changes)

        binding.etItemName.setText(device.name)
        binding.actCategory.setText(device.getCategoryDisplayName(), false)
        binding.actStatus.setText(if (device.isBroken()) "Hỏng" else "Hoạt động", false)

        if (device.getPriceLong() > 0L) {
            binding.etPrice.setText(device.getPriceLong().toString())
        }

        UiHelper.parseDateToDisplay(device.purchaseDate)?.let {
            binding.etBuyDate.setText(it)
        }

        binding.etNotes.setText(device.note)

        if (device.isElectronic()) {
            binding.layoutConditionalGpio.visibility = View.VISIBLE
            if (device.gpioPin > 0) {
                binding.etPinNumber.setText(device.gpioPin.toString())
            }
        }

        selectedImageBase64 = device.imageBase64
        updateImagePreview()
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            binding.etBuyDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
        }

        binding.etBuyDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val millis = UiHelper.parseDisplayDateToMillis(binding.etBuyDate.text?.toString())
            if (millis > 0L) calendar.timeInMillis = millis
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.tilBuyDate.setEndIconOnClickListener { binding.etBuyDate.performClick() }
    }

    private fun setupPriceFormatter() {
        binding.etPrice.addTextChangedListener(object : TextWatcher {
            private var digitsBeforeCursor = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isFormattingPrice || s == null) return
                val cursor = binding.etPrice.selectionStart.coerceIn(0, s.length)
                digitsBeforeCursor = s.substring(0, cursor).count { it.isDigit() }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormattingPrice || s == null) return
                val cursor = binding.etPrice.selectionStart.coerceIn(0, s.length)
                digitsBeforeCursor = s.substring(0, cursor).count { it.isDigit() }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingPrice || s == null) return
                val digitsOnly = s.toString().replace("[^0-9]".toRegex(), "")
                val formatted = digitsOnly.toLongOrNull()?.let { UiHelper.formatNumber(it) } ?: ""

                if (formatted != s.toString()) {
                    isFormattingPrice = true
                    s.replace(0, s.length, formatted)
                    var newCursor = 0
                    var counted = 0
                    while (newCursor < formatted.length && counted < digitsBeforeCursor) {
                        if (formatted[newCursor].isDigit()) counted++
                        newCursor++
                    }
                    binding.etPrice.setSelection(newCursor.coerceIn(0, formatted.length))
                    isFormattingPrice = false
                }
            }
        })
    }

    private fun setupImageSelector() {
        binding.btnSelectImage.setOnClickListener {
            val options = arrayOf("Chụp ảnh", "Tải ảnh lên từ thư viện")
            MaterialAlertDialogBuilder(this)
                .setTitle("Chọn ảnh vật dụng")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> checkCameraAndLaunch()
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }
                .setNegativeButton("Đóng", null)
                .show()
        }
    }

    private fun checkCameraAndLaunch() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val imageFile = File(cacheDir, "item_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImageUri(uri: Uri) {
        Thread {
            val dataUrl = encodeUriToResizedDataUrl(uri)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (dataUrl != null) {
                    selectedImageBase64 = dataUrl
                    updateImagePreview()
                    Toast.makeText(this, "Đã chọn ảnh thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun encodeUriToResizedDataUrl(uri: Uri): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }

            val maxDim = 640
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDim * 2 || boundsOpts.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val orientation = contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val rotated = applyExifOrientation(rawBitmap, orientation)
            val scaled = scaleBitmapDown(rotated, maxDim)

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, output)
            if (scaled !== rotated) scaled.recycle()
            if (rotated !== rawBitmap) rotated.recycle()
            rawBitmap.recycle()

            val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun updateImagePreview() {
        val density = resources.displayMetrics.density
        if (selectedImageBase64.isNotBlank()) {
            binding.ivAddPreview.imageTintList = null
            binding.ivAddPreview.setPadding(0, 0, 0, 0)
            binding.ivAddPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            UiHelper.loadBase64Image(binding.ivAddPreview, selectedImageBase64, R.drawable.app_logo)
            binding.tvImagePrompt.text = "Đã chọn ảnh vật dụng"
            binding.tvImageSubPrompt.text = "Chạm để chụp lại hoặc đổi ảnh"
        } else {
            val pad = (6 * density).toInt()
            binding.ivAddPreview.setPadding(pad, pad, pad, pad)
            binding.ivAddPreview.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.ivAddPreview.imageTintList = null
            binding.ivAddPreview.setImageResource(R.drawable.app_logo)
            binding.tvImagePrompt.text = getString(
                if (editingDevice != null) R.string.image_picker_edit_prompt else R.string.image_picker_prompt
            )
            binding.tvImageSubPrompt.text = "Chụp ảnh hoặc tải ảnh từ thư viện"
        }
    }

    private fun setupListeners() {
        binding.btnAddBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.etItemName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrBlank()) binding.tilItemName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSaveItem.setOnClickListener { saveDevice() }
    }

    private fun saveDevice() {
        val name = binding.etItemName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            binding.tilItemName.error = getString(R.string.error_name_required)
            binding.etItemName.requestFocus()
            return
        }

        val categoryDisplay = binding.actCategory.text?.toString()?.trim().orEmpty().ifEmpty { categories[0] }
        val statusText = binding.actStatus.text?.toString()?.trim()
        val status = if (statusText == "Hỏng") "broken" else "active"

        val priceStr = binding.etPrice.text?.toString()?.replace("[^0-9]".toRegex(), "").orEmpty().ifBlank { "0" }
        val purchaseMillis = UiHelper.parseDisplayDateToMillis(binding.etBuyDate.text?.toString())
        val purchaseDateStr = if (purchaseMillis > 0L) purchaseMillis.toString() else ""
        val gpioPin = binding.etPinNumber.text?.toString()?.trim()?.toLongOrNull() ?: 0L
        val note = binding.etNotes.text?.toString()?.trim().orEmpty()

        val existing = editingDevice
        val docId = existing?.id ?: UUID.randomUUID().toString()
        val ownerId = existing?.userId?.ifBlank { targetUserId } ?: targetUserId

        val device = DeviceModel(
            id = docId,
            devicedId = existing?.devicedId?.ifBlank { docId } ?: docId,
            userId = ownerId,
            name = name,
            category = DeviceModel.toFirestoreCategory(categoryDisplay),
            gpioPin = gpioPin,
            imageBase64 = selectedImageBase64,
            note = note,
            purchaseDate = purchaseDateStr,
            purchasePrice = priceStr,
            status = status,
            createdAt = existing?.createdAt?.ifBlank { System.currentTimeMillis().toString() }
                ?: System.currentTimeMillis().toString(),
            isOn = existing?.isOn ?: false
        )

        binding.btnSaveItem.isEnabled = false
        db.collection("devices").document(docId)
            .set(device.toFirestoreMap())
            .addOnSuccessListener {
                val mqttTargetId = MqttManager.resolveTargetId(targetEsp32Id, ownerId)
                // Nếu trước đó có gán GPIO nhưng nay bỏ GPIO hoặc đổi sang loại không phải Điện tử -> giải phóng chân trên ESP32
                if (existing != null && existing.gpioPin > 0L && (!device.isElectronic() || gpioPin <= 0L) && mqttTargetId.isNotBlank()) {
                    MqttManager.publish(
                        MqttManager.removeTopic(mqttTargetId, docId),
                        MqttManager.buildRemovePayload(docId, existing.gpioPin.toInt())
                    )
                }

                Toast.makeText(
                    this,
                    if (existing != null) R.string.msg_item_updated else R.string.msg_item_saved,
                    Toast.LENGTH_SHORT
                ).show()

                if (device.isElectronic() && gpioPin > 0L && mqttTargetId.isNotBlank()) {
                    showEsp32ProvisionDialog(mqttTargetId, docId, name, gpioPin.toInt())
                } else {
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.btnSaveItem.isEnabled = true
                Toast.makeText(this, "Lỗi lưu thiết bị: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEsp32ProvisionDialog(ownerId: String, deviceId: String, deviceName: String, gpioPin: Int) {
        val dialog = Dialog(this)
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

        val ackTopic = MqttManager.ackTopic(ownerId, deviceId)
        activeAckTopic = ackTopic
        var isAckReceived = false

        val cleanupAck = {
            mainHandler.removeCallbacksAndMessages(null)
            MqttManager.unsubscribe(ackTopic)
            if (activeAckTopic == ackTopic) activeAckTopic = null
        }

        btnAction.setOnClickListener {
            cleanupAck()
            dialog.dismiss()
            finish()
        }

        dialog.show()

        MqttManager.subscribe(
            topic = ackTopic,
            onSubscribed = {
                MqttManager.publish(
                    MqttManager.provisionTopic(ownerId, deviceId),
                    MqttManager.buildProvisionPayload(deviceId, deviceName, gpioPin)
                )
            }
        ) { _, payload ->
            runOnUiThread {
                if (isFinishing || isDestroyed || !dialog.isShowing || isAckReceived) return@runOnUiThread
                isAckReceived = true
                cleanupAck()
                pbLoading.visibility = View.GONE

                var isSuccess = true
                var failMessage = ""
                try {
                    val json = JSONObject(payload)
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
                } catch (_: Exception) {
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

                    mainHandler.postDelayed({
                        if (!isFinishing && !isDestroyed && dialog.isShowing) {
                            dialog.dismiss()
                            finish()
                        }
                    }, 1800)
                } else {
                    binding.btnSaveItem.isEnabled = true
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

        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed && !isAckReceived && dialog.isShowing) {
                cleanupAck()
                pbLoading.visibility = View.GONE
                ivSuccess.visibility = View.GONE
                ivWarning.visibility = View.VISIBLE
                tvTitle.text = "Chưa nhận phản hồi từ ESP32"
                tvMessage.text = "Vật dụng đã được lưu vào hệ thống. Vi điều khiển ESP32 sẽ tự động cấu hình chân GPIO $gpioPin khi kết nối."
                btnAction.text = "Tôi đã hiểu"
            }
        }, 4500)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        activeAckTopic?.let { MqttManager.unsubscribe(it) }
    }
}
