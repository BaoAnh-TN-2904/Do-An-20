package com.example.doan20.data.repository

import android.util.Log
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.data.model.User
import com.example.doan20.data.model.UserRole
import com.example.doan20.data.mqtt.MqttManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRepository {
    private const val TAG = "AuthRepository"

    val SAMPLE_ADMIN = User(
        username = "admin",
        name = "Quản trị viên",
        role = UserRole.ADMIN,
        roomName = "Hệ thống Quản trị Trung tâm",
        email = "admin@doan20.local",
        uid = "admin_01"
    )

    val SAMPLE_USER = User(
        username = "user",
        name = "Nguyễn Văn Nam",
        role = UserRole.USER,
        roomName = "Phòng 302 • Căn hộ Harmony",
        email = "user@doan20.local",
        uid = "sample_user_01"
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun register(
        username: String,
        name: String,
        roomName: String,
        pass: String,
        onComplete: (Result<User>) -> Unit
    ) {
        val cleanUser = username.trim().lowercase()
        val cleanPass = pass.trim()
        val cleanName = name.trim()
        val cleanRoom = if (roomName.isBlank()) "Phòng cá nhân" else roomName.trim()

        if (cleanUser.length < 3) {
            onComplete(Result.failure(Exception("Tên đăng nhập phải có ít nhất 3 ký tự!")))
            return
        }

        if (cleanUser.contains(" ") || cleanUser.contains("@")) {
            onComplete(Result.failure(Exception("Tên đăng nhập chỉ được chứa chữ cái và số, không có khoảng trắng hoặc ký tự đặc biệt!")))
            return
        }

        if (cleanName.isBlank()) {
            onComplete(Result.failure(Exception("Vui lòng nhập họ và tên!")))
            return
        }

        if (cleanPass.length < 6) {
            onComplete(Result.failure(Exception("Mật khẩu phải có tối thiểu 6 ký tự!")))
            return
        }

        // Map username -> Firebase Auth internal email
        val internalEmail = "$cleanUser@doan20.local"

        try {
            auth.createUserWithEmailAndPassword(internalEmail, cleanPass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: cleanUser
                    val newUser = User(
                        username = cleanUser,
                        name = cleanName,
                        roomName = cleanRoom,
                        role = UserRole.USER,
                        email = internalEmail,
                        uid = uid
                    )

                    // Save user profile in Firestore: users/{cleanUser}
                    saveUserToFirestore(newUser) {
                        _currentUser.value = newUser
                        MqttManager.connect(clientIdPrefix = cleanUser)
                        onComplete(Result.success(newUser))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Firebase register failed: ${exception.message}")
                    if (exception is FirebaseAuthUserCollisionException) {
                        onComplete(Result.failure(Exception("Tên đăng nhập '$cleanUser' đã tồn tại! Vui lòng chọn tên khác.")))
                    } else {
                        // Offline or unconfigured Firebase project fallback
                        val fallbackUser = User(
                            username = cleanUser,
                            name = cleanName,
                            roomName = cleanRoom,
                            role = UserRole.USER,
                            email = internalEmail,
                            uid = cleanUser
                        )
                        saveUserToFirestore(fallbackUser) {
                            _currentUser.value = fallbackUser
                            MqttManager.connect(clientIdPrefix = cleanUser)
                            onComplete(Result.success(fallbackUser))
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during register", e)
            val fallbackUser = User(
                username = cleanUser,
                name = cleanName,
                roomName = cleanRoom,
                role = UserRole.USER,
                email = internalEmail,
                uid = cleanUser
            )
            saveUserToFirestore(fallbackUser) {
                _currentUser.value = fallbackUser
                MqttManager.connect(clientIdPrefix = cleanUser)
                onComplete(Result.success(fallbackUser))
            }
        }
    }

    fun login(
        username: String,
        pass: String,
        onComplete: (Result<User>) -> Unit
    ) {
        val cleanUser = username.trim().lowercase()
        val cleanPass = pass.trim()

        if (cleanUser.isBlank()) {
            onComplete(Result.failure(Exception("Vui lòng nhập tên đăng nhập!")))
            return
        }

        if (cleanPass.isBlank()) {
            onComplete(Result.failure(Exception("Vui lòng nhập mật khẩu!")))
            return
        }

        // Shortcut 1: Admin
        if (cleanUser == "admin" || cleanUser == "admin@gmail.com" || cleanUser == "admin@doan20.local") {
            if (cleanPass == "123" || cleanPass == "123456" || cleanPass == "admin123") {
                _currentUser.value = SAMPLE_ADMIN
                saveUserToFirestore(SAMPLE_ADMIN) {}
                MqttManager.connect(clientIdPrefix = "admin")
                onComplete(Result.success(SAMPLE_ADMIN))
                return
            } else {
                onComplete(Result.failure(Exception("Mật khẩu Admin không đúng! (Mặc định: 123)")))
                return
            }
        }

        // Shortcut 2: Sample User
        if (cleanUser == "user" || cleanUser == "user@gmail.com" || cleanUser == "user01" || cleanUser == "user@doan20.local") {
            if (cleanPass == "123" || cleanPass == "123456") {
                _currentUser.value = SAMPLE_USER
                syncUserToFirebase(SAMPLE_USER, cleanPass)
                MqttManager.connect(clientIdPrefix = SAMPLE_USER.username)
                onComplete(Result.success(SAMPLE_USER))
                return
            } else {
                onComplete(Result.failure(Exception("Mật khẩu User mẫu không đúng! (Mặc định: 123456 hoặc 123)")))
                return
            }
        }

        // Firebase Auth login via internal email mapping
        val internalEmail = if (cleanUser.contains("@")) cleanUser else "$cleanUser@doan20.local"

        try {
            auth.signInWithEmailAndPassword(internalEmail, cleanPass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: cleanUser
                    // Fetch user details from Firestore
                    fetchUserFromFirestore(cleanUser, uid) { user ->
                        _currentUser.value = user
                        MqttManager.connect(clientIdPrefix = user.username)
                        onComplete(Result.success(user))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Firebase signIn failed: ${exception.message}")
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            onComplete(Result.failure(Exception("Tài khoản '$cleanUser' không tồn tại! Vui lòng đăng ký tài khoản mới.")))
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            onComplete(Result.failure(Exception("Mật khẩu không chính xác!")))
                        }
                        else -> {
                            // Fallback for offline/local test accounts
                            if (cleanPass == "123" || cleanPass.length >= 6) {
                                val fallback = User(
                                    username = cleanUser,
                                    name = cleanUser.replaceFirstChar { it.uppercase() },
                                    roomName = "Phòng cá nhân",
                                    role = UserRole.USER,
                                    email = internalEmail,
                                    uid = cleanUser
                                )
                                _currentUser.value = fallback
                                saveUserToFirestore(fallback) {}
                                MqttManager.connect(clientIdPrefix = cleanUser)
                                onComplete(Result.success(fallback))
                            } else {
                                onComplete(Result.failure(Exception("Đăng nhập thất bại: ${exception.localizedMessage}")))
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during login", e)
            val fallback = User(
                username = cleanUser,
                name = cleanUser.replaceFirstChar { it.uppercase() },
                roomName = "Phòng cá nhân",
                role = UserRole.USER,
                email = internalEmail,
                uid = cleanUser
            )
            _currentUser.value = fallback
            saveUserToFirestore(fallback) {}
            MqttManager.connect(clientIdPrefix = cleanUser)
            onComplete(Result.success(fallback))
        }
    }

    private fun saveUserToFirestore(user: User, onDone: () -> Unit) {
        try {
            val userMap = hashMapOf(
                "username" to user.username,
                "name" to user.name,
                "roomName" to user.roomName,
                "role" to user.role.name,
                "email" to user.email,
                "uid" to user.uid
            )
            firestore.collection("users").document(user.username)
                .set(userMap)
                .addOnCompleteListener { onDone() }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Firestore", e)
            onDone()
        }
    }

    private fun syncUserToFirebase(user: User, pass: String) {
        saveUserToFirestore(user) {
            seedSampleItemsIfEmpty(user.username)
        }

        try {
            auth.signInWithEmailAndPassword(user.email, pass)
                .addOnSuccessListener {
                    seedSampleItemsIfEmpty(user.username)
                }
                .addOnFailureListener {
                    auth.createUserWithEmailAndPassword(user.email, pass)
                        .addOnCompleteListener {
                            seedSampleItemsIfEmpty(user.username)
                        }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase auth sync exception: ${e.message}")
            seedSampleItemsIfEmpty(user.username)
        }
    }

    private fun seedSampleItemsIfEmpty(username: String) {
        try {
            firestore.collection("users")
                .document(username)
                .collection("items")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot == null || snapshot.isEmpty) {
                        val sampleItems = listOf(
                            Item(
                                id = "item_lamp_01",
                                name = "Đèn bàn học",
                                category = "Điện tử",
                                imageType = "lamp",
                                status = ItemStatus.ACTIVE,
                                hasSwitch = true,
                                isOn = true,
                                price = 250000L,
                                buyDate = "10/01/2024",
                                pinConfig = "GPIO 18",
                                notes = "Đèn LED bảo vệ thị lực, kết nối chân GPIO 18"
                            ),
                            Item(
                                id = "item_fan_01",
                                name = "Quạt bàn Senko",
                                category = "Điện tử",
                                imageType = "fan",
                                status = ItemStatus.ACTIVE,
                                hasSwitch = true,
                                isOn = false,
                                price = 380000L,
                                buyDate = "20/02/2024",
                                pinConfig = "GPIO 23",
                                notes = "Quạt làm mát bàn làm việc"
                            ),
                            Item(
                                id = "item_desk_01",
                                name = "Bàn làm việc gỗ sồi",
                                category = "Nội thất",
                                imageType = "desk",
                                status = ItemStatus.ACTIVE,
                                hasSwitch = false,
                                isOn = false,
                                price = 1200000L,
                                buyDate = "15/09/2023",
                                notes = "Bàn làm việc 1m2 góc phòng"
                            ),
                            Item(
                                id = "item_cooker_01",
                                name = "Nồi cơm điện mini",
                                category = "Điện tử",
                                imageType = "cooker",
                                status = ItemStatus.BROKEN,
                                hasSwitch = false,
                                isOn = false,
                                price = 450000L,
                                buyDate = "05/11/2023",
                                notes = "Nắp nồi bị lỏng chốt đóng ngắt, cần bảo dưỡng sửa chữa"
                            )
                        )
                        for (item in sampleItems) {
                            firestore.collection("users")
                                .document(username)
                                .collection("items")
                                .document(item.id)
                                .set(item)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding sample items", e)
        }
    }

    private fun fetchUserFromFirestore(username: String, uid: String, onLoaded: (User) -> Unit) {
        try {
            firestore.collection("users").document(username)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: username
                        val roomName = document.getString("roomName") ?: "Phòng cá nhân"
                        val roleStr = document.getString("role") ?: "USER"
                        val role = if (roleStr == "ADMIN") UserRole.ADMIN else UserRole.USER
                        val user = User(
                            username = username,
                            name = name,
                            roomName = roomName,
                            role = role,
                            email = "$username@doan20.local",
                            uid = uid
                        )
                        onLoaded(user)
                    } else {
                        onLoaded(createDefaultUser(username, uid))
                    }
                }
                .addOnFailureListener {
                    onLoaded(createDefaultUser(username, uid))
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Firestore", e)
            onLoaded(createDefaultUser(username, uid))
        }
    }

    private fun createDefaultUser(username: String, uid: String): User {
        return User(
            username = username,
            name = username.replaceFirstChar { it.uppercase() },
            roomName = "Phòng cá nhân",
            role = UserRole.USER,
            email = "$username@doan20.local",
            uid = uid
        )
    }

    fun updateProfile(
        newName: String,
        newRoomName: String,
        onComplete: (Result<User>) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onComplete(Result.failure(Exception("Chưa đăng nhập!")))
            return
        }
        val cleanName = newName.trim()
        val cleanRoom = if (newRoomName.isBlank()) "Phòng cá nhân" else newRoomName.trim()

        if (cleanName.isBlank()) {
            onComplete(Result.failure(Exception("Vui lòng nhập họ và tên!")))
            return
        }

        val updatedUser = user.copy(name = cleanName, roomName = cleanRoom)

        try {
            val fbUser = auth.currentUser
            if (fbUser != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                fbUser.updateProfile(profileUpdates)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update Auth displayName failed: ${e.message}")
        }

        saveUserToFirestore(updatedUser) {
            _currentUser.value = updatedUser
            onComplete(Result.success(updatedUser))
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error during signOut", e)
        }
        MqttManager.disconnect()
        _currentUser.value = null
    }

    fun getCurrentUser(): User? = _currentUser.value
}
