package com.happy.poker.app.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 连接状态
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * 断线重连管理器
 */
class ReconnectManager {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 1000L // 1秒
    private val maxReconnectDelay = 30000L // 30秒

    private var onReconnect: (() -> Unit)? = null
    private var scope: CoroutineScope? = null

    /**
     * 初始化重连管理器
     */
    fun init(scope: CoroutineScope, onReconnect: () -> Unit) {
        this.scope = scope
        this.onReconnect = onReconnect
    }

    /**
     * 连接成功
     */
    fun onConnected() {
        _connectionState.value = ConnectionState.CONNECTED
        reconnectAttempt = 0
        cancelReconnect()
    }

    /**
     * 断开连接
     */
    fun onDisconnected() {
        _connectionState.value = ConnectionState.DISCONNECTED
        startReconnect()
    }

    /**
     * 连接错误
     */
    fun onError(error: Exception) {
        _connectionState.value = ConnectionState.ERROR
        startReconnect()
    }

    /**
     * 开始重连
     */
    private fun startReconnect() {
        if (reconnectAttempt >= maxReconnectAttempts) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        _connectionState.value = ConnectionState.RECONNECTING
        cancelReconnect()

        reconnectJob = scope?.launch {
            val delay = calculateReconnectDelay()
            delay(delay)
            
            reconnectAttempt++
            onReconnect?.invoke()
        }
    }

    /**
     * 计算重连延迟（指数退避）
     */
    private fun calculateReconnectDelay(): Long {
        val delay = baseReconnectDelay * (1 shl reconnectAttempt)
        return minOf(delay, maxReconnectDelay)
    }

    /**
     * 取消重连
     */
    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    /**
     * 手动重连
     */
    fun reconnect() {
        reconnectAttempt = 0
        startReconnect()
    }

    /**
     * 停止重连
     */
    fun stopReconnect() {
        cancelReconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * 获取重连次数
     */
    fun getReconnectAttempt(): Int = reconnectAttempt

    /**
     * 是否正在重连
     */
    fun isReconnecting(): Boolean = _connectionState.value == ConnectionState.RECONNECTING
}
