package com.example.doan20.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.databinding.FragmentLoginBinding
import com.example.doan20.ui.viewmodel.ItemViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuickFill()
        setupListeners()
    }

    private fun setupQuickFill() {
        // Quick Fill for Admin account
        binding.cardQuickAdmin.setOnClickListener {
            binding.etEmail.setText("admin@gmail.com")
            binding.etPassword.setText("123")
            binding.tilEmail.error = null
            binding.tilPassword.error = null
        }

        // Quick Fill for Standard User account
        binding.cardQuickUser.setOnClickListener {
            binding.etEmail.setText("minhanh.nguyen@gmail.com")
            binding.etPassword.setText("123")
            binding.tilEmail.error = null
            binding.tilPassword.error = null
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            binding.tilEmail.error = null
            binding.tilPassword.error = null

            if (email.isBlank()) {
                binding.tilEmail.error = "Vui lòng nhập email"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                binding.tilPassword.error = "Vui lòng nhập mật khẩu"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            val result = viewModel.login(email, password)
            result.onSuccess { user ->
                Toast.makeText(
                    requireContext(),
                    "Đăng nhập thành công! Chào ${user.name}",
                    Toast.LENGTH_SHORT
                ).show()

                val actionId = if (user.role == com.example.doan20.data.model.UserRole.ADMIN) {
                    R.id.action_login_to_admin
                } else {
                    R.id.action_login_to_home
                }
                findNavController().navigate(actionId, null, navOptions)
            }.onFailure { error ->
                binding.tilPassword.error = error.message ?: "Đăng nhập thất bại"
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
