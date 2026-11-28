package com.example.doan20.user

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.doan20.R
import com.example.doan20.auth.LoginActivity
import com.example.doan20.databinding.ActivityMainBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.model.UserModel
import com.example.doan20.utils.MqttManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var currentUser: UserModel? = null
    val deviceList = mutableListOf<DeviceModel>()

    var filterStatus: String? = null   // null, "active", "broken"
    var filterCategory: String? = null // null hoặc tên hiển thị

    private var userListener: ListenerRegistration? = null
    private var devicesListener: ListenerRegistration? = null
    private var currentTabIndex = 0
    private var activeMqttTargetId: String = ""

    private var homeFragment = HomeFragment()
    private var itemsFragment = ItemsFragment()
    private var statisticsFragment = StatisticsFragment()
    private var profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupTabs(savedInstanceState)
        setupBackHandler()
        loadCurrentUserAndDevices()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.fragmentContainer.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupTabs(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, "tab_3").hide(profileFragment)
                .add(R.id.fragmentContainer, statisticsFragment, "tab_2").hide(statisticsFragment)
                .add(R.id.fragmentContainer, itemsFragment, "tab_1").hide(itemsFragment)
                .add(R.id.fragmentContainer, homeFragment, "tab_0")
                .commit()
        } else {
            homeFragment = (supportFragmentManager.findFragmentByTag("tab_0") as? HomeFragment) ?: HomeFragment()
            itemsFragment = (supportFragmentManager.findFragmentByTag("tab_1") as? ItemsFragment) ?: ItemsFragment()
            statisticsFragment = (supportFragmentManager.findFragmentByTag("tab_2") as? StatisticsFragment) ?: StatisticsFragment()
            profileFragment = (supportFragmentManager.findFragmentByTag("tab_3") as? ProfileFragment) ?: ProfileFragment()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_items -> 1
                R.id.nav_statistics -> 2
                R.id.nav_profile -> 3
                else -> 0
            }
            if (index != currentTabIndex) {
                switchTab(index, updateBottomNav = false)
            }
            true
        }
    }

    fun switchTab(index: Int, updateBottomNav: Boolean = true) {
        if (index !in 0..3) return
        currentTabIndex = index

        val fragments = listOf(homeFragment, itemsFragment, statisticsFragment, profileFragment)
        val transaction = supportFragmentManager.beginTransaction()
        fragments.forEachIndexed { i, fragment ->
            if (i == index) transaction.show(fragment) else transaction.hide(fragment)
        }
        transaction.commitAllowingStateLoss()

        if (updateBottomNav) {
            val menuId = when (index) {
                0 -> R.id.nav_home
                1 -> R.id.nav_items
                2 -> R.id.nav_statistics
                3 -> R.id.nav_profile
                else -> R.id.nav_home
            }
            if (binding.bottomNavigation.selectedItemId != menuId) {
                binding.bottomNavigation.selectedItemId = menuId
            }
        }
        notifyAllTabsUpdated()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTabIndex != 0) {
                    switchTab(0)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadCurrentUserAndDevices() {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            logout()
            return
        }
        val uid = fbUser.uid
        val email = fbUser.email.orEmpty()

        // Lắng nghe thông tin user từ users/{uid}
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val esp32Id = MqttManager.normalizeEsp32Id(doc.getString("esp32Id"))
                    currentUser = UserModel(
                        uid = uid,
                        fullName = doc.getString("fullName")
                            ?: doc.getString("name")
                            ?: email.substringBefore("@"),
                        email = doc.getString("email") ?: email,
                        role = doc.getString("role") ?: "user",
                        roomName = doc.getString("roomName")?.ifBlank { "Phòng cá nhân" } ?: "Phòng cá nhân",
                        userCode = doc.getString("userCode") ?: "",
                        esp32Id = esp32Id,
                        createdAt = doc.get("createdAt")
                    )
                } else {
                    currentUser = UserModel(
                        uid = uid,
                        fullName = email.substringBefore("@"),
                        email = email
                    )
                }
                updateMqttSubscription(uid, MqttManager.resolveTargetId(currentUser?.esp32Id, uid))
                notifyAllTabsUpdated()
            }

        // Lắng nghe thiết bị của user từ collection "devices"
        devicesListener = db.collection("devices")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Lỗi tải thiết bị: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                deviceList.clear()
                snapshot?.documents?.forEach { doc ->
                    deviceList.add(DeviceModel.fromFirestore(doc))
                }
                notifyAllTabsUpdated()
            }

        // Kết nối MQTT cho điều khiển ESP32
        updateMqttSubscription(uid, MqttManager.resolveTargetId(currentUser?.esp32Id, uid))
    }

    private fun updateMqttSubscription(uid: String, targetId: String) {
        if (targetId.isBlank() || targetId == activeMqttTargetId) return
        if (activeMqttTargetId.isNotBlank()) {
            MqttManager.unsubscribe(MqttManager.stateWildcardTopic(activeMqttTargetId))
            MqttManager.unsubscribe(MqttManager.ackWildcardTopic(activeMqttTargetId))
        }
        activeMqttTargetId = targetId
        MqttManager.connect(clientIdPrefix = uid)

        MqttManager.subscribe(MqttManager.stateWildcardTopic(targetId)) { topic, payload ->
            if (payload.isBlank()) return@subscribe
            val parts = topic.split("/")
            if (parts.size >= 5) {
                val deviceId = parts[3]
                val isOn = MqttManager.parseSwitchState(payload)
                runOnUiThread {
                    val target = deviceList.find { it.id == deviceId }
                    if (target != null && target.isOn != isOn) {
                        target.isOn = isOn
                        notifyAllTabsUpdated()
                        db.collection("devices").document(deviceId).update("isOn", isOn)
                    }
                }
            }
        }

        // Lắng nghe ACK từ ESP32 (kể cả khi ESP32 vừa online trở lại sau khi thêm thiết bị)
        MqttManager.subscribe(MqttManager.ackWildcardTopic(targetId)) { topic, payload ->
            val ack = MqttManager.parseProvisionAck(payload) ?: return@subscribe
            if (ack.status.equals("REMOVED", ignoreCase = true)) return@subscribe
            val parts = topic.split("/")
            if (parts.size >= 5) {
                val deviceId = parts[3]
                val newStatus = if (ack.isSuccess) "READY" else "FAILED"
                val newMsg = if (ack.isSuccess) "" else ack.message

                runOnUiThread {
                    val target = deviceList.find { it.id == deviceId }
                    if (target != null) {
                        target.provisionStatus = newStatus
                        target.provisionMessage = newMsg
                        notifyAllTabsUpdated()
                    }
                    db.collection("devices").document(deviceId).update(
                        mapOf(
                            "provisionStatus" to newStatus,
                            "provisionMessage" to newMsg
                        )
                    )
                    // Xóa retained /provision và /ack sau khi đã ghi nhận kết quả
                    MqttManager.publish(MqttManager.provisionTopic(targetId, deviceId), "", retain = true)
                    MqttManager.publish(MqttManager.ackTopic(targetId, deviceId), "", retain = true)

                    if (!ack.isSuccess && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                        val name = target?.name ?: "GPIO ${ack.gpio}"
                        Toast.makeText(
                            this,
                            "ESP32 khởi tạo thất bại ($name): ${ack.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun toggleDeviceSwitch(device: DeviceModel, isOn: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        device.isOn = isOn
        notifyAllTabsUpdated()

        db.collection("devices").document(device.id).update("isOn", isOn)

        val targetId = MqttManager.resolveTargetId(currentUser?.esp32Id, uid)
        val payload = MqttManager.buildSwitchPayload(device.id, isOn, device.gpioPin.toInt())
        MqttManager.publish(MqttManager.switchTopic(targetId, device.id), payload)
    }

    fun toggleDeviceStatus(device: DeviceModel) {
        val nextStatus = if (device.isBroken()) "active" else "broken"
        device.status = nextStatus
        notifyAllTabsUpdated()
        db.collection("devices").document(device.id).update("status", nextStatus)
    }

    fun notifyAllTabsUpdated() {
        val tabs: List<Fragment> = listOf(homeFragment, itemsFragment, statisticsFragment, profileFragment)
        tabs.forEach { fragment ->
            if (fragment.isAdded && fragment is DataRefreshable) {
                fragment.refreshUI()
            }
        }
    }

    fun logout() {
        auth.signOut()
        MqttManager.disconnect()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
        devicesListener?.remove()
    }

    interface DataRefreshable {
        fun refreshUI()
    }
}
