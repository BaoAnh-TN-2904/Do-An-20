package com.example.doan20.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan20.databinding.ActivityAdminUserDetailBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.user.AddItemActivity
import com.example.doan20.user.ItemDetailActivity
import com.example.doan20.utils.MqttManager
import com.example.doan20.utils.UiHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminUserDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminUserDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var deviceAdapter: AdminDeviceAdapter

    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserEmail: String = ""
    private var currentUserEsp32Id: String = ""
    private var activeMqttTargetId: String = ""

    private val userDevices = mutableListOf<DeviceModel>()
    private var devicesListener: ListenerRegistration? = null
    private var userDocListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        db = FirebaseFirestore.getInstance()

        currentUserId = intent.getStringExtra("USER_ID").orEmpty()
        currentUserName = intent.getStringExtra("USER_NAME").orEmpty().ifBlank { "Người dùng" }
        currentUserEmail = intent.getStringExtra("USER_EMAIL").orEmpty()
        currentUserEsp32Id = MqttManager.normalizeEsp32Id(intent.getStringExtra("ESP32_ID"))

        setupUI()
        setupRecyclerView()
        loadUserDevices()
        setupMqttSync()
    }

    private fun setupMqttSync() {
        if (currentUserId.isBlank()) return
        subscribeToTarget(MqttManager.resolveTargetId(currentUserEsp32Id, currentUserId))

        userDocListener = db.collection("users").document(currentUserId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    currentUserEsp32Id = MqttManager.normalizeEsp32Id(doc.getString("esp32Id"))
                    subscribeToTarget(MqttManager.resolveTargetId(currentUserEsp32Id, currentUserId))
                }
            }
    }

    private fun subscribeToTarget(targetId: String) {
        if (targetId.isBlank() || targetId == activeMqttTargetId) return
        if (activeMqttTargetId.isNotBlank()) {
            MqttManager.unsubscribe(MqttManager.stateWildcardTopic(activeMqttTargetId))
        }
        activeMqttTargetId = targetId
        val adminPrefix = FirebaseAuth.getInstance().currentUser?.uid ?: "admin"
        MqttManager.connect(clientIdPrefix = adminPrefix)
        MqttManager.subscribe(MqttManager.stateWildcardTopic(targetId)) { topic, payload ->
            val parts = topic.split("/")
            if (parts.size >= 5) {
                val deviceId = parts[3]
                val isOn = MqttManager.parseSwitchState(payload)
                runOnUiThread {
                    val target = userDevices.find { it.id == deviceId }
                    if (target != null && target.isOn != isOn) {
                        target.isOn = isOn
                        deviceAdapter.updateData(userDevices)
                        db.collection("devices").document(deviceId).update("isOn", isOn)
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnAddDevice.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java).apply {
                putExtra("USER_ID", currentUserId)
                putExtra("ESP32_ID", currentUserEsp32Id)
            }
            startActivity(intent)
        }

        binding.tvUserAvatar.text = getInitials(currentUserName)
        binding.tvDetailUserName.text = currentUserName
        binding.tvDetailUserEmail.text = currentUserEmail.ifBlank { "Chưa có email" }
        binding.tvSectionDeviceTitle.text = "Thiết bị của $currentUserName"
    }

    private fun setupRecyclerView() {
        deviceAdapter = AdminDeviceAdapter(
            deviceList = userDevices,
            onItemClick = { device ->
                val intent = Intent(this, ItemDetailActivity::class.java).apply {
                    putExtra("DEVICE_ID", device.id)
                    putExtra("USER_ID", currentUserId)
                    putExtra("ESP32_ID", currentUserEsp32Id)
                    putExtra("IS_ADMIN", true)
                    putExtra("OWNER_NAME", currentUserName)
                    putExtra("OWNER_EMAIL", currentUserEmail)
                }
                startActivity(intent)
            },
            onSwitchToggle = { device, isChecked ->
                device.isOn = isChecked
                val actionName = if (isChecked) "Bật" else "Tắt"
                Toast.makeText(this, "Đã $actionName ${device.name}", Toast.LENGTH_SHORT).show()

                if (device.id.isNotBlank()) {
                    db.collection("devices").document(device.id)
                        .update("isOn", isChecked)
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                    val payload = MqttManager.buildSwitchPayload(
                        device.id,
                        isChecked,
                        device.gpioPin.toInt()
                    )
                    val mqttTargetId = MqttManager.resolveTargetId(currentUserEsp32Id, currentUserId)
                    MqttManager.publish(
                        MqttManager.switchTopic(mqttTargetId, device.id),
                        payload
                    )
                }
            }
        )

        binding.rvUserDevices.apply {
            layoutManager = LinearLayoutManager(this@AdminUserDetailActivity)
            adapter = deviceAdapter
            isNestedScrollingEnabled = true
        }
    }

    private fun loadUserDevices() {
        devicesListener = db.collection("devices")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Lỗi tải thiết bị: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    binding.tvEmptyDevices.text = "Lỗi tải thiết bị: ${error.localizedMessage}"
                    binding.layoutEmptyDevices.visibility = View.VISIBLE
                    binding.rvUserDevices.visibility = View.GONE
                    userDevices.clear()
                    deviceAdapter.updateData(userDevices)
                    updateStatsCards()
                    return@addSnapshotListener
                }

                userDevices.clear()
                snapshot?.documents?.forEach { doc ->
                    userDevices.add(DeviceModel.fromFirestore(doc))
                }

                deviceAdapter.updateData(userDevices)
                updateStatsCards()

                if (userDevices.isEmpty()) {
                    binding.tvEmptyDevices.text = "Người dùng này chưa có thiết bị nào"
                    binding.layoutEmptyDevices.visibility = View.VISIBLE
                    binding.rvUserDevices.visibility = View.GONE
                } else {
                    binding.layoutEmptyDevices.visibility = View.GONE
                    binding.rvUserDevices.visibility = View.VISIBLE
                }
            }
    }

    private fun updateStatsCards() {
        val total = userDevices.size
        val active = userDevices.count { it.isActive() }
        val broken = userDevices.count { it.isBroken() }

        binding.tvDetailTotalDevices.text = total.toString()
        binding.tvDetailActiveDevices.text = active.toString()
        binding.tvDetailBrokenDevices.text = broken.toString()
        binding.tvDeviceCountSub.text = "$total thiết bị"
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "ND"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> {
                val firstChar = parts.first().firstOrNull()?.uppercase() ?: "N"
                val lastChar = parts.last().firstOrNull()?.uppercase() ?: "D"
                "$firstChar$lastChar"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        devicesListener?.remove()
        userDocListener?.remove()
        if (activeMqttTargetId.isNotBlank()) {
            MqttManager.unsubscribe(MqttManager.stateWildcardTopic(activeMqttTargetId))
        }
    }
}
