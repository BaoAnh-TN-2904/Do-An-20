package com.example.doan20.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.AdminUser
import com.example.doan20.data.repository.AdminRepository
import com.example.doan20.data.repository.AuthRepository
import com.example.doan20.data.repository.ItemRepository
import com.example.doan20.databinding.FragmentAdminBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUsers()
        AdminRepository.startListening()
    }

    private fun setupListeners() {
        binding.btnAdminBack.setOnClickListener {
            confirmLogout()
        }

        binding.etAdminSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AdminRepository.usersFlow.collect { users ->
                    updateStats(users)
                    applyFilter(binding.etAdminSearchUsers.text?.toString())
                }
            }
        }
    }

    private fun updateStats(users: List<AdminUser>) {
        binding.tvAdminTotalUsers.text = users.size.toString()
        binding.tvAdminTotalDevices.text = AdminRepository.totalDevices.toString()
    }

    private fun applyFilter(query: String?) {
        val q = query?.trim().orEmpty()
        val allUsers = AdminRepository.users
        val filtered = if (q.isBlank()) {
            allUsers
        } else {
            allUsers.filter {
                it.name.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true)
            }
        }
        populateUsers(filtered)
    }

    private fun populateUsers(users: List<AdminUser>) {
        binding.layoutUserCards.removeAllViews()
        binding.tvAdminUserCountNotice.text = "${users.size} người dùng"

        if (users.isEmpty()) {
            val emptyTextView = TextView(requireContext()).apply {
                text = if (binding.etAdminSearchUsers.text.isNullOrBlank()) {
                    "Chưa có người dùng nào đăng ký trong hệ thống."
                } else {
                    "Không tìm thấy người dùng phù hợp."
                }
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 48, 0, 48)
            }
            binding.layoutUserCards.addView(emptyTextView)
            return
        }

        for (user in users) {
            val userView = layoutInflater.inflate(R.layout.item_admin_user, binding.layoutUserCards, false)
            val tvInitials = userView.findViewById<TextView>(R.id.tvUserInitials)
            val tvName = userView.findViewById<TextView>(R.id.tvAdminUserName)
            val tvCode = userView.findViewById<TextView>(R.id.tvAdminUserCode)
            val tvDevices = userView.findViewById<TextView>(R.id.tvAdminDeviceCount)

            tvInitials.text = user.initials
            tvName.text = user.name
            tvCode.text = user.id
            tvDevices.text = user.deviceCount.toString()

            userView.setOnClickListener {
                AdminUserDetailBottomSheet.newInstance(user.id).show(
                    childFragmentManager,
                    AdminUserDetailBottomSheet.TAG
                )
            }

            binding.layoutUserCards.addView(userView)
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đăng xuất Quản trị")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Quản trị viên không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                AuthRepository.logout()
                ItemRepository.clear()
                AdminRepository.stopListening()
                Toast.makeText(requireContext(), "Đã đăng xuất Quản trị viên", Toast.LENGTH_SHORT).show()
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                findNavController().navigate(R.id.nav_login, null, navOptions)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdminRepository.stopListening()
        _binding = null
    }
}
