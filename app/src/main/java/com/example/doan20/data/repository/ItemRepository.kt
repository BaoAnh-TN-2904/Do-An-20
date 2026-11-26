package com.example.doan20.data.repository

import android.util.Log
import com.example.doan20.data.model.Item
import com.example.doan20.data.model.ItemStatus
import com.example.doan20.data.mqtt.MqttManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object ItemRepository {
    private const val TAG = "ItemRepository"

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val itemsFlow: StateFlow<List<Item>> = _items.asStateFlow()

    private val _provisionAck = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 5)
    val provisionAck: SharedFlow<Pair<String, String>> = _provisionAck.asSharedFlow()

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
                                val item = doc.toObject(Item::class.java) ?: return@mapNotNull null
                                val isOnValue = doc.getBoolean("isOn")
                                    ?: doc.getBoolean("on")
                                    ?: item.isOn
                                val hasSwitchValue = doc.getBoolean("hasSwitch")
                                    ?: doc.getBoolean("switch")
                                    ?: item.hasSwitch
                                item.copy(id = doc.id, isOn = isOnValue, hasSwitch = hasSwitchValue)
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

        // Subscribe to MQTT topic for device updates (supports both JSON and raw "1"/"0")
        val subTopic = "doan20/$username/device/+/state"
        MqttManager.subscribe(subTopic) { topic, payload ->
            val parts = topic.split("/")
            if (parts.size >= 5) {
                val deviceId = parts[3]
                val trimmed = payload.trim()
                val isOn = if (trimmed.startsWith("{")) {
                    try {
                        val obj = org.json.JSONObject(trimmed)
                        val st = obj.opt("state")
                        when (st) {
                            is Number -> st.toInt() == 1
                            is Boolean -> st
                            is String -> st == "1" || st.equals("ON", ignoreCase = true)
                            else -> false
                        }
                    } catch (e: Exception) {
                        trimmed.contains("\"state\":1") || trimmed.contains("\"state\": 1")
                    }
                } else {
                    trimmed == "1" || trimmed.equals("ON", ignoreCase = true)
                }
                updateSwitchLocalOnly(deviceId, isOn)
            }
        }

        // Subscribe to MQTT ACK for provision responses from ESP32
        val ackTopic = "doan20/$username/device/+/ack"
        MqttManager.subscribe(ackTopic) { topic, payload ->
            val parts = topic.split("/")
            if (parts.size >= 5) {
                val deviceId = parts[3]
                _provisionAck.tryEmit(Pair(deviceId, payload))
            }
        }
    }

    fun sendProvisionRequest(item: Item, gpioPin: Int) {
        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username ?: "user"
        val topic = "doan20/$username/device/${item.id}/provision"
        val payload = org.json.JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("gpio", gpioPin)
            put("action", "INIT")
        }.toString()
        MqttManager.publish(topic, payload)
    }

    fun extractGpioPin(pinConfig: String?): Int? {
        if (pinConfig.isNullOrBlank()) return null
        val match = Regex("""\d+""").find(pinConfig)
        return match?.value?.toIntOrNull()
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
        val currentItem = getItemById(id)
        if (currentItem != null && currentItem.isOn == isOn) {
            return
        }

        _items.value = _items.value.map { item ->
            if (item.id == id && item.hasSwitch) {
                item.copy(isOn = isOn)
            } else {
                item
            }
        }

        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username ?: "user"

        // 1. Publish to HiveMQ MQTT Broker with JSON containing gpio pin & state
        val mqttTopic = "doan20/$username/device/$id/switch"
        val gpioPin = extractGpioPin(currentItem?.pinConfig)
        val jsonPayload = org.json.JSONObject().apply {
            put("id", id)
            put("state", if (isOn) 1 else 0)
            if (gpioPin != null) {
                put("gpio", gpioPin)
            }
        }.toString()
        MqttManager.publish(mqttTopic, jsonPayload)

        // 2. Sync to Firestore (update both "isOn" and legacy "on")
        try {
            firestore.collection("users")
                .document(username)
                .collection("items")
                .document(id)
                .update(
                    mapOf(
                        "isOn" to isOn,
                        "on" to isOn
                    )
                )
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
        val current = getItemById(id)
        if (current != null && current.isOn == isOn) return

        _items.value = _items.value.map { item ->
            if (item.id == id && item.hasSwitch) {
                item.copy(isOn = isOn)
            } else {
                item
            }
        }

        // Sync to Firestore so the database persists the state changed by ESP32 physical button
        val username = currentActiveUsername ?: AuthRepository.getCurrentUser()?.username
        if (!username.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(username)
                    .collection("items")
                    .document(id)
                    .update(
                        mapOf(
                            "isOn" to isOn,
                            "on" to isOn
                        )
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing ESP32 state to Firestore", e)
            }
        }
    }
}
