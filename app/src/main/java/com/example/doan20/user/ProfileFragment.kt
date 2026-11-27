package com.example.doan20.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan20.admin.AdminDashboardActivity
import com.example.doan20.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment(), MainActivity.DataRefreshable {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        refreshUI()
    }

    private fun setupListeners() {
        binding.btnEditProfileInfo.setOnClickListener {
            EditProfileBottomSheet().show(childFragmentManager, EditProfileBottomSheet.TAG)
        }

        binding.btnNotificationSettings.setOnClickListener {
            NotificationsBottomSheet().show(childFragmentManager, NotificationsBottomSheet.TAG)
        }

        binding.btnAdminDashboard.setOnClickListener {
            startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
        }

        binding.btnSettingsLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    Toast.makeText(requireContext(), "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.logout()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun refreshUI() {
        if (_binding == null) return
        val main = activity as? MainActivity ?: return
        val user = main.currentUser
        if (user != null) {
            binding.tvProfileName.text = user.fullName
            val roomInfo = if (user.roomName.isNotBlank()) " · ${user.roomName}" else ""
            binding.tvProfileEmail.text = "${user.email}$roomInfo"
            binding.tvAvatarInitials.text = user.getInitials()
            binding.btnAdminDashboard.visibility = if (user.isAdmin()) View.VISIBLE else View.GONE
        }
        binding.tvMemoryCount.text = "${main.deviceList.size} vật dụng đang được lưu giữ"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
