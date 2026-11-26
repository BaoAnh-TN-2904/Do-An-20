package com.example.doan20.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.doan20.R
import com.example.doan20.data.model.UserRole
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
        setupWindowInsets()
        setupListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            binding.tilUsername.error = null
            binding.tilPassword.error = null

            if (username.isBlank()) {
                binding.tilUsername.error = "Vui lòng nhập tên đăng nhập"
                binding.etUsername.requestFocus()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                binding.tilPassword.error = "Vui lòng nhập mật khẩu"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            // Show progress
            setLoading(true)

            viewModel.login(username, password) { result ->
                setLoading(false)
                result.onSuccess { user ->
                    Toast.makeText(
                        requireContext(),
                        "Đăng nhập thành công! Chào ${user.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_login, true)
                        .build()

                    val actionId = if (user.role == UserRole.ADMIN) {
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

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
