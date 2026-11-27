package com.example.doan20.utils

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MqttManager {
    private const val TAG = "MqttManager"
    const val DEFAULT_HOST = "broker.hivemq.com"
    const val DEFAULT_PORT = 1883

    private var client: Mqtt3AsyncClient? = null
    private var activeClientPrefix: String? = null
    private val subscriptions = ConcurrentHashMap<String, (topic: String, payload: String) -> Unit>()
    private val pendingConnectCallbacks = mutableListOf<() -> Unit>()

    fun normalizeEsp32Id(esp32Id: String?): String {
        val clean = esp32Id?.trim()
            ?.uppercase()
            ?.replace(":", "")
            ?.replace("-", "")
            ?.replace(" ", "")
            .orEmpty()
        return if (clean.length > 6) clean.takeLast(6) else clean
    }

    fun isValidEsp32Code(code: String): Boolean {
        return code.matches(Regex("^[0-9A-F]{6}$"))
    }

    fun resolveTargetId(esp32Id: String?, fallbackUid: String): String {
        return normalizeEsp32Id(esp32Id).ifBlank { fallbackUid }
    }

    fun stateWildcardTopic(targetId: String) = "doan20/$targetId/device/+/state"
    fun switchTopic(targetId: String, deviceId: String) = "doan20/$targetId/device/$deviceId/switch"
    fun provisionTopic(targetId: String, deviceId: String) = "doan20/$targetId/device/$deviceId/provision"
    fun removeTopic(targetId: String, deviceId: String) = "doan20/$targetId/device/$deviceId/remove"
    fun ackTopic(targetId: String, deviceId: String) = "doan20/$targetId/device/$deviceId/ack"

    fun buildSwitchPayload(deviceId: String, isOn: Boolean, gpioPin: Int): String {
        return JSONObject().apply {
            put("id", deviceId)
            put("state", if (isOn) 1 else 0)
            if (gpioPin > 0) put("gpio", gpioPin)
        }.toString()
    }

    fun buildProvisionPayload(deviceId: String, name: String, gpioPin: Int): String {
        return JSONObject().apply {
            put("id", deviceId)
            put("name", name)
            put("gpio", gpioPin)
            put("action", "INIT")
        }.toString()
    }

    fun buildRemovePayload(deviceId: String, gpioPin: Int = 0): String {
        return JSONObject().apply {
            put("id", deviceId)
            if (gpioPin > 0) put("gpio", gpioPin)
            put("action", "REMOVE")
        }.toString()
    }

    fun parseSwitchState(payload: String): Boolean {
        val trimmed = payload.trim()
        return if (trimmed.startsWith("{")) {
            try {
                val obj = JSONObject(trimmed)
                when (val st = obj.opt("state")) {
                    is Number -> st.toInt() == 1
                    is Boolean -> st
                    is String -> st == "1" || st.equals("ON", ignoreCase = true)
                    else -> false
                }
            } catch (_: Exception) {
                trimmed.contains("\"state\":1") || trimmed.contains("\"state\": 1")
            }
        } else {
            trimmed == "1" || trimmed.equals("ON", ignoreCase = true)
        }
    }

    @Synchronized
    fun connect(
        clientIdPrefix: String = "doan20",
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        onConnected: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        val current = client
        if (current != null && activeClientPrefix == clientIdPrefix) {
            if (current.state == MqttClientState.CONNECTED) {
                onConnected?.invoke()
                return
            }
            if (current.state == MqttClientState.CONNECTING ||
                current.state == MqttClientState.CONNECTING_RECONNECT
            ) {
                onConnected?.let { pendingConnectCallbacks.add(it) }
                return
            }
        }

        if (current != null && activeClientPrefix != clientIdPrefix) {
            try {
                current.disconnect()
            } catch (_: Exception) {
            }
            subscriptions.clear()
            client = null
        }

        activeClientPrefix = clientIdPrefix
        onConnected?.let { pendingConnectCallbacks.add(it) }
        val clientId = "${clientIdPrefix}_${UUID.randomUUID().toString().take(8)}"

        try {
            val builtClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener {
                    Log.i(TAG, "MQTT Connected/Reconnected to $host:$port as $clientId")
                    resubscribeAll()
                    flushPendingConnectCallbacks()
                }
                .addDisconnectedListener { context ->
                    Log.w(TAG, "MQTT Disconnected: ${context.cause.message}")
                }
                .buildAsync()

            builtClient.publishes(MqttGlobalPublishFilter.SUBSCRIBED) { publish ->
                dispatchIncomingPublish(publish)
            }

            client = builtClient
            builtClient.connect()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "MQTT Connection failed: ${throwable.message}")
                        synchronized(this) { pendingConnectCallbacks.clear() }
                        onError?.invoke(throwable)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MQTT client", e)
            synchronized(this) { pendingConnectCallbacks.clear() }
            onError?.invoke(e)
        }
    }

    @Synchronized
    private fun flushPendingConnectCallbacks() {
        val callbacks = pendingConnectCallbacks.toList()
        pendingConnectCallbacks.clear()
        callbacks.forEach { it.invoke() }
    }

    private fun dispatchIncomingPublish(publish: Mqtt3Publish) {
        val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
        val incomingTopic = publish.topic
        val topicString = incomingTopic.toString()

        subscriptions.forEach { (filter, callback) ->
            try {
                if (MqttTopicFilter.of(filter).matches(MqttTopic.of(topicString))) {
                    callback(topicString, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching MQTT message for filter $filter", e)
            }
        }
    }

    private fun resubscribeAll() {
        val activeClient = client ?: return
        if (activeClient.state != MqttClientState.CONNECTED) return

        subscriptions.keys.forEach { topic ->
            sendSubscribe(activeClient, topic)
        }
    }

    private fun sendSubscribe(
        activeClient: Mqtt3AsyncClient,
        topic: String,
        onSubscribed: (() -> Unit)? = null
    ) {
        try {
            activeClient.subscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Subscription to $topic failed: ${throwable.message}")
                    } else {
                        Log.i(TAG, "Subscribed to $topic")
                        onSubscribed?.invoke()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during subscription to $topic", e)
        }
    }

    fun disconnect() {
        try {
            subscriptions.clear()
            synchronized(this) { pendingConnectCallbacks.clear() }
            activeClientPrefix = null
            client?.disconnect()
            client = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting MQTT", e)
        }
    }

    fun publish(
        topic: String,
        payload: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        val activeClient = client
        if (activeClient == null || activeClient.state != MqttClientState.CONNECTED) {
            val prefix = activeClientPrefix ?: "doan20"
            connect(
                clientIdPrefix = prefix,
                onConnected = { executePublish(topic, payload, onSuccess, onError) },
                onError = { onError?.invoke(it) }
            )
            return
        }
        executePublish(topic, payload, onSuccess, onError)
    }

    private fun executePublish(
        topic: String,
        payload: String,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        try {
            client?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray(StandardCharsets.UTF_8))
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Publish to $topic failed: ${throwable.message}")
                    onError?.invoke(throwable)
                } else {
                    Log.i(TAG, "Published to $topic -> $payload")
                    onSuccess?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during publish", e)
            onError?.invoke(e)
        }
    }

    fun subscribe(
        topic: String,
        onSubscribed: (() -> Unit)? = null,
        onMessage: (topic: String, payload: String) -> Unit
    ) {
        subscriptions[topic] = onMessage
        val activeClient = client
        if (activeClient != null && activeClient.state == MqttClientState.CONNECTED) {
            sendSubscribe(activeClient, topic, onSubscribed)
        } else {
            val prefix = activeClientPrefix ?: "doan20"
            connect(clientIdPrefix = prefix, onConnected = onSubscribed)
        }
    }

    fun unsubscribe(topic: String) {
        subscriptions.remove(topic)
        try {
            val activeClient = client
            if (activeClient != null && activeClient.state == MqttClientState.CONNECTED) {
                activeClient.unsubscribeWith().topicFilter(topic).send()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from $topic", e)
        }
    }
}
