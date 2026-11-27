package com.example.doan20.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan20.R
import com.example.doan20.admin.AdminDashboardActivity
import com.example.doan20.databinding.ActivityLoginBinding
import com.example.doan20.user.MainActivity
import com.example.doan20.utils.UiHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        auth = FirebaseAuth.getInstance()

        // Chuyển sang màn hình đăng ký
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Ẩn/hiện mật khẩu
        binding.ivTogglePass.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPass.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePass.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etPass.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePass.setImageResource(R.drawable.ic_eye_open)
            }
            binding.etPass.setSelection(binding.etPass.text.length)
        }

        // Đăng nhập
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPass.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Email không đúng định dạng"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.etPass.error = "Mật khẩu phải có ít nhất 6 ký tự"
                binding.etPass.requestFocus()
                return@setOnClickListener
            }

            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Đang xử lý..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role")?.trim()?.lowercase().orEmpty()
                                val isAdmin = role == "admin" || email.contains("admin", ignoreCase = true)
                                navigateAfterLogin(isAdmin)
                            }
                            .addOnFailureListener {
                                val isAdmin = email.contains("admin", ignoreCase = true)
                                navigateAfterLogin(isAdmin)
                            }
                    } else {
                        navigateAfterLogin(false)
                    }
                } else {
                    resetLoginButton()
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại!"
                        is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không chính xác!"
                        else -> "Đăng nhập thất bại: ${task.exception?.localizedMessage}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateAfterLogin(isAdmin: Boolean) {
        val targetClass = if (isAdmin) {
            Toast.makeText(this, "Đăng nhập quyền Quản trị thành công!", Toast.LENGTH_SHORT).show()
            AdminDashboardActivity::class.java
        } else {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
            MainActivity::class.java
        }
        val intent = Intent(this, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun resetLoginButton() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "Đăng nhập"
    }
}
