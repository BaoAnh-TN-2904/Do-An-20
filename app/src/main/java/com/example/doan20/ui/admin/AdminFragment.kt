package com.example.doan20.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.AdminUser
import com.example.doan20.data.repository.AdminRepository
import com.example.doan20.databinding.FragmentAdminBinding

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private var userList: List<AdminUser> = AdminRepository.users

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
        setupStats()
        setupListeners()
        populateUsers(userList)
    }

    private fun setupStats() {
        binding.tvAdminTotalUsers.text = AdminRepository.users.size.toString()
        binding.tvAdminTotalDevices.text = AdminRepository.TOTAL_DEVICES.toString()
    }

    private fun setupListeners() {
        binding.btnAdminBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.etAdminSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim().orEmpty()
                val filtered = if (q.isBlank()) {
                    AdminRepository.users
                } else {
                    AdminRepository.users.filter {
                        it.name.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true)
                    }
                }
                populateUsers(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun populateUsers(users: List<AdminUser>) {
        binding.layoutUserCards.removeAllViews()
        binding.tvAdminUserCountNotice.text = "${users.size} người dùng"

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
