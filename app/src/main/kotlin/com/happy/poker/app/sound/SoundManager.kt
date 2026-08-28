package com.happy.poker.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * 音效类型
 */
enum class SoundType {
    CARD_PLAY,      // 出牌音效
    CARD_PASS,      // 过牌音效
    BID_HIGH,       // 叫高分音效
    BID_PASS,       // 不叫音效
    WIN,            // 胜利音效
    LOSE,           // 失败音效
    BOMB,           // 炸弹音效
    ROCKET,         // 火箭音效
    SPRING,         // 春天音效
    BUTTON_CLICK,   // 按钮点击音效
    CARD_SELECT     // 选牌音效
}

/**
 * 游戏音效管理器
 */
class SoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var isLoaded = false
    private var isEnabled = true

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        // 加载音效资源（使用系统音效作为占位符）
        // 实际项目中应该替换为真实的音效文件
        loadSounds()
    }

    private fun loadSounds() {
        // 这里使用系统资源作为占位符
        // 在实际项目中，应该添加res/raw/目录下的音效文件
        // 例如：R.raw.card_play, R.raw.card_pass 等
        
        // 暂时标记为已加载
        isLoaded = true
    }

    /**
     * 播放音效
     */
    fun play(soundType: SoundType) {
        if (!isEnabled || !isLoaded) return

        soundMap[soundType]?.let { soundId ->
            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    /**
     * 设置音效开关
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * 获取音效开关状态
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isLoaded = false
    }
}
