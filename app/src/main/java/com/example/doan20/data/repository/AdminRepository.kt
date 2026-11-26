package com.example.doan20.data.repository

import android.util.Log
import com.example.doan20.data.model.AdminUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdminRepository {
    private const val TAG = "AdminRepository"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Admin account starts with an empty list until synced from Firestore
    private val _usersFlow = MutableStateFlow<List<AdminUser>>(emptyList())
    val usersFlow: StateFlow<List<AdminUser>> = _usersFlow.asStateFlow()

    // Backwards compatibility property
    val users: List<AdminUser> get() = _usersFlow.value

    val totalDevices: Int get() = _usersFlow.value.sumOf { it.deviceCount }
    val totalActive: Int get() = _usersFlow.value.sumOf { it.activeCount }
    val totalBroken: Int get() = _usersFlow.value.sumOf { it.brokenCount }

    private var usersListener: ListenerRegistration? = null
    private val itemListeners = mutableMapOf<String, ListenerRegistration>()

    fun startListening(onUpdate: (() -> Unit)? = null) {
        usersListener?.remove()
        clearItemListeners()

        try {
            usersListener = firestore.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen to users failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        _usersFlow.value = emptyList()
                        onUpdate?.invoke()
                        return@addSnapshotListener
                    }

                    // Only keep real registered users (exclude admin)
                    val realUserDocs = snapshot.documents.filter { doc ->
                        val uName = doc.getString("username") ?: doc.id
                        val role = doc.getString("role") ?: "USER"
                        !uName.equals("admin", ignoreCase = true) && !role.equals("ADMIN", ignoreCase = true)
                    }

                    if (realUserDocs.isEmpty()) {
                        _usersFlow.value = emptyList()
                        onUpdate?.invoke()
                        return@addSnapshotListener
                    }

                    val updatedUsers = realUserDocs.map { doc ->
                        val uId = doc.getString("username") ?: doc.id
                        val name = doc.getString("name") ?: uId
                        val email = doc.getString("email") ?: "$uId@doan20.local"
                        val existing = _usersFlow.value.find { it.id == uId }
                        AdminUser(
                            id = uId,
                            name = name,
                            initials = com.example.doan20.data.model.getInitials(name),
                            email = email,
                            deviceCount = existing?.deviceCount ?: 0,
                            activeCount = existing?.activeCount ?: 0,
                            brokenCount = existing?.brokenCount ?: 0
                        )
                    }

                    _usersFlow.value = updatedUsers
                    onUpdate?.invoke()

                    // Attach listener to each user's items subcollection to count devices
                    for (doc in realUserDocs) {
                        val uId = doc.getString("username") ?: doc.id
                        attachUserItemsListener(uId, onUpdate)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Firestore listener", e)
        }
    }

    private fun attachUserItemsListener(username: String, onUpdate: (() -> Unit)?) {
        if (itemListeners.containsKey(username)) return

        try {
            val listener = firestore.collection("users")
                .document(username)
                .collection("items")
                .addSnapshotListener { itemSnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen to items of $username failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    val docs = itemSnapshot?.documents ?: emptyList()
                    val devCount = docs.size
                    val actCount = docs.count { it.getString("status") == "ACTIVE" || it.getString("status") == null }
                    val brkCount = docs.count { it.getString("status") == "BROKEN" }

                    _usersFlow.value = _usersFlow.value.map { u ->
                        if (u.id == username) {
                            u.copy(
                                deviceCount = devCount,
                                activeCount = actCount,
                                brokenCount = brkCount
                            )
                        } else {
                            u
                        }
                    }
                    onUpdate?.invoke()
                }
            itemListeners[username] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching items listener for $username", e)
        }
    }

    fun stopListening() {
        usersListener?.remove()
        usersListener = null
        clearItemListeners()
    }

    private fun clearItemListeners() {
        itemListeners.values.forEach { it.remove() }
        itemListeners.clear()
    }
}
