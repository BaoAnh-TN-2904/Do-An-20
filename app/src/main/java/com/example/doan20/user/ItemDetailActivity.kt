package com.example.doan20.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan20.R
import com.example.doan20.databinding.ActivityItemDetailBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.utils.MqttManager
import com.example.doan20.utils.UiHelper
import com.example.doan20.utils.applyDeviceStatusBadge
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private val db = FirebaseFirestore.getInstance()

    private var deviceId: String = ""
    private var userId: String = ""
    private var esp32Id: String = ""
    private var isAdmin: Boolean = false
    private var ownerName: String = ""
    private var ownerEmail: String = ""

    private var currentDevice: DeviceModel? = null
    private var deviceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        deviceId = intent.getStringExtra("DEVICE_ID").orEmpty()
        userId = intent.getStringExtra("USER_ID").orEmpty()
        esp32Id = MqttManager.normalizeEsp32Id(intent.getStringExtra("ESP32_ID"))
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)

        if (esp32Id.isBlank() && userId.isNotBlank()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    esp32Id = MqttManager.normalizeEsp32Id(doc.getString("esp32Id"))
                }
        }
        ownerName = intent.getStringExtra("OWNER_NAME").orEmpty()
        ownerEmail = intent.getStringExtra("OWNER_EMAIL").orEmpty()

        if (deviceId.isBlank()) {
            finish()
            return
        }

        setupOwnerUI()
        setupListeners()
        listenToDevice()
    }

    private fun setupOwnerUI() {
        if (isAdmin) {
            binding.tvDetailTitle.text = "Chi tiết thiết bị"
            binding.layoutOwnerSection.visibility = View.VISIBLE
            binding.tvOwnerName.text = ownerName.ifBlank { "Người dùng" }
            binding.tvOwnerEmail.text = ownerEmail
            binding.tvOwnerInitials.text = getInitials(ownerName)
            binding.tvOwnerLoginDate.visibility = View.GONE
        } else {
            binding.tvDetailTitle.text = "Chi tiết vật dụng"
            binding.layoutOwnerSection.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java).apply {
                putExtra("DEVICE_ID", deviceId)
                putExtra("USER_ID", currentDevice?.userId?.ifBlank { userId } ?: userId)
                putExtra("ESP32_ID", esp32Id)
            }
            startActivity(intent)
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.tvDetailStatus.setOnClickListener {
            val device = currentDevice ?: return@setOnClickListener
            val nextStatus = if (device.isBroken()) "active" else "broken"
            db.collection("devices").document(device.id)
                .update("status", nextStatus)
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun listenToDevice() {
        deviceListener = db.collection("devices").document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    finish()
                    return@addSnapshotListener
                }
                val device = DeviceModel.fromFirestore(snapshot)
                currentDevice = device
                bindDevice(device)
            }
    }

    private fun bindDevice(device: DeviceModel) {
        binding.tvDetailName.text = device.name
        binding.tvDetailCategory.text = device.getCategoryDisplayName()
        binding.tvDetailStatus.applyDeviceStatusBadge(device)
        val isEmptyImage = device.imageBase64.isBlank()
        val pad = if (isEmptyImage) (24 * resources.displayMetrics.density).toInt() else 0
        binding.ivDetailImage.setPadding(pad, pad, pad, pad)
        binding.ivDetailImage.scaleType = if (isEmptyImage) {
            ImageView.ScaleType.FIT_CENTER
        } else {
            ImageView.ScaleType.CENTER_CROP
        }
        UiHelper.loadBase64Image(binding.ivDetailImage, device.imageBase64, R.drawable.app_logo)
        binding.tvDetailPrice.text = device.getFormattedPrice()
        binding.tvDetailDate.text = device.getFormattedDate()
        binding.tvDetailNotes.text = device.note.trim()
    }

    private fun showDeleteConfirmDialog() {
        val device = currentDevice ?: return
        val dialog = Dialog(this)
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

        tvMessage.text = getString(R.string.dialog_delete_message, device.name)
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            deviceListener?.remove()
            deviceListener = null
            val ownerId = device.userId.ifBlank { userId }
            val mqttTargetId = MqttManager.resolveTargetId(esp32Id, ownerId)
            if (device.isElectronic() && device.gpioPin > 0L && mqttTargetId.isNotBlank()) {
                MqttManager.publish(MqttManager.provisionTopic(mqttTargetId, device.id), "", retain = true)
                MqttManager.publish(MqttManager.ackTopic(mqttTargetId, device.id), "", retain = true)
                MqttManager.publish(
                    MqttManager.removeTopic(mqttTargetId, device.id),
                    MqttManager.buildRemovePayload(device.id, device.gpioPin.toInt())
                )
            }
            db.collection("devices").document(device.id).delete()
            Toast.makeText(this, R.string.msg_item_deleted, Toast.LENGTH_SHORT).show()
            finish()
        }
        dialog.show()
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "ND"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceListener?.remove()
    }
}
