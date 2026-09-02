package com.happy.poker.core.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * MQTT默认配置
 */
object MqttConfig {
    const val DEFAULT_BROKER_URL = "tcp://localhost:1883"
    const val DEFAULT_USERNAME = "mqtt"
    const val DEFAULT_PASSWORD = "123456"
    const val DEFAULT_CLIENT_ID_PREFIX = "happy-poker"
    const val DEFAULT_QOS = 1
    const val CONNECTION_TIMEOUT = 10
    const val KEEP_ALIVE_INTERVAL = 20
    const val AUTO_RECONNECT = true
}

/**
 * MQTT连接状态
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * MQTT连接配置
 */
data class MqttConnectionConfig(
    val brokerUrl: String = MqttConfig.DEFAULT_BROKER_URL,
    val username: String = MqttConfig.DEFAULT_USERNAME,
    val password: String = MqttConfig.DEFAULT_PASSWORD,
    val clientId: String = "${MqttConfig.DEFAULT_CLIENT_ID_PREFIX}-${UUID.randomUUID().toString().take(8)}",
    val qos: Int = MqttConfig.DEFAULT_QOS,
    val connectionTimeout: Int = MqttConfig.CONNECTION_TIMEOUT,
    val keepAliveInterval: Int = MqttConfig.KEEP_ALIVE_INTERVAL,
    val autoReconnect: Boolean = MqttConfig.AUTO_RECONNECT
)

/**
 * MQTT客户端接口
 */
interface MqttClient {
    val connectionState: StateFlow<ConnectionState>
    val messages: Flow<Pair<String, String>>

    suspend fun connect(config: MqttConnectionConfig = MqttConnectionConfig())
    suspend fun disconnect()
    suspend fun publish(topic: String, payload: String, qos: Int = MqttConfig.DEFAULT_QOS)
    fun subscribe(topic: String)
    fun unsubscribe(topic: String)
}

/**
 * MQTT回调接口
 */
interface MqttCallback {
    fun onConnected()
    fun onDisconnected()
    fun onMessageReceived(topic: String, payload: String)
    fun onError(error: Exception)
}

/**
 * 基于Eclipse Paho的MQTT客户端实现
 */
class PahoMqttClient : MqttClient {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<Pair<String, String>>()
    override val messages: Flow<Pair<String, String>> = _messages.asSharedFlow()

    private val subscriptions = ConcurrentHashMap.newKeySet<String>()
    private var callback: MqttCallback? = null
    private var currentConfig: MqttConnectionConfig? = null
    private var pahoClient: MqttAsyncClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun connect(config: MqttConnectionConfig) = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.CONNECTING
        currentConfig = config

        try {
            LoggerFactory.setLogger(NoOpMqttLogger::class.java.name)

            // 创建Paho客户端
            pahoClient = MqttAsyncClient(
                config.brokerUrl,
                config.clientId,
                MemoryPersistence()
            )

            // 设置回调
            pahoClient!!.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    _connectionState.value = ConnectionState.CONNECTED
                    callback?.onConnected()
                }

                override fun connectionLost(cause: Throwable?) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    callback?.onDisconnected()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic != null && message != null) {
                        val payload = String(message.payload)
                        scope.launch {
                            _messages.emit(Pair(topic, payload))
                            callback?.onMessageReceived(topic, payload)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // 消息发送完成
                }
            })

            // 配置连接选项
            val options = MqttConnectOptions().apply {
                if (config.username.isNotEmpty()) {
                    userName = config.username
                    password = config.password.toCharArray()
                }
                connectionTimeout = config.connectionTimeout
                keepAliveInterval = config.keepAliveInterval
                isAutomaticReconnect = config.autoReconnect
                isCleanSession = true
                maxInflight = 100
            }

            // 连接
            pahoClient!!.connect(options).waitForCompletion(config.connectionTimeout * 1000L)
            if (pahoClient?.isConnected == true) {
                _connectionState.value = ConnectionState.CONNECTED
                callback?.onConnected()
            } else {
                throw MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED.toInt())
            }

            // 订阅之前已订阅的topics
            subscriptions.forEach { topic ->
                pahoClient!!.subscribe(topic, config.qos)
            }

        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            callback?.onError(e)
            throw e
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                pahoClient?.disconnect()?.waitForCompletion(5000)
                pahoClient?.close()
            } catch (e: Exception) {
                // 忽略断开连接时的异常
            } finally {
                pahoClient = null
                _connectionState.value = ConnectionState.DISCONNECTED
                subscriptions.clear()
                currentConfig = null
                callback?.onDisconnected()
            }
        }
    }

    override suspend fun publish(topic: String, payload: String, qos: Int) = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            throw IllegalStateException("MQTT客户端未连接")
        }

        val message = MqttMessage(payload.toByteArray()).apply {
            this.qos = qos
            isRetained = false
        }

        pahoClient?.publish(topic, message)?.waitForCompletion(5000)
            ?: throw IllegalStateException("MQTT客户端未初始化")
    }

    override fun subscribe(topic: String) {
        subscriptions.add(topic)
        // 如果已连接，立即订阅
        if (_connectionState.value == ConnectionState.CONNECTED) {
            try {
                pahoClient?.subscribe(topic, currentConfig?.qos ?: MqttConfig.DEFAULT_QOS)
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    override fun unsubscribe(topic: String) {
        subscriptions.remove(topic)
        // 如果已连接，立即取消订阅
        if (_connectionState.value == ConnectionState.CONNECTED) {
            try {
                pahoClient?.unsubscribe(topic)
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    /**
     * 设置回调
     */
    fun setCallback(callback: MqttCallback) {
        this.callback = callback
    }

    /**
     * 模拟接收消息（用于测试）
     */
    fun simulateMessage(topic: String, payload: String) {
        scope.launch {
            _messages.emit(Pair(topic, payload))
            callback?.onMessageReceived(topic, payload)
        }
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        val connected = pahoClient?.isConnected == true
        if (connected && _connectionState.value != ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.CONNECTED
        }
        return connected
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): MqttConnectionConfig? = currentConfig

    /**
     * 获取订阅的topics
     */
    fun getSubscriptions(): Set<String> = subscriptions.toSet()
}

/**
 * 游戏MQTT客户端
 * 封装MQTT操作，提供游戏相关的消息收发
 */
class GameMqttClient(
    private val mqttClient: PahoMqttClient = PahoMqttClient()
) {
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messageJob: Job? = null

    /**
     * 连接到MQTT broker（使用默认配置）
     */
    suspend fun connect() {
        connect(MqttConnectionConfig())
    }

    /**
     * 连接到MQTT broker
     */
    suspend fun connect(config: MqttConnectionConfig) {
        mqttClient.connect(config)

        // 取消之前的监听任务
        messageJob?.cancel()

        // 启动消息监听
        messageJob = scope.launch {
            mqttClient.messages.collect { (topic, payload) ->
                val message = MessageSerializer.deserialize(payload)
                if (message != null) {
                    _incomingMessages.emit(message)
                }
            }
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect() {
        messageJob?.cancel()
        messageJob = null
        mqttClient.disconnect()
        scope.cancel()
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(topic: String, message: Message) {
        val payload = MessageSerializer.serialize(message)
        mqttClient.publish(topic, payload)
    }

    /**
     * 订阅topic
     */
    fun subscribe(topic: String) {
        mqttClient.subscribe(topic)
    }

    /**
     * 取消订阅
     */
    fun unsubscribe(topic: String) {
        mqttClient.unsubscribe(topic)
    }

    /**
     * 获取连接状态
     */
    fun getConnectionState(): ConnectionState {
        return if (mqttClient.isConnected()) {
            ConnectionState.CONNECTED
        } else {
            mqttClient.connectionState.value
        }
    }

    /**
     * 是否已连接
     */
    fun isConnected(): Boolean = mqttClient.isConnected()
}
