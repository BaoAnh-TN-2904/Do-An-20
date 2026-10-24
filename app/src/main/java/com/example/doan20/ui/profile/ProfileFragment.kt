package com.example.doan20.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.UserRole
import com.example.doan20.databinding.FragmentProfileBinding
import com.example.doan20.ui.notifications.NotificationsBottomSheet
import com.example.doan20.ui.viewmodel.ItemViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

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
        observeData()
    }

    private fun setupListeners() {
        binding.btnEditProfileInfo.setOnClickListener {
            EditProfileBottomSheet.newInstance().show(
                childFragmentManager,
                EditProfileBottomSheet.TAG
            )
        }

        binding.btnNotificationSettings.setOnClickListener {
            NotificationsBottomSheet.newInstance().show(
                childFragmentManager,
                NotificationsBottomSheet.TAG
            )
        }

        binding.btnAdminDashboard.setOnClickListener {
            findNavController().navigate(R.id.nav_admin)
        }

        binding.btnSettingsLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    viewModel.logout()
                    Toast.makeText(requireContext(), "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                    findNavController().navigate(R.id.nav_login, null, navOptions)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            binding.tvProfileName.text = user.name
                            val roomInfo = if (user.roomName.isNotBlank()) " · ${user.roomName}" else ""
                            binding.tvProfileEmail.text = "@${user.username}$roomInfo"
                            binding.tvAvatarInitials.text = user.initials
                            binding.btnAdminDashboard.visibility = if (user.role == UserRole.ADMIN) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.allItems.collect { items ->
                        binding.tvMemoryCount.text = "${items.size} vật dụng đang được lưu giữ"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
