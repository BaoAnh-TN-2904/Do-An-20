package com.example.doan20.data.repository

import android.util.Log
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.data.mqtt.MqttManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ItemRepository {
    private const val TAG = "ItemRepository"

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val itemsFlow: StateFlow<List<Item>> = _items.asStateFlow()

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var firestoreListener: ListenerRegistration? = null
    private var currentActiveUsername: String? = null

    fun clear() {
        firestoreListener?.remove()
        firestoreListener = null
        currentActiveUsername = null
        _items.value = emptyList()
    }

    fun initForUser(username: String) {
        if (currentActiveUsername == username) return
        currentActiveUsername = username

        firestoreListener?.remove()
        _items.value = emptyList()

        // Attach Firestore listener: users/{username}/items
        try {
            firestoreListener = firestore.collection("users")
                .document(username)
                .collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore items listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        if (!snapshot.isEmpty) {
                            val cloudItems = snapshot.documents.mapNotNull { doc ->
                                val item = doc.toObject(Item::class.java)
                                item?.copy(id = doc.id)
                            }
                            _items.value = cloudItems
                        } else {
                            _items.value = emptyList()
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Firestore items", e)
        }

        // Subscribe to MQTT topic for device updates
        val subTopic = "doan20/$username/device/+/state"
        MqttManager.subscribe(subTopic) { topic, payload ->
            val parts = topic.split("/")
            if (parts.size >= 5) {
                val deviceId = parts[3]
                val isOn = payload.trim() == "1" || payload.trim().equals("ON", ignoreCase = true)
                updateSwitchLocalOnly(deviceId, isOn)
            }
        }
    }

    fun getItemById(id: String): Item? {
        return _items.value.find { it.id == id }
    }

    fun addItem(newItem: Item) {
        _items.value = listOf(newItem) + _items.value

        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username
        if (!username.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(username)
                    .collection("items")
                    .document(newItem.id)
                    .set(newItem)
                    .addOnFailureListener {
                        Log.w(TAG, "Failed to sync addItem to Firestore", it)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding item to Firestore", e)
            }
        }
    }

    fun updateItem(updatedItem: Item) {
        _items.value = _items.value.map { item ->
            if (item.id == updatedItem.id) updatedItem else item
        }

        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username
        if (!username.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(username)
                    .collection("items")
                    .document(updatedItem.id)
                    .set(updatedItem)
                    .addOnFailureListener {
                        Log.w(TAG, "Failed to sync updateItem to Firestore", it)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating item in Firestore", e)
            }
        }
    }

    fun deleteItem(id: String) {
        _items.value = _items.value.filterNot { it.id == id }

        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username
        if (!username.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(username)
                    .collection("items")
                    .document(id)
                    .delete()
                    .addOnFailureListener {
                        Log.w(TAG, "Failed to delete item from Firestore", it)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting item from Firestore", e)
            }
        }
    }

    fun setSwitchState(id: String, isOn: Boolean) {
        _items.value = _items.value.map { item ->
            if (item.id == id && item.hasSwitch) {
                item.copy(isOn = isOn)
            } else {
                item
            }
        }

        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username ?: "user"

        // 1. Publish to HiveMQ MQTT Broker
        val mqttTopic = "doan20/$username/device/$id/switch"
        val mqttPayload = if (isOn) "1" else "0"
        MqttManager.publish(mqttTopic, mqttPayload)

        // 2. Sync to Firestore
        try {
            firestore.collection("users")
                .document(username)
                .collection("items")
                .document(id)
                .update("isOn", isOn)
                .addOnFailureListener {
                    Log.w(TAG, "Failed to update switch in Firestore", it)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating switch in Firestore", e)
        }
    }

    fun toggleSwitch(id: String) {
        val currentItem = getItemById(id) ?: return
        setSwitchState(id, !currentItem.isOn)
    }

    fun toggleStatus(id: String) {
        var updatedItem: Item? = null
        _items.value = _items.value.map { item ->
            if (item.id == id) {
                val nextStatus = if (item.status == ItemStatus.ACTIVE) ItemStatus.BROKEN else ItemStatus.ACTIVE
                val newItem = item.copy(status = nextStatus)
                updatedItem = newItem
                newItem
            } else {
                item
            }
        }

        updatedItem?.let { updateItem(it) }
    }

    private fun updateSwitchLocalOnly(id: String, isOn: Boolean) {
        _items.value = _items.value.map { item ->
            if (item.id == id && item.hasSwitch) {
                item.copy(isOn = isOn)
            } else {
                item
            }
        }
    }
}
