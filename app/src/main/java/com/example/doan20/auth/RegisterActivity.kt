package com.example.doan20.auth

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan20.R
import com.example.doan20.databinding.ActivityRegisterBinding
import com.example.doan20.utils.UiHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelper.applySystemBarPadding(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Chuyển về màn hình đăng nhập
        binding.tvLogin.setOnClickListener {
            finish()
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

        // Ẩn/hiện xác nhận mật khẩu
        binding.ivToggleConfirmPass.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                binding.etConfirmPass.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivToggleConfirmPass.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etConfirmPass.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivToggleConfirmPass.setImageResource(R.drawable.ic_eye_open)
            }
            binding.etConfirmPass.setSelection(binding.etConfirmPass.text.length)
        }

        // Đăng ký
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPass.text.toString()
            val confirmPass = binding.etConfirmPass.text.toString()

            if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPass.isBlank()) {
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

            if (password != confirmPass) {
                binding.etConfirmPass.error = "Mật khẩu xác nhận không khớp"
                binding.etConfirmPass.requestFocus()
                return@setOnClickListener
            }

            registerUser(fullName, email, password)
        }
    }

    private fun registerUser(fullName: String, email: String, password: String) {
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Đang xử lý..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val userMap = hashMapOf(
                        "uid" to userId,
                        "fullName" to fullName,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            resetRegisterButton()
                            Toast.makeText(this, "Lỗi lưu thông tin: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    resetRegisterButton()
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký!"
                        else -> "Đăng ký thất bại: ${task.exception?.localizedMessage}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun resetRegisterButton() {
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = "Đăng ký"
    }
}
