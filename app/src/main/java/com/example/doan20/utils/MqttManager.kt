package com.example.doan20.utils

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.nio.charset.StandardCharsets
import java.util.UUID

object MqttManager {
    private const val TAG = "MqttManager"
    const val DEFAULT_HOST = "broker.hivemq.com"
    const val DEFAULT_PORT = 1883

    private var client: Mqtt3AsyncClient? = null

    fun connect(
        clientIdPrefix: String = "doan20",
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        onConnected: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (client?.state == MqttClientState.CONNECTED) {
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
                        onError?.invoke(throwable)
                    } else {
                        Log.i(TAG, "MQTT Connected to $host:$port as $clientId")
                        onConnected?.invoke()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MQTT client", e)
            onError?.invoke(e)
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
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
            connect(
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

    fun subscribe(topic: String, onMessage: (topic: String, payload: String) -> Unit) {
        try {
            client?.subscribeWith()
                ?.topicFilter(topic)
                ?.callback { publish ->
                    val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                    val incomingTopic = publish.topic.toString()
                    onMessage(incomingTopic, message)
                }
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Subscription to $topic failed: ${throwable.message}")
                    } else {
                        Log.i(TAG, "Subscribed to $topic")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during subscription", e)
        }
    }
}
