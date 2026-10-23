package com.example.doan20.data.repository

import com.example.doan20.data.model.User
import com.example.doan20.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRepository {

    val SAMPLE_USER = User(
        email = "minhanh.nguyen@gmail.com",
        name = "Nguyễn Minh Anh",
        role = UserRole.USER,
        roomName = "Phòng 402 • Căn hộ Harmony"
    )

    val SAMPLE_ADMIN = User(
        email = "admin@gmail.com",
        name = "Quản trị viên",
        role = UserRole.ADMIN,
        roomName = "Hệ thống Quản trị Trung tâm"
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun login(email: String, pass: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập địa chỉ email!"))
        }

        if (cleanPass.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập mật khẩu!"))
        }

        // Admin account
        if (cleanEmail == "admin@gmail.com") {
            if (cleanPass == "123") {
                _currentUser.value = SAMPLE_ADMIN
                return Result.success(SAMPLE_ADMIN)
            } else {
                return Result.failure(Exception("Mật khẩu Admin không đúng! (Mặc định: 123)"))
            }
        }

        // Standard sample user
        if (cleanEmail == "minhanh.nguyen@gmail.com") {
            if (cleanPass == "123") {
                _currentUser.value = SAMPLE_USER
                return Result.success(SAMPLE_USER)
            } else {
                return Result.failure(Exception("Mật khẩu không đúng! (Mặc định: 123)"))
            }
        }

        // Any other email account
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            if (cleanPass == "123") {
                val displayName = cleanEmail.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                val newUser = User(
                    email = cleanEmail,
                    name = displayName,
                    role = UserRole.USER,
                    roomName = "Phòng cá nhân"
                )
                _currentUser.value = newUser
                return Result.success(newUser)
            } else {
                return Result.failure(Exception("Mật khẩu không đúng! (Mật khẩu mặc định: 123)"))
            }
        }

        return Result.failure(Exception("Định dạng email không hợp lệ!"))
    }

    fun logout() {
        _currentUser.value = null
    }

    fun getCurrentUser(): User? = _currentUser.value
}
