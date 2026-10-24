package com.example.doan20.data.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

object MqttManager {
    private const val TAG = "MqttManager"
    const val DEFAULT_HOST = "broker.hivemq.com"
    const val DEFAULT_PORT = 1883

    private var client: Mqtt3AsyncClient? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastMessage = MutableStateFlow<Pair<String, String>?>(null)
    val lastMessage: StateFlow<Pair<String, String>?> = _lastMessage.asStateFlow()

    fun connect(
        clientIdPrefix: String = "doan20",
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        onConnected: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (client?.state == MqttClientState.CONNECTED) {
            _isConnected.value = true
            onConnected?.invoke()
            return
        }

        val clientId = "${clientIdPrefix}_${UUID.randomUUID().toString().take(8)}"

        try {
            client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .automaticReconnectWithDefaultConfig()
                .buildAsync()

            client?.connect()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "MQTT Connection failed: ${throwable.message}")
                        _isConnected.value = false
                        onError?.invoke(throwable)
                    } else {
                        Log.i(TAG, "MQTT Connected successfully to $host:$port as $clientId")
                        _isConnected.value = true
                        onConnected?.invoke()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MQTT client", e)
            _isConnected.value = false
            onError?.invoke(e)
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()?.whenComplete { _, _ ->
                _isConnected.value = false
                Log.i(TAG, "MQTT Disconnected")
            }
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
            Log.w(TAG, "MQTT not connected. Attempting auto-connect before publish...")
            connect(
                onConnected = {
                    executePublish(topic, payload, onSuccess, onError)
                },
                onError = {
                    onError?.invoke(it)
                }
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

    fun subscribe(topic: String, onMessage: (topic: String, payload: String) -> Unit) {
        try {
            client?.subscribeWith()
                ?.topicFilter(topic)
                ?.callback { publish ->
                    val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                    val incomingTopic = publish.topic.toString()
                    Log.d(TAG, "Received message on $incomingTopic: $message")
                    _lastMessage.value = Pair(incomingTopic, message)
                    onMessage(incomingTopic, message)
                }
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Subscription to $topic failed: ${throwable.message}")
                    } else {
                        Log.i(TAG, "Subscribed successfully to topic: $topic")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during subscription", e)
        }
    }
}
