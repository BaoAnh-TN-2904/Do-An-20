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
import com.example.doan20.databinding.FragmentRegisterBinding
import com.example.doan20.ui.viewmodel.ItemViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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
        binding.btnBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSubmitRegister.setOnClickListener {
            val username = binding.etRegUsername.text?.toString()?.trim().orEmpty()
            val name = binding.etRegName.text?.toString()?.trim().orEmpty()
            val room = binding.etRegRoom.text?.toString()?.trim().orEmpty()
            val password = binding.etRegPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = binding.etRegConfirmPassword.text?.toString()?.trim().orEmpty()

            // Reset errors
            binding.tilRegUsername.error = null
            binding.tilRegName.error = null
            binding.tilRegPassword.error = null
            binding.tilRegConfirmPassword.error = null

            // Validate
            if (username.length < 3) {
                binding.tilRegUsername.error = "Tên đăng nhập tối thiểu 3 ký tự"
                binding.etRegUsername.requestFocus()
                return@setOnClickListener
            }

            if (username.contains(" ") || username.contains("@")) {
                binding.tilRegUsername.error = "Tên đăng nhập không được chứa khoảng trắng"
                binding.etRegUsername.requestFocus()
                return@setOnClickListener
            }

            if (name.isBlank()) {
                binding.tilRegName.error = "Vui lòng nhập họ và tên"
                binding.etRegName.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.tilRegPassword.error = "Mật khẩu phải từ 6 ký tự trở lên"
                binding.etRegPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.tilRegConfirmPassword.error = "Mật khẩu xác nhận không khớp!"
                binding.etRegConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            setLoading(true)

            viewModel.register(username, name, room, password) { result ->
                setLoading(false)
                result.onSuccess { user ->
                    Toast.makeText(
                        requireContext(),
                        "Đăng ký thành công! Chào mừng ${user.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_login, true)
                        .build()

                    findNavController().navigate(R.id.action_register_to_home, null, navOptions)
                }.onFailure { error ->
                    binding.tilRegUsername.error = error.message ?: "Đăng ký thất bại"
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmitRegister.isEnabled = !isLoading
        binding.etRegUsername.isEnabled = !isLoading
        binding.etRegName.isEnabled = !isLoading
        binding.etRegRoom.isEnabled = !isLoading
        binding.etRegPassword.isEnabled = !isLoading
        binding.etRegConfirmPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
