package com.example.doan20.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan20.auth.LoginActivity
import com.example.doan20.databinding.ActivityAdminDashboardBinding
import com.example.doan20.model.DeviceModel
import com.example.doan20.model.UserModel
import com.example.doan20.utils.MqttManager
import com.example.doan20.utils.UiHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var userAdapter: AdminUserAdapter

    private val allUsers = mutableListOf<UserModel>()
    private val allDevices = mutableListOf<DeviceModel>()
    private var filteredUsers = mutableListOf<UserModel>()

    private var usersListener: ListenerRegistration? = null
    private var devicesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupSearch()
        setupLogout()
        loadDataFromFirestore()
    }

    private fun setupRecyclerView() {
        userAdapter = AdminUserAdapter(filteredUsers) { user ->
            val intent = Intent(this, AdminUserDetailActivity::class.java).apply {
                putExtra("USER_ID", user.uid)
                putExtra("USER_NAME", user.fullName)
                putExtra("USER_EMAIL", user.email)
                putExtra("USER_ROLE", user.role)
                putExtra("USER_CODE", user.userCode)
            }
            startActivity(intent)
        }

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
            adapter = userAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchUser.doAfterTextChanged { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            filterUsers(query)
        }
    }

    private fun setupLogout() {
        binding.btnAdminLogout.setOnClickListener {
            confirmLogout()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmLogout()
            }
        })
    }

    private fun filterUsers(query: String) {
        filteredUsers = if (query.isEmpty()) {
            allUsers.toMutableList()
        } else {
            allUsers.filter { user ->
                user.fullName.lowercase().contains(query) ||
                        user.userCode.lowercase().contains(query) ||
                        user.email.lowercase().contains(query)
            }.toMutableList()
        }

        userAdapter.updateData(filteredUsers)

        if (filteredUsers.isEmpty()) {
            binding.layoutEmptyUsers.visibility = View.VISIBLE
            binding.rvUsers.visibility = View.GONE
            binding.tvEmptyUsers.text = if (allUsers.isEmpty()) {
                "Chưa có người dùng nào trong hệ thống"
            } else {
                "Không tìm thấy người dùng phù hợp"
            }
        } else {
            binding.layoutEmptyUsers.visibility = View.GONE
            binding.rvUsers.visibility = View.VISIBLE
        }

        binding.tvUserCountBadge.text = "${filteredUsers.size} người dùng"
    }

    private fun loadDataFromFirestore() {
        devicesListener = db.collection("devices")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Lỗi tải thiết bị: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    updateDashboardStats()
                    return@addSnapshotListener
                }

                allDevices.clear()
                snapshot?.documents?.forEach { doc ->
                    allDevices.add(DeviceModel.fromFirestore(doc))
                }
                updateDashboardStats()
            }

        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Lỗi tải người dùng: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    binding.tvEmptyUsers.text = "Lỗi kết nối: ${error.localizedMessage}"
                    binding.layoutEmptyUsers.visibility = View.VISIBLE
                    binding.rvUsers.visibility = View.GONE
                    allUsers.clear()
                    updateDashboardStats()
                    return@addSnapshotListener
                }

                allUsers.clear()
                var userIndex = 1

                snapshot?.documents?.forEach { doc ->
                    val role = doc.getString("role")?.trim()?.lowercase().orEmpty()
                    if (role != "admin") {
                        val fullName = doc.getString("fullName")
                            ?: doc.getString("name")
                            ?: "Chưa đặt tên"
                        val email = doc.getString("email") ?: ""
                        val uid = doc.getString("uid") ?: doc.id
                        val userCode = doc.getString("userCode") ?: "ND-%03d".format(userIndex++)

                        allUsers.add(
                            UserModel(
                                uid = uid,
                                fullName = fullName,
                                email = email,
                                role = role.ifBlank { "user" },
                                userCode = userCode,
                                deviceCount = 0,
                                createdAt = doc.get("createdAt")
                            )
                        )
                    }
                }

                updateDashboardStats()
            }
    }

    private fun updateDashboardStats() {
        val deviceCountMap = allDevices.groupBy { it.userId }.mapValues { it.value.size }
        allUsers.forEach { user ->
            user.deviceCount = deviceCountMap[user.uid] ?: 0
        }

        val totalUsers = allUsers.size
        val totalDevices = allDevices.size
        val activeDevices = allDevices.count { it.isActive() }
        val brokenDevices = totalDevices - activeDevices

        binding.tvTotalUsers.text = totalUsers.toString()
        binding.tvTotalDevices.text = totalDevices.toString()
        binding.tvDeviceSubtext.text = "$activeDevices hoạt động · $brokenDevices hỏng"
        binding.tvUserCountBadge.text = "$totalUsers người dùng"

        val currentQuery = binding.etSearchUser.text?.toString()?.trim()?.lowercase().orEmpty()
        filterUsers(currentQuery)
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đăng xuất Quản trị")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Quản trị viên không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                MqttManager.disconnect()
                Toast.makeText(this, "Đã đăng xuất Quản trị viên", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.remove()
        devicesListener?.remove()
    }
}
