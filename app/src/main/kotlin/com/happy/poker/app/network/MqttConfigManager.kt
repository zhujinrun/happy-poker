package com.happy.poker.app.network

import android.content.Context
import android.content.SharedPreferences
import com.happy.poker.core.network.MqttConnectionConfig
import java.util.UUID

/**
 * MQTT配置管理器
 * 支持通过SharedPreferences配置MQTT连接参数
 */
class MqttConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "mqtt_config"
        private const val KEY_BROKER_URL = "broker_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_CLIENT_ID_PREFIX = "client_id_prefix"
        private const val KEY_QOS = "qos"
        private const val KEY_CONNECTION_TIMEOUT = "connection_timeout"
        private const val KEY_KEEP_ALIVE_INTERVAL = "keep_alive_interval"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"

        // 默认值
        const val DEFAULT_BROKER_URL = "tcp://172.16.101.118:1883"
        const val DEFAULT_USERNAME = "mqtt"
        const val DEFAULT_PASSWORD = "123456"
        const val DEFAULT_CLIENT_ID_PREFIX = "happy-poker"
        const val DEFAULT_QOS = 1
        const val DEFAULT_CONNECTION_TIMEOUT = 10
        const val DEFAULT_KEEP_ALIVE_INTERVAL = 20
        const val DEFAULT_AUTO_RECONNECT = true
    }

    /**
     * 获取broker URL
     */
    fun getBrokerUrl(): String = prefs.getString(KEY_BROKER_URL, DEFAULT_BROKER_URL) ?: DEFAULT_BROKER_URL

    /**
     * 设置broker URL
     */
    fun setBrokerUrl(url: String) {
        prefs.edit().putString(KEY_BROKER_URL, url).apply()
    }

    /**
     * 获取用户名
     */
    fun getUsername(): String = prefs.getString(KEY_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME

    /**
     * 设置用户名
     */
    fun setUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    /**
     * 获取密码
     */
    fun getPassword(): String = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

    /**
     * 设置密码
     */
    fun setPassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    /**
     * 获取客户端ID前缀
     */
    fun getClientIdPrefix(): String = prefs.getString(KEY_CLIENT_ID_PREFIX, DEFAULT_CLIENT_ID_PREFIX) ?: DEFAULT_CLIENT_ID_PREFIX

    /**
     * 设置客户端ID前缀
     */
    fun setClientIdPrefix(prefix: String) {
        prefs.edit().putString(KEY_CLIENT_ID_PREFIX, prefix).apply()
    }

    /**
     * 获取QoS
     */
    fun getQos(): Int = prefs.getInt(KEY_QOS, DEFAULT_QOS)

    /**
     * 设置QoS
     */
    fun setQos(qos: Int) {
        prefs.edit().putInt(KEY_QOS, qos).apply()
    }

    /**
     * 获取连接超时时间（秒）
     */
    fun getConnectionTimeout(): Int = prefs.getInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT)

    /**
     * 设置连接超时时间（秒）
     */
    fun setConnectionTimeout(timeout: Int) {
        prefs.edit().putInt(KEY_CONNECTION_TIMEOUT, timeout).apply()
    }

    /**
     * 获取心跳间隔（秒）
     */
    fun getKeepAliveInterval(): Int = prefs.getInt(KEY_KEEP_ALIVE_INTERVAL, DEFAULT_KEEP_ALIVE_INTERVAL)

    /**
     * 设置心跳间隔（秒）
     */
    fun setKeepAliveInterval(interval: Int) {
        prefs.edit().putInt(KEY_KEEP_ALIVE_INTERVAL, interval).apply()
    }

    /**
     * 获取是否自动重连
     */
    fun getAutoReconnect(): Boolean = prefs.getBoolean(KEY_AUTO_RECONNECT, DEFAULT_AUTO_RECONNECT)

    /**
     * 设置是否自动重连
     */
    fun setAutoReconnect(autoReconnect: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, autoReconnect).apply()
    }

    /**
     * 获取MQTT连接配置
     */
    fun getMqttConnectionConfig(): MqttConnectionConfig {
        return MqttConnectionConfig(
            brokerUrl = getBrokerUrl(),
            username = getUsername(),
            password = getPassword(),
            clientId = "${getClientIdPrefix()}-${java.util.UUID.randomUUID().toString().take(8)}",
            qos = getQos(),
            connectionTimeout = getConnectionTimeout(),
            keepAliveInterval = getKeepAliveInterval(),
            autoReconnect = getAutoReconnect()
        )
    }

    /**
     * 重置所有配置为默认值
     */
    fun resetToDefaults() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    /**
     * 检查是否已配置（非默认值）
     */
    fun isConfigured(): Boolean {
        return prefs.contains(KEY_BROKER_URL)
    }
}
